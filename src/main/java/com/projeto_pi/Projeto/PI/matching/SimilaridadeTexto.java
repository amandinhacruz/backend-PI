package com.projeto_pi.Projeto.PI.matching;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Similaridade de texto calculada dentro do backend (TF-IDF com cosseno), sem serviço externo.
 *
 * Compara a tese do investidor com o pitch de cada startup. É a parte "semântica" do motor
 * local: funciona sem chave de IA e serve de primeira ordenação antes do analista generativo.
 */
public final class SimilaridadeTexto {

    private SimilaridadeTexto() {
    }

    /** Palavras sem valor para comparar teses e pitches (já sem acento). */
    private static final Set<String> STOPWORDS = Set.copyOf(List.of(
            "para", "por", "com", "sem", "sob", "sobre", "que", "aos", "das", "dos", "nas", "nos",
            "uma", "uns", "umas", "como", "mais", "menos", "muito", "muita", "nao", "sim", "ser",
            "sao", "foi", "era", "tem", "ter", "temos", "nossa", "nosso", "sua", "seu", "minha",
            "meu", "este", "esta", "esse", "essa", "isso", "isto", "aquele", "aquela", "entre",
            "ate", "apos", "desde", "pela", "pelo", "tambem", "cada", "todo", "toda", "qual",
            "quais", "quando", "onde", "quem", "eles", "elas", "ele", "ela", "voce", "the", "and",
            "for", "with", "startup", "empresa", "negocio", "solucao", "investidor", "investimento",
            "busco", "buscamos", "procuro", "atraves", "meio", "forma", "area", "ano", "anos",
            "ainda", "apenas", "assim", "bem", "pode", "podem", "sendo", "estar", "estao",
            "ja", "nem", "seja", "suas", "seus", "nossas", "nossos", "pelas", "pelos", "todas", "todos"));

    public record Resultado(double similaridade, List<String> termosEmComum) {
    }

    /** Reduz plurais comuns do português para que "soluções" e "solução" contem como o mesmo termo. */
    static String radical(String token) {
        int n = token.length();
        if (n <= 4) {
            return token;
        }
        if (token.endsWith("oes") || token.endsWith("aes")) {
            return token.substring(0, n - 3) + "ao";
        }
        if (token.endsWith("ais")) {
            return token.substring(0, n - 3) + "al";
        }
        if (token.endsWith("eis")) {
            return token.substring(0, n - 3) + "el";
        }
        if (token.endsWith("ns")) {
            return token.substring(0, n - 2) + "m";
        }
        if (token.endsWith("res") || token.endsWith("zes")) {
            return token.substring(0, n - 2);
        }
        if (token.endsWith("s")) {
            return token.substring(0, n - 1);
        }
        return token;
    }

    /** Pares [radical, palavra original] dos termos relevantes do texto. */
    static List<String[]> termos(String texto) {
        List<String[]> saida = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return saida;
        }
        for (String bruto : texto.split("[^\\p{L}\\p{Nd}]+")) {
            String normalizado = Criterios.norm(bruto);
            if (normalizado.length() < 3 || STOPWORDS.contains(normalizado)
                    || normalizado.chars().allMatch(Character::isDigit)) {
                continue;
            }
            String radical = radical(normalizado);
            if (STOPWORDS.contains(radical)) {
                continue;
            }
            saida.add(new String[]{radical, bruto.toLowerCase(Locale.ROOT)});
        }
        return saida;
    }

    private static Map<String, Integer> frequencias(List<String[]> termos) {
        Map<String, Integer> tf = new HashMap<>();
        for (String[] termo : termos) {
            tf.merge(termo[0], 1, Integer::sum);
        }
        return tf;
    }

    private static double peso(int frequencia, double idf) {
        return (1 + Math.log(frequencia)) * idf;
    }

    /**
     * Compara o texto de referência com cada candidato.
     *
     * @return por candidato, o cosseno (0 a 1) e até quatro termos que os dois textos têm em comum
     */
    public static Map<Long, Resultado> comparar(String referencia, Map<Long, String> candidatos) {
        List<String[]> termosReferencia = termos(referencia);
        Map<String, Integer> tfReferencia = frequencias(termosReferencia);
        Map<String, String> exibicao = new HashMap<>();
        termosReferencia.forEach(termo -> exibicao.putIfAbsent(termo[0], termo[1]));

        Map<Long, Map<String, Integer>> tfCandidatos = new LinkedHashMap<>();
        Map<String, Integer> documentosComTermo = new HashMap<>();
        tfReferencia.keySet().forEach(termo -> documentosComTermo.merge(termo, 1, Integer::sum));
        candidatos.forEach((id, texto) -> {
            Map<String, Integer> tf = frequencias(termos(texto));
            tfCandidatos.put(id, tf);
            tf.keySet().forEach(termo -> documentosComTermo.merge(termo, 1, Integer::sum));
        });

        int totalDocumentos = candidatos.size() + 1;
        Map<String, Double> idf = new HashMap<>();
        documentosComTermo.forEach((termo, documentos) ->
                idf.put(termo, Math.log((totalDocumentos + 1.0) / (documentos + 1.0)) + 1.0));

        double normaReferencia = Math.sqrt(tfReferencia.entrySet().stream()
                .mapToDouble(e -> Math.pow(peso(e.getValue(), idf.get(e.getKey())), 2)).sum());

        Map<Long, Resultado> resultados = new LinkedHashMap<>();
        tfCandidatos.forEach((id, tf) -> {
            double normaCandidato = Math.sqrt(tf.entrySet().stream()
                    .mapToDouble(e -> Math.pow(peso(e.getValue(), idf.get(e.getKey())), 2)).sum());
            Map<String, Double> contribuicoes = new HashMap<>();
            tf.forEach((termo, frequencia) -> {
                Integer naReferencia = tfReferencia.get(termo);
                if (naReferencia != null) {
                    double termoIdf = idf.get(termo);
                    contribuicoes.put(termo, peso(frequencia, termoIdf) * peso(naReferencia, termoIdf));
                }
            });
            double produto = contribuicoes.values().stream().mapToDouble(Double::doubleValue).sum();
            double cosseno = normaReferencia == 0 || normaCandidato == 0
                    ? 0
                    : produto / (normaReferencia * normaCandidato);
            List<String> emComum = contribuicoes.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(4)
                    .map(e -> exibicao.getOrDefault(e.getKey(), e.getKey()))
                    .toList();
            resultados.put(id, new Resultado(Math.max(0, Math.min(1, cosseno)), emComum));
        });
        return resultados;
    }
}
