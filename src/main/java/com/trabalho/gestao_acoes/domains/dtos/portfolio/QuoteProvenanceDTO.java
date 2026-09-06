package com.trabalho.gestao_acoes.domains.dtos.portfolio;

import com.trabalho.gestao_acoes.domains.enums.Availability;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonFormat;

public record QuoteProvenanceDTO(Availability availability, String sourceType, String provider,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant referenceAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant fetchedAt,
        String referenceKind, String currency, String reason) {
    public QuoteProvenanceDTO {
        if (availability == null) throw new IllegalArgumentException("availability is required");
        currency = isoCurrency(currency, "currency");
        if (availability != Availability.UNAVAILABLE) {
            required(sourceType, "sourceType"); required(provider, "provider");
            required(referenceKind, "referenceKind");
            if (referenceAt == null || fetchedAt == null) throw new IllegalArgumentException("quote timestamps are required");
            if (referenceAt.isAfter(fetchedAt)) throw new IllegalArgumentException("quote referenceAt cannot be after fetchedAt");
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
