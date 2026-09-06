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
      ('corretora','id','bigint','NO',NULL,NULL,NULL), ('corretora','cnpj','character varying','NO',14,NULL,NULL),
      ('corretora','razao_social','character varying','NO',255,NULL,NULL), ('corretora','nome_fantasia','character varying','YES',255,NULL,NULL),
      ('corretora','email','character varying','YES',255,NULL,NULL), ('corretora','telefone','character varying','YES',255,NULL,NULL),
      ('corretora','cep','character varying','NO',9,NULL,NULL), ('corretora','logradouro','character varying','YES',255,NULL,NULL),
      ('corretora','numero','character varying','YES',255,NULL,NULL), ('corretora','complemento','character varying','YES',255,NULL,NULL),
      ('corretora','bairro','character varying','YES',255,NULL,NULL), ('corretora','cidade','character varying','YES',255,NULL,NULL),
      ('corretora','uf','character varying','YES',2,NULL,NULL), ('corretora','situacao_cadastral','character varying','YES',255,NULL,NULL),
      ('corretora','validada_na_cvm','boolean','YES',NULL,NULL,NULL), ('corretora','data_cadastro','timestamp without time zone','YES',NULL,NULL,NULL),
      ('transacao','id','bigint','NO',NULL,NULL,NULL), ('transacao','tipo','character varying','NO',255,NULL,NULL),
      ('transacao','quantidade','integer','NO',NULL,NULL,NULL), ('transacao','preco_unitario','numeric','NO',NULL,19,8),
      ('transacao','data_hora','timestamp without time zone','NO',NULL,NULL,NULL), ('transacao','acao_id','bigint','NO',NULL,NULL,NULL),
      ('transacao','corretora_id','bigint','NO',NULL,NULL,NULL),
      ('posicao_carteira','id','bigint','NO',NULL,NULL,NULL), ('posicao_carteira','quantidade_total','integer','NO',NULL,NULL,NULL),
      ('posicao_carteira','preco_medio','numeric','NO',NULL,19,8), ('posicao_carteira','acao_id','bigint','NO',NULL,NULL,NULL),
      ('posicao_carteira','corretora_id','bigint','NO',NULL,NULL,NULL)
  ), actual AS (
    SELECT table_name, column_name, data_type, is_nullable,
           CASE WHEN data_type='character varying' THEN character_maximum_length END AS character_maximum_length,
           CASE WHEN data_type='numeric' THEN numeric_precision END AS numeric_precision,
           CASE WHEN data_type='numeric' THEN numeric_scale END AS numeric_scale
    FROM information_schema.columns
    WHERE table_schema = current_schema() AND table_name IN ('acao','corretora','transacao','posicao_carteira')
  )
  SELECT concat(e.table_name,'.',e.column_name) INTO missing_column FROM expected e
  LEFT JOIN actual a ON (a.table_name,a.column_name,a.data_type,a.is_nullable,
      coalesce(a.character_maximum_length,-1),coalesce(a.numeric_precision,-1),coalesce(a.numeric_scale,-1)) =
     (e.table_name,e.column_name,e.data_type,e.nullable,coalesce(e.char_len,-1),coalesce(e.precision,-1),coalesce(e.scale,-1))
  WHERE a.column_name IS NULL LIMIT 1;
  IF missing_column IS NOT NULL THEN RAISE EXCEPTION 'schema equivalence: missing or divergent column %', missing_column; END IF;

  WITH expected(table_name, column_name) AS (VALUES
    ('acao','id'),('acao','ticker'),('acao','nome_empresa'),('acao','mercado'),('acao','moeda'),('acao','cotacao_atual'),('acao','data_hora_cotacao'),
    ('acao','quote_source_type'),('acao','quote_provider'),('acao','quote_reference_at'),('acao','quote_fetched_at'),('acao','quote_reference_kind'),
    ('corretora','id'),('corretora','cnpj'),('corretora','razao_social'),('corretora','nome_fantasia'),('corretora','email'),('corretora','telefone'),('corretora','cep'),('corretora','logradouro'),('corretora','numero'),('corretora','complemento'),('corretora','bairro'),('corretora','cidade'),('corretora','uf'),('corretora','situacao_cadastral'),('corretora','validada_na_cvm'),('corretora','data_cadastro'),
    ('transacao','id'),('transacao','tipo'),('transacao','quantidade'),('transacao','preco_unitario'),('transacao','data_hora'),('transacao','acao_id'),('transacao','corretora_id'),
    ('posicao_carteira','id'),('posicao_carteira','quantidade_total'),('posicao_carteira','preco_medio'),('posicao_carteira','acao_id'),('posicao_carteira','corretora_id'))
  SELECT concat(c.table_name,'.',c.column_name) INTO unexpected_column
  FROM information_schema.columns c LEFT JOIN expected e USING (table_name,column_name)
  WHERE c.table_schema=current_schema() AND c.table_name IN ('acao','corretora','transacao','posicao_carteira')
    AND e.column_name IS NULL LIMIT 1;
  IF unexpected_column IS NOT NULL THEN RAISE EXCEPTION 'schema equivalence: unexpected column %', unexpected_column; END IF;

  SELECT required.name INTO missing_constraint FROM (VALUES
    ('pk_acao'),('pk_corretora'),('pk_transacao'),('pk_posicao_carteira'),
    ('uk_acao_ticker'),('uk_corretora_cnpj'),('uk_posicao_acao_corretora'),
    ('fk_transacao_acao'),('fk_transacao_corretora'),('fk_posicao_acao'),('fk_posicao_corretora'),
    ('ck_transacao_valores_positivos'),('ck_transacao_tipo'),('ck_posicao_valores_positivos')) required(name)
  WHERE NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname=required.name) LIMIT 1;
  IF missing_constraint IS NOT NULL THEN RAISE EXCEPTION 'schema equivalence: missing constraint %', missing_constraint; END IF;
END
$schema_contract$;
