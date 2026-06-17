package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Schema(description = "Payload para criar uma alternativa de resposta em um turno.")
public record CreateOptionRequest(
        @NotBlank
        @Size(max = 800)
        String text,

        @NotNull
        @Schema(example = "{\"Empatia\": 86, \"Aderencia a politica\": 92}")
        Map<@NotBlank @Size(max = 140) String, @Min(0) @Max(100) Integer> competencyLevels,

        boolean isBest,

        boolean isCritical,

        @Size(max = 120)
        String nextNodeId,

        @Size(max = 1000)
        String resultingTone
) {
}
