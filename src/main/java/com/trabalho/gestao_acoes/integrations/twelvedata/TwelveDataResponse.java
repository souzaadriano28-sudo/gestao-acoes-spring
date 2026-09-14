package com.trabalho.gestao_acoes.integrations.twelvedata;

import java.math.BigDecimal;

public class TwelveDataResponse {
    private BigDecimal price;
    private BigDecimal close;
    private String name;
    private String currency;

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
