package br.com.iforce.praxis.gupy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta com URL da simulação e identificador para consulta do resultado.")
public record CreateCandidateResponse(
        @Schema(example = "http://localhost:8080/candidate/attempts/att_123")
        String testUrl,

        @Schema(example = "res_123")
        String testResultId,

        @Schema(example = "att_123")
        String attemptId
) {
}
