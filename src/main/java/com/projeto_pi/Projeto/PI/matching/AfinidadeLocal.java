package com.projeto_pi.Projeto.PI.matching;

import com.projeto_pi.Projeto.PI.matching.Criterios.Faixa;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Afinidade calculada pelo motor interno (camada 2), entre startups que já passaram no filtro.
 *
 * Como toda startup elegível já bate em segmento, estágio e ticket, a nota começa em 60 e os
 * 40 pontos restantes vêm do que diferencia uma candidata da outra:
 * texto da tese x pitch (50%), cobertura da faixa de ticket (25%), completude do perfil (15%)
 * e proximidade geográfica (10%).
 */
public final class AfinidadeLocal {

    private AfinidadeLocal() {
    }

    static final int BASE_ELEGIVEL = 60;
    /** Cosseno a partir do qual o texto é considerado totalmente aderente (textos curtos raramente passam disso). */
    static final double COSSENO_PLENO = 0.30;

    public record Resultado(int nota, double texto, double ticket, double completude, double proximidade,
                            List<String> termosEmComum) {
    }

    public static Map<Long, Resultado> calcular(PreferenciasInvestidor investidor, List<StartupResumo> elegiveis) {
        Map<Long, String> textos = new LinkedHashMap<>();
        for (StartupResumo startup : elegiveis) {
            textos.put(startup.id(), textoDoPitch(startup));
        }
        Map<Long, SimilaridadeTexto.Resultado> similaridades =
                SimilaridadeTexto.comparar(investidor.textoTese(), textos);

        Map<Long, Resultado> resultados = new LinkedHashMap<>();
        for (StartupResumo startup : elegiveis) {
            SimilaridadeTexto.Resultado similaridade = similaridades.get(startup.id());
            double texto = Math.min(1, similaridade.similaridade() / COSSENO_PLENO);
            double ticket = Criterios.coberturaTicket(
                    investidor.ticket(), new Faixa(startup.ticketMin(), startup.ticketMax()));
            double completude = completude(startup);
            double proximidade = proximidade(investidor.uf(), startup.uf());
            double diferencial = 0.50 * texto + 0.25 * ticket + 0.15 * completude + 0.10 * proximidade;
            int nota = (int) Math.round(BASE_ELEGIVEL + (100 - BASE_ELEGIVEL) * diferencial);
            resultados.put(startup.id(), new Resultado(
                    Math.max(0, Math.min(100, nota)), texto, ticket, completude, proximidade,
                    similaridade.termosEmComum()));
        }
        return resultados;
    }

    static String textoDoPitch(StartupResumo startup) {
        return Stream.of(startup.resumo(), startup.problema(), startup.diferenciais(),
                        startup.necessidades(), startup.pitch(), startup.segmento())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    /** Fração dos itens de perfil que a startup preencheu. */
    static double completude(StartupResumo startup) {
        List<String> itens = Stream.of(startup.resumo(), startup.problema(), startup.diferenciais(),
                startup.website(), startup.video(), startup.pitchDeckNome(), startup.canvasNome())
                .map(valor -> valor == null ? "" : valor.trim())
                .toList();
        long preenchidos = itens.stream().filter(valor -> !valor.isEmpty()).count();
        return (double) preenchidos / itens.size();
    }

    /** 1 no mesmo estado, 0 em estados diferentes, 0,5 quando algum lado não informou. */
    static double proximidade(String ufInvestidor, String ufStartup) {
        String a = Criterios.norm(ufInvestidor);
        String b = Criterios.norm(ufStartup);
        if (a.isEmpty() || b.isEmpty()) {
            return 0.5;
        }
        return a.equals(b) ? 1 : 0;
    }
}
