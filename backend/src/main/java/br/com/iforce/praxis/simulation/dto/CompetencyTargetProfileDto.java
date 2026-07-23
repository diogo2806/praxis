package br.com.iforce.praxis.simulation.dto;

public record CompetencyTargetProfileDto(
        String competencyName,
        int targetScore,
        String source,
        String warning
) {
}
