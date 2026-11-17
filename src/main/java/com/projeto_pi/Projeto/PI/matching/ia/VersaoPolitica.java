package com.projeto_pi.Projeto.PI.matching.ia;

/** Comparação de versões da Política de Privacidade ("1.0", "1.1", "2.0"...). */
public final class VersaoPolitica {

    private VersaoPolitica() {
    }

    /**
     * O usuário aceitou uma versão igual ou posterior à mínima exigida?
     * Versão ausente ou ilegível conta como não aceita.
     */
    public static boolean atende(String consentida, String minima) {
        if (minima == null || minima.isBlank()) {
            return true;
        }
        if (consentida == null || consentida.isBlank()) {
            return false;
        }
        try {
            return comparar(consentida.trim(), minima.trim()) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int comparar(String a, String b) {
        String[] partesA = a.split("\\.");
        String[] partesB = b.split("\\.");
        int tamanho = Math.max(partesA.length, partesB.length);
        for (int i = 0; i < tamanho; i++) {
            int numeroA = i < partesA.length ? Integer.parseInt(partesA[i]) : 0;
            int numeroB = i < partesB.length ? Integer.parseInt(partesB[i]) : 0;
            if (numeroA != numeroB) {
                return Integer.compare(numeroA, numeroB);
            }
        }
        return 0;
    }
}
