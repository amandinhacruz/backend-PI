package com.projeto_pi.Projeto.PI.matching;

import com.projeto_pi.Projeto.PI.entity.AnaliseAfinidade;
import com.projeto_pi.Projeto.PI.entity.DossieMatch;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade.Dossie;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade.PerfilInvestidor;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade.PerfilStartup;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade.ResultadoAnalise;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaClaude;
import com.projeto_pi.Projeto.PI.matching.ia.IaProperties;
import com.projeto_pi.Projeto.PI.repository.AnaliseAfinidadeRepository;
import com.projeto_pi.Projeto.PI.repository.DossieMatchRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Orquestra o analista generativo: guarda as análises, só chama a IA para pares novos ou cujo
 * perfil mudou, processa em lotes e em segundo plano, e se protege de falhas.
 *
 * A tela de matching nunca espera pela IA: recebe na hora o que já está guardado (ou a nota local)
 * e a lista de análises pendentes, que ficam prontas nos carregamentos seguintes.
 */
@Service
public class AnaliseAfinidadeService {

    private static final Logger log = LoggerFactory.getLogger(AnaliseAfinidadeService.class);

    /** Mude ao alterar os prompts: as análises guardadas passam a ser refeitas. */
    static final String VERSAO_PROMPT = "2026-11-16";
    private static final Duration PAUSA_APOS_FALHA = Duration.ofSeconds(60);
    private static final int LIMITE_TEXTO = 1500;

    private final AnalistaAfinidade analista;
    private final IaProperties props;
    private final AnaliseAfinidadeRepository analises;
    private final DossieMatchRepository dossies;
    private final TransactionTemplate transacao;
    private final JsonMapper json;

    private final ExecutorService executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(50), tarefa -> {
                Thread thread = new Thread(tarefa, "vertice-ia");
                thread.setDaemon(true);
                return thread;
            });
    private final Set<Long> emAndamento = ConcurrentHashMap.newKeySet();
    private volatile Instant pausadoAte = Instant.EPOCH;
    private volatile String ultimoErro;
    private volatile LocalDateTime ultimoSucessoEm;

    public AnaliseAfinidadeService(AnalistaAfinidade analista, IaProperties props,
                                   AnaliseAfinidadeRepository analises, DossieMatchRepository dossies,
                                   PlatformTransactionManager transacoes, JsonMapper json) {
        this.analista = analista;
        this.props = props;
        this.analises = analises;
        this.dossies = dossies;
        this.transacao = new TransactionTemplate(transacoes);
        this.json = json;
    }

    @PreDestroy
    void encerrar() {
        executor.shutdownNow();
    }

    /** Startup candidata e se o perfil dela (e o do investidor) pode ser enviado ao analista generativo. */
    public record Candidata(StartupResumo startup, boolean aptaParaIa) {
    }

    public record Analises(Map<Long, AnaliseAfinidade> porStartup, Set<Long> pendentes, boolean iaAtiva) {
    }

    public record DossieArmazenado(Dossie dossie, String modelo, LocalDateTime geradoEm) {
    }

    public record StatusIa(boolean habilitada, boolean chaveConfigurada, boolean disponivel, String modelo,
                           String politicaMinima, boolean assincrono, long analisesArmazenadas,
                           long dossiesArmazenados, String ultimoErro, LocalDateTime ultimoSucessoEm) {
    }

    public boolean iaAtiva() {
        return analista.disponivel();
    }

    public String modelo() {
        return analista.modelo();
    }

    public StatusIa status() {
        return new StatusIa(props.habilitada(), AnalistaClaude.chaveConfigurada(), analista.disponivel(),
                analista.modelo(), props.politicaMinima(), props.assincrono(), analises.count(),
                dossies.count(), ultimoErro, ultimoSucessoEm);
    }

    /* ---------------------------------------------------------------- perfis anônimos */

    public static PerfilInvestidor perfilAnonimo(PreferenciasInvestidor investidor) {
        return new PerfilInvestidor(
                investidor.segmentos(),
                investidor.estagios(),
                Formatos.faixa(investidor.ticket().min(), investidor.ticket().max()),
                Formatos.texto(investidor.uf()),
                Formatos.cortar(investidor.sobre(), LIMITE_TEXTO),
                Formatos.cortar(investidor.carreira(), LIMITE_TEXTO),
                Formatos.cortar(investidor.busca(), LIMITE_TEXTO));
    }

    /** Sem nome, e-mail, CNPJ, responsável, site ou links: só dados de negócio. */
    public static PerfilStartup perfilAnonimo(StartupResumo startup) {
        return new PerfilStartup(
                "S" + startup.id(),
                Formatos.texto(startup.segmento()),
                Formatos.texto(startup.estagio()),
                Formatos.faixa(startup.ticketMin(), startup.ticketMax()),
                startup.local(),
                Formatos.cortar(startup.resumo(), LIMITE_TEXTO),
                Formatos.cortar(startup.problema(), LIMITE_TEXTO),
                Formatos.cortar(startup.diferenciais(), LIMITE_TEXTO),
                Formatos.cortar(startup.necessidades(), LIMITE_TEXTO));
    }

    /* ------------------------------------------------------------------- análises */

    private record Trabalho(Long startupPerfilId, PerfilStartup perfil, String hash) {
    }

    /**
     * Devolve as análises guardadas que continuam válidas e providencia as que faltam.
     * As candidatas devem vir ordenadas da mais para a menos promissora: só as primeiras
     * {@code maxPorConsulta} novas são enviadas ao modelo a cada chamada.
     */
    public Analises obter(Long investidorPerfilId, PreferenciasInvestidor preferencias, List<Candidata> candidatas) {
        if (!analista.disponivel()) {
            return new Analises(Map.of(), Set.of(), false);
        }
        PerfilInvestidor investidor = perfilAnonimo(preferencias);
        Map<Long, AnaliseAfinidade> existentes = analises.findByInvestidorPerfilId(investidorPerfilId).stream()
                .collect(Collectors.toMap(AnaliseAfinidade::getStartupPerfilId, a -> a, (a, b) -> a));

        Map<Long, AnaliseAfinidade> validas = new HashMap<>();
        List<Trabalho> faltando = new ArrayList<>();
        for (Candidata candidata : candidatas) {
            if (!candidata.aptaParaIa()) {
                continue;
            }
            PerfilStartup perfil = perfilAnonimo(candidata.startup());
            String hash = hash(investidor, perfil);
            AnaliseAfinidade existente = existentes.get(candidata.startup().id());
            if (existente != null && hash.equals(existente.getHash())) {
                validas.put(candidata.startup().id(), existente);
            } else if (faltando.size() < props.maxPorConsulta()) {
                faltando.add(new Trabalho(candidata.startup().id(), perfil, hash));
            }
        }
        if (faltando.isEmpty() || pausado()) {
            return new Analises(validas, Set.of(), true);
        }
        if (props.assincrono()) {
            agendar(investidorPerfilId, investidor, faltando);
            Set<Long> pendentes = new HashSet<>();
            faltando.forEach(trabalho -> pendentes.add(trabalho.startupPerfilId()));
            return new Analises(validas, pendentes, true);
        }
        validas.putAll(processar(investidorPerfilId, investidor, faltando));
        return new Analises(validas, Set.of(), true);
    }

    /** Análise guardada de um par, se existir (usada no portal da startup). */
    public Optional<AnaliseAfinidade> guardada(Long investidorPerfilId, Long startupPerfilId) {
        if (!analista.disponivel()) {
            return Optional.empty();
        }
        return analises.findByInvestidorPerfilIdAndStartupPerfilId(investidorPerfilId, startupPerfilId);
    }

    private void agendar(Long investidorPerfilId, PerfilInvestidor investidor, List<Trabalho> trabalhos) {
        if (!emAndamento.add(investidorPerfilId)) {
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    processar(investidorPerfilId, investidor, trabalhos);
                } finally {
                    emAndamento.remove(investidorPerfilId);
                }
            });
        } catch (RejectedExecutionException e) {
            emAndamento.remove(investidorPerfilId);
        }
    }

    private Map<Long, AnaliseAfinidade> processar(Long investidorPerfilId, PerfilInvestidor investidor,
                                                  List<Trabalho> trabalhos) {
        Map<Long, AnaliseAfinidade> salvas = new HashMap<>();
        int lote = Math.max(1, props.lote());
        for (int inicio = 0; inicio < trabalhos.size(); inicio += lote) {
            List<Trabalho> parte = trabalhos.subList(inicio, Math.min(inicio + lote, trabalhos.size()));
            try {
                List<ResultadoAnalise> resultados =
                        analista.avaliar(investidor, parte.stream().map(Trabalho::perfil).toList());
                Map<String, ResultadoAnalise> porRef = new HashMap<>();
                if (resultados != null) {
                    resultados.forEach(resultado -> porRef.putIfAbsent(resultado.ref(), resultado));
                }
                Map<Long, AnaliseAfinidade> gravadas =
                        transacao.execute(status -> gravar(investidorPerfilId, parte, porRef));
                if (gravadas != null) {
                    salvas.putAll(gravadas);
                }
                ultimoSucessoEm = LocalDateTime.now();
                ultimoErro = null;
            } catch (RuntimeException e) {
                registrarFalha(e);
                break;
            }
        }
        return salvas;
    }

    private Map<Long, AnaliseAfinidade> gravar(Long investidorPerfilId, List<Trabalho> parte,
                                               Map<String, ResultadoAnalise> porRef) {
        Map<Long, AnaliseAfinidade> gravadas = new HashMap<>();
        for (Trabalho trabalho : parte) {
            ResultadoAnalise resultado = porRef.get(trabalho.perfil().ref());
            if (resultado == null) {
                continue;
            }
            AnaliseAfinidade analise = analises
                    .findByInvestidorPerfilIdAndStartupPerfilId(investidorPerfilId, trabalho.startupPerfilId())
                    .orElseGet(() -> new AnaliseAfinidade(investidorPerfilId, trabalho.startupPerfilId()));
            analise.registrar(trabalho.hash(), nota(resultado.nota()), nota(resultado.aderenciaTese()),
                    nota(resultado.problemaMercado()), nota(resultado.diferenciais()),
                    Formatos.cortar(resultado.justificativa(), 600),
                    itens(resultado.pontosFortes(), 3), itens(resultado.pontosAtencao(), 2),
                    itens(resultado.perguntas(), 3), analista.modelo());
            gravadas.put(trabalho.startupPerfilId(), analises.save(analise));
        }
        return gravadas;
    }

    /* -------------------------------------------------------------------- dossiê */

    /** Dossiê gerado pelo analista (guardado por par); vazio quando a IA não pode ser usada. */
    public Optional<DossieArmazenado> dossie(Long investidorPerfilId, PreferenciasInvestidor preferencias,
                                             StartupResumo startup, boolean aptaParaIa) {
        if (!analista.disponivel() || !aptaParaIa || pausado()) {
            return Optional.empty();
        }
        PerfilInvestidor investidor = perfilAnonimo(preferencias);
        PerfilStartup perfil = perfilAnonimo(startup);
        String hash = hash(investidor, perfil);
        Optional<DossieMatch> existente =
                dossies.findByInvestidorPerfilIdAndStartupPerfilId(investidorPerfilId, startup.id());
        if (existente.isPresent() && hash.equals(existente.get().getHash())) {
            DossieMatch guardado = existente.get();
            return Optional.of(new DossieArmazenado(
                    json.readValue(guardado.getConteudo(), Dossie.class), guardado.getModelo(), guardado.getCriadoEm()));
        }
        try {
            Dossie gerado = limpar(analista.dossie(investidor, perfil));
            DossieMatch salvo = transacao.execute(status -> {
                DossieMatch registro = existente.orElseGet(() -> new DossieMatch(investidorPerfilId, startup.id()));
                registro.registrar(hash, json.writeValueAsString(gerado), analista.modelo());
                return dossies.save(registro);
            });
            ultimoSucessoEm = LocalDateTime.now();
            ultimoErro = null;
            return Optional.of(new DossieArmazenado(gerado, analista.modelo(),
                    salvo == null ? LocalDateTime.now() : salvo.getCriadoEm()));
        } catch (RuntimeException e) {
            registrarFalha(e);
            return Optional.empty();
        }
    }

    private static Dossie limpar(Dossie dossie) {
        return new Dossie(
                Formatos.cortar(dossie.resumoExecutivo(), 1200),
                Formatos.cortar(dossie.aderenciaTese(), 1200),
                lista(dossie.oportunidades(), 4),
                lista(dossie.riscos(), 4),
                lista(dossie.perguntas(), 5),
                lista(dossie.proximosPassos(), 3));
    }

    /* -------------------------------------------------------------------- apoio */

    private boolean pausado() {
        return Instant.now().isBefore(pausadoAte);
    }

    private void registrarFalha(RuntimeException e) {
        pausadoAte = Instant.now().plus(PAUSA_APOS_FALHA);
        ultimoErro = LocalDateTime.now() + " - " + e.getMessage();
        log.warn("Analista generativo indisponível; usando a afinidade local por {}s. Motivo: {}",
                PAUSA_APOS_FALHA.toSeconds(), e.getMessage());
    }

    private static int nota(int valor) {
        return Math.max(0, Math.min(100, valor));
    }

    private static List<String> lista(List<String> itens, int maximo) {
        if (itens == null) {
            return List.of();
        }
        return itens.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(item -> Formatos.cortar(item.replaceAll("\\s+", " "), 300))
                .limit(maximo)
                .toList();
    }

    private static String itens(List<String> itens, int maximo) {
        return String.join("\n", lista(itens, maximo));
    }

    /** Itens guardados em uma coluna (um por linha) de volta para lista. */
    public static List<String> linhas(String valor) {
        if (valor == null || valor.isBlank()) {
            return List.of();
        }
        return valor.lines().map(String::trim).filter(linha -> !linha.isEmpty()).toList();
    }

    String hash(PerfilInvestidor investidor, PerfilStartup startup) {
        String base = VERSAO_PROMPT + "|" + analista.modelo() + "|"
                + json.writeValueAsString(investidor) + "|" + json.writeValueAsString(startup);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(base.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
