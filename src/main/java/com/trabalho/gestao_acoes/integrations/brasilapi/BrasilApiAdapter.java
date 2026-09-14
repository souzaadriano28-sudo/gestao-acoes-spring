package com.trabalho.gestao_acoes.integrations.brasilapi;

import com.trabalho.gestao_acoes.services.ports.CnpjClientPort;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BrasilApiAdapter implements CnpjClientPort {
    private static final Logger log = LoggerFactory.getLogger(BrasilApiAdapter.class);

    private final BrasilApiClient brasilApiClient;

    public BrasilApiAdapter(BrasilApiClient brasilApiClient) {
        this.brasilApiClient = brasilApiClient;
    }

    @Override
    public BrasilApiResponse buscarDadosPorCnpj(String cnpj) {

        try {
            return brasilApiClient.consultarCnpj(cnpj);
        } catch (RuntimeException failure) {
            if (failure instanceof FeignException feign) {
                log.warn("BrasilAPI CNPJ consultation failed: http_status={}", feign.status());
            } else {
                log.warn("BrasilAPI CNPJ consultation failed: exception_type={}", failure.getClass().getSimpleName());
            }
            throw failure;
        }
    }
}
