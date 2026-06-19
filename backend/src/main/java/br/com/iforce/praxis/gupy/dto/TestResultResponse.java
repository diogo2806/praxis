package br.com.iforce.praxis.gupy.dto;

import br.com.iforce.praxis.gupy.model.ReliabilityLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Payload EXATO esperado pela Gupy no /test/result/{resultId}")
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
        ReliabilityLevel reliabilityLevel,
        Map<String, Object> other_informations,
        List<TestResultItemResponse> results
) {
}
