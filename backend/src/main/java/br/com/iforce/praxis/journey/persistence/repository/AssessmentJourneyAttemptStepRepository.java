package br.com.iforce.praxis.journey.persistence.repository;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptStepEntity;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;


public interface AssessmentJourneyAttemptStepRepository extends JpaRepository<AssessmentJourneyAttemptStepEntity, Long> {

    Optional<AssessmentJourneyAttemptStepEntity> findByEmpresaIdAndId(String empresaId, Long id);
}
