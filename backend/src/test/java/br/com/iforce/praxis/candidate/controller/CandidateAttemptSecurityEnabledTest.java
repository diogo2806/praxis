package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private CandidateAttemptRepository candidateAttemptRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void publicCandidateAttemptLoadsWithoutAuthenticationWhenSecurityIsEnabled() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 1,
                                  "document_id": 4398157401,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "callback_url": "https://cliente.gupy.io/candidate-return",
                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                                  "candidate_type": "external",
                                  "previous_result": null
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
                .andExpect(jsonPath("$.etapaAtual.id").value("turno-1"));
    }

    @Test
    void legacyCandidateAttemptIdResolvesEmpresaFromAttemptWhenSecurityIsEnabled() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 1,
                                  "document_id": 4398157402,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "callback_url": "https://cliente.gupy.io/candidate-return",
                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                                  "candidate_type": "external",
                                  "previous_result": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String testUrl = JsonPath.read(createResult.getResponse().getContentAsString(), "$.test_url");
        String publicToken = testUrl.substring(testUrl.lastIndexOf('/') + 1);
        String attemptId = jwtService.parseCandidateAttemptToken(publicToken).attemptId();
        jdbcTemplate.update("""
                INSERT INTO empresas (id, name, company_id)
                VALUES ('empresa-legacy-link', 'Empresa Legacy Link', 'empresa-legacy-link')
                """);
        CandidateAttemptEntity attempt = candidateAttemptRepository.findById(attemptId).orElseThrow();
        attempt.setEmpresaId("empresa-legacy-link");
        candidateAttemptRepository.save(attempt);

        mockMvc.perform(get("/candidate/attempts/" + attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participacaoId").value(attemptId))
                .andExpect(jsonPath("$.etapaAtual.id").value("turno-1"));
    }

    @Test
    void candidatePagePathRedirectsToConfiguredPublicFrontendWithoutAuthentication() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 1,
                                  "document_id": 4398157403,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "callback_url": "https://cliente.gupy.io/candidate-return",
                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                                  "candidate_type": "external",
                                  "previous_result": null
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
