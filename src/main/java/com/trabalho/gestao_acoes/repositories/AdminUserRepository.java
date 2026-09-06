package com.trabalho.gestao_acoes.repositories;

import com.trabalho.gestao_acoes.domains.AdminUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsername(String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AdminUser u where u.username = :username")
    Optional<AdminUser> findForUpdateByUsername(@Param("username") String username);
}
