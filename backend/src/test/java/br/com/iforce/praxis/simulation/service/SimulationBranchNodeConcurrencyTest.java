package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Sql(scripts = "/seed-branch-node-concurrency.sql")
class SimulationBranchNodeConcurrencyTest {

    @Autowired
    private SimulationBranchNodeService simulationBranchNodeService;

    @Autowired
    private SimulationVersionRepository simulationVersionRepository;

    @MockitoBean
    private CurrentEmpresaService currentEmpresaService;

    @MockitoBean
    private AuditEventService auditEventService;

    @BeforeEach
    void setUp() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-data17");
    }

    @Test
    void serializesConcurrentBranchCreationForTheSameVersion() throws Exception {
        CyclicBarrier startBarrier = new CyclicBarrier(2);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            Future<CreatedBranch> firstFuture = executorService.submit(
                    () -> createBranch(startBarrier, "opcao-a")
            );
            Future<CreatedBranch> secondFuture = executorService.submit(
                    () -> createBranch(startBarrier, "opcao-b")
            );

            CreatedBranch firstCreated = firstFuture.get(15, TimeUnit.SECONDS);
            CreatedBranch secondCreated = secondFuture.get(15, TimeUnit.SECONDS);

            assertThat(firstCreated.nodeId()).isNotEqualTo(secondCreated.nodeId());
            assertThat(List.of(firstCreated.nodeId(), secondCreated.nodeId()))
                    .containsExactlyInAnyOrder("turno-2", "turno-3");

            SimulationVersionEntity version = simulationVersionRepository
                    .findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                            "empresa-data17",
                            "sim-data17-concurrency",
                            1
                    )
                    .orElseThrow();

            SimulationNodeEntity rootNode = version.getNodes().stream()
                    .filter(node -> "turno-1".equals(node.getNodeId()))
                    .findFirst()
                    .orElseThrow();
            Map<String, String> targetByOption = rootNode.getOptions().stream()
                    .collect(Collectors.toMap(
                            SimulationOptionEntity::getOptionId,
                            SimulationOptionEntity::getNextNodeId
                    ));
            Map<String, Integer> turnIndexByNode = version.getNodes().stream()
                    .collect(Collectors.toMap(
                            SimulationNodeEntity::getNodeId,
                            SimulationNodeEntity::getTurnIndex
                    ));

            assertThat(targetByOption)
                    .containsEntry(firstCreated.optionId(), firstCreated.nodeId())
                    .containsEntry(secondCreated.optionId(), secondCreated.nodeId());
            assertThat(turnIndexByNode)
                    .containsEntry("turno-2", 2)
                    .containsEntry("turno-3", 3);
            assertThat(version.getNodes()).hasSize(3);
        } finally {
            executorService.shutdownNow();
        }
    }

    private CreatedBranch createBranch(CyclicBarrier startBarrier, String optionId) throws Exception {
        startBarrier.await(10, TimeUnit.SECONDS);
        String nodeId = simulationBranchNodeService.createBranchNode(
                "sim-data17-concurrency",
                1,
                "turno-1",
                optionId
        );
        return new CreatedBranch(optionId, nodeId);
    }

    private record CreatedBranch(String optionId, String nodeId) {
    }
}
