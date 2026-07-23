package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.simulation.model.NormativeGroupStatus;

import java.time.Instant;
import java.util.List;

public record NormativeReferenceResponse(
        Long id,
        String name,
        String jobTitle,
        String seniority,
        Long gupyJobId,
        String populationDescription,
        Instant periodStart,
        Instant periodEnd,
        int versionNumber,
        int sampleSize,
        int minimumSample,
        boolean eligible,
        NormativeGroupStatus status,
        String limitation,
        List<NormativeMetricDto> metrics
) {
}
