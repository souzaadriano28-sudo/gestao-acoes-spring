package com.trabalho.gestao_acoes.services.ports;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

public record ExchangeRate(String baseCurrency, String quoteCurrency, BigDecimal rate,
        String sourceType, String provider, Instant referenceAt, Instant fetchedAt, String referenceKind) {
    public ExchangeRate {
        try { baseCurrency = Currency.getInstance(baseCurrency).getCurrencyCode(); quoteCurrency = Currency.getInstance(quoteCurrency).getCurrencyCode(); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("Invalid currency pair", ex); }
        if (baseCurrency.equals(quoteCurrency)) throw new IllegalArgumentException("Invalid currency pair");
        if (rate == null || rate.signum() <= 0) throw new IllegalArgumentException("Rate must be positive");
        if (sourceType == null || sourceType.isBlank() || provider == null || provider.isBlank()) throw new IllegalArgumentException("Rate source is required");
        if (referenceAt == null || fetchedAt == null || referenceAt.isAfter(fetchedAt)) throw new IllegalArgumentException("Rate instants are invalid");
        if (referenceKind == null || referenceKind.isBlank()) throw new IllegalArgumentException("Rate reference kind is required");
    }
}
