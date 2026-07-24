package br.com.iforce.praxis.audit.persistence.repository;

import br.com.iforce.praxis.audit.persistence.entity.AuditEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    @Query("SELECT e FROM AuditEventEntity e WHERE e.empresaId = :empresaId AND e.aggregateType = :aggregateType AND e.aggregateId = :aggregateId ORDER BY e.createdAt ASC")
    List<AuditEventEntity> findByEmpresaIdAndAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            @Param("empresaId") String empresaId,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") String aggregateId
    );

    /** Conta chamadas autenticadas concluídas em um endpoint específico da integração. */
    @Query(value = """
            SELECT COUNT(*)
            FROM audit_events
            WHERE empresa_id = :empresaId
              AND aggregate_type = 'Integration'
              AND aggregate_id = :provider
              AND metadata::jsonb ->> 'endpoint' = :endpoint
            """, nativeQuery = true)
    long countIntegrationEndpointEvidence(
            @Param("empresaId") String empresaId,
            @Param("provider") String provider,
            @Param("endpoint") String endpoint
    );

    /** Trilha de auditoria de um empresa alvo, do evento mais recente para o mais antigo. */
    List<AuditEventEntity> findByEmpresaIdOrderByCreatedAtDesc(String empresaId, Pageable pageable);

    /** Trilha de auditoria global da plataforma (uso administrativo). */
    List<AuditEventEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
