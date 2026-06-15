package br.com.iforce.praxis.audit.service;

import br.com.iforce.praxis.audit.dto.AuditEventResponse;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.persistence.entity.AuditEventEntity;
import br.com.iforce.praxis.audit.persistence.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuditEventService {

    public static final String CANDIDATE_ATTEMPT_AGGREGATE = "CandidateAttempt";

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void appendCandidateAttemptEvent(
            String attemptId,
            AuditEventType eventType,
            String message,
            String metadata
    ) {
        AuditEventEntity auditEventEntity = new AuditEventEntity();
        auditEventEntity.setAggregateType(CANDIDATE_ATTEMPT_AGGREGATE);
        auditEventEntity.setAggregateId(attemptId);
        auditEventEntity.setEventType(eventType);
        auditEventEntity.setMessage(message);
        auditEventEntity.setMetadata(metadata);
        auditEventEntity.setCreatedAt(Instant.now());

        auditEventRepository.save(auditEventEntity);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> listCandidateAttemptEvents(String attemptId) {
        return auditEventRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(CANDIDATE_ATTEMPT_AGGREGATE, attemptId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditEventResponse toResponse(AuditEventEntity auditEventEntity) {
        return new AuditEventResponse(
                auditEventEntity.getId(),
                auditEventEntity.getAggregateType(),
                auditEventEntity.getAggregateId(),
                auditEventEntity.getEventType(),
                auditEventEntity.getMessage(),
                auditEventEntity.getMetadata(),
                auditEventEntity.getCreatedAt()
        );
    }
}
