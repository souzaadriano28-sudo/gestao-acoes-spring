package com.trabalho.gestao_acoes.services.ports;

import java.util.Optional;

public interface ExchangeRatePort {
    Optional<ExchangeRate> find(String baseCurrency, String quoteCurrency);
}
