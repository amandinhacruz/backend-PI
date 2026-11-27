package com.projeto_pi.Projeto.PI.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Participação de um usuário numa comunidade e até onde ele já leu. */
@Entity
@Table(name = "comunidade_membros",
        uniqueConstraints = @UniqueConstraint(columnNames = {"comunidade_id", "usuario_id"}))
public class MembroComunidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "comunidade_id", nullable = false)
    private Comunidade comunidade;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "entrou_em", nullable = false)
    private LocalDateTime entrouEm;

    /** Id da última mensagem que o membro viu; as posteriores contam como não lidas. */
    @Column(name = "ultima_lida_id")
    private Long ultimaLidaId;

    protected MembroComunidade() {
    }

    public MembroComunidade(Comunidade comunidade, Usuario usuario, Long ultimaLidaId) {
        this.comunidade = comunidade;
        this.usuario = usuario;
        this.ultimaLidaId = ultimaLidaId;
        this.entrouEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Comunidade getComunidade() { return comunidade; }
    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getEntrouEm() { return entrouEm; }
    public Long getUltimaLidaId() { return ultimaLidaId; }
    public void setUltimaLidaId(Long value) { this.ultimaLidaId = value; }
}
