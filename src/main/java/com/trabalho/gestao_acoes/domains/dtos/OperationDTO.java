package com.trabalho.gestao_acoes.domains.dtos;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record OperationDTO(Long id, String tipo, Long ativoId, String ticker, Long corretoraId, String corretora,
        Integer quantidade, String moeda, BigDecimal precoUnitario, BigDecimal corretagem, BigDecimal taxas,
        BigDecimal impostos, BigDecimal outrosCustos, BigDecimal valorBruto, BigDecimal valorTotal,
        BigDecimal resultadoRealizado, LocalDateTime dataHora, String observacao) {}
