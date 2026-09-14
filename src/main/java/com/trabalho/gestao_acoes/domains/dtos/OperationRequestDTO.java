package com.trabalho.gestao_acoes.domains.dtos;

import com.trabalho.gestao_acoes.domains.enums.TipoTransacao;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OperationRequestDTO(
        @NotNull TipoTransacao tipo, @NotNull @Positive Long ativoId, @NotNull @Positive Long corretoraId,
        @NotNull LocalDateTime dataHora, @NotNull @Positive Integer quantidade,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String moeda, @NotNull @DecimalMin(value = "0.00000001") BigDecimal precoUnitario,
        @DecimalMin("0") BigDecimal corretagem, @DecimalMin("0") BigDecimal taxas,
        @DecimalMin("0") BigDecimal impostos, @DecimalMin("0") BigDecimal outrosCustos,
        @Size(max = 2000) String observacao, @Size(max = 100) String idempotencyKey) {}
