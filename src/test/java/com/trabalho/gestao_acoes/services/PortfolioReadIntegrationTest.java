package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.*;
import com.trabalho.gestao_acoes.domains.enums.*;
import com.trabalho.gestao_acoes.repositories.*;
import com.trabalho.gestao_acoes.services.ports.*;
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
    @Autowired private AcaoRepository assets;
    @Autowired private CorretoraRepository brokers;
    @Autowired private PosicaoCarteiraRepository positions;
    @Autowired private TransacaoRepository transactions;
    @MockitoBean private ExchangeRatePort exchangeRates;

    @BeforeEach void clean() {
        transactions.deleteAll();
        positions.deleteAll();
        assets.deleteAll();
        brokers.deleteAll();
        reset(exchangeRates);
    }

    @Test void emptyPortfolioIsAConfirmedSuccessfulRead() {
        var dashboard = service.dashboard();
        assertThat(dashboard.positionCount()).isZero();
        assertThat(dashboard.patrimony().availability()).isEqualTo(Availability.AVAILABLE);
        assertThat(dashboard.patrimony().value()).isEqualByComparingTo("0.00");
    }

    @Test void persistedPositionAndMovementsProduceEnrichedDeterministicReads() {
        Instant now = Instant.now();
        Corretora broker = brokers.save(broker("11222333000181", "Corretora Um"));
        Acao asset = assets.save(asset("PETR4", "BRASIL", "BRL", "22.50", now.minusSeconds(60), true));
        positions.save(new PosicaoCarteira(null, 4, new BigDecimal("20.00"), asset, broker));
        transactions.save(new Transacao(null, TipoTransacao.COMPRA, 2, new BigDecimal("19.00"), LocalDateTime.now().minusDays(1), asset, broker));
        Transacao newest = transactions.save(new Transacao(null, TipoTransacao.COMPRA, 2, new BigDecimal("21.00"), LocalDateTime.now(), asset, broker));

        var dashboard = service.dashboard();
        var movements = service.movements(0, 10, "COMPRA", "PETR4", broker.getId(), null, null);

        assertThat(dashboard.patrimony().value()).isEqualByComparingTo("90.00");
        assertThat(dashboard.cost().value()).isEqualByComparingTo("80.00");
        assertThat(dashboard.unrealizedResult().value()).isEqualByComparingTo("10.00");
        assertThat(dashboard.positions().get(0).positionId()).isNotNull();
        assertThat(movements.items()).hasSize(2);
        assertThat(movements.items().get(0).id()).isEqualTo(newest.getId());
        assertThat(movements.items().get(0).recordedAt().getOffset()).isNotNull();
        assertThat(movements.items().get(0).historicalQuoteProvenance().availability()).isEqualTo(Availability.UNAVAILABLE);
    }

    @Test void missingQuoteProvenanceReturnsUsefulPersistedDataAndExplicitPartialFailure() {
        Instant now = Instant.now();
        Corretora broker = brokers.save(broker("22333444000181", "Corretora Dois"));
        Acao asset = assets.save(asset("VALE3", "BRASIL", "BRL", "60.00", now, false));
        positions.save(new PosicaoCarteira(null, 3, new BigDecimal("55.00"), asset, broker));

        var dashboard = service.dashboard();
        assertThat(dashboard.positions().get(0).quantity()).isEqualTo(3);
        assertThat(dashboard.positions().get(0).cost().value()).isEqualByComparingTo("165.00");
        assertThat(dashboard.positions().get(0).marketValue().availability()).isEqualTo(Availability.UNAVAILABLE);
        assertThat(dashboard.patrimony().availability()).isEqualTo(Availability.UNAVAILABLE);
    }

    @Test void missingExchangeNeverReturnsAPartialBrlTotalForUsdPortfolio() {
        Instant now = Instant.now();
        Corretora broker = brokers.save(broker("33444555000181", "Corretora Três"));
        Acao asset = assets.save(asset("AAPL", "AMERICANO", "USD", "100.00", now.minusSeconds(60), true));
        positions.save(new PosicaoCarteira(null, 2, new BigDecimal("90.00"), asset, broker));
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
        positions.save(new PosicaoCarteira(null, 1, new BigDecimal("30.00"), asset, first));
        positions.save(new PosicaoCarteira(null, 2, new BigDecimal("31.00"), asset, second));

        var page = service.detailedPositions(0, 20, "BRASIL", null);
        assertThat(page.items()).hasSize(2).extracting(item -> item.brokerName()).containsExactly("Alfa", "Beta");
        assertThat(page.items()).extracting(item -> item.ticker()).containsOnly("ITUB4");
    }

    private static Acao asset(String ticker, String market, String currency, String quote, Instant reference, boolean provenance) {
        Acao asset = new Acao(null, ticker, ticker, market, currency, new BigDecimal(quote), LocalDateTime.now());
        if (provenance) {
            asset.setQuoteSourceType("MARKET_DATA_PROVIDER"); asset.setQuoteProvider("INTEGRATION_FIXTURE");
            asset.setQuoteReferenceAt(reference); asset.setQuoteFetchedAt(reference); asset.setQuoteReferenceKind("PROVIDER_REFERENCE_TIME");
        }
        return asset;
    }

    private static Corretora broker(String cnpj, String name) {
        Corretora broker = new Corretora();
        broker.setCnpj(cnpj); broker.setRazaoSocial(name); broker.setCep("01001000");
        broker.setValidadaNaCvm(false); broker.setDataCadastro(LocalDateTime.now());
        return broker;
    }
}
