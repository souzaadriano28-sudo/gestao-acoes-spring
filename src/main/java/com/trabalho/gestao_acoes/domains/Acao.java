package com.trabalho.gestao_acoes.domains;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "acao")
public class Acao implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String ticker;

    private String nomeEmpresa;

    @Column(nullable = false, length = 50)
    private String mercado; // Ex: "BRASILEIRO" ou "AMERICANO"

    @Column(nullable = false, length = 10)
    private String moeda; // Ex: "BRL" ou "USD"

    @Column(nullable = false)
    private Double cotacaoAtual;

    @Column(nullable = false)
    private LocalDateTime dataHoraCotacao;

    public Acao() {
    }

    public Acao(Long id, String ticker, String nomeEmpresa, String mercado, String moeda, Double cotacaoAtual, LocalDateTime dataHoraCotacao) {
        this.id = id;
        this.ticker = ticker;
        this.nomeEmpresa = nomeEmpresa;
        this.mercado = mercado;
        this.moeda = moeda;
        this.cotacaoAtual = cotacaoAtual;
        this.dataHoraCotacao = dataHoraCotacao;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getNomeEmpresa() { return nomeEmpresa; }
    public void setNomeEmpresa(String nomeEmpresa) { this.nomeEmpresa = nomeEmpresa; }

    public String getMercado() { return mercado; }
    public void setMercado(String mercado) { this.mercado = mercado; }

    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }

    public Double getCotacaoAtual() { return cotacaoAtual; }
    public void setCotacaoAtual(Double cotacaoAtual) { this.cotacaoAtual = cotacaoAtual; }

    public LocalDateTime getDataHoraCotacao() { return dataHoraCotacao; }
    public void setDataHoraCotacao(LocalDateTime dataHoraCotacao) { this.dataHoraCotacao = dataHoraCotacao; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Acao acao = (Acao) o;
        return Objects.equals(id, acao.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}