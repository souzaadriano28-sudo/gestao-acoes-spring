package com.trabalho.gestao_acoes.domains.dtos.portfolio;

import com.trabalho.gestao_acoes.domains.enums.Availability;
import java.math.BigDecimal;

public record PercentageMetricDTO(Availability availability, BigDecimal value, String reason) {
    public static PercentageMetricDTO available(BigDecimal value) { return new PercentageMetricDTO(Availability.AVAILABLE, value, null); }
    public static PercentageMetricDTO unavailable(String reason) { return new PercentageMetricDTO(Availability.UNAVAILABLE, null, reason); }
}
