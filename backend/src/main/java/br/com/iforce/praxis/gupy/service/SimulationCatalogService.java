package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import br.com.iforce.praxis.simulation.service.SimulationMapperService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SimulationCatalogService {

    private final SimulationVersionRepository simulationVersionRepository;
    private final SimulationMapperService simulationMapperService;

    public SimulationCatalogService(
            SimulationVersionRepository simulationVersionRepository,
            SimulationMapperService simulationMapperService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.simulationMapperService = simulationMapperService;
    }

    @Transactional(readOnly = true)
    public List<PublishedSimulation> findPublished() {
        return simulationVersionRepository
                .findByStatusAndSimulationArchivedFalseAndSimulationDeletedAtIsNullOrderByPublishedAtDesc(
                        SimulationVersionStatus.PUBLISHED
                )
                .stream()
                .map(simulationMapperService::toPublishedSimulation)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PublishedSimulation> findPublishedById(String simulationId) {
        return simulationVersionRepository
                .findBySimulationIdAndStatusAndSimulationArchivedFalseAndSimulationDeletedAtIsNullOrderByPublishedAtDesc(
                        simulationId,
                        SimulationVersionStatus.PUBLISHED
                )
                .stream()
                .findFirst()
                .map(simulationMapperService::toPublishedSimulation);
    }

    @Transactional(readOnly = true)
    public Optional<PublishedSimulation> findByVersionId(Long simulationVersionId) {
        return simulationVersionRepository.findById(simulationVersionId)
                .map(simulationMapperService::toPublishedSimulation);
    }

    public Optional<ScenarioNode> findNode(PublishedSimulation simulation, String nodeId) {
        return simulation.nodes().stream()
                .filter(node -> node.id().equals(nodeId))
                .findFirst();
    }
}
