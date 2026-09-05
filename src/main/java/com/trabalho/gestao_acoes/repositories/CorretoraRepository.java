package com.trabalho.gestao_acoes.repositories;

import com.trabalho.gestao_acoes.domains.Corretora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

@Repository
public interface CorretoraRepository extends JpaRepository<Corretora, Long> {

    // RF06 e RF12: Busca uma corretora específica pelo CNPJ para evitar duplicidade
    Optional<Corretora> findByCnpj(String cnpj);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "1000"))
    @Query("select c from Corretora c where c.id = :id")
    Optional<Corretora> findByIdForUpdate(@Param("id") Long id);
}
