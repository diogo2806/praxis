package br.com.iforce.praxis.journey.persistence.repository;

/**
 * Dados materializados necessários para redirecionar uma tentativa individual
 * de volta à jornada que a originou.
 */
public record JourneyRedirectTarget(
        String journeyAttemptId,
        Long stepId
) {
}
