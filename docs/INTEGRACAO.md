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


## Atualização: mensagens e reuniões (RF05)

Só existe conversa entre os dois lados de uma **conexão firmada**, e o remetente é sempre o usuário do token. As rotas antigas `/mensagens` e `/agendamentos`, que recebiam o id do remetente no corpo e permitiam falar em nome de outro usuário, foram removidas.

| Método | Rota | Papel | Descrição |
| --- | --- | --- | --- |
| GET | `/conversas` | os dois | Conversas com última mensagem e não lidas |
| GET | `/conversas/{conexaoId}/mensagens` | os dois | Histórico; marca as recebidas como lidas |
| POST | `/conversas/{conexaoId}/mensagens` | os dois | `{"text":"..."}`, até 1000 caracteres |
| POST | `/conversas/{conexaoId}/convites` | INVESTIDOR | `{"date":"2026-11-24","time":"15:00","durationMinutes":30,"subject":"...","location":"..."}` |
| GET | `/reunioes` | os dois | Agenda do usuário |
| POST | `/reunioes/{id}/resposta` | STARTUP convidada | `{"decision":"accepted"}` ou `"rejected"` |
| POST | `/reunioes/{id}/cancelar` | os dois | Cancela reunião pendente ou confirmada que ainda não aconteceu |

Regras do convite: data e horário futuros e em até 6 meses; duração de 15 a 240 minutos; um convite aberto por conversa; não pode coincidir com outra reunião confirmada do investidor ou da startup (409). Aceite, recusa e cancelamento geram um aviso automático no chat. Mensagens e assunto/local das reuniões são criptografados em repouso.

## Atualização: perfis, foto, materiais e animação de match

**Perfis (STARTUP e INVESTIDOR)**

| Rota | Uso |
|---|---|
| `GET /perfil` | Perfil do usuário logado: `{role, startup, investor}` (só um preenchido). Inclui e-mail, CPF/CNPJ e responsável. |
| `PUT /perfil/startup` / `PUT /perfil/investidor` | Edição do próprio perfil (JSON). Nome, segmento(s) e estágio obrigatórios; ticket mínimo ≤ máximo; links só `http(s)`. Para o investidor, segmentos/estágios/ticket alimentam o matching. |
| `PUT /perfil/foto` (multipart `photo`) / `DELETE /perfil/foto` | Foto de perfil: JPG, PNG ou WebP, até 2 MB, conferidos pelo conteúdo do arquivo. Guardada criptografada (tabela `fotos_perfil`). |
| `PUT /perfil/materiais/{pitch\|canvas}` (multipart `file`) / `DELETE ...` | Pitch deck e Business Model Canvas da startup (PDF/PPT/PPTX, até 10 MB). |
| `GET /startups/{id}/perfil` | Perfil público de uma startup aprovada (botão "Visualizar Perfil" da página Startups → `/investors/startups/:id`). Sem e-mail, CNPJ nem responsável. |
| `GET /usuarios/{id}/foto` | Foto de um usuário aprovado, para usuários logados (`nosniff`, cache privado). O front busca com o token e usa *object URL*. |

As rotas antigas `POST /startups` e `PUT /startups/{id}` foram removidas: não conferiam o dono do perfil.

**Cadastro com foto**: `POST /auth/cadastro/startup` (multipart) aceita a parte `photo`; `POST /auth/cadastro/investidor` passou a aceitar multipart também (listas como campos repetidos: `segments=a&segments=b`). Sem foto, os dois continuam aceitando JSON.

**Anexos nos cartões de matching**: pelo RF06 ("permitir que a startup apresente seu pitch e modelo de negócio (Canvas) dentro do perfil") os anexos são **opcionais**. O `MatchCard` traz `hasPitchDeck`, `hasCanvas` e `presentationVideo`; o cartão só mostra os botões do que a startup enviou. Os downloads usam `GET /startups/{id}/materiais/{pitch|canvas}` (o CORS expõe `Content-Disposition` para o nome do arquivo). Cartões, conexões e conversas trazem `hasPhoto`.

**Animação de match**: ao firmar a conexão (startup) ou na primeira visita após a startup firmar (investidor, verificação a cada 15 s, conexões já vistas no `localStorage`), a tela é preenchida com o laranja `#EF5948`, que recua das margens para o centro e forma o símbolo do Vértice, com "Conexão bem-sucedida!" e o pop-up "Inicie a conversa" (abre `…/mensagens?conversa=<id>`). Respeita `prefers-reduced-motion`.

## Atualização: métricas do admin e animações

**Métricas (`GET /admin/metricas`) — o que mudou na validação**

| Indicador | Antes | Agora |
|---|---|---|
| Startups / investidores | total de cadastros (incluía pendentes e reprovados) | cartão mostra **aprovados** (`startupsAprovadas`, `investidoresAprovados`); total e "neste mês" no detalhe |
| Mensagens trocadas | contava convites de reunião e avisos automáticos | só mensagens escritas (`TipoMensagem.TEXTO`) |
| `conexoes` | pares com mensagem ou reunião | matches (conexões firmadas) |
| Reuniões agendadas | todos os convites (inclusive recusados e cancelados) | reuniões **aceitas**; convites e `reunioesAguardando` no detalhe |
| "Conexões com reunião agendada" | sempre ~100% (todo par com reunião também tem mensagem) | **Matches com reunião confirmada** = `matchesComReuniao` / matches |
| "Reuniões aceitas" | aceitas / todos os convites (inclusive os ainda sem resposta) | **Convites de reunião aceitos** = aceitas / (convites − aguardando resposta) |
| "Pedidos firmados pelas startups" | mesmo cálculo | renomeado para **Pedidos respondidos que viraram match** (pendentes ficam de fora) |

**Animações do front** (todas respeitam "reduzir movimento"):
- Métricas: números e barras contam de 0 até o valor ao abrir a página (`components/motion.ts`).
- Transição entre páginas: fade da página e subida leve do conteúdo (`.page-enter` em `index.css`). Os menus laterais passaram a usar `<Link>` (navegação sem recarregar) e o logo do menu leva à página inicial do portal (antes apontava para `/painel`, que não existe).
- Matching: troca de cartão com curva de aceleração e desaceleração (620 ms) e rastro de desfoque no auge da velocidade; o cartão que entra e o que sai também deslizam.
- Login (startup, investidor e admin): tela laranja recua até o centro, o símbolo se forma do centro para as pontas com brilho e o sistema abre por baixo (`components/LoginIntro.tsx`).
