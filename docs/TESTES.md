# Testes do Vértice

O sistema tem quatro camadas de teste. As três primeiras rodam sem nada além do que o projeto
já exige (Java 21, Node e o PostgreSQL local).

| Camada | O que cobre | Onde | Comando |
|---|---|---|---|
| Backend (H2) | Regras de negócio e API: cadastro, moderação, matching, IA (simulada), conexões, conversas, reuniões, perfis, métricas e regressões de acesso | `backend-PI-main/src/test` | `./mvnw test` |
| Backend (PostgreSQL real) | A mesma suíte no PostgreSQL instalado (bytea, TEXT e consultas como em produção) | perfil Maven `postgres` | `./mvnw test -Ppostgres` |
| Front (unidade/componentes) | Máscaras e validações, cliente HTTP, logout, popups, matching, aviso de match, animações | `frontend-PI/src/test` | `npm test` |
| Ponta a ponta (Playwright) | Front + backend + PostgreSQL juntos, num navegador de verdade | `frontend-PI/e2e` | `npm run test:e2e` |

## Backend no PostgreSQL real

Usa o banco **`vertice_teste`** (as tabelas são criadas e apagadas a cada execução). Os bancos
`projeto_pi` e `vertice_simulacao` não são tocados. Na primeira vez, crie o banco:

```bash
psql -U postgres -c "create database vertice_teste"
```

Credenciais padrão: as mesmas do `application.properties` (`postgres`/`root`). Para outro servidor:
`./mvnw test -Ppostgres -Dteste.pg.url=jdbc:postgresql://host:5432/vertice_teste -Dteste.pg.usuario=... -Dteste.pg.senha=...`

## Ponta a ponta (Playwright)

`npm run test:e2e` (na pasta `frontend-PI`):

1. compila o backend (`mvnw package -DskipTests`);
2. apaga e recria o banco **`vertice_e2e`** no PostgreSQL local;
3. sobe o backend na porta **8083** e o front na **5175** (não conflita com 8080/5173 do desenvolvimento);
4. roda os cenários e encerra os servidores.

Variáveis opcionais: `E2E_PG_HOST`, `E2E_PG_PORT`, `E2E_PG_USER`, `E2E_PG_PASSWORD`, `E2E_PSQL`.
Na primeira vez, instale o navegador: `npx playwright install chromium`.

**Cenários** (`e2e/1-jornada.spec.ts`, em sequência, como duas pessoas em navegadores diferentes):
cadastro da startup em 4 etapas (logo, pitch deck, link) e do investidor em 3 (foto) → login
recusado enquanto pendente → admin aprova → investidor vê a startup compatível com os anexos e baixa
o pitch → pede conexão → startup firma (animação de match) → conversa → investidor recebe a animação
e convida para reunião → startup aceita → reunião na agenda dos dois → avaliação (RF10) → métricas
do admin → edição de perfil e canvas vistos pelo investidor (sem dados privados) → logout nos três
portais → animação de login.

**Responsividade** (`e2e/2-responsivo.spec.ts`): todas as páginas dos três portais em 390, 768 e
1280 px sem rolagem lateral; popup de edição dentro da margem; comparação visual das telas estáveis
com as capturas de referência em `e2e/capturas/`.

Depois de uma mudança visual intencional, atualize as referências:
`npx playwright test --update-snapshots`. Relatório da última execução: `npm run test:e2e:relatorio`.

## Docker (opcional)

Os testes não dependem de Docker. Se for usá-lo nesta máquina e o Docker Desktop abrir com o erro
"initializing Inference manager" / "Secrets Engine" (opções só "Fechar" ou "Resetar"), **não resete**:
rode `scripts\iniciar-docker.ps1`. O Windows aqui não consegue apagar os arquivos de socket que o
Docker deixa ao fechar (provável interferência do filtro do McAfee); o script os remove pelo WSL e
inicia o Docker. Para resolver de vez, adicione `%LOCALAPPDATA%\Docker` e
`%LOCALAPPDATA%\docker-secrets-engine` às exclusões do McAfee.
