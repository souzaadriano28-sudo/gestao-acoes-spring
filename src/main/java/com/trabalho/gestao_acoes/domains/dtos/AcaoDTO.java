package com.trabalho.gestao_acoes.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AcaoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Null(message = "O ID não deve ser informado no cadastro")
    private Long id;

    @NotBlank(message = "O Ticker é obrigatório")
    private String ticker;

    private String nomeEmpresa;

    @NotBlank(message = "O mercado (NACIONAL/INTERNACIONAL) é obrigatório")
    private String mercado;

    private String moeda;
    private BigDecimal cotacaoAtual;
    private LocalDateTime dataHoraCotacao;

    public AcaoDTO() {
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

    public BigDecimal getCotacaoAtual() { return cotacaoAtual; }
    public void setCotacaoAtual(BigDecimal cotacaoAtual) { this.cotacaoAtual = cotacaoAtual; }

    public LocalDateTime getDataHoraCotacao() { return dataHoraCotacao; }
    public void setDataHoraCotacao(LocalDateTime dataHoraCotacao) { this.dataHoraCotacao = dataHoraCotacao; }
}
