package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.*;
import com.trabalho.gestao_acoes.domains.enums.Availability;
import com.trabalho.gestao_acoes.repositories.*;
import com.trabalho.gestao_acoes.services.ports.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PortfolioReadServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-06T12:00:00Z");
    private final PosicaoCarteiraRepository positions = mock(PosicaoCarteiraRepository.class);
    private final TransacaoRepository transactions = mock(TransacaoRepository.class);
    private final ExchangeRatePort exchange = mock(ExchangeRatePort.class);
    private PortfolioReadService service;

    @BeforeEach void setUp() {
        service = new PortfolioReadService(positions, transactions, exchange, Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(30), Duration.ofHours(36), "America/Sao_Paulo");
        when(transactions.findMovements(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());
    }

    @Test void emptyPortfolioReturnsConfirmedZeroWithoutInventingCashOrPercentage() {
        when(positions.findAllDetailed()).thenReturn(List.of());
        var dashboard = service.dashboard();
        assertThat(dashboard.patrimony().value()).isEqualByComparingTo("0.00");
        assertThat(dashboard.patrimony().availability()).isEqualTo(Availability.AVAILABLE);
        assertThat(dashboard.unrealizedResultPercentage().availability()).isEqualTo(Availability.UNAVAILABLE);
        verifyNoInteractions(exchange);
    }

    @Test void completeMixedCurrencyPortfolioUsesTraceableRateAndOneFinalRounding() {
        var broker = broker();
        var brl = position(1L, asset(1L, "PETR4", "BRASIL", "BRL", "20", NOW.minusSeconds(60)), broker, 6, "18");
        var usd = position(2L, asset(2L, "AAPL", "AMERICANO", "USD", "100", NOW.minusSeconds(60)), broker, 2, "90");
        when(positions.findAllDetailed()).thenReturn(List.of(brl, usd));
        when(exchange.find("USD", "BRL")).thenReturn(Optional.of(rate("5.25", NOW.minusSeconds(3600))));
        var dashboard = service.dashboard();
        assertThat(dashboard.patrimony().value()).isEqualByComparingTo("1170.00");
        assertThat(dashboard.cost().value()).isEqualByComparingTo("1053.00");
        assertThat(dashboard.unrealizedResult().value()).isEqualByComparingTo("117.00");
        assertThat(dashboard.exchangeSource().provider()).isEqualTo("TEST_REFERENCE");
    }

    @Test void missingQuotePreservesPersistedCostAndMakesDependentTotalsUnavailable() {
        var asset = asset(1L, "PETR4", "BRASIL", "BRL", "20", NOW.minusSeconds(60));
        asset.setQuoteProvider(null);
        when(positions.findAllDetailed()).thenReturn(List.of(position(1L, asset, broker(), 2, "18")));
        var dashboard = service.dashboard();
        assertThat(dashboard.cost().availability()).isEqualTo(Availability.AVAILABLE);
        assertThat(dashboard.patrimony().availability()).isEqualTo(Availability.UNAVAILABLE);
        assertThat(dashboard.positions().get(0).cost().value()).isEqualByComparingTo("36.00");
        assertThat(dashboard.positions().get(0).marketValue().value()).isNull();
    }

    @Test void missingExchangeMakesEveryMixedCurrencyConsolidatedTotalUnavailable() {
        when(positions.findAllDetailed()).thenReturn(List.of(position(1L,
                asset(1L, "AAPL", "AMERICANO", "USD", "100", NOW.minusSeconds(60)), broker(), 2, "90")));
        when(exchange.find("USD", "BRL")).thenReturn(Optional.empty());
        var dashboard = service.dashboard();
        assertThat(dashboard.cost().availability()).isEqualTo(Availability.UNAVAILABLE);
        assertThat(dashboard.patrimony().availability()).isEqualTo(Availability.UNAVAILABLE);
        assertThat(dashboard.exchangeSource().reason()).isEqualTo("EXCHANGE_RATE_UNAVAILABLE");
    }

    @Test void partialProviderFailureIsContainedAndDeclared() {
        when(positions.findAllDetailed()).thenReturn(List.of(position(1L,
                asset(1L, "AAPL", "AMERICANO", "USD", "100", NOW.minusSeconds(60)), broker(), 1, "90")));
        when(exchange.find("USD", "BRL")).thenThrow(new IllegalStateException("provider down"));
        var dashboard = service.dashboard();
        assertThat(dashboard.positions()).hasSize(1);
        assertThat(dashboard.positions().get(0).marketValue().availability()).isEqualTo(Availability.AVAILABLE);
        assertThat(dashboard.patrimony().availability()).isEqualTo(Availability.UNAVAILABLE);
        assertThat(dashboard.exchangeSource().reason()).isEqualTo("EXCHANGE_PROVIDER_FAILURE");
    }

    @Test void staleQuoteRemainsVisibleButCannotProduceACompleteDashboardTotal() {
        when(positions.findAllDetailed()).thenReturn(List.of(position(1L,
                asset(1L, "PETR4", "BRASIL", "BRL", "20", NOW.minusSeconds(3600)), broker(), 2, "18")));
        var dashboard = service.dashboard();
        assertThat(dashboard.positions().get(0).currentQuote().availability()).isEqualTo(Availability.STALE);
        assertThat(dashboard.positions().get(0).currentQuote().value()).isEqualByComparingTo("20");
        assertThat(dashboard.patrimony().availability()).isEqualTo(Availability.UNAVAILABLE);
    }

    @Test void staleExchangeIsExposedButNeverUsedForConsolidation() {
        when(positions.findAllDetailed()).thenReturn(List.of(position(1L,
                asset(1L, "AAPL", "AMERICANO", "USD", "100", NOW.minusSeconds(60)), broker(), 1, "90")));
        when(exchange.find("USD", "BRL")).thenReturn(Optional.of(rate("5.25", NOW.minus(Duration.ofDays(3)))));
        var dashboard = service.dashboard();
        assertThat(dashboard.exchangeSource().availability()).isEqualTo(Availability.STALE);
        assertThat(dashboard.exchangeSource().rate()).isEqualByComparingTo("5.25");
        assertThat(dashboard.patrimony().availability()).isEqualTo(Availability.UNAVAILABLE);
    }

    @Test void movementFiltersRejectInvalidPagingTypePeriodAndBrokerBeforeQuerying() {
        assertThatThrownBy(() -> service.movements(-1, 20, null, null, null, null, null)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.movements(0, 101, null, null, null, null, null)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.movements(0, 20, "DIVIDENDO", null, null, null, null)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.movements(0, 20, null, null, -1L, null, null)).isInstanceOf(RuntimeException.class);
        OffsetDateTime later = OffsetDateTime.parse("2026-09-07T12:00:00Z");
        OffsetDateTime earlier = OffsetDateTime.parse("2026-09-06T12:00:00Z");
        assertThatThrownBy(() -> service.movements(0, 20, null, null, null, later, earlier)).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(positions);
    }

    @Test void emptyMovementPageKeepsRequestedPagingMetadata() {
        when(transactions.findMovements(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 25), 0));
        var page = service.movements(2, 25, null, null, null, null, null);
        assertThat(page.items()).isEmpty();
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(25);
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    private static Acao asset(Long id, String ticker, String market, String currency, String quote, Instant reference) {
        Acao a = new Acao(id, ticker, ticker, market, currency, new BigDecimal(quote), LocalDateTime.ofInstant(reference, ZoneOffset.UTC));
        a.setQuoteSourceType("MARKET_DATA_PROVIDER"); a.setQuoteProvider("TEST_QUOTES");
        a.setQuoteReferenceAt(reference); a.setQuoteFetchedAt(reference); a.setQuoteReferenceKind("PROVIDER_REFERENCE_TIME");
        return a;
    }
    private static Corretora broker() { Corretora b = new Corretora(); b.setId(7L); b.setRazaoSocial("Corretora Teste"); return b; }
    private static PosicaoCarteira position(Long id, Acao a, Corretora b, int quantity, String average) {
        return new PosicaoCarteira(id, quantity, new BigDecimal(average), a, b);
    }
    private static ExchangeRate rate(String value, Instant reference) {
        return new ExchangeRate("USD", "BRL", new BigDecimal(value), "CONFIGURED_REFERENCE", "TEST_REFERENCE",
                reference, reference.plusSeconds(10), "EXPLICIT_REFERENCE_INSTANT");
    }
}
