package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resultado do arquivamento logico de uma simulacao.")
public record ArchiveSimulationResponse(
        @Schema(example = "sim-atendimento-caos")
        String simulationId,

        @Schema(example = "true")
        boolean archived,

        @Schema(example = "2026-06-16T13:20:00Z")
        Instant deletedAt,

        @Schema(example = "user-123")
        String deletedBy
) {
}
