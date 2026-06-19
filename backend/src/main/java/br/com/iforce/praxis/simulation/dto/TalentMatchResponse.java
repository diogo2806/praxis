package br.com.iforce.praxis.simulation.dto;

import java.util.List;

public record TalentMatchResponse(
        String simulationId,
        int versionNumber,
        List<CompetencyBenchmarkDto> benchmark,
        List<CandidateRadarDto> candidates
) {
}
