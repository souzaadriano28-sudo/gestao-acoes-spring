package com.trabalho.gestao_acoes.repositories;

import com.trabalho.gestao_acoes.domains.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
}