package com.trabalho.gestao_acoes.integrations.brapi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "brapi", url = "https://brapi.dev/api")
public interface BrapiClient {

    // O token é necessário na Brapi agora. Usaremos um parâmetro de query.
    @GetMapping("/quote/{ticker}")
    BrapiResponse consultarCotacao(@PathVariable("ticker") String ticker, @RequestParam("token") String token);
}