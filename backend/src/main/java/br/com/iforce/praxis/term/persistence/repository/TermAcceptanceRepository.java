package br.com.iforce.praxis.term.persistence.repository;

import br.com.iforce.praxis.term.persistence.entity.TermAcceptanceEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


import java.util.Optional;


@Repository
public interface TermAcceptanceRepository extends JpaRepository<TermAcceptanceEntity, Long> {

    Optional<TermAcceptanceEntity> findFirstByEmpresaIdAndUserIdAndTermTypeOrderByAcceptedAtDesc(
            String empresaId,
            String userId,
            String termType
    );
}
