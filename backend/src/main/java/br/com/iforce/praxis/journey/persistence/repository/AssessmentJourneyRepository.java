package br.com.iforce.praxis.journey.persistence.repository;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentJourneyRepository extends JpaRepository<AssessmentJourneyEntity, String> {

    @EntityGraph(attributePaths = {"steps"})
    List<AssessmentJourneyEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    @EntityGraph(attributePaths = {"steps"})
    Optional<AssessmentJourneyEntity> findByTenantIdAndId(String tenantId, String id);

    boolean existsById(String id);
}
