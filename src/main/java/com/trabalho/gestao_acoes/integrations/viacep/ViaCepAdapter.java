package com.trabalho.gestao_acoes.integrations.viacep;

import com.trabalho.gestao_acoes.services.ports.CepClientPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.cache.annotation.Cacheable;

@Component
public class ViaCepAdapter implements CepClientPort {

    @Autowired
    private ViaCepClient viaCepClient;

    @Override
    @Cacheable(value = "ceps", key = "#cep")
    public ViaCepResponse buscarEnderecoPorCep(String cep) {
        String cepLimpo = cep.replace("-", "").replace(".", "");

        return viaCepClient.consultarCep(cepLimpo);
    }

}