package com.projeto_pi.Projeto.PI.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Contratos das conversas (RF05) e das reuniões, no formato das telas React. */
public final class ConversaDTOs {

    private ConversaDTOs() {
    }

    /** Uma conversa por conexão firmada; {@code id} é o id da conexão. */
    public record Conversation(String id, Long contactUserId, String name, String role,
                               LastMessage lastMessage, long unread, boolean hasPhoto) {
    }

    public record LastMessage(String text, String type, LocalDateTime sentAt, boolean mine) {
    }

    /**
     * Reunião vista por um dos participantes.
     * status: PENDENTE, ACEITO, REJEITADO, CANCELADO ou EXPIRADO (convite não respondido até o horário).
     */
    public record Meeting(String id, String connectionId, String contactName, String contactRole,
                          LocalDate date, LocalTime time, int durationMinutes, String subject, String location,
                          String status, boolean organizer, boolean canRespond, boolean canCancel) {
    }

    /** sender: "me", "contact" ou "system" (avisos automáticos de reunião). */
    public record ChatMessage(String id, String sender, String type, String text, LocalDateTime sentAt,
                              boolean read, Meeting meeting) {
    }

    public record SendMessageRequest(String text) {
    }

    public record InviteRequest(LocalDate date, LocalTime time, Integer durationMinutes, String subject,
                                String location) {
    }

    /** decision: "accepted" ou "rejected". */
    public record MeetingDecisionRequest(String decision) {
    }
}
