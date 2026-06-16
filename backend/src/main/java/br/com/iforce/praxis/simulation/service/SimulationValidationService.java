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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SimulationValidationService {

    private static final int MAX_DEPTH_TURNS = 10;
    private static final int LARGE_GRAPH_NODE_THRESHOLD = 8;

    private final PraxisProperties praxisProperties;

    public SimulationValidationService(PraxisProperties praxisProperties) {
        this.praxisProperties = praxisProperties;
    }

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

        validateCompetencyWeights(simulationVersionEntity, issues);
        warnLargeGraph(simulationVersionEntity, issues);

        if (nodesById.containsKey(simulationVersionEntity.getRootNodeId())) {
            detectCycles(simulationVersionEntity.getRootNodeId(), nodesById, issues);
            validateReachability(simulationVersionEntity.getRootNodeId(), nodesById, issues);
            validateDepth(simulationVersionEntity.getRootNodeId(), nodesById, issues);
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

    private void validateCompetencyWeights(
            SimulationVersionEntity simulationVersionEntity,
            List<ValidationIssueResponse> issues
    ) {
        Set<SimulationCompetencyEntity> competencies = simulationVersionEntity.getCompetencies();
        if (competencies.isEmpty()) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    simulationVersionEntity.getRootNodeId(),
                    "Versão sem competências configuradas."
            ));
            return;
        }

        double weightSum = 0.0;
        for (SimulationCompetencyEntity competency : competencies) {
            if (competency.getWeight() < 0) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        competency.getName(),
                        "Peso de competência não pode ser negativo."
                ));
            }
            weightSum += competency.getWeight();
        }

        if (Math.abs(weightSum - 1.0) > praxisProperties.competencyWeightTolerance()) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    simulationVersionEntity.getRootNodeId(),
                    "A soma dos pesos das competências deve ser 1.0 (atual: " + weightSum + ")."
            ));
        }
    }

    private void warnLargeGraph(
            SimulationVersionEntity simulationVersionEntity,
            List<ValidationIssueResponse> issues
    ) {
        if (simulationVersionEntity.getNodes().size() > LARGE_GRAPH_NODE_THRESHOLD) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.WARNING,
                    simulationVersionEntity.getRootNodeId(),
                    "Grafo grande detectado — validação pode demorar."
            ));
        }
    }

    private void validateDepth(
            String rootNodeId,
            Map<String, SimulationNodeEntity> nodesById,
            List<ValidationIssueResponse> issues
    ) {
        int longestPath = longestPathLength(rootNodeId, nodesById, new HashMap<>(), new HashSet<>());
        if (longestPath > MAX_DEPTH_TURNS) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    rootNodeId,
                    "A simulação excede a profundidade máxima de " + MAX_DEPTH_TURNS + " turnos (atual: " + longestPath + ")."
            ));
        }
    }

    private int longestPathLength(
            String nodeId,
            Map<String, SimulationNodeEntity> nodesById,
            Map<String, Integer> memo,
            Set<String> visiting
    ) {
        SimulationNodeEntity node = nodesById.get(nodeId);
        if (node == null || !visiting.add(nodeId)) {
            // Nó inexistente ou ciclo (já sinalizado por detectCycles): interrompe a recursão.
            return 0;
        }

        Integer cached = memo.get(nodeId);
        if (cached != null) {
            visiting.remove(nodeId);
            return cached;
        }

        int longestChild = 0;
        for (SimulationOptionEntity option : node.getOptions()) {
            if (option.getNextNodeId() != null && nodesById.containsKey(option.getNextNodeId())) {
                longestChild = Math.max(longestChild, longestPathLength(option.getNextNodeId(), nodesById, memo, visiting));
            }
        }

        int longest = 1 + longestChild;
        memo.put(nodeId, longest);
        visiting.remove(nodeId);
        return longest;
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
