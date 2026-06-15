package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.gupy.model.ScoreCalculationResult;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResultScoringService {

    public ScoreCalculationResult calculate(
            PublishedSimulation simulation,
            Map<String, AttemptAnswer> answersByNodeId
    ) {
        List<ScenarioOption> selectedOptions = collectSelectedOptions(simulation, answersByNodeId);
        Map<String, List<Integer>> scoresByCompetency = initializeCompetencyScores(simulation);
        List<String> auditNotes = new ArrayList<>();
        boolean humanReviewRequired = false;

        for (ScenarioOption selectedOption : selectedOptions) {
            humanReviewRequired = humanReviewRequired || selectedOption.critical();
            auditNotes.add(selectedOption.auditNote());

            for (Map.Entry<String, Integer> scoreEntry : selectedOption.competencyScores().entrySet()) {
                scoresByCompetency.computeIfAbsent(scoreEntry.getKey(), key -> new ArrayList<>())
                        .add(scoreEntry.getValue());
            }
        }

        List<ResultItem> resultItems = toResultItems(scoresByCompetency);
        int score = calculateOverallScore(resultItems);

        return new ScoreCalculationResult(
                score,
                resultItems,
                humanReviewRequired,
                String.join(" | ", auditNotes)
        );
    }

    private List<ScenarioOption> collectSelectedOptions(
            PublishedSimulation simulation,
            Map<String, AttemptAnswer> answersByNodeId
    ) {
        List<ScenarioOption> selectedOptions = new ArrayList<>();
        String currentNodeId = simulation.rootNodeId();

        while (currentNodeId != null) {
            ScenarioNode currentNode = findNode(simulation, currentNodeId);
            AttemptAnswer answer = answersByNodeId.get(currentNodeId);
            if (answer == null) {
                break;
            }

            ScenarioOption selectedOption = findOption(currentNode, answer.optionId());
            selectedOptions.add(selectedOption);
            currentNodeId = selectedOption.nextNodeId();
        }

        return selectedOptions;
    }

    private ScenarioNode findNode(PublishedSimulation simulation, String nodeId) {
        return simulation.nodes().stream()
                .filter(node -> node.id().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Grafo da simulacao invalido."));
    }

    private ScenarioOption findOption(ScenarioNode node, String optionId) {
        return node.options().stream()
                .filter(option -> option.id().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Resposta salva aponta para alternativa invalida."));
    }

    private Map<String, List<Integer>> initializeCompetencyScores(PublishedSimulation simulation) {
        Map<String, List<Integer>> scoresByCompetency = new LinkedHashMap<>();
        for (String competency : simulation.competencies()) {
            scoresByCompetency.put(competency, new ArrayList<>());
        }
        return scoresByCompetency;
    }

    private List<ResultItem> toResultItems(Map<String, List<Integer>> scoresByCompetency) {
        List<ResultItem> resultItems = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : scoresByCompetency.entrySet()) {
            resultItems.add(new ResultItem(
                    entry.getKey(),
                    calculateCompetencyScore(entry.getValue()),
                    tierFor(entry.getKey())
            ));
        }
        return resultItems;
    }

    private int calculateCompetencyScore(List<Integer> scores) {
        if (scores.isEmpty()) {
            return 0;
        }

        int total = scores.stream()
                .mapToInt(Integer::intValue)
                .sum();

        return Math.round((float) total / scores.size());
    }

    private int calculateOverallScore(List<ResultItem> resultItems) {
        if (resultItems.isEmpty()) {
            return 0;
        }

        int total = resultItems.stream()
                .mapToInt(ResultItem::score)
                .sum();

        return Math.round((float) total / resultItems.size());
    }

    private ResultTier tierFor(String competencyName) {
        if ("aderencia a politica".equals(normalize(competencyName))) {
            return ResultTier.MINOR;
        }
        return ResultTier.MAJOR;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
