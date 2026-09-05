# Evidências das fases 1 e 2

## Linha de base

- Raiz: `main` em `d6a8fa31ff48e293987b3d967de09f7abe31e26b`; artefatos OpenSpec ainda não commitados.
- Backend: `main` em `d18a3b30ab9ed26044640dac102ab37587b6bece`, limpo.
- Frontend: `master` em `e1d6f873cedc6d2e05a1e1964a3dd21678ce86c3`, limpo e mantido sem edição.
- Java padrão do terminal: 8; validações executadas com Eclipse Temurin 17.0.14. Maven global 3.9.11. Maven Wrapper 3.3.4 apresentou falha ambiental ao consultar `.m2`; o wrapper não foi alterado.
- Docker/Compose: não disponível no terminal. Portas 4200, 5432, 8080 e 9090 livres no inventário.
- Backend antes da mudança: `mvn -B verify`, 38 testes, zero falha/erro/ignorado; 9 testes PostgreSQL/concorrência.
- Schema Hibernate observado em H2 e PostgreSQL foi reconciliado com as quatro entidades e os scripts V001/V002 antes da criação do changelog.

## Segurança e escopo

A busca inicial encontrou URL/usuário PostgreSQL e CORS locais fixos, tokens já externalizados e quatro endpoints com defaults públicos. Não foram encontrados segredos funcionais versionados. `.env.example` contém apenas marcadores falsos. A responsabilidade por injeção e rotação de segredos de produção permanece com a plataforma de deploy, fora desta mudança.

## Resultado do checkpoint

- Maven Wrapper/JDK 17: `./mvnw.cmd -B verify` passou em 34,161 s com 47 testes, zero falha, erro ou ignorado, incluindo binding/fail-fast integrado.
- PostgreSQL embarcado 14.15: 10 testes de migration/concorrência no gate completo; banco vazio, segunda execução sem reaplicação, histórico com 3 changesets, lock liberado, preflight, normalização e rejeição de schema divergente passaram.
- H2: migration vazia/idempotente, Hibernate `validate`, checksum adulterado, rollback dos 3 changesets e reaplicação passaram.
- Frontend imutável: `npm ci` instalou 472 pacotes e encontrou 0 vulnerabilidades; 20/20 testes unitários passaram; build passou. Hashes SHA-256 permaneceram `64FCBBC34A0B817CEA0C1EFC9D28040334437640B8944A6C7772A298CD3FB80C` (`package.json`) e `5F1172600EBC18405B33AE7519F6109F0A3E4A72CB15207B516F5EC6025EDF37` (`package-lock.json`).
- E2E real Angular/Spring/H2/Liquibase/stub: a primeira execução expôs flutuação já existente por falta de espera após a compra de AAPL; a repetição passou 1/1 em 17,5 s. Nenhum arquivo do frontend foi alterado e os relatórios gerados foram removidos.
- Workflows: comandos, Java 17, Node 24.16.0, npm 11.13.0, SHAs das actions, permissões, caches, timeouts e gates existentes foram preservados. Ampliação remota para Docker continua pendente da fase 3.
- Dependências: Spring Boot 4.0.6 gerencia `liquibase-core` 5.0.2 por `spring-boot-starter-liquibase`.
- Segurança: nenhuma assinatura conhecida de chave privada, token GitHub ou chave AWS foi encontrada; `.env` e variantes são ignorados e `.env.example` é explicitamente reaberto.
- OpenSpec: validação global estrita passou com 7 itens aprovados e 0 falhas; avisos informativos preexistentes sobre requisitos longos permanecem.
- Portas após os testes: 4200, 5432, 8080 e 9090 sem listeners.
