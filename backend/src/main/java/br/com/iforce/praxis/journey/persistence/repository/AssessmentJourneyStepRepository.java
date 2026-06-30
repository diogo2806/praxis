package br.com.iforce.praxis.journey.persistence.repository;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentJourneyStepRepository extends JpaRepository<AssessmentJourneyStepEntity, Long> {

    Optional<AssessmentJourneyStepEntity> findByTenantIdAndId(String tenantId, Long id);

    List<AssessmentJourneyStepEntity> findByTenantIdAndJourneyIdOrderBySequenceKeyAscOrderIndexAsc(
            String tenantId,
            String journeyId
    );
}
