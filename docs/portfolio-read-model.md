# Portfolio read model contracts

This phase adds authenticated, additive read endpoints. Existing `/carteira/saldo-total` and
`/carteira/posicoes` payloads remain unchanged and are not deprecated by this change. The legacy
name `saldo-total` means market patrimony, never available cash. New clients must use the contracts
below and must not derive financial totals by joining independent lists.

## Endpoints

- `GET /carteira/dashboard`
- `GET /carteira/posicoes/detalhadas?page=0&size=20&market=BRASIL&brokerId=1`
- `GET /carteira/movimentacoes?page=0&size=20&type=COMPRA&ticker=PETR4&brokerId=1&from=2026-09-01T00:00:00-03:00&to=2026-09-30T23:59:59-03:00`

Pages contain `items`, zero-based `page`, `size`, `totalElements` and `totalPages`. Movement order is
always `recordedAt DESC, id DESC`. Page size is 1–100. Filters are optional and validated by the backend.

## Availability and money

Every monetary object has `availability` (`AVAILABLE`, `STALE` or `UNAVAILABLE`), decimal JSON `value`,
ISO 4217 `currency` and machine-readable `reason`. `value` is null when unavailable; it is never replaced
with zero. Persisted position quantity, average price and cost remain usable if a quote fails. Quote-dependent
market value and unrealized result become unavailable. Stale values remain visible with timestamps, but are
not used in consolidated totals.

The dashboard uses presentation currency BRL. A portfolio with USD positions is consolidated only when an
explicit USD/BRL rate has a positive `BigDecimal` rate, provider, reference instant, fetch instant and reference
kind. If any required quote or exchange rate is missing/stale/invalid, every dependent consolidated total is
`UNAVAILABLE`; a BRL subtotal is never presented as the complete portfolio. Arithmetic keeps native precision,
converts with the traceable rate and applies `HALF_UP` only to the final displayed total.

No default exchange rate exists. The deterministic adapter accepts only the four explicit runtime values
`PORTFOLIO_USD_BRL_RATE`, `PORTFOLIO_USD_BRL_PROVIDER`, `PORTFOLIO_USD_BRL_REFERENCE_AT` and
`PORTFOLIO_USD_BRL_FETCHED_AT`; partial or invalid configuration fails fast. The initial operational adapter is
a configured academic/reference source, not a claim of real-time or executable price. Default freshness is 30
minutes for quotes and 36 hours for exchange references, configurable by ISO-8601 durations. Outage or absent
configuration yields `UNAVAILABLE`.

## Dashboard field ownership

| UI information | Backend field/source |
|---|---|
| patrimônio | `patrimony`, calculated from complete current position values |
| custo | `cost`, calculated from persisted average prices and quantities |
| resultado não realizado | `unrealizedResult` and optional `unrealizedResultPercentage` |
| positions | enriched `positions`, kept per asset-broker pair |
| recent movements | last five persisted `recentMovements` |
| quote quality | `quoteSources` and each position's `quoteProvenance` |
| exchange quality | `exchangeSource` |
| temporal reference | UTC `asOf`; quote/exchange instants are ISO 8601 |

The contract intentionally has no cash, available balance, dividends, taxes, fees, income, daily return or
performance history. Historical transactions did not persist quote provenance; their explicit state is
`HISTORICAL_QUOTE_PROVENANCE_NOT_RECORDED`. Their legacy `LocalDateTime` is emitted with an offset derived from
the configured server zone and marked as `LEGACY_SERVER_ZONE:<zone>` instead of silently assuming browser time.

## Quote provenance

New quote writes persist source type, public provider identifier, reference/fetch instants, reference kind and
currency. Providers that do not supply a market reference instant are labeled `FETCH_TIME_PROXY`; the API does
not present that instant as an exchange timestamp. Rows created before the migration keep nullable provenance
and are returned as `QUOTE_PROVENANCE_UNAVAILABLE`, never attributed retroactively to the current provider.

## Representative unavailable metric

```json
{
  "availability": "UNAVAILABLE",
  "value": null,
  "currency": "BRL",
  "reason": "QUOTE_OR_EXCHANGE_UNAVAILABLE"
}
```

All endpoints remain protected by the existing administrative session and use the existing error envelope for
total request failures. Partial expected failures remain HTTP 200 with per-field availability.
