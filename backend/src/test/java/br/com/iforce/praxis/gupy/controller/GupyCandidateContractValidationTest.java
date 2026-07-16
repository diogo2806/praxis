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
    void acceptsOfficialPreviousResultValueAndAppliesRetestRule() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateRequest(4398157501L, "internal", "\"fail\"")))
                .andExpect(status().isConflict());
    }

    @Test
    void acceptsExplicitJsonNullAsNoPreviousResult() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateRequest(4398157502L, "external", "null")))
                .andExpect(status().isCreated());
    }

    @Test
    void acceptsOfficialStringNullAsNoPreviousResult() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(candidateRequest(4398157507L, "external", "\"null\"")))
                .andExpect(status().isCreated());
    }

    @Test
    void acceptsOmittedOptionalContractFieldsAsNull() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 1,
                                  "document_id": 4398157503,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Candidato Contrato",
                                  "email": "contrato@example.com",
                                  "job_id": 100,
                                  "callback_url": "https://cliente.gupy.io/candidate-return"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsUnknownCandidateType() throws Exception {
        assertInvalidContractValue(candidateRequest(4398157504L, "partner", "null"));
    }

    @Test
    void rejectsArtificialPreviousResultNone() throws Exception {
        assertInvalidContractValue(candidateRequest(4398157505L, "external", "\"none\""));
    }

    @Test
    void rejectsPreviousResultPass() throws Exception {
        assertInvalidContractValue(candidateRequest(4398157506L, "external", "\"pass\""));
    }

    private void assertInvalidContractValue(String requestBody) throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido."));
    }

    private String candidateRequest(long documentId, String candidateType, String previousResultJson) {
        return """
                {
                  "company_id": 1,
                  "document_id": %d,
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
