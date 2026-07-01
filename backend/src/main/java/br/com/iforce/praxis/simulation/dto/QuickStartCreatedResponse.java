package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/** Resultado da criação de um rascunho a partir de um modelo pronto. */
@Schema(description = "Rascunho criado a partir de um modelo pronto.")
public record QuickStartCreatedResponse(
        @Schema(example = "atendimento-sob-pressao-1a2b3c4d")
        String simulationId,

        @Schema(example = "1")
        int versionNumber,

        @Schema(example = "/nova/mapa")
        String redirectTo
) {
}
