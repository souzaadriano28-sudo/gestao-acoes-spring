package com.trabalho.gestao_acoes.integrations.exchange;

import com.trabalho.gestao_acoes.domains.ExchangeRateSnapshot;
import com.trabalho.gestao_acoes.repositories.ExchangeRateSnapshotRepository;
import com.trabalho.gestao_acoes.services.MoneyPolicy;
import com.trabalho.gestao_acoes.services.ports.ExchangeRate;
import com.trabalho.gestao_acoes.services.ports.ExchangeRatePort;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;

@Component
@ConditionalOnProperty(name = "app.portfolio.exchange.mode", havingValue = "bcb-ptax", matchIfMissing = true)
public class BcbPtaxExchangeRateAdapter implements ExchangeRatePort {
    static final String PROVIDER = "BANCO_CENTRAL_DO_BRASIL_PTAX";
    private static final ZoneId BCB_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter QUERY_DATE = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final DateTimeFormatter RESPONSE_DATE = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd().toFormatter();
    private final BcbPtaxClient client;
    private final ExchangeRateSnapshotRepository snapshots;
    private final Clock clock;
    private final Duration refreshAfter;
    private final int lookbackDays;

    public BcbPtaxExchangeRateAdapter(BcbPtaxClient client, ExchangeRateSnapshotRepository snapshots, Clock clock,
            @Value("${app.portfolio.exchange.refresh-after:PT1H}") Duration refreshAfter,
            @Value("${app.portfolio.exchange.ptax-lookback-days:10}") int lookbackDays) {
        this.client = client;
        this.snapshots = snapshots;
        this.clock = clock;
        if (refreshAfter == null || refreshAfter.isNegative() || refreshAfter.isZero()) throw new IllegalArgumentException("refresh-after must be positive");
        if (lookbackDays < 1 || lookbackDays > 31) throw new IllegalArgumentException("ptax-lookback-days must be between 1 and 31");
        this.refreshAfter = refreshAfter;
        this.lookbackDays = lookbackDays;
    }

    @Override
    public Optional<ExchangeRate> find(String baseCurrency, String quoteCurrency) {
        if (!"USD".equals(baseCurrency) || !"BRL".equals(quoteCurrency)) return Optional.empty();
        Instant now = clock.instant();
        Optional<ExchangeRateSnapshot> cached = snapshots.findByBaseCurrencyAndQuoteCurrency("USD", "BRL");
        Optional<ExchangeRate> cachedDomain = cached.flatMap(this::safeDomain);
        if (cachedDomain.filter(rate -> !rate.fetchedAt().plus(refreshAfter).isBefore(now)).isPresent()) return cachedDomain;
        ExchangeRate rate;
        try {
            LocalDate today = LocalDate.ofInstant(now, BCB_ZONE);
            BcbPtaxResponse response = client.period("'" + today.minusDays(lookbackDays).format(QUERY_DATE) + "'",
                    "'" + today.format(QUERY_DATE) + "'", "json", 100);
            BcbPtaxResponse.Quote selected = Optional.ofNullable(response).map(BcbPtaxResponse::value).stream()
                    .flatMap(java.util.Collection::stream)
                    .filter(q -> q != null && q.cotacaoVenda() != null && q.cotacaoVenda().signum() > 0)
                    .filter(q -> "FECHAMENTO PTAX".equalsIgnoreCase(q.tipoBoletim()))
                    .max(Comparator.comparing(q -> parseReference(q.dataHoraCotacao()))).orElseThrow();
            Instant referenceAt = parseReference(selected.dataHoraCotacao());
            rate = new ExchangeRate("USD", "BRL", MoneyPolicy.quote(selected.cotacaoVenda()),
                    "OFFICIAL_REFERENCE_RATE", PROVIDER, referenceAt, now, "BCB_PTAX_CLOSING_REFERENCE");
        } catch (RuntimeException invalidOrUnavailable) {
            return cachedDomain;
        }
        ExchangeRateSnapshot entity = cached.orElseGet(ExchangeRateSnapshot::new);
        copy(rate, entity);
        try {
            snapshots.saveAndFlush(entity);
            return Optional.of(rate);
        } catch (DataIntegrityViolationException concurrentFirstInsert) {
            return snapshots.findByBaseCurrencyAndQuoteCurrency("USD", "BRL").flatMap(this::safeDomain);
        }
    }

    private Optional<ExchangeRate> safeDomain(ExchangeRateSnapshot entity) {
        try {
            return Optional.of(new ExchangeRate(entity.getBaseCurrency(), entity.getQuoteCurrency(), entity.getRate(),
                    entity.getSourceType(), entity.getProvider(), entity.getReferenceAt(), entity.getFetchedAt(), entity.getReferenceKind()));
        } catch (RuntimeException invalidPersistedValue) {
            return Optional.empty();
        }
    }

    private static Instant parseReference(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("PTAX reference is missing");
        return LocalDateTime.parse(value, RESPONSE_DATE).atZone(BCB_ZONE).toInstant();
    }

    private static void copy(ExchangeRate rate, ExchangeRateSnapshot entity) {
        entity.setBaseCurrency(rate.baseCurrency()); entity.setQuoteCurrency(rate.quoteCurrency());
        entity.setRate(rate.rate()); entity.setSourceType(rate.sourceType()); entity.setProvider(rate.provider());
        entity.setReferenceAt(rate.referenceAt()); entity.setFetchedAt(rate.fetchedAt());
        entity.setReferenceKind(rate.referenceKind());
    }
}
