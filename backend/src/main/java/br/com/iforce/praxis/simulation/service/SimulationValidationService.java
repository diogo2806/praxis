package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.simulation.dto.CompetencyWeightDto;
import br.com.iforce.praxis.simulation.dto.UpdateBlueprintRequest;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.dto.ValidationIssueResponse;
import br.com.iforce.praxis.simulation.model.ValidationIssueSeverity;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
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
                    "A primeira etapa do teste não foi encontrada. Escolha uma etapa inicial válida."
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
            validatePathScoreBalance(simulationVersionEntity.getRootNodeId(), nodesById, simulationVersionEntity, issues);
        }

        long blockerCount = issues.stream()
                .filter(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER)
                .count();
        long warningCount = issues.stream()
                .filter(issue -> issue.severity() == ValidationIssueSeverity.WARNING)
                .count();
        boolean publishable = blockerCount == 0;
        int qualityScore = Math.max(0, 100 - (int) blockerCount * 30 - (int) warningCount * 10);

        return new SimulationValidationResponse(
                simulationVersionEntity.getSimulation().getId(),
                simulationVersionEntity.getVersionNumber(),
                publishable,
                blockerCount,
                warningCount,
                qualityScore,
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
                        "Há etapas duplicadas com o mesmo identificador. Revise as etapas repetidas."
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
                    "Cada etapa precisa ter de 2 a 4 respostas."
            ));
        }

        for (SimulationOptionEntity option : node.getOptions()) {
            validateOption(node, option, nodesById, issues);
        }

        validateTimeoutTransition(node, nodesById, issues);
    }

    private void validateTimeoutTransition(
            SimulationNodeEntity node,
            Map<String, SimulationNodeEntity> nodesById,
            List<ValidationIssueResponse> issues
    ) {
        String timeoutNextNodeId = node.getTimeoutNextNodeId();
        if (timeoutNextNodeId == null) {
            boolean hasNonTerminalOption = node.getOptions().stream()
                    .anyMatch(option -> option.getNextNodeId() != null);
            if (hasNonTerminalOption) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        node.getNodeId(),
                        "Esta etapa continua o teste, então escolha para qual etapa ir caso o tempo se esgote."
                ));
            }
            return;
        }

        SimulationNodeEntity nextNode = nodesById.get(timeoutNextNodeId);
        if (nextNode == null) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "O caminho usado quando o tempo se esgota aponta para uma etapa que não existe."
            ));
            return;
        }

        if (nextNode.getTurnIndex() <= node.getTurnIndex()) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "Quando o tempo se esgota, o teste só pode seguir para uma etapa posterior."
            ));
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
                    "Uma resposta está sem pontuação de competência."
            ));
        }

        for (OptionCompetencyScoreEntity score : option.getCompetencyScores()) {
            if (score.getScore() < 0 || score.getScore() > 100) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        node.getNodeId(),
                        "A pontuação de competência deve ficar entre 0 e 100."
                ));
            }
        }

        if (option.getNextNodeId() != null && !nodesById.containsKey(option.getNextNodeId())) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "Uma resposta aponta para uma etapa que não existe."
            ));
        }

        if (option.getNextNodeId() != null) {
            SimulationNodeEntity nextNode = nodesById.get(option.getNextNodeId());
            if (nextNode != null && nextNode.getTurnIndex() <= node.getTurnIndex()) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        node.getNodeId(),
                        "As respostas só podem levar o candidato para etapas posteriores."
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
                    "O teste precisa ter pelo menos uma competência configurada."
            ));
            return;
        }

        double weightSum = 0.0;
        for (SimulationCompetencyEntity competency : competencies) {
            if (competency.getWeight() < 0) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        competency.getName(),
                        "O peso de uma competência não pode ser negativo."
                ));
            }
            weightSum += competency.getWeight();
        }

        if (Math.abs(weightSum - 1.0) > praxisProperties.competencyWeightTolerance()) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    simulationVersionEntity.getRootNodeId(),
                    "Os pesos das competências precisam somar 100%. Soma atual: " + (weightSum * 100) + "%."
            ));
        }
    }

    public void validateWeights(List<CompetencyWeightDto> competencies) {
        validateWeightValues(
                competencies.stream()
                        .map(CompetencyWeightDto::weight)
                        .toList()
        );
    }

    public void validateBlueprintWeights(List<UpdateBlueprintRequest.CompetencyRequest> competencies) {
        validateWeightValues(
                competencies.stream()
                        .map(UpdateBlueprintRequest.CompetencyRequest::weight)
                        .toList()
        );
    }

    private void validateWeightValues(List<Double> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ao menos uma competência é obrigatória.");
        }

        double sum = weights.stream().mapToDouble(weight -> weight == null ? 0.0 : weight).sum();
        if (Math.abs(sum - 1.0) > praxisProperties.competencyWeightTolerance()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os pesos precisam somar 100%."
            );
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
                    "Este teste tem muitas etapas. A validação pode demorar um pouco mais."
            ));
        }
    }

    private void validatePathScoreBalance(
            String rootNodeId,
            Map<String, SimulationNodeEntity> nodesById,
            SimulationVersionEntity simulationVersionEntity,
            List<ValidationIssueResponse> issues
    ) {
        List<List<SimulationNodeEntity>> paths = new ArrayList<>();
        collectTerminalNodePaths(rootNodeId, nodesById, new HashSet<>(), new ArrayList<>(), paths);
        if (paths.size() < 2) {
            return;
        }

        Map<String, Double> weightsByCompetency = new HashMap<>();
        for (SimulationCompetencyEntity competency : simulationVersionEntity.getCompetencies()) {
            weightsByCompetency.put(competency.getName(), competency.getWeight());
        }

        double referenceScore = calculateMaxPathScore(paths.getFirst(), weightsByCompetency);
        for (int i = 1; i < paths.size(); i++) {
            double pathScore = calculateMaxPathScore(paths.get(i), weightsByCompetency);
            if (Math.abs(pathScore - referenceScore) > praxisProperties.competencyWeightTolerance()) {
                SimulationNodeEntity terminalNode = paths.get(i).getLast();
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        terminalNode.getNodeId(),
                        "Um caminho do teste permite pontuação máxima diferente (caminho "
                                + (i + 1)
                                + ": "
                                + String.format("%.2f", pathScore)
                                + ", referência: "
                                + String.format("%.2f", referenceScore)
                                + "). Revise a pontuação das respostas neste caminho."
                ));
            }
        }
    }

    private void collectTerminalNodePaths(
            String nodeId,
            Map<String, SimulationNodeEntity> nodesById,
            Set<String> visiting,
            List<SimulationNodeEntity> currentPath,
            List<List<SimulationNodeEntity>> paths
    ) {
        SimulationNodeEntity node = nodesById.get(nodeId);
        if (node == null || !visiting.add(nodeId)) {
            return;
        }

        currentPath.add(node);
        List<String> nextNodeIds = nextNodeIds(node, nodesById);
        if (nextNodeIds.isEmpty()) {
            paths.add(new ArrayList<>(currentPath));
        } else {
            for (String nextNodeId : nextNodeIds) {
                collectTerminalNodePaths(nextNodeId, nodesById, visiting, currentPath, paths);
            }
        }

        currentPath.removeLast();
        visiting.remove(nodeId);
    }

    private double calculateMaxPathScore(List<SimulationNodeEntity> path, Map<String, Double> weightsByCompetency) {
        return path.stream()
                .mapToDouble(node -> calculateMaxNodeOptionScore(node, weightsByCompetency))
                .sum();
    }

    private double calculateMaxNodeOptionScore(SimulationNodeEntity node, Map<String, Double> weightsByCompetency) {
        OptionalDouble maxScore = node.getOptions().stream()
                .mapToDouble(option -> calculateOptionWeightedScore(option, weightsByCompetency))
                .max();
        return maxScore.orElse(0.0);
    }

    private double calculateOptionWeightedScore(SimulationOptionEntity option, Map<String, Double> weightsByCompetency) {
        return option.getCompetencyScores().stream()
                .mapToDouble(score -> score.getScore() * weightsByCompetency.getOrDefault(score.getCompetencyName(), 0.0))
                .sum();
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
                    "O teste passa do limite de " + MAX_DEPTH_TURNS + " etapas em um único caminho. Total atual: " + longestPath + "."
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
        for (String nextNodeId : nextNodeIds(node, nodesById)) {
            longestChild = Math.max(longestChild, longestPathLength(nextNodeId, nodesById, memo, visiting));
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
                    "Uma resposta leva de volta a uma etapa anterior e cria um caminho sem fim. Confira para onde cada resposta aponta."
            ));
            return;
        }

        SimulationNodeEntity node = nodesById.get(nodeId);
        if (node == null) {
            return;
        }

        visiting.add(nodeId);
        for (String nextNodeId : nextNodeIds(node, nodesById)) {
            visitForCycles(nextNodeId, nodesById, visiting, visited, issues);
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
                        "Existe uma etapa que nenhum candidato chega a ver, porque nenhuma resposta leva até ela."
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

        for (String nextNodeId : nextNodeIds(node, nodesById)) {
            collectReachable(nextNodeId, nodesById, reachable);
        }
    }

    private List<String> nextNodeIds(SimulationNodeEntity node, Map<String, SimulationNodeEntity> nodesById) {
        List<String> nextNodeIds = new ArrayList<>(node.getOptions().stream()
                .map(SimulationOptionEntity::getNextNodeId)
                .filter(nextNodeId -> nextNodeId != null && nodesById.containsKey(nextNodeId))
                .distinct()
                .toList());
        if (node.getTimeoutNextNodeId() != null && nodesById.containsKey(node.getTimeoutNextNodeId())
                && !nextNodeIds.contains(node.getTimeoutNextNodeId())) {
            nextNodeIds.add(node.getTimeoutNextNodeId());
        }
        return nextNodeIds;
    }
}
