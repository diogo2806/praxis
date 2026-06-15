package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.dto.ValidationIssueResponse;
import br.com.iforce.praxis.simulation.model.ValidationIssueSeverity;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SimulationValidationService {

    public SimulationValidationResponse validate(SimulationVersionEntity simulationVersionEntity) {
        List<ValidationIssueResponse> issues = new ArrayList<>();
        Map<String, SimulationNodeEntity> nodesById = buildNodeMap(simulationVersionEntity, issues);

        if (!nodesById.containsKey(simulationVersionEntity.getRootNodeId())) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    simulationVersionEntity.getRootNodeId(),
                    "Nó raiz não encontrado na versão."
            ));
        }

        for (SimulationNodeEntity node : simulationVersionEntity.getNodes()) {
            validateNode(node, nodesById, issues);
        }

        if (nodesById.containsKey(simulationVersionEntity.getRootNodeId())) {
            detectCycles(simulationVersionEntity.getRootNodeId(), nodesById, issues);
            validateReachability(simulationVersionEntity.getRootNodeId(), nodesById, issues);
        }

        boolean publishable = issues.stream()
                .noneMatch(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER);

        return new SimulationValidationResponse(
                simulationVersionEntity.getSimulation().getId(),
                simulationVersionEntity.getVersionNumber(),
                publishable,
                issues
        );
    }

    private Map<String, SimulationNodeEntity> buildNodeMap(
            SimulationVersionEntity simulationVersionEntity,
            List<ValidationIssueResponse> issues
    ) {
        Map<String, SimulationNodeEntity> nodesById = new HashMap<>();
        for (SimulationNodeEntity node : simulationVersionEntity.getNodes()) {
            SimulationNodeEntity previousNode = nodesById.put(node.getNodeId(), node);
            if (previousNode != null) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        node.getNodeId(),
                        "Há nós duplicados com o mesmo identificador."
                ));
            }
        }
        return nodesById;
    }

    private void validateNode(
            SimulationNodeEntity node,
            Map<String, SimulationNodeEntity> nodesById,
            List<ValidationIssueResponse> issues
    ) {
        if (node.getOptions().size() < 2 || node.getOptions().size() > 4) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "Cada turno precisa ter de 2 a 4 alternativas."
            ));
        }

        for (SimulationOptionEntity option : node.getOptions()) {
            validateOption(node, option, nodesById, issues);
        }
    }

    private void validateOption(
            SimulationNodeEntity node,
            SimulationOptionEntity option,
            Map<String, SimulationNodeEntity> nodesById,
            List<ValidationIssueResponse> issues
    ) {
        if (option.getCompetencyScores().isEmpty()) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "Alternativa sem score de competência."
            ));
        }

        for (OptionCompetencyScoreEntity score : option.getCompetencyScores()) {
            if (score.getScore() < 0 || score.getScore() > 100) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        node.getNodeId(),
                        "Score de competência fora do intervalo 0-100."
                ));
            }
        }

        if (option.getNextNodeId() != null && !nodesById.containsKey(option.getNextNodeId())) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "A alternativa aponta para um nó inexistente."
            ));
        }

        if (option.getNextNodeId() != null) {
            SimulationNodeEntity nextNode = nodesById.get(option.getNextNodeId());
            if (nextNode != null && nextNode.getTurnIndex() <= node.getTurnIndex()) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        node.getNodeId(),
                        "Ramificações só podem apontar para turnos posteriores."
                ));
            }
        }
    }

    private void detectCycles(
            String rootNodeId,
            Map<String, SimulationNodeEntity> nodesById,
            List<ValidationIssueResponse> issues
    ) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        visitForCycles(rootNodeId, nodesById, visiting, visited, issues);
    }

    private void visitForCycles(
            String nodeId,
            Map<String, SimulationNodeEntity> nodesById,
            Set<String> visiting,
            Set<String> visited,
            List<ValidationIssueResponse> issues
    ) {
        if (visited.contains(nodeId)) {
            return;
        }
        if (visiting.contains(nodeId)) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    nodeId,
                    "Ciclo detectado no grafo da simulação."
            ));
            return;
        }

        SimulationNodeEntity node = nodesById.get(nodeId);
        if (node == null) {
            return;
        }

        visiting.add(nodeId);
        for (SimulationOptionEntity option : node.getOptions()) {
            if (option.getNextNodeId() != null) {
                visitForCycles(option.getNextNodeId(), nodesById, visiting, visited, issues);
            }
        }
        visiting.remove(nodeId);
        visited.add(nodeId);
    }

    private void validateReachability(
            String rootNodeId,
            Map<String, SimulationNodeEntity> nodesById,
            List<ValidationIssueResponse> issues
    ) {
        Set<String> reachable = new HashSet<>();
        collectReachable(rootNodeId, nodesById, reachable);

        for (String nodeId : nodesById.keySet()) {
            if (!reachable.contains(nodeId)) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.WARNING,
                        nodeId,
                        "Nó não alcançável a partir do início da simulação."
                ));
            }
        }
    }

    private void collectReachable(String nodeId, Map<String, SimulationNodeEntity> nodesById, Set<String> reachable) {
        if (!reachable.add(nodeId)) {
            return;
        }

        SimulationNodeEntity node = nodesById.get(nodeId);
        if (node == null) {
            return;
        }

        for (SimulationOptionEntity option : node.getOptions()) {
            if (option.getNextNodeId() != null) {
                collectReachable(option.getNextNodeId(), nodesById, reachable);
            }
        }
    }
}
