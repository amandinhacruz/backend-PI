# Simulação de produção

Banco separado (`vertice_simulacao`) com perfis fictícios para testar o cruzamento de startups e investidores e o matching por IA, sem tocar no banco `projeto_pi`.

## O que tem no banco

| Item | Quantidade |
|---|---|
| Startups aprovadas | 70, em 8 segmentos e nos 3 estágios |
| Startups de controle | 3 pendentes e 2 reprovadas (não podem aparecer no matching) |
| Investidores e mentores | 20 (18 investidores, 2 mentores) |
| Pedidos de conexão | 20, feitos pelos 10 primeiros investidores |
| Startups passadas | 10 |
| Conexões firmadas / negadas | 15 / 5 |
| Avaliações (RF10) | 30, uma delas com nota 2 |
| Conversas (RF05) | 15 (uma por conexão firmada), com cerca de 80 mensagens espalhadas pelas últimas semanas |
| Reuniões | 18 convites: já realizadas, confirmadas, aguardando resposta, recusadas e canceladas |

Casos de borda incluídos: 3 startups com descrição vaga (SaúdeBot, AgroData, VerdeData), 2 sem faixa de ticket (AgroData, LogiBox), 1 com tentativa de manipular a nota da IA no pitch (InvestMais), 2 investidores sem preferência de estágio (Ana Paula Freire, Camila Bezerra) e 2 sem faixa de ticket (Carlos Eduardo Moura, Camila Bezerra).

Cada investidor vê de 6 a 20 startups compatíveis antes do histórico. Os que só investem em Escala veem 6, o que é esperado; André Carvalho vê 20, acima do limite de 18 análises por carregamento, o que testa esse limite.

Todos os nomes e textos são inventados. E-mails usam `@simulacao.vertice.local`, sites `.example`; CPF e CNPJ são aleatórios com dígitos válidos.

## Contas

Senha de todas: `Simulacao@2026`. Admin: `admin@projetopi.com` / `admin123`.

O e-mail é o nome sem acentos, separado por pontos, com `@simulacao.vertice.local`:

- Investidores: `helena.duarte@...`, `rafael.lins@...`, `marta.cavalcanti@...`, `joao.batista.neto@...`, `fernando.rocha@...`, `andre.carvalho@...` (lista completa em `simulacao/dados.py`).
- Startups: `telesertao@...`, `credifeira@...`, `irrigasol@...`, `codenordeste@...`, `rotacerta@...`, `solarcoop@...`, `atendezap@...`.

Quem tem histórico: os 10 primeiros investidores da lista (Helena Duarte até Fernando Rocha). Boas contas para ver mensagens e reuniões: `rafael.lins@...` (investidor) e `notafacil@...` (startup). Os outros 10 começam sem decisões, para comparar o efeito do aprendizado.

## Como rodar

1. No IntelliJ, pare o backend normal e rode a configuração **"Vértice Backend (simulação)"**. Ela aponta para o banco `vertice_simulacao` com a chave de criptografia dele (arquivo `.run/Simulacao.run.xml`, fora do git).
2. Rode o front normalmente (`npm run dev`) e entre em `http://localhost:5173`.

A chave fica só nesse arquivo. Se ele se perder, os dados da simulação ficam ilegíveis; basta recriar o banco.

## Ligar a IA na simulação

A configuração da simulação já usa o Claude Haiku 5.5 e dispensa a trava da política 1.1, **só neste banco**. Para ligar:

1. Na configuração "Vértice Backend (simulação)", adicione `ANTHROPIC_API_KEY` e troque `VERTICE_IA_HABILITADA` para `true`.
2. Entre como investidor e abra o Matching. As análises rodam em segundo plano e a tela se atualiza sozinha.
3. Acompanhe em `GET /admin/ia/status`.

Custo esperado para analisar todos os pares no Haiku 5.5: menos de US$ 0,20. Detalhes em [IA.md](IA.md).

## Recriar do zero

```bash
"C:/Program Files/PostgreSQL/18/bin/psql.exe" -h localhost -U postgres -c "drop database vertice_simulacao with (force)" -c "create database vertice_simulacao"
```

Depois rode a configuração da simulação no IntelliJ (porta 8080) e, na pasta `backend-PI-main`:

```bash
python simulacao/popular.py --api http://localhost:8080
```

```bash
python simulacao/conversas.py --api http://localhost:8080
```

O segundo script cria as conversas e as reuniões pela API e depois ajusta, direto no banco da simulação, só as datas (para o histórico ficar espalhado no tempo e algumas reuniões ficarem no passado). Ele usa o `psql` do PostgreSQL 18 e a senha da variável `PGPASSWORD` (padrão `root`).

O script só roda em banco vazio, para não misturar dados de rodadas diferentes.
