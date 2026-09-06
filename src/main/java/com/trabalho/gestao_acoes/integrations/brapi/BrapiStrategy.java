package com.trabalho.gestao_acoes.integrations.brapi;

import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import com.trabalho.gestao_acoes.services.ports.CotacaoStrategy;
import com.trabalho.gestao_acoes.services.exceptions.InvalidQuoteException;
import feign.codec.DecodeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BrapiStrategy implements CotacaoStrategy {

    private final BrapiClient brapiClient;
    private final String token;

    public BrapiStrategy(BrapiClient brapiClient, @Value("${api.brapi.token}") String token) {
        if (token == null || token.isBlank()) throw new IllegalStateException("Configuração obrigatória ausente: api.brapi.token");
        this.brapiClient = brapiClient;
        this.token = token;
    }

    @Override
    public CotacaoBolsa buscarCotacao(String ticker) {
        BrapiResponse response;
        try {
            response = brapiClient.consultarCotacao(ticker, token);
        } catch (DecodeException ex) {
            throw new InvalidQuoteException("Resposta de cotação brasileira inválida.");
        }

        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            java.math.BigDecimal preco = response.getResults().get(0).getRegularMarketPrice();
            String moeda = response.getResults().get(0).getCurrency();
            return new CotacaoBolsa(preco, moeda, "MARKET_DATA_PROVIDER", "BRAPI", null, null, null);
        }
        throw new InvalidQuoteException("Resposta de cotação brasileira ausente ou vazia.");
    }

    @Override
    public boolean suportaMercado(String mercado) {
        return "BRASIL".equals(mercado);
    }
}
