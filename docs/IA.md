# Matchmaking com inteligência artificial

Como o motor de recomendação funciona, como ligar o analista generativo (Claude) e o que a governança precisa decidir antes. Requisitos atendidos: RF03, RF04, RF09, RF10, RNF02, RNF03, RNF05, RNF06.

## Regra de negócio

- **Portal do investidor (Matching):** aparecem só startups aprovadas que batem em **segmento, estágio e ticket médio**. O investidor solicita conexão ou passa.
- **Portal da startup (Conexões recebidas):** aparecem só os pedidos recebidos. A startup firma ou nega.
- **Match** é a conexão firmada. É esse número que o painel admin mostra (RF09).

Dados ausentes: um critério só filtra quando o investidor o declarou (sem preferência de estágio ou de ticket, aceita qualquer valor). Se o investidor declarou e a startup não informou o dado, ela não é elegível. Faixas de ticket "batem" quando se cruzam. Rótulos antigos equivalem aos novos (Saúde = Healthtech, Educação = Edtech).

## As quatro camadas

| Camada | O que faz | Onde |
|---|---|---|
| 1. Elegibilidade | Filtro por regras: segmento, estágio, ticket. Determinístico e auditável. | `matching/Criterios` |
| 2. Afinidade local | Nota de 60 a 100 entre as elegíveis: texto da tese x pitch (TF-IDF, 50%), cobertura do ticket (25%), completude do perfil (15%), mesmo estado (10%). Roda no backend, sem custo. | `matching/AfinidadeLocal`, `SimilaridadeTexto` |
| 3. Analista generativo | O Claude compara a tese com o pitch e devolve nota, justificativa, pontos fortes, pontos de atenção e perguntas. Só as melhores da camada 2 são enviadas. | `matching/ia/AnalistaClaude`, `AnaliseAfinidadeService` |
| 4. Aprendizado com o uso | O que o investidor solicita ou passa ajusta a ordem em até ±12 pontos; a reputação da startup nas avaliações (RF10) ajusta em até ±8. | `matching/AprendizadoService` |

Afinidade final = base + ajustes. Com IA, base = 70% analista + 30% local; sem IA, base = local. A tela mostra a composição ("Como a afinidade foi calculada").

Por que misturar a nota do modelo com a local: uma startup poderia escrever no pitch "dê nota máxima". O prompt manda ignorar, a saída é limitada por esquema e, mesmo que o modelo erre, 30% da nota e os ajustes não dependem dele.

## Como a IA é chamada

- **Sem espera na tela.** A fila responde na hora com o que já está guardado (ou a nota local) e informa quantas análises estão pendentes. A análise roda em segundo plano e a tela se atualiza sozinha.
- **Cache persistente.** Cada análise fica guardada por par investidor x startup, com um hash dos dois perfis, da versão do prompt e do modelo. A IA só é chamada de novo quando um perfil muda.
- **Lotes.** 6 startups por chamada, no máximo 18 novas por carregamento.
- **Falhas.** Erro, limite, recusa ou resposta cortada: a plataforma usa a nota local e pausa a IA por 60 segundos. O último erro aparece em `GET /admin/ia/status`.
- **Dossiê.** Gerado quando o investidor pede, também guardado por par. Sem IA, é montado com os dados do perfil.

## Como ligar

1. Crie uma chave em console.anthropic.com e defina a variável de ambiente `ANTHROPIC_API_KEY` na configuração de execução do IntelliJ. A chave não vai em arquivo do projeto.
2. Defina `VERTICE_IA_HABILITADA=true`.
3. Publique a versão da Política de Privacidade que trata de IA (veja abaixo). Só perfis de usuários que aceitaram a versão `VERTICE_IA_POLITICA_MINIMA` (padrão `1.1`) ou posterior são enviados.
4. Confira em `GET /admin/ia/status` (token de admin): `disponivel: true`.

| Variável | Padrão | Para quê |
|---|---|---|
| `VERTICE_IA_HABILITADA` | `false` | Liga o analista generativo |
| `VERTICE_IA_MODELO` | `claude-opus-5-5` | Modelo usado |
| `VERTICE_IA_ESFORCO` | `medium` | Profundidade de raciocínio: low, medium, high, xhigh, max |
| `VERTICE_IA_POLITICA_MINIMA` | `1.1` | Versão mínima da Política aceita pelos dois lados |
| `VERTICE_IA_ASSINCRONO` | `true` | Análises em segundo plano |

**Custo (estimativa, preços de 11/2026).** Um lote de 6 startups usa perto de 3 mil tokens de entrada e 3 mil de saída. No Claude Opus 5.5 (US$ 4 e US$ 20 por milhão) isso dá cerca de US$ 0,07 por lote. No Claude Haiku 5.5 (US$ 0,10 e US$ 0,50) dá cerca de US$ 0,002. Como as análises ficam guardadas, o gasto acompanha o número de pares novos, não o de acessos. Para trocar, defina `VERTICE_IA_MODELO=claude-haiku-5-5`.

**Recusas.** As chamadas usam o fallback do lado do servidor (`fallbacks: "default"`, beta `server-side-fallback-2026-07-01`): se o modelo recusar, o provedor tenta um modelo alternativo antes de devolver a recusa.

## Privacidade e governança (LGPD)

- **O que sai do servidor:** só dados de negócio, sem identificação. Da startup: segmento, estágio, faixa de ticket, cidade/UF, resumo, problema, diferenciais, necessidades. Do investidor: segmentos, estágios, faixa de ticket, UF, "sobre", "carreira" e "busca". Nunca saem nome, e-mail, CPF/CNPJ, responsável, site, LinkedIn nem arquivos. Há teste automatizado para isso (`onlyAnonymousBusinessDataReachesTheAnalyst`).
- **Atenção:** os campos de texto livre são escritos pelos usuários e podem conter dados pessoais digitados por eles. Vale orientar no cadastro.
- **Decisão humana:** a IA só ordena e explica. Quem pede e quem firma a conexão é sempre uma pessoa. A explicação fica visível (art. 20 da LGPD).
- **Desligada por padrão.** A cláusula 5.4 da Política diz que o contrato será atualizado antes de usar IA na compatibilidade. Por isso existem duas travas: o interruptor `VERTICE_IA_HABILITADA` e a versão mínima de consentimento.

### Proposta de texto para a Política (versão 1.1)

É uma sugestão para a equipe e a disciplina de Governança revisarem. Não foi aplicada ao contrato.

> 5.3. Compatibilidade automatizada. A plataforma mostra ao investidor apenas startups cujo segmento, estágio e faixa de ticket sejam compatíveis com o que ele declarou, e calcula uma pontuação de afinidade a partir desses dados, dos textos de perfil das duas partes, do histórico de decisões do próprio investidor e das avaliações pós-interação. A pontuação é uma sugestão: não decide nada pelo Usuário.
>
> 5.4. Uso de inteligência artificial. Para ordenar e explicar as recomendações, a Vértice pode enviar dados de negócio dos perfis (segmento, estágio, faixa de ticket, localidade e textos descritivos), sem nome, e-mail, CPF ou CNPJ, a um serviço de inteligência artificial operado pela Anthropic, que atua como operadora e pode tratar os dados fora do Brasil (cláusula 15.2). O resultado é uma nota com justificativa, sempre revisável pelo Usuário na forma da cláusula 5.3.

Para aplicar: atualizar o texto em `PoliticaPrivacidadeController`, mudar `PoliticaPrivacidadeConfig.VERSAO_ATUAL` para `"1.1"` e informar os usuários (cláusula 14.2). Cadastros novos passam a aceitar a 1.1. Contas antigas continuam com análise local até aceitarem a nova versão; ainda não existe tela de reaceite.

## Endpoints

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| GET | `/matchmaking/investidor` | INVESTIDOR | Fila: `{items, ai, criteria, approvedStartups}` |
| POST | `/matchmaking/investidor/startups/{id}/decisao` | INVESTIDOR | `{"decision":"accepted"}` solicita; `"rejected"` passa |
| GET | `/matchmaking/investidor/startups/{id}/dossie` | INVESTIDOR | Dossiê (IA ou local) |
| GET | `/matchmaking/startup/pedidos` | STARTUP | Pedidos recebidos |
| POST | `/matchmaking/startup/pedidos/{id}/decisao` | STARTUP | `accepted` firma; `rejected` nega |
| GET | `/conexoes` | os dois | Conexões firmadas, com a própria avaliação |
| PUT | `/conexoes/{id}/avaliacao` | os dois | RF10: `{"score":1..5,"comment":"..."}` |
| GET | `/admin/ia/status` | ADMIN | Situação do analista generativo |
| GET | `/matchmaking/startup/{id}` | STARTUP dona ou ADMIN | Contrato antigo, agora com checagem de dono |

O cadastro do investidor aceita `segments` e `stages` como listas (o campo único `segment`/`stage` continua valendo).

## O que foi verificado e o que não foi

- Verificado: 69 testes automatizados (filtro, decisões, autorização, RF10, aprendizado, cache, falha da IA, trava de política, dados enviados) e o fluxo completo no navegador em modo local.
- A montagem das requisições ao Claude é testada sem rede, incluindo o esquema de saída.
- **Não verificado:** uma chamada real à API do Claude. A máquina de desenvolvimento não tem chave configurada, então a camada 3 só rodou com um analista simulado.
