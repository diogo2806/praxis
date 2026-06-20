package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Dados para criar uma simulacao em rascunho a partir do plano da avaliacao inicial.")
public record CreateSimulationDraftRequest(
        @NotBlank
        @Size(max = 180)
        @Schema(example = "Analista de Atendimento N2")
        String name,

        @NotBlank
        @Size(max = 1000)
        @Schema(example = "Cliente quer estorno fora da politica, com risco de churn.")
        String description,

        @NotBlank
        @Size(max = 120)
        @Schema(example = "turno-1")
        String rootNodeId,

        @NotEmpty
        @Size(max = 12)
        @Schema(description = "Competencias avaliadas no plano da avaliacao.")
        List<@NotBlank @Size(max = 140) String> competencies,

        @Size(max = 1200)
        @Schema(example = "Cliente exige solucao imediata para atraso recorrente.")
        String criticalSituation,

        @Size(max = 120)
        @Schema(example = "Triagem")
        String resultUse
) {
}
