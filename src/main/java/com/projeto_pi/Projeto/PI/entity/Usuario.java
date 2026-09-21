package com.projeto_pi.Projeto.PI.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusUsuario status;

    private Boolean consentiuEm;

    @OneToOne
    @JoinColumn(name = "startup_id")
    private Startup startup;

    @OneToOne
    @JoinColumn(name = "investidor_mentor_id")
    private InvestidorMentor investidorMentor;

    public Usuario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Papel getPapel() {
        return papel;
    }

    public void setPapel(Papel papel) {
        this.papel = papel;
    }

    public StatusUsuario getStatus() {
        return status;
    }

    public void setStatus(StatusUsuario status) {
        this.status = status;
    }

    public Boolean getConsentiuEm() {
        return consentiuEm;
    }

    public void setConsentiuEm(Boolean consentiuEm) {
        this.consentiuEm = consentiuEm;
    }

    public Startup getStartup() {
        return startup;
    }

    public void setStartup(Startup startup) {
        this.startup = startup;
    }

    public InvestidorMentor getInvestidorMentor() {
        return investidorMentor;
    }

    public void setInvestidorMentor(InvestidorMentor investidorMentor) {
        this.investidorMentor = investidorMentor;
    }
}
