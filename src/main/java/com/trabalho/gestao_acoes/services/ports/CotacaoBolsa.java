package com.trabalho.gestao_acoes.services.ports;

public class CotacaoBolsa {

    private Double precoAtual;
    private String moeda; // "BRL" ou "USD"

    public CotacaoBolsa() {
    }

    public CotacaoBolsa(Double precoAtual, String moeda) {
        this.precoAtual = precoAtual;
        this.moeda = moeda;
    }

    public Double getPrecoAtual() { return precoAtual; }
    public void setPrecoAtual(Double precoAtual) { this.precoAtual = precoAtual; }

    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }
}