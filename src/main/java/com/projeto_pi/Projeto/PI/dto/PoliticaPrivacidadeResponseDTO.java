package com.projeto_pi.Projeto.PI.dto;

public class PoliticaPrivacidadeResponseDTO {

    private String versao;
    private String titulo;
    private String finalidade;
    private String dadosTratados;
    private String compartilhamento;
    private String armazenamento;
    private String direitosTitular;
    private String contato;

    public PoliticaPrivacidadeResponseDTO() {
    }

    public PoliticaPrivacidadeResponseDTO(
            String versao,
            String titulo,
            String finalidade,
            String dadosTratados,
            String compartilhamento,
            String armazenamento,
            String direitosTitular,
            String contato) {

        this.versao = versao;
        this.titulo = titulo;
        this.finalidade = finalidade;
        this.dadosTratados = dadosTratados;
        this.compartilhamento = compartilhamento;
        this.armazenamento = armazenamento;
        this.direitosTitular = direitosTitular;
        this.contato = contato;
    }

    public String getVersao() {
        return versao;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getFinalidade() {
        return finalidade;
    }

    public String getDadosTratados() {
        return dadosTratados;
    }

    public String getCompartilhamento() {
        return compartilhamento;
    }

    public String getArmazenamento() {
        return armazenamento;
    }

    public String getDireitosTitular() {
        return direitosTitular;
    }

    public String getContato() {
        return contato;
    }
}