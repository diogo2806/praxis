package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationRepository;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationDuplicateCatalogServiceTest {

    @Mock
    private SimulationVersionRepository simulationVersionRepository;
    @Mock
    private SimulationRepository simulationRepository;
    @Mock
    private SimulationMapperService simulationMapperService;
    @Mock
    private AuditEventService auditEventService;
    @Mock
    private CurrentEmpresaService currentEmpresaService;

    private SimulationDuplicateService service;

    @BeforeEach
    void setUp() {
        service = new SimulationDuplicateService(
                simulationVersionRepository,
                simulationRepository,
                simulationMapperService,
                auditEventService,
                currentEmpresaService
        );
    }

    @Test
    void shouldCopyApprovedCatalogSourceIntoIndependentTenantDraft() {
        SimulationVersionEntity source = sourceVersion();
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-destino");
        when(simulationVersionRepository.findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                "empresa-origem",
                "modelo-atendimento",
                4
        )).thenReturn(Optional.of(source));
        when(simulationRepository.existsById(anyString())).thenReturn(false);
        when(simulationRepository.save(any(SimulationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.duplicateFromCatalog(
                "empresa-origem",
                "modelo-atendimento",
                4,
                "template-123",
                "2",
                "Atendimento copiado"
        );

        ArgumentCaptor<SimulationEntity> simulationCaptor = ArgumentCaptor.forClass(SimulationEntity.class);
        verify(simulationRepository).save(simulationCaptor.capture());
        SimulationEntity copied = simulationCaptor.getValue();
        SimulationVersionEntity copiedVersion = copied.getVersions().iterator().next();
        SimulationNodeEntity copiedNode = copiedVersion.getNodes().iterator().next();
        SimulationOptionEntity copiedOption = copiedNode.getOptions().iterator().next();

        assertThat(copied.getEmpresaId()).isEqualTo("empresa-destino");
        assertThat(copied.getId()).isNotEqualTo(source.getSimulation().getId());
        assertThat(copiedVersion.getStatus()).isEqualTo(SimulationVersionStatus.DRAFT);
        assertThat(copiedVersion.getVersionNumber()).isEqualTo(1);
        assertThat(copiedVersion.getPublishedAt()).isNull();
        assertThat(copiedVersion).isNotSameAs(source);
        assertThat(copiedNode).isNotSameAs(source.getNodes().iterator().next());
        assertThat(copiedOption.getCompetencyScores()).singleElement()
                .extracting(OptionCompetencyScoreEntity::getScore)
                .isEqualTo(85);

        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditEventService).appendSimulationVersionEvent(
                org.mockito.ArgumentMatchers.eq("empresa-destino"),
                org.mockito.ArgumentMatchers.eq(copied.getId()),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(AuditEventType.SIMULATION_VERSION_CLONED),
                anyString(),
                metadataCaptor.capture()
        );
        assertThat(metadataCaptor.getValue())
                .contains("template-123")
                .contains("modelo-atendimento")
                .contains("sourceVersionNumber\":4")
                .contains("templateVersion\":\"2\"");
    }

    private SimulationVersionEntity sourceVersion() {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setId("modelo-atendimento");
        simulation.setEmpresaId("empresa-origem");
        simulation.setName("Modelo Atendimento");
        simulation.setDescription("Modelo governado para atendimento.");
        simulation.setCriticalSituation("Cliente contesta uma cobrança.");
        simulation.setResultUse("Seleção");
        simulation.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setSimulation(simulation);
        version.setVersionNumber(4);
        version.setStatus(SimulationVersionStatus.PUBLISHED);
        version.setRootNodeId("N1");
        version.setCreatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        version.setPublishedAt(Instant.parse("2026-01-03T00:00:00Z"));
        simulation.getVersions().add(version);

        SimulationCompetencyEntity competency = new SimulationCompetencyEntity();
        competency.setSimulationVersion(version);
        competency.setName("Comunicação");
        competency.setWeight(1.0);
        competency.setTargetScore(70);
        version.getCompetencies().add(competency);

        SimulationNodeEntity node = new SimulationNodeEntity();
        node.setSimulationVersion(version);
        node.setNodeId("N1");
        node.setTurnIndex(1);
        node.setSpeaker("Cliente");
        node.setMessage("Não reconheço esta cobrança.");
        node.setFinal(false);
        version.getNodes().add(node);

        SimulationOptionEntity option = new SimulationOptionEntity();
        option.setSimulationNode(node);
        option.setOptionId("A");
        option.setText("Escutar, confirmar os dados e explicar a solução.");
        option.setAuditNote("Demonstra escuta e clareza.");
        node.getOptions().add(option);

        OptionCompetencyScoreEntity score = new OptionCompetencyScoreEntity();
        score.setSimulationOption(option);
        score.setCompetencyName("Comunicação");
        score.setScore(85);
        option.getCompetencyScores().add(score);
        return version;
    }
}
