package com.trabalho.gestao_acoes.domains.dtos.portfolio;

import com.trabalho.gestao_acoes.domains.enums.Availability;
import java.math.BigDecimal;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonFormat;

public record ExchangeProvenanceDTO(Availability availability, String baseCurrency, String quoteCurrency,
        BigDecimal rate, String sourceType, String provider,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant referenceAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant fetchedAt,
        String referenceKind, String reason) {
    public ExchangeProvenanceDTO {
        if (availability == null) throw new IllegalArgumentException("availability is required");
        baseCurrency = isoCurrency(baseCurrency, "baseCurrency");
        quoteCurrency = isoCurrency(quoteCurrency, "quoteCurrency");
        if (baseCurrency.equals(quoteCurrency)) throw new IllegalArgumentException("exchange currencies must differ");
        if (availability != Availability.UNAVAILABLE) {
            if (rate == null || rate.signum() <= 0) throw new IllegalArgumentException("available exchange rate must be positive");
            required(sourceType, "sourceType"); required(provider, "provider"); required(referenceKind, "referenceKind");
            if (referenceAt == null || fetchedAt == null) throw new IllegalArgumentException("exchange timestamps are required");
            if (referenceAt.isAfter(fetchedAt)) throw new IllegalArgumentException("exchange referenceAt cannot be after fetchedAt");
        } else if (rate != null) {
            throw new IllegalArgumentException("unavailable exchange rate must not expose a value");
        }
    }

    private static String isoCurrency(String value, String field) {
        required(value, field);
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        try { java.util.Currency.getInstance(normalized); } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(field + " must be an ISO-4217 currency", error);
        }
        return normalized;
    }

    private static void required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
