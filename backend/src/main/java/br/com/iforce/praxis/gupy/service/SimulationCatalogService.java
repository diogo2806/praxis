package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import br.com.iforce.praxis.simulation.service.SimulationMapperService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
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
    public List<PublishedSimulation> findPublished(String searchString, int offset, int limit) {
        int normalizedOffset = Math.max(offset, 0);
        int normalizedLimit = Math.max(limit, 1);

        return filteredPublished(searchString).stream()
                .skip(normalizedOffset)
                .limit(normalizedLimit)
                .toList();
    }

    @Transactional(readOnly = true)
    public int countPublished(String searchString) {
        return filteredPublished(searchString).size();
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

    private List<PublishedSimulation> filteredPublished(String searchString) {
        String normalizedSearch = normalize(searchString);
        return findPublished().stream()
                .filter(simulation -> normalizedSearch.isBlank()
                        || normalize(simulation.id()).contains(normalizedSearch)
                        || normalize(simulation.name()).contains(normalizedSearch)
                        || normalize(simulation.description()).contains(normalizedSearch))
                .toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }
}
