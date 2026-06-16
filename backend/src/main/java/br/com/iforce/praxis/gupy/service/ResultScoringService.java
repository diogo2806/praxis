package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ResultDecision;
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

/**
 * Cálculo determinístico do score, normalizado pelo caminho percorrido e ponderado pelo peso de
 * cada competência (com renormalização das competências presentes). Nenhuma chamada a IA: a nota é
 * função pura das opções escolhidas e do grafo publicado.
 *
 * <pre>
 *   raw(c)  = Σ pontos das opções escolhidas que pontuam em c
 *   max(c)  = Σ, nó a nó no MESMO caminho, do maior ponto possível em c naquele nó
 *   norm(c) = raw(c) / max(c)                    (ignora c com max == 0)
 *   final   = round( Σ ( norm(c) × weightRenormalizado(c) ) × 100 )
 * </pre>
 */
@Service
public class ResultScoringService {

    private static final String POLICY_ADHERENCE_COMPETENCY = "aderencia a politica";

    private final PraxisProperties praxisProperties;

    public ResultScoringService(PraxisProperties praxisProperties) {
        this.praxisProperties = praxisProperties;
    }

    public ScoreCalculationResult calculate(
            PublishedSimulation simulation,
            Map<String, AttemptAnswer> answersByNodeId
    ) {
        List<PathStep> path = walkPath(simulation, answersByNodeId);

        boolean humanReviewRequired = path.stream()
                .map(PathStep::pickedOption)
                .anyMatch(option -> option != null && option.critical());

        String auditTrail = path.stream()
                .map(PathStep::auditNote)
                .reduce((left, right) -> left + " | " + right)
                .orElse("");

        Map<String, Integer> raw = new LinkedHashMap<>();
        Map<String, Integer> max = new LinkedHashMap<>();
        for (String competency : simulation.competencies()) {
            raw.put(competency, 0);
            max.put(competency, 0);
        }

        for (PathStep step : path) {
            for (String competency : simulation.competencies()) {
                int bestAtNode = step.node().options().stream()
                        .mapToInt(option -> option.competencyScores().getOrDefault(competency, 0))
                        .max()
                        .orElse(0);
                max.merge(competency, bestAtNode, Integer::sum);

                int gained = step.pickedOption() == null
                        ? 0
                        : step.pickedOption().competencyScores().getOrDefault(competency, 0);
                raw.merge(competency, gained, Integer::sum);
            }
        }

        Map<String, Double> normalizedByCompetency = new LinkedHashMap<>();
        for (String competency : simulation.competencies()) {
            if (max.get(competency) > 0) {
                normalizedByCompetency.put(competency, (double) raw.get(competency) / max.get(competency));
            }
        }

        double weightSum = normalizedByCompetency.keySet().stream()
                .mapToDouble(competency -> simulation.competencyWeights().getOrDefault(competency, 0.0))
                .sum();

        double weighted = 0.0;
        List<ResultItem> resultItems = new ArrayList<>();
        for (Map.Entry<String, Double> entry : normalizedByCompetency.entrySet()) {
            String competency = entry.getKey();
            double renormalizedWeight = weightSum == 0
                    ? 0.0
                    : simulation.competencyWeights().getOrDefault(competency, 0.0) / weightSum;
            weighted += entry.getValue() * renormalizedWeight;
            resultItems.add(new ResultItem(
                    competency,
                    (int) Math.round(entry.getValue() * 100),
                    tierFor(competency)
            ));
        }

        int finalScore = (int) Math.round(weighted * 100);
        ResultDecision decision = deriveDecision(finalScore, humanReviewRequired);

        return new ScoreCalculationResult(finalScore, resultItems, humanReviewRequired, auditTrail, decision);
    }

    /**
     * Deriva a decisão a partir da regra: erro crítico sempre vence (REVIEW_REQUIRED); senão,
     * score &gt;= limiar configurável → RECOMMEND_INTERVIEW; senão, decisão neutra. Nunca existe
     * reprovação automática.
     */
    public ResultDecision deriveDecision(int finalScore, boolean humanReviewRequired) {
        if (humanReviewRequired) {
            return ResultDecision.REVIEW_REQUIRED;
        }
        if (finalScore >= praxisProperties.recommendInterviewThreshold()) {
            return ResultDecision.RECOMMEND_INTERVIEW;
        }
        return ResultDecision.IN_PROGRESS;
    }

    private List<PathStep> walkPath(
            PublishedSimulation simulation,
            Map<String, AttemptAnswer> answersByNodeId
    ) {
        List<PathStep> path = new ArrayList<>();
        String currentNodeId = simulation.rootNodeId();

        while (currentNodeId != null) {
            ScenarioNode currentNode = findNode(simulation, currentNodeId);
            AttemptAnswer answer = answersByNodeId.get(currentNodeId);
            if (answer == null) {
                break;
            }

            if (answer.timedOut() || answer.optionId() == null) {
                // Timeout do turno: nível 0 naquele nó e o caminho termina ali.
                path.add(new PathStep(currentNode, null));
                break;
            }

            ScenarioOption pickedOption = findOption(currentNode, answer.optionId());
            path.add(new PathStep(currentNode, pickedOption));
            currentNodeId = pickedOption.nextNodeId();
        }

        return path;
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

    private ResultTier tierFor(String competencyName) {
        if (POLICY_ADHERENCE_COMPETENCY.equals(normalize(competencyName))) {
            return ResultTier.MINOR;
        }
        return ResultTier.MAJOR;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private record PathStep(ScenarioNode node, ScenarioOption pickedOption) {

        String auditNote() {
            return pickedOption == null
                    ? "Turno " + node.id() + " sem resposta (timeout)."
                    : pickedOption.auditNote();
        }
    }
}
