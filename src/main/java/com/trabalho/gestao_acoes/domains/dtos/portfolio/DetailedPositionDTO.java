package com.trabalho.gestao_acoes.domains.dtos.portfolio;

public record DetailedPositionDTO(Long positionId, Long assetId, String ticker, String market,
        Long brokerId, String brokerName, Integer quantity, String nativeCurrency,
        MoneyMetricDTO averagePrice, MoneyMetricDTO cost, MoneyMetricDTO currentQuote,
        MoneyMetricDTO marketValue, MoneyMetricDTO unrealizedResult, QuoteProvenanceDTO quoteProvenance) {}
