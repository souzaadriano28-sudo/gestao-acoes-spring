package com.trabalho.gestao_acoes.integrations.twelvedata;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "twelvedata", url = "https://api.twelvedata.com")
public interface TwelveDataClient {

    @GetMapping("/price")
    TwelveDataResponse consultarCotacao(@RequestParam("symbol") String symbol, @RequestParam("apikey") String apiKey);
}