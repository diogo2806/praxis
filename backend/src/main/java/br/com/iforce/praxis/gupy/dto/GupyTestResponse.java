package br.com.iforce.praxis.gupy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Teste Praxis publicado e visivel para consumo pela Gupy.")
public record GupyTestResponse(
        @Schema(example = "sim-atendimento-n2")
        String id,

        @Schema(example = "Atendimento N2")
        String name,

        @Schema(example = "Situational Judgment", nullable = true)
        String category,

        @Schema(example = "Avaliação situacional determinística para priorização, comunicação e decisão em contexto.")
        String description,

        @Schema(example = "advanced", allowableValues = {"advanced", "intermediate", "basic"}, nullable = true)
        String level
) {
}
