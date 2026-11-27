package com.projeto_pi.Projeto.PI.dto;

import java.time.LocalDateTime;

/** Contratos das comunidades (conversa em grupo entre startups e investidores). */
public final class ComunidadeDTOs {

    private ComunidadeDTOs() {
    }

    /** Comunidade na lista; {@code unread} só conta para quem participa. */
    public record Community(String id, String slug, String name, String description, long members,
                            boolean joined, long unread, LastPost lastMessage) {
    }

    public record LastPost(String text, String author, String category, LocalDateTime sentAt, boolean mine) {
    }

    /**
     * Mensagem no grupo. sender: "me", "member" ou "system" (entrada e saída de membros).
     * category: MENSAGEM, NOTICIA, INICIATIVA, MOVIMENTO ou SISTEMA.
     */
    public record Post(String id, String sender, Long authorUserId, String authorName, String authorRole,
                       boolean authorHasPhoto, String category, String text, LocalDateTime sentAt) {
    }

    public record Member(Long userId, String name, String role, boolean hasPhoto, boolean me,
                         LocalDateTime joinedAt) {
    }

    public record PostRequest(String text, String category) {
    }
}
