package com.projeto_pi.Projeto.PI.entity;

import com.projeto_pi.Projeto.PI.config.BigDecimalCriptografadoConverter;
import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Atualização mensal de crescimento enviada pela startup. Os valores informados e os indicadores
 * calculados (resultado operacional, queima e runway) ficam gravados, criptografados (RNF02).
 * O modo e o critério de receita são uma fotografia da configuração no momento do envio.
 */
@Entity
@Table(name = "crescimento_atualizacoes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"startup_usuario_id", "mes"}))
public class AtualizacaoMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_usuario_id", nullable = false)
    private Usuario startup;

    /** Primeiro dia do mês de referência. */
    @Column(nullable = false)
    private LocalDate mes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModoCrescimento modo;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterio_receita", length = 20)
    private CriterioReceita criterioReceita;

    @Convert(converter = BigDecimalCriptografadoConverter.class) @Column(columnDefinition = "TEXT")
    private BigDecimal receita;
    @Convert(converter = BigDecimalCriptografadoConverter.class) @Column(name = "custos_diretos", columnDefinition = "TEXT")
    private BigDecimal custosDiretos;
    @Convert(converter = BigDecimalCriptografadoConverter.class) @Column(name = "despesas_operacionais", columnDefinition = "TEXT")
    private BigDecimal despesasOperacionais;
    /** Gastos totais do mês (startup sem faturamento). */
    @Convert(converter = BigDecimalCriptografadoConverter.class) @Column(columnDefinition = "TEXT")
    private BigDecimal gastos;
    @Convert(converter = BigDecimalCriptografadoConverter.class) @Column(columnDefinition = "TEXT")
    private BigDecimal caixa;

    @Column(name = "nova_captacao", nullable = false)
    private boolean novaCaptacao;
    @Convert(converter = BigDecimalCriptografadoConverter.class) @Column(name = "valor_captacao", columnDefinition = "TEXT")
    private BigDecimal valorCaptacao;
    @Column(name = "nova_divida", nullable = false)
    private boolean novaDivida;
    @Convert(converter = BigDecimalCriptografadoConverter.class) @Column(name = "valor_divida", columnDefinition = "TEXT")
    private BigDecimal valorDivida;

    /* -------- indicadores calculados no envio -------- */
    @Convert(converter = BigDecimalCriptografadoConverter.class) @Column(name = "resultado_operacional", columnDefinition = "TEXT")
    private BigDecimal resultadoOperacional;
    @Convert(converter = BigDecimalCriptografadoConverter.class) @Column(name = "queima_mensal", columnDefinition = "TEXT")
    private BigDecimal queimaMensal;
    /** Meses de caixa no ritmo de queima atual; nulo quando não há queima. */
    @Convert(converter = BigDecimalCriptografadoConverter.class) @Column(name = "runway_meses", columnDefinition = "TEXT")
    private BigDecimal runwayMeses;

    /** Valores dos indicadores de tração, em JSON ({"Pilotos ativos": 3}). */
    @Convert(converter = StringCriptografadoConverter.class) @Column(columnDefinition = "TEXT")
    private String indicadores;

    @Column(length = 120)
    private String marco;
    @Enumerated(EnumType.STRING) @Column(name = "status_marco", length = 20)
    private StatusMarco statusMarco;
    @Column(name = "progresso_marco")
    private Integer progressoMarco;

    @Convert(converter = StringCriptografadoConverter.class) @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "enviada_em", nullable = false)
    private LocalDateTime enviadaEm;
    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    protected AtualizacaoMensal() {
    }

    public AtualizacaoMensal(Usuario startup, LocalDate mes) {
        this.startup = startup;
        this.mes = mes;
        this.enviadaEm = LocalDateTime.now();
        this.atualizadaEm = enviadaEm;
    }

    public Long getId() { return id; }
    public Usuario getStartup() { return startup; }
    public LocalDate getMes() { return mes; }
    public ModoCrescimento getModo() { return modo; }
    public void setModo(ModoCrescimento v) { modo = v; }
    public CriterioReceita getCriterioReceita() { return criterioReceita; }
    public void setCriterioReceita(CriterioReceita v) { criterioReceita = v; }
    public BigDecimal getReceita() { return receita; }
    public void setReceita(BigDecimal v) { receita = v; }
    public BigDecimal getCustosDiretos() { return custosDiretos; }
    public void setCustosDiretos(BigDecimal v) { custosDiretos = v; }
    public BigDecimal getDespesasOperacionais() { return despesasOperacionais; }
    public void setDespesasOperacionais(BigDecimal v) { despesasOperacionais = v; }
    public BigDecimal getGastos() { return gastos; }
    public void setGastos(BigDecimal v) { gastos = v; }
    public BigDecimal getCaixa() { return caixa; }
    public void setCaixa(BigDecimal v) { caixa = v; }
    public boolean isNovaCaptacao() { return novaCaptacao; }
    public void setNovaCaptacao(boolean v) { novaCaptacao = v; }
    public BigDecimal getValorCaptacao() { return valorCaptacao; }
    public void setValorCaptacao(BigDecimal v) { valorCaptacao = v; }
    public boolean isNovaDivida() { return novaDivida; }
    public void setNovaDivida(boolean v) { novaDivida = v; }
    public BigDecimal getValorDivida() { return valorDivida; }
    public void setValorDivida(BigDecimal v) { valorDivida = v; }
    public BigDecimal getResultadoOperacional() { return resultadoOperacional; }
    public void setResultadoOperacional(BigDecimal v) { resultadoOperacional = v; }
    public BigDecimal getQueimaMensal() { return queimaMensal; }
    public void setQueimaMensal(BigDecimal v) { queimaMensal = v; }
    public BigDecimal getRunwayMeses() { return runwayMeses; }
    public void setRunwayMeses(BigDecimal v) { runwayMeses = v; }
    public String getIndicadores() { return indicadores; }
    public void setIndicadores(String v) { indicadores = v; }
    public String getMarco() { return marco; }
    public void setMarco(String v) { marco = v; }
    public StatusMarco getStatusMarco() { return statusMarco; }
    public void setStatusMarco(StatusMarco v) { statusMarco = v; }
    public Integer getProgressoMarco() { return progressoMarco; }
    public void setProgressoMarco(Integer v) { progressoMarco = v; }
    public String getComentario() { return comentario; }
    public void setComentario(String v) { comentario = v; }
    public LocalDateTime getEnviadaEm() { return enviadaEm; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
    public void tocar() { atualizadaEm = LocalDateTime.now(); }
}
