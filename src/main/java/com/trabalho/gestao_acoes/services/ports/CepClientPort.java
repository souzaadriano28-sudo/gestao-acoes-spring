package com.trabalho.gestao_acoes.services.ports;

import com.trabalho.gestao_acoes.integrations.viacep.ViaCepResponse;

public interface CepClientPort {
    ViaCepResponse buscarEnderecoPorCep(String cep);
}