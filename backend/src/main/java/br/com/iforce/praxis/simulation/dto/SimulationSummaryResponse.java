package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;

import java.time.Instant;
import java.util.List;

public record SimulationSummaryResponse(
        String id,
        String name,
        String description,
        int versionNumber,
        SimulationVersionStatus status,
        Instant updatedAt,
        List<String> competencies,
        long attemptsCreated,
        long attemptsCompleted,
        double completionRatePercent
) {
}
