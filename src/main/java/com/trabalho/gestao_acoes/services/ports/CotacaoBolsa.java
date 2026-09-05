package com.trabalho.gestao_acoes.services.ports;

import java.math.BigDecimal;

public class CotacaoBolsa {

    private BigDecimal precoAtual;
    private String moeda; // "BRL" ou "USD"

    public CotacaoBolsa() {
    }

    public CotacaoBolsa(BigDecimal precoAtual, String moeda) {
        this.precoAtual = precoAtual;
        this.moeda = moeda;
    }

    public BigDecimal getPrecoAtual() { return precoAtual; }
    public void setPrecoAtual(BigDecimal precoAtual) { this.precoAtual = precoAtual; }

    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }
}
