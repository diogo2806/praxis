package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * Distribuição das notas finais de uma competência na amostra de tentativas
 * concluídas: média e desvio-padrão (populacional).
 */
@Schema(description = "Distribuição das notas de uma competência na calibração.")
public record CompetencyCalibrationDto(
        @Schema(example = "Resolução de Conflitos")
        String competencyName,

        @Schema(example = "68.4")
        double averageScore,

        @Schema(example = "14.2")
        double stdDeviation
) {
}
