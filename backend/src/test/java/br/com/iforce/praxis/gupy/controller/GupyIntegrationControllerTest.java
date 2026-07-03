package br.com.iforce.praxis.gupy.controller;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;

import org.springframework.test.context.jdbc.Sql;

import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.MvcResult;


import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.empty;

import static org.hamcrest.Matchers.containsString;

import static org.hamcrest.Matchers.not;

import static org.hamcrest.Matchers.startsWith;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class GupyIntegrationControllerTest {

    private static final String AUTHORIZATION = "Bearer empresa1-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listPublishedTestsRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token Bearer obrigatório."));
    }

    @Test
    void listPublishedTestsReturnsPublishedSimulationsWithoutInternalScoringRules() throws Exception {
        mockMvc.perform(get("/test").header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(50))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.total_tests").value(2))
                .andExpect(jsonPath("$.payload", not(empty())))
                .andExpect(jsonPath("$.payload[?(@.id=='sim-atendimento-caos')].name")
                        .value(org.hamcrest.Matchers.hasItem("Cenario Seed de Teste")))
                .andExpect(jsonPath("$.payload[0].category").value("Situational Judgment"))
                .andExpect(jsonPath("$.payload[0].level").value("advanced"))
                .andExpect(jsonPath("$.payload[0].isBest").doesNotExist())
                .andExpect(jsonPath("$.payload[0].weight").doesNotExist());
    }

    @Test
    void listPublishedTestsSupportsSearchAndPagination() throws Exception {
        mockMvc.perform(get("/test")
                        .header("Authorization", AUTHORIZATION)
                        .param("searchString", "seed")
                        .param("offset", "0")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(1))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.total_tests").value(1))
                .andExpect(jsonPath("$.payload[0].id").value("sim-atendimento-caos"));

        mockMvc.perform(get("/test")
                        .header("Authorization", AUTHORIZATION)
                        .param("searchString", "inexistente")
                        .param("offset", "0")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_tests").value(0))
                .andExpect(jsonPath("$.payload").isEmpty());
    }

    @Test
    void createCandidateAttemptReturnsCandidateUrlAndResultId() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest("candidate-document-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.test_url").value(startsWith("http://localhost:8080/candidato/")))
                .andExpect(jsonPath("$.test_result_id").value(startsWith("res_")))
                .andExpect(jsonPath("$.attemptId").doesNotExist());
    }

    @Test
    void createCandidateAttemptIsIdempotentByCompanyDocumentAndTest() throws Exception {
        MvcResult firstResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest("candidate-document-2")))
                .andExpect(status().isCreated())
                .andReturn();

        String firstBody = firstResult.getResponse().getContentAsString();

        MvcResult secondResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest("candidate-document-2")))
                .andExpect(status().isCreated())
                .andReturn();

        String secondBody = secondResult.getResponse().getContentAsString();
        assertThat(secondBody).isEqualTo(firstBody);
    }

    @Test
    void getTestResultReturnsAttemptStatus() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest("candidate-document-3")))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String resultId = JsonPath.read(responseBody, "$.test_result_id");

        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", AUTHORIZATION)
                        .param("company_id", "empresa-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Cenario Seed de Teste"))
                .andExpect(jsonPath("$.testCode").value("sim-atendimento-caos"))
                .andExpect(jsonPath("$.providerName").value("Praxis"))
                .andExpect(jsonPath("$.status").value("notStarted"))
                .andExpect(jsonPath("$.score").doesNotExist())
                .andExpect(jsonPath("$.company_result_string").exists())
                .andExpect(content().string(containsString("\"title\":\"Empatia\"")))
                .andExpect(content().string(containsString("\"tier\":\"major\"")));
    }

    @Test
    void getTestResultRequiresMatchingCompanyId() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest("candidate-document-wrong-company")))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String resultId = JsonPath.read(responseBody, "$.test_result_id");

        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", AUTHORIZATION)
                        .param("company_id", "outra-empresa"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCandidateAttemptRejectsCompanyIdThatDoesNotBelongToToken() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": "outra-empresa",
                                  "document_id": "candidate-document-forbidden",
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCandidateAttemptValidatesRequiredFields() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos."))
                .andExpect(jsonPath("$.fields.companyId").exists())
                .andExpect(jsonPath("$.fields.documentId").exists())
                .andExpect(jsonPath("$.fields.testId").exists())
                .andExpect(jsonPath("$.fields.candidateName").exists())
                .andExpect(jsonPath("$.fields.candidateEmail").exists());
    }

    private String validCandidateRequest(String documentId) {
        return """
                {
                  "company_id": "empresa-123",
                  "document_id": "%s",
                  "test_id": "sim-atendimento-caos",
                  "name": "Thiago Souza",
                  "email": "thiago@example.com",
                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                  "candidate_type": "external",
                  "previous_result": "none"
                }
                """.formatted(documentId);
    }
}

