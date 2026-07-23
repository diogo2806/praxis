package br.com.iforce.praxis.integrity.persistence.repository;

import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityReviewAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateIntegrityReviewAuditRepository
        extends JpaRepository<CandidateIntegrityReviewAuditEntity, Long> {

    List<CandidateIntegrityReviewAuditEntity> findByEmpresaIdAndCandidateAttemptIdOrderByCreatedAtAscIdAsc(
            String empresaId,
            String candidateAttemptId
    );
}
