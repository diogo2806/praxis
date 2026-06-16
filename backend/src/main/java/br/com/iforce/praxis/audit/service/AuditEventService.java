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
    public static final String SIMULATION_AGGREGATE = "Simulation";
    public static final String SIMULATION_VERSION_AGGREGATE = "SimulationVersion";

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void appendCandidateAttemptEvent(
            String tenantId,
            String attemptId,
            AuditEventType eventType,
            String message,
            String metadata
    ) {
        AuditEventEntity auditEventEntity = new AuditEventEntity();
        auditEventEntity.setTenantId(tenantId);
        auditEventEntity.setAggregateType(CANDIDATE_ATTEMPT_AGGREGATE);
        auditEventEntity.setAggregateId(attemptId);
        auditEventEntity.setEventType(eventType);
        auditEventEntity.setMessage(message);
        auditEventEntity.setMetadata(metadata);
        auditEventEntity.setCreatedAt(Instant.now());

        auditEventRepository.save(auditEventEntity);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void appendSimulationEvent(
            String tenantId,
            String simulationId,
            AuditEventType eventType,
            String message,
            String metadata
    ) {
        AuditEventEntity auditEventEntity = new AuditEventEntity();
        auditEventEntity.setTenantId(tenantId);
        auditEventEntity.setAggregateType(SIMULATION_AGGREGATE);
        auditEventEntity.setAggregateId(simulationId);
        auditEventEntity.setEventType(eventType);
        auditEventEntity.setMessage(message);
        auditEventEntity.setMetadata(metadata);
        auditEventEntity.setCreatedAt(Instant.now());

        auditEventRepository.save(auditEventEntity);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void appendSimulationVersionEvent(
            String tenantId,
            String simulationId,
            int versionNumber,
            AuditEventType eventType,
            String message,
            String metadata
    ) {
        AuditEventEntity auditEventEntity = new AuditEventEntity();
        auditEventEntity.setTenantId(tenantId);
        auditEventEntity.setAggregateType(SIMULATION_VERSION_AGGREGATE);
        auditEventEntity.setAggregateId(simulationVersionAggregateId(simulationId, versionNumber));
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

    @Transactional(readOnly = true)
    public List<AuditEventResponse> listSimulationVersionEvents(String simulationId, int versionNumber) {
        return auditEventRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        SIMULATION_VERSION_AGGREGATE,
                        simulationVersionAggregateId(simulationId, versionNumber)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String simulationVersionAggregateId(String simulationId, int versionNumber) {
        return simulationId + ":v" + versionNumber;
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
