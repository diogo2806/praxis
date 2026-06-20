package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.simulation.model.ValidationIssueSeverity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Problema estrutural encontrado na validação da simulação.")
public record ValidationIssueResponse(
        @Schema(example = "blocker")
        ValidationIssueSeverity severity,

        @Schema(example = "turno-1")
        String nodeId,

        @Schema(example = "Uma resposta aponta para uma etapa que não existe.")
        String message
) {
}
