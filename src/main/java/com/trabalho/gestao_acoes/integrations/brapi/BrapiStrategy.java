package com.trabalho.gestao_acoes.integrations.brapi;

import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import com.trabalho.gestao_acoes.services.ports.CotacaoStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrapiStrategy implements CotacaoStrategy {

    @Autowired
    private BrapiClient brapiClient;

    // Coloque um token gratuito gerado no site brapi.dev (ou deixe este de teste acadêmico se funcionar)
    private final String TOKEN = "rLFSeWPu1T9XtEV4E2XbX3";

    @Override
    public CotacaoBolsa buscarCotacao(String ticker) {
        BrapiResponse response = brapiClient.consultarCotacao(ticker, TOKEN);

        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            Double preco = response.getResults().get(0).getRegularMarketPrice();
            String moeda = response.getResults().get(0).getCurrency();
            return new CotacaoBolsa(preco, moeda != null ? moeda : "BRL");
        }
        throw new RuntimeException("Cotação não encontrada na bolsa brasileira para o ticker: " + ticker);
    }

    @Override
    public boolean suportaMercado(String mercado) {
        // Esta estratégia SÓ liga se o usuário digitar "NACIONAL" ou "BRASILEIRO"
        return mercado != null && (mercado.equalsIgnoreCase("NACIONAL") || mercado.equalsIgnoreCase("BRASIL"));
    }
}