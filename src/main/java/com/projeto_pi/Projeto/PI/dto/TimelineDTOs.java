package com.projeto_pi.Projeto.PI.dto;

import java.time.LocalDateTime;

/** Contratos da timeline (publicações de texto, curtidas, comentários e compartilhamentos). */
public final class TimelineDTOs {

    private TimelineDTOs() {
    }

    /**
     * Quem publicou ou comentou. {@code type}: STARTUP ou INVESTIDOR; {@code profileId}: id do perfil da
     * startup (para /startups/{id}/perfil) ou do investidor; {@code role}: rótulo exibido ("Startup",
     * "Investidor(a) anjo"...). O front monta o link do perfil com esses dados.
     */
    public record Author(Long userId, String type, String profileId, String name, String role, boolean hasPhoto) {
    }

    /**
     * Publicação no feed. {@code repost}: compartilhamento sem texto (o front mostra a original com
     * "Fulano compartilhou"). {@code sharedPost}: a publicação compartilhada (um nível); nula e
     * {@code sharedUnavailable} verdadeiro quando a original foi removida.
     * {@code likes}, {@code comments} e {@code shares} contam só usuários aprovados.
     */
    public record Post(String id, Author author, String text, LocalDateTime createdAt, boolean mine,
                       long likes, long comments, long shares, boolean liked, boolean shared,
                       boolean repost, Post sharedPost, boolean sharedUnavailable) {
    }

    public record Comment(String id, String postId, Author author, String text, LocalDateTime createdAt,
                          boolean mine, boolean canDelete) {
    }

    /** {@code sharedPostId}: compartilhar outra publicação (texto opcional). */
    public record PostRequest(String text, String sharedPostId) {
    }

    public record CommentRequest(String text) {
    }
}
