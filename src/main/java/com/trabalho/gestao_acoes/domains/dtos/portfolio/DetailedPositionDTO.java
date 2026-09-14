package com.trabalho.gestao_acoes.domains.dtos.portfolio;

public record DetailedPositionDTO(Long positionId, Long assetId, String ticker, String assetName, String market,
        Long brokerId, String brokerName, Integer quantity, String nativeCurrency,
        MoneyMetricDTO averagePrice, MoneyMetricDTO cost, MoneyMetricDTO currentQuote,
        MoneyMetricDTO marketValue, MoneyMetricDTO unrealizedResult, PercentageMetricDTO unrealizedResultPercentage,
        MoneyMetricDTO realizedResult, QuoteProvenanceDTO quoteProvenance) {
    public DetailedPositionDTO(Long positionId, Long assetId, String ticker, String market, Long brokerId, String brokerName,
            Integer quantity, String nativeCurrency, MoneyMetricDTO averagePrice, MoneyMetricDTO cost,
            MoneyMetricDTO currentQuote, MoneyMetricDTO marketValue, MoneyMetricDTO unrealizedResult, QuoteProvenanceDTO quoteProvenance) {
        this(positionId, assetId, ticker, null, market, brokerId, brokerName, quantity, nativeCurrency, averagePrice, cost,
                currentQuote, marketValue, unrealizedResult, PercentageMetricDTO.unavailable("NOT_RECORDED"),
                MoneyMetricDTO.unavailable(nativeCurrency, "NOT_RECORDED"), quoteProvenance);
    }
}
