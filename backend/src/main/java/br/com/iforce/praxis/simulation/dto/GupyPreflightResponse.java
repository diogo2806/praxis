package br.com.iforce.praxis.simulation.dto;

import java.util.List;


public record GupyPreflightResponse(
        String simulationId,
        int versionNumber,
        boolean ok,
        List<GupyPreflightCheckResponse> checks
) {
}
