package br.com.iforce.praxis.shared.privacy.persistence.repository;

import br.com.iforce.praxis.shared.privacy.model.ComplianceRequestStatus;
import br.com.iforce.praxis.shared.privacy.persistence.entity.HumanReviewRequestEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HumanReviewRequestRepository extends JpaRepository<HumanReviewRequestEntity, String> {

    List<HumanReviewRequestEntity> findByEmpresaIdOrderByRequestedAtDesc(String empresaId);

    Optional<HumanReviewRequestEntity> findByIdAndEmpresaId(String id, String empresaId);

    Optional<HumanReviewRequestEntity> findFirstByAttemptIdAndStatusInOrderByRequestedAtDesc(
            String attemptId,
            Collection<ComplianceRequestStatus> statuses
    );

    List<HumanReviewRequestEntity> findByAttemptId(String attemptId);

    List<HumanReviewRequestEntity> findByStatusAndResultReleasedAtIsNullOrderByResolvedAtAsc(
            ComplianceRequestStatus status,
            Pageable pageable
    );
}
