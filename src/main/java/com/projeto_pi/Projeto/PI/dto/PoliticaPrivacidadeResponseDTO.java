package com.projeto_pi.Projeto.PI.dto;

public class PoliticaPrivacidadeResponseDTO {

    private String versao;
    private String titulo;
    private String conteudo;
    private boolean aceiteObrigatorio;

    public PoliticaPrivacidadeResponseDTO() {
    }

    public PoliticaPrivacidadeResponseDTO(
            String versao,
            String titulo,
            String conteudo,
            boolean aceiteObrigatorio) {

        this.versao = versao;
        this.titulo = titulo;
        this.conteudo = conteudo;
        this.aceiteObrigatorio = aceiteObrigatorio;
    }

    public String getVersao() {
        return versao;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getConteudo() {
        return conteudo;
    }

    public boolean isAceiteObrigatorio() {
        return aceiteObrigatorio;
    }
}