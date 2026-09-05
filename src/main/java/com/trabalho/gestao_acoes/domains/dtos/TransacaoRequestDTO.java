package com.trabalho.gestao_acoes.domains.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.trabalho.gestao_acoes.config.StrictIntegerDeserializer;
import com.trabalho.gestao_acoes.config.StrictLongDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class TransacaoRequestDTO {

    @NotBlank(message = "O ticker é obrigatório")
    private String ticker;

    @NotBlank(message = "O mercado é obrigatório")
    private String mercado;

    @NotNull(message = "A quantidade é obrigatória")
    @Positive(message = "A quantidade deve ser positiva")
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer qtd;

    @NotNull(message = "A corretora é obrigatória")
    @Positive(message = "A corretora deve ter identificador positivo")
    @JsonDeserialize(using = StrictLongDeserializer.class)
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
