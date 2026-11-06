package com.projeto_pi.Projeto.PI.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import com.projeto_pi.Projeto.PI.config.BigDecimalCriptografadoConverter;
import jakarta.persistence.Convert;

@Entity
@Table(name = "investidores_mentores")
public class InvestidorMentor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoInvestidorMentor tipo;

    @Convert(converter = StringCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String areasInteresse;


    @Convert(converter = BigDecimalCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
    private BigDecimal ticketMin;

    @Convert(converter = BigDecimalCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
    private BigDecimal ticketMax;

    @Convert(converter = StringCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String estagiosPref;

    @Convert(converter = StringCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
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