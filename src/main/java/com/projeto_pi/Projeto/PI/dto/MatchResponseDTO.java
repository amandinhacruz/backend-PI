package com.projeto_pi.Projeto.PI.dto;

public class MatchResponseDTO {

    private Long startupId;
    private String startupNome;

    private Long investidorMentorId;
    private String tipoInvestidorMentor;
    private String areasInteresse;
    private String estagiosPref;
    private String disponibilidade;

    private int pontuacao;
    private String nivelAfinidade;

    public MatchResponseDTO() {
    }

    public MatchResponseDTO(
            Long startupId,
            String startupNome,
            Long investidorMentorId,
            String tipoInvestidorMentor,
            String areasInteresse,
            String estagiosPref,
            String disponibilidade,
            int pontuacao,
            String nivelAfinidade) {

        this.startupId = startupId;
        this.startupNome = startupNome;
        this.investidorMentorId = investidorMentorId;
        this.tipoInvestidorMentor = tipoInvestidorMentor;
        this.areasInteresse = areasInteresse;
        this.estagiosPref = estagiosPref;
        this.disponibilidade = disponibilidade;
        this.pontuacao = pontuacao;
        this.nivelAfinidade = nivelAfinidade;
    }

    public Long getStartupId() {
        return startupId;
    }

    public String getStartupNome() {
        return startupNome;
    }

    public Long getInvestidorMentorId() {
        return investidorMentorId;
    }

    public String getTipoInvestidorMentor() {
        return tipoInvestidorMentor;
    }

    public String getAreasInteresse() {
        return areasInteresse;
    }

    public String getEstagiosPref() {
        return estagiosPref;
    }

    public String getDisponibilidade() {
        return disponibilidade;
    }

    public int getPontuacao() {
        return pontuacao;
    }

    public String getNivelAfinidade() {
        return nivelAfinidade;
    }
}