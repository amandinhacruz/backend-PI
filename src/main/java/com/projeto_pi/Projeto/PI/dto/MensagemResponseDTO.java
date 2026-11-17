package com.projeto_pi.Projeto.PI.dto;

import java.time.LocalDateTime;

public class MensagemResponseDTO {

    private Long id;
    private Long remetenteId;
    private Long destinatarioId;
    private String conteudo;
    private LocalDateTime dataEnvio;

    public MensagemResponseDTO() {
    }

    public MensagemResponseDTO(
            Long id,
            Long remetenteId,
            Long destinatarioId,
            String conteudo,
            LocalDateTime dataEnvio) {

        this.id = id;
        this.remetenteId = remetenteId;
        this.destinatarioId = destinatarioId;
        this.conteudo = conteudo;
        this.dataEnvio = dataEnvio;
    }

    public Long getId() {
        return id;
    }

    public Long getRemetenteId() {
        return remetenteId;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public String getConteudo() {
        return conteudo;
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }
}
