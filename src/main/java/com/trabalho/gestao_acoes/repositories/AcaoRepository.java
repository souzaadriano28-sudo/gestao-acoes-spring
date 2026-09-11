package com.trabalho.gestao_acoes.repositories;

import com.trabalho.gestao_acoes.domains.Acao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface AcaoRepository extends JpaRepository<Acao, Long> {

    // RF10 e RF12: Busca uma ação específica pelo Ticker para evitar duplicidade
    Optional<Acao> findByTickerAndMercadoAndOwnerId(String ticker, String mercado, Long ownerId);
    Optional<Acao> findByTickerAndOwnerId(String ticker, Long ownerId);
    Optional<Acao> findByIdAndOwnerId(Long id, Long ownerId);
    List<Acao> findAllByOwnerId(Long ownerId);
}
