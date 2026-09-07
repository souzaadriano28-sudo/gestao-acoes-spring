package com.trabalho.gestao_acoes.integrations.exchange;

import com.trabalho.gestao_acoes.domains.ExchangeRateSnapshot;
import com.trabalho.gestao_acoes.repositories.ExchangeRateSnapshotRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BcbPtaxExchangeRateAdapterTest {
    private static final Instant NOW = Instant.parse("2026-09-06T15:00:00Z");

    @Test
    void persistsLatestOfficialClosingReferenceWithOriginalTimestampsAndDecimalPrecision() {
        BcbPtaxClient client = mock(BcbPtaxClient.class);
        ExchangeRateSnapshotRepository repository = mock(ExchangeRateSnapshotRepository.class);
        when(repository.findByBaseCurrencyAndQuoteCurrency("USD", "BRL")).thenReturn(Optional.empty());
        when(client.period(anyString(), anyString(), eq("json"), eq(100))).thenReturn(new BcbPtaxResponse(List.of(
                new BcbPtaxResponse.Quote(new BigDecimal("5.43219876"), "2026-09-05 13:05:00.000", "FECHAMENTO PTAX"),
                new BcbPtaxResponse.Quote(new BigDecimal("9.99"), "2026-09-05 14:00:00.000", "ABERTURA"))));

        var rate = adapter(client, repository).find("USD", "BRL").orElseThrow();

        assertThat(rate.rate()).isEqualByComparingTo("5.43219876");
        assertThat(rate.referenceAt()).isEqualTo(Instant.parse("2026-09-05T16:05:00Z"));
        assertThat(rate.fetchedAt()).isEqualTo(NOW);
        assertThat(rate.provider()).isEqualTo(BcbPtaxExchangeRateAdapter.PROVIDER);
        ArgumentCaptor<ExchangeRateSnapshot> saved = ArgumentCaptor.forClass(ExchangeRateSnapshot.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getFetchedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getReferenceAt()).isEqualTo(rate.referenceAt());
    }

    @Test
    void timeoutOrRateLimitFallsBackToPersistedObservationWithoutRefreshingItsAge() {
        BcbPtaxClient client = mock(BcbPtaxClient.class);
        ExchangeRateSnapshotRepository repository = mock(ExchangeRateSnapshotRepository.class);
        ExchangeRateSnapshot cached = snapshot(Instant.parse("2026-09-03T16:05:00Z"), Instant.parse("2026-09-03T16:10:00Z"));
        when(repository.findByBaseCurrencyAndQuoteCurrency("USD", "BRL")).thenReturn(Optional.of(cached));
        when(client.period(anyString(), anyString(), anyString(), anyInt())).thenThrow(new RuntimeException("HTTP 429 or timeout"));

        var rate = adapter(client, repository).find("USD", "BRL").orElseThrow();

        assertThat(rate.fetchedAt()).isEqualTo(Instant.parse("2026-09-03T16:10:00Z"));
        assertThat(rate.referenceAt()).isEqualTo(Instant.parse("2026-09-03T16:05:00Z"));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void invalidResponseWithoutCacheIsUnavailableAndOtherPairsAreNotInvented() {
        BcbPtaxClient client = mock(BcbPtaxClient.class);
        ExchangeRateSnapshotRepository repository = mock(ExchangeRateSnapshotRepository.class);
        when(repository.findByBaseCurrencyAndQuoteCurrency("USD", "BRL")).thenReturn(Optional.empty());
        when(client.period(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new BcbPtaxResponse(List.of(new BcbPtaxResponse.Quote(BigDecimal.ZERO, null, "FECHAMENTO PTAX"))));
        var adapter = adapter(client, repository);

        assertThat(adapter.find("USD", "BRL")).isEmpty();
        assertThat(adapter.find("EUR", "BRL")).isEmpty();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentFirstInsertReturnsTheSnapshotCommittedByTheWinner() {
        BcbPtaxClient client = mock(BcbPtaxClient.class);
        ExchangeRateSnapshotRepository repository = mock(ExchangeRateSnapshotRepository.class);
        ExchangeRateSnapshot winner = snapshot(Instant.parse("2026-09-05T16:05:00Z"), NOW);
        when(repository.findByBaseCurrencyAndQuoteCurrency("USD", "BRL"))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(client.period(anyString(), anyString(), anyString(), anyInt())).thenReturn(new BcbPtaxResponse(List.of(
                new BcbPtaxResponse.Quote(new BigDecimal("5.20"), "2026-09-05 13:05:00", "FECHAMENTO PTAX"))));
        when(repository.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("pair race"));

        var result = adapter(client, repository).find("USD", "BRL").orElseThrow();

        assertThat(result.rate()).isEqualByComparingTo("5.10000000");
        assertThat(result.fetchedAt()).isEqualTo(NOW);
    }

    private static BcbPtaxExchangeRateAdapter adapter(BcbPtaxClient client, ExchangeRateSnapshotRepository repository) {
        return new BcbPtaxExchangeRateAdapter(client, repository, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofHours(1), 10);
    }

    private static ExchangeRateSnapshot snapshot(Instant reference, Instant fetched) {
        ExchangeRateSnapshot value = new ExchangeRateSnapshot();
        value.setBaseCurrency("USD"); value.setQuoteCurrency("BRL"); value.setRate(new BigDecimal("5.10000000"));
        value.setSourceType("OFFICIAL_REFERENCE_RATE"); value.setProvider(BcbPtaxExchangeRateAdapter.PROVIDER);
        value.setReferenceAt(reference); value.setFetchedAt(fetched); value.setReferenceKind("BCB_PTAX_CLOSING_REFERENCE");
        return value;
    }
}
