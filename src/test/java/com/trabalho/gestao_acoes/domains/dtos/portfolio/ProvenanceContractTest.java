package com.trabalho.gestao_acoes.domains.dtos.portfolio;

import com.trabalho.gestao_acoes.domains.enums.Availability;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProvenanceContractTest {
    private static final Instant FETCHED = Instant.parse("2026-09-06T15:00:00Z");

    @Test
    void exchangeRequiresPositiveDecimalOfficialFieldsAndOrderedInstants() {
        assertThatThrownBy(() -> new ExchangeProvenanceDTO(Availability.AVAILABLE, "USD", "BRL", BigDecimal.ZERO,
                "OFFICIAL_REFERENCE_RATE", "BCB", FETCHED, FETCHED, "PTAX", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExchangeProvenanceDTO(Availability.AVAILABLE, "USD", "BRL", BigDecimal.ONE,
                "OFFICIAL_REFERENCE_RATE", "BCB", FETCHED.plusSeconds(1), FETCHED, "PTAX", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExchangeProvenanceDTO(Availability.UNAVAILABLE, "USD", "BRL", BigDecimal.ONE,
                null, null, null, null, null, "FAILED")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void quoteRequiresIsoCurrencyAndCompleteProvenanceWhenUsable() {
        assertThatThrownBy(() -> new QuoteProvenanceDTO(Availability.AVAILABLE, null, null, FETCHED, FETCHED,
                null, "BRL", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuoteProvenanceDTO(Availability.UNAVAILABLE, null, null, null, null,
                null, "REAL", "FAILED")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new QuoteProvenanceDTO(Availability.UNAVAILABLE, null, null, null, null,
                null, "brl", "FAILED").currency()).isEqualTo("BRL");
    }
}
