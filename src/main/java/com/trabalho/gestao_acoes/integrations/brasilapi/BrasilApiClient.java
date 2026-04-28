package com.trabalho.gestao_acoes.integrations.brasilapi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "brasilapi", url = "https://brasilapi.com.br/api/cnpj/v1")
public interface BrasilApiClient {

    @GetMapping("/{cnpj}")
    BrasilApiResponse consultarCnpj(@PathVariable("cnpj") String cnpj);
}