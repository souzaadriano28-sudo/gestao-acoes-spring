package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.InvalidQuoteException;
import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import com.trabalho.gestao_acoes.services.ports.CotacaoStrategy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CotacaoService {
    private final List<CotacaoStrategy> strategies;

    public CotacaoService(List<CotacaoStrategy> strategies) { this.strategies = strategies; }

    public CotacaoBolsa buscar(String ticker, String mercado) {
        CotacaoStrategy strategy = strategies.stream().filter(s -> s.suportaMercado(mercado)).findFirst()
                .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "Mercado não suportado."));
        CotacaoBolsa quote = strategy.buscarCotacao(ticker);
        if (quote == null || quote.getMoeda() == null || quote.getMoeda().trim().isEmpty()) {
            throw new InvalidQuoteException("Cotação ou moeda ausente.");
        }
        String currency = quote.getMoeda().trim().toUpperCase(Locale.ROOT);
        String expected = "BRASIL".equals(mercado) ? "BRL" : "USD";
        if (!expected.equals(currency)) throw new InvalidQuoteException("Moeda incompatível com o mercado do ativo.");
        return new CotacaoBolsa(MoneyPolicy.quote(quote.getPrecoAtual()), currency);
    }
}
