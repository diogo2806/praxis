package br.com.iforce.praxis.journey.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "Atualiza ordem, sequência ou obrigatoriedade de uma etapa da jornada.")
public record UpdateJourneyStepRequest(
        @Size(max = 80)
        @Schema(example = "A")
        String sequenceKey,

        @Min(0)
        @Schema(example = "1")
        Integer orderIndex,

        @Schema(example = "false")
        Boolean required
) {
}
