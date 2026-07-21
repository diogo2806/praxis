package br.com.iforce.praxis.journey.persistence.repository;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface AssessmentJourneyAttemptStepRepository extends JpaRepository<AssessmentJourneyAttemptStepEntity, Long> {

    Optional<AssessmentJourneyAttemptStepEntity> findByEmpresaIdAndId(String empresaId, Long id);

    @Query("""
            SELECT step.candidateAttemptId
            FROM AssessmentJourneyAttemptStepEntity step
            WHERE step.empresaId = :empresaId
              AND step.candidateAttemptId IS NOT NULL
            """)
    Set<String> findCandidateAttemptIdsByEmpresaId(@Param("empresaId") String empresaId);
}
