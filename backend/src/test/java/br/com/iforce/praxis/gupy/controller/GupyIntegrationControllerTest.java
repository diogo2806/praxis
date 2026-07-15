package br.com.iforce.praxis.gupy.controller;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
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

    @Autowired
    private CandidateAttemptRepository candidateAttemptRepository;

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
                .andExpect(jsonPath("$.payload[0].category").doesNotExist())
                .andExpect(jsonPath("$.payload[0].level").doesNotExist())
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
                        .content(validCandidateRequest(4398157001L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.test_url").value(startsWith("http://localhost:8080/candidato/")))
                .andExpect(jsonPath("$.test_result_id").value(startsWith("res_")))
                .andExpect(jsonPath("$.attemptId").doesNotExist());
    }

    @Test
    void createCandidateAttemptPersistsCallbackAndJobId() throws Exception {
        MvcResult result = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest(4398157002L, 901L)))
                .andExpect(status().isCreated())
                .andReturn();

        String resultId = JsonPath.read(result.getResponse().getContentAsString(), "$.test_result_id");
        CandidateAttemptEntity attempt = candidateAttemptRepository
                .findByEmpresaIdAndResultId("empresa-1", resultId)
                .orElseThrow();

        assertThat(attempt.getCompanyId()).isEqualTo("1");
        assertThat(attempt.getGupyJobId()).isEqualTo(901L);
        assertThat(attempt.getCallbackUrl()).isEqualTo("https://cliente.gupy.io/candidate-return");
    }

    @Test
    void createCandidateAttemptIsIdempotentByCanonicalNumericCompanyDocumentTestAndJob() throws Exception {
        MvcResult firstResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest(4398157003L)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstBody = firstResult.getResponse().getContentAsString();

        MvcResult secondResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest(4398157003L)))
                .andExpect(status().isCreated())
                .andReturn();

        String secondBody = secondResult.getResponse().getContentAsString();
        assertThat(secondBody).isEqualTo(firstBody);
    }

    @Test
    void differentJobsCreateDifferentAttemptsForSameCandidateAndTest() throws Exception {
        MvcResult first = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest(4398157004L, 100L)))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult second = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest(4398157004L, 200L)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstResultId = JsonPath.read(first.getResponse().getContentAsString(), "$.test_result_id");
        String secondResultId = JsonPath.read(second.getResponse().getContentAsString(), "$.test_result_id");
        assertThat(secondResultId).isNotEqualTo(firstResultId);
    }

    @Test
    void acceptsAbsentAndNullOptionalContractEnums() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 1,
                                  "document_id": 4398157005,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "job_id": 100,
                                  "callback_url": "https://cliente.gupy.io/candidate-return",
                                  "candidate_type": null,
                                  "previous_result": null
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 1,
                                  "document_id": 4398157006,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "job_id": 100,
                                  "callback_url": "https://cliente.gupy.io/candidate-return"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsIdentifiersThatAreNotJsonInt64Values() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": "1",
                                  "document_id": "4398157007",
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "callback_url": "https://cliente.gupy.io/candidate-return"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNonPositiveIdentifiersBeforeStartingFlow() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 0,
                                  "document_id": -1,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "callback_url": "https://cliente.gupy.io/candidate-return"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.companyId").exists())
                .andExpect(jsonPath("$.fields.documentId").exists());
    }

    @Test
    void rejectsValuesOutsideOfficialCandidateEnums() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 1,
                                  "document_id": 4398157008,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "callback_url": "https://cliente.gupy.io/candidate-return",
                                  "candidate_type": "partner",
                                  "previous_result": "none"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTestResultReturnsAttemptStatus() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCandidateRequest(4398157009L)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String resultId = JsonPath.read(responseBody, "$.test_result_id");

        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Cenario Seed de Teste"))
                .andExpect(jsonPath("$.testCode").value("sim-atendimento-caos"))
                .andExpect(jsonPath("$.providerName").value("Praxis"))
                .andExpect(jsonPath("$.status").value("notStarted"))
                .andExpect(jsonPath("$.score").doesNotExist())
                .andExpect(jsonPath("$.company_result_string").exists())
                .andExpect(jsonPath("$.result_page_url").value(containsString("/results/")))
                .andExpect(jsonPath("$.result_page_url").value(not(containsString("/test/result/"))))
                .andExpect(jsonPath("$.result_page_url").value(not(containsString("?company_id="))))
                .andExpect(content().string(containsString("\"title\":\"Empatia\"")))
                .andExpect(content().string(containsString("\"tier\":\"major\"")));
    }

    @Test
    void createCandidateAttemptRejectsCompanyIdThatDoesNotBelongToToken() throws Exception {
        mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 2,
                                  "document_id": 4398157010,
                                  "test_id": "sim-atendimento-caos",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "job_id": 100,
                                  "callback_url": "https://cliente.gupy.io/candidate-return"
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
                .andExpect(jsonPath("$.fields.candidateEmail").exists())
                .andExpect(jsonPath("$.fields.callbackUrl").exists());
    }

    private String validCandidateRequest(long documentId) {
        return validCandidateRequest(documentId, 100L);
    }

    private String validCandidateRequest(long documentId, long jobId) {
        return """
                {
                  "company_id": 1,
                  "document_id": %d,
                  "test_id": "sim-atendimento-caos",
                  "name": "Thiago Souza",
                  "email": "thiago@example.com",
                  "job_id": %d,
                  "callback_url": "https://cliente.gupy.io/candidate-return",
                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                  "candidate_type": "external",
                  "previous_result": null
                }
                """.formatted(documentId, jobId);
    }
}
