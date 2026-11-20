package com.projeto_pi.Projeto.PI.entity;

import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Análise de afinidade produzida pelo analista generativo para um par investidor x startup.
 *
 * Fica guardada para que a IA só seja chamada de novo quando um dos perfis mudar
 * (o {@code hash} cobre os textos enviados, a versão do prompt e o modelo). Os textos
 * derivam de dados de negócio e por isso são criptografados em repouso (RNF02).
 */
@Entity
@Table(name = "analises_afinidade",
        uniqueConstraints = @UniqueConstraint(columnNames = {"investidor_perfil_id", "startup_perfil_id"}))
public class AnaliseAfinidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investidor_perfil_id", nullable = false)
    private Long investidorPerfilId;

    @Column(name = "startup_perfil_id", nullable = false)
    private Long startupPerfilId;

    @Column(nullable = false, length = 64)
    private String hash;

    @Column(nullable = false)
    private int nota;

    @Column(name = "aderencia_tese", nullable = false)
    private int aderenciaTese;

    @Column(name = "problema_mercado", nullable = false)
    private int problemaMercado;

    @Column(nullable = false)
    private int diferenciais;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String justificativa;

    /** Itens separados por quebra de linha. */
    @Column(name = "pontos_fortes", columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String pontosFortes;

    @Column(name = "pontos_atencao", columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String pontosAtencao;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String perguntas;

    @Column(length = 80)
    private String modelo;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected AnaliseAfinidade() {
    }

    public AnaliseAfinidade(Long investidorPerfilId, Long startupPerfilId) {
        this.investidorPerfilId = investidorPerfilId;
        this.startupPerfilId = startupPerfilId;
    }

    public void registrar(String hash, int nota, int aderenciaTese, int problemaMercado, int diferenciais,
                          String justificativa, String pontosFortes, String pontosAtencao, String perguntas,
                          String modelo) {
        this.hash = hash;
        this.nota = nota;
        this.aderenciaTese = aderenciaTese;
        this.problemaMercado = problemaMercado;
        this.diferenciais = diferenciais;
        this.justificativa = justificativa;
        this.pontosFortes = pontosFortes;
        this.pontosAtencao = pontosAtencao;
        this.perguntas = perguntas;
        this.modelo = modelo;
        this.criadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getInvestidorPerfilId() { return investidorPerfilId; }
    public Long getStartupPerfilId() { return startupPerfilId; }
    public String getHash() { return hash; }
    public int getNota() { return nota; }
    public int getAderenciaTese() { return aderenciaTese; }
    public int getProblemaMercado() { return problemaMercado; }
    public int getDiferenciais() { return diferenciais; }
    public String getJustificativa() { return justificativa; }
    public String getPontosFortes() { return pontosFortes; }
    public String getPontosAtencao() { return pontosAtencao; }
    public String getPerguntas() { return perguntas; }
    public String getModelo() { return modelo; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
