package br.com.iforce.praxis.billing.persistence.repository;

import br.com.iforce.praxis.billing.model.SubscriptionStatus;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaSubscriptionEntity;

import org.springframework.data.jpa.repository.JpaRepository;


import java.time.Instant;

import java.util.List;

import java.util.Optional;


public interface EmpresaSubscriptionRepository extends JpaRepository<EmpresaSubscriptionEntity, Long> {

    Optional<EmpresaSubscriptionEntity> findFirstByEmpresaIdOrderByCreatedAtDesc(String empresaId);

    Optional<EmpresaSubscriptionEntity> findByMpPreapprovalId(String mpPreapprovalId);

    /** Assinaturas inadimplentes cuja carência já venceu (candidatas a suspensão automática). */
    List<EmpresaSubscriptionEntity> findByStatusAndGraceUntilBefore(SubscriptionStatus status, Instant moment);
}
