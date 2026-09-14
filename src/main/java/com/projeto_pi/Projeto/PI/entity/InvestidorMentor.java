package com.projeto_pi.Projeto.PI.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "investidores_mentores")
public class InvestidorMentor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoInvestidorMentor tipo;

    @Column(length = 1000)
    private String areasInteresse;

    private BigDecimal ticketMin;

    private BigDecimal ticketMax;

    @Column(length = 1000)
    private String estagiosPref;

    @Column(length = 1000)
    private String disponibilidade;

    public InvestidorMentor() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoInvestidorMentor getTipo() {
        return tipo;
    }

    public void setTipo(TipoInvestidorMentor tipo) {
        this.tipo = tipo;
    }

    public String getAreasInteresse() {
        return areasInteresse;
    }

    public void setAreasInteresse(String areasInteresse) {
        this.areasInteresse = areasInteresse;
    }

    public BigDecimal getTicketMin() {
        return ticketMin;
    }

    public void setTicketMin(BigDecimal ticketMin) {
        this.ticketMin = ticketMin;
    }

    public BigDecimal getTicketMax() {
        return ticketMax;
    }

    public void setTicketMax(BigDecimal ticketMax) {
        this.ticketMax = ticketMax;
    }

    public String getEstagiosPref() {
        return estagiosPref;
    }

    public void setEstagiosPref(String estagiosPref) {
        this.estagiosPref = estagiosPref;
    }

    public String getDisponibilidade() {
        return disponibilidade;
    }

    public void setDisponibilidade(String disponibilidade) {
        this.disponibilidade = disponibilidade;
    }
}