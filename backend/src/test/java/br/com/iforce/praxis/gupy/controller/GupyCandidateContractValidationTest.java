package br.com.iforce.praxis.gupy.controller;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class GupyCandidateContractValidationTest {

    private static final String AUTHORIZATION = "Bearer empresa1-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsOfficialCandidateTypeAndPreviousResult() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateRequest("candidate-contract-valid", "internal", "\"fail\"")))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsUnknownCandidateType() throws Exception {
        assertInvalidContractValue(candidateRequest(
                "candidate-contract-invalid-type",
                "partner",
                "null"
        ));
    }

    @Test
    void rejectsArtificialPreviousResultNone() throws Exception {
        assertInvalidContractValue(candidateRequest(
                "candidate-contract-invalid-none",
                "external",
                "\"none\""
        ));
    }

    @Test
    void rejectsPreviousResultPass() throws Exception {
        assertInvalidContractValue(candidateRequest(
                "candidate-contract-invalid-pass",
                "external",
                "\"pass\""
        ));
    }

    @Test
    void rejectsStringNullInsteadOfJsonNull() throws Exception {
        assertInvalidContractValue(candidateRequest(
                "candidate-contract-invalid-string-null",
                "external",
                "\"null\""
        ));
    }

    private void assertInvalidContractValue(String requestBody) throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido."));
    }

    private String candidateRequest(String documentId, String candidateType, String previousResultJson) {
        return """
                {
                  "company_id": "empresa-123",
                  "document_id": "%s",
                  "test_id": "sim-atendimento-caos",
                  "name": "Candidato Contrato",
                  "email": "contrato@example.com",
                  "job_id": 100,
                  "callback_url": "https://cliente.gupy.io/candidate-return",
                  "candidate_type": "%s",
                  "previous_result": %s
                }
                """.formatted(documentId, candidateType, previousResultJson);
    }
}
