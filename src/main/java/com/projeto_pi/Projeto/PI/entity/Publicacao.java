package com.projeto_pi.Projeto.PI.entity;

import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Publicação de texto na timeline. Um compartilhamento é uma publicação que aponta para outra:
 * sem texto é um compartilhamento simples; com texto, um compartilhamento com comentário.
 * Excluir apenas marca como removida, para os compartilhamentos mostrarem "publicação indisponível".
 */
@Entity
@Table(name = "publicacoes", indexes = @Index(name = "idx_publicacoes_compartilhada", columnList = "compartilhada_id"))
public class Publicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    /** Nulo num compartilhamento simples. Criptografado (RNF02). */
    @Convert(converter = StringCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String conteudo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compartilhada_id")
    private Publicacao compartilhada;

    @Column(nullable = false)
    private boolean removida;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected Publicacao() {
    }

    public Publicacao(Usuario autor, String conteudo, Publicacao compartilhada) {
        this.autor = autor;
        this.conteudo = conteudo;
        this.compartilhada = compartilhada;
        this.criadaEm = LocalDateTime.now();
    }

    /** Compartilhamento sem texto próprio. */
    public boolean compartilhamentoSimples() {
        return compartilhada != null && (conteudo == null || conteudo.isBlank());
    }

    public Long getId() { return id; }
    public Usuario getAutor() { return autor; }
    public String getConteudo() { return conteudo; }
    public Publicacao getCompartilhada() { return compartilhada; }
    public boolean isRemovida() { return removida; }
    public void remover() { this.removida = true; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
