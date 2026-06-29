package br.com.iforce.praxis.billing.persistence.repository;

import br.com.iforce.praxis.billing.model.SubscriptionStatus;
import br.com.iforce.praxis.billing.persistence.entity.TenantSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscriptionEntity, Long> {

    Optional<TenantSubscriptionEntity> findFirstByTenantIdOrderByCreatedAtDesc(String tenantId);

    Optional<TenantSubscriptionEntity> findByMpPreapprovalId(String mpPreapprovalId);

    /** Assinaturas inadimplentes cuja carência já venceu (candidatas a suspensão automática). */
    List<TenantSubscriptionEntity> findByStatusAndGraceUntilBefore(SubscriptionStatus status, Instant moment);
}
