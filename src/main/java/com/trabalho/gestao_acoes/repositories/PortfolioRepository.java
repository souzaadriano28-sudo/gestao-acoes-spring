package com.trabalho.gestao_acoes.repositories;

import com.trabalho.gestao_acoes.domains.Portfolio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Optional<Portfolio> findByIdAndOwnerId(Long id, Long ownerId);
    List<Portfolio> findAllByOwnerId(Long ownerId);
    Optional<Portfolio> findFirstByOwnerIdOrderByIdAsc(Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "1000"))
    @Query("select p from Portfolio p where p.owner.id = :ownerId order by p.id asc")
    Optional<Portfolio> findFirstByOwnerIdForUpdate(@Param("ownerId") Long ownerId);
}
