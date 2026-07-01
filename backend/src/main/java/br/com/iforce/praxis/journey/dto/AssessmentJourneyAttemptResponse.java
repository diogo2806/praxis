package br.com.iforce.praxis.journey.dto;

import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;

import io.swagger.v3.oas.annotations.media.Schema;


import java.time.Instant;

import java.util.List;


@Schema(description = "Progresso da tentativa de um candidato em uma jornada.")
public record AssessmentJourneyAttemptResponse(
        @Schema(example = "jatt_abc123")
        String id,

        @Schema(example = "processo-trainee-2026-abc12345")
        String journeyId,

        @Schema(example = "Processo Trainee 2026")
        String journeyName,

        @Schema(example = "Maria Silva")
        String candidateName,

        @Schema(example = "maria@example.com")
        String candidateEmail,

        @Schema(example = "principal")
        String sequenceKey,

        @Schema(example = "inProgress")
        AssessmentJourneyAttemptStatus status,

        @Schema(example = "2026-06-30T12:00:00Z")
        Instant startedAt,

        @Schema(example = "2026-06-30T13:00:00Z")
        Instant completedAt,

        @Schema(example = "2026-06-30T11:00:00Z")
        Instant createdAt,

        @Schema(description = "Etapas da sequência escolhida, na ordem de execução.")
        List<JourneyAttemptStepResponse> steps
) {
}
