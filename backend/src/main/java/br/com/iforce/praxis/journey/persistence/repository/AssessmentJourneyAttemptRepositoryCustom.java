package br.com.iforce.praxis.journey.persistence.repository;

import java.util.Optional;

public interface AssessmentJourneyAttemptRepositoryCustom {

    Optional<JourneyRedirectTarget> findJourneyRedirectTarget(
            String empresaId,
            String candidateAttemptId
    );
}
