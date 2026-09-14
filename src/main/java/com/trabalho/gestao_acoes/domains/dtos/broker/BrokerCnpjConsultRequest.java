package com.trabalho.gestao_acoes.domains.dtos.broker;

import jakarta.validation.constraints.NotBlank;

public record BrokerCnpjConsultRequest(@NotBlank(message = "O CNPJ é obrigatório") String cnpj) {}
