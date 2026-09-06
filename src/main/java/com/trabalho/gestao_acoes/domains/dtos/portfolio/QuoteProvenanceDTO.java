package com.trabalho.gestao_acoes.domains.dtos.portfolio;

import com.trabalho.gestao_acoes.domains.enums.Availability;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonFormat;

public record QuoteProvenanceDTO(Availability availability, String sourceType, String provider,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant referenceAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant fetchedAt,
        String referenceKind, String currency, String reason) {}
