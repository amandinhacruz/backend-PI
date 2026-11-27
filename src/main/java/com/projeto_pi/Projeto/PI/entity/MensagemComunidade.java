package com.projeto_pi.Projeto.PI.entity;

import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Mensagem enviada ao grupo de uma comunidade. O texto fica criptografado (RNF02). */
@Entity
@Table(name = "comunidade_mensagens",
        indexes = @Index(name = "idx_comunidade_mensagens_comunidade", columnList = "comunidade_id, id"))
public class MensagemComunidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "comunidade_id", nullable = false)
    private Comunidade comunidade;

    @ManyToOne(optional = false)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaPostagem categoria;

    @Convert(converter = StringCriptografadoConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "enviada_em", nullable = false)
    private LocalDateTime enviadaEm;

    protected MensagemComunidade() {
    }

    public MensagemComunidade(Comunidade comunidade, Usuario remetente, CategoriaPostagem categoria, String conteudo) {
        this.comunidade = comunidade;
        this.remetente = remetente;
        this.categoria = categoria;
        this.conteudo = conteudo;
        this.enviadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Comunidade getComunidade() { return comunidade; }
    public Usuario getRemetente() { return remetente; }
    public CategoriaPostagem getCategoria() { return categoria; }
    public String getConteudo() { return conteudo; }
    public LocalDateTime getEnviadaEm() { return enviadaEm; }
}
