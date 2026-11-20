package com.projeto_pi.Projeto.PI.entity;

import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Dossiê da startup gerado sob demanda para um investidor (guardado para não gerar duas vezes). */
@Entity
@Table(name = "dossies_match",
        uniqueConstraints = @UniqueConstraint(columnNames = {"investidor_perfil_id", "startup_perfil_id"}))
public class DossieMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investidor_perfil_id", nullable = false)
    private Long investidorPerfilId;

    @Column(name = "startup_perfil_id", nullable = false)
    private Long startupPerfilId;

    @Column(nullable = false, length = 64)
    private String hash;

    /** JSON do dossiê, criptografado em repouso. */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String conteudo;

    @Column(length = 80)
    private String modelo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected DossieMatch() {
    }

    public DossieMatch(Long investidorPerfilId, Long startupPerfilId) {
        this.investidorPerfilId = investidorPerfilId;
        this.startupPerfilId = startupPerfilId;
    }

    public void registrar(String hash, String conteudo, String modelo) {
        this.hash = hash;
        this.conteudo = conteudo;
        this.modelo = modelo;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getInvestidorPerfilId() { return investidorPerfilId; }
    public Long getStartupPerfilId() { return startupPerfilId; }
    public String getHash() { return hash; }
    public String getConteudo() { return conteudo; }
    public String getModelo() { return modelo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
