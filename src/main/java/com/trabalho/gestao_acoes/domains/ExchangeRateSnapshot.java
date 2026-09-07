package com.trabalho.gestao_acoes.domains;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "exchange_rate_snapshot", uniqueConstraints =
        @UniqueConstraint(name = "uk_exchange_rate_pair", columnNames = {"base_currency", "quote_currency"}))
public class ExchangeRateSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 3) private String baseCurrency;
    @Column(nullable = false, length = 3) private String quoteCurrency;
    @Column(nullable = false, precision = 19, scale = 8) private BigDecimal rate;
    @Column(nullable = false, length = 40) private String sourceType;
    @Column(nullable = false, length = 80) private String provider;
    @Column(nullable = false) private Instant referenceAt;
    @Column(nullable = false) private Instant fetchedAt;
    @Column(nullable = false, length = 60) private String referenceKind;

    public Long getId() { return id; }
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String value) { this.baseCurrency = value; }
    public String getQuoteCurrency() { return quoteCurrency; }
    public void setQuoteCurrency(String value) { this.quoteCurrency = value; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal value) { this.rate = value; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String value) { this.sourceType = value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { this.provider = value; }
    public Instant getReferenceAt() { return referenceAt; }
    public void setReferenceAt(Instant value) { this.referenceAt = value; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant value) { this.fetchedAt = value; }
    public String getReferenceKind() { return referenceKind; }
    public void setReferenceKind(String value) { this.referenceKind = value; }
}
