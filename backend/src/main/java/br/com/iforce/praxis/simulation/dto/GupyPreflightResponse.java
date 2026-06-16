package br.com.iforce.praxis.simulation.dto;

import java.time.Instant;
import java.util.List;

public record GupyPreflightResponse(
        String simulationId,
        int versionNumber,
        boolean ok,
        boolean integrationActive,
        Instant integrationActivatedAt,
        List<GupyPreflightCheckResponse> checks
) {
}
