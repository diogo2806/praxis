package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Competencia avaliada e seu peso normalizado no score final.")
public record CompetencyWeightDto(
        @NotBlank
        @Size(max = 140)
        @Schema(example = "Empatia")
        String name,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        @Schema(example = "0.4")
        Double weight
) {
}
