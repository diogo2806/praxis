package br.com.iforce.praxis.integrity.persistence.repository;

import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateIntegrityReviewRepository extends JpaRepository<CandidateIntegrityReviewEntity, String> {

    Optional<CandidateIntegrityReviewEntity> findByEmpresaIdAndCandidateAttemptId(
            String empresaId,
            String candidateAttemptId
    );

    Page<CandidateIntegrityReviewEntity> findByEmpresaIdOrderByUpdatedAtDesc(
            String empresaId,
            Pageable pageable
    );
}
