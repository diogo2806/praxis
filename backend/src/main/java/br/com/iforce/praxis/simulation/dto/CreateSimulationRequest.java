package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Dados para criar uma simulacao e sua versao inicial em rascunho.")
public record CreateSimulationRequest(
        @NotBlank
        @Size(max = 180)
        @Schema(example = "Analista de Atendimento N2")
        String name,

        @NotBlank
        @Size(max = 180)
        @Schema(example = "Atendimento")
        String role,

        @Size(max = 120)
        @Schema(example = "Pleno")
        String seniority,

        @Size(max = 1000)
        @Schema(example = "Avaliar tomada de decisao em situacoes de pressao.")
        String objective,

        @Valid
        @NotEmpty
        @Size(max = 12)
        List<CompetencyWeightDto> competencies,

        @Size(max = 1200)
        @Schema(example = "Cliente exige estorno fora da politica.")
        String criticalSituation,

        @Size(max = 1000)
        @Schema(example = "Prometer estorno sem validar regras internas.")
        String criticalError
) {
}
