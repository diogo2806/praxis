package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Faixa de pontuação e quantidade de evidências de uma competência em uma rota terminal.")
public record PathCompetencyMetricsResponse(
        @Schema(example = "Comunicação")
        String competency,

        @Schema(example = "20")
        int minimumScore,

        @Schema(example = "180")
        int maximumScore,

        @Schema(example = "3")
        int evidenceCount,

        @Schema(example = "true")
        boolean observable
) {
}
