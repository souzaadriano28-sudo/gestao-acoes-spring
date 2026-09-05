package com.trabalho.gestao_acoes.repositories;

import com.trabalho.gestao_acoes.domains.PosicaoCarteira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PosicaoCarteiraRepository extends JpaRepository<PosicaoCarteira, Long> {
    Optional<PosicaoCarteira> findByAcaoTickerAndCorretoraId(String ticker, Long corretoraId);
    Optional<PosicaoCarteira> findByAcaoIdAndCorretoraId(Long acaoId, Long corretoraId);
}
