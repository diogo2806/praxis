package br.com.iforce.praxis.gupy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Resposta com URL da simulação e identificador para consulta do resultado.")
public record CreateCandidateResponse(
        @JsonProperty("test_url")
        @Schema(example = "http://localhost:8080/candidate/attempts/att_123")
        String testUrl,

        @JsonProperty("test_result_id")
        @Schema(example = "res_123")
        String testResultId
) {
}
