package br.com.iforce.praxis.billing.persistence.repository;

import br.com.iforce.praxis.billing.model.BillingEventType;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaBillingEventEntity;

import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;


import java.time.Instant;

import java.util.List;


public interface EmpresaBillingEventRepository extends JpaRepository<EmpresaBillingEventEntity, Long> {

    List<EmpresaBillingEventEntity> findByEmpresaIdOrderByCreatedAtDesc(String empresaId, Pageable pageable);

    /** Idempotência da aplicação financeira: evita aplicar duas vezes o mesmo recurso do MP. */
    boolean existsByMpResourceIdAndEventType(String mpResourceId, BillingEventType eventType);

    /**
     * Anti-flood da régua de cobrança: indica se o cliente já recebeu um toque de um tipo desde o
     * instante informado, para não repetir a mesma notificação educativa em curto intervalo.
     */
    boolean existsByEmpresaIdAndEventTypeAndCreatedAtAfter(
            String empresaId, BillingEventType eventType, Instant after);
}
