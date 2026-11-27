package com.projeto_pi.Projeto.PI.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Comunidade temática aberta a startups e investidores aprovados (conversa em grupo). */
@Entity
@Table(name = "comunidades")
public class Comunidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador estável (o front escolhe a imagem da comunidade por ele). */
    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    /** Posição na lista. */
    @Column(nullable = false)
    private int ordem;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected Comunidade() {
    }

    public Comunidade(String slug, String nome, String descricao, int ordem) {
        this.slug = slug;
        this.nome = nome;
        this.descricao = descricao;
        this.ordem = ordem;
        this.criadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public int getOrdem() { return ordem; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
