package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

@Schema(description = "Payload para criar uma simulacao em rascunho a partir do blueprint inicial.")
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
        @Schema(description = "Competencias avaliadas no blueprint.")
        List<@NotBlank @Size(max = 140) String> competencies,

        @Size(max = 120)
        @Schema(example = "Pleno")
        String seniority,

        @Size(max = 1200)
        @Schema(example = "Cliente exige solucao imediata para atraso recorrente.")
        String criticalSituation,

        @Size(max = 1000)
        @Schema(example = "Reconhece o impacto, aplica a politica e propoe proximos passos.")
        String highPerformance,

        @Size(max = 1000)
        @Schema(example = "Prometer excecao proibida ou culpar outra area.")
        String criticalError,

        @Size(max = 6)
        @Schema(description = "Comportamento esperado por nivel de senioridade.")
        Map<@NotBlank @Size(max = 120) String, @Size(max = 600) String> seniorityExpectations,

        @Size(max = 120)
        @Schema(example = "Triagem")
        String resultUse
) {
}
