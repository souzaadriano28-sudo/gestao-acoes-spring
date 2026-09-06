package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.dtos.portfolio.MoneyMetricDTO;
import com.trabalho.gestao_acoes.services.ports.ExchangeRate;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortfolioValueObjectsTest {
    @Test void availableMoneyRejectsMissingCurrencyOrValue() {
        assertThatThrownBy(() -> MoneyMetricDTO.available(null, "BRL")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoneyMetricDTO.available(BigDecimal.ONE, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoneyMetricDTO.available(BigDecimal.ONE, "NOT_A_CURRENCY")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void exchangeRateRejectsInvalidPairSourceAndInstants() {
        Instant now = Instant.parse("2026-09-06T12:00:00Z");
        assertThatThrownBy(() -> new ExchangeRate("USD", "USD", BigDecimal.ONE, "TYPE", "P", now, now, "REFERENCE"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExchangeRate("USD", "BRL", BigDecimal.ONE, null, "P", now, now, "REFERENCE"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExchangeRate("USD", "BRL", BigDecimal.ONE, "TYPE", "P", now.plusSeconds(1), now, "REFERENCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
