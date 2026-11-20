package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.MatchResponseDTO;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.AiStatus;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.Breakdown;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.Criteria;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.DecisionResult;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.Dossier;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.MatchCard;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.Queue;
import com.projeto_pi.Projeto.PI.dto.MatchingDTOs.RequestCard;
import com.projeto_pi.Projeto.PI.entity.AnaliseAfinidade;
import com.projeto_pi.Projeto.PI.entity.Conexao;
import com.projeto_pi.Projeto.PI.entity.InvestidorMentor;
import com.projeto_pi.Projeto.PI.entity.Papel;
import com.projeto_pi.Projeto.PI.entity.Startup;
import com.projeto_pi.Projeto.PI.entity.StatusConexao;
import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.TipoInvestidorMentor;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.matching.AfinidadeLocal;
import com.projeto_pi.Projeto.PI.matching.AnaliseAfinidadeService;
import com.projeto_pi.Projeto.PI.matching.AnaliseAfinidadeService.Analises;
import com.projeto_pi.Projeto.PI.matching.AnaliseAfinidadeService.Candidata;
import com.projeto_pi.Projeto.PI.matching.AnaliseAfinidadeService.DossieArmazenado;
import com.projeto_pi.Projeto.PI.matching.AprendizadoService;
import com.projeto_pi.Projeto.PI.matching.AprendizadoService.PerfilAprendido;
import com.projeto_pi.Projeto.PI.matching.Criterios;
import com.projeto_pi.Projeto.PI.matching.Criterios.Faixa;
import com.projeto_pi.Projeto.PI.matching.Formatos;
import com.projeto_pi.Projeto.PI.matching.PreferenciasInvestidor;
import com.projeto_pi.Projeto.PI.matching.StartupResumo;
import com.projeto_pi.Projeto.PI.matching.ia.IaProperties;
import com.projeto_pi.Projeto.PI.matching.ia.VersaoPolitica;
import com.projeto_pi.Projeto.PI.repository.ConexaoRepository;
import com.projeto_pi.Projeto.PI.repository.StartupRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Matchmaking (RF03/RF04).
 *
 * Portal do investidor: fila só com startups que batem em segmento, estágio e ticket médio,
 * ordenadas por afinidade. A afinidade combina o analista generativo (quando ligado), o motor
 * local e o aprendizado com o uso. O investidor solicita conexão ou passa.
 *
 * Portal da startup: só os pedidos de conexão recebidos, para firmar ou negar.
 */
@Service
public class MatchmakingService {

    /** Peso do analista generativo na nota base quando há análise; o restante é do motor local. */
    private static final double PESO_IA = 0.7;

    private final UsuarioRepository usuarios;
    private final ConexaoRepository conexoes;
    private final StartupRepository startups;
    private final AnaliseAfinidadeService analisesIa;
    private final AprendizadoService aprendizado;
    private final IaProperties ia;
    private final com.projeto_pi.Projeto.PI.repository.FotoPerfilRepository fotos;

    public MatchmakingService(UsuarioRepository usuarios, ConexaoRepository conexoes, StartupRepository startups,
                              AnaliseAfinidadeService analisesIa, AprendizadoService aprendizado, IaProperties ia,
                              com.projeto_pi.Projeto.PI.repository.FotoPerfilRepository fotos) {
        this.fotos = fotos;
        this.usuarios = usuarios;
        this.conexoes = conexoes;
        this.startups = startups;
        this.analisesIa = analisesIa;
        this.aprendizado = aprendizado;
        this.ia = ia;
    }

    /* =============================================================== portal do investidor */

    private record Contexto(Usuario usuario, InvestidorMentor perfil, PreferenciasInvestidor preferencias) {
    }

    private record Pontuada(StartupResumo startup, int afinidade, boolean pendente, AnaliseAfinidade analise,
                            AfinidadeLocal.Resultado local, int ajustePreferencia, int ajusteReputacao) {

        String origem() {
            return analise != null ? "IA" : "LOCAL";
        }
    }

    @Transactional
    public Queue filaDoInvestidor(String email) {
        Contexto ctx = investidor(email);
        List<StartupResumo> aprovadas = usuarios.findStartupsAprovadas();
        List<Pontuada> fila = pontuar(ctx, elegiveis(ctx, aprovadas));
        Set<Long> comFoto = fila.isEmpty() ? Set.of()
                : fotos.comFoto(fila.stream().map(p -> p.startup().usuarioId()).toList());
        boolean iaAtiva = analisesIa.iaAtiva();
        int pendentes = (int) fila.stream().filter(Pontuada::pendente).count();
        PreferenciasInvestidor pref = ctx.preferencias();
        return new Queue(
                fila.stream().map(pontuada -> cartao(ctx, pontuada, comFoto)).toList(),
                new AiStatus(iaAtiva, pendentes, iaAtiva ? analisesIa.modelo() : null),
                new Criteria(pref.segmentos(), pref.estagios(), pref.ticket().min(), pref.ticket().max()),
                aprovadas.size());
    }

    @Transactional
    public DecisionResult decidir(String email, Long startupPerfilId, boolean solicitar) {
        Contexto ctx = investidor(email);
        Pontuada escolhida = pontuar(ctx, elegiveis(ctx, usuarios.findStartupsAprovadas())).stream()
                .filter(pontuada -> pontuada.startup().id().equals(startupPerfilId))
                .findFirst()
                .orElseThrow(() -> naoDisponivel(ctx, startupPerfilId));
        StartupResumo startup = escolhida.startup();
        Conexao conexao = new Conexao(
                ctx.usuario(), usuarios.getReferenceById(startup.usuarioId()),
                solicitar ? StatusConexao.SOLICITADA : StatusConexao.DISPENSADA,
                escolhida.afinidade(), escolhida.origem());
        conexao.fotografarStartup(startup.segmento(), startup.estagio(), startup.uf(), startup.ticketMedio());
        Conexao salva = conexoes.save(conexao);
        return new DecisionResult(salva.getId(), salva.getStatus().name());
    }

    @Transactional
    public Dossier dossie(String email, Long startupPerfilId) {
        Contexto ctx = investidor(email);
        StartupResumo startup = usuarios.findStartupAprovada(startupPerfilId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrada"));
        Optional<Conexao> conexao = conexoes.findPar(ctx.usuario().getId(), startup.usuarioId());
        boolean visivel = conexao
                .map(c -> c.getStatus() == StatusConexao.SOLICITADA || c.getStatus() == StatusConexao.FIRMADA)
                .orElseGet(() -> Criterios.avaliar(ctx.preferencias(), startup).elegivel());
        if (!visivel) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrada");
        }
        Optional<DossieArmazenado> gerado = analisesIa.dossie(
                ctx.perfil().getId(), ctx.preferencias(), startup, aptoParaIa(ctx.usuario(), startup));
        if (gerado.isPresent()) {
            var d = gerado.get().dossie();
            return new Dossier("IA", gerado.get().modelo(), d.resumoExecutivo(), d.aderenciaTese(),
                    d.oportunidades(), d.riscos(), d.perguntas(), d.proximosPassos(), gerado.get().geradoEm());
        }
        return dossieLocal(ctx, startup);
    }

    private ResponseStatusException naoDisponivel(Contexto ctx, Long startupPerfilId) {
        boolean jaDecidida = usuarios.findStartupAprovada(startupPerfilId)
                .flatMap(startup -> conexoes.findPar(ctx.usuario().getId(), startup.usuarioId()))
                .isPresent();
        return jaDecidida
                ? new ResponseStatusException(HttpStatus.CONFLICT, "Você já decidiu sobre esta startup")
                : new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não disponível no seu matching");
    }

    private List<StartupResumo> elegiveis(Contexto ctx, List<StartupResumo> aprovadas) {
        Set<Long> jaDecididas = conexoes.findStartupsJaDecididas(ctx.usuario().getId());
        return aprovadas.stream()
                .filter(startup -> !jaDecididas.contains(startup.usuarioId()))
                .filter(startup -> Criterios.avaliar(ctx.preferencias(), startup).elegivel())
                .toList();
    }

    /** Camadas 2 a 4: nota local, analista generativo e aprendizado, da maior para a menor afinidade. */
    private List<Pontuada> pontuar(Contexto ctx, List<StartupResumo> elegiveis) {
        if (elegiveis.isEmpty()) {
            return List.of();
        }
        Map<Long, AfinidadeLocal.Resultado> locais = AfinidadeLocal.calcular(ctx.preferencias(), elegiveis);
        PerfilAprendido aprendido = aprendizado.doInvestidor(ctx.usuario().getId());
        Map<Long, Integer> reputacao =
                aprendizado.ajusteReputacao(elegiveis.stream().map(StartupResumo::usuarioId).toList());

        // O motor local ordena primeiro; o analista generativo reavalia as mais promissoras.
        List<Candidata> candidatas = elegiveis.stream()
                .sorted(Comparator.comparingInt((StartupResumo s) -> locais.get(s.id()).nota()).reversed()
                        .thenComparing(StartupResumo::id))
                .map(startup -> new Candidata(startup, aptoParaIa(ctx.usuario(), startup)))
                .toList();
        Analises analises = analisesIa.obter(ctx.perfil().getId(), ctx.preferencias(), candidatas);

        List<Pontuada> pontuadas = new ArrayList<>();
        for (StartupResumo startup : elegiveis) {
            AfinidadeLocal.Resultado local = locais.get(startup.id());
            AnaliseAfinidade analise = analises.porStartup().get(startup.id());
            double base = analise != null
                    ? PESO_IA * analise.getNota() + (1 - PESO_IA) * local.nota()
                    : local.nota();
            int ajustePreferencia = aprendido.ajuste(startup);
            int ajusteReputacao = reputacao.getOrDefault(startup.usuarioId(), 0);
            int afinidade = (int) Math.max(0, Math.min(100,
                    Math.round(base) + ajustePreferencia + ajusteReputacao));
            pontuadas.add(new Pontuada(startup, afinidade, analises.pendentes().contains(startup.id()),
                    analise, local, ajustePreferencia, ajusteReputacao));
        }
        pontuadas.sort(Comparator.comparingInt(Pontuada::afinidade).reversed()
                .thenComparing(pontuada -> pontuada.startup().id()));
        return pontuadas;
    }

    /** O perfil só vai ao analista generativo se os dois lados aceitaram a versão da Política que trata de IA. */
    private boolean aptoParaIa(Usuario investidor, StartupResumo startup) {
        return VersaoPolitica.atende(investidor.getVersaoPoliticaConsentida(), ia.politicaMinima())
                && VersaoPolitica.atende(startup.versaoPolitica(), ia.politicaMinima());
    }

    private MatchCard cartao(Contexto ctx, Pontuada p, Set<Long> comFoto) {
        StartupResumo s = p.startup();
        PreferenciasInvestidor pref = ctx.preferencias();
        AnaliseAfinidade analise = p.analise();

        List<String> fatores = new ArrayList<>();
        fatores.add("Segmento: " + Formatos.texto(s.segmento()));
        fatores.add("Estágio: " + Formatos.texto(s.estagio()));
        if (!pref.ticket().vazia()) {
            fatores.add("Ticket: " + Math.round(p.local().ticket() * 100) + "% da faixa buscada cabe na sua");
        }
        if (p.local().proximidade() == 1) {
            fatores.add("Mesmo estado (" + Formatos.texto(s.uf()) + ")");
        }
        if (p.ajustePreferencia() > 0) {
            fatores.add("Parecida com startups que você já solicitou");
        } else if (p.ajustePreferencia() < 0) {
            fatores.add("Parecida com startups que você passou");
        }
        if (p.ajusteReputacao() > 0) {
            fatores.add("Bem avaliada em conexões anteriores");
        }

        String resumo;
        List<String> fortes;
        List<String> atencao;
        List<String> perguntas;
        if (analise != null) {
            resumo = analise.getJustificativa();
            fortes = AnaliseAfinidadeService.linhas(analise.getPontosFortes());
            atencao = AnaliseAfinidadeService.linhas(analise.getPontosAtencao());
            perguntas = AnaliseAfinidadeService.linhas(analise.getPerguntas());
        } else {
            resumo = resumoLocal(pref, s);
            fortes = fortesLocais(p);
            atencao = atencaoLocal(s);
            perguntas = List.of();
        }

        return new MatchCard(
                s.id().toString(), s.usuarioId(), Formatos.texto(s.nome()), "Startup cadastrada",
                tags(s.segmento(), s.estagio()), descricao(s), s.local(), s.ticketMin(), s.ticketMax(),
                p.afinidade(), p.origem(), p.pendente(), resumo, fortes, atencao, perguntas, fatores,
                new Breakdown(analise != null ? analise.getNota() : null, p.local().nota(),
                        p.ajustePreferencia(), p.ajusteReputacao()),
                comFoto.contains(s.usuarioId()),
                !Formatos.texto(s.pitchDeckNome()).isEmpty(), !Formatos.texto(s.canvasNome()).isEmpty(),
                Formatos.texto(s.video()));
    }

    private static List<String> tags(String... valores) {
        List<String> tags = new ArrayList<>();
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                tags.add(valor.trim());
            }
        }
        return tags;
    }

    private static String descricao(StartupResumo s) {
        for (String candidato : new String[]{s.resumo(), s.problema(), s.pitch()}) {
            if (candidato != null && !candidato.isBlank()) {
                return Formatos.cortar(candidato, 400);
            }
        }
        return "";
    }

    private static String resumoLocal(PreferenciasInvestidor pref, StartupResumo s) {
        StringBuilder texto = new StringBuilder("A startup atua em ")
                .append(Formatos.texto(s.segmento()))
                .append(", um dos segmentos de interesse do investidor");
        if (!pref.estagios().isEmpty()) {
            texto.append(", e está em ").append(Formatos.texto(s.estagio())).append(", estágio que ele prefere");
        }
        texto.append(".");
        if (!pref.ticket().vazia()) {
            texto.append(" Busca ").append(Formatos.faixa(s.ticketMin(), s.ticketMax()))
                    .append(", dentro da faixa de ticket dele.");
        }
        return texto.toString();
    }

    private static List<String> fortesLocais(Pontuada p) {
        List<String> fortes = new ArrayList<>();
        if (!p.local().termosEmComum().isEmpty()) {
            fortes.add("Termos em comum com a sua tese: " + String.join(", ", p.local().termosEmComum()));
        }
        if (p.local().completude() >= 0.85) {
            fortes.add("Perfil completo, com materiais anexados");
        }
        if (p.local().proximidade() == 1) {
            fortes.add("Fica no seu estado");
        }
        return fortes;
    }

    private static List<String> atencaoLocal(StartupResumo s) {
        List<String> atencao = new ArrayList<>();
        if (Formatos.texto(s.resumo()).length() < 80) {
            atencao.add("Descrição curta: peça mais detalhes sobre o negócio");
        }
        if (Formatos.texto(s.pitchDeckNome()).isEmpty()) {
            atencao.add("Sem pitch deck anexado");
        }
        return atencao;
    }

    private Dossier dossieLocal(Contexto ctx, StartupResumo s) {
        PreferenciasInvestidor pref = ctx.preferencias();
        String resumo = Formatos.texto(s.resumo()).isEmpty()
                ? "A startup não informou um resumo do negócio."
                : Formatos.cortar(s.resumo(), 1200);
        if (!Formatos.texto(s.problema()).isEmpty()) {
            resumo += (resumo.matches(".*[.!?…]$") ? " " : ". ")
                    + "Problema que resolve: " + Formatos.cortar(s.problema(), 600);
        }
        List<String> oportunidades = new ArrayList<>();
        if (!Formatos.texto(s.diferenciais()).isEmpty()) {
            oportunidades.add("Diferenciais declarados: " + Formatos.cortar(s.diferenciais(), 300));
        }
        if (Criterios.norm(pref.uf()).equals(Criterios.norm(s.uf())) && !Criterios.norm(s.uf()).isEmpty()) {
            oportunidades.add("Fica no mesmo estado do investidor, o que facilita o acompanhamento");
        }
        List<String> riscos = new ArrayList<>(atencaoLocal(s));
        if (Formatos.texto(s.canvasNome()).isEmpty()) {
            riscos.add("Sem Business Model Canvas anexado");
        }
        if (Formatos.texto(s.diferenciais()).isEmpty()) {
            riscos.add("Não descreveu diferenciais competitivos");
        }
        return new Dossier("LOCAL", null, resumo, resumoLocal(pref, s), oportunidades, riscos,
                List.of("Qual é o tamanho do mercado e quem são os clientes hoje?",
                        "Como o valor buscado será usado e em quanto tempo?",
                        "Quem compõe o time e qual a dedicação de cada pessoa?"),
                List.of("Solicitar a conexão para abrir a conversa",
                        "Pedir o pitch deck e o canvas atualizados",
                        "Agendar uma primeira reunião pela plataforma"),
                LocalDateTime.now());
    }

    /* ================================================================= portal da startup */

    @Transactional(readOnly = true)
    public List<RequestCard> pedidosDaStartup(String email) {
        Usuario usuario = usuarioAprovado(email, Papel.STARTUP);
        Long startupPerfilId = usuario.getStartup() == null ? null : usuario.getStartup().getId();
        List<RequestCard> pedidos = new ArrayList<>();
        for (Conexao conexao : conexoes.findByStartup_IdAndStatusOrderByCriadaEmDesc(
                usuario.getId(), StatusConexao.SOLICITADA)) {
            Usuario investidor = conexao.getInvestidor();
            InvestidorMentor perfil = investidor.getInvestidorMentor();
            if (investidor.getStatus() != StatusUsuario.APROVADO || perfil == null) {
                continue;
            }
            PreferenciasInvestidor pref = PreferenciasInvestidor.de(perfil);
            String resumo = (startupPerfilId == null
                    ? Optional.<AnaliseAfinidade>empty()
                    : analisesIa.guardada(perfil.getId(), startupPerfilId))
                    .map(AnaliseAfinidade::getJustificativa)
                    .filter(texto -> texto != null && !texto.isBlank())
                    .orElseGet(() -> resumoDoInvestidor(pref));
            pedidos.add(new RequestCard(
                    conexao.getId().toString(), investidor.getId(), nomeDe(perfil, investidor), papelDe(perfil),
                    pref.segmentos(), descricaoDe(perfil),
                    local(perfil.getCity(), perfil.getState()), perfil.getTicketMin(), perfil.getTicketMax(),
                    conexao.getAfinidade(), resumo, conexao.getCriadaEm(),
                    fotos.existsByUsuarioId(investidor.getId())));
        }
        return pedidos;
    }

    @Transactional
    public DecisionResult responder(String email, Long conexaoId, boolean firmar) {
        Usuario usuario = usuarioAprovado(email, Papel.STARTUP);
        Conexao conexao = conexoes.findById(conexaoId)
                .filter(c -> c.getStartup().getId().equals(usuario.getId()))
                .filter(c -> c.getStatus() != StatusConexao.DISPENSADA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
        if (conexao.getStatus() != StatusConexao.SOLICITADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido já foi respondido");
        }
        conexao.responder(firmar ? StatusConexao.FIRMADA : StatusConexao.NEGADA);
        conexoes.save(conexao);
        return new DecisionResult(conexao.getId(), conexao.getStatus().name());
    }

    private static String resumoDoInvestidor(PreferenciasInvestidor pref) {
        StringBuilder texto = new StringBuilder("O investidor se interessa por ")
                .append(String.join(", ", pref.segmentos()));
        if (!pref.estagios().isEmpty()) {
            texto.append(" e prefere startups em ").append(String.join(", ", pref.estagios()));
        }
        texto.append(".");
        if (!pref.ticket().vazia()) {
            texto.append(" A faixa de ticket dele cobre o valor que a sua startup busca.");
        }
        return texto.toString();
    }

    static String nomeDe(InvestidorMentor perfil, Usuario usuario) {
        String nome = Formatos.texto(perfil.getName());
        return nome.isEmpty() ? usuario.getEmail() : nome;
    }

    static String papelDe(InvestidorMentor perfil) {
        return perfil.getTipo() == TipoInvestidorMentor.MENTOR ? "Mentor(a)" : "Investidor(a) anjo";
    }

    static String descricaoDe(InvestidorMentor perfil) {
        for (String candidato : new String[]{perfil.getLookingFor(), perfil.getAboutInvestor(), perfil.getCareer()}) {
            if (candidato != null && !candidato.isBlank()) {
                return Formatos.cortar(candidato, 400);
            }
        }
        return "";
    }

    static String local(String cidade, String uf) {
        String c = Formatos.texto(cidade);
        String u = Formatos.texto(uf);
        return c + (!c.isEmpty() && !u.isEmpty() ? "/" : "") + u;
    }

    /* ================================================================== contrato legado */

    /**
     * Contrato antigo: investidores ordenados por afinidade com uma startup.
     * Agora só a própria startup (ou um administrador) pode consultar, e só entram investidores aprovados.
     */
    @Transactional(readOnly = true)
    public List<MatchResponseDTO> encontrarMatches(Long startupId, String email, boolean administrador) {
        Startup startup = startups.findById(startupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Startup não encontrada"));
        if (!administrador) {
            Usuario usuario = usuarioAprovado(email, Papel.STARTUP);
            if (usuario.getStartup() == null || !usuario.getStartup().getId().equals(startupId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido");
            }
        }
        Faixa ticketStartup = new Faixa(startup.getMinimumTicket(), startup.getMaximumTicket());
        List<MatchResponseDTO> resultados = new ArrayList<>();
        for (Usuario usuario : usuarios.findByPapelAndStatus(Papel.INVESTIDOR, StatusUsuario.APROVADO)) {
            InvestidorMentor investidor = usuario.getInvestidorMentor();
            if (investidor == null) {
                continue;
            }
            PreferenciasInvestidor pref = PreferenciasInvestidor.de(investidor);
            int pontuacao = 0;
            if (Criterios.segmentoCompativel(startup.getSegmento(), pref.segmentos())) {
                pontuacao += 40;
            }
            if (!pref.estagios().isEmpty() && Criterios.estagioCompativel(startup.getEstagio(), pref.estagios())) {
                pontuacao += 30;
            }
            if (!pref.ticket().vazia() && Criterios.ticketCompativel(pref.ticket(), ticketStartup)) {
                pontuacao += 30;
            }
            resultados.add(new MatchResponseDTO(
                    startup.getId(), startup.getNome(), investidor.getId(),
                    investidor.getTipo() == null ? null : investidor.getTipo().name(),
                    investidor.getAreasInteresse(), investidor.getEstagiosPref(), investidor.getDisponibilidade(),
                    pontuacao, pontuacao >= 70 ? "ALTA" : pontuacao >= 40 ? "MEDIA" : "BAIXA"));
        }
        resultados.sort((a, b) -> Integer.compare(b.getPontuacao(), a.getPontuacao()));
        return resultados;
    }

    /* ========================================================================= apoio */

    private Contexto investidor(String email) {
        Usuario usuario = usuarioAprovado(email, Papel.INVESTIDOR);
        InvestidorMentor perfil = usuario.getInvestidorMentor();
        if (perfil == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Complete o seu perfil de investidor");
        }
        return new Contexto(usuario, perfil, PreferenciasInvestidor.de(perfil));
    }

    Usuario usuarioAprovado(String email, Papel papel) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email == null ? "" : email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido"));
        if (usuario.getPapel() != papel || usuario.getStatus() != StatusUsuario.APROVADO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido");
        }
        return usuario;
    }
}
