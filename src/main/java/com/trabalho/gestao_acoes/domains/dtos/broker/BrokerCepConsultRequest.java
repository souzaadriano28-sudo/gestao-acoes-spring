package com.trabalho.gestao_acoes.domains.dtos.broker;

import jakarta.validation.constraints.NotBlank;

public record BrokerCepConsultRequest(@NotBlank(message = "O CEP é obrigatório") String cep) {}
