package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.simulation.model.CutScorePolicyStatus;

import java.time.Instant;

public record DecisionThresholdResponse(
        Long id,
        int score,
        String populationDescription,
        String justification,
        String evidence,
        Instant validFrom,
        Instant validUntil,
        CutScorePolicyStatus status,
        String approvedBy,
        String warning
) {
}
