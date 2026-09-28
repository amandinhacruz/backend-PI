package com.projeto_pi.Projeto.PI.dto;

public class MatchResponseDTO {

    private Long startupId;
    private String startupNome;

    private Long investidorMentorId;
    private String tipoInvestidorMentor;

    private int pontuacao;
    private String nivelAfinidade;

    public MatchResponseDTO() {
    }

    public MatchResponseDTO(
            Long startupId,
            String startupNome,
            Long investidorMentorId,
            String tipoInvestidorMentor,
            int pontuacao,
            String nivelAfinidade) {

        this.startupId = startupId;
        this.startupNome = startupNome;
        this.investidorMentorId = investidorMentorId;
        this.tipoInvestidorMentor = tipoInvestidorMentor;
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

    public int getPontuacao() {
        return pontuacao;
    }

    public String getNivelAfinidade() {
        return nivelAfinidade;
    }
}