package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.gupy.model.ResultTier;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;

import jakarta.validation.constraints.DecimalMax;

import jakarta.validation.constraints.DecimalMin;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotEmpty;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;


import java.util.List;


@Schema(description = "Dados para atualizar o plano estrutural de uma versao de simulacao.")
public record UpdateBlueprintRequest(
        @NotBlank
        @Size(max = 120)
        @Schema(example = "turno-1")
        String rootNodeId,

        @Valid
        @NotEmpty
        @Schema(description = "Competencias avaliadas. Na interface, os pesos devem somar 100%.")
        List<CompetencyRequest> competencies,

        @Size(max = 1200)
        @Schema(example = "Cliente exige solucao imediata para atraso recorrente.")
        String criticalSituation,

        @Size(max = 120)
        @Schema(example = "Triagem")
        String resultUse
) {

    public record CompetencyRequest(
            @NotBlank
            @Size(max = 140)
            @Schema(example = "Empatia")
            String name,

            @NotNull
            @DecimalMin("0.0")
            @DecimalMax("1.0")
            @Schema(example = "0.35", description = "Peso normalizado para API. Equivale a 35% na interface.")
            Double weight,

            @Min(0)
            @Max(100)
            @Schema(example = "80", description = "Nota alvo da competência para a vaga.")
            Integer targetScore,

            @Schema(example = "major", description = "Peso de severidade da competência no resultado.")
            ResultTier tier
    ) {
        public int normalizedTargetScore() {
            return targetScore == null ? 70 : targetScore;
        }

        public ResultTier normalizedTier() {
            return tier == null ? ResultTier.MAJOR : tier;
        }
    }
}
