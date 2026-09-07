package com.trabalho.gestao_acoes.integrations.cvm;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "cvm-registry", url = "${integrations.cvm-registry.url:https://dados.cvm.gov.br/dados/INTERMED/CAD/DADOS}")
public interface CvmRegistryClient {
    @GetMapping(value = "/cad_intermed.zip")
    ResponseEntity<byte[]> download();
}
