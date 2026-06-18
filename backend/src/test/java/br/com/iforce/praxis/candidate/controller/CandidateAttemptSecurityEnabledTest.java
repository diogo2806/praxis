package br.com.iforce.praxis.candidate.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "praxis.security.enabled=true")
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class CandidateAttemptSecurityEnabledTest {

    private static final String AUTHORIZATION = "Bearer dev-company-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicCandidateAttemptLoadsWithoutAuthenticationWhenSecurityIsEnabled() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": "empresa-123",
                                  "document_id": "candidate-public-with-security",
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "callback_url": "https://cliente.gupy.io/callback",
                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                                  "candidate_type": "external",
                                  "previous_result": "none"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String testUrl = JsonPath.read(createResult.getResponse().getContentAsString(), "$.test_url");
        String attemptId = testUrl.substring(testUrl.lastIndexOf('/') + 1);

        mockMvc.perform(get("/candidate/attempts/" + attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.currentNode.id").value("turno-1"));
    }
}
