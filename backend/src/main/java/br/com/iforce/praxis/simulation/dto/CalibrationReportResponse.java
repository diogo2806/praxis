package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import java.util.List;


/**
 * Relatório de calibração estatística de uma versão de simulação.
 *
 * <p>Quando a amostra de tentativas concluídas ainda é pequena
 * ({@code sufficientSample = false}), as listas vêm vazias e a tela mostra
 * apenas o aviso de amostra insuficiente.</p>
 */
@Schema(description = "Relatório de calibração estatística da versão.")
public record CalibrationReportResponse(
        @Schema(example = "84")
        long sampleSize,

        @Schema(example = "30")
        int minimumSampleRequired,

        @Schema(example = "true")
        boolean sufficientSample,

        List<OptionDiscriminationDto> items,

        List<CompetencyCalibrationDto> competencies
) {

    public static CalibrationReportResponse insufficient(long sampleSize, int minimumSampleRequired) {
        return new CalibrationReportResponse(sampleSize, minimumSampleRequired, false, List.of(), List.of());
    }
}
