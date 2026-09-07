package com.trabalho.gestao_acoes.services;

import com.trabalho.gestao_acoes.services.exceptions.BusinessException;
import com.trabalho.gestao_acoes.services.exceptions.InvalidQuoteException;
import com.trabalho.gestao_acoes.services.ports.CotacaoBolsa;
import com.trabalho.gestao_acoes.services.ports.CotacaoStrategy;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class CotacaoService {
    private final List<CotacaoStrategy> strategies;
    private final Clock clock;

    public CotacaoService(List<CotacaoStrategy> strategies) { this(strategies, Clock.systemUTC()); }

    @Autowired
    public CotacaoService(List<CotacaoStrategy> strategies, Clock clock) {
        this.strategies = strategies;
        this.clock = clock;
    }

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
        Instant fetchedAt = quote.getFetchedAt() == null ? clock.instant() : quote.getFetchedAt();
        Instant referenceAt = quote.getReferenceAt() == null ? fetchedAt : quote.getReferenceAt();
        String referenceKind = quote.getReferenceAt() == null ? "FETCH_TIME_PROXY" : quote.getReferenceKind();
        return new CotacaoBolsa(MoneyPolicy.quote(quote.getPrecoAtual()), currency,
                required(quote.getSourceType(), "tipo da fonte"),
                required(quote.getProvider(), "provedor"),
                referenceAt, fetchedAt, required(referenceKind, "semântica do instante de referência"));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidQuoteException("Cotação sem " + field + " verificável.");
        }
        return value;
    }
}
