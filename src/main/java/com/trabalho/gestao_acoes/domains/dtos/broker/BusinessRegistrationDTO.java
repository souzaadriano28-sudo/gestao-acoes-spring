package com.trabalho.gestao_acoes.domains.dtos.broker;

public record BusinessRegistrationDTO(String source, String status, String reason) {
    public BusinessRegistrationDTO {
        if (source == null || source.isBlank()) throw new IllegalArgumentException("business registration source is required");
        if ((status == null || status.isBlank()) && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("business registration status or reason is required");
        }
    }
}
