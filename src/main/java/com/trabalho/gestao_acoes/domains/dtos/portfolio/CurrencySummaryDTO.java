package com.trabalho.gestao_acoes.domains.dtos.portfolio;

/** Native-currency aggregate. Values from different currencies are never combined here. */
public record CurrencySummaryDTO(String currency, MoneyMetricDTO patrimony, MoneyMetricDTO cost,
        MoneyMetricDTO unrealizedResult, PercentageMetricDTO unrealizedResultPercentage) {}
