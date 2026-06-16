package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para criar um turno no grafo da simulacao.")
public record CreateNodeRequest(
        @NotBlank
        @Size(max = 1200)
        @Schema(example = "Chegou quebrado. Quero meu dinheiro de volta agora.")
        String clientMessage,

        @Schema(example = "45")
        Integer timeLimitSeconds,

        @Size(max = 1000)
        @Schema(example = "Turno inicial com pressao alta.")
        String timeJustification
) {
}
