package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.auth.service.JwtService;
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
        "praxis.candidate-page-base-url=https://praxis.iforce.com.br"
})
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class CandidateAttemptSecurityEnabledTest {

    private static final String AUTHORIZATION = "Bearer empresa1-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void publicCandidateAttemptLoadsWithOpaqueIdentifiersWhenSecurityIsEnabled() throws Exception {
        MvcResult createResult = createAttempt(4398157401L);
        String testUrl = JsonPath.read(createResult.getResponse().getContentAsString(), "$.test_url");
        String token = testUrl.substring(testUrl.lastIndexOf('/') + 1);

        mockMvc.perform(get("/candidate/attempts/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participacaoId").value(org.hamcrest.Matchers.startsWith("pub_")))
                .andExpect(jsonPath("$.etapaAtual.descricao").exists())
                .andExpect(jsonPath("$.etapaAtual.id").value(org.hamcrest.Matchers.startsWith("pub_")));
    }

    @Test
    void internalAttemptIdIsRejectedWhenSecurityIsEnabled() throws Exception {
        MvcResult createResult = createAttempt(4398157402L);
        String testUrl = JsonPath.read(createResult.getResponse().getContentAsString(), "$.test_url");
        String publicToken = testUrl.substring(testUrl.lastIndexOf('/') + 1);
        String attemptId = jwtService.parseCandidateAttemptToken(publicToken).attemptId();

        mockMvc.perform(get("/candidate/attempts/" + attemptId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidatePagePathRedirectsToConfiguredPublicFrontendWithoutAuthentication() throws Exception {
        MvcResult createResult = createAttempt(4398157403L);
        String testUrl = JsonPath.read(createResult.getResponse().getContentAsString(), "$.test_url");
        String token = testUrl.substring(testUrl.lastIndexOf('/') + 1);

        mockMvc.perform(get("/candidato/" + token))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://praxis.iforce.com.br/candidato/" + token));
    }

    private MvcResult createAttempt(long documentId) throws Exception {
        return mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 1,
                                  "document_id": %d,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "callback_url": "https://cliente.gupy.io/candidate-return",
                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                                  "candidate_type": "external",
                                  "previous_result": null
                                }
                                """.formatted(documentId)))
                .andExpect(status().isCreated())
                .andReturn();
    }
}
