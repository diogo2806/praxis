package br.com.iforce.praxis.journey.service;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.audit.service.AuditMetadata;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import br.com.iforce.praxis.gupy.service.CandidateAttemptService;

import br.com.iforce.praxis.gupy.service.SimulationCatalogService;

import br.com.iforce.praxis.journey.dto.AssessmentJourneyAttemptResponse;

import br.com.iforce.praxis.journey.dto.CreateJourneyAttemptRequest;

import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;

import br.com.iforce.praxis.journey.model.AssessmentJourneyStatus;

import br.com.iforce.praxis.journey.model.AssessmentJourneyStepStatus;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptEntity;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptStepEntity;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyEntity;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyStepEntity;

import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyAttemptRepository;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.lenient;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AssessmentJourneyAttemptServiceTest {

    @Mock private AssessmentJourneyAttemptRepository attemptRepository;
    @Mock private AssessmentJourneyService journeyService;
    @Mock private CandidateAttemptService candidateAttemptService;
    @Mock private CandidateAttemptRepository candidateAttemptRepository;
    @Mock private SimulationCatalogService simulationCatalogService;
    @Mock private CurrentEmpresaService currentEmpresaService;
    @Mock private AuditEventService auditEventService;

    private AssessmentJourneyAttemptService service;

    @BeforeEach
    void setUp() {
        service = new AssessmentJourneyAttemptService(
                attemptRepository,
                journeyService,
                candidateAttemptService,
                candidateAttemptRepository,
                simulationCatalogService,
                currentEmpresaService,
                auditEventService,
                new AuditMetadata(new com.fasterxml.jackson.databind.ObjectMapper())
        );
        lenient().when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        lenient().when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(simulationCatalogService.findPublishedById(anyString(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    void createAttemptRequiresPublishedJourney() {
        AssessmentJourneyEntity journey = journey(AssessmentJourneyStatus.DRAFT);
        when(journeyService.findJourneyForEmpresa("empresa-1", "j1")).thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> service.createAttempt(
                new CreateJourneyAttemptRequest("j1", "Maria", "maria@example.com", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("publicada");
    }

    @Test
    void createAttemptMaterializesSequenceSteps() {
        AssessmentJourneyEntity journey = journey(AssessmentJourneyStatus.PUBLISHED);
        journey.getSteps().add(journeyStep(journey, 1L, "sim-a", "principal", 0));
        journey.getSteps().add(journeyStep(journey, 2L, "sim-b", "principal", 1));
        when(journeyService.findJourneyForEmpresa("empresa-1", "j1")).thenReturn(Optional.of(journey));

        AssessmentJourneyAttemptResponse response = service.createAttempt(
                new CreateJourneyAttemptRequest("j1", "Maria", "maria@example.com", null));

        assertThat(response.status()).isEqualTo(AssessmentJourneyAttemptStatus.CREATED);
        assertThat(response.sequenceKey()).isEqualTo("principal");
        assertThat(response.steps()).hasSize(2);
        verify(auditEventService).appendAssessmentJourneyAttemptEvent(
                eq("empresa-1"), anyString(),
                eq(AuditEventType.ASSESSMENT_JOURNEY_ATTEMPT_CREATED), anyString(), anyString());
    }

    @Test
    void startStepRejectsWhenPreviousRequiredStepNotCompleted() {
        AssessmentJourneyAttemptEntity attempt = attempt(AssessmentJourneyAttemptStatus.IN_PROGRESS);
        attempt.getSteps().add(attemptStep(attempt, 10L, 0, true, AssessmentJourneyStepStatus.PENDING));
        AssessmentJourneyAttemptStepEntity second = attemptStep(attempt, 20L, 1, true, AssessmentJourneyStepStatus.PENDING);
        attempt.getSteps().add(second);
        when(attemptRepository.findByEmpresaIdAndId("empresa-1", "jatt_1")).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.startStep("jatt_1", 20L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("anterior");
    }

    @Test
    void startStepGeneratesCandidateAttemptAndLink() {
        AssessmentJourneyAttemptEntity attempt = attempt(AssessmentJourneyAttemptStatus.CREATED);
        attempt.getSteps().add(attemptStep(attempt, 10L, 0, true, AssessmentJourneyStepStatus.PENDING));
        when(attemptRepository.findByEmpresaIdAndId("empresa-1", "jatt_1")).thenReturn(Optional.of(attempt));
        when(candidateAttemptService.createCompanyLink(any()))
                .thenReturn(new CreateCandidateLinkResponse("att_1", "https://x/candidato/tok", "Teste"));
        lenient().when(journeyService.findJourneyForEmpresa(anyString(), anyString())).thenReturn(Optional.empty());

        AssessmentJourneyAttemptResponse response = service.startStep("jatt_1", 10L);

        assertThat(attempt.getStatus()).isEqualTo(AssessmentJourneyAttemptStatus.IN_PROGRESS);
        assertThat(response.steps().get(0).status()).isEqualTo(AssessmentJourneyStepStatus.IN_PROGRESS);
        assertThat(response.steps().get(0).candidateAttemptId()).isEqualTo("att_1");
        assertThat(response.steps().get(0).candidateUrl()).isEqualTo("https://x/candidato/tok");
        verify(auditEventService).appendAssessmentJourneyAttemptEvent(
                eq("empresa-1"), eq("jatt_1"),
                eq(AuditEventType.ASSESSMENT_JOURNEY_STEP_STARTED), anyString(), anyString());
    }

    @Test
    void completeStepRequiresCandidateAttemptCompleted() {
        AssessmentJourneyAttemptEntity attempt = attempt(AssessmentJourneyAttemptStatus.IN_PROGRESS);
        AssessmentJourneyAttemptStepEntity step = attemptStep(attempt, 10L, 0, true, AssessmentJourneyStepStatus.IN_PROGRESS);
        step.setCandidateAttemptId("att_1");
        attempt.getSteps().add(step);
        when(attemptRepository.findByEmpresaIdAndId("empresa-1", "jatt_1")).thenReturn(Optional.of(attempt));
        CandidateAttemptEntity candidateAttempt = new CandidateAttemptEntity();
        candidateAttempt.setStatus(AttemptStatus.IN_PROGRESS);
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", "att_1")).thenReturn(Optional.of(candidateAttempt));

        assertThatThrownBy(() -> service.completeStep("jatt_1", 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ainda não foi concluído");
    }

    @Test
    void completeStepFinishesJourneyWhenAllRequiredDone() {
        AssessmentJourneyAttemptEntity attempt = attempt(AssessmentJourneyAttemptStatus.IN_PROGRESS);
        AssessmentJourneyAttemptStepEntity step = attemptStep(attempt, 10L, 0, true, AssessmentJourneyStepStatus.IN_PROGRESS);
        step.setCandidateAttemptId("att_1");
        attempt.getSteps().add(step);
        when(attemptRepository.findByEmpresaIdAndId("empresa-1", "jatt_1")).thenReturn(Optional.of(attempt));
        CandidateAttemptEntity candidateAttempt = new CandidateAttemptEntity();
        candidateAttempt.setStatus(AttemptStatus.COMPLETED);
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", "att_1")).thenReturn(Optional.of(candidateAttempt));
        lenient().when(journeyService.findJourneyForEmpresa(anyString(), anyString())).thenReturn(Optional.empty());
        lenient().when(candidateAttemptService.candidatePageUrlFor(anyString(), anyString())).thenReturn("https://x/candidato/tok");

        AssessmentJourneyAttemptResponse response = service.completeStep("jatt_1", 10L);

        assertThat(response.status()).isEqualTo(AssessmentJourneyAttemptStatus.COMPLETED);
        assertThat(response.completedAt()).isNotNull();
        assertThat(response.steps().get(0).status()).isEqualTo(AssessmentJourneyStepStatus.COMPLETED);
        verify(auditEventService).appendAssessmentJourneyAttemptEvent(
                eq("empresa-1"), eq("jatt_1"),
                eq(AuditEventType.ASSESSMENT_JOURNEY_ATTEMPT_COMPLETED), anyString(), anyString());
    }

    private AssessmentJourneyEntity journey(AssessmentJourneyStatus status) {
        AssessmentJourneyEntity journey = new AssessmentJourneyEntity();
        journey.setId("j1");
        journey.setEmpresaId("empresa-1");
        journey.setName("Jornada");
        journey.setStatus(status);
        journey.setCreatedAt(Instant.now());
        journey.setUpdatedAt(Instant.now());
        return journey;
    }

    private AssessmentJourneyStepEntity journeyStep(AssessmentJourneyEntity journey, Long id, String simId, String seq, int order) {
        AssessmentJourneyStepEntity step = new AssessmentJourneyStepEntity();
        step.setId(id);
        step.setEmpresaId("empresa-1");
        step.setJourney(journey);
        step.setSimulationId(simId);
        step.setSimulationVersionNumber(1);
        step.setSequenceKey(seq);
        step.setOrderIndex(order);
        step.setRequired(true);
        step.setCreatedAt(Instant.now());
        return step;
    }

    private AssessmentJourneyAttemptEntity attempt(AssessmentJourneyAttemptStatus status) {
        AssessmentJourneyAttemptEntity attempt = new AssessmentJourneyAttemptEntity();
        attempt.setId("jatt_1");
        attempt.setEmpresaId("empresa-1");
        attempt.setJourneyId("j1");
        attempt.setCandidateName("Maria");
        attempt.setCandidateEmail("maria@example.com");
        attempt.setSequenceKey("principal");
        attempt.setStatus(status);
        attempt.setCreatedAt(Instant.now());
        return attempt;
    }

    private AssessmentJourneyAttemptStepEntity attemptStep(
            AssessmentJourneyAttemptEntity attempt, Long id, int order, boolean required, AssessmentJourneyStepStatus status) {
        AssessmentJourneyAttemptStepEntity step = new AssessmentJourneyAttemptStepEntity();
        step.setId(id);
        step.setEmpresaId("empresa-1");
        step.setJourneyAttempt(attempt);
        step.setJourneyStepId(id);
        step.setSimulationId("sim-" + id);
        step.setSimulationVersionNumber(1);
        step.setOrderIndex(order);
        step.setRequired(required);
        step.setStatus(status);
        step.setCreatedAt(Instant.now());
        return step;
    }
}
