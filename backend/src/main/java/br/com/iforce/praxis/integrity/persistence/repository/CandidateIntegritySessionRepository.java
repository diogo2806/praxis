package br.com.iforce.praxis.integrity.persistence.repository;

import br.com.iforce.praxis.integrity.model.IntegritySessionStatus;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegritySessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CandidateIntegritySessionRepository extends JpaRepository<CandidateIntegritySessionEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CandidateIntegritySessionEntity> findFirstByCandidateAttemptIdAndStatusOrderByStartedAtDesc(
            String candidateAttemptId,
            IntegritySessionStatus status
    );

    Optional<CandidateIntegritySessionEntity> findByIdAndCandidateAttemptId(
            String id,
            String candidateAttemptId
    );

    List<CandidateIntegritySessionEntity> findByEmpresaIdAndCandidateAttemptIdOrderByStartedAtAsc(
            String empresaId,
            String candidateAttemptId
    );

    Page<CandidateIntegritySessionEntity> findByEmpresaIdAndStatusInAndClosedAtBeforeOrderByClosedAtAsc(
            String empresaId,
            Collection<IntegritySessionStatus> statuses,
            Instant closedBefore,
            Pageable pageable
    );

    boolean existsByEmpresaIdAndCandidateAttemptId(String empresaId, String candidateAttemptId);
}
