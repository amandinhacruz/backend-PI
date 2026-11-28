package com.projeto_pi.Projeto.PI.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Curtida de um usuário numa publicação (no máximo uma por pessoa). */
@Entity
@Table(name = "publicacao_curtidas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"publicacao_id", "usuario_id"}))
public class CurtidaPublicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacao_id", nullable = false)
    private Publicacao publicacao;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected CurtidaPublicacao() {
    }

    public CurtidaPublicacao(Publicacao publicacao, Usuario usuario) {
        this.publicacao = publicacao;
        this.usuario = usuario;
        this.criadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
}
