package br.com.iforce.praxis.gupy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Contrato oficial TestResult esperado pela Gupy no /test/result/{resultId} e no webhook")
public record TestResultResponse(
        String title,
        String testCode,
        String description,
        String providerName,
        String company_result_string,        // Markdown para o RH
        String providerLink,
        String status,                       // "done"
        String result_page_url,
        String result_candidate_page_url,
        List<TestResultItemResponse> results
) {
}
