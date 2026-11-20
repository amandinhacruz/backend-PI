# Relatório de QA — adequação backend Vértice

Data: 11/11/2026. Escopo: login, cadastro de startups/investidores e feed de empresas. Base: código dos dois projetos anexados e documento de kickoff, especialmente atores (seção 1.3), arquitetura/DTOs (2.1–2.5), segurança e testes (2.7) e matriz de requisitos (6.2). O kickoff é uma linha de base e não contém critérios detalhados de aceitação; as regras abaixo combinam o documento com o comportamento do código existente.

## Resultado executado

**26 testes executados, 26 aprovados, 0 falhas, 0 erros, 0 ignorados.** São 25 execuções de integração, incluindo parametrizações, e 1 teste de inicialização. Evidências brutas em `docs/evidencias/`.

Comando: `mvnw.cmd -o test -q`, após baixar as dependências. Para uma máquina sem cache, usar `mvnw.cmd test`. Ambiente: Windows, Java 21, Spring Boot 4.1.1, MockMvc com cadeia real de segurança, serviços/repositórios reais e H2 em modo PostgreSQL. Transações dos casos são revertidas; o administrador inicial é removido dentro de cada transação de teste para não contaminar contagens.

Os testes usam requisições HTTP simuladas pelo Spring, sem servidor externo e sem navegador. A chave AES do Surefire é exclusiva de teste. O runner SQL legado fica desativado nesse ambiente.

## Defeitos encontrados e tratados

| ID | Evidência no código recebido | Correção |
|---|---|---|
| D01 | Login envia `password`; DTO espera `senha` | Alias `password`, preservando `senha` |
| D02 | Etapas enviam `name`, `segment`, `stage`, tickets e demais dados sem correspondência | Aliases e persistência dos campos de perfil |
| D03 | Formulário alternativo produz FormData/arquivos, mas back aceita somente JSON | Multipart com campos portugueses/ingleses e materiais criptografados |
| D04 | `/startups/**` exige STARTUP; o feed está na área INVESTIDOR | Leitura para INVESTIDOR, STARTUP e ADMIN; investidor continua sem escrita |
| D05 | Listagem retorna entidade em português e não o tipo do cartão React | DTO com `id,name,location,segment,stage,description,tone` |
| D06 | Listagem inclui startups sem aprovação | Consulta somente perfis vinculados a usuários aprovados |
| D07 | Falhas de login lançam RuntimeException e podem gerar 500 | 400/401/403 e mensagem padronizada nas exceções tratadas |
| D08 | Ausência de configuração CORS para Vite | Origens permitidas configuráveis e preflight validado |
| D09 | Duplicidade depende da caixa e não há validação consistente de tickets/credenciais | Normalização, busca de e-mail sem distinguir caixa e validação antes da gravação |
| D10 | Teste recebido depende de PostgreSQL local | Base H2 descartável e configuração isolada |

## Casos executados e rastreabilidade

| Caso/método em IntegrationContractTests | Execuções | Requisito/objetivo | Resultado |
|---|---:|---|---|
| registrationPersistsActualReactFieldsAndHashedPassword | 1 | RF01, RF11, RNF02: perfil, hash BCrypt, consentimento versionado, CPF/CNPJ protegido em repouso (CNPJ exercitado) | Aprovado |
| legacyPortugueseRegistrationStillWorks | 1 | RF01: compatibilidade com contrato português | Aprovado |
| investorReactStepsPersistProfileAndTickets | 1 | RF02: campos de investidor e tipo padrão | Aprovado |
| duplicateEmailIsCaseInsensitiveAndDoesNotCreateOrphan | 1 | RF01: duplicidade 409, sem perfil órfão | Aprovado |
| invalidRegistrationDoesNotPersist | 8 | RF01/RF11: consentimento falso, e-mail inválido, senha/nome vazios, tickets invertidos, confirmação diferente, termos/privacidade falsos | Aprovado |
| loginAcceptsPasswordAndSenhaAndNormalizesEmail | 1 | RF07: login aprovado nos dois formatos, com normalização | Aprovado |
| pendingAndRejectedUsersCannotLogin | 1 | RF07/RF08: estados não aprovados recebem 403 | Aprovado |
| invalidCredentialsReturn401 | 2 | RF07: conta inexistente e senha incorreta | Aprovado |
| investorCanReadApprovedFeedWithoutPersonalData | 1 | RF04/RF08: feed aprovado, formato do cartão, sem dados pessoais | Aprovado |
| feedFiltersMatchFrontend | 1 | RF04: busca e filtros combinados, resultados vazios | Aprovado |
| anonymousAndInvalidTokensCannotReadFeed | 1 | RF07: acesso sem token/com token inválido recebe 401 | Aprovado |
| investorCannotWriteStartup | 1 | RF07: investidor recebe 403 ao escrever | Aprovado |
| unapprovedAndMissingProfilesAre404 | 1 | RF04/RF08: detalhe aprovado, ocultação de não aprovados e 404 | Aprovado |
| vitePreflightIsAllowed | 1 | RNF09: CORS do Vite permitido; origem não confiável rejeitada | Aprovado |
| multipartFormFieldsAndFilesArePersisted | 1 | RF01/RF06: aliases FormData, arquivos persistidos, leitura após conversão criptográfica e download autenticado | Aprovado |
| invalidUploadIsRejectedBeforePersistence | 1 | RF06: extensão não permitida gera 400 sem cadastro | Aprovado |
| malformedJsonReturnsReadable400 | 1 | RNF04: JSON inválido recebe erro legível | Aprovado |
| ProjetoPiApplicationTests.contextLoads | 1 | RNF07: inicialização do contexto | Aprovado |

RF04 é usado aqui para a listagem de perfis. A compatibilidade por afinidade do motor RF03 não é avaliada nem substituída pelo feed geral.

## Pendências observadas para integração e homologação

| Prioridade | Achado | Encaminhamento |
|---|---|---|
| Alta | Front sem callbacks conectados e sem cliente HTTP nos fluxos; feed usa array fixo | Conectar login, acumular etapas e enviar cadastro completo; buscar o feed com Bearer token. Exemplos em INTEGRACAO.md |
| Alta | O filtro de estágio do front fixa `Captação`, enquanto cadastro oferece MVP, Tração etc. | Montar opções com valores reais da API |
| Alta | Cadastro inicial fornece somente credenciais; perfil e consentimentos vêm depois | Guardar dados entre etapas e submeter ao final. A API não cria rascunhos |
| Alta | Código legado contém chave JWT fixa e cria administrador com senha fixa | Remover esses padrões antes de exposição pública; não é correção de contrato realizada nesta entrega |
| Alta | Endpoints legados de edição de startups validam papel, mas não propriedade do perfil | Fazer revisão de autorização por recurso antes de uso com dados reais; escopo automatizado aqui é leitura do feed e bloqueio de escrita para investidores |
| Média | Consentimentos do front começam selecionados | Revisar a experiência de aceite explícito; backend não consegue distinguir clique do usuário de valor pré-selecionado |
| Média | Arquivos validados por tamanho/extensão | Não há inspeção interna/antimalware; avaliar antes de aceitar uploads públicos |
| Média | Feed sem paginação e com filtros após descriptografia | Medir com volume real antes de ampliar o uso |

Não foram executados: PostgreSQL real e migração em base existente; testes de interface/ponta a ponta; carga, responsividade, acessibilidade ou beta testers; auditoria completa de segurança; teste de expiração do JWT; concorrência de cadastro; teste com arquivo real de 10 MB; validação fiscal de CNPJ/CPF. Os cenários automatizados usam pequenos arquivos de teste, não documentos Office/PDF completos.

Cobertura percentual não foi medida. Portanto, não se afirma o cumprimento da meta de 60% de cobertura de todas as classes de serviço mencionada no kickoff. A execução local também não equivale a aprovação de PR/pipeline ou homologação do produto inteiro.

Conclusão de QA: as rotas adequadas passaram na suíte local de integração e estão prontas para a equipe conectar o front e realizar homologação com PostgreSQL. A validação completa da plataforma e a liberação pública dependem das pendências descritas acima.
