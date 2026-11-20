package com.projeto_pi.Projeto.PI.entity;

import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Registro das ações de moderação feitas por administradores. */
@Entity
@Table(name = "auditoria_admin")
public class AuditoriaAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String acao;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String alvo;

    @Column(nullable = false)
    private String ator;

    @Column(nullable = false)
    private LocalDateTime data;

    protected AuditoriaAdmin() {}

    public AuditoriaAdmin(String acao, String alvo, String ator) {
        this.acao = acao;
        this.alvo = alvo;
        this.ator = ator;
        this.data = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getAcao() { return acao; }
    public String getAlvo() { return alvo; }
    public String getAtor() { return ator; }
    public LocalDateTime getData() { return data; }
}
