package br.com.iforce.praxis.journey.dto;

import br.com.iforce.praxis.journey.model.AssessmentJourneyStepStatus;

import io.swagger.v3.oas.annotations.media.Schema;


import java.time.Instant;


@Schema(description = "Etapa da tentativa da jornada, ligada a uma tentativa individual de teste.")
public record JourneyAttemptStepResponse(
        @Schema(example = "100")
        Long id,

        @Schema(example = "10")
        Long journeyStepId,

        @Schema(example = "sim-atendimento-caos")
        String simulationId,

        @Schema(example = "Teste de Atendimento")
        String simulationName,

        @Schema(example = "3")
        int simulationVersionNumber,

        @Schema(example = "0")
        int orderIndex,

        @Schema(example = "true")
        boolean required,

        @Schema(example = "pending")
        AssessmentJourneyStepStatus status,

        @Schema(example = "att_abc123", description = "Tentativa individual (CandidateAttempt) do teste, quando iniciada.")
        String candidateAttemptId,

        @Schema(description = "Link do candidato para executar o teste, quando a etapa já foi iniciada.")
        String candidateUrl,

        @Schema(example = "2026-06-30T12:00:00Z")
        Instant startedAt,

        @Schema(example = "2026-06-30T12:30:00Z")
        Instant completedAt
) {
}
