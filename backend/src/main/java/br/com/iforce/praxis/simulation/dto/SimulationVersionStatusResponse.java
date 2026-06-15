package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;

import java.time.Instant;

public record SimulationVersionStatusResponse(
        String simulationId,
        int versionNumber,
        SimulationVersionStatus status,
        Instant publishedAt
) {
}
