package com.trabalho.gestao_acoes.integrations.brasilapi;

import com.trabalho.gestao_acoes.services.ports.CnpjClientPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrasilApiAdapter implements CnpjClientPort {

    @Autowired
    private BrasilApiClient brasilApiClient;

    @Override
    public BrasilApiResponse buscarDadosPorCnpj(String cnpj) {

        String cnpjLimpo = cnpj.replaceAll("[^0-9]", "");

        return brasilApiClient.consultarCnpj(cnpjLimpo);
    }
}