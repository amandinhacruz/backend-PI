package com.projeto_pi.Projeto.PI.matching;

import com.projeto_pi.Projeto.PI.matching.Criterios.Faixa;
import com.projeto_pi.Projeto.PI.matching.ia.VersaoPolitica;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Regras puras do motor: critérios, similaridade de texto, aprendizado e versão da política. */
class MotorLocalTests {

    static Faixa faixa(Integer min, Integer max) {
        return new Faixa(min == null ? null : BigDecimal.valueOf(min), max == null ? null : BigDecimal.valueOf(max));
    }

    @Test
    void segmentMatchesByLabelPartOrLegacyEquivalent() {
        assertTrue(Criterios.segmentoCompativel("Healthtech", List.of("Fintech", "healthtech")));
        assertTrue(Criterios.segmentoCompativel("Saúde", List.of("Healthtech")));
        assertTrue(Criterios.segmentoCompativel("Agritech / Agronegócio", List.of("agronegocio")));
        assertFalse(Criterios.segmentoCompativel("Fintech", List.of("Healthtech", "Edtech")));
        assertFalse(Criterios.segmentoCompativel(null, List.of("Healthtech")));
        assertEquals(List.of("Fintech", "Web3 / Blockchain"), Criterios.lista("Fintech; Web3 / Blockchain;"));
    }

    @Test
    void stageAndTicketFollowTheMissingDataRules() {
        assertTrue(Criterios.estagioCompativel("ideação", List.of("Ideação", "Operação")));
        assertFalse(Criterios.estagioCompativel("Escala", List.of("Ideação")));
        assertTrue(Criterios.estagioCompativel("Escala", List.of()), "sem preferência aceita qualquer estágio");
        assertFalse(Criterios.estagioCompativel(null, List.of("Ideação")));

        assertTrue(Criterios.ticketCompativel(faixa(50, 200), faixa(200, 400)), "faixas que se encostam");
        assertFalse(Criterios.ticketCompativel(faixa(50, 200), faixa(201, 400)));
        assertTrue(Criterios.ticketCompativel(faixa(null, null), faixa(1, 2)), "investidor sem faixa não filtra");
        assertFalse(Criterios.ticketCompativel(faixa(50, 200), faixa(null, null)), "startup sem faixa não é verificável");
        assertTrue(Criterios.ticketCompativel(faixa(100, null), faixa(null, 150)));

        assertEquals(1.0, Criterios.coberturaTicket(faixa(50, 200), faixa(80, 150)));
        assertEquals(0.5, Criterios.coberturaTicket(faixa(50, 200), faixa(100, 300)));
        assertEquals(0.5, Criterios.coberturaTicket(faixa(null, null), faixa(100, 300)));
    }

    @Test
    void textSimilarityRanksTheCloserPitchFirst() {
        Map<Long, String> pitches = new LinkedHashMap<>();
        pitches.put(1L, "Plataforma de telemedicina com inteligência artificial para clínicas do interior");
        pitches.put(2L, "Marketplace de peças automotivas usadas");
        var resultado = SimilaridadeTexto.comparar(
                "Busco soluções de saúde digital, telemedicina e inteligências artificiais", pitches);
        assertTrue(resultado.get(1L).similaridade() > resultado.get(2L).similaridade());
        assertEquals(0, resultado.get(2L).similaridade());
        assertTrue(resultado.get(1L).termosEmComum().contains("telemedicina"));
        // plural e acento não atrapalham
        assertEquals(SimilaridadeTexto.radical("solucao"), SimilaridadeTexto.radical("solucoes"));
        assertEquals(0, SimilaridadeTexto.comparar("", pitches).get(1L).similaridade());
    }

    @Test
    void learnedPreferenceMovesWithDecisions() {
        var perfil = new AprendizadoService.PerfilAprendido();
        StartupResumo ideacao = resumo("Healthtech", "Ideação");
        StartupResumo operacao = resumo("Healthtech", "Operação");
        assertEquals(0, perfil.ajuste(ideacao), "sem histórico não há ajuste");

        for (int i = 0; i < 3; i++) {
            perfil.registrar("Healthtech", "Ideação", "PE", BigDecimal.valueOf(100000), true, 1);
            perfil.registrar("Healthtech", "Operação", "PE", BigDecimal.valueOf(100000), false, 1);
        }
        assertTrue(perfil.ajuste(ideacao) > 0);
        assertTrue(perfil.ajuste(operacao) < 0);
        assertTrue(Math.abs(perfil.ajuste(ideacao)) <= AprendizadoService.AJUSTE_MAX_PREFERENCIA);
    }

    @Test
    void policyVersionsCompareNumerically() {
        assertTrue(VersaoPolitica.atende("1.1", "1.1"));
        assertTrue(VersaoPolitica.atende("1.10", "1.9"));
        assertTrue(VersaoPolitica.atende("2.0", "1.1"));
        assertFalse(VersaoPolitica.atende("1.0", "1.1"));
        assertFalse(VersaoPolitica.atende(null, "1.1"));
        assertFalse(VersaoPolitica.atende("abc", "1.1"));
        assertTrue(VersaoPolitica.atende(null, ""));
    }

    static StartupResumo resumo(String segmento, String estagio) {
        return new StartupResumo(1L, "1.0", 1L, "X", segmento, estagio, "Recife", "PE", "resumo", null, null,
                null, null, BigDecimal.valueOf(80000), BigDecimal.valueOf(120000), null, null, null, null);
    }
}
