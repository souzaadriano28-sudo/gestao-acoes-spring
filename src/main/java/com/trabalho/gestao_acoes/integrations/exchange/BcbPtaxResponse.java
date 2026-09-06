package com.trabalho.gestao_acoes.integrations.exchange;

import java.math.BigDecimal;
import java.util.List;

public record BcbPtaxResponse(List<Quote> value) {
    public record Quote(BigDecimal cotacaoVenda, String dataHoraCotacao, String tipoBoletim) {}
}
