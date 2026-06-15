package br.com.iforce.praxis.gupy.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class GupyIntegrationControllerTest {

    private static final String AUTHORIZATION = "Bearer dev-company-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listPublishedTestsRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token Bearer obrigatorio."));
    }

    @Test
    void listPublishedTestsReturnsPublishedSimulationsWithoutInternalScoringRules() throws Exception {
        mockMvc.perform(get("/test").header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].id").value("sim-atendimento-caos"))
                .andExpect(jsonPath("$[0].name").value("O Dia do Caos"))
                .andExpect(jsonPath("$[0].isBest").doesNotExist())
                .andExpect(jsonPath("$[0].weight").doesNotExist());
    }

    @Test
    void createCandidateAttemptReturnsCandidateUrlAndResultId() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest("candidate-document-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testUrl").value(startsWith("http://localhost:8080/candidate/attempts/att_")))
                .andExpect(jsonPath("$.testResultId").value(startsWith("res_")))
                .andExpect(jsonPath("$.attemptId").value(startsWith("att_")));
    }

    @Test
    void createCandidateAttemptIsIdempotentByCompanyDocumentAndTest() throws Exception {
        MvcResult firstResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest("candidate-document-2")))
                .andExpect(status().isOk())
                .andReturn();

        String firstBody = firstResult.getResponse().getContentAsString();

        MvcResult secondResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest("candidate-document-2")))
                .andExpect(status().isOk())
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
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String resultId = JsonPath.read(responseBody, "$.testResultId");

        mockMvc.perform(get("/test/result/" + resultId).header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resultId))
                .andExpect(jsonPath("$.status").value("notStarted"))
                .andExpect(jsonPath("$.score").doesNotExist())
                .andExpect(content().string(containsString("\"name\":\"Empatia\"")))
                .andExpect(content().string(containsString("\"tier\":\"major\"")));
    }

    @Test
    void createCandidateAttemptValidatesRequiredFields() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados invalidos."))
                .andExpect(jsonPath("$.fields.companyId").exists())
                .andExpect(jsonPath("$.fields.documentId").exists())
                .andExpect(jsonPath("$.fields.testId").exists())
                .andExpect(jsonPath("$.fields.candidateName").exists())
                .andExpect(jsonPath("$.fields.candidateEmail").exists());
    }

    private String validCandidateRequest(String documentId) {
        return """
                {
                  "companyId": "empresa-123",
                  "documentId": "%s",
                  "testId": "sim-atendimento-caos",
                  "candidateName": "Thiago Souza",
                  "candidateEmail": "thiago@example.com",
                  "callbackUrl": "https://cliente.gupy.io/callback",
                  "resultWebhookUrl": "https://cliente.gupy.io/result-webhook",
                  "candidateType": "external",
                  "previousResult": "none"
                }
                """.formatted(documentId);
    }
}
