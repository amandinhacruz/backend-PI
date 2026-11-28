package com.projeto_pi.Projeto.PI.entity;

import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Comentário numa publicação da timeline. Texto criptografado (RNF02). */
@Entity
@Table(name = "publicacao_comentarios",
        indexes = @Index(name = "idx_publicacao_comentarios_publicacao", columnList = "publicacao_id, id"))
public class ComentarioPublicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacao_id", nullable = false)
    private Publicacao publicacao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Convert(converter = StringCriptografadoConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected ComentarioPublicacao() {
    }

    public ComentarioPublicacao(Publicacao publicacao, Usuario autor, String conteudo) {
        this.publicacao = publicacao;
        this.autor = autor;
        this.conteudo = conteudo;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Publicacao getPublicacao() { return publicacao; }
    public Usuario getAutor() { return autor; }
    public String getConteudo() { return conteudo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
