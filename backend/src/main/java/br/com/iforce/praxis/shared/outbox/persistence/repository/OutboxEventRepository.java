package br.com.iforce.praxis.shared.outbox.persistence.repository;

import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
        List<OutboxEventEntity.OutboxEventStatus> statuses,
        Instant now
    );

    List<OutboxEventEntity> findByTenantIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
        String tenantId,
        List<OutboxEventEntity.OutboxEventStatus> statuses,
        Instant now
    );

    List<OutboxEventEntity> findByTenantIdAndEventTypeOrderByCreatedAtDesc(String tenantId, String eventType);

    List<OutboxEventEntity> findByTenantIdAndEventTypeAndStatusOrderByCreatedAtDesc(
        String tenantId,
        String eventType,
        OutboxEventEntity.OutboxEventStatus status
    );

    Optional<OutboxEventEntity> findByIdAndTenantId(Long id, String tenantId);
}
