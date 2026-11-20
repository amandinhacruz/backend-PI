package com.projeto_pi.Projeto.PI.entity;

import jakarta.persistence.*;
import com.projeto_pi.Projeto.PI.config.StringCriptografadoConverter;
import jakarta.persistence.Convert;

@Entity
@Table(name = "startups")
public class Startup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String segmento;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String estagio;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String necessidades;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String pitch;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String canvas;

    public Startup() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSegmento() {
        return segmento;
    }

    public void setSegmento(String segmento) {
        this.segmento = segmento;
    }

    public String getEstagio() {
        return estagio;
    }

    public void setEstagio(String estagio) {
        this.estagio = estagio;
    }

    public String getNecessidades() {
        return necessidades;
    }

    public void setNecessidades(String necessidades) {
        this.necessidades = necessidades;
    }

    public String getPitch() {
        return pitch;
    }

    public void setPitch(String pitch) {
        this.pitch = pitch;
    }

    public String getCanvas() {
        return canvas;
    }

    public void setCanvas(String canvas) {
        this.canvas = canvas;
    }

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String cnpj;
    public String getCnpj() { return cnpj; }
    public void setCnpj(String value) { this.cnpj = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"site"})
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String website;
    public String getWebsite() { return website; }
    public void setWebsite(String value) { this.website = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"cidade"})
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String city;
    public String getCity() { return city; }
    public void setCity(String value) { this.city = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"estado"})
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String state;
    public String getState() { return state; }
    public void setState(String value) { this.state = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"anoFundacao"})
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String foundedYear;
    public String getFoundedYear() { return foundedYear; }
    public void setFoundedYear(String value) { this.foundedYear = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"responsavel"})
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String profileOwner;
    public String getProfileOwner() { return profileOwner; }
    public void setProfileOwner(String value) { this.profileOwner = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"resumo"})
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String summary;
    public String getSummary() { return summary; }
    public void setSummary(String value) { this.summary = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"problema"})
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String problem;
    public String getProblem() { return problem; }
    public void setProblem(String value) { this.problem = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"diferenciais"})
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String competitiveAdvantages;
    public String getCompetitiveAdvantages() { return competitiveAdvantages; }
    public void setCompetitiveAdvantages(String value) { this.competitiveAdvantages = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"ticketMinimo"})
    private java.math.BigDecimal minimumTicket;
    public java.math.BigDecimal getMinimumTicket() { return minimumTicket; }
    public void setMinimumTicket(java.math.BigDecimal value) { this.minimumTicket = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"ticketMaximo"})
    private java.math.BigDecimal maximumTicket;
    public java.math.BigDecimal getMaximumTicket() { return maximumTicket; }
    public void setMaximumTicket(java.math.BigDecimal value) { this.maximumTicket = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"video"})
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String presentationVideo;
    public String getPresentationVideo() { return presentationVideo; }
    public void setPresentationVideo(String value) { this.presentationVideo = value; }

    @Column(columnDefinition = "bytea")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Convert(converter = com.projeto_pi.Projeto.PI.config.ArquivoCriptografadoConverter.class)
    private byte[] pitchDeckData;
    public byte[] getPitchDeckData() { return pitchDeckData; }
    public void setPitchDeckData(byte[] value) { this.pitchDeckData = value; }

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String pitchDeckName;
    public String getPitchDeckName() { return pitchDeckName; }
    public void setPitchDeckName(String value) { this.pitchDeckName = value; }

    @Column(columnDefinition = "bytea")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Convert(converter = com.projeto_pi.Projeto.PI.config.ArquivoCriptografadoConverter.class)
    private byte[] businessCanvasData;
    public byte[] getBusinessCanvasData() { return businessCanvasData; }
    public void setBusinessCanvasData(byte[] value) { this.businessCanvasData = value; }

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringCriptografadoConverter.class)
    private String businessCanvasName;
    public String getBusinessCanvasName() { return businessCanvasName; }
    public void setBusinessCanvasName(String value) { this.businessCanvasName = value; }
}
