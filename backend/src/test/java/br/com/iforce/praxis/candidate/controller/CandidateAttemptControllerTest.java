package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CandidateAttemptControllerTest {

    private static final String AUTHORIZATION = "Bearer dev-company-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CandidateAttemptRepository candidateAttemptRepository;

    @Test
    void getCandidateAttemptReturnsCurrentNodeWithoutScoringInternals() throws Exception {
        String attemptId = createAttempt("candidate-public-payload");

        mockMvc.perform(get("/candidate/attempts/" + attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.status").value("notStarted"))
                .andExpect(jsonPath("$.currentNode.id").value("turno-1"))
                .andExpect(jsonPath("$.currentNode.options[0].id").value(startsWith("opcao-")))
                .andExpect(jsonPath("$.currentNode.options[0].competencyScores").doesNotExist())
                .andExpect(jsonPath("$.currentNode.options[0].critical").doesNotExist())
                .andExpect(jsonPath("$.currentNode.options[0].auditNote").doesNotExist());
    }

    @Test
    void submitAnswerCompletesAttemptAndUpdatesGupyResult() throws Exception {
        MvcResult createResult = createAttemptResult("candidate-balanced-answer");
        String responseBody = createResult.getResponse().getContentAsString();
        String attemptId = JsonPath.read(responseBody, "$.attemptId");
        String resultId = JsonPath.read(responseBody, "$.testResultId");

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "optionId": "opcao-equilibrada"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.currentNode").doesNotExist());

        mockMvc.perform(get("/test/result/" + resultId).header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.score").value(85))
                .andExpect(jsonPath("$.decision").value("recommendInterview"))
                .andExpect(jsonPath("$.humanReviewRequired").value(false))
                .andExpect(jsonPath("$.companyResultString").value(org.hamcrest.Matchers.containsString("Score geral: 85/100")));

        mockMvc.perform(get("/api/v1/audit/candidate-attempts/" + attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("attemptCreated"))
                .andExpect(jsonPath("$[1].eventType").value("answerSubmitted"))
                .andExpect(jsonPath("$[2].eventType").value("attemptCompleted"));
    }

    @Test
    void submitSameAnswerTwiceIsIdempotent() throws Exception {
        String attemptId = createAttempt("candidate-retry");
        String answerPayload = """
                {
                  "nodeId": "turno-1",
                  "optionId": "opcao-equilibrada"
                }
                """;

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(false));

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void criticalAnswerRequiresHumanReviewWithoutZeroScore() throws Exception {
        MvcResult createResult = createAttemptResult("candidate-critical-answer");
        String responseBody = createResult.getResponse().getContentAsString();
        String attemptId = JsonPath.read(responseBody, "$.attemptId");
        String resultId = JsonPath.read(responseBody, "$.testResultId");

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "optionId": "opcao-promete-estorno"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));

        mockMvc.perform(get("/test/result/" + resultId).header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(46))
                .andExpect(jsonPath("$.decision").value("reviewRequired"))
                .andExpect(jsonPath("$.humanReviewRequired").value(true))
                .andExpect(jsonPath("$.companyResultString").value(org.hamcrest.Matchers.containsString("Revisão humana obrigatória")));
    }

    @Test
    void createCandidateAttemptPinsPublishedSimulationVersion() throws Exception {
        String attemptId = createAttempt("candidate-pinned-version");

        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository.findById(attemptId)
                .orElseThrow();

        assertThat(candidateAttemptEntity.getSimulationId()).isEqualTo("sim-atendimento-caos");
        assertThat(candidateAttemptEntity.getSimulationVersionId()).isEqualTo(1L);
        assertThat(candidateAttemptEntity.getSimulationVersionNumber()).isEqualTo(1);
    }

    private String createAttempt(String documentId) throws Exception {
        MvcResult createResult = createAttemptResult(documentId);
        String responseBody = createResult.getResponse().getContentAsString();
        return JsonPath.read(responseBody, "$.attemptId");
    }

    private MvcResult createAttemptResult(String documentId) throws Exception {
        return mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """.formatted(documentId)))
                .andExpect(status().isOk())
                .andReturn();
    }
}
