package com.trabalho.gestao_acoes.domains.dtos;

import java.math.BigDecimal;
public record OperationPreviewDTO(Integer quantidadeAnterior, BigDecimal precoMedioAnterior, BigDecimal custoAnterior,
        BigDecimal valorBruto, BigDecimal custos, BigDecimal valorTotal, Integer quantidadeProjetada,
        BigDecimal precoMedioProjetado, BigDecimal custoProjetado, BigDecimal resultadoRealizadoProjetado, String moeda) {}
