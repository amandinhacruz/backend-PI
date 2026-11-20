package com.projeto_pi.Projeto.PI.entity;

import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * RF10: avaliação pós-interação. Cada participante de uma conexão firmada avalia o outro
 * com nota de 1 a 5 e um comentário opcional (uma avaliação por autor e conexão).
 */
@Entity
@Table(name = "avaliacoes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conexao_id", "autor_id"}))
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "conexao_id", nullable = false)
    private Conexao conexao;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliado_id", nullable = false)
    private Usuario avaliado;

    @Column(nullable = false)
    private int nota;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String comentario;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em")
    private LocalDateTime atualizadaEm;

    protected Avaliacao() {
    }

    public Avaliacao(Conexao conexao, Usuario autor, Usuario avaliado) {
        this.conexao = conexao;
        this.autor = autor;
        this.avaliado = avaliado;
        this.criadaEm = LocalDateTime.now();
    }

    public void registrar(int nota, String comentario) {
        this.nota = nota;
        this.comentario = comentario;
        this.atualizadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Conexao getConexao() { return conexao; }
    public Usuario getAutor() { return autor; }
    public Usuario getAvaliado() { return avaliado; }
    public int getNota() { return nota; }
    public String getComentario() { return comentario; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
