package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class CandidateAttemptControllerTest {

    private static final String AUTHORIZATION = "Bearer empresa1-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CandidateAttemptRepository candidateAttemptRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void getCandidateAttemptRejectsInvalidAttemptToken() throws Exception {
        mockMvc.perform(get("/candidate/attempts/not-a-valid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitAnswerRejectsInvalidAttemptToken() throws Exception {
        mockMvc.perform(post("/candidate/attempts/not-a-valid-token/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "optionId": "opcao-equilibrada"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCandidateAttemptReturnsCurrentNodeWithoutScoringInternals() throws Exception {
        MvcResult createResult = createAttemptResult("candidate-public-payload");
        String responseBody = createResult.getResponse().getContentAsString();
        String attemptId = attemptIdFromResponse(responseBody);
        String publicToken = tokenFromResponse(responseBody);

        mockMvc.perform(get("/candidate/attempts/" + publicToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participacaoId").value(attemptId))
                .andExpect(jsonPath("$.status").value("em_andamento"))
                .andExpect(jsonPath("$.progresso.passoAtual").value(1))
                .andExpect(jsonPath("$.progresso.passosEstimados").value(1))
                .andExpect(jsonPath("$.progresso.percentual").value(100))
                .andExpect(jsonPath("$.etapaAtual.descricao").exists())
                .andExpect(jsonPath("$.etapaAtual.alternativas[0].id").value("A"))
                .andExpect(jsonPath("$.etapaAtual.alternativas[0].competencyScores").doesNotExist())
                .andExpect(jsonPath("$.etapaAtual.alternativas[0].critical").doesNotExist())
                .andExpect(jsonPath("$.etapaAtual.alternativas[0].auditNote").doesNotExist())
                .andExpect(jsonPath("$.etapaAtual.id").value("turno-1"));

        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository.findById(attemptId)
                .orElseThrow();

        assertThat(candidateAttemptEntity.getStartedAt()).isNotNull();
        assertThat(candidateAttemptEntity.getFinishedAt()).isNull();

        List<OutboxEventEntity> startedEvents = outboxEventRepository
                .findByEmpresaIdAndEventTypeOrderByCreatedAtDesc("empresa-1", "ATTEMPT_STARTED");
        assertThat(startedEvents).hasSize(1);
        assertThat(startedEvents.getFirst().getAggregateId()).isEqualTo(attemptId);
        assertThat(startedEvents.getFirst().getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.PENDING);
        assertThat(startedEvents.getFirst().getPayload()).contains("\"event_type\":\"ATTEMPT_STARTED\"");
        assertThat(startedEvents.getFirst().getPayload()).contains("\"webhookUrl\":\"https://cliente.gupy.io/result-webhook\"");
    }

    @Test
    void submitAnswerCompletesAttemptAndUpdatesGupyResult() throws Exception {
        MvcResult createResult = createAttemptResult("candidate-balanced-answer");
        String responseBody = createResult.getResponse().getContentAsString();
        String attemptId = attemptIdFromResponse(responseBody);
        String resultId = JsonPath.read(responseBody, "$.test_result_id");

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "optionId": "opcao-equilibrada"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("concluida"))
                .andExpect(jsonPath("$.finalizado").value(true))
                .andExpect(jsonPath("$.redirectUrl").value("https://cliente.gupy.io/candidate-return"))
                .andExpect(jsonPath("$.progresso.passoAtual").value(1))
                .andExpect(jsonPath("$.progresso.passosEstimados").value(1))
                .andExpect(jsonPath("$.progresso.percentual").value(100))
                .andExpect(jsonPath("$.etapaAtual").doesNotExist());

        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", AUTHORIZATION)
                        .param("company_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.results[?(@.title=='Empatia')].score").value(org.hamcrest.Matchers.hasItem(100)))
                .andExpect(jsonPath("$.company_result_string").value(org.hamcrest.Matchers.containsString("Pontuação geral: 100/100")))
                .andExpect(jsonPath("$.company_result_string").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Revisao humana obrigatoria"))));

        mockMvc.perform(get("/api/v1/audit/candidate-attempts/" + attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("attemptCreated"))
                .andExpect(jsonPath("$[1].eventType").value("attemptStarted"))
                .andExpect(jsonPath("$[2].eventType").value("answerSubmitted"))
                .andExpect(jsonPath("$[3].eventType").value("attemptCompleted"));

        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository.findById(attemptId)
                .orElseThrow();

        assertThat(candidateAttemptEntity.getStartedAt()).isNotNull();
        assertThat(candidateAttemptEntity.getFinishedAt()).isNotNull();
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
                .andExpect(jsonPath("$.repetida").value(false));

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repetida").value(true))
                .andExpect(jsonPath("$.finalizado").value(true));
    }

    @Test
    void criticalAnswerRequiresHumanReviewWithoutZeroScore() throws Exception {
        MvcResult createResult = createAttemptResult("candidate-critical-answer");
        String responseBody = createResult.getResponse().getContentAsString();
        String attemptId = attemptIdFromResponse(responseBody);
        String resultId = JsonPath.read(responseBody, "$.test_result_id");

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "optionId": "opcao-promete-estorno"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("concluida"));

        // Enquanto a revisão humana não é concluída, o resultado fica retido: status "paused",
        // itens não liberados e mensagem de aguardo. A liberação ocorre depois, pela revisão.
        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", AUTHORIZATION)
                        .param("company_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paused"))
                .andExpect(jsonPath("$.results").isEmpty())
                .andExpect(jsonPath("$.company_result_string")
                        .value(org.hamcrest.Matchers.containsString("aguardando revisão humana")));

        CandidateAttemptEntity reviewedAttempt = candidateAttemptRepository.findById(attemptId).orElseThrow();
        assertThat(reviewedAttempt.isHumanReviewRequired()).isTrue();
        assertThat(reviewedAttempt.getScore()).isNotNull().isPositive();
    }

    @Test
    void timeoutAnswerCompletesAttemptScoringTheTurnAsLevelZero() throws Exception {
        MvcResult createResult = createAttemptResult("candidate-timeout");
        String responseBody = createResult.getResponse().getContentAsString();
        String attemptId = attemptIdFromResponse(responseBody);
        String resultId = JsonPath.read(responseBody, "$.test_result_id");

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "timedOut": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("concluida"))
                .andExpect(jsonPath("$.finalizado").value(true))
                .andExpect(jsonPath("$.etapaAtual").doesNotExist());

        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", AUTHORIZATION)
                        .param("company_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.results[?(@.title=='Empatia')].score").value(org.hamcrest.Matchers.hasItem(0)))
                .andExpect(jsonPath("$.company_result_string").value(org.hamcrest.Matchers.containsString("Pontuação geral: 0/100")));

        mockMvc.perform(get("/api/v1/audit/candidate-attempts/" + attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("attemptCreated"))
                .andExpect(jsonPath("$[1].eventType").value("attemptStarted"))
                .andExpect(jsonPath("$[2].eventType").value("answerSubmitted"))
                .andExpect(jsonPath("$[3].eventType").value("attemptCompleted"));
    }

    @Test
    void timeoutAnswerWithFallbackAdvancesToNextNodeWithoutCompletingAttempt() throws Exception {
        MvcResult createResult = createAttemptResult("candidate-timeout-fallback", "sim-timeout-fallback");
        String responseBody = createResult.getResponse().getContentAsString();
        String attemptId = attemptIdFromResponse(responseBody);
        String resultId = JsonPath.read(responseBody, "$.test_result_id");

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "timedOut": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("em_andamento"))
                .andExpect(jsonPath("$.finalizado").value(false))
                .andExpect(jsonPath("$.progresso.passoAtual").value(2))
                .andExpect(jsonPath("$.progresso.passosEstimados").value(2))
                .andExpect(jsonPath("$.progresso.percentual").value(100))
                .andExpect(jsonPath("$.etapaAtual.descricao").value("Segundo turno terminal."))
                .andExpect(jsonPath("$.etapaAtual.alternativas[0].id").value("A"));

        CandidateAttemptEntity inProgressAttempt = candidateAttemptRepository.findById(attemptId)
                .orElseThrow();
        assertThat(inProgressAttempt.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(inProgressAttempt.getFinishedAt()).isNull();

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-2",
                                  "optionId": "n2-melhor"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("concluida"))
                .andExpect(jsonPath("$.finalizado").value(true))
                .andExpect(jsonPath("$.etapaAtual").doesNotExist());

        CandidateAttemptEntity completedAttempt = candidateAttemptRepository.findById(attemptId)
                .orElseThrow();
        assertThat(completedAttempt.getAnswers())
                .anySatisfy(answer -> assertThat(answer.isTimedOut()).isTrue());

        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", AUTHORIZATION)
                        .param("company_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.other_informations").doesNotExist())
                .andExpect(jsonPath("$.reliabilityLevel").doesNotExist())
                .andExpect(jsonPath("$.results[?(@.title=='Empatia')].score").value(org.hamcrest.Matchers.hasItem(50)));
    }

    @Test
    void delayedAnswerInsideGracePeriodUsesClientTimestampAndCompletesAttempt() throws Exception {
        String attemptId = createStartedAttempt("candidate-delayed-inside-grace");
        Instant startedAt = Instant.now().minusSeconds(50);
        updateStartedAt(attemptId, startedAt);

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "nodeNumber": 1,
                                  "optionId": "opcao-equilibrada",
                                  "answeredAt": "%s"
                                }
                                """.formatted(startedAt.plusSeconds(40))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("concluida"))
                .andExpect(jsonPath("$.finalizado").value(true));

        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", attemptId)
                .orElseThrow();
        assertThat(candidateAttemptEntity.getAnswers()).hasSize(1);
        assertThat(Duration.between(
                startedAt.plusSeconds(40).truncatedTo(ChronoUnit.MICROS),
                candidateAttemptEntity.getAnswers().iterator().next().getAnsweredAt()
        ).abs()).isLessThanOrEqualTo(Duration.of(1, ChronoUnit.MICROS));
    }

    @Test
    void delayedAnswerAfterServerGraceUsesTrustedClientTimestampAndCompletesAttempt() throws Exception {
        String attemptId = createStartedAttempt("candidate-delayed-after-server-grace");
        Instant startedAt = Instant.now().minusSeconds(70);
        updateStartedAt(attemptId, startedAt);

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "nodeNumber": 1,
                                  "optionId": "opcao-equilibrada",
                                  "answeredAt": "%s"
                                }
                                """.formatted(startedAt.plusSeconds(40))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("concluida"))
                .andExpect(jsonPath("$.finalizado").value(true));

        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", attemptId)
                .orElseThrow();
        assertThat(candidateAttemptEntity.getAnswers()).hasSize(1);
        assertThat(Duration.between(
                startedAt.plusSeconds(40).truncatedTo(ChronoUnit.MICROS),
                candidateAttemptEntity.getAnswers().iterator().next().getAnsweredAt()
        ).abs()).isLessThanOrEqualTo(Duration.of(1, ChronoUnit.MICROS));
    }

    @Test
    void answerGeneratedAfterFrontendLimitIsRejectedEvenInsideGracePeriod() throws Exception {
        String attemptId = createStartedAttempt("candidate-late-after-frontend-limit");
        Instant startedAt = Instant.now().minusSeconds(50);
        updateStartedAt(attemptId, startedAt);

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "nodeNumber": 1,
                                  "optionId": "opcao-equilibrada",
                                  "answeredAt": "%s"
                                }
                                """.formatted(startedAt.plusSeconds(46))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Tempo da etapa esgotado."));
    }

    @Test
    void timedOutAnswerAfterFrontendLimitRoutesToTimeoutAlternative() throws Exception {
        String attemptId = createStartedAttempt("candidate-timeout-after-frontend-limit");
        Instant startedAt = Instant.now().minusSeconds(60);
        updateStartedAt(attemptId, startedAt);

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "nodeNumber": 1,
                                  "timedOut": true,
                                  "answeredAt": "%s"
                                }
                                """.formatted(startedAt.plusSeconds(50))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("concluida"));

        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", attemptId)
                .orElseThrow();
        assertThat(candidateAttemptEntity.getAnswers()).hasSize(1);
        assertThat(candidateAttemptEntity.getAnswers().iterator().next().isTimedOut()).isTrue();
    }

    @Test
    void lateRealAnswerReconcilesPreviousAutomaticTimeoutForSameStep() throws Exception {
        String attemptId = createStartedAttempt("candidate-reconciles-timeout");
        String resultId = candidateAttemptRepository.findById(attemptId).orElseThrow().getResultId();
        Instant startedAt = Instant.now().minusSeconds(40);
        updateStartedAt(attemptId, startedAt);

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "nodeNumber": 1,
                                  "timedOut": true,
                                  "answeredAt": "%s"
                                }
                                """.formatted(startedAt.plusSeconds(40))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("concluida"));

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeNumber": 1,
                                  "optionId": "opcao-equilibrada",
                                  "answeredAt": "%s"
                                }
                                """.formatted(startedAt.plusSeconds(39))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("concluida"))
                .andExpect(jsonPath("$.finalizado").value(true));

        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", AUTHORIZATION)
                        .param("company_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[?(@.title=='Empatia')].score").value(org.hamcrest.Matchers.hasItem(100)))
                .andExpect(jsonPath("$.company_result_string").value(org.hamcrest.Matchers.containsString("Pontuação geral: 100/100")));
    }

    @Test
    void resultExposesPolicyAdherenceAsMinorAndOthersAsMajor() throws Exception {
        MvcResult createResult = createAttemptResult("candidate-tier-check");
        String responseBody = createResult.getResponse().getContentAsString();
        String attemptId = attemptIdFromResponse(responseBody);
        String resultId = JsonPath.read(responseBody, "$.test_result_id");

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "optionId": "opcao-equilibrada"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test/result/" + resultId)
                        .header("Authorization", AUTHORIZATION)
                        .param("company_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[?(@.title=='Aderencia a politica')].tier").value(org.hamcrest.Matchers.hasItem("minor")))
                .andExpect(jsonPath("$.results[?(@.title=='Empatia')].tier").value(org.hamcrest.Matchers.hasItem("major")))
                .andExpect(jsonPath("$.results[?(@.title=='Resolucao de conflito')].tier").value(org.hamcrest.Matchers.hasItem("major")));
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

    @Test
    void createCompanyLinkReturnsCandidatePageUrl() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/candidate-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "simulationId": "sim-atendimento-caos",
                                  "candidateName": "Thiago Souza",
                                  "candidateEmail": "thiago.company-link@example.com",
                                  "applicationCycleId": "test-company-link-thiago-20260716"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidateUrl").value(startsWith("http://localhost:8080/candidato/")))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String attemptId = JsonPath.read(responseBody, "$.attemptId");
        String candidateUrl = JsonPath.read(responseBody, "$.candidateUrl");

        assertThat(attemptId).startsWith("att_");
        assertThat(candidateUrl).contains("/candidato/");
        assertThat(candidateUrl).doesNotEndWith("/" + attemptId);
    }

    @Test
    void listCompanyLinksReturnsExistingCandidateAttempts() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/candidate-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "simulationId": "sim-atendimento-caos",
                                  "candidateName": "Maria Souza",
                                  "candidateEmail": "maria.company-link@example.com",
                                  "applicationCycleId": "test-company-link-maria-20260716"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String attemptId = JsonPath.read(responseBody, "$.attemptId");

        mockMvc.perform(get("/api/v1/candidate-links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attemptId").value(attemptId))
                .andExpect(jsonPath("$[0].candidateName").value("Maria Souza"))
                .andExpect(jsonPath("$[0].candidateEmail").value("maria.company-link@example.com"))
                .andExpect(jsonPath("$[0].simulationId").value("sim-atendimento-caos"))
                .andExpect(jsonPath("$[0].simulationName").value("Cenario Seed de Teste"))
                .andExpect(jsonPath("$[0].status").value("notStarted"))
                .andExpect(jsonPath("$[0].candidateUrl").value(startsWith("http://localhost:8080/candidato/")))
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    void expiredNotStartedAttemptReturnsExpiredWithoutStarting() throws Exception {
        String attemptId = createAttempt("candidate-expired-before-start");
        // created_at é imutável (gatilho de auditoria universal reverte updates), então o
        // backdate para simular um convite criado há 8 dias é feito via SQL com o gatilho
        // temporariamente desabilitado.
        jdbcTemplate.execute("ALTER TABLE candidate_attempts DISABLE TRIGGER USER");
        jdbcTemplate.update(
                "UPDATE candidate_attempts SET created_at = CURRENT_TIMESTAMP - INTERVAL '8 days' WHERE id = ?",
                attemptId);
        jdbcTemplate.execute("ALTER TABLE candidate_attempts ENABLE TRIGGER USER");

        mockMvc.perform(get("/candidate/attempts/" + attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("expirada"))
                .andExpect(jsonPath("$.finalizado").value(false))
                .andExpect(jsonPath("$.etapaAtual").doesNotExist());

        CandidateAttemptEntity expiredAttemptEntity = candidateAttemptRepository.findById(attemptId)
                .orElseThrow();
        assertThat(expiredAttemptEntity.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(expiredAttemptEntity.getStartedAt()).isNull();
        assertThat(expiredAttemptEntity.getFinishedAt()).isNotNull();

        mockMvc.perform(get("/api/v1/audit/candidate-attempts/" + attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("attemptCreated"))
                .andExpect(jsonPath("$[1].eventType").value("attemptExpired"));
    }

    @Test
    void expiredRunningAttemptIsMarkedAbandonedAndBlocksAnswerSubmission() throws Exception {
        String attemptId = createAttempt("candidate-abandoned-running");
        mockMvc.perform(get("/candidate/attempts/" + attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("em_andamento"));

        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository.findById(attemptId)
                .orElseThrow();
        candidateAttemptEntity.setStartedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        candidateAttemptRepository.save(candidateAttemptEntity);

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeId": "turno-1",
                                  "optionId": "opcao-equilibrada"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Tentativa expirada ou abandonada."));

        CandidateAttemptEntity abandonedAttemptEntity = candidateAttemptRepository.findById(attemptId)
                .orElseThrow();
        assertThat(abandonedAttemptEntity.getStatus()).isEqualTo(AttemptStatus.ABANDONED);
        assertThat(abandonedAttemptEntity.getFinishedAt()).isNotNull();

        List<OutboxEventEntity> abandonedEvents = outboxEventRepository
                .findByEmpresaIdAndEventTypeOrderByCreatedAtDesc("empresa-1", "ATTEMPT_ABANDONED");
        assertThat(abandonedEvents).hasSize(1);
        assertThat(abandonedEvents.getFirst().getAggregateId()).isEqualTo(attemptId);
        assertThat(abandonedEvents.getFirst().getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.PENDING);
        assertThat(abandonedEvents.getFirst().getPayload()).contains("\"event_type\":\"ATTEMPT_ABANDONED\"");
    }

    @Test
    void dataSubjectRequestIsRecordedOnAuditTrail() throws Exception {
        String attemptId = createAttempt("candidate-data-request");

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/data-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestType": "anonymizationDeletion",
                                  "contact": "titular@example.com",
                                  "details": "Quero excluir meus dados."
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/audit/candidate-attempts/" + attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("attemptCreated"))
                .andExpect(jsonPath("$[1].eventType").value("dataSubjectRequest"))
                .andExpect(jsonPath("$[1].metadata").value(org.hamcrest.Matchers.containsString("anonymizationDeletion")));
    }

    @Test
    void dataSubjectRequestRejectsMissingRequestType() throws Exception {
        String attemptId = createAttempt("candidate-data-request-invalid");

        mockMvc.perform(post("/candidate/attempts/" + attemptId + "/data-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private String createAttempt(String documentId) throws Exception {
        MvcResult createResult = createAttemptResult(documentId);
        String responseBody = createResult.getResponse().getContentAsString();
        return attemptIdFromResponse(responseBody);
    }

    private String createStartedAttempt(String documentId) throws Exception {
        String attemptId = createAttempt(documentId);
        mockMvc.perform(get("/candidate/attempts/" + attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("em_andamento"));
        return attemptId;
    }

    private void updateStartedAt(String attemptId, Instant startedAt) {
        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository.findById(attemptId)
                .orElseThrow();
        candidateAttemptEntity.setStartedAt(startedAt);
        candidateAttemptRepository.save(candidateAttemptEntity);
        // O prazo de cada etapa é medido a partir do momento em que o nó foi servido
        // (attempt_node_serves.served_at), gravado no GET que inicia a participação. Para
        // simular uma etapa iniciada no passado, também retrocedemos o served_at do nó.
        jdbcTemplate.update(
                "UPDATE attempt_node_serves SET served_at = ? WHERE candidate_attempt_id = ?",
                startedAt.atOffset(java.time.ZoneOffset.UTC),
                attemptId
        );
    }

    private MvcResult createAttemptResult(String documentId) throws Exception {
        return createAttemptResult(documentId, "sim-atendimento-caos");
    }

    private MvcResult createAttemptResult(String documentId, String simulationId) throws Exception {
        long numericDocumentId = Integer.toUnsignedLong(documentId.hashCode()) + 1L;
        return mockMvc.perform(post("/test/candidate")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company_id": 1,
                                  "document_id": %d,
                                  "test_id": "%s",
                                  "name": "Thiago Souza",
                                  "email": "thiago@example.com",
                                  "job_id": 100,
                                  "callback_url": "https://cliente.gupy.io/candidate-return",
                                  "result_webhook_url": "https://cliente.gupy.io/result-webhook",
                                  "candidate_type": "external",
                                  "previous_result": null
                                }
                                """.formatted(numericDocumentId, simulationId)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String attemptIdFromResponse(String responseBody) {
        String token = tokenFromResponse(responseBody);
        try {
            return jwtService.parseCandidateAttemptToken(token).attemptId();
        } catch (RuntimeException exception) {
            return token;
        }
    }

    private String tokenFromResponse(String responseBody) {
        String testUrl = JsonPath.read(responseBody, "$.test_url");
        return testUrl.substring(testUrl.lastIndexOf('/') + 1);
    }
}
