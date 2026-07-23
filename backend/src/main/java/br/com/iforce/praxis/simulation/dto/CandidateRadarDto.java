package br.com.iforce.praxis.simulation.dto;

import java.util.List;

public record CandidateRadarDto(
        String attemptId,
        String candidateName,
        int generalScore,
        Integer normativePercentile,
        Boolean meetsDecisionThreshold,
        List<CompetencyScoreDto> competencies
) {
}
