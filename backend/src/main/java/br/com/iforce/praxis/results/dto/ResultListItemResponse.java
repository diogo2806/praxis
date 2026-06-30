package br.com.iforce.praxis.results.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;


import java.time.Instant;


public record ResultListItemResponse(
        String attemptId,
        String candidateName,
        String candidateEmail,
        String simulationId,
        String simulationTitle,
        AttemptStatus status,
        Instant startedAt,
        Instant finishedAt,
        Integer overallScore,
        String highlightCompetency,
        String integrationProvider
) {
}
