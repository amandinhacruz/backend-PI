package com.projeto_pi.Projeto.PI.entity;

import com.projeto_pi.Projeto.PI.config.BigDecimalCriptografadoConverter;
import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Decisão de um investidor sobre uma startup recomendada e a resposta da startup.
 *
 * Guarda uma fotografia (segmento, estágio, UF, ticket) do perfil da startup no momento
 * da decisão. É dela que o aprendizado por investidor é calculado, sem precisar recarregar
 * o perfil completo (que inclui os arquivos de pitch).
 */
@Entity
@Table(name = "conexoes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"investidor_id", "startup_id"}))
public class Conexao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "investidor_id", nullable = false)
    private Usuario investidor;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id", nullable = false)
    private Usuario startup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConexao status;

    /** Afinidade (0 a 100) exibida ao investidor quando ele decidiu. */
    @Column(nullable = false)
    private int afinidade;

    /** "IA" quando a nota veio do analista generativo; "LOCAL" quando veio só do motor interno. */
    @Column(name = "origem_analise", length = 10)
    private String origemAnalise;

    @Column(name = "segmento_startup", columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String segmentoStartup;

    @Column(name = "estagio_startup", columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String estagioStartup;

    @Column(name = "uf_startup", columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String ufStartup;

    @Column(name = "ticket_medio_startup", columnDefinition = "TEXT")
    @Convert(converter = BigDecimalCriptografadoConverter.class)
    private BigDecimal ticketMedioStartup;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "respondida_em")
    private LocalDateTime respondidaEm;

    protected Conexao() {
    }

    public Conexao(Usuario investidor, Usuario startup, StatusConexao status, int afinidade, String origemAnalise) {
        this.investidor = investidor;
        this.startup = startup;
        this.status = status;
        this.afinidade = afinidade;
        this.origemAnalise = origemAnalise;
        this.criadaEm = LocalDateTime.now();
    }

    public void fotografarStartup(String segmento, String estagio, String uf, BigDecimal ticketMedio) {
        this.segmentoStartup = segmento;
        this.estagioStartup = estagio;
        this.ufStartup = uf;
        this.ticketMedioStartup = ticketMedio;
    }

    public void responder(StatusConexao novoStatus) {
        this.status = novoStatus;
        this.respondidaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Usuario getInvestidor() { return investidor; }
    public Usuario getStartup() { return startup; }
    public StatusConexao getStatus() { return status; }
    public int getAfinidade() { return afinidade; }
    public String getOrigemAnalise() { return origemAnalise; }
    public String getSegmentoStartup() { return segmentoStartup; }
    public String getEstagioStartup() { return estagioStartup; }
    public String getUfStartup() { return ufStartup; }
    public BigDecimal getTicketMedioStartup() { return ticketMedioStartup; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getRespondidaEm() { return respondidaEm; }
}
