package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.services.exceptions.InvalidQuoteException;
import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import com.trabalho.gestao_acoes.services.ports.CotacaoStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CotacaoServiceTest {
    @Test
    void supportsFixedBrlAndUsdFixturesWithoutNetworkOrCredentials() {
        CotacaoService service = new CotacaoService(List.of(
                fixture("BRASIL", quote("20", "brl")),
                fixture("AMERICANO", quote("100", "USD"))));

        assertThat(service.buscar("PETR4", "BRASIL").getPrecoAtual()).isEqualByComparingTo("20.00000000");
        assertThat(service.buscar("AAPL", "AMERICANO").getMoeda()).isEqualTo("USD");
    }

    @Test
    void rejectsMissingQuotePriceCurrencyAndCurrencyMismatch() {
        assertInvalid(null, "BRASIL");
        assertInvalid(new CotacaoBolsa(null, "BRL"), "BRASIL");
        assertInvalid(new CotacaoBolsa(BigDecimal.ZERO, "BRL"), "BRASIL");
        assertInvalid(new CotacaoBolsa(BigDecimal.ONE, null), "BRASIL");
        assertInvalid(new CotacaoBolsa(BigDecimal.ONE, "USD"), "BRASIL");
        assertInvalid(new CotacaoBolsa(BigDecimal.ONE, "BRL"), "BRASIL");
    }

    private static void assertInvalid(CotacaoBolsa quote, String market) {
        CotacaoService service = new CotacaoService(List.of(fixture(market, quote)));
        assertThatThrownBy(() -> service.buscar("PETR4", market)).isInstanceOf(InvalidQuoteException.class);
    }

    private static CotacaoStrategy fixture(String market, CotacaoBolsa quote) {
        return new CotacaoStrategy() {
            public CotacaoBolsa buscarCotacao(String ticker) { return quote; }
            public boolean suportaMercado(String candidate) { return market.equals(candidate); }
        };
    }

    private static CotacaoBolsa quote(String value, String currency) {
        return new CotacaoBolsa(new BigDecimal(value), currency, "TEST_FIXTURE", "TEST_PROVIDER",
                null, null, null);
    }
}
