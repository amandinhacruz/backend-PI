package com.projeto_pi.Projeto.PI.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto_pi.Projeto.PI.dto.CrescimentoDTOs.Dashboard;
import com.projeto_pi.Projeto.PI.dto.CrescimentoDTOs.MonthlyUpdate;
import com.projeto_pi.Projeto.PI.dto.CrescimentoDTOs.Settings;
import com.projeto_pi.Projeto.PI.dto.CrescimentoDTOs.SettingsRequest;
import com.projeto_pi.Projeto.PI.dto.CrescimentoDTOs.UpdateRequest;
import com.projeto_pi.Projeto.PI.entity.AtualizacaoMensal;
import com.projeto_pi.Projeto.PI.entity.ConfiguracaoCrescimento;
import com.projeto_pi.Projeto.PI.entity.CriterioReceita;
import com.projeto_pi.Projeto.PI.entity.ModoCrescimento;
import com.projeto_pi.Projeto.PI.entity.Papel;
import com.projeto_pi.Projeto.PI.entity.StatusMarco;
import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.matching.StartupResumo;
import com.projeto_pi.Projeto.PI.repository.AtualizacaoMensalRepository;
import com.projeto_pi.Projeto.PI.repository.ConfiguracaoCrescimentoRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Aba "Crescimento": a startup configura como acompanha o crescimento e envia uma atualização
 * por mês; investidores aprovados acompanham pelo perfil da startup.
 *
 * Os números são declarados pela startup e não são verificados pela plataforma. O serviço só
 * valida coerência básica (valores não negativos, campos obrigatórios por modo) e calcula
 * resultado operacional, queima de caixa e runway, que ficam gravados com a atualização.
 */
@Service
public class CrescimentoService {

    static final Set<String> MODELOS = Set.of("B2B", "B2C", "B2B2C", "MARKETPLACE", "OUTRO");
    static final int MESES_PARA_TRAS = 24;
    static final int TEXTO_MAXIMO = 1000;
    static final BigDecimal VALOR_MAXIMO = new BigDecimal("1000000000000"); // 1 trilhão
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final TypeReference<LinkedHashMap<String, BigDecimal>> MAPA = new TypeReference<>() { };

    private final UsuarioRepository usuarios;
    private final ConfiguracaoCrescimentoRepository configuracoes;
    private final AtualizacaoMensalRepository atualizacoes;
    private final ObjectMapper json = new ObjectMapper();

    public CrescimentoService(UsuarioRepository usuarios, ConfiguracaoCrescimentoRepository configuracoes,
                              AtualizacaoMensalRepository atualizacoes) {
        this.usuarios = usuarios;
        this.configuracoes = configuracoes;
        this.atualizacoes = atualizacoes;
    }

    /* ============================================================ leitura */

    /** Painel da própria startup (com o que falta enviar). */
    @Transactional(readOnly = true)
    public Dashboard meu(String email) {
        Usuario startup = startupAprovada(email);
        return painel(startup, true);
    }

    /** Painel de uma startup aprovada, visto por investidores (ou pela própria startup / admin). */
    @Transactional(readOnly = true)
    public Dashboard daStartup(String email, Long startupPerfilId) {
        Usuario leitor = usuarios.findByEmailIgnoreCase(email == null ? "" : email)
                .orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Acesso não permitido"));
        StartupResumo resumo = usuarios.findStartupAprovada(startupPerfilId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Startup não encontrada"));
        boolean propria = resumo.usuarioId().equals(leitor.getId());
        boolean investidor = leitor.getPapel() == Papel.INVESTIDOR && leitor.getStatus() == StatusUsuario.APROVADO;
        if (!propria && !investidor && leitor.getPapel() != Papel.ADMIN) {
            throw erro(HttpStatus.FORBIDDEN, "Só investidores acompanham o crescimento de outras startups");
        }
        return painel(usuarios.findById(resumo.usuarioId()).orElseThrow(), propria);
    }

    /* ============================================================ escrita */

    @Transactional
    public Dashboard configurar(String email, SettingsRequest pedido) {
        Usuario startup = startupAprovada(email);
        if (pedido == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe a configuração");
        }
        ModoCrescimento modo = enumerado(ModoCrescimento.class, pedido.mode(), "Informe se a startup já tem faturamento");
        ConfiguracaoCrescimento config = configuracoes.findByStartup_Id(startup.getId())
                .orElseGet(() -> new ConfiguracaoCrescimento(startup));
        config.setModo(modo);
        if (modo == ModoCrescimento.COM_FATURAMENTO) {
            config.setCriterioReceita(enumerado(CriterioReceita.class, pedido.revenueCriterion(),
                    "Escolha o critério de receita: faturada ou recebida"));
        } else {
            String modelo = pedido.businessModel() == null ? "" : pedido.businessModel().strip().toUpperCase(Locale.ROOT);
            if (!MODELOS.contains(modelo)) {
                throw erro(HttpStatus.BAD_REQUEST, "Informe o modelo de negócio: B2B, B2C, B2B2C, MARKETPLACE ou OUTRO");
            }
            config.setModeloNegocio(modelo);
            config.definirIndicadores(indicadores(pedido.indicators()));
            String marco = pedido.milestone() == null ? "" : pedido.milestone().strip();
            if (marco.length() < 3 || marco.length() > 120) {
                throw erro(HttpStatus.BAD_REQUEST, "Informe o marco que será acompanhado (de 3 a 120 caracteres)");
            }
            config.setMarco(marco);
            config.setPrazoMarco(pedido.milestoneDeadline() == null || pedido.milestoneDeadline().isBlank()
                    ? null : mes(pedido.milestoneDeadline(), "Prazo do marco inválido: use o formato AAAA-MM").atDay(1));
        }
        config.tocar();
        configuracoes.save(config);
        return painel(startup, true);
    }

    /** Cria ou corrige a atualização de um mês ("yyyy-MM"). */
    @Transactional
    public Dashboard atualizar(String email, String mesTexto, UpdateRequest pedido) {
        Usuario startup = startupAprovada(email);
        ConfiguracaoCrescimento config = configuracoes.findByStartup_Id(startup.getId())
                .orElseThrow(() -> erro(HttpStatus.CONFLICT, "Configure a aba Crescimento antes de enviar atualizações"));
        YearMonth mes = mes(mesTexto, "Mês inválido: use o formato AAAA-MM");
        YearMonth agora = YearMonth.now();
        if (mes.isAfter(agora)) {
            throw erro(HttpStatus.BAD_REQUEST, "Não é possível enviar a atualização de um mês futuro");
        }
        if (mes.isBefore(agora.minusMonths(MESES_PARA_TRAS))) {
            throw erro(HttpStatus.BAD_REQUEST, "Envie atualizações dos últimos " + MESES_PARA_TRAS + " meses");
        }
        if (pedido == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe os dados do mês");
        }

        AtualizacaoMensal registro = atualizacoes.findByStartup_IdAndMes(startup.getId(), mes.atDay(1))
                .orElseGet(() -> new AtualizacaoMensal(startup, mes.atDay(1)));
        registro.tocar();
        registro.setModo(config.getModo());
        BigDecimal caixa = valor(pedido.cash(), "o caixa disponível", true);
        registro.setCaixa(caixa);

        BigDecimal queima;
        if (config.getModo() == ModoCrescimento.COM_FATURAMENTO) {
            BigDecimal receita = valor(pedido.revenue(), "a receita do mês", true);
            BigDecimal custos = valor(pedido.directCosts(), "os custos diretos", true);
            BigDecimal despesas = valor(pedido.operatingExpenses(), "as despesas operacionais", true);
            BigDecimal resultado = receita.subtract(custos).subtract(despesas);
            registro.setCriterioReceita(config.getCriterioReceita());
            registro.setReceita(receita);
            registro.setCustosDiretos(custos);
            registro.setDespesasOperacionais(despesas);
            registro.setGastos(null);
            registro.setResultadoOperacional(resultado);
            registro.setIndicadores(null);
            registro.setMarco(null);
            registro.setStatusMarco(null);
            registro.setProgressoMarco(null);
            queima = resultado.signum() < 0 ? resultado.negate() : BigDecimal.ZERO;
        } else {
            BigDecimal gastos = valor(pedido.expenses(), "os gastos do mês", true);
            registro.setCriterioReceita(null);
            registro.setReceita(null);
            registro.setCustosDiretos(null);
            registro.setDespesasOperacionais(null);
            registro.setGastos(gastos);
            registro.setResultadoOperacional(gastos.negate());
            registro.setIndicadores(valoresIndicadores(config.listaIndicadores(), pedido.indicators()));
            StatusMarco status = enumerado(StatusMarco.class, pedido.milestoneStatus(), "Informe o andamento do marco");
            int progresso = switch (status) {
                case NAO_INICIADO -> 0;
                case CONCLUIDO -> 100;
                case EM_ANDAMENTO -> {
                    Integer p = pedido.milestoneProgress();
                    if (p == null || p < 0 || p > 100) {
                        throw erro(HttpStatus.BAD_REQUEST, "Informe o progresso do marco, de 0 a 100%");
                    }
                    yield p;
                }
            };
            registro.setMarco(config.getMarco());
            registro.setStatusMarco(status);
            registro.setProgressoMarco(progresso);
            queima = gastos;
        }
        registro.setQueimaMensal(queima);
        registro.setRunwayMeses(queima.signum() > 0 ? caixa.divide(queima, 1, RoundingMode.HALF_UP) : null);

        boolean captacao = Boolean.TRUE.equals(pedido.newFunding());
        registro.setNovaCaptacao(captacao);
        registro.setValorCaptacao(captacao ? positivo(pedido.fundingAmount(), "o valor captado") : null);
        boolean divida = Boolean.TRUE.equals(pedido.newDebt());
        registro.setNovaDivida(divida);
        registro.setValorDivida(divida ? positivo(pedido.debtAmount(), "o valor da dívida") : null);

        String comentario = pedido.comment() == null || pedido.comment().isBlank() ? null : pedido.comment().strip();
        if (comentario != null && comentario.length() > TEXTO_MAXIMO) {
            throw erro(HttpStatus.BAD_REQUEST, "O comentário deve ter no máximo " + TEXTO_MAXIMO + " caracteres");
        }
        registro.setComentario(comentario);
        atualizacoes.save(registro);
        return painel(startup, true);
    }

    /* ============================================================ montagem */

    private Dashboard painel(Usuario startup, boolean dono) {
        ConfiguracaoCrescimento config = configuracoes.findByStartup_Id(startup.getId()).orElse(null);
        List<AtualizacaoMensal> lista = atualizacoes.findByStartup_IdOrderByMesAsc(startup.getId());
        List<MonthlyUpdate> meses = new ArrayList<>();
        AtualizacaoMensal anterior = null;
        for (AtualizacaoMensal a : lista) {
            meses.add(mensal(a, anterior));
            anterior = a;
        }
        String proximo = null;
        List<String> pendentes = List.of();
        if (dono && config != null) {
            pendentes = pendentes(lista);
            proximo = pendentes.isEmpty() ? null : pendentes.get(0);
        }
        Long perfilId = startup.getStartup() == null ? null : startup.getStartup().getId();
        return new Dashboard(perfilId, ConversaService.nome(startup), dono,
                config == null ? null : configuracao(config), meses, proximo, pendentes);
    }

    /** Meses fechados ainda sem atualização: depois do último enviado até o mês passado (no máximo 12). */
    static List<String> pendentes(List<AtualizacaoMensal> lista) {
        YearMonth passado = YearMonth.now().minusMonths(1);
        YearMonth inicio = lista.isEmpty() ? passado : YearMonth.from(lista.get(lista.size() - 1).getMes()).plusMonths(1);
        if (inicio.isBefore(passado.minusMonths(11))) {
            inicio = passado.minusMonths(11);
        }
        List<String> meses = new ArrayList<>();
        for (YearMonth m = inicio; !m.isAfter(passado); m = m.plusMonths(1)) {
            meses.add(m.toString());
        }
        return meses;
    }

    private Settings configuracao(ConfiguracaoCrescimento c) {
        return new Settings(c.getModo().name(), c.getCriterioReceita() == null ? null : c.getCriterioReceita().name(),
                c.getModeloNegocio(), c.listaIndicadores(), c.getMarco(),
                c.getPrazoMarco() == null ? null : YearMonth.from(c.getPrazoMarco()).toString(), c.getAtualizadaEm());
    }

    private MonthlyUpdate mensal(AtualizacaoMensal a, AtualizacaoMensal anterior) {
        BigDecimal lucroBruto = null;
        BigDecimal margem = null;
        BigDecimal crescimento = null;
        if (a.getModo() == ModoCrescimento.COM_FATURAMENTO && a.getReceita() != null) {
            lucroBruto = a.getReceita().subtract(a.getCustosDiretos());
            if (a.getReceita().signum() > 0) {
                margem = lucroBruto.multiply(CEM).divide(a.getReceita(), 1, RoundingMode.HALF_UP);
            }
            boolean mesAnterior = anterior != null && anterior.getModo() == ModoCrescimento.COM_FATURAMENTO
                    && anterior.getReceita() != null && anterior.getReceita().signum() > 0
                    && YearMonth.from(anterior.getMes()).plusMonths(1).equals(YearMonth.from(a.getMes()));
            if (mesAnterior) {
                crescimento = a.getReceita().subtract(anterior.getReceita()).multiply(CEM)
                        .divide(anterior.getReceita(), 1, RoundingMode.HALF_UP);
            }
        }
        return new MonthlyUpdate(YearMonth.from(a.getMes()).toString(), a.getModo().name(),
                a.getCriterioReceita() == null ? null : a.getCriterioReceita().name(),
                a.getReceita(), a.getCustosDiretos(), a.getDespesasOperacionais(), lucroBruto, margem, crescimento,
                a.getGastos(), a.getResultadoOperacional(), a.getQueimaMensal(), a.getCaixa(), a.getRunwayMeses(),
                a.isNovaCaptacao(), a.getValorCaptacao(), a.isNovaDivida(), a.getValorDivida(),
                lerIndicadores(a.getIndicadores()), a.getMarco(),
                a.getStatusMarco() == null ? null : a.getStatusMarco().name(), a.getProgressoMarco(),
                a.getComentario(), a.getEnviadaEm(),
                a.getAtualizadaEm().isAfter(a.getEnviadaEm().plusSeconds(1)) ? a.getAtualizadaEm() : null);
    }

    /* ============================================================ validação */

    private static List<String> indicadores(List<String> nomes) {
        List<String> limpos = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (String nome : nomes == null ? List.<String>of() : nomes) {
            String limpo = nome == null ? "" : nome.strip().replaceAll("\\s+", " ");
            if (limpo.isEmpty()) {
                continue;
            }
            if (limpo.length() > 60) {
                throw erro(HttpStatus.BAD_REQUEST, "Cada indicador deve ter no máximo 60 caracteres");
            }
            if (!vistos.add(limpo.toLowerCase(Locale.ROOT))) {
                throw erro(HttpStatus.BAD_REQUEST, "Indicador repetido: " + limpo);
            }
            limpos.add(limpo);
        }
        if (limpos.size() < 2 || limpos.size() > 3) {
            throw erro(HttpStatus.BAD_REQUEST, "Escolha de dois a três indicadores de tração");
        }
        return limpos;
    }

    private String valoresIndicadores(List<String> configurados, Map<String, BigDecimal> enviados) {
        Map<String, BigDecimal> valores = new LinkedHashMap<>();
        Map<String, BigDecimal> recebidos = enviados == null ? Map.of() : enviados;
        for (String nome : configurados) {
            valores.put(nome, valor(recebidos.get(nome), "o valor de \"" + nome + "\"", true));
        }
        for (String nome : recebidos.keySet()) {
            if (!valores.containsKey(nome)) {
                throw erro(HttpStatus.BAD_REQUEST, "Indicador não configurado: " + nome);
            }
        }
        try {
            return json.writeValueAsString(valores);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gravar indicadores", e);
        }
    }

    private Map<String, BigDecimal> lerIndicadores(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return json.readValue(texto, MAPA);
        } catch (Exception e) {
            throw new IllegalStateException("Indicadores gravados em formato inválido", e);
        }
    }

    private static BigDecimal valor(BigDecimal valor, String campo, boolean obrigatorio) {
        if (valor == null) {
            if (obrigatorio) {
                throw erro(HttpStatus.BAD_REQUEST, "Informe " + campo);
            }
            return null;
        }
        if (valor.signum() < 0) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe " + campo + " sem valor negativo");
        }
        if (valor.compareTo(VALOR_MAXIMO) > 0) {
            throw erro(HttpStatus.BAD_REQUEST, "Valor muito alto para " + campo);
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal positivo(BigDecimal valor, String campo) {
        BigDecimal v = valor(valor, campo, true);
        if (v.signum() == 0) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe " + campo + " (maior que zero)");
        }
        return v;
    }

    private static YearMonth mes(String texto, String mensagem) {
        try {
            return YearMonth.parse(texto == null ? "" : texto.strip());
        } catch (DateTimeParseException e) {
            throw erro(HttpStatus.BAD_REQUEST, mensagem);
        }
    }

    private static <E extends Enum<E>> E enumerado(Class<E> tipo, String valor, String mensagem) {
        try {
            return Enum.valueOf(tipo, valor == null ? "" : valor.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw erro(HttpStatus.BAD_REQUEST, mensagem);
        }
    }

    private Usuario startupAprovada(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email == null ? "" : email)
                .orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Acesso não permitido"));
        if (usuario.getPapel() != Papel.STARTUP || usuario.getStatus() != StatusUsuario.APROVADO) {
            throw erro(HttpStatus.FORBIDDEN, "Acesso não permitido");
        }
        return usuario;
    }

    private static ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
