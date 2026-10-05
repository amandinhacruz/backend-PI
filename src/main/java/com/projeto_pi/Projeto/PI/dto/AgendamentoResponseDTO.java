package com.projeto_pi.Projeto.PI.dto;

import com.projeto_pi.Projeto.PI.entity.StatusAgendamento;

import java.time.LocalDate;
import java.time.LocalTime;

public class AgendamentoResponseDTO {

    private Long id;
    private Long solicitanteId;
    private Long destinatarioId;
    private LocalDate data;
    private LocalTime horario;
    private String assunto;
    private StatusAgendamento status;

    public AgendamentoResponseDTO() {
    }

    public AgendamentoResponseDTO(
            Long id,
            Long solicitanteId,
            Long destinatarioId,
            LocalDate data,
            LocalTime horario,
            String assunto,
            StatusAgendamento status) {

        this.id = id;
        this.solicitanteId = solicitanteId;
        this.destinatarioId = destinatarioId;
        this.data = data;
        this.horario = horario;
        this.assunto = assunto;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getSolicitanteId() {
        return solicitanteId;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public LocalDate getData() {
        return data;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public String getAssunto() {
        return assunto;
    }

    public StatusAgendamento getStatus() {
        return status;
    }
}
