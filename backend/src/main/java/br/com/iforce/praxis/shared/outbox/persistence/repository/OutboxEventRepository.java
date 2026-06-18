package br.com.iforce.praxis.shared.outbox.persistence.repository;

import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

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
}
