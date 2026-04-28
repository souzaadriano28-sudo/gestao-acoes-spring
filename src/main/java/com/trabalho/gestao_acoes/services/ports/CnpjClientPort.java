package com.trabalho.gestao_acoes.services.ports;

import com.trabalho.gestao_acoes.integrations.brasilapi.BrasilApiResponse;

public interface CnpjClientPort {
    BrasilApiResponse buscarDadosPorCnpj(String cnpj);
}