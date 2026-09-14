-- Existing PostgreSQL adoption only. Run preflight and take a tested backup first.
BEGIN;

UPDATE acao SET ticker = upper(btrim(ticker));
UPDATE acao SET mercado = CASE upper(btrim(mercado))
  WHEN 'NACIONAL' THEN 'BRASIL' WHEN 'BRASIL' THEN 'BRASIL'
  WHEN 'INTERNACIONAL' THEN 'AMERICANO' WHEN 'AMERICANO' THEN 'AMERICANO' END;
UPDATE corretora SET cnpj = regexp_replace(btrim(cnpj), '[./-]', '', 'g');

ALTER TABLE acao ALTER COLUMN cotacao_atual TYPE numeric(19,8) USING cotacao_atual::numeric(19,8);
ALTER TABLE transacao ALTER COLUMN preco_unitario TYPE numeric(19,8) USING preco_unitario::numeric(19,8);
ALTER TABLE posicao_carteira ALTER COLUMN preco_medio TYPE numeric(19,8) USING preco_medio::numeric(19,8);

DO $constraint_names$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='acao_pkey')
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_acao') THEN
    ALTER TABLE acao RENAME CONSTRAINT acao_pkey TO pk_acao;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='corretora_pkey')
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_corretora') THEN
    ALTER TABLE corretora RENAME CONSTRAINT corretora_pkey TO pk_corretora;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='transacao_pkey')
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_transacao') THEN
    ALTER TABLE transacao RENAME CONSTRAINT transacao_pkey TO pk_transacao;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='posicao_carteira_pkey')
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_posicao_carteira') THEN
    ALTER TABLE posicao_carteira RENAME CONSTRAINT posicao_carteira_pkey TO pk_posicao_carteira;
  END IF;
END
$constraint_names$;

-- Remove only obsolete *global* uniqueness rules. The catalog predicates deliberately
-- match one key column, so owner/portfolio-scoped constraints, primary keys, foreign
-- keys and non-unique indexes are left untouched. Do not match by legacy object name.
DO $remove_global_uniques$
DECLARE
  candidate record;
BEGIN
  FOR candidate IN
    SELECT constraint_schema, table_name, constraint_name
    FROM information_schema.table_constraints tc
    WHERE tc.table_schema = current_schema()
      AND tc.constraint_type = 'UNIQUE'
      AND ((tc.table_name = 'acao' AND (SELECT array_agg(kcu.column_name::text ORDER BY kcu.ordinal_position)
                                        FROM information_schema.key_column_usage kcu
                                        WHERE kcu.constraint_schema = tc.constraint_schema
                                          AND kcu.constraint_name = tc.constraint_name) = ARRAY['ticker']::text[])
        OR (tc.table_name = 'corretora' AND (SELECT array_agg(kcu.column_name::text ORDER BY kcu.ordinal_position)
                                             FROM information_schema.key_column_usage kcu
                                             WHERE kcu.constraint_schema = tc.constraint_schema
                                               AND kcu.constraint_name = tc.constraint_name) = ARRAY['cnpj']::text[]))
  LOOP
    EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT %I',
                   candidate.constraint_schema, candidate.table_name, candidate.constraint_name);
  END LOOP;

  -- A unique index not owned by a constraint can enforce the same obsolete global rule.
  -- indnkeyatts excludes INCLUDE columns; the sole uniqueness key must be ticker/cnpj.
  FOR candidate IN
    SELECT index_schema, table_name, index_name
    FROM (
      SELECT ns.nspname AS index_schema, tbl.relname AS table_name, idx.relname AS index_name,
             i.indexrelid, i.indnkeyatts,
             array_agg(att.attname::text ORDER BY key_columns.ordinality) FILTER (WHERE key_columns.ordinality <= i.indnkeyatts) AS key_columns
      FROM pg_index i
      JOIN pg_class tbl ON tbl.oid = i.indrelid
      JOIN pg_namespace ns ON ns.oid = tbl.relnamespace
      JOIN pg_class idx ON idx.oid = i.indexrelid
      LEFT JOIN pg_constraint c ON c.conindid = i.indexrelid
      JOIN LATERAL unnest(i.indkey) WITH ORDINALITY AS key_columns(attnum, ordinality) ON key_columns.attnum > 0
      JOIN pg_attribute att ON att.attrelid = tbl.oid AND att.attnum = key_columns.attnum
      WHERE ns.nspname = current_schema() AND i.indisunique AND NOT i.indisprimary AND c.oid IS NULL
      GROUP BY ns.nspname, tbl.relname, idx.relname, i.indexrelid, i.indnkeyatts
    ) standalone
    WHERE (table_name = 'acao' AND key_columns = ARRAY['ticker']::text[])
       OR (table_name = 'corretora' AND key_columns = ARRAY['cnpj']::text[])
  LOOP
    EXECUTE format('DROP INDEX %I.%I', candidate.index_schema, candidate.index_name);
  END LOOP;
END
$remove_global_uniques$;

DO $required_uniques$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uk_acao_ticker_market_owner') THEN
    ALTER TABLE acao ADD CONSTRAINT uk_acao_ticker_market_owner UNIQUE (ticker, mercado, owner_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uk_corretora_cnpj_owner') THEN
    ALTER TABLE corretora ADD CONSTRAINT uk_corretora_cnpj_owner UNIQUE (cnpj, owner_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uk_posicao_acao_corretora_portfolio') THEN
    ALTER TABLE posicao_carteira ADD CONSTRAINT uk_posicao_acao_corretora_portfolio UNIQUE (acao_id, corretora_id, portfolio_id);
  END IF;
END
$required_uniques$;

DO $foreign_key_names$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fkm66250vbx2rl6ayjuqi98fry')
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_transacao_acao') THEN
    ALTER TABLE transacao RENAME CONSTRAINT fkm66250vbx2rl6ayjuqi98fry TO fk_transacao_acao;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fkko1d69n6fp414nm1nkt7dybyr')
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_transacao_corretora') THEN
    ALTER TABLE transacao RENAME CONSTRAINT fkko1d69n6fp414nm1nkt7dybyr TO fk_transacao_corretora;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk65o9t3g72w1jvcvxt9r5ubjkc')
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_posicao_acao') THEN
    ALTER TABLE posicao_carteira RENAME CONSTRAINT fk65o9t3g72w1jvcvxt9r5ubjkc TO fk_posicao_acao;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fkad2b7uvh37kg3xbnxjl48dg0p')
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_posicao_corretora') THEN
    ALTER TABLE posicao_carteira RENAME CONSTRAINT fkad2b7uvh37kg3xbnxjl48dg0p TO fk_posicao_corretora;
  END IF;
END
$foreign_key_names$;

ALTER TABLE transacao DROP CONSTRAINT IF EXISTS ck_transacao_positive;
ALTER TABLE transacao DROP CONSTRAINT IF EXISTS transacao_check;
ALTER TABLE transacao DROP CONSTRAINT IF EXISTS transacao_tipo_check;
ALTER TABLE transacao DROP CONSTRAINT IF EXISTS ck_transacao_valores_positivos;
ALTER TABLE transacao DROP CONSTRAINT IF EXISTS ck_transacao_tipo;
ALTER TABLE transacao ADD CONSTRAINT ck_transacao_valores_positivos CHECK (quantidade > 0 AND preco_unitario > 0);
ALTER TABLE transacao ADD CONSTRAINT ck_transacao_tipo CHECK (tipo IN ('COMPRA', 'VENDA'));
ALTER TABLE posicao_carteira DROP CONSTRAINT IF EXISTS ck_posicao_positive;
ALTER TABLE posicao_carteira DROP CONSTRAINT IF EXISTS posicao_carteira_check;
ALTER TABLE posicao_carteira DROP CONSTRAINT IF EXISTS ck_posicao_valores_positivos;
ALTER TABLE posicao_carteira ADD CONSTRAINT ck_posicao_valores_positivos CHECK (quantidade_total > 0 AND preco_medio > 0);

COMMIT;
