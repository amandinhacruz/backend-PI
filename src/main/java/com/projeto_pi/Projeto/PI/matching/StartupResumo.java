package com.projeto_pi.Projeto.PI.matching;

import java.math.BigDecimal;

/**
 * Projeção do perfil de uma startup usada pelo matchmaking.
 *
 * É carregada por consulta com colunas explícitas (ver UsuarioRepository), sem os arquivos
 * de pitch deck e canvas: assim listar candidatas não descriptografa megabytes por startup.
 */
public record StartupResumo(
        Long usuarioId,
        String versaoPolitica,
        Long id,
        String nome,
        String segmento,
        String estagio,
        String cidade,
        String uf,
        String resumo,
        String problema,
        String diferenciais,
        String necessidades,
        String pitch,
        BigDecimal ticketMin,
        BigDecimal ticketMax,
        String website,
        String video,
        String pitchDeckNome,
        String canvasNome) {

    public String local() {
        String c = cidade == null ? "" : cidade.trim();
        String u = uf == null ? "" : uf.trim();
        return c + (!c.isEmpty() && !u.isEmpty() ? "/" : "") + u;
    }

    /** Ponto médio da faixa de ticket buscada (ou o único limite informado). */
    public BigDecimal ticketMedio() {
        if (ticketMin != null && ticketMax != null) {
            return ticketMin.add(ticketMax).divide(BigDecimal.valueOf(2));
        }
        return ticketMin != null ? ticketMin : ticketMax;
    }
}
