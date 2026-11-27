package com.projeto_pi.Projeto.PI.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Configuração do painel de crescimento de uma startup (aba "Crescimento").
 * Com faturamento: o critério de receita. Sem faturamento: modelo de negócio, 2 a 3 indicadores
 * de tração e o marco acompanhado. Os campos do outro modo são mantidos ao trocar de modo.
 */
@Entity
@Table(name = "crescimento_configuracoes")
public class ConfiguracaoCrescimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_usuario_id", nullable = false, unique = true)
    private Usuario startup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModoCrescimento modo;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterio_receita", length = 20)
    private CriterioReceita criterioReceita;

    @Column(name = "modelo_negocio", length = 20)
    private String modeloNegocio;

    /** Nomes dos indicadores de tração, um por linha. */
    @Column(columnDefinition = "TEXT")
    private String indicadores;

    @Column(length = 120)
    private String marco;

    /** Primeiro dia do mês previsto para o marco (opcional). */
    @Column(name = "prazo_marco")
    private LocalDate prazoMarco;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    protected ConfiguracaoCrescimento() {
    }

    public ConfiguracaoCrescimento(Usuario startup) {
        this.startup = startup;
        this.criadaEm = LocalDateTime.now();
        this.atualizadaEm = criadaEm;
    }

    public List<String> listaIndicadores() {
        return indicadores == null || indicadores.isBlank() ? List.of() : Arrays.asList(indicadores.split("\n"));
    }

    public void definirIndicadores(List<String> nomes) {
        this.indicadores = nomes == null || nomes.isEmpty() ? null : String.join("\n", nomes);
    }

    public Long getId() { return id; }
    public Usuario getStartup() { return startup; }
    public ModoCrescimento getModo() { return modo; }
    public void setModo(ModoCrescimento value) { this.modo = value; }
    public CriterioReceita getCriterioReceita() { return criterioReceita; }
    public void setCriterioReceita(CriterioReceita value) { this.criterioReceita = value; }
    public String getModeloNegocio() { return modeloNegocio; }
    public void setModeloNegocio(String value) { this.modeloNegocio = value; }
    public String getMarco() { return marco; }
    public void setMarco(String value) { this.marco = value; }
    public LocalDate getPrazoMarco() { return prazoMarco; }
    public void setPrazoMarco(LocalDate value) { this.prazoMarco = value; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
    public void tocar() { this.atualizadaEm = LocalDateTime.now(); }
}
