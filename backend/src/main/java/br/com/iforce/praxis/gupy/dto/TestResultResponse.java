package br.com.iforce.praxis.gupy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado de teste retornado para a Gupy.")
public record TestResultResponse(
        @Schema(example = "Atendimento N2")
        String title,

        @Schema(example = "sim-atendimento-n2")
        String testCode,

        @Schema(example = "Avaliacao situacional deterministica.")
        String description,

        @Schema(example = "Praxis")
        String providerName,

        @Schema(example = "Score geral: 78/100")
        String company_result_string,

        @Schema(example = "https://praxis.example.com")
        String providerLink,

        @Schema(example = "done", allowableValues = {"notStarted", "paused", "done"})
        String status,

        @Schema(example = "https://praxis.example.com/test/result/res_123")
        String result_page_url,

        @Schema(example = "https://praxis.example.com/candidate/attempts/att_123")
        String result_candidate_page_url,

        List<TestResultItemResponse> results
) {
}
