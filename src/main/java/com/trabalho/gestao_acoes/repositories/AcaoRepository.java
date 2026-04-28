package com.trabalho.gestao_acoes.repositories;

import com.trabalho.gestao_acoes.domains.Acao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcaoRepository extends JpaRepository<Acao, Long> {

    // RF10 e RF12: Busca uma ação específica pelo Ticker para evitar duplicidade
    Optional<Acao> findByTicker(String ticker);
}