package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class CompanyCandidateLinkData14Test {

    private static final String EMAIL = "maria.data14@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CandidateAttemptRepository candidateAttemptRepository;

    @Test
    void sameApplicationCycleIsIdempotentAndAnotherCycleCreatesNewAttempt() throws Exception {
        long before = attemptsForEmail().size();

        String firstAttemptId = createAttempt("vaga-123-ciclo-1");
        String repeatedAttemptId = createAttempt("vaga-123-ciclo-1");
        String newCycleAttemptId = createAttempt("vaga-123-ciclo-2");

        assertThat(repeatedAttemptId).isEqualTo(firstAttemptId);
        assertThat(newCycleAttemptId).isNotEqualTo(firstAttemptId);
        assertThat(attemptsForEmail()).hasSize((int) before + 2);
        assertThat(candidateAttemptRepository.findById(firstAttemptId).orElseThrow().getApplicationCycleId())
                .isEqualTo("vaga-123-ciclo-1");
        assertThat(candidateAttemptRepository.findById(newCycleAttemptId).orElseThrow().getApplicationCycleId())
                .isEqualTo("vaga-123-ciclo-2");
    }

    @Test
    void resendReturnsExistingAttemptWithoutCreatingAnotherOne() throws Exception {
        String attemptId = createAttempt("vaga-456-ciclo-1");
        long beforeResend = attemptsForEmail().size();

        mockMvc.perform(post("/api/v1/candidate-links/{attemptId}/resend", attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.candidateUrl").value(startsWith("http://localhost:8080/candidato/")))
                .andExpect(jsonPath("$.simulationName").value("Cenario Seed de Teste"));

        assertThat(attemptsForEmail()).hasSize((int) beforeResend);
    }

    @Test
    void missingApplicationCycleCreatesIndependentAttemptsForLegacyClients() throws Exception {
        long before = attemptsForEmail().size();

        String firstAttemptId = createAttemptWithoutCycle();
        String secondAttemptId = createAttemptWithoutCycle();

        assertThat(secondAttemptId).isNotEqualTo(firstAttemptId);
        assertThat(attemptsForEmail()).hasSize((int) before + 2);
    }

    private String createAttempt(String applicationCycleId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/candidate-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "simulationId": "sim-atendimento-caos",
                                  "candidateName": "Maria Data14",
                                  "candidateEmail": "%s",
                                  "applicationCycleId": "%s"
                                }
                                """.formatted(EMAIL, applicationCycleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidateUrl").value(startsWith("http://localhost:8080/candidato/")))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.attemptId");
    }

    private String createAttemptWithoutCycle() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/candidate-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "simulationId": "sim-atendimento-caos",
                                  "candidateName": "Maria Data14",
                                  "candidateEmail": "%s"
                                }
                                """.formatted(EMAIL)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.attemptId");
    }

    private List<CandidateAttemptEntity> attemptsForEmail() {
        return candidateAttemptRepository
                .findByEmpresaIdOrderByCreatedAtDesc("empresa-1", PageRequest.of(0, 200))
                .stream()
                .filter(attempt -> EMAIL.equals(attempt.getCandidateEmail()))
                .toList();
    }
}
