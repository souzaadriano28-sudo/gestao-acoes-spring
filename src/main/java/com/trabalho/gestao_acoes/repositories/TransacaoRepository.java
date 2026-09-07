package com.trabalho.gestao_acoes.repositories;

import com.trabalho.gestao_acoes.domains.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.trabalho.gestao_acoes.domains.enums.TipoTransacao;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    @EntityGraph(attributePaths = {"acao", "corretora"})
    @Query("select t from Transacao t where (:type is null or t.tipo = :type) " +
            "and (:ticker is null or t.acao.ticker = :ticker) " +
            "and (:brokerId is null or t.corretora.id = :brokerId) " +
            "and t.dataHora >= coalesce(:fromDate, t.dataHora) " +
            "and t.dataHora <= coalesce(:toDate, t.dataHora)")
    Page<Transacao> findMovements(@Param("type") TipoTransacao type, @Param("ticker") String ticker,
            @Param("brokerId") Long brokerId, @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate, Pageable pageable);
}
