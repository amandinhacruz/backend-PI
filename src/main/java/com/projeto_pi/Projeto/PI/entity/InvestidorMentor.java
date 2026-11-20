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

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String areasInteresse;


    @Convert(converter = BigDecimalCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
    private BigDecimal ticketMin;

    @Convert(converter = BigDecimalCriptografadoConverter.class)
    @Column(columnDefinition = "TEXT")
    private BigDecimal ticketMax;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String estagiosPref;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
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

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String name;
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String cpf;
    public String getCpf() { return cpf; }
    public void setCpf(String value) { this.cpf = value; }

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String cnpj;
    public String getCnpj() { return cnpj; }
    public void setCnpj(String value) { this.cnpj = value; }

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String linkedin;
    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String value) { this.linkedin = value; }

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String city;
    public String getCity() { return city; }
    public void setCity(String value) { this.city = value; }

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String state;
    public String getState() { return state; }
    public void setState(String value) { this.state = value; }

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String aboutInvestor;
    public String getAboutInvestor() { return aboutInvestor; }
    public void setAboutInvestor(String value) { this.aboutInvestor = value; }

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String career;
    public String getCareer() { return career; }
    public void setCareer(String value) { this.career = value; }

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String lookingFor;
    public String getLookingFor() { return lookingFor; }
    public void setLookingFor(String value) { this.lookingFor = value; }
}
