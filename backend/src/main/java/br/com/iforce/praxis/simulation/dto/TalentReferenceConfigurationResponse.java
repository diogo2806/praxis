package br.com.iforce.praxis.simulation.dto;

import java.util.List;

public record TalentReferenceConfigurationResponse(
        List<CompetencyTargetProfileDto> targetProfile,
        List<NormativeReferenceResponse> normativeGroups,
        List<DecisionThresholdResponse> decisionThresholds,
        List<String> warnings
) {
}
