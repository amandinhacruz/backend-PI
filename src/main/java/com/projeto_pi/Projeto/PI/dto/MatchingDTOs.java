package com.projeto_pi.Projeto.PI.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contratos do matchmaking e das conexões. As chaves seguem os tipos dos cartões React
 * (IMatching, SMatching, IConections, SConections), como já acontece no feed de startups.
 */
public final class MatchingDTOs {

    private MatchingDTOs() {
    }

    /** Composição da afinidade: nota do analista (nula sem IA), nota local e ajustes em pontos. */
    public record Breakdown(Integer ai, int local, int learning, int reputation) {
    }

    /** Startup recomendada ao investidor. {@code source}: "IA" ou "LOCAL". */
    public record MatchCard(
            String id, Long userId, String name, String role, List<String> tags, String description,
            String location, BigDecimal ticketMin, BigDecimal ticketMax,
            int affinity, String source, boolean pending, String summary,
            List<String> strengths, List<String> concerns, List<String> questions,
            List<String> factors, Breakdown breakdown,
            boolean hasPhoto, boolean hasPitchDeck, boolean hasCanvas, String presentationVideo) {
    }

    public record AiStatus(boolean enabled, int pending, String model) {
    }

    public record Criteria(List<String> segments, List<String> stages, BigDecimal ticketMin, BigDecimal ticketMax) {
    }

    /** Fila de matching do investidor. {@code approvedStartups} ajuda a explicar uma fila vazia. */
    public record Queue(List<MatchCard> items, AiStatus ai, Criteria criteria, int approvedStartups) {
    }

    /** {@code decision}: "accepted" (solicitar/firmar) ou "rejected" (passar/negar). */
    public record DecisionRequest(String decision) {
    }

    public record DecisionResult(Long connectionId, String status) {
    }

    /** Pedido de conexão recebido pela startup. */
    public record RequestCard(
            String id, Long userId, String name, String role, List<String> tags, String description,
            String location, BigDecimal ticketMin, BigDecimal ticketMax,
            int affinity, String summary, LocalDateTime requestedAt, boolean hasPhoto) {
    }

    public record Rating(int score, String comment) {
    }

    public record RatingRequest(Integer score, String comment) {
    }

    /** Conexão firmada, vista por um dos lados; os dados são da outra parte. */
    public record ConnectionItem(
            String id, String profileId, Long userId, String name, String type, String city, String state,
            List<String> segments, BigDecimal ticketMin, BigDecimal ticketMax, String connectedAt,
            Rating myRating, boolean hasPhoto) {
    }

    public record Dossier(
            String source, String model, String executiveSummary, String thesisFit,
            List<String> opportunities, List<String> risks, List<String> questions, List<String> nextSteps,
            LocalDateTime generatedAt) {
    }
}
