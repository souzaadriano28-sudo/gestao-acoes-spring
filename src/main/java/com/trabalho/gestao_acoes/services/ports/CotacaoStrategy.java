package com.trabalho.gestao_acoes.services.ports;

public interface CotacaoStrategy {

    // O método que as nossas integrações terão que implementar
    CotacaoBolsa buscarCotacao(String ticker);

    // Método para o sistema saber qual estratégia usar
    boolean suportaMercado(String mercado);
}