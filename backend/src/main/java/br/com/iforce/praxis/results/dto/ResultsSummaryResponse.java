package br.com.iforce.praxis.results.dto;

public record ResultsSummaryResponse(
        long completed,
        long inProgress,
        long expired,
        Integer averageScore
) {
}
