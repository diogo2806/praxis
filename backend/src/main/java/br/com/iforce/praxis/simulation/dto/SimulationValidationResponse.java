package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import java.util.List;


@Schema(description = "Resultado da validação estrutural antes da publicação.")
public record SimulationValidationResponse(
        @Schema(example = "sim-atendimento-caos")
        String simulationId,

        @Schema(example = "1")
        int versionNumber,

        @Schema(example = "true")
        boolean publishable,

        @Schema(example = "0")
        long blockerCount,

        @Schema(example = "1")
        long warningCount,

        @Schema(example = "90")
        int qualityScore,

        List<ValidationIssueResponse> issues
) {
}
