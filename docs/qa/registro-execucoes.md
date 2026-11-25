# Registro de Execuções de Testes — Vértice

## EXE-001 — Inicialização do contexto Spring

**Data:** 25/11/2026  
**Responsável:** Ronald da Paixão e Silva  
**Projeto:** Vértice — Backend  
**Ambiente:** Local — Windows / IntelliJ IDEA  
**Branch testada:** main
**Branch de documentação:** test/qa-evidencias-iniciais
**Commit testado:** 1e5a37f8f6d140bd70d57a25e2d85a0ef5d56eaa
**Ferramentas:** Java 21, Maven, JUnit e Spring Boot

### Objetivo

Verificar se o contexto da aplicação Spring Boot é inicializado corretamente por meio da execução da classe `ProjetoPiApplicationTests`.

### Procedimento realizado

1. Abrir o projeto backend no IntelliJ IDEA.
2. Acessar o terminal na pasta raiz do projeto.
3. Executar o comando:

`.\mvnw.cmd -Dtest=ProjetoPiApplicationTests test`

4. Verificar o resultado da execução.

### Resultado esperado

O contexto Spring deve inicializar sem erros, e o Maven deve finalizar a execução com `BUILD SUCCESS`.

### Resultado obtido

**Status: APROVADO (PASS)**

O Maven finalizou com `BUILD SUCCESS`.

- Tests run: 1
- Failures: 0
- Errors: 0
- Skipped: 0

### Evidências

- Relatório do Maven Surefire: `docs/qa/evidencias/EXE-001-surefire.txt`
- Identificador do commit: 1e5a37f8f6d140bd70d57a25e2d85a0ef5d56eaa

### Observações

A execução valida a inicialização do contexto da aplicação, mas não comprova o funcionamento de funcionalidades específicas, como login, cadastro ou matchmaking.