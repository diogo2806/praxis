package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.auth.context.EmpresaContextHolder;
import br.com.iforce.praxis.candidate.dto.CandidateAttemptMonitoringPageResponse;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.CandidateAttemptMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateAttemptMonitoringQueryServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-16T15:00:00Z");
    private static final Instant ANSWERED_AT = Instant.parse("2026-07-16T15:01:00Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-07-16T15:02:00Z");

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;
    @Mock
    private SimulationCatalogService simulationCatalogService;
    @Mock
    private CandidateAttemptMapper candidateAttemptMapper;

    private CandidateAttemptMonitoringQueryService service;

    @BeforeEach
    void setUp() {
        EmpresaContextHolder.set("empresa-1");
        service = new CandidateAttemptMonitoringQueryService(
                candidateAttemptRepository,
                simulationCatalogService,
                candidateAttemptMapper
        );
    }

    @AfterEach
    void tearDown() {
        EmpresaContextHolder.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchHandlesSelectedTerminalOptionWithoutNextNode() {
        CandidateAttemptEntity entity = attemptEntity();
        ScenarioNode rootNode = rootNodeWithTerminalOption();
        PublishedSimulation simulation = simulation(rootNode);
        CandidateAttempt attempt = CandidateAttempt.builder()
                .answersByNodeId(Map.of(
                        rootNode.id(),
                        AttemptAnswer.answered(rootNode.id(), "finish", ANSWERED_AT)
                ))
                .build();

        when(candidateAttemptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 25), 1));
        when(simulationCatalogService.findByVersionId(10L)).thenReturn(Optional.of(simulation));
        when(simulationCatalogService.findNode(simulation, rootNode.id())).thenReturn(Optional.of(rootNode));
        when(candidateAttemptMapper.toDomain(entity)).thenReturn(attempt);

        CandidateAttemptMonitoringPageResponse response = service.search(0, 25, null, null, null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().currentTurn()).isEqualTo(1);
        assertThat(response.items().getFirst().estimatedTurns()).isEqualTo(1);
        assertThat(response.items().getFirst().progressPercent()).isEqualTo(100);
        assertThat(response.items().getFirst().active()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchUsesPublishedSimulationForLegacyAttemptWithoutPinnedVersion() {
        CandidateAttemptEntity entity = attemptEntity();
        entity.setSimulationVersionId(null);
        entity.setSimulationVersionNumber(null);
        ScenarioNode rootNode = rootNodeWithTerminalOption();
        PublishedSimulation simulation = simulation(rootNode);
        CandidateAttempt attempt = CandidateAttempt.builder()
                .answersByNodeId(Map.of())
                .build();

        when(candidateAttemptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 25), 1));
        when(simulationCatalogService.findPublishedById("empresa-1", "sim-1"))
                .thenReturn(Optional.of(simulation));
        when(simulationCatalogService.findNode(simulation, rootNode.id())).thenReturn(Optional.of(rootNode));
        when(candidateAttemptMapper.toDomain(entity)).thenReturn(attempt);

        CandidateAttemptMonitoringPageResponse response = service.search(0, 25, null, null, null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().simulationName()).isEqualTo("Atendimento N2");
        assertThat(response.items().getFirst().versionNumber()).isEqualTo(3);
        assertThat(response.items().getFirst().currentTurn()).isEqualTo(1);
        assertThat(response.items().getFirst().estimatedTurns()).isEqualTo(1);
        assertThat(response.items().getFirst().progressPercent()).isEqualTo(100);
    }

    private CandidateAttemptEntity attemptEntity() {
        CandidateAttemptEntity entity = new CandidateAttemptEntity();
        entity.setId("att_1234567890123456");
        entity.setEmpresaId("empresa-1");
        entity.setSimulationId("sim-1");
        entity.setSimulationVersionId(10L);
        entity.setCandidateName("Maria Silva");
        entity.setCandidateEmail("maria@example.com");
        entity.setStatus(AttemptStatus.COMPLETED);
        entity.setCreatedAt(CREATED_AT);
        entity.setStartedAt(CREATED_AT);
        entity.setFinishedAt(FINISHED_AT);
        return entity;
    }

    private ScenarioNode rootNodeWithTerminalOption() {
        ScenarioOption terminalOption = new ScenarioOption(
                "finish",
                "Finalizar avaliação",
                null,
                Map.of(),
                false,
                null
        );
        return new ScenarioNode(
                "node-1",
                1,
                "Atendente",
                "Encerrar atendimento",
                60,
                List.of(terminalOption)
        );
    }

    private PublishedSimulation simulation(ScenarioNode rootNode) {
        return new PublishedSimulation(
                10L,
                3,
                "sim-1",
                "Atendimento N2",
                "Descrição",
                List.of("Comunicação"),
                Map.of("Comunicação", 1.0),
                Map.of(),
                rootNode.id(),
                List.of(rootNode)
        );
    }
}
