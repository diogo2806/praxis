package br.com.iforce.praxis.journey.persistence.repository;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentJourneyAttemptRepository extends JpaRepository<AssessmentJourneyAttemptEntity, String> {

    @EntityGraph(attributePaths = {"steps"})
    Optional<AssessmentJourneyAttemptEntity> findByTenantIdAndId(String tenantId, String id);

    @EntityGraph(attributePaths = {"steps"})
    List<AssessmentJourneyAttemptEntity> findByTenantIdAndJourneyIdOrderByCreatedAtDesc(
            String tenantId,
            String journeyId
    );
}
