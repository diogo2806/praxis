package br.com.iforce.praxis.gupy.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestResultResponseContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesOnlyOfficialTopLevelFieldsForEndpointAndWebhook() {
        JsonNode payload = objectMapper.valueToTree(response());

        List<String> fieldNames = new ArrayList<>();
        payload.fieldNames().forEachRemaining(fieldNames::add);

        assertThat(fieldNames).containsExactlyInAnyOrder(
                "title",
                "testCode",
                "description",
                "providerName",
                "company_result_string",
                "providerLink",
                "status",
                "result_page_url",
                "result_candidate_page_url",
                "results"
        );
        assertThat(payload.has("reliabilityLevel")).isFalse();
        assertThat(payload.has("other_informations")).isFalse();
        assertThat(payload.path("results").get(0).has("other_informations")).isTrue();
    }

    @Test
    void ignoresLegacyTopLevelExtensionsFromPersistedOutboxPayload() throws Exception {
        TestResultResponse response = objectMapper.readerFor(TestResultResponse.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue("""
                        {
                          "title": "Teste",
                          "testCode": "teste",
                          "description": "Descricao",
                          "providerName": "Praxis",
                          "company_result_string": "Resultado",
                          "providerLink": "https://praxis.example",
                          "status": "done",
                          "result_page_url": "https://praxis.example/results/att_1",
                          "result_candidate_page_url": "https://praxis.example/candidato/token/resultado",
                          "reliabilityLevel": "NORMAL",
                          "other_informations": {
                            "timeout_count": 1
                          },
                          "results": []
                        }
                        """);

        JsonNode payload = objectMapper.valueToTree(response);

        assertThat(payload.has("reliabilityLevel")).isFalse();
        assertThat(payload.has("other_informations")).isFalse();
        assertThat(payload.path("results").isArray()).isTrue();
    }

    private TestResultResponse response() {
        return new TestResultResponse(
                "Teste",
                "teste",
                "Descricao",
                "Praxis",
                "Resultado",
                "https://praxis.example",
                "done",
                "https://praxis.example/results/att_1",
                "https://praxis.example/candidato/token/resultado",
                List.of(new TestResultItemResponse(
                        90,
                        "90%",
                        "percentage",
                        "major",
                        "Competencia",
                        "Descricao da competencia.",
                        "2026-07-15T12:00:00Z",
                        Map.of("time", 11)
                ))
        );
    }
}
