package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.domains.Acao;
import com.trabalho.gestao_acoes.domains.Corretora;
import com.trabalho.gestao_acoes.domains.PosicaoCarteira;
import com.trabalho.gestao_acoes.repositories.AcaoRepository;
import com.trabalho.gestao_acoes.repositories.CorretoraRepository;
import com.trabalho.gestao_acoes.repositories.PosicaoCarteiraRepository;
import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.NotFoundException;
import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CarteiraServiceTest {
    private final PosicaoCarteiraRepository positions = mock(PosicaoCarteiraRepository.class);
    private final AcaoRepository assets = mock(AcaoRepository.class);
    private final CorretoraRepository brokers = mock(CorretoraRepository.class);
    private final CotacaoService quotes = mock(CotacaoService.class);
    private final CarteiraTransactionService transactions = mock(CarteiraTransactionService.class);
    private CarteiraService service;

    @BeforeEach
    void setUp() { service = new CarteiraService(positions, assets, brokers, quotes, transactions); }

    @Test
    void rejectsInvalidBusinessInputBeforeRepositoriesOrExternalQuotes() {
        assertThatThrownBy(() -> service.comprar("PETR4", "BRASIL", 0, 1L))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(assets, brokers, quotes, transactions);
    }

    @Test
    void rejectsMissingReferencesAndMarketMismatchBeforeExternalQuote() {
        when(assets.findByTicker("PETR4")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.comprar("PETR4", "BRASIL", 1, 1L))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(quotes);

        reset(assets, brokers);
        Acao asset = asset("AAPL", "BRASIL", "BRL");
        when(assets.findByTicker("AAPL")).thenReturn(Optional.of(asset));
        when(brokers.existsById(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.comprar("AAPL", "AMERICANO", 1, 1L))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(quotes);
    }

    @Test
    void resolvesBothCanonicalMarketsAndQuotesBeforeDelegatingTheTransaction() {
        Acao asset = asset("AAPL", "AMERICANO", "USD");
        when(assets.findByTicker("AAPL")).thenReturn(Optional.of(asset));
        when(brokers.existsById(7L)).thenReturn(true);
        when(quotes.buscar("AAPL", "AMERICANO"))
                .thenReturn(new CotacaoBolsa(new BigDecimal("100.00000000"), "USD"));

        service.comprar(" aapl ", "internacional", 2, 7L);

        var order = inOrder(quotes, transactions);
        order.verify(quotes).buscar("AAPL", "AMERICANO");
        order.verify(transactions).comprar(10L, 7L, 2, new BigDecimal("100.00000000"));
    }

    @Test
    void calculatesTheDocumentedMixedMarketPortfolioAsExactNumericJsonValue() {
        Acao petr4 = asset("PETR4", "BRASIL", "BRL");
        Acao aapl = asset("AAPL", "AMERICANO", "USD");
        Corretora broker = new Corretora(); broker.setRazaoSocial("Teste");
        when(positions.findAll()).thenReturn(List.of(
                new PosicaoCarteira(1L, 6, new BigDecimal("20"), petr4, broker),
                new PosicaoCarteira(2L, 2, new BigDecimal("100"), aapl, broker)));
        when(quotes.buscar("PETR4", "BRASIL")).thenReturn(new CotacaoBolsa(new BigDecimal("20"), "BRL"));
        when(quotes.buscar("AAPL", "AMERICANO")).thenReturn(new CotacaoBolsa(new BigDecimal("100"), "USD"));

        assertThat(service.calcularSaldoTotal()).isEqualByComparingTo("1180.00");
    }

    private static Acao asset(String ticker, String market, String currency) {
        return new Acao(10L, ticker, ticker, market, currency, BigDecimal.ONE, LocalDateTime.now());
    }
}
