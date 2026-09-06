package com.trabalho.gestao_acoes.domains.dtos.portfolio;

import com.trabalho.gestao_acoes.domains.enums.Availability;
import java.math.BigDecimal;
import java.util.Currency;

public record MoneyMetricDTO(Availability availability, BigDecimal value, String currency, String reason) {
    public static MoneyMetricDTO available(BigDecimal value, String currency) {
        return new MoneyMetricDTO(Availability.AVAILABLE, requireValue(value), iso(currency), null);
    }
    public static MoneyMetricDTO stale(BigDecimal value, String currency, String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Stale money requires reason");
        return new MoneyMetricDTO(Availability.STALE, requireValue(value), iso(currency), reason);
    }
    public static MoneyMetricDTO unavailable(String currency, String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Unavailable money requires reason");
        return new MoneyMetricDTO(Availability.UNAVAILABLE, null, iso(currency), reason);
    }
    private static BigDecimal requireValue(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Money value is required");
        return value;
    }
    private static String iso(String currency) {
        try { return Currency.getInstance(currency).getCurrencyCode(); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("ISO currency is required", ex); }
    }
}
