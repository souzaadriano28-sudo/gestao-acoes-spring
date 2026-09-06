# Liquibase e adoção segura do PostgreSQL

PostgreSQL 17 é o alvo compatível. O fluxo principal é banco vazio: o Spring executa `db/changelog/db.changelog-master.xml`, Liquibase cria as quatro tabelas e seu histórico, e então Hibernate valida o mapeamento com `ddl-auto=validate`. Hibernate não cria, atualiza nem remove objetos.

## Schema de investimentos inventariado

| Tabela | Identidade e dados principais | Integridade preservada |
|---|---|---|
| `acao` | `id` bigint identity; ticker 10; mercado 50; moeda 10; cotação numeric(19,8); timestamp(6) | PK, ticker único, campos financeiros/identificadores obrigatórios |
| `corretora` | `id` bigint identity; CNPJ 14; CEP 9; UF 2; demais dados cadastrais | PK, CNPJ único, CNPJ/razão social/CEP obrigatórios |
| `transacao` | `id` bigint identity; tipo varchar; quantidade; preço numeric(19,8); timestamp(6); duas FKs | tipo COMPRA/VENDA, valores positivos, FKs obrigatórias |
| `posicao_carteira` | `id` bigint identity; quantidade; preço médio numeric(19,8); duas FKs | valores positivos, FKs e uma posição por ação/corretora |

Os nomes explícitos de PK, FK, unique e check estão no changelog. Os scripts em `db/adoption` não participam da inicialização normal e nunca são executados automaticamente.

## Banco vazio

1. Crie um banco vazio no PostgreSQL 17 e um usuário com privilégios apenas sobre esse banco/schema.
2. Exporte as variáveis descritas em `runtime-configuration.md`.
3. Inicie a aplicação. Confirme três linhas em `databasechangelog`, o lock liberado em `databasechangeloglock` e a inicialização do JPA em modo `validate`.
4. Reinicie e confirme que nenhum changeset foi reaplicado.

## Adoção de PostgreSQL existente

Este procedimento é administrativo, deliberadamente manual e deve ser ensaiado numa cópia descartável antes da janela real.

1. Interrompa escritas da aplicação e registre versão, contagens, IDs máximos e checksums de uma amostra controlada.
2. Faça backup nativo com `pg_dump` compatível com PostgreSQL 17. Restaure-o em outro banco com `pg_restore` e prove consultas e contagens. Um arquivo não restaurado não é backup validado.
3. Na cópia restaurada, execute com parada no primeiro erro: `psql -v ON_ERROR_STOP=1 -f db/adoption/preflight-existing-postgresql.sql`. Resolva manualmente cada colisão ou dado inválido; não descarte registros automaticamente.
4. Se e somente se o preflight passar, execute uma vez `db/adoption/normalize-existing-postgresql.sql`. A normalização de ticker/CNPJ/mercado é potencialmente lossy; seu rollback é restaurar o backup testado.
5. Execute `db/adoption/verify-schema-equivalence.sql`. Qualquer coluna ou constraint divergente bloqueia a adoção. Reconcilie explicitamente e repita preflight/equivalência.
6. Somente após equivalência exata, um operador autorizado pode executar manualmente o comando `liquibase changelog-sync` apontando para `src/main/resources/db/changelog/db.changelog-master.xml` e para a cópia aprovada. O projeto não contém startup, teste ou script que execute `changelog-sync`.
7. Inicie a aplicação com Liquibase habilitado e Hibernate `validate`; confira histórico, contagens, IDs, posição da carteira e uma operação transacional controlada.
8. Repita a janela no banco alvo somente após aprovação das evidências e com plano de restauração disponível.

Nunca use `clear-checksums`, nunca apague o lock enquanto houver migração ativa e nunca marque uma estrutura divergente como baseline. Para changesets reversíveis e schema inicial descartável, use o rollback Liquibase testado. Para normalização de dados ou qualquer perda, pare o tráfego e restaure o backup verificado.

## Limitações atuais

A suíte embarcada disponível executa PostgreSQL 14.15 e comprova portabilidade básica, migrations, locks e concorrência. PostgreSQL 17 permanece o alvo e requer uma execução dedicada antes de produção. Nenhum snapshot de banco persistente real foi fornecido; portanto, a adoção real continua condicionada ao inventário e ensaio acima.
