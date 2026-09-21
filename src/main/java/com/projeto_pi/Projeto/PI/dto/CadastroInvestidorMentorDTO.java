package com.projeto_pi.Projeto.PI.dto;

import com.projeto_pi.Projeto.PI.entity.TipoInvestidorMentor;

import java.math.BigDecimal;

public class CadastroInvestidorMentorDTO {

    private String email;
    private String senha;

    private TipoInvestidorMentor tipo;
    private String areasInteresse;
    private BigDecimal ticketMin;
    private BigDecimal ticketMax;
    private String estagiosPref;
    private String disponibilidade;

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
}