package com.trabalho.gestao_acoes.repositories;

import com.trabalho.gestao_acoes.domains.Corretora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CorretoraRepository extends JpaRepository<Corretora, Long> {

    // RF06 e RF12: Busca uma corretora específica pelo CNPJ para evitar duplicidade
    Optional<Corretora> findByCnpj(String cnpj);
}