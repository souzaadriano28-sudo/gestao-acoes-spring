package com.trabalho.gestao_acoes.domains.dtos.portfolio;

import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

public record MovementDTO(Long id, String type, Long assetId, String ticker, String market,
        Long brokerId, String brokerName, Integer quantity, MoneyMetricDTO unitPrice,
        @JsonFormat(shape = JsonFormat.Shape.STRING) OffsetDateTime recordedAt,
        String timeBasis, QuoteProvenanceDTO historicalQuoteProvenance) {}
