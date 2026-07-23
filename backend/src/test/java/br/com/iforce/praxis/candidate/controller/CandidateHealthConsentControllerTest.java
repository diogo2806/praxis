package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class CandidateHealthConsentControllerTest {

    private static final String AUTHORIZATION = "Bearer empresa1-token";
    private static final String CURRENT_VERSION = "2026-06-01";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private CandidateAttemptRepository candidateAttemptRepository;

    @BeforeEach
    void enableHealthVertical() {
        jdbcTemplate.update("""
                UPDATE empresas
                   SET health_vertical = TRUE,
                       health_compliance_approved_at = CURRENT_TIMESTAMP,
                       health_compliance_approved_by = 'test-user'
                 WHERE id = 'empresa-1'
                """);
    }

    @AfterEach
    void disableHealthVertical() {
        jdbcTemplate.update("""
                UPDATE empresas
                   SET health_vertical = FALSE,
                       health_compliance_approved_at = NULL,
                       health_compliance_approved_by = NULL
                 WHERE id = 'empresa-1'
                """);
    }

    @Test
    void healthAttemptRequiresPersistsRestoresAndRevokesConsent() throws Exception {
        MvcResult created = createAttempt("candidate-health-consent");
        String publicToken = tokenFromResponse(created.getResponse().getContentAsString());
        String attemptId = jwtService.parseCandidateAttemptToken(publicToken).attemptId();

        mockMvc.perform(get("/candidate/attempts/" + publicToken + "/health-consent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthVertical").value(true))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.noticeVersion").value(CURRENT_VERSION));

        mockMvc.perform(get("/candidate/attempts/" + publicToken))
                .andExpect(status().isPreconditionRequired());
        mockMvc.perform(post("/candidate/attempts/" + publicToken + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "optionId": "opcao-equilibrada",
                                  "timedOut": false
                                }
                                """))
                .andExpect(status().isPreconditionRequired());

        String consentBody = """
                {
                  "version": "2026-06-01",
                  "onBehalfOfMinor": false
                }
                """;
        mockMvc.perform(post("/candidate/attempts/" + publicToken + "/health-consent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(consentBody))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/candidate/attempts/" + publicToken + "/health-consent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(consentBody))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/candidate/attempts/" + publicToken + "/health-consent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
        mockMvc.perform(get("/candidate/attempts/" + publicToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verticalSaude").value(true));

        CandidateAttemptEntity consented = candidateAttemptRepository.findById(attemptId).orElseThrow();
        assertThat(consented.getHealthConsentVersion()).isEqualTo(CURRENT_VERSION);
        assertThat(consented.getHealthConsentRecordedAt()).isNotNull();
        assertThat(consented.getHealthConsentSubjectType()).isEqualTo("DATA_SUBJECT");
        assertThat(consented.getHealthConsentSource()).isEqualTo("CANDIDATE_PORTAL");

        mockMvc.perform(delete("/candidate/attempts/" + publicToken + "/health-consent"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/candidate/attempts/" + publicToken + "/health-consent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
        mockMvc.perform(get("/candidate/attempts/" + publicToken))
                .andExpect(status().isPreconditionRequired());
        mockMvc.perform(post("/candidate/attempts/" + publicToken + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "optionId": "opcao-equilibrada",
                                  "timedOut": false
                                }
                                """))
                .andExpect(status().isPreconditionRequired());
    }

    @Test
    void nonHealthAttemptRemainsUnaffected() throws Exception {
        disableHealthVertical();
        MvcResult created = createAttempt("candidate-non-health-consent");
        String publicToken = tokenFromResponse(created.getResponse().getContentAsString());

        mockMvc.perform(get("/candidate/attempts/" + publicToken + "/health-consent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthVertical").value(false))
                .andExpect(jsonPath("$.required").value(false));
        mockMvc.perform(get("/candidate/attempts/" + publicToken))
                .andExpect(status().isOk());
    }

    private MvcResult createAttempt(String documentId) throws Exception {
        long numericDocumentId = Integer.toUnsignedLong(documentId.hashCode()) + 1L;
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
                                  "job_id": 100,
                                  "callback_url": "https://cliente.gupy.io/candidate-return",
                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                                  "candidate_type": "external",
                                  "previous_result": null
                                }
                                """.formatted(numericDocumentId)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String tokenFromResponse(String responseBody) {
        String testUrl = JsonPath.read(responseBody, "$.test_url");
        return testUrl.substring(testUrl.lastIndexOf('/') + 1);
    }
}
