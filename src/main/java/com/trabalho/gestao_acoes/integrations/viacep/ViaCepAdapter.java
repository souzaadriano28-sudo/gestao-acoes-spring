package com.trabalho.gestao_acoes.integrations.viacep;

import com.trabalho.gestao_acoes.services.ports.CepClientPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ViaCepAdapter implements CepClientPort {

    @Autowired
    private ViaCepClient viaCepClient;

    @Override
    public ViaCepResponse buscarEnderecoPorCep(String cep) {
        String cepLimpo = cep.replace("-", "").replace(".", "");

        return viaCepClient.consultarCep(cepLimpo);
    }
}