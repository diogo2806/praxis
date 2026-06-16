package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;

public record CloneSimulationVersionResponse(
        String simulationId,
        int sourceVersionNumber,
        int newVersionNumber,
        SimulationVersionStatus status
) {
}
