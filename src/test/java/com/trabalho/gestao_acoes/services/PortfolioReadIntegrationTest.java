package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.*;
import com.trabalho.gestao_acoes.domains.enums.*;
import com.trabalho.gestao_acoes.repositories.*;
import com.trabalho.gestao_acoes.services.ports.*;
import com.trabalho.gestao_acoes.domains.dtos.OperationRequestDTO;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class PortfolioReadIntegrationTest {
    @Autowired private PortfolioReadService service;
    @Autowired private OperationLedgerService ledger;
    @Autowired private AcaoRepository assets;
    @Autowired private CorretoraRepository brokers;
    @Autowired private PosicaoCarteiraRepository positions;
    @Autowired private TransacaoRepository transactions;
    @Autowired private com.trabalho.gestao_acoes.repositories.UserAccountRepository users;
    @Autowired private com.trabalho.gestao_acoes.repositories.PortfolioRepository portfolios;
    @MockitoBean private ExchangeRatePort exchangeRates;
    @MockitoBean private SecurityUtils securityUtils;

    private com.trabalho.gestao_acoes.domains.UserAccount user;
    private com.trabalho.gestao_acoes.domains.Portfolio defaultPortfolio;

    @BeforeEach void clean() {
        transactions.deleteAll();
        positions.deleteAll();
        assets.deleteAll();
        brokers.deleteAll();
        portfolios.deleteAll();
        users.deleteAll();
        reset(exchangeRates);

        user = users.save(new com.trabalho.gestao_acoes.domains.UserAccount("testuser", "test@user.com", "pass", Instant.now()));
        defaultPortfolio = portfolios.save(new com.trabalho.gestao_acoes.domains.Portfolio("Principal", user, Instant.now()));
        when(securityUtils.currentUser()).thenReturn(user);
        when(securityUtils.currentOwnerId()).thenReturn(user.getId());
    }

    @Test void emptyPortfolioIsAConfirmedSuccessfulRead() {
        var dashboard = service.dashboard();
        assertThat(dashboard.positionCount()).isZero();
        assertThat(dashboard.patrimony().availability()).isEqualTo(Availability.AVAILABLE);
        assertThat(dashboard.patrimony().value()).isEqualByComparingTo("0.00");
    }

    @Test void persistedPositionAndMovementsProduceEnrichedDeterministicReads() {
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        Corretora broker = brokers.save(broker("11222333000181", "Corretora Um"));
        Acao asset = assets.save(asset("PETR4", "BRASIL", "BRL", "22.50", now.minusSeconds(60), true));

        PosicaoCarteira pos = new PosicaoCarteira(null, 4, new BigDecimal("20.00"), asset, broker);
        pos.setPortfolio(defaultPortfolio);
        positions.save(pos);

        Transacao tx1 = new Transacao(null, TipoTransacao.COMPRA, 2, new BigDecimal("19.00"), LocalDateTime.now().minusDays(1), asset, broker);
        tx1.setPortfolio(defaultPortfolio);
        transactions.save(tx1);

        Transacao tx2 = new Transacao(null, TipoTransacao.COMPRA, 2, new BigDecimal("21.00"), LocalDateTime.now(), asset, broker);
        tx2.setPortfolio(defaultPortfolio);
        Transacao newest = transactions.save(tx2);

        var dashboard = service.dashboard();
        var movements = service.movements(0, 10, "COMPRA", "PETR4", broker.getId(), null, null);

        assertThat(dashboard.patrimony().value()).isEqualByComparingTo("90.00");
        assertThat(dashboard.cost().value()).isEqualByComparingTo("80.00");
        assertThat(dashboard.unrealizedResult().value()).isEqualByComparingTo("10.00");
        assertThat(dashboard.positions().get(0).positionId()).isNotNull();
        var detail = dashboard.positions().get(0);
        assertThat(detail.assetName()).isEqualTo("PETR4");
        assertThat(detail.brokerName()).isEqualTo("Corretora Um");
        assertThat(detail.nativeCurrency()).isEqualTo("BRL");
        assertThat(detail.quantity()).isEqualTo(4);
        assertThat(detail.averagePrice().value()).isEqualByComparingTo("20.00");
        assertThat(detail.cost().value()).isEqualByComparingTo("80.00");
        assertThat(detail.currentQuote().value()).isEqualByComparingTo("22.50");
        assertThat(detail.quoteProvenance().referenceAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS)).isEqualTo(now.minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        assertThat(detail.marketValue().value()).isEqualByComparingTo("90.00");
        assertThat(detail.unrealizedResult().value()).isEqualByComparingTo("10.00");
        assertThat(detail.unrealizedResultPercentage().value()).isEqualByComparingTo("12.5000");
        assertThat(detail.realizedResult().value()).isEqualByComparingTo("0");
        assertThat(movements.items()).hasSize(2);
        assertThat(movements.items().get(0).id()).isEqualTo(newest.getId());
        assertThat(movements.items().get(0).recordedAt().getOffset()).isNotNull();
        assertThat(movements.items().get(0).historicalQuoteProvenance().availability()).isEqualTo(Availability.UNAVAILABLE);
    }

    @Test void missingQuoteProvenanceReturnsUsefulPersistedDataAndExplicitPartialFailure() {
        Instant now = Instant.now();
        Corretora broker = brokers.save(broker("22333444000181", "Corretora Dois"));
        Acao asset = assets.save(asset("VALE3", "BRASIL", "BRL", "60.00", now, false));

        PosicaoCarteira pos = new PosicaoCarteira(null, 3, new BigDecimal("55.00"), asset, broker);
        pos.setPortfolio(defaultPortfolio);
        positions.save(pos);

        var dashboard = service.dashboard();
        assertThat(dashboard.positions().get(0).quantity()).isEqualTo(3);
        assertThat(dashboard.positions().get(0).cost().value()).isEqualByComparingTo("165.00");
        assertThat(dashboard.positions().get(0).marketValue().availability()).isEqualTo(Availability.UNAVAILABLE);
        assertThat(dashboard.positions().get(0).currentQuote().availability()).isEqualTo(Availability.UNAVAILABLE);
        assertThat(dashboard.positions().get(0).unrealizedResult().availability()).isEqualTo(Availability.UNAVAILABLE);
        assertThat(dashboard.patrimony().availability()).isEqualTo(Availability.UNAVAILABLE);
    }

    @Test void missingExchangeNeverReturnsAPartialBrlTotalForUsdPortfolio() {
        Instant now = Instant.now();
        Corretora broker = brokers.save(broker("33444555000181", "Corretora Três"));
        Acao asset = assets.save(asset("AAPL", "AMERICANO", "USD", "100.00", now.minusSeconds(60), true));

        PosicaoCarteira pos = new PosicaoCarteira(null, 2, new BigDecimal("90.00"), asset, broker);
        pos.setPortfolio(defaultPortfolio);
        positions.save(pos);

        when(exchangeRates.find("USD", "BRL")).thenReturn(Optional.empty());

        var dashboard = service.dashboard();
        assertThat(dashboard.positions().get(0).marketValue().value()).isEqualByComparingTo("200.00");
        assertThat(dashboard.positions().get(0).marketValue().currency()).isEqualTo("USD");
        assertThat(dashboard.patrimony().availability()).isEqualTo(Availability.UNAVAILABLE);
        assertThat(dashboard.patrimony().value()).isNull();
    }

    @Test void sameAssetInTwoBrokersRemainsTwoStableDetailedPositions() {
        Instant now = Instant.now();
        Acao asset = assets.save(asset("ITUB4", "BRASIL", "BRL", "35.00", now.minusSeconds(60), true));
        Corretora first = brokers.save(broker("44555666000181", "Alfa"));
        Corretora second = brokers.save(broker("55666777000181", "Beta"));

        PosicaoCarteira pos1 = new PosicaoCarteira(null, 1, new BigDecimal("30.00"), asset, first);
        pos1.setPortfolio(defaultPortfolio);
        positions.save(pos1);

        PosicaoCarteira pos2 = new PosicaoCarteira(null, 2, new BigDecimal("31.00"), asset, second);
        pos2.setPortfolio(defaultPortfolio);
        positions.save(pos2);

        var page = service.detailedPositions(0, 20, "BRASIL", null);
        assertThat(page.items()).hasSize(2).extracting(item -> item.brokerName()).containsExactly("Alfa", "Beta");
        assertThat(page.items()).extracting(item -> item.ticker()).containsOnly("ITUB4");
    }

    @Test void closedPositionLeavesOpenPositionsButKeepsRealizedResultInMovementHistory() {
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        Corretora broker = brokers.save(broker("66777888000181", "Corretora Fechada"));
        broker.setRegulatoryStatus(RegulatoryStatus.VERIFIED); brokers.saveAndFlush(broker);
        Acao asset = assets.save(asset("CLOSE3", "BRASIL", "BRL", "15.00", now.minusSeconds(60), true));
        var purchase = new OperationRequestDTO(TipoTransacao.COMPRA, asset.getId(), broker.getId(), LocalDateTime.of(2026, 2, 1, 10, 0), 2, "BRL", new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "compra", "closed-buy");
        var sale = new OperationRequestDTO(TipoTransacao.VENDA, asset.getId(), broker.getId(), LocalDateTime.of(2026, 2, 2, 10, 0), 2, "BRL", new BigDecimal("15.00"), new BigDecimal("1.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "venda", "closed-sale");
        ledger.create(purchase); var sold = ledger.create(sale);
        assertThat(service.detailedPositions(0, 20, "BRASIL", broker.getId()).items()).isEmpty();
        assertThat(transactions.findByIdAndPortfolioId(sold.id(), defaultPortfolio.getId()).orElseThrow().getResultadoRealizado()).isEqualByComparingTo("9.00000000");
        var history = service.movements(0, 20, "VENDA", "CLOSE3", broker.getId(), null, null);
        assertThat(history.items()).hasSize(1);
        // Closed groups are intentionally absent from open positions; the realized result is retained in the ledger transaction above.
    }

    private Acao asset(String ticker, String market, String currency, String quote, Instant reference, boolean provenance) {
        Acao asset = new Acao(null, ticker, ticker, market, currency, new BigDecimal(quote), LocalDateTime.now());
        if (provenance) {
            asset.setQuoteSourceType("MARKET_DATA_PROVIDER"); asset.setQuoteProvider("INTEGRATION_FIXTURE");
            asset.setQuoteReferenceAt(reference); asset.setQuoteFetchedAt(reference); asset.setQuoteReferenceKind("PROVIDER_REFERENCE_TIME");
        }
        asset.setOwner(user);
        return asset;
    }

    private Corretora broker(String cnpj, String name) {
        Corretora broker = new Corretora();
        broker.setCnpj(cnpj); broker.setRazaoSocial(name); broker.setCep("01001000");
        broker.setValidadaNaCvm(false); broker.setDataCadastro(LocalDateTime.now());
        broker.setRegulatoryStatus(RegulatoryStatus.VERIFIED);
        broker.setOwner(user);
        return broker;
    }

}
