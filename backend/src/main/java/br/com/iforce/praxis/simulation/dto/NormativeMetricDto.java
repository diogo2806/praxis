package br.com.iforce.praxis.simulation.dto;

public record NormativeMetricDto(
        String competencyName,
        int sampleSize,
        double mean,
        double standardDeviation
) {
}
