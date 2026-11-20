package com.projeto_pi.Projeto.PI.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.Convert;

@Entity
@Table(name = "mensagens")
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;

    @ManyToOne
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Convert(converter = StringCriptografadoConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(nullable = false)
    private LocalDateTime dataEnvio;

    public Mensagem() {
    }

    public Long getId() {
        return id;
    }

    public Usuario getRemetente() {
        return remetente;
    }

    public void setRemetente(Usuario remetente) {
        this.remetente = remetente;
    }

    public Usuario getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Usuario destinatario) {
        this.destinatario = destinatario;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDateTime dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    /** Conversa (conexão firmada) a que a mensagem pertence; nula em mensagens antigas. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conexao_id")
    private Conexao conexao;
    public Conexao getConexao() { return conexao; }
    public void setConexao(Conexao value) { this.conexao = value; }

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoMensagem tipo;
    public TipoMensagem getTipo() { return tipo == null ? TipoMensagem.TEXTO : tipo; }
    public void setTipo(TipoMensagem value) { this.tipo = value; }

    /** Reunião ligada a uma mensagem do tipo CONVITE ou SISTEMA. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;
    public Agendamento getAgendamento() { return agendamento; }
    public void setAgendamento(Agendamento value) { this.agendamento = value; }

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;
    public LocalDateTime getLidaEm() { return lidaEm; }
    public void setLidaEm(LocalDateTime value) { this.lidaEm = value; }
}
