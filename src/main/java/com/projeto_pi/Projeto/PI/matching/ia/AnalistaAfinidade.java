package com.projeto_pi.Projeto.PI.matching.ia;

import java.util.List;

/**
 * Analista generativo do matchmaking (camada 3): compara a tese do investidor com o pitch das
 * startups que já passaram no filtro e devolve nota, justificativa e pontos de atenção.
 *
 * Os perfis trocados por esta interface são anônimos por construção: não têm nome, e-mail,
 * CPF/CNPJ, site nem links. Só dados de negócio saem do servidor (LGPD, minimização).
 */
public interface AnalistaAfinidade {

    /** Há chave configurada e o uso está habilitado. */
    boolean disponivel();

    String modelo();

    /**
     * Avalia cada startup contra a tese do investidor.
     *
     * @throws AnaliseIndisponivelException quando o modelo não pôde responder; quem chama usa a nota local
     */
    List<ResultadoAnalise> avaliar(PerfilInvestidor investidor, List<PerfilStartup> startups);

    /** Dossiê aprofundado de uma startup para um investidor. */
    Dossie dossie(PerfilInvestidor investidor, PerfilStartup startup);

    record PerfilInvestidor(
            List<String> segmentosDeInteresse,
            List<String> estagiosPreferidos,
            String faixaDeTicket,
            String uf,
            String sobre,
            String carreira,
            String busca) {
    }

    /** @param ref identificador opaco da startup dentro da requisição (ex.: "S12") */
    record PerfilStartup(
            String ref,
            String segmento,
            String estagio,
            String faixaDeTicketBuscada,
            String local,
            String resumo,
            String problema,
            String diferenciais,
            String necessidades) {
    }

    record ResultadoAnalise(
            String ref,
            int nota,
            int aderenciaTese,
            int problemaMercado,
            int diferenciais,
            String justificativa,
            List<String> pontosFortes,
            List<String> pontosAtencao,
            List<String> perguntas) {
    }

    record Dossie(
            String resumoExecutivo,
            String aderenciaTese,
            List<String> oportunidades,
            List<String> riscos,
            List<String> perguntas,
            List<String> proximosPassos) {
    }
}
