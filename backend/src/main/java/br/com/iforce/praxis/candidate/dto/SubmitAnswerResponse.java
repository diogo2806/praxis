package br.com.iforce.praxis.candidate.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado operacional do salvamento de resposta.")
public record SubmitAnswerResponse(
        @Schema(example = "att_123")
        String attemptId,

        @Schema(example = "completed")
        AttemptStatus status,

        @Schema(example = "true")
        boolean duplicate,

        @Schema(example = "true")
        boolean completed,

        CandidateNodeResponse currentNode
) {
}
