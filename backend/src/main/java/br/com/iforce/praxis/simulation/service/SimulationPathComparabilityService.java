package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.simulation.dto.PathCompetencyMetricsResponse;
import br.com.iforce.praxis.simulation.dto.TerminalRouteResponse;
import br.com.iforce.praxis.simulation.dto.ValidationIssueResponse;
import br.com.iforce.praxis.simulation.model.ValidationIssueSeverity;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enumera rotas terminais e compara as oportunidades de evidência oferecidas
 * por cada ramificação. A análise usa somente o grafo versionado e, portanto,
 * é determinística e reproduzível durante publicação e auditoria.
 */
@Service
public class SimulationPathComparabilityService {

    private static final int MINIMUM_EVIDENCE_COUNT = 2;
    private static final int MAXIMUM_DECISION_DIFFERENCE = 2;
    private static final int MAXIMUM_DIFFICULTY_DIFFERENCE = 20;
    private static final int MINIMUM_DURATION_DIFFERENCE_SECONDS = 60;
    private static final double MAXIMUM_RELATIVE_DIFFERENCE = 0.25;
    private static final int MAXIMUM_ENUMERATED_ROUTES = 500;

    public PathComparabilityAnalysis analyze(SimulationVersionEntity version) {
        Map<String, SimulationNodeEntity> nodesById = new LinkedHashMap<>();
        for (SimulationNodeEntity node : version.getNodes()) {
            nodesById.putIfAbsent(node.getNodeId(), node);
        }

        String rootNodeId = version.getRootNodeId();
        if (rootNodeId == null || !nodesById.containsKey(rootNodeId)) {
            return new PathComparabilityAnalysis(List.of(), List.of());
        }

        List<SimulationCompetencyEntity> competencies = version.getCompetencies().stream()
                .sorted(Comparator.comparing(SimulationCompetencyEntity::getName))
                .toList();
        Set<String> convergenceNodeIds = findConvergenceNodeIds(version, nodesById);
        List<RouteDraft> drafts = new ArrayList<>();
        enumerate(
                rootNodeId,
                nodesById,
                competencies,
                convergenceNodeIds,
                new RouteAccumulator(competencies),
                new LinkedHashSet<>(),
                drafts
        );

        drafts.sort(Comparator.comparing(RouteDraft::stableKey));
        List<TerminalRouteResponse> routes = new ArrayList<>();
        for (int index = 0; index < drafts.size(); index++) {
            routes.add(toResponse("R" + (index + 1), drafts.get(index), competencies));
        }

        List<ValidationIssueResponse> issues = new ArrayList<>();
        appendCoverageIssues(routes, competencies, issues);
        appendComparabilityIssues(routes, competencies, issues);
        if (drafts.size() >= MAXIMUM_ENUMERATED_ROUTES) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    rootNodeId,
                    "A avaliação possui mais de " + MAXIMUM_ENUMERATED_ROUTES
                            + " rotas terminais. Reduza ramificações redundantes ou crie pontos de convergência."
            ));
        }

        return new PathComparabilityAnalysis(List.copyOf(routes), List.copyOf(issues));
    }

    private void enumerate(
            String nodeId,
            Map<String, SimulationNodeEntity> nodesById,
            List<SimulationCompetencyEntity> competencies,
            Set<String> convergenceNodeIds,
            RouteAccumulator accumulator,
            Set<String> visiting,
            List<RouteDraft> routes
    ) {
        if (routes.size() >= MAXIMUM_ENUMERATED_ROUTES || !visiting.add(nodeId)) {
            return;
        }

        SimulationNodeEntity node = nodesById.get(nodeId);
        if (node == null) {
            visiting.remove(nodeId);
            return;
        }

        RouteAccumulator atNode = accumulator.copy();
        atNode.addNode(nodeId, convergenceNodeIds.contains(nodeId));
        if (node.isFinal()) {
            routes.add(atNode.finish(nodeId));
            visiting.remove(nodeId);
            return;
        }

        List<RouteTransition> transitions = transitions(node);
        for (RouteTransition transition : transitions) {
            RouteAccumulator next = atNode.copy();
            next.addDecision(node, transition, competencies);
            if (transition.nextNodeId() == null) {
                routes.add(next.finish(transition.terminalId()));
            } else if (nodesById.containsKey(transition.nextNodeId())) {
                enumerate(
                        transition.nextNodeId(),
                        nodesById,
                        competencies,
                        convergenceNodeIds,
                        next,
                        new LinkedHashSet<>(visiting),
                        routes
                );
            }
        }
        visiting.remove(nodeId);
    }

    private List<RouteTransition> transitions(SimulationNodeEntity node) {
        Map<String, List<SimulationOptionEntity>> grouped = new LinkedHashMap<>();
        for (SimulationOptionEntity option : node.getOptions()) {
            String key = option.getNextNodeId() == null
                    ? "direct:" + option.getOptionId()
                    : "node:" + option.getNextNodeId();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(option);
        }

        List<RouteTransition> transitions = new ArrayList<>();
        for (Map.Entry<String, List<SimulationOptionEntity>> entry : grouped.entrySet()) {
            SimulationOptionEntity first = entry.getValue().get(0);
            String terminalId = first.getNextNodeId() == null
                    ? "FIM:" + node.getNodeId() + ":" + first.getOptionId()
                    : null;
            transitions.add(new RouteTransition(
                    entry.getKey(),
                    first.getNextNodeId(),
                    terminalId,
                    List.copyOf(entry.getValue()),
                    false
            ));
        }

        if (node.getTimeLimitSeconds() != null && node.getTimeLimitSeconds() > 0) {
            String terminalId = node.getTimeoutNextNodeId() == null
                    ? "FIM:" + node.getNodeId() + ":TIMEOUT"
                    : null;
            transitions.add(new RouteTransition(
                    "timeout:" + node.getNodeId(),
                    node.getTimeoutNextNodeId(),
                    terminalId,
                    List.of(),
                    true
            ));
        }
        return transitions;
    }

    private Set<String> findConvergenceNodeIds(
            SimulationVersionEntity version,
            Map<String, SimulationNodeEntity> nodesById
    ) {
        Map<String, Set<String>> incomingSources = new HashMap<>();
        for (SimulationNodeEntity node : version.getNodes()) {
            for (SimulationOptionEntity option : node.getOptions()) {
                if (option.getNextNodeId() != null && nodesById.containsKey(option.getNextNodeId())) {
                    incomingSources.computeIfAbsent(option.getNextNodeId(), ignored -> new HashSet<>())
                            .add(node.getNodeId());
                }
            }
            if (node.getTimeoutNextNodeId() != null && nodesById.containsKey(node.getTimeoutNextNodeId())) {
                incomingSources.computeIfAbsent(node.getTimeoutNextNodeId(), ignored -> new HashSet<>())
                        .add(node.getNodeId());
            }
        }

        Set<String> convergenceNodeIds = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : incomingSources.entrySet()) {
            if (entry.getValue().size() > 1) {
                convergenceNodeIds.add(entry.getKey());
            }
        }
        return convergenceNodeIds;
    }

    private TerminalRouteResponse toResponse(
            String routeId,
            RouteDraft draft,
            List<SimulationCompetencyEntity> competencies
    ) {
        List<PathCompetencyMetricsResponse> competencyMetrics = competencies.stream()
                .map(competency -> {
                    CompetencyAccumulator metrics = draft.competencies().get(competency.getName());
                    return new PathCompetencyMetricsResponse(
                            competency.getName(),
                            metrics.minimumScore(),
                            metrics.maximumScore(),
                            metrics.evidenceCount(),
                            metrics.evidenceCount() > 0
                    );
                })
                .toList();

        int rawMinimumScore = competencyMetrics.stream()
                .mapToInt(PathCompetencyMetricsResponse::minimumScore)
                .sum();
        int rawMaximumScore = competencyMetrics.stream()
                .mapToInt(PathCompetencyMetricsResponse::maximumScore)
                .sum();
        double observableWeight = competencies.stream()
                .filter(competency -> draft.competencies().get(competency.getName()).evidenceCount() > 0)
                .mapToDouble(SimulationCompetencyEntity::getWeight)
                .sum();
        int maximumNormalizedScore = (int) Math.round(Math.min(1.0, observableWeight) * 100.0);
        int difficulty = draft.decisionCount() == 0
                ? 0
                : (int) Math.round(draft.difficultySum() / draft.decisionCount());

        return new TerminalRouteResponse(
                routeId,
                draft.terminalNodeId(),
                draft.nodeIds(),
                draft.convergenceNodeIds(),
                draft.decisionCount(),
                draft.estimatedDurationSeconds(),
                Math.max(0, Math.min(100, difficulty)),
                rawMinimumScore,
                rawMaximumScore,
                maximumNormalizedScore,
                competencyMetrics
        );
    }

    private void appendCoverageIssues(
            List<TerminalRouteResponse> routes,
            List<SimulationCompetencyEntity> competencies,
            List<ValidationIssueResponse> issues
    ) {
        for (TerminalRouteResponse route : routes) {
            Map<String, PathCompetencyMetricsResponse> metricsByName = new HashMap<>();
            for (PathCompetencyMetricsResponse metric : route.competencies()) {
                metricsByName.put(metric.competency(), metric);
            }
            for (SimulationCompetencyEntity competency : competencies) {
                if (competency.getWeight() <= 0) {
                    continue;
                }
                PathCompetencyMetricsResponse metric = metricsByName.get(competency.getName());
                int evidenceCount = metric == null ? 0 : metric.evidenceCount();
                if (evidenceCount == 0) {
                    issues.add(new ValidationIssueResponse(
                            ValidationIssueSeverity.BLOCKER,
                            route.terminalNodeId(),
                            "A rota " + route.routeId() + " termina sem observar a competência obrigatória \""
                                    + competency.getName() + "\". Inclua pelo menos uma decisão que gere evidência dessa competência."
                    ));
                } else if (evidenceCount < MINIMUM_EVIDENCE_COUNT) {
                    issues.add(new ValidationIssueResponse(
                            ValidationIssueSeverity.WARNING,
                            route.terminalNodeId(),
                            "A rota " + route.routeId() + " possui somente " + evidenceCount
                                    + " evidência para a competência \"" + competency.getName()
                                    + "\". O recomendado é pelo menos " + MINIMUM_EVIDENCE_COUNT + "."
                    ));
                }
            }
        }
    }

    private void appendComparabilityIssues(
            List<TerminalRouteResponse> routes,
            List<SimulationCompetencyEntity> competencies,
            List<ValidationIssueResponse> issues
    ) {
        if (routes.size() < 2) {
            return;
        }

        int minimumDuration = routes.stream().mapToInt(TerminalRouteResponse::estimatedDurationSeconds).min().orElse(0);
        int maximumDuration = routes.stream().mapToInt(TerminalRouteResponse::estimatedDurationSeconds).max().orElse(0);
        if (exceedsAllowedDifference(minimumDuration, maximumDuration, MINIMUM_DURATION_DIFFERENCE_SECONDS)) {
            issues.add(globalWarning(
                    "As rotas têm duração estimada muito diferente: de " + minimumDuration + "s a "
                            + maximumDuration + "s. Ajuste tempos ou crie um ponto de convergência."
            ));
        }

        int minimumDecisions = routes.stream().mapToInt(TerminalRouteResponse::decisionCount).min().orElse(0);
        int maximumDecisions = routes.stream().mapToInt(TerminalRouteResponse::decisionCount).max().orElse(0);
        if (maximumDecisions - minimumDecisions > MAXIMUM_DECISION_DIFFERENCE) {
            issues.add(globalWarning(
                    "As rotas oferecem quantidades muito diferentes de decisões: de " + minimumDecisions
                            + " a " + maximumDecisions + ". Revise a equivalência de oportunidades de evidência."
            ));
        }

        int minimumDifficulty = routes.stream().mapToInt(TerminalRouteResponse::estimatedDifficultyPercent).min().orElse(0);
        int maximumDifficulty = routes.stream().mapToInt(TerminalRouteResponse::estimatedDifficultyPercent).max().orElse(0);
        if (maximumDifficulty - minimumDifficulty > MAXIMUM_DIFFICULTY_DIFFERENCE) {
            issues.add(globalWarning(
                    "A dificuldade estimada varia de " + minimumDifficulty + "% a " + maximumDifficulty
                            + "% entre rotas. Revise alternativas e critérios de pontuação."
            ));
        }

        int minimumRawMaximum = routes.stream().mapToInt(TerminalRouteResponse::rawMaximumScore).min().orElse(0);
        int maximumRawMaximum = routes.stream().mapToInt(TerminalRouteResponse::rawMaximumScore).max().orElse(0);
        if (exceedsAllowedDifference(minimumRawMaximum, maximumRawMaximum, 25)) {
            issues.add(globalWarning(
                    "Candidatos podem alcançar tetos brutos diferentes apenas pelo caminho seguido: de "
                            + minimumRawMaximum + " a " + maximumRawMaximum
                            + " pontos. A nota final é normalizada por caminho, mas a estrutura deve ser revisada."
            ));
        }

        for (SimulationCompetencyEntity competency : competencies) {
            int minimum = Integer.MAX_VALUE;
            int maximum = Integer.MIN_VALUE;
            for (TerminalRouteResponse route : routes) {
                PathCompetencyMetricsResponse metric = route.competencies().stream()
                        .filter(candidate -> candidate.competency().equals(competency.getName()))
                        .findFirst()
                        .orElse(null);
                int routeMaximum = metric == null ? 0 : metric.maximumScore();
                minimum = Math.min(minimum, routeMaximum);
                maximum = Math.max(maximum, routeMaximum);
            }
            if (minimum != Integer.MAX_VALUE && exceedsAllowedDifference(minimum, maximum, 20)) {
                issues.add(globalWarning(
                        "O teto bruto da competência \"" + competency.getName() + "\" varia de "
                                + minimum + " a " + maximum + " pontos entre rotas."
                ));
            }
        }
    }

    private boolean exceedsAllowedDifference(int minimum, int maximum, int absoluteFloor) {
        int difference = maximum - minimum;
        int relativeLimit = (int) Math.round(maximum * MAXIMUM_RELATIVE_DIFFERENCE);
        return difference > Math.max(absoluteFloor, relativeLimit);
    }

    private ValidationIssueResponse globalWarning(String message) {
        return new ValidationIssueResponse(ValidationIssueSeverity.WARNING, null, message);
    }

    private int estimateDurationSeconds(SimulationNodeEntity node) {
        if (node.getTimeLimitSeconds() != null && node.getTimeLimitSeconds() > 0) {
            return node.getTimeLimitSeconds();
        }
        int words = countWords(node.getMessage());
        for (SimulationOptionEntity option : node.getOptions()) {
            words += countWords(option.getText());
        }
        return Math.max(10, (int) Math.ceil(words * 0.3) + 5);
    }

    private int countWords(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return value.trim().split("\\s+").length;
    }

    private double transitionDifficulty(
            RouteTransition transition,
            List<SimulationCompetencyEntity> competencies
    ) {
        if (transition.timeout() || transition.options().isEmpty()) {
            return 100.0;
        }
        double bestWeightedScore = transition.options().stream()
                .mapToDouble(option -> weightedOptionScore(option, competencies))
                .max()
                .orElse(0.0);
        return 100.0 - Math.max(0.0, Math.min(100.0, bestWeightedScore));
    }

    private double weightedOptionScore(
            SimulationOptionEntity option,
            List<SimulationCompetencyEntity> competencies
    ) {
        double score = 0.0;
        for (SimulationCompetencyEntity competency : competencies) {
            score += score(option, competency.getName()) * competency.getWeight();
        }
        return score;
    }

    private int score(SimulationOptionEntity option, String competencyName) {
        return option.getCompetencyScores().stream()
                .filter(candidate -> candidate.getCompetencyName().equals(competencyName))
                .mapToInt(OptionCompetencyScoreEntity::getScore)
                .findFirst()
                .orElse(0);
    }

    private boolean hasEvidence(SimulationOptionEntity option, String competencyName) {
        return option.getCompetencyScores().stream()
                .anyMatch(candidate -> candidate.getCompetencyName().equals(competencyName));
    }

    public record PathComparabilityAnalysis(
            List<TerminalRouteResponse> routes,
            List<ValidationIssueResponse> issues
    ) {
    }

    private record RouteTransition(
            String key,
            String nextNodeId,
            String terminalId,
            List<SimulationOptionEntity> options,
            boolean timeout
    ) {
    }

    private record CompetencyAccumulator(int minimumScore, int maximumScore, int evidenceCount) {

        CompetencyAccumulator add(int minimum, int maximum, boolean evidence) {
            return new CompetencyAccumulator(
                    minimumScore + minimum,
                    maximumScore + maximum,
                    evidenceCount + (evidence ? 1 : 0)
            );
        }
    }

    private record RouteDraft(
            String terminalNodeId,
            List<String> nodeIds,
            List<String> convergenceNodeIds,
            int decisionCount,
            int estimatedDurationSeconds,
            double difficultySum,
            Map<String, CompetencyAccumulator> competencies
    ) {
        String stableKey() {
            return String.join(">", nodeIds) + ">" + terminalNodeId;
        }
    }

    private final class RouteAccumulator {

        private final List<String> nodeIds;
        private final List<String> convergenceNodeIds;
        private final Map<String, CompetencyAccumulator> competencies;
        private int decisionCount;
        private int estimatedDurationSeconds;
        private double difficultySum;

        private RouteAccumulator(List<SimulationCompetencyEntity> configuredCompetencies) {
            this.nodeIds = new ArrayList<>();
            this.convergenceNodeIds = new ArrayList<>();
            this.competencies = new LinkedHashMap<>();
            for (SimulationCompetencyEntity competency : configuredCompetencies) {
                competencies.put(competency.getName(), new CompetencyAccumulator(0, 0, 0));
            }
        }

        private RouteAccumulator(RouteAccumulator source) {
            this.nodeIds = new ArrayList<>(source.nodeIds);
            this.convergenceNodeIds = new ArrayList<>(source.convergenceNodeIds);
            this.competencies = new LinkedHashMap<>(source.competencies);
            this.decisionCount = source.decisionCount;
            this.estimatedDurationSeconds = source.estimatedDurationSeconds;
            this.difficultySum = source.difficultySum;
        }

        private RouteAccumulator copy() {
            return new RouteAccumulator(this);
        }

        private void addNode(String nodeId, boolean convergencePoint) {
            nodeIds.add(nodeId);
            if (convergencePoint) {
                convergenceNodeIds.add(nodeId);
            }
        }

        private void addDecision(
                SimulationNodeEntity node,
                RouteTransition transition,
                List<SimulationCompetencyEntity> configuredCompetencies
        ) {
            decisionCount++;
            estimatedDurationSeconds += estimateDurationSeconds(node);
            difficultySum += transitionDifficulty(transition, configuredCompetencies);

            for (SimulationCompetencyEntity competency : configuredCompetencies) {
                List<Integer> scores = transition.timeout()
                        ? List.of(0)
                        : transition.options().stream()
                                .map(option -> score(option, competency.getName()))
                                .toList();
                int minimum = scores.stream().mapToInt(Integer::intValue).min().orElse(0);
                int maximum = scores.stream().mapToInt(Integer::intValue).max().orElse(0);
                boolean evidence = !transition.timeout() && transition.options().stream()
                        .anyMatch(option -> hasEvidence(option, competency.getName()));
                competencies.computeIfPresent(
                        competency.getName(),
                        (ignored, current) -> current.add(minimum, maximum, evidence)
                );
            }
        }

        private RouteDraft finish(String terminalNodeId) {
            return new RouteDraft(
                    terminalNodeId,
                    List.copyOf(nodeIds),
                    List.copyOf(convergenceNodeIds),
                    decisionCount,
                    estimatedDurationSeconds,
                    difficultySum,
                    Map.copyOf(competencies)
            );
        }
    }
}
