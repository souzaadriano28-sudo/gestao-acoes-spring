package com.trabalho.gestao_acoes.domains.dtos.broker;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trabalho.gestao_acoes.domains.enums.RegulatoryStatus;
import java.time.Instant;

public record RegulatoryEvidenceDTO(RegulatoryStatus status, String category, String source,
        String evidenceId, @JsonFormat(shape = JsonFormat.Shape.STRING) Instant referenceAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant checkedAt, String reason) {
    public RegulatoryEvidenceDTO {
        if (status == null) throw new IllegalArgumentException("regulatory status is required");
        if (status == RegulatoryStatus.VERIFIED || status == RegulatoryStatus.STALE) {
            if (blank(category) || blank(source) || blank(evidenceId) || referenceAt == null || checkedAt == null) {
                throw new IllegalArgumentException("verified/stale regulatory evidence requires complete provenance");
            }
            if (referenceAt.isAfter(checkedAt)) throw new IllegalArgumentException("regulatory reference cannot be after check");
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
