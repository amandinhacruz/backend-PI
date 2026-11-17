package com.projeto_pi.Projeto.PI.matching.ia;

import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade.PerfilInvestidor;
import com.projeto_pi.Projeto.PI.matching.ia.AnalistaAfinidade.PerfilStartup;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Monta as requisições ao Claude sem usar a rede. O SDK valida localmente o esquema JSON
 * derivado das classes de saída, então um esquema que a API recusaria falha aqui.
 */
class AnalistaClaudeTests {

    final IaProperties props = new IaProperties(true, "claude-opus-5-5", "medium", "1.1", true, 6, 18, 120);
    final AnalistaClaude analista = new AnalistaClaude(props, JsonMapper.builder().build());

    final PerfilInvestidor investidor = new PerfilInvestidor(List.of("Healthtech"), List.of("Ideação"),
            "de R$ 50.000 a R$ 200.000", "PE", "Investidora anjo", "Mercado financeiro", "Saúde digital");
    final PerfilStartup startup = new PerfilStartup("S7", "Healthtech", "Ideação",
            "de R$ 80.000 a R$ 150.000", "Recife/PE", "Telemedicina", "Faltam médicos", "IA", "Capital");

    @Test
    void analysisRequestCarriesModelSchemaFallbackAndProfiles() {
        var requisicao = analista.requisicaoAnalise(investidor, List.of(startup));
        String corpo = requisicao.rawParams()._body().toString();
        assertTrue(corpo.contains("claude-opus-5-5"));
        assertTrue(corpo.contains("Telemedicina"), "o perfil da startup vai na mensagem");
        assertTrue(corpo.contains("S7"));
        assertTrue(corpo.contains("aderenciaTese"), "o esquema de saída vai na requisição");
        assertTrue(corpo.contains("fallbacks=BetaFallbacksParam{default_=default}"), "fallback de recusa ligado");
        assertTrue(corpo.contains("effort=medium"));
        assertTrue(requisicao.rawParams()._headers().toString().contains(AnalistaClaude.BETA_FALLBACK));
    }

    @Test
    void dossierRequestBuildsWithValidSchema() {
        var requisicao = analista.requisicaoDossie(investidor, startup);
        assertTrue(requisicao.rawParams()._body().toString().contains("resumoExecutivo"));
    }

    @Test
    void analystIsOffWithoutKeyOrSwitch() {
        var desligado = new AnalistaClaude(
                new IaProperties(false, "claude-opus-5-5", "medium", "1.1", true, 6, 18, 120),
                JsonMapper.builder().build());
        assertFalse(desligado.disponivel());
    }
}
