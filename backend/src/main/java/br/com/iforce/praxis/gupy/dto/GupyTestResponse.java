package br.com.iforce.praxis.gupy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teste Práxis publicado e visível para consumo pela Gupy.")
public record GupyTestResponse(
        @Schema(example = "sim-atendimento-n2")
        String id,

        @Schema(example = "Atendimento N2")
        String name,

        @Schema(example = "Avaliação situacional determinística para priorização, comunicação e decisão em contexto.")
        String description
) {
}
