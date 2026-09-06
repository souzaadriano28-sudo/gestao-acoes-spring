package com.trabalho.gestao_acoes.integrations.exchange;

import com.trabalho.gestao_acoes.services.ports.ExchangeRate;
import com.trabalho.gestao_acoes.services.ports.ExchangeRatePort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredExchangeRateAdapter implements ExchangeRatePort {
    private final Optional<ExchangeRate> usdBrl;

    public ConfiguredExchangeRateAdapter(
            @Value("${app.portfolio.exchange.usd-brl.rate:}") String rate,
            @Value("${app.portfolio.exchange.usd-brl.provider:}") String provider,
            @Value("${app.portfolio.exchange.usd-brl.reference-at:}") String referenceAt,
            @Value("${app.portfolio.exchange.usd-brl.fetched-at:}") String fetchedAt) {
        this.usdBrl = build(rate, provider, referenceAt, fetchedAt);
    }

    private static Optional<ExchangeRate> build(String rate, String provider, String referenceAt, String fetchedAt) {
        if (rate.isBlank() && provider.isBlank() && referenceAt.isBlank() && fetchedAt.isBlank()) return Optional.empty();
        try {
            return Optional.of(new ExchangeRate("USD", "BRL", new BigDecimal(rate), "CONFIGURED_REFERENCE",
                    provider, Instant.parse(referenceAt), Instant.parse(fetchedAt), "EXPLICIT_REFERENCE_INSTANT"));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Configuração USD/BRL inválida ou incompleta.", ex);
        }
    }

    @Override
    public Optional<ExchangeRate> find(String baseCurrency, String quoteCurrency) {
        return "USD".equals(baseCurrency) && "BRL".equals(quoteCurrency) ? usdBrl : Optional.empty();
    }
}
