package com.projeto_pi.Projeto.PI.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class CadastroStartupDTO {

    @JsonAlias({"corporateEmail"})
    private String email;
    @JsonAlias("password")
    private String senha;
    @JsonAlias({"name", "nomeStartup"})
    private String nome;
    @JsonAlias({"segment"})
    private String segmento;
    @JsonAlias({"stage"})
    private String estagio;
    private String necessidades;
    private String pitch;
    private String canvas;
    @JsonAlias({"dataProcessing", "autorizaTratamento"})
    private Boolean consentimentoLGPD;

    public CadastroStartupDTO() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
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

    public Boolean getConsentimentoLGPD() {
        return consentimentoLGPD;
    }

    public void setConsentimentoLGPD(Boolean consentimentoLGPD) {
        this.consentimentoLGPD = consentimentoLGPD;
    }

    private String confirmPassword;
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String value) { this.confirmPassword = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"aceitaTermos"})
    private Boolean terms;
    public Boolean getTerms() { return terms; }
    public void setTerms(Boolean value) { this.terms = value; }

    @JsonAlias("cientePrivacidade")
    private Boolean privacy;
    public Boolean getPrivacy() { return privacy; }
    public void setPrivacy(Boolean value) { this.privacy = value; }

    private String cnpj;
    public String getCnpj() { return cnpj; }
    public void setCnpj(String value) { this.cnpj = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"site"})
    private String website;
    public String getWebsite() { return website; }
    public void setWebsite(String value) { this.website = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"cidade"})
    private String city;
    public String getCity() { return city; }
    public void setCity(String value) { this.city = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"estado"})
    private String state;
    public String getState() { return state; }
    public void setState(String value) { this.state = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"anoFundacao"})
    private String foundedYear;
    public String getFoundedYear() { return foundedYear; }
    public void setFoundedYear(String value) { this.foundedYear = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"responsavel"})
    private String profileOwner;
    public String getProfileOwner() { return profileOwner; }
    public void setProfileOwner(String value) { this.profileOwner = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"resumo"})
    private String summary;
    public String getSummary() { return summary; }
    public void setSummary(String value) { this.summary = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"problema"})
    private String problem;
    public String getProblem() { return problem; }
    public void setProblem(String value) { this.problem = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"diferenciais"})
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
    private String presentationVideo;
    public String getPresentationVideo() { return presentationVideo; }
    public void setPresentationVideo(String value) { this.presentationVideo = value; }
}
