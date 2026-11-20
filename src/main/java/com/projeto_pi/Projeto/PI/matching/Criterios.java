package com.projeto_pi.Projeto.PI.matching;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Regras de elegibilidade do matchmaking (camada 1, determinística e auditável).
 *
 * No portal do investidor só aparecem startups que batem nos três critérios:
 * segmento, estágio e ticket médio. Regras de dados ausentes:
 * - um critério só filtra quando o investidor o declarou (sem preferência = aceita qualquer valor);
 * - se o investidor declarou o critério e a startup não informou o dado, ela não é elegível,
 *   porque não há como verificar a compatibilidade.
 */
public final class Criterios {

    private Criterios() {
    }

    /** Rótulos antigos do cadastro que equivalem aos atuais. */
    private static final Map<String, String> EQUIVALENTES = Map.of(
            "saude", "healthtech",
            "educacao", "edtech");

    /** Minúsculas, sem acentos e com espaços normalizados. */
    public static String norm(String valor) {
        if (valor == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** Divide "Fintech; Healthtech" (também aceita vírgula, barra vertical e quebra de linha). */
    public static List<String> lista(String valor) {
        if (valor == null || valor.isBlank()) {
            return List.of();
        }
        return Arrays.stream(valor.split("[;,|\\n]+"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .toList();
    }

    /**
     * Chaves de comparação de um rótulo de segmento. "Agritech / Agronegócio" gera
     * {"agritech / agronegocio", "agritech", "agronegocio"}; "Saúde" também gera "healthtech".
     */
    static Set<String> chavesSegmento(String rotulo) {
        Set<String> chaves = new HashSet<>();
        String normalizado = norm(rotulo);
        if (normalizado.isEmpty()) {
            return chaves;
        }
        chaves.add(normalizado);
        for (String parte : normalizado.split("/")) {
            String p = parte.trim();
            if (!p.isEmpty()) {
                chaves.add(p);
            }
        }
        for (String chave : List.copyOf(chaves)) {
            EQUIVALENTES.forEach((antigo, atual) -> {
                if (chave.equals(antigo)) {
                    chaves.add(atual);
                }
                if (chave.equals(atual)) {
                    chaves.add(antigo);
                }
            });
        }
        return chaves;
    }

    public static boolean segmentoCompativel(String segmentoStartup, List<String> segmentosInvestidor) {
        Set<String> chavesStartup = chavesSegmento(segmentoStartup);
        if (chavesStartup.isEmpty()) {
            return false;
        }
        for (String segmento : segmentosInvestidor) {
            for (String chave : chavesSegmento(segmento)) {
                if (chavesStartup.contains(chave)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean estagioCompativel(String estagioStartup, List<String> estagiosPreferidos) {
        if (estagiosPreferidos.isEmpty()) {
            return true;
        }
        String estagio = norm(estagioStartup);
        if (estagio.isEmpty()) {
            return false;
        }
        return estagiosPreferidos.stream().map(Criterios::norm).anyMatch(estagio::equals);
    }

    /** Faixa de valores; limite nulo significa "não informado". */
    public record Faixa(BigDecimal min, BigDecimal max) {

        public boolean vazia() {
            return min == null && max == null;
        }

        BigDecimal piso() {
            return min != null ? min : BigDecimal.ZERO;
        }
    }

    /** As faixas se cruzam: a startup busca um valor que o investidor aceita aportar. */
    public static boolean ticketCompativel(Faixa investidor, Faixa startup) {
        if (investidor.vazia()) {
            return true;
        }
        if (startup.vazia()) {
            return false;
        }
        boolean startupCabeNoTeto = investidor.max() == null || startup.piso().compareTo(investidor.max()) <= 0;
        boolean investidorCabeNoTeto = startup.max() == null || investidor.piso().compareTo(startup.max()) <= 0;
        return startupCabeNoTeto && investidorCabeNoTeto;
    }

    /**
     * Parcela (0 a 1) da faixa buscada pela startup que cabe na faixa do investidor.
     * Sem preferência do investidor o resultado é neutro (0,5).
     */
    public static double coberturaTicket(Faixa investidor, Faixa startup) {
        if (investidor.vazia()) {
            return 0.5;
        }
        if (!ticketCompativel(investidor, startup)) {
            return 0;
        }
        if (startup.min() == null || startup.max() == null) {
            return 1;
        }
        double inicio = startup.min().doubleValue();
        double fim = startup.max().doubleValue();
        if (fim <= inicio) {
            return 1;
        }
        double pisoInvestidor = investidor.piso().doubleValue();
        double tetoInvestidor = investidor.max() == null ? Double.MAX_VALUE : investidor.max().doubleValue();
        double sobreposicao = Math.min(fim, tetoInvestidor) - Math.max(inicio, pisoInvestidor);
        return Math.max(0, Math.min(1, sobreposicao / (fim - inicio)));
    }

    /** Resultado dos três critérios para um par investidor x startup. */
    public record Elegibilidade(boolean segmento, boolean estagio, boolean ticket) {

        public boolean elegivel() {
            return segmento && estagio && ticket;
        }
    }

    public static Elegibilidade avaliar(PreferenciasInvestidor investidor, StartupResumo startup) {
        return new Elegibilidade(
                segmentoCompativel(startup.segmento(), investidor.segmentos()),
                estagioCompativel(startup.estagio(), investidor.estagios()),
                ticketCompativel(investidor.ticket(), new Faixa(startup.ticketMin(), startup.ticketMax())));
    }
}
