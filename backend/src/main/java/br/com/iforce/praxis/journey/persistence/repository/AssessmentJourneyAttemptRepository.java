package br.com.iforce.praxis.journey.persistence.repository;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptEntity;

import org.springframework.data.jpa.repository.EntityGraph;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

import java.util.Optional;

import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;


public interface AssessmentJourneyAttemptRepository extends JpaRepository<AssessmentJourneyAttemptEntity, String> {

    @EntityGraph(attributePaths = {"steps"})
    Optional<AssessmentJourneyAttemptEntity> findByEmpresaIdAndId(String empresaId, String id);

    @EntityGraph(attributePaths = {"steps"})
    List<AssessmentJourneyAttemptEntity> findByEmpresaIdAndJourneyIdOrderByCreatedAtDesc(
            String empresaId,
            String journeyId
    );

    long countByEmpresaIdAndStatusIn(String empresaId, List<AssessmentJourneyAttemptStatus> statuses);

    long countByEmpresaIdAndJourneyIdAndStatusIn(
            String empresaId,
            String journeyId,
            List<AssessmentJourneyAttemptStatus> statuses
    );
}
