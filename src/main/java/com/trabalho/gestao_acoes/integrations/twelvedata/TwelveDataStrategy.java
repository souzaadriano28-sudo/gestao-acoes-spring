package com.trabalho.gestao_acoes.integrations.twelvedata;

import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import com.trabalho.gestao_acoes.services.ports.CotacaoStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TwelveDataStrategy implements CotacaoStrategy {

    @Autowired
    private TwelveDataClient twelveDataClient;

    // Você precisará de uma API Key gratuita do site da Twelve Data.
    // Para testes acadêmicos rápidos, às vezes a palavra "demo" funciona para as ações da Apple/IBM.
    private final String API_KEY = "bf4fe2d0ddb84d02a30e8ce871bac170";

    @Override
    public CotacaoBolsa buscarCotacao(String ticker) {
        TwelveDataResponse response = twelveDataClient.consultarCotacao(ticker, API_KEY);

        if (response != null && response.getPrice() != null) {
            // A Twelve Data retorna sempre em dólar para ativos americanos
            return new CotacaoBolsa(response.getPrice(), "USD");
        }
        throw new RuntimeException("Cotação não encontrada na bolsa americana para o ticker: " + ticker);
    }

    @Override
    public boolean suportaMercado(String mercado) {
        // Esta estratégia SÓ liga se o usuário digitar "INTERNACIONAL" ou "AMERICANO"
        return mercado != null && (mercado.equalsIgnoreCase("INTERNACIONAL") || mercado.equalsIgnoreCase("AMERICANO"));
    }
}