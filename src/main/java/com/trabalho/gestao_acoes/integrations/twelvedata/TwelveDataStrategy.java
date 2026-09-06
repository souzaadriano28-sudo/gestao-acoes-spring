package com.trabalho.gestao_acoes.integrations.twelvedata;

import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import com.trabalho.gestao_acoes.services.ports.CotacaoStrategy;
import com.trabalho.gestao_acoes.services.exceptions.InvalidQuoteException;
import feign.codec.DecodeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TwelveDataStrategy implements CotacaoStrategy {

    private final TwelveDataClient twelveDataClient;
    private final String apiKey;

    public TwelveDataStrategy(TwelveDataClient twelveDataClient, @Value("${api.twelvedata.key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Configuração obrigatória ausente: api.twelvedata.key");
        this.twelveDataClient = twelveDataClient;
        this.apiKey = apiKey;
    }

    @Override
    public CotacaoBolsa buscarCotacao(String ticker) {
        TwelveDataResponse response;
        try {
            response = twelveDataClient.consultarCotacao(ticker, apiKey);
        } catch (DecodeException ex) {
            throw new InvalidQuoteException("Resposta de cotação americana inválida.");
        }

        if (response != null && response.getPrice() != null) {
            return new CotacaoBolsa(response.getPrice(), "USD", "MARKET_DATA_PROVIDER", "TWELVE_DATA", null, null, null);
        }
        throw new InvalidQuoteException("Resposta de cotação americana ausente ou vazia.");
    }

    @Override
    public boolean suportaMercado(String mercado) {
        return "AMERICANO".equals(mercado);
    }
}
