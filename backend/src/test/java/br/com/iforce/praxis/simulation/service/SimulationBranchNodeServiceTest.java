package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationBranchNodeServiceTest {

    @Mock
    private SimulationVersionRepository simulationVersionRepository;
    @Mock
    private CurrentEmpresaService currentEmpresaService;
    @Mock
    private AuditEventService auditEventService;

    private SimulationBranchNodeService service;

    @BeforeEach
    void setUp() {
        service = new SimulationBranchNodeService(
                simulationVersionRepository,
                currentEmpresaService,
                auditEventService
        );
    }

    @Test
    void createsAndLinksBranchNodeInSameTransaction() {
        SimulationVersionEntity version = draftVersion();
        SimulationNodeEntity root = version.getNodes().iterator().next();
        SimulationOptionEntity selectedOption = root.getOptions().iterator().next();

        when(currentEmpresaService.requiredEmpresaId()).thenReturn("tenant-1");
        when(simulationVersionRepository.findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                "tenant-1",
                "sim-ramificada",
                1
        )).thenReturn(Optional.of(version));
        when(simulationVersionRepository.save(any(SimulationVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String createdNodeId = service.createBranchNode(
                "sim-ramificada",
                1,
                "turno-1",
                "opcao-1"
        );

        SimulationNodeEntity createdNode = version.getNodes().stream()
                .filter(node -> createdNodeId.equals(node.getNodeId()))
                .findFirst()
                .orElseThrow();

        assertThat(createdNodeId).isEqualTo("turno-2");
        assertThat(selectedOption.getNextNodeId()).isEqualTo("turno-2");
        assertThat(version.getNodes()).hasSize(2);
        assertThat(createdNode.getTurnIndex()).isEqualTo(2);
        assertThat(createdNode.getMessage()).isEmpty();
        assertThat(createdNode.isFinal()).isFalse();
        assertThat(createdNode.getPositionX()).isEqualTo(390.0);
        assertThat(createdNode.getPositionY()).isEqualTo(40.0);
        verify(simulationVersionRepository).save(version);
        verify(auditEventService, times(2)).appendSimulationVersionEvent(
                any(),
                any(),
                any(Integer.class),
                any(),
                any(),
                any()
        );
    }

    @Test
    void positionsNewBranchBelowExistingSibling() {
        SimulationVersionEntity version = draftVersion();
        SimulationNodeEntity root = version.getNodes().iterator().next();
        SimulationOptionEntity selectedOption = root.getOptions().iterator().next();

        SimulationNodeEntity existingChild = node(version, "turno-2", 2, "Continuação existente");
        version.getNodes().add(existingChild);

        SimulationOptionEntity siblingOption = option(root, "opcao-2", "turno-2");
        root.getOptions().add(siblingOption);

        when(currentEmpresaService.requiredEmpresaId()).thenReturn("tenant-1");
        when(simulationVersionRepository.findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                "tenant-1",
                "sim-ramificada",
                1
        )).thenReturn(Optional.of(version));
        when(simulationVersionRepository.save(any(SimulationVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String createdNodeId = service.createBranchNode(
                "sim-ramificada",
                1,
                "turno-1",
                selectedOption.getOptionId()
        );

        SimulationNodeEntity createdNode = version.getNodes().stream()
                .filter(node -> createdNodeId.equals(node.getNodeId()))
                .findFirst()
                .orElseThrow();

        assertThat(createdNodeId).isEqualTo("turno-3");
        assertThat(createdNode.getPositionX()).isEqualTo(390.0);
        assertThat(createdNode.getPositionY()).isEqualTo(200.0);
    }

    private SimulationVersionEntity draftVersion() {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setId("sim-ramificada");
        simulation.setEmpresaId("tenant-1");
        simulation.setName("Avaliação ramificada");
        simulation.setDescription("Teste de ramificações.");
        simulation.setCreatedAt(Instant.now());

        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setSimulation(simulation);
        version.setVersionNumber(1);
        version.setStatus(SimulationVersionStatus.DRAFT);
        version.setRootNodeId("turno-1");
        version.setCreatedAt(Instant.now());
        simulation.getVersions().add(version);

        SimulationNodeEntity root = node(version, "turno-1", 1, "Como você responderia?");
        root.setPositionX(40.0);
        root.setPositionY(40.0);
        root.getOptions().add(option(root, "opcao-1", null));
        version.getNodes().add(root);
        return version;
    }

    private SimulationNodeEntity node(
            SimulationVersionEntity version,
            String nodeId,
            int turnIndex,
            String message
    ) {
        SimulationNodeEntity node = new SimulationNodeEntity();
        node.setSimulationVersion(version);
        node.setNodeId(nodeId);
        node.setTurnIndex(turnIndex);
        node.setSpeaker("Cliente");
        node.setMessage(message);
        return node;
    }

    private SimulationOptionEntity option(
            SimulationNodeEntity node,
            String optionId,
            String nextNodeId
    ) {
        SimulationOptionEntity option = new SimulationOptionEntity();
        option.setSimulationNode(node);
        option.setOptionId(optionId);
        option.setText("Alternativa " + optionId);
        option.setNextNodeId(nextNodeId);
        option.setAuditNote("");
        return option;
    }
}
