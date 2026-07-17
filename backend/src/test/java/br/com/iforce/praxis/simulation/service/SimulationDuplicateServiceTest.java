package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.simulation.dto.DuplicateSimulationRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationDuplicateServiceTest {

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
    void duplicatesAllAssessmentContentIntoIndependentDraft() {
        SimulationVersionEntity sourceVersion = sourceVersion();
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("tenant-1");
        when(simulationVersionRepository.findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                "tenant-1", "sim-origem", 3
        )).thenReturn(Optional.of(sourceVersion));
        when(simulationRepository.existsById(any())).thenReturn(false);
        when(simulationRepository.save(any(SimulationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.duplicate(
                "sim-origem",
                3,
                new DuplicateSimulationRequest("Atendimento - modelo interno")
        );

        ArgumentCaptor<SimulationEntity> captor = ArgumentCaptor.forClass(SimulationEntity.class);
        verify(simulationRepository).save(captor.capture());
        SimulationEntity duplicate = captor.getValue();
        SimulationVersionEntity duplicateVersion = duplicate.getVersions().iterator().next();
        SimulationNodeEntity duplicateNode = duplicateVersion.getNodes().iterator().next();
        SimulationOptionEntity duplicateOption = duplicateNode.getOptions().iterator().next();

        assertThat(duplicate.getId()).isNotEqualTo("sim-origem");
        assertThat(duplicate.getEmpresaId()).isEqualTo("tenant-1");
        assertThat(duplicate.getName()).isEqualTo("Atendimento - modelo interno");
        assertThat(duplicateVersion.getVersionNumber()).isEqualTo(1);
        assertThat(duplicateVersion.getStatus()).isEqualTo(SimulationVersionStatus.DRAFT);
        assertThat(duplicateVersion.getPublishedAt()).isNull();
        assertThat(duplicateVersion.getCompetencies()).hasSize(1);
        assertThat(duplicateVersion.getNodes()).hasSize(1);
        assertThat(duplicateOption.getCompetencyScores()).hasSize(1);
        assertThat(duplicateVersion).isNotSameAs(sourceVersion);
        assertThat(duplicateNode).isNotSameAs(sourceVersion.getNodes().iterator().next());
    }

    private SimulationVersionEntity sourceVersion() {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setId("sim-origem");
        simulation.setEmpresaId("tenant-1");
        simulation.setName("Atendimento");
        simulation.setDescription("Avaliação de atendimento.");
        simulation.setCriticalSituation("Cliente insatisfeito.");
        simulation.setResultUse("Entrevista estruturada");
        simulation.setCreatedAt(Instant.now());

        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setSimulation(simulation);
        version.setVersionNumber(3);
        version.setStatus(SimulationVersionStatus.PUBLISHED);
        version.setRootNodeId("turno-1");
        version.setCreatedAt(Instant.now());
        version.setPublishedAt(Instant.now());
        simulation.getVersions().add(version);

        SimulationCompetencyEntity competency = new SimulationCompetencyEntity();
        competency.setSimulationVersion(version);
        competency.setName("Comunicação");
        competency.setWeight(100);
        competency.setTargetScore(3);
        version.getCompetencies().add(competency);

        SimulationNodeEntity node = new SimulationNodeEntity();
        node.setSimulationVersion(version);
        node.setNodeId("turno-1");
        node.setTurnIndex(1);
        node.setSpeaker("Cliente");
        node.setMessage("Preciso de ajuda.");
        version.getNodes().add(node);

        SimulationOptionEntity option = new SimulationOptionEntity();
        option.setSimulationNode(node);
        option.setOptionId("opcao-1");
        option.setText("Ouvir e confirmar o problema.");
        option.setAuditNote("Demonstra escuta ativa.");
        node.getOptions().add(option);

        OptionCompetencyScoreEntity score = new OptionCompetencyScoreEntity();
        score.setSimulationOption(option);
        score.setCompetencyName("Comunicação");
        score.setScore(3);
        option.getCompetencyScores().add(score);
        return version;
    }
}
