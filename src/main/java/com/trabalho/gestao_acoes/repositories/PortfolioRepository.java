package com.trabalho.gestao_acoes.repositories;

import com.trabalho.gestao_acoes.domains.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Optional<Portfolio> findByIdAndOwnerId(Long id, Long ownerId);
    List<Portfolio> findAllByOwnerId(Long ownerId);
    Optional<Portfolio> findFirstByOwnerIdOrderByIdAsc(Long ownerId);
}
