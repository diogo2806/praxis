package br.com.iforce.praxis.gupy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teste Praxis publicado e visivel para consumo pela Gupy.")
public record GupyTestResponse(
        @Schema(example = "sim-atendimento-n2")
        String id,

        @Schema(example = "Atendimento N2")
        String name,

        @Schema(example = "Situational Judgment")
        String category,

        @Schema(example = "Avaliacao situacional deterministica para priorizacao, comunicacao e decisao em contexto.")
        String description,

        @Schema(example = "professional")
        String level
) {
}
