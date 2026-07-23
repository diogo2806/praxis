package br.com.iforce.praxis.simulation.dto;

import java.util.List;

public record TalentMatchResponse(
        String simulationId,
        int versionNumber,
        List<CompetencyTargetProfileDto> targetProfile,
        NormativeReferenceResponse normativeReference,
        DecisionThresholdResponse decisionThreshold,
        List<String> warnings,
        List<CandidateRadarDto> candidates
) {
}
