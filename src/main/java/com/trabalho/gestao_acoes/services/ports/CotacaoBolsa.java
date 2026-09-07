package com.trabalho.gestao_acoes.services.ports;

import java.math.BigDecimal;
import java.time.Instant;

public class CotacaoBolsa {

    private BigDecimal precoAtual;
    private String moeda; // "BRL" ou "USD"
    private String sourceType;
    private String provider;
    private Instant referenceAt;
    private Instant fetchedAt;
    private String referenceKind;

    public CotacaoBolsa() {
    }

    public CotacaoBolsa(BigDecimal precoAtual, String moeda) {
        this.precoAtual = precoAtual;
        this.moeda = moeda;
    }

    public CotacaoBolsa(BigDecimal precoAtual, String moeda, String sourceType, String provider,
                        Instant referenceAt, Instant fetchedAt, String referenceKind) {
        this.precoAtual = precoAtual;
        this.moeda = moeda;
        this.sourceType = sourceType;
        this.provider = provider;
        this.referenceAt = referenceAt;
        this.fetchedAt = fetchedAt;
        this.referenceKind = referenceKind;
    }

    public BigDecimal getPrecoAtual() { return precoAtual; }
    public void setPrecoAtual(BigDecimal precoAtual) { this.precoAtual = precoAtual; }

    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }
    public String getSourceType() { return sourceType; }
    public String getProvider() { return provider; }
    public Instant getReferenceAt() { return referenceAt; }
    public Instant getFetchedAt() { return fetchedAt; }
    public String getReferenceKind() { return referenceKind; }
}
