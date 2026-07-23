package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calcula os valores auditáveis que acompanham a nota normalizada. O snapshot
 * é gravado na conclusão da tentativa e nunca é recalculado a partir de uma
 * versão posterior da avaliação.
 */
@Service
public class PathScoringSnapshotService {

    public static final String ALGORITHM_VERSION = "path-normalized-v2";

    public ScoringSnapshot calculate(
            PublishedSimulation simulation,
            Map<String, AttemptAnswer> answersByNodeId,
            int normalizedScore
    ) {
        Map<String, Integer> rawByCompetency = new LinkedHashMap<>();
        Map<String, Integer> maximumByCompetency = new LinkedHashMap<>();
        for (String competency : simulation.competencies()) {
            rawByCompetency.put(competency, 0);
            maximumByCompetency.put(competency, 0);
        }

        String currentNodeId = simulation.rootNodeId();
        while (currentNodeId != null) {
            ScenarioNode node = findNode(simulation, currentNodeId);
            if (node.isFinal()) {
                break;
            }

            AttemptAnswer answer = answersByNodeId.get(currentNodeId);
            if (answer == null) {
                break;
            }

            for (String competency : simulation.competencies()) {
                int bestAtNode = node.options().stream()
                        .mapToInt(option -> option.competencyScores().getOrDefault(competency, 0))
                        .max()
                        .orElse(0);
                maximumByCompetency.merge(competency, bestAtNode, Integer::sum);
            }

            if (answer.timedOut() || answer.optionId() == null) {
                currentNodeId = node.timeoutNextNodeId();
                continue;
            }

            ScenarioOption option = findOption(node, answer.optionId());
            for (String competency : simulation.competencies()) {
                rawByCompetency.merge(
                        competency,
                        option.competencyScores().getOrDefault(competency, 0),
                        Integer::sum
                );
            }
            currentNodeId = option.nextNodeId();
        }

        int rawScore = rawByCompetency.values().stream().mapToInt(Integer::intValue).sum();
        int pathMaximumScore = maximumByCompetency.values().stream().mapToInt(Integer::intValue).sum();
        return new ScoringSnapshot(
                rawScore,
                pathMaximumScore,
                normalizedScore,
                ALGORITHM_VERSION
        );
    }

    private ScenarioNode findNode(PublishedSimulation simulation, String nodeId) {
        return simulation.nodes().stream()
                .filter(node -> node.id().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Estado interno inválido."));
    }

    private ScenarioOption findOption(ScenarioNode node, String optionId) {
        return node.options().stream()
                .filter(option -> option.id().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Estado interno inválido."));
    }

    public record ScoringSnapshot(
            int rawScore,
            int pathMaximumScore,
            int normalizedScore,
            String algorithmVersion
    ) {
    }
}
