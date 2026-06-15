package br.com.iforce.praxis.gupy.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.ResultDecision;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado de teste retornado para a Gupy.")
public record TestResultResponse(
        @Schema(example = "res_123")
        String id,

        @Schema(example = "notStarted")
        AttemptStatus status,

        @Schema(example = "78", minimum = "0", maximum = "100", nullable = true)
        Integer score,

        List<TestResultItemResponse> results,

        @Schema(example = "recommendInterview")
        ResultDecision decision,

        @Schema(example = "false")
        boolean humanReviewRequired,

        @Schema(example = "Trilha auditável de pontuação para a empresa.")
        String companyResultString
) {
}
