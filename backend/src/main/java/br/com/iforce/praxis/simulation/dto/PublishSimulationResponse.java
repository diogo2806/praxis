package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;

import io.swagger.v3.oas.annotations.media.Schema;


import java.time.Instant;


@Schema(description = "Resposta da publicação de uma versão de simulação.")
public record PublishSimulationResponse(
        @Schema(example = "sim-atendimento-caos")
        String simulationId,

        @Schema(example = "1")
        int versionNumber,

        @Schema(example = "published")
        SimulationVersionStatus status,

        @Schema(example = "2026-06-15T20:00:00Z")
        Instant publishedAt
) {
}
