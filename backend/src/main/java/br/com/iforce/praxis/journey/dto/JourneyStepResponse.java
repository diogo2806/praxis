package br.com.iforce.praxis.journey.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teste (simulação publicada) dentro de uma jornada.")
public record JourneyStepResponse(
        @Schema(example = "10")
        Long id,

        @Schema(example = "sim-atendimento-caos")
        String simulationId,

        @Schema(example = "Teste de Atendimento")
        String simulationName,

        @Schema(example = "3")
        int simulationVersionNumber,

        @Schema(example = "principal")
        String sequenceKey,

        @Schema(example = "0")
        int orderIndex,

        @Schema(example = "true")
        boolean required
) {
}
