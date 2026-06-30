package br.com.iforce.praxis.journey.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Atualiza os dados básicos de uma jornada em rascunho.")
public record UpdateAssessmentJourneyRequest(
        @Size(max = 180)
        @Schema(example = "Processo Trainee 2026")
        String name,

        @Size(max = 1000)
        @Schema(example = "Descrição atualizada da jornada.")
        String description
) {
}
