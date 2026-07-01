package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * Indicadores de calibração de uma opção (alternativa) específica.
 *
 * <p>O índice de discriminação compara, entre quem teve nota final alta
 * (grupo de cima) e quem teve nota final baixa (grupo de baixo), a proporção
 * que escolheu esta opção. O índice de dificuldade é a proporção de quem
 * respondeu a etapa e escolheu esta opção. A flag resume a leitura:
 * {@code OK}, {@code FRACO} ou {@code REVISAR}.</p>
 */
@Schema(description = "Indicadores de calibração de uma opção da simulação.")
public record OptionDiscriminationDto(
        @Schema(example = "turno-2")
        String nodeId,

        @Schema(example = "opcao-3")
        String optionId,

        @Schema(example = "Encaminhar direto para o supervisor sem tentar resolver.")
        String optionLabel,

        @Schema(example = "-0.12")
        double discriminationIndex,

        @Schema(example = "0.18")
        double difficultyIndex,

        @Schema(example = "REVISAR")
        CalibrationFlag flag
) {

    /** Leitura resumida da qualidade estatística de uma opção. */
    public enum CalibrationFlag {
        OK,
        FRACO,
        REVISAR
    }
}
