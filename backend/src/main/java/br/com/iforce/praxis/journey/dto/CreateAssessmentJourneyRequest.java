package br.com.iforce.praxis.journey.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Pedido para criar uma Jornada de Avaliação em rascunho.")
public record CreateAssessmentJourneyRequest(
        @NotBlank
        @Size(max = 180)
        @Schema(example = "Processo Trainee 2026")
        String name,

        @Size(max = 1000)
        @Schema(example = "Jornada com testes de atendimento, tomada de decisão e escrita.")
        String description
) {
}
