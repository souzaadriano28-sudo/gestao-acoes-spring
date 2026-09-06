package com.trabalho.gestao_acoes.domains.dtos.portfolio;

import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

public record DashboardDTO(@JsonFormat(shape = JsonFormat.Shape.STRING) Instant asOf, String presentationCurrency, int positionCount,
        MoneyMetricDTO patrimony, MoneyMetricDTO cost, MoneyMetricDTO unrealizedResult,
        PercentageMetricDTO unrealizedResultPercentage, List<DetailedPositionDTO> positions,
        List<MovementDTO> recentMovements, List<QuoteProvenanceDTO> quoteSources,
        ExchangeProvenanceDTO exchangeSource) {}
