package br.com.iforce.praxis.journey.persistence.repository;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;

public interface AssessmentJourneyAttemptRepository extends JpaRepository<AssessmentJourneyAttemptEntity, String> {

    @EntityGraph(attributePaths = {"steps"})
    Optional<AssessmentJourneyAttemptEntity> findByTenantIdAndId(String tenantId, String id);

    @EntityGraph(attributePaths = {"steps"})
    List<AssessmentJourneyAttemptEntity> findByTenantIdAndJourneyIdOrderByCreatedAtDesc(
            String tenantId,
            String journeyId
    );

    long countByTenantIdAndStatusIn(String tenantId, List<AssessmentJourneyAttemptStatus> statuses);

    long countByTenantIdAndJourneyIdAndStatusIn(
            String tenantId,
            String journeyId,
            List<AssessmentJourneyAttemptStatus> statuses
    );
}
