package com.projeto_pi.Projeto.PI.dto;

import com.projeto_pi.Projeto.PI.entity.TipoInvestidorMentor;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;

public class CadastroInvestidorMentorDTO {

    @JsonAlias({"corporateEmail"})
    private String email;
    @JsonAlias("password")
    private String senha;
    private TipoInvestidorMentor tipo;
    @JsonAlias({"segment"})
    private String areasInteresse;
    @JsonAlias({"minimumTicket", "ticketMinimo"})
    private BigDecimal ticketMin;
    @JsonAlias({"maximumTicket", "ticketMaximo"})
    private BigDecimal ticketMax;
    @JsonAlias({"stage"})
    private String estagiosPref;
    private String disponibilidade;
    @JsonAlias({"dataProcessing"})
    private Boolean consentimentoLGPD;

    public CadastroInvestidorMentorDTO() {
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

    public Boolean getConsentimentoLGPD() {
        return consentimentoLGPD;
    }

    public void setConsentimentoLGPD(Boolean consentimentoLGPD) {
        this.consentimentoLGPD = consentimentoLGPD;
    }

    /** Vários segmentos de interesse (o front envia "segments"); guardados separados por "; ". */
    public void setSegments(java.util.List<String> segments) {
        if (segments != null && !segments.isEmpty()) {
            this.areasInteresse = String.join("; ", segments);
        }
    }

    /** Vários estágios preferidos (o front envia "stages"). */
    public void setStages(java.util.List<String> stages) {
        if (stages != null && !stages.isEmpty()) {
            this.estagiosPref = String.join("; ", stages);
        }
    }

    private String confirmPassword;
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String value) { this.confirmPassword = value; }

    @com.fasterxml.jackson.annotation.JsonAlias({"aceitaTermos"})
    private Boolean terms;
    public Boolean getTerms() { return terms; }
    public void setTerms(Boolean value) { this.terms = value; }

    private Boolean privacy;
    public Boolean getPrivacy() { return privacy; }
    public void setPrivacy(Boolean value) { this.privacy = value; }

    private String name;
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }

    private String cpf;
    public String getCpf() { return cpf; }
    public void setCpf(String value) { this.cpf = value; }

    private String cnpj;
    public String getCnpj() { return cnpj; }
    public void setCnpj(String value) { this.cnpj = value; }

    private String linkedin;
    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String value) { this.linkedin = value; }

    private String city;
    public String getCity() { return city; }
    public void setCity(String value) { this.city = value; }

    private String state;
    public String getState() { return state; }
    public void setState(String value) { this.state = value; }

    private String aboutInvestor;
    public String getAboutInvestor() { return aboutInvestor; }
    public void setAboutInvestor(String value) { this.aboutInvestor = value; }

    private String career;
    public String getCareer() { return career; }
    public void setCareer(String value) { this.career = value; }

    private String lookingFor;
    public String getLookingFor() { return lookingFor; }
    public void setLookingFor(String value) { this.lookingFor = value; }
}
