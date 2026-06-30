package br.com.iforce.praxis.journey.dto;

import br.com.iforce.praxis.journey.model.AssessmentJourneyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Detalhe de uma Jornada de Avaliação, com suas sequências e testes.")
public record AssessmentJourneyDetailResponse(
        @Schema(example = "processo-trainee-2026-abc12345")
        String id,

        @Schema(example = "Processo Trainee 2026")
        String name,

        @Schema(example = "Jornada com testes de atendimento e escrita.")
        String description,

        @Schema(example = "draft")
        AssessmentJourneyStatus status,

        @Schema(example = "2026-06-30T12:00:00Z")
        Instant createdAt,

        @Schema(example = "2026-06-30T12:00:00Z")
        Instant updatedAt,

        @Schema(example = "2026-06-30T12:00:00Z")
        Instant publishedAt,

        @Schema(description = "Sequências da jornada, cada uma com seus testes em ordem.")
        List<SequenceResponse> sequences
) {

    @Schema(description = "Uma sequência/caminho da jornada com seus testes ordenados.")
    public record SequenceResponse(
            @Schema(example = "principal")
            String sequenceKey,

            List<JourneyStepResponse> steps
    ) {
    }
}
