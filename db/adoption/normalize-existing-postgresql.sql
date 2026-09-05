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
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='acao_ticker_key')
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uk_acao_ticker') THEN
    ALTER TABLE acao RENAME CONSTRAINT acao_ticker_key TO uk_acao_ticker;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='corretora_cnpj_key')
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uk_corretora_cnpj') THEN
    ALTER TABLE corretora RENAME CONSTRAINT corretora_cnpj_key TO uk_corretora_cnpj;
  END IF;
END
$constraint_names$;

DO $required_uniques$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uk_acao_ticker') THEN
    ALTER TABLE acao ADD CONSTRAINT uk_acao_ticker UNIQUE (ticker);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uk_corretora_cnpj') THEN
    ALTER TABLE corretora ADD CONSTRAINT uk_corretora_cnpj UNIQUE (cnpj);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uk_posicao_acao_corretora') THEN
    ALTER TABLE posicao_carteira ADD CONSTRAINT uk_posicao_acao_corretora UNIQUE (acao_id, corretora_id);
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
