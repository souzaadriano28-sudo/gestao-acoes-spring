-- Run with ON_ERROR_STOP=1. This script makes no persistent changes.
CREATE OR REPLACE FUNCTION pg_temp.valid_cnpj(input_value text) RETURNS boolean
AS $function$
WITH normalized AS (
  SELECT regexp_replace(btrim(input_value), '[./-]', '', 'g') AS digits,
         btrim(input_value) ~ '^([0-9]{14}|[0-9]{2}\.[0-9]{3}\.[0-9]{3}/[0-9]{4}-[0-9]{2})$' AS format_ok
), first_digit AS (
  SELECT n.*, CASE WHEN total % 11 < 2 THEN 0 ELSE 11 - total % 11 END AS digit
  FROM normalized n
  CROSS JOIN LATERAL (
    SELECT sum(substring(n.digits, i, 1)::integer * (ARRAY[5,4,3,2,9,8,7,6,5,4,3,2])[i]) AS total
    FROM generate_series(1, 12) i
  ) value
), second_digit AS (
  SELECT f.*, CASE WHEN total % 11 < 2 THEN 0 ELSE 11 - total % 11 END AS second
  FROM first_digit f
  CROSS JOIN LATERAL (
    SELECT sum(substring(f.digits, i, 1)::integer * (ARRAY[6,5,4,3,2,9,8,7,6,5,4,3,2])[i]) AS total
    FROM generate_series(1, 13) i
  ) value
)
SELECT format_ok AND digits !~ '^([0-9])\1{13}$'
   AND substring(digits, 13, 1)::integer = digit
   AND substring(digits, 14, 1)::integer = second
FROM second_digit
$function$
LANGUAGE sql IMMUTABLE;

-- Diagnostic rows are emitted before the blocking checks below. Resolve every row explicitly.
SELECT 'canonical ticker collision' AS issue, string_agg(id::text, ',' ORDER BY id) AS record_ids
FROM acao GROUP BY upper(btrim(ticker)) HAVING count(*) > 1;
SELECT 'canonical CNPJ collision' AS issue, string_agg(id::text, ',' ORDER BY id) AS record_ids
FROM corretora GROUP BY regexp_replace(btrim(cnpj), '[./-]', '', 'g') HAVING count(*) > 1;
SELECT 'duplicate position' AS issue, string_agg(id::text, ',' ORDER BY id) AS record_ids
FROM posicao_carteira GROUP BY acao_id, corretora_id HAVING count(*) > 1;
SELECT 'invalid legacy identifier' AS issue, id AS record_id
FROM acao WHERE upper(btrim(ticker)) !~ '^[A-Z]{1,5}$|^[A-Z]{4}[0-9]{1,2}$'
UNION ALL
SELECT 'invalid legacy CNPJ', id FROM corretora WHERE NOT pg_temp.valid_cnpj(cnpj);

DO $block$
BEGIN
  IF EXISTS (SELECT 1 FROM acao WHERE upper(btrim(ticker)) !~ '^[A-Z]{1,5}$|^[A-Z]{4}[0-9]{1,2}$') THEN
    RAISE EXCEPTION 'preflight: invalid ticker';
  END IF;
  IF EXISTS (SELECT upper(btrim(ticker)) FROM acao GROUP BY 1 HAVING count(*) > 1) THEN
    RAISE EXCEPTION 'preflight: canonical ticker collision';
  END IF;
  IF EXISTS (SELECT 1 FROM acao WHERE upper(btrim(mercado)) NOT IN ('BRASIL','NACIONAL','AMERICANO','INTERNACIONAL')) THEN
    RAISE EXCEPTION 'preflight: unsupported market';
  END IF;
  IF EXISTS (SELECT 1 FROM corretora WHERE NOT pg_temp.valid_cnpj(cnpj)) THEN
    RAISE EXCEPTION 'preflight: invalid CNPJ format or check digits';
  END IF;
  IF EXISTS (SELECT regexp_replace(btrim(cnpj), '[./-]', '', 'g') FROM corretora GROUP BY 1 HAVING count(*) > 1) THEN
    RAISE EXCEPTION 'preflight: canonical CNPJ collision';
  END IF;
  IF EXISTS (SELECT acao_id, corretora_id FROM posicao_carteira GROUP BY 1,2 HAVING count(*) > 1) THEN
    RAISE EXCEPTION 'preflight: duplicate portfolio position';
  END IF;
  IF EXISTS (SELECT 1 FROM transacao WHERE quantidade <= 0 OR preco_unitario <= 0
      OR preco_unitario::numeric <> round(preco_unitario::numeric, 8) OR abs(preco_unitario::numeric) >= 100000000000) THEN
    RAISE EXCEPTION 'preflight: invalid transaction quantity/price or price outside numeric(19,8)';
  END IF;
  IF EXISTS (SELECT 1 FROM posicao_carteira WHERE quantidade_total <= 0 OR preco_medio <= 0
      OR preco_medio::numeric <> round(preco_medio::numeric, 8) OR abs(preco_medio::numeric) >= 100000000000) THEN
    RAISE EXCEPTION 'preflight: invalid position quantity/price or price outside numeric(19,8)';
  END IF;
  IF EXISTS (SELECT 1 FROM acao WHERE cotacao_atual <= 0
      OR cotacao_atual::numeric <> round(cotacao_atual::numeric, 8) OR abs(cotacao_atual::numeric) >= 100000000000) THEN
    RAISE EXCEPTION 'preflight: invalid quote or quote outside numeric(19,8)';
  END IF;
END;
$block$;
