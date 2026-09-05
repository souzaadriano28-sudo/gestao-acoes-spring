-- Run V001 first and take a database backup. Execute this file with ON_ERROR_STOP=1.
BEGIN;

UPDATE acao SET ticker = upper(btrim(ticker));
UPDATE acao SET mercado = CASE upper(btrim(mercado))
  WHEN 'NACIONAL' THEN 'BRASIL' WHEN 'BRASIL' THEN 'BRASIL'
  WHEN 'INTERNACIONAL' THEN 'AMERICANO' WHEN 'AMERICANO' THEN 'AMERICANO' END;
UPDATE corretora SET cnpj = regexp_replace(btrim(cnpj), '[./-]', '', 'g');

ALTER TABLE acao ALTER COLUMN cotacao_atual TYPE numeric(19,8) USING cotacao_atual::numeric(19,8);
ALTER TABLE transacao ALTER COLUMN preco_unitario TYPE numeric(19,8) USING preco_unitario::numeric(19,8);
ALTER TABLE posicao_carteira ALTER COLUMN preco_medio TYPE numeric(19,8) USING preco_medio::numeric(19,8);

CREATE UNIQUE INDEX IF NOT EXISTS uk_acao_ticker_canonical ON acao (ticker);
CREATE UNIQUE INDEX IF NOT EXISTS uk_corretora_cnpj_canonical ON corretora (cnpj);
CREATE UNIQUE INDEX IF NOT EXISTS uk_posicao_acao_corretora ON posicao_carteira (acao_id, corretora_id);

ALTER TABLE transacao DROP CONSTRAINT IF EXISTS ck_transacao_positive;
ALTER TABLE transacao ADD CONSTRAINT ck_transacao_positive CHECK (quantidade > 0 AND preco_unitario > 0);
ALTER TABLE posicao_carteira DROP CONSTRAINT IF EXISTS ck_posicao_positive;
ALTER TABLE posicao_carteira ADD CONSTRAINT ck_posicao_positive CHECK (quantidade_total > 0 AND preco_medio > 0);

COMMIT;
