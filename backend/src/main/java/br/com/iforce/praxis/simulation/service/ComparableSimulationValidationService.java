package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.dto.ValidationIssueResponse;
import br.com.iforce.praxis.simulation.model.ValidationIssueSeverity;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Complementa a validação estrutural com a regra necessária para comparar
 * candidatos: todos os caminhos devem usar a mesma base máxima por competência.
 */
@Primary
@Service
public class ComparableSimulationValidationService extends SimulationValidationService {

    public ComparableSimulationValidationService(PraxisProperties praxisProperties) {
        super(praxisProperties);
    }

    @Override
    public SimulationValidationResponse validate(SimulationVersionEntity version) {
        SimulationValidationResponse base = super.validate(version);
        List<ValidationIssueResponse> issues = new ArrayList<>(base.issues());
        addComparabilityBlockers(version, issues);

        long blockers = issues.stream()
                .filter(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER)
                .count();
        long warnings = issues.stream()
                .filter(issue -> issue.severity() == ValidationIssueSeverity.WARNING)
                .count();

        return new SimulationValidationResponse(
                base.simulationId(),
                base.versionNumber(),
                blockers == 0,
                blockers,
                warnings,
                Math.max(0, 100 - (int) blockers * 30 - (int) warnings * 10),
                List.copyOf(issues)
        );
    }

    private void addComparabilityBlockers(
            SimulationVersionEntity version,
            List<ValidationIssueResponse> issues
    ) {
        Map<String, SimulationNodeEntity> nodesById = version.getNodes().stream()
                .collect(Collectors.toMap(
                        SimulationNodeEntity::getNodeId,
                        node -> node,
                        (first, ignored) -> first,
                        HashMap::new
                ));
        if (version.getRootNodeId() == null || !nodesById.containsKey(version.getRootNodeId())) {
            return;
        }

        List<List<SimulationNodeEntity>> paths = new ArrayList<>();
        collectPaths(
                version.getRootNodeId(),
                nodesById,
                new HashSet<>(),
                new ArrayList<>(),
                paths
        );
        if (paths.size() < 2) {
            return;
        }

        for (SimulationCompetencyEntity competency : version.getCompetencies()) {
            Set<Integer> pathMaximums = paths.stream()
                    .map(path -> maximumFor(path, competency.getName()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (pathMaximums.size() > 1 || pathMaximums.contains(0)) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        version.getRootNodeId(),
                        "A competência \"" + competency.getName()
                                + "\" possui bases máximas diferentes entre os caminhos ("
                                + pathMaximums.stream().map(String::valueOf).collect(Collectors.joining(", "))
                                + "). Ajuste as alternativas para que todos os candidatos sejam avaliados na mesma escala."
                ));
            }
        }
    }

    private void collectPaths(
            String nodeId,
            Map<String, SimulationNodeEntity> nodesById,
            Set<String> visiting,
            List<SimulationNodeEntity> current,
            List<List<SimulationNodeEntity>> paths
    ) {
        SimulationNodeEntity node = nodesById.get(nodeId);
        if (node == null || !visiting.add(nodeId)) {
            return;
        }

        current.add(node);
        List<String> nextIds = nextNodeIds(node, nodesById);
        if (node.isFinal() || nextIds.isEmpty()) {
            paths.add(List.copyOf(current));
        } else {
            for (String nextId : nextIds) {
                collectPaths(nextId, nodesById, visiting, current, paths);
            }
        }

        current.remove(current.size() - 1);
        visiting.remove(nodeId);
    }

    private List<String> nextNodeIds(
            SimulationNodeEntity node,
            Map<String, SimulationNodeEntity> nodesById
    ) {
        if (node.isFinal()) {
            return List.of();
        }

        LinkedHashSet<String> result = node.getOptions().stream()
                .map(SimulationOptionEntity::getNextNodeId)
                .filter(nextId -> nextId != null && nodesById.containsKey(nextId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (node.getTimeoutNextNodeId() != null
                && nodesById.containsKey(node.getTimeoutNextNodeId())) {
            result.add(node.getTimeoutNextNodeId());
        }
        return List.copyOf(result);
    }

    private int maximumFor(List<SimulationNodeEntity> path, String competencyName) {
        return path.stream()
                .filter(node -> !node.isFinal())
                .mapToInt(node -> node.getOptions().stream()
                        .mapToInt(option -> option.getCompetencyScores().stream()
                                .filter(score -> competencyName.equals(score.getCompetencyName()))
                                .mapToInt(OptionCompetencyScoreEntity::getScore)
                                .max()
                                .orElse(0))
                        .max()
                        .orElse(0))
                .sum();
    }
}
