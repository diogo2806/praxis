package br.com.iforce.praxis.journey.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;


@Schema(description = "Adiciona um teste publicado a uma jornada em rascunho.")
public record AddJourneyStepRequest(
        @NotBlank
        @Schema(example = "sim-atendimento-caos", description = "Simulação publicada a reaproveitar.")
        String simulationId,

        @Size(max = 80)
        @Schema(example = "principal", description = "Sequência/caminho da jornada. Padrão: 'principal'.")
        String sequenceKey,

        @Min(0)
        @Schema(example = "0", description = "Ordem do teste na sequência. Se ausente, vai para o fim.")
        Integer orderIndex,

        @Schema(example = "true", description = "Indica se o teste é obrigatório para concluir a jornada. Padrão: true.")
        Boolean required
) {
}
