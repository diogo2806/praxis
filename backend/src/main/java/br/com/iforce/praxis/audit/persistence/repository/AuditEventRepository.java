package br.com.iforce.praxis.audit.persistence.repository;

import br.com.iforce.praxis.audit.persistence.entity.AuditEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    @Query("SELECT e FROM AuditEventEntity e WHERE e.tenantId = :tenantId AND e.aggregateType = :aggregateType AND e.aggregateId = :aggregateId ORDER BY e.createdAt ASC")
    List<AuditEventEntity> findByTenantIdAndAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            @Param("tenantId") String tenantId,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") String aggregateId
    );

    /** Trilha de auditoria de um tenant alvo, do evento mais recente para o mais antigo. */
    List<AuditEventEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    /** Trilha de auditoria global da plataforma (uso administrativo). */
    List<AuditEventEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
