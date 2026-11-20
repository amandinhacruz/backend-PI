package com.projeto_pi.Projeto.PI.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.Convert;

@Entity
@Table(name = "agendamentos")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime horario;

    @Convert(converter = StringCriptografadoConverter.class)
    @Column(nullable = false, length = 800)
    private String assunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAgendamento status;

    public Agendamento() {
    }

    public Long getId() {
        return id;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Usuario solicitante) {
        this.solicitante = solicitante;
    }

    public Usuario getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Usuario destinatario) {
        this.destinatario = destinatario;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public void setHorario(LocalTime horario) {
        this.horario = horario;
    }

    public String getAssunto() {
        return assunto;
    }

    public void setAssunto(String assunto) {
        this.assunto = assunto;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    public void setStatus(StatusAgendamento status) {
        this.status = status;
    }

    /** Conexão firmada em que o convite foi feito; nula em agendamentos antigos. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conexao_id")
    private Conexao conexao;
    public Conexao getConexao() { return conexao; }
    public void setConexao(Conexao value) { this.conexao = value; }

    @Column(name = "duracao_minutos")
    private Integer duracaoMinutos;
    public int getDuracaoMinutos() { return duracaoMinutos == null ? 60 : duracaoMinutos; }
    public void setDuracaoMinutos(Integer value) { this.duracaoMinutos = value; }

    /** Link da videochamada ou endereço do encontro (opcional). */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String local;
    public String getLocal() { return local; }
    public void setLocal(String value) { this.local = value; }

    @Column(name = "criado_em")
    private java.time.LocalDateTime criadoEm;
    public java.time.LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(java.time.LocalDateTime value) { this.criadoEm = value; }

    @Column(name = "respondido_em")
    private java.time.LocalDateTime respondidoEm;
    public java.time.LocalDateTime getRespondidoEm() { return respondidoEm; }
    public void setRespondidoEm(java.time.LocalDateTime value) { this.respondidoEm = value; }
}
