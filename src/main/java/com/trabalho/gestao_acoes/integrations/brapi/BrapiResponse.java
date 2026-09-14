package com.trabalho.gestao_acoes.integrations.brapi;

import java.util.List;
import java.math.BigDecimal;

public class BrapiResponse {
    private List<Result> results;

    public List<Result> getResults() { return results; }
    public void setResults(List<Result> results) { this.results = results; }

    public static class Result {
        private BigDecimal regularMarketPrice;
        private String currency;
        private String longName;
        private String shortName;

        public BigDecimal getRegularMarketPrice() { return regularMarketPrice; }
        public void setRegularMarketPrice(BigDecimal regularMarketPrice) { this.regularMarketPrice = regularMarketPrice; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getLongName() { return longName; }
        public void setLongName(String longName) { this.longName = longName; }
        public String getShortName() { return shortName; }
        public void setShortName(String shortName) { this.shortName = shortName; }
    }
}
