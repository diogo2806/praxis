package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resumo comparável de uma rota que termina a avaliação.")
public record TerminalRouteResponse(
        @Schema(example = "R1")
        String routeId,

        @Schema(example = "fim-aprovado")
        String terminalNodeId,

        @Schema(description = "Etapas percorridas, incluindo o encerramento quando ele é um nó explícito.")
        List<String> nodeIds,

        @Schema(description = "Etapas da rota que recebem duas ou mais ramificações.")
        List<String> convergenceNodeIds,

        @Schema(example = "4")
        int decisionCount,

        @Schema(example = "180")
        int estimatedDurationSeconds,

        @Schema(example = "42")
        int estimatedDifficultyPercent,

        @Schema(example = "40")
        int rawMinimumScore,

        @Schema(example = "520")
        int rawMaximumScore,

        @Schema(example = "100")
        int maximumNormalizedScore,

        List<PathCompetencyMetricsResponse> competencies
) {
}
