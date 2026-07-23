package br.com.iforce.praxis.simulation.dto;

import java.time.Instant;
import java.util.List;

public record CandidateReferenceSnapshotDto(
        List<CompetencyTargetProfileDto> targetProfile,
        NormativeReferenceResponse normativeReference,
        DecisionThresholdResponse decisionThreshold,
        Instant capturedAt
) {
}
