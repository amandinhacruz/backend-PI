# Integração: login, cadastro e feed

Entrega de adequação de 11/11/2026. Backend Java 21 / Spring Boot 4.1.1 conforme o código recebido. O kickoff cita Boot 3; a versão do projeto foi preservada.

## Preparação

1. Disponibilize PostgreSQL e configure `spring.datasource.*` (ou `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
2. Configure `VERTICE_ENCRYPTION_KEY` com 32 bytes em Base64. Em uma base existente, mantenha a mesma chave. Os novos campos de texto e arquivos usam a mesma criptografia AES/GCM do projeto.
3. Configure `CORS_ALLOWED_ORIGINS` com as origens do front separadas por vírgula. Padrão: `http://localhost:5173,http://127.0.0.1:5173`.
4. Execute `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).

O projeto recebido usa `ddl-auto=update`; ele cria as novas colunas em desenvolvimento. Não foi adicionado Flyway nem executada alteração em banco compartilhado. O runner de migração legado continua ativo em produção; apenas os testes o desativam porque contém SQL específico do PostgreSQL.

## O que o front anexado realmente faz

`Routes.tsx` monta login/cadastro/etapas sem callbacks. Não há cliente HTTP nesses fluxos. Login e cadastro inicial chamam `onLogin({email,password})`; as etapas chamam `onNext(values)`; a tela alternativa `SCompletRegister` chama `onSubmit(FormData)`. O feed `Investors/Pages/Startups.tsx` usa seis empresas fixas.

Assim, a adequação do back está pronta para ser consumida, mas o front ainda precisa conectar os callbacks, guardar temporariamente os dados entre etapas e substituir o array fixo por uma chamada HTTP. O trabalho realizado não altera esse front. Nenhum resultado aqui representa validação ponta a ponta no navegador.

> **Atualização (integração do front):** os callbacks, o rascunho entre etapas e o feed via HTTP foram conectados em `frontend-PI` (ver seção "Integração com o backend" no README do front). Os fluxos de cadastro de startup e investidor, login (aprovado, pendente, token inválido) e feed foram validados ponta a ponta no navegador contra PostgreSQL local. Testes de contrato do formato enviado pelo front foram adicionados em `IntegrationContractTests`.

## Login

`POST /auth/login`, `Content-Type: application/json`:

```json
{"email":"contato@exemplo.com","password":"sua-senha"}
```

`senha` continua aceito. E-mail é normalizado com trim e minúsculas; busca também ignora caixa para contas antigas. Resposta 200: `{ "token": "JWT", "email": "...", "papel": "STARTUP" }`. Credenciais incorretas: 401. Conta pendente/reprovada: 403. Corpo inválido/campo obrigatório ausente: 400. A aprovação administrativa continua obrigatória.

## Cadastro por etapas

Reúna as credenciais da tela inicial e os valores das etapas em **um objeto plano** e envie ao final. Enviar apenas `{email,password}` não conclui cadastro: faltam perfil e consentimento. Não há endpoint de rascunho nem criação de conta parcial.

`POST /auth/cadastro/startup`, JSON:

```json
{
  "email":"startup@exemplo.com", "password":"sua-senha",
  "name":"Minha Startup", "cnpj":"12345678000100", "website":"https://exemplo.com",
  "city":"Recife", "state":"PE", "foundedYear":"2026", "profileOwner":"Ana",
  "segment":"Fintech", "stage":"MVP", "minimumTicket":"1000", "maximumTicket":"5000",
  "summary":"Descrição da solução", "problem":"Problema resolvido", "competitiveAdvantages":"Diferenciais",
  "presentationVideo":"https://exemplo.com/video",
  "terms":true, "privacy":true, "dataProcessing":true
}
```

Arquivos `File` não devem ser serializados como JSON: use multipart. `corporateEmail` é um alias de `email`; envie apenas um deles como endereço da conta, evitando dois valores divergentes. `confirmPassword` é opcional para compatibilidade, mas é validado quando enviado. Os campos originais `nome`, `segmento`, `estagio`, `senha`, `necessidades`, `pitch`, `canvas`, `consentimentoLGPD` continuam aceitos. Campos de nome, segmento, estágio, credenciais e consentimento são obrigatórios. Campos complementares são preservados quando fornecidos, sem inventar validação fiscal de CNPJ/CPF.

`POST /auth/cadastro/investidor`, JSON:

```json
{
  "email":"investidor@exemplo.com", "password":"sua-senha", "name":"Ana",
  "cpf":"12345678900", "linkedin":"https://linkedin.com/in/exemplo", "city":"Recife", "state":"PE",
  "segment":"Fintech", "stage":"MVP", "minimumTicket":"1000", "maximumTicket":"5000",
  "aboutInvestor":"Perfil", "career":"Trajetória", "lookingFor":"Objetivos",
  "terms":true, "privacy":true, "dataProcessing":true
}
```

Os aliases convertem `segment` em `areasInteresse`, `stage` em `estagiosPref`, `minimumTicket` em `ticketMin`, `maximumTicket` em `ticketMax`. `tipo` assume `INVESTIDOR`, pois o front não o informa; envie `"tipo":"MENTOR"` para cadastrar um mentor. `disponibilidade` é mantida como campo legado opcional, sem mapear campos de significado diferente para ela. Área de interesse é obrigatória.

Consentimento `dataProcessing` / `consentimentoLGPD` precisa ser explicitamente verdadeiro. `terms` e `privacy`, quando fornecidos, não podem ser falsos. O contrato legado pode continuar informando apenas o consentimento unificado. A UI pré-seleciona os consentimentos: isso deve ser revisto pela equipe de produto; o backend só recebe os valores enviados.

Tickets são números decimais não negativos; mínimo não pode superar máximo. No JSON, omita valores opcionais vazios ou envie `null`; nos formulários multipart, valores vazios viram `null`. Não envie moeda formatada como `R$ 1.000,00`.

Sucesso: 201, `{id,email,papel,status:"PENDENTE"}` sem token. Duplicidade de e-mail: 409. Entradas inválidas: 400. Erros tratados retornam `{status,message}`; erros de autenticação na cadeia de segurança podem retornar corpo padrão do servidor, portanto o front deve tratar o status HTTP também.

## FormData e materiais

A mesma rota `/auth/cadastro/startup` aceita `multipart/form-data` com campos planos e arquivos `pitchDeck`, `businessModelCanvas` (ou `businessCanvas`). O navegador deve definir o boundary; não configure manualmente o cabeçalho Content-Type.

Aliases da tela alternativa: `nomeStartup`, `site`, `cidade`, `estado`, `anoFundacao`, `responsavel`, `resumo`, `problema`, `diferenciais`, `ticketMinimo`, `ticketMaximo`, `video`, `aceitaTermos`, `cientePrivacidade`, `autorizaTratamento`. Checkboxes multipart aceitam `on` ou `true`. Acrescente `password` das credenciais antes de enviar esse formulário, pois a tela alternativa não contém senha.

Arquivos opcionais de até 10 MB cada, 22 MB por requisição; extensões PDF/PPT/PPTX. Armazenados criptografados no banco. A verificação é de extensão e tamanho, sem análise antimalware ou inspeção do formato interno. Download autenticado de empresa aprovada: `GET /startups/{id}/materiais/pitch` ou `/canvas`. Material inexistente ou empresa não aprovada retorna 404. Downloads usam attachment e `nosniff`.

## Feed de empresas

`GET /startups`, com `Authorization: Bearer <token>`. Acesso de STARTUP, INVESTIDOR e ADMIN. Retorna um array, inclusive `[]` quando vazio:

```json
[{"id":"1","name":"Minha Startup","location":"Recife/PE","segment":"Fintech","stage":"MVP","description":"Descrição da solução","tone":"bg-[#7250f4]"}]
```

Esse formato corresponde ao tipo `Startup` do feed React. Apenas startups vinculadas a usuários APROVADO aparecem. Registros legados sem cidade/descrição retornam strings vazias nesses campos. O estágio vem do cadastro (MVP, Tração etc.); o filtro fixo `Captação` do front precisa ser populado com os valores reais da API.

Filtros opcionais combinados por AND: `?search=nexa&segment=Fintech&stage=MVP&location=Recife%2FPE`. Busca por nome ignora caixa e espaços externos; demais filtros são exatos, como no front. Sem paginação nesta entrega. Consulta e filtros são aplicados após descriptografia; validar desempenho antes de ampliar a base.

`GET /startups/{id}` retorna o mesmo DTO para empresas aprovadas; 404 para inexistentes/não aprovadas. O feed e o detalhe não expõem e-mail, CPF, CNPJ, responsáveis, senhas ou conteúdo binário. Escritas legadas continuam com a regra STARTUP; a revisão de propriedade de todos os endpoints de edição legados está fora desta adequação.

## Testes

Execute `mvnw.cmd test` no backend. H2 em modo PostgreSQL, schema descartável e chave exclusiva de teste são configurados automaticamente. Nenhum PostgreSQL real ou banco de produção é utilizado. A chave fixa no Surefire é exclusivamente de testes e não deve ser usada para iniciar a aplicação real.

O contrato OpenAPI das rotas deste escopo está em `docs/openapi-integracao.json`; importe em um cliente compatível. Não foi instalado Swagger UI.

## Atualização: regras de cadastro e painel administrativo

**Senha (cadastro):** mínimo 8 caracteres, com ao menos um número e um caractere especial. Senha fora da regra → 400. O login não aplica a regra, então contas existentes (ex.: seed do admin) continuam entrando.

**CPF/CNPJ:** máscaras são aceitas e removidas antes de gravar. O investidor pode enviar `cpf` (11 dígitos) **ou** `cnpj` (14 dígitos), nunca os dois (400). O CNPJ da startup também é normalizado. O backend valida a quantidade de dígitos; os dígitos verificadores são validados no front.

**Painel admin** (`/admin/**`, somente ADMIN):

| Método | Rota | Descrição |
| --- | --- | --- |
| GET | `/admin/usuarios?status=PENDENTE` | Fila de moderação: `{id,tipo,nome,responsavel,email,status,enviadoEm}` (sem senha/CPF/CNPJ) |
| PUT | `/admin/usuarios/{id}/aprovar` \| `rejeitar` \| `suspender` | Altera o status, registra auditoria e devolve o item (antes devolvia a entidade com `senhaHash`) |
| GET | `/admin/metricas` | Contagens reais de startups, investidores, pendências, conexões, conversas e reuniões, mais os percentuais de engajamento |
| GET | `/admin/auditoria` | Últimas 200 ações `{acao,alvo,ator,data}` |

Não existe registro persistido de "matches" (o matchmaking é calculado na hora). Por isso o painel mostra **conexões**: pares de usuários que trocaram mensagem ou agendaram reunião. Novas colunas e tabela (`usuarios.criado_em`, `investidores_mentores.cnpj`, `auditoria_admin`) são criadas pelo `ddl-auto=update`. Cadastros antigos usam a data de consentimento como data de envio.
