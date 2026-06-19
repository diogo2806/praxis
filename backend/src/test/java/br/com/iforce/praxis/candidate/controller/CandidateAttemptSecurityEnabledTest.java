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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "praxis.security.enabled=true",
        "praxis.jwt-secret=test-only-strong-jwt-secret-2026-with-more-than-32-chars",
        "praxis.integration-token=test-only-strong-gupy-token-2026",
        "praxis.candidate-page-base-url=https://praxis.iforce.com.br"
})
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class CandidateAttemptSecurityEnabledTest {

    private static final String AUTHORIZATION = "Bearer test-only-strong-gupy-token-2026";

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
                .andExpect(jsonPath("$.participacaoId").value(org.hamcrest.Matchers.startsWith("att_")))
                .andExpect(jsonPath("$.etapaAtual.descricao").exists())
                .andExpect(jsonPath("$.etapaAtual.id").doesNotExist());
    }

    @Test
    void candidatePagePathRedirectsToConfiguredPublicFrontendWithoutAuthentication() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": "empresa-123",
                                  "document_id": "candidate-page-redirect-with-security",
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
        String token = testUrl.substring(testUrl.lastIndexOf('/') + 1);

        mockMvc.perform(get("/candidato/" + token))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://praxis.iforce.com.br/candidato/" + token));
    }
}
