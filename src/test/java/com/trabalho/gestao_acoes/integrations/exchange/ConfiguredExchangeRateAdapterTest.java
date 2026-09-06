package com.trabalho.gestao_acoes.integrations.exchange;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ConfiguredExchangeRateAdapterTest {
    @Test void completelyAbsentConfigurationMeansUnavailableInsteadOfAFixedFallback() {
        assertThat(new ConfiguredExchangeRateAdapter("", "", "", "").find("USD", "BRL")).isEmpty();
    }

    @Test void explicitTraceableConfigurationRoundTripsExactly() {
        var rate = new ConfiguredExchangeRateAdapter("5.12345678", "ACADEMIC_FIXTURE",
                "2026-09-06T10:00:00Z", "2026-09-06T10:01:00Z").find("USD", "BRL").orElseThrow();
        assertThat(rate.rate()).isEqualByComparingTo("5.12345678");
        assertThat(rate.provider()).isEqualTo("ACADEMIC_FIXTURE");
        assertThat(new ConfiguredExchangeRateAdapter("5.1", "P", "2026-09-06T10:00:00Z",
                "2026-09-06T10:01:00Z").find("BRL", "USD")).isEmpty();
    }

    @Test void partialOrInvalidConfigurationFailsFast() {
        assertThatThrownBy(() -> new ConfiguredExchangeRateAdapter("5.1", "", "", ""))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ConfiguredExchangeRateAdapter("-1", "P", "2026-09-06T10:00:00Z", "2026-09-06T10:01:00Z"))
                .isInstanceOf(IllegalStateException.class);
    }
}
