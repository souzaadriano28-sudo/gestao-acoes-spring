package com.trabalho.gestao_acoes.integrations.exchange;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "bcb-ptax", url = "${integrations.bcb-ptax.url:https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata}")
public interface BcbPtaxClient {
    @GetMapping("/CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)")
    BcbPtaxResponse period(@RequestParam("@dataInicial") String from,
            @RequestParam("@dataFinalCotacao") String to,
            @RequestParam("$format") String format,
            @RequestParam("$top") int top);
}
