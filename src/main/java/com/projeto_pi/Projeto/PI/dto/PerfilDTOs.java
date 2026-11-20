package com.projeto_pi.Projeto.PI.dto;

import java.math.BigDecimal;
import java.util.List;

/** Perfis de startup e investidor (visualização e edição). */
public final class PerfilDTOs {

    private PerfilDTOs() {
    }

    /**
     * Perfil da startup. Os campos {@code cnpj}, {@code profileOwner} e {@code email} só vêm
     * preenchidos para a própria startup; na visualização por investidores são nulos.
     */
    public record StartupProfile(
            String id, Long userId, String name, String segment, String stage, String city, String state,
            String location, String foundedYear, String website, String summary, String problem,
            String competitiveAdvantages, BigDecimal minimumTicket, BigDecimal maximumTicket,
            String presentationVideo, boolean hasPitchDeck, String pitchDeckName, boolean hasCanvas,
            String canvasName, boolean hasPhoto, String cnpj, String profileOwner, String email) {
    }

    /** Perfil do investidor ou mentor; {@code cpf}, {@code cnpj} e {@code email} só para o próprio. */
    public record InvestorProfile(
            String id, Long userId, String type, String name, String city, String state, String linkedin,
            List<String> segments, List<String> stages, BigDecimal ticketMin, BigDecimal ticketMax,
            String aboutInvestor, String career, String lookingFor, boolean hasPhoto,
            String cpf, String cnpj, String email) {
    }

    /** Resposta de GET /perfil: só um dos dois vem preenchido, conforme o papel. */
    public record OwnProfile(String role, StartupProfile startup, InvestorProfile investor) {
    }

    public record StartupUpdate(
            String name, String website, String city, String state, String foundedYear, String profileOwner,
            String segment, String stage, BigDecimal minimumTicket, BigDecimal maximumTicket,
            String summary, String problem, String competitiveAdvantages, String presentationVideo) {
    }

    public record InvestorUpdate(
            String name, String linkedin, String city, String state, List<String> segments, List<String> stages,
            BigDecimal ticketMin, BigDecimal ticketMax, String aboutInvestor, String career, String lookingFor) {
    }
}
