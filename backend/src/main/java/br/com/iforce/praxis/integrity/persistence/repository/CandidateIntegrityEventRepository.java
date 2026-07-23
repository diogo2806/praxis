package br.com.iforce.praxis.integrity.persistence.repository;

import br.com.iforce.praxis.integrity.model.IntegrityEventType;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateIntegrityEventRepository
        extends JpaRepository<CandidateIntegrityEventEntity, Long>, CandidateIntegrityEventAtomicRepository {

    boolean existsBySessionIdAndSequenceNumber(String sessionId, Long sequenceNumber);

    long countByEmpresaIdAndCandidateAttemptIdAndEventType(
            String empresaId,
            String candidateAttemptId,
            IntegrityEventType eventType
    );

    List<CandidateIntegrityEventEntity> findByEmpresaIdAndCandidateAttemptIdOrderByOccurredAtAscIdAsc(
            String empresaId,
            String candidateAttemptId
    );

    Page<CandidateIntegrityEventEntity> findByEmpresaIdOrderByOccurredAtAscIdAsc(
            String empresaId,
            Pageable pageable
    );
}
