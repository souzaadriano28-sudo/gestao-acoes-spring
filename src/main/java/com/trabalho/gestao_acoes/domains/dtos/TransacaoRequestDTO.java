package com.trabalho.gestao_acoes.domains.dtos;

public class TransacaoRequestDTO {

    private String ticker;
    private String mercado;
    private Integer qtd;
    private Long corretoraId;

    public TransacaoRequestDTO() {
    }

    public TransacaoRequestDTO(String ticker, String mercado, Integer qtd, Long corretoraId) {
        this.ticker = ticker;
        this.mercado = mercado;
        this.qtd = qtd;
        this.corretoraId = corretoraId;
    }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getMercado() { return mercado; }
    public void setMercado(String mercado) { this.mercado = mercado; }

    public Integer getQtd() { return qtd; }
    public void setQtd(Integer qtd) { this.qtd = qtd; }

    public Long getCorretoraId() { return corretoraId; }
    public void setCorretoraId(Long corretoraId) { this.corretoraId = corretoraId; }
}