package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.context.EmpresaContextHolder;
import br.com.iforce.praxis.auth.service.CandidateTokenWindowService;
import br.com.iforce.praxis.auth.service.CandidateTokenWindowService.CandidateTokenWindow;
import br.com.iforce.praxis.billing.service.CreditService;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.CandidateAttemptMapper;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyCandidateLinkServiceTest {

    @Mock private CandidateAttemptRepository candidateAttemptRepository;
    @Mock private SimulationCatalogService simulationCatalogService;
    @Mock private CandidateAttemptService candidateAttemptService;
    @Mock private CandidateTokenWindowService candidateTokenWindowService;
    @Mock private CreditService creditService;
    @Mock private AuditEventService auditEventService;
    @Mock private AuditMetadata auditMetadata;

    private CompanyCandidateLinkService service;
    private PublishedSimulation simulation;
    private PraxisProperties properties;
    private final Map<String, CandidateAttemptEntity> attemptsByIdempotencyKey = new HashMap<>();

    @BeforeEach
    void setUp() {
        EmpresaContextHolder.set("empresa-1");
        properties = new PraxisProperties(
                "https://api.praxis.test",
                "https://app.praxis.test",
                168,
                12,
                720,
                70,
                15,
                0.001
        );
        simulation = new PublishedSimulation(
                10L,
                3,
                "sim-java",
                "Avaliação Java",
                "Avaliação técnica",
                List.of("Conhecimento técnico"),
                Map.of("Conhecimento técnico", 1.0),
                Map.of("Conhecimento técnico", ResultTier.MAJOR),
                "inicio",
                List.of()
        );

        when(simulationCatalogService.findPublishedById("empresa-1", "sim-java"))
                .thenReturn(Optional.of(simulation));
        when(candidateAttemptRepository.findByEmpresaIdAndIdempotencyKey(eq("empresa-1"), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(
                        attemptsByIdempotencyKey.get(invocation.getArgument(1, String.class))
                ));
        when(candidateAttemptRepository.saveAndFlush(any(CandidateAttemptEntity.class)))
                .thenAnswer(invocation -> {
                    CandidateAttemptEntity entity = invocation.getArgument(0, CandidateAttemptEntity.class);
                    attemptsByIdempotencyKey.put(entity.getIdempotencyKey(), entity);
                    return entity;
                });
        when(candidateAttemptService.candidatePageUrlFor(eq("empresa-1"), anyString()))
                .thenAnswer(invocation -> "https://praxis.example/candidato/" + invocation.getArgument(1));
        when(auditMetadata.of(any(Object[].class))).thenReturn("{}");

        service = new CompanyCandidateLinkService(
                candidateAttemptRepository,
                simulationCatalogService,
                new CandidateAttemptMapper(),
                candidateAttemptService,
                candidateTokenWindowService,
                properties,
                creditService,
                auditEventService,
                auditMetadata
        );
    }

    @AfterEach
    void tearDown() {
        EmpresaContextHolder.clear();
    }

    @Test
    void sameApplicationCycleReusesOnlyEquivalentRequest() {
        CreateCandidateLinkRequest request = request("cycle-vaga-1");

        CreateCandidateLinkResponse first = service.createNewApplication(request);
        CreateCandidateLinkResponse retry = service.createNewApplication(request);

        assertThat(first.reused()).isFalse();
        assertThat(retry.reused()).isTrue();
        assertThat(retry.attemptId()).isEqualTo(first.attemptId());
        verify(candidateAttemptRepository, times(1)).saveAndFlush(any(CandidateAttemptEntity.class));
        verify(creditService, times(1)).assertCanStartNewAttempt("empresa-1");
    }

    @Test
    void differentApplicationCyclesCreateIndependentAttempts() {
        CreateCandidateLinkResponse first = service.createNewApplication(request("cycle-vaga-1"));
        CreateCandidateLinkResponse second = service.createNewApplication(request("cycle-vaga-2"));

        assertThat(second.attemptId()).isNotEqualTo(first.attemptId());
        verify(creditService, times(2)).assertCanStartNewAttempt("empresa-1");
    }

    @Test
    void resendRejectsExpiredLink() {
        CreateCandidateLinkResponse created = service.createNewApplication(request("cycle-vaga-1"));
        CandidateAttemptEntity entity = attemptsByIdempotencyKey.values().iterator().next();
        CandidateTokenWindow expired = new CandidateTokenWindow(
                Instant.now().minusSeconds(8 * 24 * 60 * 60L),
                Instant.now().minusSeconds(60)
        );
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", created.attemptId()))
                .thenReturn(Optional.of(entity));
        when(candidateTokenWindowService.currentWindow("empresa-1", created.attemptId(), 168))
                .thenReturn(expired);
        when(candidateTokenWindowService.isExpired(expired)).thenReturn(true);

        assertThatThrownBy(() -> service.resendExisting(created.attemptId()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void extendReactivatesWithoutConsumingAnotherCredit() {
        CreateCandidateLinkResponse created = service.createNewApplication(request("cycle-vaga-1"));
        CandidateAttemptEntity entity = attemptsByIdempotencyKey.values().iterator().next();
        CandidateTokenWindow expired = new CandidateTokenWindow(
                Instant.now().minusSeconds(8 * 24 * 60 * 60L),
                Instant.now().minusSeconds(60)
        );
        CandidateTokenWindow extended = new CandidateTokenWindow(
                Instant.now(),
                Instant.now().plusSeconds(7 * 24 * 60 * 60L)
        );
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", created.attemptId()))
                .thenReturn(Optional.of(entity));
        when(simulationCatalogService.findByVersionId(10L)).thenReturn(Optional.of(simulation));
        when(candidateTokenWindowService.currentWindow("empresa-1", created.attemptId(), 168))
                .thenReturn(expired);
        when(candidateTokenWindowService.isExpired(expired)).thenReturn(true);
        when(candidateTokenWindowService.extendValidity("empresa-1", created.attemptId(), 168, 7))
                .thenReturn(extended);

        CreateCandidateLinkResponse response = service.extendValidity(created.attemptId(), 7);

        assertThat(response.operation()).isEqualTo(CompanyCandidateLinkService.EXTENDED_LINK_VALIDITY);
        verify(creditService, times(1)).assertCanStartNewAttempt("empresa-1");
        verify(auditEventService).appendCandidateAttemptEvent(
                eq("empresa-1"),
                eq(created.attemptId()),
                eq(AuditEventType.CANDIDATE_LINK_EXTENDED),
                anyString(),
                anyString()
        );
    }

    @Test
    void resendDoesNotExposeAttemptFromAnotherCompany() {
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", "att-other-company"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resendExisting("att-other-company"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(creditService, never()).assertCanStartNewAttempt(anyString());
    }

    private CreateCandidateLinkRequest request(String applicationCycleId) {
        return new CreateCandidateLinkRequest(
                "sim-java",
                "Maria Silva",
                "Maria@Example.com",
                applicationCycleId,
                "Vaga Java - etapa técnica",
                null
        );
    }
}
