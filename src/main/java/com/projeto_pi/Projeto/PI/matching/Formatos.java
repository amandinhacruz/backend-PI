package com.projeto_pi.Projeto.PI.matching;

import java.math.BigDecimal;
import java.util.Locale;

/** Formatação de textos exibidos ou enviados ao analista. */
public final class Formatos {

    private Formatos() {
    }

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    public static String moeda(BigDecimal valor) {
        return "R$ " + String.format(PT_BR, "%,.0f", valor);
    }

    public static String faixa(BigDecimal min, BigDecimal max) {
        if (min != null && max != null) {
            return "de " + moeda(min) + " a " + moeda(max);
        }
        if (min != null) {
            return "a partir de " + moeda(min);
        }
        if (max != null) {
            return "até " + moeda(max);
        }
        return "não informada";
    }

    public static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    public static String cortar(String valor, int limite) {
        String limpo = texto(valor);
        return limpo.length() <= limite ? limpo : limpo.substring(0, limite).trim() + "…";
    }
}
