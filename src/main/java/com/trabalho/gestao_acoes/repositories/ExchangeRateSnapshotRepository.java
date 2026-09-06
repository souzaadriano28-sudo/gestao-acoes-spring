package com.trabalho.gestao_acoes.repositories;

import com.trabalho.gestao_acoes.domains.ExchangeRateSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateSnapshotRepository extends JpaRepository<ExchangeRateSnapshot, Long> {
    Optional<ExchangeRateSnapshot> findByBaseCurrencyAndQuoteCurrency(String baseCurrency, String quoteCurrency);
}
