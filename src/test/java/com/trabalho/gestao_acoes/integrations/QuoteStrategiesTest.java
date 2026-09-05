package com.trabalho.gestao_acoes.integrations;

import com.trabalho.gestao_acoes.integrations.brapi.BrapiClient;
import com.trabalho.gestao_acoes.integrations.brapi.BrapiResponse;
import com.trabalho.gestao_acoes.integrations.brapi.BrapiStrategy;
import com.trabalho.gestao_acoes.integrations.twelvedata.TwelveDataClient;
import com.trabalho.gestao_acoes.integrations.twelvedata.TwelveDataResponse;
import com.trabalho.gestao_acoes.integrations.twelvedata.TwelveDataStrategy;
import com.trabalho.gestao_acoes.services.exceptions.InvalidQuoteException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class QuoteStrategiesTest {
    @Test
    void refusesBlankCredentialsAtStartup() {
        assertThatThrownBy(() -> new BrapiStrategy(mock(BrapiClient.class), " ")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new TwelveDataStrategy(mock(TwelveDataClient.class), "" )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void adaptsFixedBrazilianAndAmericanResponsesWithoutFallbacks() {
        BrapiClient brapi = mock(BrapiClient.class);
        BrapiResponse brapiResponse = new BrapiResponse();
        BrapiResponse.Result item = new BrapiResponse.Result();
        item.setRegularMarketPrice(new BigDecimal("20")); item.setCurrency("BRL");
        brapiResponse.setResults(List.of(item));
        when(brapi.consultarCotacao("PETR4", "fake-token")).thenReturn(brapiResponse);
        assertThat(new BrapiStrategy(brapi, "fake-token").buscarCotacao("PETR4").getMoeda()).isEqualTo("BRL");

        TwelveDataClient twelve = mock(TwelveDataClient.class);
        TwelveDataResponse twelveResponse = new TwelveDataResponse(); twelveResponse.setPrice(new BigDecimal("100"));
        when(twelve.consultarCotacao("AAPL", "fake-key")).thenReturn(twelveResponse);
        assertThat(new TwelveDataStrategy(twelve, "fake-key").buscarCotacao("AAPL").getPrecoAtual())
                .isEqualByComparingTo("100");
    }

    @Test
    void rejectsNullAndEmptyProviderPayloads() {
        BrapiClient brapi = mock(BrapiClient.class);
        when(brapi.consultarCotacao(any(), any())).thenReturn(new BrapiResponse());
        assertThatThrownBy(() -> new BrapiStrategy(brapi, "fake").buscarCotacao("PETR4"))
                .isInstanceOf(InvalidQuoteException.class);
        TwelveDataClient twelve = mock(TwelveDataClient.class);
        when(twelve.consultarCotacao(any(), any())).thenReturn(null);
        assertThatThrownBy(() -> new TwelveDataStrategy(twelve, "fake").buscarCotacao("AAPL"))
                .isInstanceOf(InvalidQuoteException.class);
    }
}
