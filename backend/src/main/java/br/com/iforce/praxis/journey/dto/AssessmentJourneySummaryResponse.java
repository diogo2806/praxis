package br.com.iforce.praxis.journey.dto;

import br.com.iforce.praxis.journey.model.AssessmentJourneyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resumo de uma Jornada de Avaliação para listagem.")
public record AssessmentJourneySummaryResponse(
        @Schema(example = "processo-trainee-2026-abc12345")
        String id,

        @Schema(example = "Processo Trainee 2026")
        String name,

        @Schema(example = "Jornada com testes de atendimento e escrita.")
        String description,

        @Schema(example = "draft")
        AssessmentJourneyStatus status,

        @Schema(example = "6")
        int stepCount,

        @Schema(example = "2")
        int sequenceCount,

        @Schema(example = "2026-06-30T12:00:00Z")
        Instant createdAt,

        @Schema(example = "2026-06-30T12:00:00Z")
        Instant updatedAt
) {
}
