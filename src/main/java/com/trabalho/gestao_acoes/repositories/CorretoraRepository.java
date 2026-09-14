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
import java.util.List;

@Repository
public interface CorretoraRepository extends JpaRepository<Corretora, Long> {

    Optional<Corretora> findByCnpjAndOwnerId(String cnpj, Long ownerId);
    Optional<Corretora> findByIdAndOwnerId(Long id, Long ownerId);
    List<Corretora> findAllByOwnerId(Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "1000"))
    @Query("select c from Corretora c where c.id = :id and c.owner.id = :ownerId")
    Optional<Corretora> findByIdAndOwnerIdForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);
}
