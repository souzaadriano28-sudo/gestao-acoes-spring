-- Existing PostgreSQL adoption only. Read-only structural gate before changelog-sync.
-- Any exception means the database MUST NOT be marked as baselined.
DO $schema_contract$
DECLARE
  missing_column text;
  unexpected_column text;
  missing_constraint text;
BEGIN
  WITH expected(table_name, column_name, data_type, nullable, char_len, precision, scale) AS (
    VALUES
      ('acao','id','bigint','NO',NULL,NULL,NULL), ('acao','ticker','character varying','NO',10,NULL,NULL),
      ('acao','nome_empresa','character varying','YES',255,NULL,NULL), ('acao','mercado','character varying','NO',50,NULL,NULL),
      ('acao','moeda','character varying','NO',10,NULL,NULL), ('acao','cotacao_atual','numeric','NO',NULL,19,8),
      ('acao','data_hora_cotacao','timestamp without time zone','NO',NULL,NULL,NULL),
      ('acao','quote_source_type','character varying','YES',40,NULL,NULL),
      ('acao','quote_provider','character varying','YES',80,NULL,NULL),
      ('acao','quote_reference_at','timestamp with time zone','YES',NULL,NULL,NULL),
      ('acao','quote_fetched_at','timestamp with time zone','YES',NULL,NULL,NULL),
      ('acao','quote_reference_kind','character varying','YES',40,NULL,NULL),
      ('acao','owner_id','bigint','NO',NULL,NULL,NULL),
      ('corretora','id','bigint','NO',NULL,NULL,NULL), ('corretora','cnpj','character varying','NO',14,NULL,NULL),
      ('corretora','razao_social','character varying','NO',255,NULL,NULL), ('corretora','nome_fantasia','character varying','YES',255,NULL,NULL),
      ('corretora','email','character varying','YES',255,NULL,NULL), ('corretora','telefone','character varying','YES',255,NULL,NULL),
      ('corretora','cep','character varying','NO',9,NULL,NULL), ('corretora','logradouro','character varying','YES',255,NULL,NULL),
      ('corretora','numero','character varying','YES',255,NULL,NULL), ('corretora','complemento','character varying','YES',255,NULL,NULL),
      ('corretora','bairro','character varying','YES',255,NULL,NULL), ('corretora','cidade','character varying','YES',255,NULL,NULL),
      ('corretora','uf','character varying','YES',2,NULL,NULL), ('corretora','situacao_cadastral','character varying','YES',255,NULL,NULL),
      ('corretora','validada_na_cvm','boolean','YES',NULL,NULL,NULL), ('corretora','data_cadastro','timestamp without time zone','YES',NULL,NULL,NULL),
      ('corretora','regulatory_status','character varying','NO',20,NULL,NULL),
      ('corretora','regulatory_category','character varying','YES',120,NULL,NULL),
      ('corretora','regulatory_source','character varying','YES',160,NULL,NULL),
      ('corretora','regulatory_evidence_id','character varying','YES',80,NULL,NULL),
      ('corretora','regulatory_reference_at','timestamp with time zone','YES',NULL,NULL,NULL),
      ('corretora','regulatory_checked_at','timestamp with time zone','YES',NULL,NULL,NULL),
      ('corretora','regulatory_reason','character varying','YES',120,NULL,NULL),
      ('corretora','owner_id','bigint','NO',NULL,NULL,NULL),
      ('transacao','id','bigint','NO',NULL,NULL,NULL), ('transacao','tipo','character varying','NO',255,NULL,NULL),
      ('transacao','quantidade','integer','NO',NULL,NULL,NULL), ('transacao','preco_unitario','numeric','NO',NULL,19,8),
      ('transacao','data_hora','timestamp without time zone','NO',NULL,NULL,NULL), ('transacao','acao_id','bigint','NO',NULL,NULL,NULL),
      ('transacao','corretora_id','bigint','NO',NULL,NULL,NULL),
      ('transacao','portfolio_id','bigint','NO',NULL,NULL,NULL),
      ('transacao','moeda','character varying','NO',3,NULL,NULL),
      ('transacao','corretagem','numeric','NO',NULL,19,8), ('transacao','taxas','numeric','NO',NULL,19,8),
      ('transacao','impostos','numeric','NO',NULL,19,8), ('transacao','outros_custos','numeric','NO',NULL,19,8),
      ('transacao','valor_bruto','numeric','NO',NULL,19,8), ('transacao','valor_total','numeric','NO',NULL,19,8),
      ('transacao','observacao','character varying','YES',2000,NULL,NULL),
      ('transacao','idempotency_key','character varying','YES',100,NULL,NULL),
      ('transacao','resultado_realizado','numeric','NO',NULL,19,8),
      ('posicao_carteira','id','bigint','NO',NULL,NULL,NULL), ('posicao_carteira','quantidade_total','integer','NO',NULL,NULL,NULL),
      ('posicao_carteira','preco_medio','numeric','NO',NULL,19,8), ('posicao_carteira','acao_id','bigint','NO',NULL,NULL,NULL),
      ('posicao_carteira','corretora_id','bigint','NO',NULL,NULL,NULL),
      ('posicao_carteira','portfolio_id','bigint','NO',NULL,NULL,NULL),
      ('posicao_carteira','resultado_realizado','numeric','NO',NULL,19,8),
      ('exchange_rate_snapshot','id','bigint','NO',NULL,NULL,NULL),
      ('exchange_rate_snapshot','base_currency','character varying','NO',3,NULL,NULL),
      ('exchange_rate_snapshot','quote_currency','character varying','NO',3,NULL,NULL),
      ('exchange_rate_snapshot','rate','numeric','NO',NULL,19,8),
      ('exchange_rate_snapshot','source_type','character varying','NO',40,NULL,NULL),
      ('exchange_rate_snapshot','provider','character varying','NO',80,NULL,NULL),
      ('exchange_rate_snapshot','reference_at','timestamp with time zone','NO',NULL,NULL,NULL),
      ('exchange_rate_snapshot','fetched_at','timestamp with time zone','NO',NULL,NULL,NULL),
      ('exchange_rate_snapshot','reference_kind','character varying','NO',60,NULL,NULL)
  ), actual AS (
    SELECT table_name, column_name, data_type, is_nullable,
           CASE WHEN data_type='character varying' THEN character_maximum_length END AS character_maximum_length,
           CASE WHEN data_type='numeric' THEN numeric_precision END AS numeric_precision,
           CASE WHEN data_type='numeric' THEN numeric_scale END AS numeric_scale
    FROM information_schema.columns
    WHERE table_schema = current_schema() AND table_name IN ('acao','corretora','transacao','posicao_carteira','exchange_rate_snapshot')
  )
  SELECT concat(e.table_name,'.',e.column_name) INTO missing_column FROM expected e
  LEFT JOIN actual a ON (a.table_name,a.column_name,a.data_type,a.is_nullable,
      coalesce(a.character_maximum_length,-1),coalesce(a.numeric_precision,-1),coalesce(a.numeric_scale,-1)) =
     (e.table_name,e.column_name,e.data_type,e.nullable,coalesce(e.char_len,-1),coalesce(e.precision,-1),coalesce(e.scale,-1))
  WHERE a.column_name IS NULL LIMIT 1;
  IF missing_column IS NOT NULL THEN RAISE EXCEPTION 'schema equivalence: missing or divergent column %', missing_column; END IF;

  WITH expected(table_name, column_name) AS (VALUES
    ('acao','id'),('acao','ticker'),('acao','nome_empresa'),('acao','mercado'),('acao','moeda'),('acao','cotacao_atual'),('acao','data_hora_cotacao'),
    ('acao','quote_source_type'),('acao','quote_provider'),('acao','quote_reference_at'),('acao','quote_fetched_at'),('acao','quote_reference_kind'),('acao','owner_id'),
    ('corretora','id'),('corretora','cnpj'),('corretora','razao_social'),('corretora','nome_fantasia'),('corretora','email'),('corretora','telefone'),('corretora','cep'),('corretora','logradouro'),('corretora','numero'),('corretora','complemento'),('corretora','bairro'),('corretora','cidade'),('corretora','uf'),('corretora','situacao_cadastral'),('corretora','validada_na_cvm'),('corretora','data_cadastro'),('corretora','regulatory_status'),('corretora','regulatory_category'),('corretora','regulatory_source'),('corretora','regulatory_evidence_id'),('corretora','regulatory_reference_at'),('corretora','regulatory_checked_at'),('corretora','regulatory_reason'),('corretora','owner_id'),
    ('transacao','id'),('transacao','tipo'),('transacao','quantidade'),('transacao','preco_unitario'),('transacao','data_hora'),('transacao','acao_id'),('transacao','corretora_id'),('transacao','portfolio_id'),('transacao','moeda'),('transacao','corretagem'),('transacao','taxas'),('transacao','impostos'),('transacao','outros_custos'),('transacao','valor_bruto'),('transacao','valor_total'),('transacao','observacao'),('transacao','idempotency_key'),('transacao','resultado_realizado'),
    ('posicao_carteira','id'),('posicao_carteira','quantidade_total'),('posicao_carteira','preco_medio'),('posicao_carteira','acao_id'),('posicao_carteira','corretora_id'),('posicao_carteira','portfolio_id'),('posicao_carteira','resultado_realizado'),
    ('exchange_rate_snapshot','id'),('exchange_rate_snapshot','base_currency'),('exchange_rate_snapshot','quote_currency'),('exchange_rate_snapshot','rate'),('exchange_rate_snapshot','source_type'),('exchange_rate_snapshot','provider'),('exchange_rate_snapshot','reference_at'),('exchange_rate_snapshot','fetched_at'),('exchange_rate_snapshot','reference_kind'),
    ('user_account','id'),('user_account','username'),('user_account','email'),('user_account','password_hash'),('user_account','enabled'),('user_account','failed_attempts'),('user_account','failure_window_started_at'),('user_account','locked_until'),('user_account','created_at'),('user_account','updated_at'),('user_account','version'),
    ('portfolio','id'),('portfolio','name'),('portfolio','owner_id'),('portfolio','created_at'))
  SELECT concat(c.table_name,'.',c.column_name) INTO unexpected_column
  FROM information_schema.columns c LEFT JOIN expected e USING (table_name,column_name)
  WHERE c.table_schema=current_schema() AND c.table_name IN ('acao','corretora','transacao','posicao_carteira','exchange_rate_snapshot','user_account','portfolio')
    AND e.column_name IS NULL LIMIT 1;
  IF unexpected_column IS NOT NULL THEN RAISE EXCEPTION 'schema equivalence: unexpected column %', unexpected_column; END IF;

  SELECT required.name INTO missing_constraint FROM (VALUES
    ('pk_acao'),('pk_corretora'),('pk_transacao'),('pk_posicao_carteira'),('pk_exchange_rate_snapshot'),
    ('pk_user_account'),('pk_portfolio'),
    ('uk_acao_ticker_market_owner'),('uk_corretora_cnpj_owner'),('uk_posicao_acao_corretora_portfolio'),('uk_exchange_rate_pair'),('uk_transacao_portfolio_idempotency'),
    ('uk_user_account_username'),('uk_user_account_email'),
    ('fk_transacao_acao'),('fk_transacao_corretora'),('fk_posicao_acao'),('fk_posicao_corretora'),
    ('fk_acao_owner'),('fk_corretora_owner'),('fk_portfolio_owner'),('fk_transacao_portfolio'),('fk_posicao_portfolio'),
    ('ck_transacao_valores_positivos'),('ck_transacao_tipo'),('ck_posicao_valores_positivos'),
    ('ck_exchange_rate_positive'),('ck_corretora_regulatory_status')) required(name)
  WHERE NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname=required.name) LIMIT 1;
  IF missing_constraint IS NOT NULL THEN RAISE EXCEPTION 'schema equivalence: missing constraint %', missing_constraint; END IF;

  -- Names alone are not enough: the ownership-scoped unique keys must have their
  -- exact expected columns and order.
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    WHERE c.contype = 'u' AND c.conname = 'uk_acao_ticker_market_owner' AND t.relname = 'acao'
      AND c.conkey = ARRAY[
        (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'ticker'),
        (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'mercado'),
        (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'owner_id')
      ]::smallint[]
  ) THEN RAISE EXCEPTION 'schema equivalence: divergent owner-scoped unique on acao(ticker,mercado,owner_id)'; END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    WHERE c.contype = 'u' AND c.conname = 'uk_corretora_cnpj_owner' AND t.relname = 'corretora'
      AND c.conkey = ARRAY[
        (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'cnpj'),
        (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'owner_id')
      ]::smallint[]
  ) THEN RAISE EXCEPTION 'schema equivalence: divergent owner-scoped unique on corretora(cnpj,owner_id)'; END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    WHERE c.contype = 'u' AND c.conname = 'uk_posicao_acao_corretora_portfolio' AND t.relname = 'posicao_carteira'
      AND c.conkey = ARRAY[
        (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'acao_id'),
        (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'corretora_id'),
        (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'portfolio_id')
      ]::smallint[]
  ) THEN RAISE EXCEPTION 'schema equivalence: divergent owner-scoped unique on posicao_carteira(acao_id,corretora_id,portfolio_id)'; END IF;

  -- Reject residual global unique constraints and standalone unique indexes. A
  -- primary key is excluded, and only a single ticker/cnpj key is considered global.
  SELECT format('%I.%I', n.nspname, t.relname) INTO missing_constraint
  FROM pg_constraint c
  JOIN pg_class t ON t.oid = c.conrelid
  JOIN pg_namespace n ON n.oid = t.relnamespace
  WHERE n.nspname = current_schema() AND c.contype = 'u'
    AND ((t.relname = 'acao' AND c.conkey = ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'ticker')]::smallint[])
      OR (t.relname = 'corretora' AND c.conkey = ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'cnpj')]::smallint[]))
  LIMIT 1;
  IF missing_constraint IS NOT NULL THEN RAISE EXCEPTION 'schema equivalence: residual global unique constraint on %', missing_constraint; END IF;

  SELECT format('%I.%I', n.nspname, t.relname) INTO missing_constraint
  FROM pg_index i
  JOIN pg_class t ON t.oid = i.indrelid
  JOIN pg_namespace n ON n.oid = t.relnamespace
  LEFT JOIN pg_constraint c ON c.conindid = i.indexrelid
  WHERE n.nspname = current_schema() AND i.indisunique AND NOT i.indisprimary AND c.oid IS NULL
    AND ((t.relname = 'acao' AND i.indnkeyatts = 1 AND (SELECT key_column.attnum
          FROM unnest(i.indkey) WITH ORDINALITY AS key_column(attnum, ordinality)
          WHERE key_column.ordinality = 1) = (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'ticker'))
      OR (t.relname = 'corretora' AND i.indnkeyatts = 1 AND (SELECT key_column.attnum
          FROM unnest(i.indkey) WITH ORDINALITY AS key_column(attnum, ordinality)
          WHERE key_column.ordinality = 1) = (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'cnpj')))
  LIMIT 1;
  IF missing_constraint IS NOT NULL THEN RAISE EXCEPTION 'schema equivalence: residual global unique index on %', missing_constraint; END IF;
END
$schema_contract$;
