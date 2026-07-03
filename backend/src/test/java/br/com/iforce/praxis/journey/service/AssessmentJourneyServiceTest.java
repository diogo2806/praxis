package br.com.iforce.praxis.journey.service;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.audit.service.AuditMetadata;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.auth.service.CurrentUserService;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;

import br.com.iforce.praxis.gupy.model.ResultTier;

import br.com.iforce.praxis.gupy.service.SimulationCatalogService;

import br.com.iforce.praxis.journey.dto.AddJourneyStepRequest;

import br.com.iforce.praxis.journey.dto.AssessmentJourneyDetailResponse;

import br.com.iforce.praxis.journey.dto.CreateAssessmentJourneyRequest;

import br.com.iforce.praxis.journey.model.AssessmentJourneyStatus;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyEntity;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyStepEntity;

import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyRepository;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.util.List;

import java.util.Map;

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
class AssessmentJourneyServiceTest {

    @Mock private AssessmentJourneyRepository journeyRepository;
    @Mock private SimulationCatalogService simulationCatalogService;
    @Mock private CurrentEmpresaService currentEmpresaService;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuditEventService auditEventService;

    private AssessmentJourneyService service;

    @BeforeEach
    void setUp() {
        service = new AssessmentJourneyService(
                journeyRepository,
                simulationCatalogService,
                currentEmpresaService,
                currentUserService,
                auditEventService,
                new AuditMetadata(new com.fasterxml.jackson.databind.ObjectMapper())
        );
        lenient().when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        lenient().when(currentUserService.requiredUserId()).thenReturn("u1");
        lenient().when(journeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private PublishedSimulation publishedSimulation(String id, int versionNumber) {
        return new PublishedSimulation(
                100L,
                versionNumber,
                id,
                "Teste " + id,
                "desc",
                List.of("Comunicação"),
                Map.of("Comunicação", 1.0),
                Map.of("Comunicação", ResultTier.MAJOR),
                "turno-1",
                List.of()
        );
    }

    @Test
    void createsJourneyInDraftAndAudits() {
        when(journeyRepository.existsById(anyString())).thenReturn(false);

        AssessmentJourneyDetailResponse response = service.createJourney(
                new CreateAssessmentJourneyRequest("Processo Trainee 2026", "Descrição"));

        assertThat(response.status()).isEqualTo(AssessmentJourneyStatus.DRAFT);
        assertThat(response.name()).isEqualTo("Processo Trainee 2026");
        verify(auditEventService).appendAssessmentJourneyEvent(
                eq("empresa-1"), eq("u1"), anyString(),
                eq(AuditEventType.ASSESSMENT_JOURNEY_CREATED), anyString(), anyString());
    }

    @Test
    void addStepRejectsUnpublishedSimulation() {
        AssessmentJourneyEntity journey = draftJourney();
        when(journeyRepository.findByEmpresaIdAndId("empresa-1", "j1")).thenReturn(Optional.of(journey));
        when(simulationCatalogService.findPublishedById("empresa-1", "sim-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addStep("j1", new AddJourneyStepRequest("sim-x", "principal", null, true)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("publicada");
    }

    @Test
    void addStepPinsPublishedVersionNumber() {
        AssessmentJourneyEntity journey = draftJourney();
        when(journeyRepository.findByEmpresaIdAndId("empresa-1", "j1")).thenReturn(Optional.of(journey));
        when(simulationCatalogService.findPublishedById("empresa-1", "sim-x"))
                .thenReturn(Optional.of(publishedSimulation("sim-x", 3)));

        AssessmentJourneyDetailResponse response =
                service.addStep("j1", new AddJourneyStepRequest("sim-x", null, null, null));

        assertThat(response.sequences()).hasSize(1);
        assertThat(response.sequences().get(0).sequenceKey()).isEqualTo("principal");
        assertThat(response.sequences().get(0).steps().get(0).simulationVersionNumber()).isEqualTo(3);
        assertThat(response.sequences().get(0).steps().get(0).required()).isTrue();
    }

    @Test
    void addStepRejectsSimulationAlreadyInSameSequence() {
        AssessmentJourneyEntity journey = draftJourney();
        journey.getSteps().add(step(journey, 1L, "sim-x", "principal", 0));
        when(journeyRepository.findByEmpresaIdAndId("empresa-1", "j1")).thenReturn(Optional.of(journey));
        when(simulationCatalogService.findPublishedById("empresa-1", "sim-x"))
                .thenReturn(Optional.of(publishedSimulation("sim-x", 1)));

        assertThatThrownBy(() -> service.addStep("j1", new AddJourneyStepRequest("sim-x", "principal", null, true)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("já foi adicionada");
    }

    @Test
    void addStepAllowsSameSimulationInDifferentSequences() {
        AssessmentJourneyEntity journey = draftJourney();
        journey.getSteps().add(step(journey, 1L, "sim-x", "principal", 0));
        when(journeyRepository.findByEmpresaIdAndId("empresa-1", "j1")).thenReturn(Optional.of(journey));
        when(simulationCatalogService.findPublishedById("empresa-1", "sim-x"))
                .thenReturn(Optional.of(publishedSimulation("sim-x", 1)));

        AssessmentJourneyDetailResponse response =
                service.addStep("j1", new AddJourneyStepRequest("sim-x", "alternativa", null, true));

        assertThat(response.sequences()).hasSize(2);
    }

    @Test
    void cannotEditPublishedJourney() {
        AssessmentJourneyEntity journey = draftJourney();
        journey.setStatus(AssessmentJourneyStatus.PUBLISHED);
        when(journeyRepository.findByEmpresaIdAndId("empresa-1", "j1")).thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> service.addStep("j1", new AddJourneyStepRequest("sim-x", "principal", null, true)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("rascunho");
    }

    @Test
    void publishRejectsJourneyWithoutSteps() {
        AssessmentJourneyEntity journey = draftJourney();
        when(journeyRepository.findByEmpresaIdAndId("empresa-1", "j1")).thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> service.publishJourney("j1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("sequência");
    }

    @Test
    void publishSucceedsWhenAllStepsPublished() {
        AssessmentJourneyEntity journey = draftJourney();
        journey.getSteps().add(step(journey, 1L, "sim-a", "principal", 0));
        journey.getSteps().add(step(journey, 2L, "sim-b", "principal", 1));
        when(journeyRepository.findByEmpresaIdAndId("empresa-1", "j1")).thenReturn(Optional.of(journey));
        when(simulationCatalogService.findPublishedById(eq("empresa-1"), anyString()))
                .thenReturn(Optional.of(publishedSimulation("sim-a", 1)));

        AssessmentJourneyDetailResponse response = service.publishJourney("j1");

        assertThat(response.status()).isEqualTo(AssessmentJourneyStatus.PUBLISHED);
        assertThat(response.publishedAt()).isNotNull();
        verify(auditEventService).appendAssessmentJourneyEvent(
                eq("empresa-1"), eq("u1"), eq("j1"),
                eq(AuditEventType.ASSESSMENT_JOURNEY_PUBLISHED), anyString(), anyString());
    }

    @Test
    void publishRejectsDuplicateOrderWithinSequence() {
        AssessmentJourneyEntity journey = draftJourney();
        journey.getSteps().add(step(journey, 1L, "sim-a", "principal", 0));
        AssessmentJourneyStepEntity duplicate = step(journey, 2L, "sim-b", "principal", 0);
        journey.getSteps().add(duplicate);
        when(journeyRepository.findByEmpresaIdAndId("empresa-1", "j1")).thenReturn(Optional.of(journey));
        lenient().when(simulationCatalogService.findPublishedById(eq("empresa-1"), anyString()))
                .thenReturn(Optional.of(publishedSimulation("sim-a", 1)));

        assertThatThrownBy(() -> service.publishJourney("j1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ordem duplicada");
    }

    @Test
    void archivedJourneyCannotBePublished() {
        AssessmentJourneyEntity journey = draftJourney();
        journey.setStatus(AssessmentJourneyStatus.ARCHIVED);
        when(journeyRepository.findByEmpresaIdAndId("empresa-1", "j1")).thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> service.publishJourney("j1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("arquivada");
    }

    private AssessmentJourneyEntity draftJourney() {
        AssessmentJourneyEntity journey = new AssessmentJourneyEntity();
        journey.setId("j1");
        journey.setEmpresaId("empresa-1");
        journey.setName("Jornada");
        journey.setStatus(AssessmentJourneyStatus.DRAFT);
        journey.setCreatedAt(Instant.now());
        journey.setUpdatedAt(Instant.now());
        return journey;
    }

    private AssessmentJourneyStepEntity step(AssessmentJourneyEntity journey, Long id, String simId, String seq, int order) {
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
}
