package com.trabalho.gestao_acoes.domains.dtos.broker;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public record BrokerCnpjPreviewDTO(String cnpj, String razaoSocial, String nomeFantasia,
        String situacaoEmpresarial, String cepSugerido, boolean cnaeCompativel,
        boolean autorizadaPelaCvm, String situacaoCvm, String categoriaCvm,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant consultadaEm, String mensagemCvm) {}
