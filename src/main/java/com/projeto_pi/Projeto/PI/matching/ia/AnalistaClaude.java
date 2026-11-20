package com.projeto_pi.Projeto.PI.matching.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonSchemaLocalValidation;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.beta.messages.BetaCacheControlEphemeral;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaTextBlockParam;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.anthropic.models.beta.messages.StructuredMessage;
import com.anthropic.models.beta.messages.StructuredMessageCreateParams;
import com.anthropic.models.beta.messages.StructuredTextBlock;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Analista generativo implementado com o Claude (SDK oficial da Anthropic).
 *
 * - Saída estruturada: o modelo devolve JSON validado contra as classes {@link LoteAnalise} e
 *   {@link DossieGerado}, sem parsing manual.
 * - Prompt em cache: as instruções são idênticas em toda chamada e ficam em cache no provedor,
 *   o que barateia os lotes seguintes.
 * - Recusas: nos modelos que têm o recurso ({@link #MODELOS_COM_FALLBACK}), o provedor tenta um
 *   modelo alternativo ("fallbacks"). Se ainda assim não houver resposta, lançamos
 *   {@link AnaliseIndisponivelException} e a plataforma usa a nota local.
 *
 * A chave é lida da variável de ambiente ANTHROPIC_API_KEY. Ela nunca vai para arquivo de configuração.
 */
@Component
public class AnalistaClaude implements AnalistaAfinidade {

    /** Cabeçalho beta que habilita o modelo alternativo do lado do servidor em caso de recusa. */
    static final String BETA_FALLBACK = "server-side-fallback-2026-07-01";

    /**
     * Modelos que aceitam o modelo alternativo do lado do servidor. Os demais (ex.: Claude Haiku 5.5)
     * não têm o recurso: neles o parâmetro não teria efeito, então não é enviado e uma recusa
     * cai direto na nota local.
     */
    static final java.util.Set<String> MODELOS_COM_FALLBACK = java.util.Set.of(
            "claude-fable-5-2", "claude-fable-5-1", "claude-mythos-5-2", "claude-mythos-5-1",
            "claude-opus-5-5", "claude-opus-5", "claude-sonnet-5-5");
    private static final long MAX_TOKENS = 16000L;

    private final IaProperties props;
    private final JsonMapper json;
    private volatile AnthropicClient cliente;

    public AnalistaClaude(IaProperties props, JsonMapper json) {
        this.props = props;
        this.json = json;
    }

    public static boolean chaveConfigurada() {
        return naoVazio(System.getenv("ANTHROPIC_API_KEY")) || naoVazio(System.getenv("ANTHROPIC_AUTH_TOKEN"));
    }

    private static boolean naoVazio(String valor) {
        return valor != null && !valor.isBlank();
    }

    @Override
    public boolean disponivel() {
        return props.habilitada() && chaveConfigurada();
    }

    @Override
    public String modelo() {
        return props.modelo();
    }

    @Override
    public List<ResultadoAnalise> avaliar(PerfilInvestidor investidor, List<PerfilStartup> startups) {
        LoteAnalise lote = executar(requisicaoAnalise(investidor, startups));
        if (lote.analises() == null) {
            return List.of();
        }
        return lote.analises().stream()
                .map(item -> new ResultadoAnalise(item.ref(), item.nota(), item.aderenciaTese(),
                        item.problemaMercado(), item.diferenciais(), item.justificativa(),
                        item.pontosFortes(), item.pontosAtencao(), item.perguntas()))
                .toList();
    }

    @Override
    public Dossie dossie(PerfilInvestidor investidor, PerfilStartup startup) {
        DossieGerado gerado = executar(requisicaoDossie(investidor, startup));
        return new Dossie(gerado.resumoExecutivo(), gerado.aderenciaTese(), gerado.oportunidades(),
                gerado.riscos(), gerado.perguntas(), gerado.proximosPassos());
    }

    StructuredMessageCreateParams<LoteAnalise> requisicaoAnalise(PerfilInvestidor investidor,
                                                                List<PerfilStartup> startups) {
        return base(Prompts.ANALISTA)
                .outputConfig(LoteAnalise.class, esforco(), JsonSchemaLocalValidation.YES)
                .addUserMessage(json.writeValueAsString(new EntradaAnalise(investidor, startups)))
                .build();
    }

    StructuredMessageCreateParams<DossieGerado> requisicaoDossie(PerfilInvestidor investidor,
                                                                PerfilStartup startup) {
        return base(Prompts.DOSSIE)
                .outputConfig(DossieGerado.class, esforco(), JsonSchemaLocalValidation.YES)
                .addUserMessage(json.writeValueAsString(new EntradaDossie(investidor, startup)))
                .build();
    }

    static boolean suportaFallback(String modelo) {
        return modelo != null && MODELOS_COM_FALLBACK.contains(modelo.trim());
    }

    private MessageCreateParams.Builder base(String instrucoes) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(props.modelo())
                .maxTokens(MAX_TOKENS)
                .systemOfBetaTextBlockParams(List.of(BetaTextBlockParam.builder()
                        .text(instrucoes)
                        .cacheControl(BetaCacheControlEphemeral.builder().build())
                        .build()));
        if (suportaFallback(props.modelo())) {
            builder.addBeta(BETA_FALLBACK).fallbacksDefault();
        }
        return builder;
    }

    private BetaOutputConfig.Effort esforco() {
        String valor = props.esforco() == null ? "" : props.esforco().trim().toLowerCase(Locale.ROOT);
        return switch (valor) {
            case "low" -> BetaOutputConfig.Effort.LOW;
            case "high" -> BetaOutputConfig.Effort.HIGH;
            case "xhigh" -> BetaOutputConfig.Effort.XHIGH;
            case "max" -> BetaOutputConfig.Effort.MAX;
            default -> BetaOutputConfig.Effort.MEDIUM;
        };
    }

    private <T> T executar(StructuredMessageCreateParams<T> requisicao) {
        try {
            StructuredMessage<T> resposta = cliente().beta().messages().create(requisicao);
            verificarParada(resposta.stopReason());
            return resposta.content().stream()
                    .flatMap(bloco -> bloco.text().stream())
                    .map(StructuredTextBlock::text)
                    .findFirst()
                    .orElseThrow(() -> new AnaliseIndisponivelException("A resposta do modelo veio sem conteúdo."));
        } catch (AnaliseIndisponivelException e) {
            throw e;
        } catch (RateLimitException e) {
            throw new AnaliseIndisponivelException("Limite de requisições da API do Claude atingido.", e);
        } catch (AnthropicServiceException e) {
            throw new AnaliseIndisponivelException(
                    "A API do Claude respondeu com erro HTTP " + e.statusCode() + ".", e);
        } catch (AnthropicIoException e) {
            throw new AnaliseIndisponivelException("Falha de rede ao chamar a API do Claude.", e);
        } catch (AnthropicException e) {
            throw new AnaliseIndisponivelException(
                    "Falha ao usar a API do Claude (" + e.getClass().getSimpleName() + ").", e);
        }
    }

    private static void verificarParada(Optional<BetaStopReason> motivo) {
        if (motivo.isEmpty()) {
            return;
        }
        if (BetaStopReason.REFUSAL.equals(motivo.get())) {
            throw new AnaliseIndisponivelException("O modelo recusou a análise.");
        }
        if (BetaStopReason.MAX_TOKENS.equals(motivo.get())) {
            throw new AnaliseIndisponivelException("A resposta do modelo foi cortada pelo limite de tokens.");
        }
    }

    private AnthropicClient cliente() {
        AnthropicClient atual = cliente;
        if (atual == null) {
            synchronized (this) {
                if (cliente == null) {
                    cliente = AnthropicOkHttpClient.builder()
                            .fromEnv()
                            .timeout(Duration.ofSeconds(props.timeoutSegundos()))
                            .build();
                }
                atual = cliente;
            }
        }
        return atual;
    }

    /* ------------------------------------------------------------- formato das mensagens */

    record EntradaAnalise(PerfilInvestidor investidor, List<PerfilStartup> startups) {
    }

    record EntradaDossie(PerfilInvestidor investidor, PerfilStartup startup) {
    }

    public record LoteAnalise(
            @JsonPropertyDescription("Uma análise para cada startup recebida")
            List<ItemAnalise> analises) {
    }

    public record ItemAnalise(
            @JsonPropertyDescription("O mesmo ref da startup recebida, por exemplo S12")
            String ref,
            @JsonPropertyDescription("Avaliação geral da combinação, inteiro de 0 a 100")
            int nota,
            @JsonPropertyDescription("Aderência à tese do investidor, inteiro de 0 a 100")
            int aderenciaTese,
            @JsonPropertyDescription("Clareza e relevância do problema resolvido, inteiro de 0 a 100")
            int problemaMercado,
            @JsonPropertyDescription("Força dos diferenciais declarados, inteiro de 0 a 100")
            int diferenciais,
            @JsonPropertyDescription("Uma ou duas frases, em terceira pessoa, explicando a combinação")
            String justificativa,
            @JsonPropertyDescription("Até 3 pontos fortes, curtos")
            List<String> pontosFortes,
            @JsonPropertyDescription("Até 2 pontos de atenção, curtos")
            List<String> pontosAtencao,
            @JsonPropertyDescription("Até 3 perguntas para a primeira conversa")
            List<String> perguntas) {
    }

    public record DossieGerado(
            @JsonPropertyDescription("De 2 a 4 frases sobre o que a startup faz, para quem e em que momento está")
            String resumoExecutivo,
            @JsonPropertyDescription("De 2 a 3 frases sobre a relação da startup com a tese do investidor")
            String aderenciaTese,
            @JsonPropertyDescription("Até 4 razões concretas para o investidor se interessar")
            List<String> oportunidades,
            @JsonPropertyDescription("Até 4 lacunas, incertezas ou pontos não esclarecidos")
            List<String> riscos,
            @JsonPropertyDescription("Até 5 perguntas para a primeira conversa")
            List<String> perguntas,
            @JsonPropertyDescription("Até 3 ações práticas dentro da plataforma")
            List<String> proximosPassos) {
    }
}
