package com.projeto_pi.Projeto.PI.matching.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração do analista generativo (ver application.properties e docs/IA.md).
 *
 * @param habilitada      liga o uso do Claude; desligada por padrão. A cláusula 5.4 da Política de
 *                        Privacidade exige atualizar o contrato antes de ligar.
 * @param modelo          modelo usado nas análises e no dossiê
 * @param esforco         profundidade de raciocínio do modelo: low, medium, high, xhigh ou max
 * @param politicaMinima  versão mínima da Política que o usuário precisa ter aceitado para que o perfil
 *                        dele seja enviado ao analista generativo
 * @param assincrono      true: as análises rodam em segundo plano e a tela não espera pela IA
 * @param lote            startups avaliadas por chamada ao modelo
 * @param maxPorConsulta  teto de startups novas enviadas ao modelo a cada carregamento da fila
 * @param timeoutSegundos tempo máximo de cada chamada
 */
@ConfigurationProperties(prefix = "vertice.ia")
public record IaProperties(
        @DefaultValue("false") boolean habilitada,
        @DefaultValue("claude-opus-5-5") String modelo,
        @DefaultValue("medium") String esforco,
        @DefaultValue("1.1") String politicaMinima,
        @DefaultValue("true") boolean assincrono,
        @DefaultValue("6") int lote,
        @DefaultValue("18") int maxPorConsulta,
        @DefaultValue("120") int timeoutSegundos) {
}
