package br.com.iforce.praxis.candidate.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado público da tentativa do candidato.")
public record CandidateAttemptResponse(
        @Schema(example = "att_123")
        String attemptId,

        @Schema(example = "O Dia do Caos")
        String simulationName,

        @Schema(example = "notStarted")
        AttemptStatus status,

        @Schema(example = "false")
        boolean completed,

        CandidateNodeResponse currentNode
) {
}
