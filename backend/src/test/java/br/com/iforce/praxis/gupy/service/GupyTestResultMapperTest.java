package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.gupy.model.ResultTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GupyTestResultMapperTest {

    @Mock
    private PraxisProperties praxisProperties;

    @Mock
    private JwtService jwtService;

    private GupyTestResultMapper mapper;
    private PublishedSimulation simulation;

    @BeforeEach
    void setUp() {
        mapper = new GupyTestResultMapper(praxisProperties, jwtService);
        simulation = new PublishedSimulation(
                1L,
                1,
                "sim-1",
                "Simulação",
                "Descrição",
                List.of("Comunicação"),
                Map.of("Comunicação", 1.0),
                Map.of("Comunicação", ResultTier.MAJOR),
                "node-1",
                List.of()
        );
    }

    @Test
    void shouldNotExposeProvisionalScoresBeforeCompletion() {
        givenUrlConfiguration();
        CandidateAttempt attempt = attemptWithStatus(AttemptStatus.IN_PROGRESS, null);

        TestResultResponse response = mapper.toResponse(attempt, simulation);

        assertThat(response.status()).isEqualTo("paused");
        assertThat(response.results()).isEmpty();
    }

    @Test
    void shouldExposeScoresOnlyAfterRealCompletion() {
        givenUrlConfiguration();
        CandidateAttempt attempt = attemptWithStatus(AttemptStatus.COMPLETED, Instant.parse("2026-07-15T12:00:00Z"));

        TestResultResponse response = mapper.toResponse(attempt, simulation);

        assertThat(response.status()).isEqualTo("done");
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().score()).isEqualTo(87);
    }

    @Test
    void shouldRejectAbandonedAttemptAsCompletedResult() {
        assertThatThrownBy(() -> mapper.toResponse(attemptWithStatus(AttemptStatus.ABANDONED, Instant.now()), simulation))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não possuem resultado final válido");
    }

    @Test
    void shouldRejectExpiredAttemptAsCompletedResult() {
        assertThatThrownBy(() -> mapper.toResponse(attemptWithStatus(AttemptStatus.EXPIRED, Instant.now()), simulation))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não possuem resultado final válido");
    }

    private void givenUrlConfiguration() {
        when(praxisProperties.publicBaseUrl()).thenReturn("https://api.praxis.test");
        when(praxisProperties.candidatePageBaseUrl()).thenReturn("https://praxis.test");
        when(praxisProperties.attemptLinkTtlHours()).thenReturn(24L);
        when(jwtService.generateCandidateAttemptToken("empresa-1", "attempt-1", 24L)).thenReturn("token");
    }

    private CandidateAttempt attemptWithStatus(AttemptStatus status, Instant finishedAt) {
        return CandidateAttempt.builder()
                .id("attempt-1")
                .resultId("result-1")
                .empresaId("empresa-1")
                .companyId("company-1")
                .simulationId("sim-1")
                .simulationVersionId(1L)
                .simulationVersionNumber(1)
                .idempotencyKey("key")
                .candidateName("Candidato")
                .candidateEmail("candidato@example.com")
                .status(status)
                .score(status == AttemptStatus.COMPLETED ? 87 : null)
                .results(List.of(new ResultItem("Comunicação", 87, ResultTier.MAJOR)))
                .answersByNodeId(Map.of())
                .servedAtByNodeId(Map.of())
                .companyResultString("Resultado")
                .createdAt(Instant.parse("2026-07-15T11:00:00Z"))
                .finishedAt(finishedAt)
                .build();
    }
}
