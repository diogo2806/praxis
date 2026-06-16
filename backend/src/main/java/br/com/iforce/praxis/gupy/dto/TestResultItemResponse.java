package br.com.iforce.praxis.gupy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Nota por competencia no formato esperado pela Gupy.")
public record TestResultItemResponse(
        @Schema(example = "86", minimum = "0", maximum = "100")
        int score,

        @Schema(example = "86%")
        String result_string,

        @Schema(example = "percentage")
        String type_result,

        @Schema(example = "major", allowableValues = {"major", "minor"})
        String tier,

        @Schema(example = "Empatia")
        String title,

        @Schema(example = "Pontuacao da competencia Empatia.")
        String description,

        @Schema(example = "2026-06-16T13:20:00Z")
        String date,

        Map<String, Object> other_informations
) {
}
