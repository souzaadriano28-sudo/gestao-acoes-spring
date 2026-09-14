package com.trabalho.gestao_acoes.repositories;

import com.trabalho.gestao_acoes.domains.PosicaoCarteira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface PosicaoCarteiraRepository extends JpaRepository<PosicaoCarteira, Long> {
    Optional<PosicaoCarteira> findByAcaoTickerAndCorretoraIdAndPortfolioId(String ticker, Long corretoraId, Long portfolioId);
    Optional<PosicaoCarteira> findByAcaoIdAndCorretoraIdAndPortfolioId(Long acaoId, Long corretoraId, Long portfolioId);

    @EntityGraph(attributePaths = {"acao", "corretora"})
    @Query("select p from PosicaoCarteira p where p.portfolio.id = :portfolioId and (:market is null or p.acao.mercado = :market) and (:brokerId is null or p.corretora.id = :brokerId)")
    Page<PosicaoCarteira> findDetailed(@Param("portfolioId") Long portfolioId, @Param("market") String market, @Param("brokerId") Long brokerId, Pageable pageable);

    @EntityGraph(attributePaths = {"acao", "corretora"})
    @Query("select p from PosicaoCarteira p where p.portfolio.id = :portfolioId")
    java.util.List<PosicaoCarteira> findAllDetailed(@Param("portfolioId") Long portfolioId);
}
