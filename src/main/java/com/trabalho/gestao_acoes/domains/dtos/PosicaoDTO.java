package com.trabalho.gestao_acoes.domains.dtos;

import java.io.Serializable;
import java.math.BigDecimal;

public class PosicaoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ticker;
    private String corretora;
    private Integer quantidade;
    private BigDecimal precoMedio;
    private String moeda; // <--- NOVO CAMPO

    public PosicaoDTO() {}

    public PosicaoDTO(String ticker, String corretora, Integer quantidade, BigDecimal precoMedio, String moeda) {
        this.ticker = ticker;
        this.corretora = corretora;
        this.quantidade = quantidade;
        this.precoMedio = precoMedio;
        this.moeda = moeda;
    }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getCorretora() { return corretora; }
    public void setCorretora(String corretora) { this.corretora = corretora; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoMedio() { return precoMedio; }
    public void setPrecoMedio(BigDecimal precoMedio) { this.precoMedio = precoMedio; }

    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }
}
