package br.com.iforce.praxis.integrity.persistence.repository;

import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityEventEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.transaction.annotation.Transactional;

public class CandidateIntegrityEventAtomicRepositoryImpl implements CandidateIntegrityEventAtomicRepository {

    private static final String INSERT_IF_ABSENT_SQL = """
            INSERT INTO candidate_integrity_events (
                empresa_id,
                candidate_attempt_id,
                session_id,
                event_type,
                occurred_at,
                received_at,
                input_mode,
                visibility_state,
                sequence_number,
                detail
            ) VALUES (
                :empresaId,
                :candidateAttemptId,
                :sessionId,
                :eventType,
                :occurredAt,
                :receivedAt,
                :inputMode,
                :visibilityState,
                :sequenceNumber,
                :detail
            )
            ON CONFLICT (session_id, sequence_number)
            WHERE sequence_number IS NOT NULL
            DO NOTHING
            """;

    private final EntityManager entityManager;

    public CandidateIntegrityEventAtomicRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public boolean insertIfAbsent(CandidateIntegrityEventEntity event) {
        if (event == null || event.getSequenceNumber() == null) {
            throw new IllegalArgumentException("O evento sequenciado é obrigatório para a inserção idempotente.");
        }

        Query query = entityManager.createNativeQuery(INSERT_IF_ABSENT_SQL);
        query.setParameter("empresaId", event.getEmpresaId());
        query.setParameter("candidateAttemptId", event.getCandidateAttemptId());
        query.setParameter("sessionId", event.getSessionId());
        query.setParameter("eventType", event.getEventType().name());
        query.setParameter("occurredAt", event.getOccurredAt());
        query.setParameter("receivedAt", event.getReceivedAt());
        query.setParameter("inputMode", event.getInputMode() == null ? null : event.getInputMode().name());
        query.setParameter("visibilityState", event.getVisibilityState());
        query.setParameter("sequenceNumber", event.getSequenceNumber());
        query.setParameter("detail", event.getDetail());
        return query.executeUpdate() == 1;
    }
}
