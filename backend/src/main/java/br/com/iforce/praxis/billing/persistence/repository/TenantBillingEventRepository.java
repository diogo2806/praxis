package br.com.iforce.praxis.billing.persistence.repository;

import br.com.iforce.praxis.billing.model.BillingEventType;
import br.com.iforce.praxis.billing.persistence.entity.TenantBillingEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantBillingEventRepository extends JpaRepository<TenantBillingEventEntity, Long> {

    List<TenantBillingEventEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    /** Idempotência da aplicação financeira: evita aplicar duas vezes o mesmo recurso do MP. */
    boolean existsByMpResourceIdAndEventType(String mpResourceId, BillingEventType eventType);
}
