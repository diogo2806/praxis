package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ReliabilityLevel;
import br.com.iforce.praxis.gupy.model.ResultDecision;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.gupy.model.ScoreCalculationResult;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
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
    private static final double FAST_RESPONSE_THRESHOLD_RATIO = 0.20;
    private static final double HIGH_SCORE_OPTION_RATIO = 0.80;
    private static final long MILLIS_PER_VISIBLE_WORD = 300L;
    private static final long MIN_REASONABLE_RESPONSE_MILLIS = 5_000L;

    private final PraxisProperties praxisProperties;

    public ResultScoringService(PraxisProperties praxisProperties) {
        this.praxisProperties = praxisProperties;
    }

    /**
     * Calcula a pontuação final de uma prova a partir das respostas.
     *
     * <p>Versão simplificada, sem informação de tempo das respostas (não
     * avalia o sinal de "respostas rápidas demais"). Veja a sobrecarga com
     * {@code firstTurnReceivedAt} para o cálculo completo.</p>
     *
     * @param simulation a prova respondida (cenário e pesos das competências)
     * @param answersByNodeId as respostas do candidato, por etapa
     * @return a pontuação final, o detalhamento por competência e a decisão sugerida
     */
    public ScoreCalculationResult calculate(
            PublishedSimulation simulation,
            Map<String, AttemptAnswer> answersByNodeId
    ) {
        return calculate(simulation, answersByNodeId, null);
    }

    /**
     * Calcula a pontuação final de uma prova de forma determinística.
     *
     * <p>Fluxo do processo: percorre o caminho que o candidato realmente
     * seguiu, soma os pontos obtidos em cada competência e compara com o
     * máximo possível naquele mesmo caminho, ponderando pelo peso de cada
     * competência. O resultado é uma nota de 0 a 100, sem nenhuma IA: dadas as
     * mesmas respostas e a mesma prova, o resultado é sempre idêntico. Também
     * sinaliza se uma revisão humana é obrigatória (erro crítico) e se há
     * indício de baixa confiabilidade (respostas rápidas demais em pontos
     * decisivos). Nunca há reprovação automática.</p>
     *
     * @param simulation a prova respondida (cenário e pesos das competências)
     * @param answersByNodeId as respostas do candidato, por etapa
     * @param firstTurnReceivedAt momento em que a primeira etapa foi exibida,
     *                            usado para avaliar o tempo de resposta (pode ser nulo)
     * @return a pontuação final, o detalhamento por competência, a necessidade
     *         de revisão humana, o nível de confiabilidade e a decisão sugerida
     */
    public ScoreCalculationResult calculate(
            PublishedSimulation simulation,
            Map<String, AttemptAnswer> answersByNodeId,
            Instant firstTurnReceivedAt
    ) {
        List<PathStep> path = walkPath(simulation, answersByNodeId, firstTurnReceivedAt);

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
        ReliabilityLevel reliabilityLevel = hasLowReliabilitySignal(path, simulation.competencies())
                ? ReliabilityLevel.LOW_RELIABILITY
                : ReliabilityLevel.NORMAL;

        return new ScoreCalculationResult(finalScore, resultItems, humanReviewRequired, reliabilityLevel, auditTrail, decision);
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
            Map<String, AttemptAnswer> answersByNodeId,
            Instant firstTurnReceivedAt
    ) {
        List<PathStep> path = new ArrayList<>();
        String currentNodeId = simulation.rootNodeId();
        Instant currentTurnReceivedAt = firstTurnReceivedAt;

        while (currentNodeId != null) {
            ScenarioNode currentNode = findNode(simulation, currentNodeId);
            if (currentNode.isFinal()) {
                break;
            }
            AttemptAnswer answer = answersByNodeId.get(currentNodeId);
            if (answer == null) {
                break;
            }

            if (answer.timedOut() || answer.optionId() == null) {
                // Timeout do turno: nível 0 naquele nó e continuidade pela rota configurada.
                path.add(new PathStep(currentNode, null, answer, currentTurnReceivedAt));
                currentTurnReceivedAt = answer.answeredAt();
                currentNodeId = currentNode.timeoutNextNodeId();
                continue;
            }

            ScenarioOption pickedOption = findOption(currentNode, answer.optionId());
            path.add(new PathStep(currentNode, pickedOption, answer, currentTurnReceivedAt));
            currentTurnReceivedAt = answer.answeredAt();
            currentNodeId = pickedOption.nextNodeId();
        }

        return path;
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

    private boolean hasLowReliabilitySignal(List<PathStep> path, List<String> competencies) {
        return path.stream()
                .anyMatch(step -> isSuspiciousFastCriticalAnswer(step, competencies));
    }

    private boolean isSuspiciousFastCriticalAnswer(PathStep step, List<String> competencies) {
        if (step.pickedOption() == null
                || step.answer() == null
                || step.answer().answeredAt() == null
                || step.turnReceivedAt() == null
                || !isCriticalForReliability(step.node(), step.pickedOption(), competencies)) {
            return false;
        }

        long elapsedMillis = Duration.between(step.turnReceivedAt(), step.answer().answeredAt()).toMillis();
        if (elapsedMillis < 0) {
            return false;
        }

        long reasonableMinimumMillis = reasonableMinimumMillis(step.node());
        return elapsedMillis < Math.round(reasonableMinimumMillis * FAST_RESPONSE_THRESHOLD_RATIO);
    }

    private boolean isCriticalForReliability(
            ScenarioNode node,
            ScenarioOption pickedOption,
            List<String> competencies
    ) {
        if (pickedOption.critical()) {
            return true;
        }

        for (String competency : competencies) {
            int bestAtNode = node.options().stream()
                    .mapToInt(option -> option.competencyScores().getOrDefault(competency, 0))
                    .max()
                    .orElse(0);
            if (bestAtNode > 0
                    && pickedOption.competencyScores().getOrDefault(competency, 0) >= Math.ceil(bestAtNode * HIGH_SCORE_OPTION_RATIO)) {
                return true;
            }
        }
        return false;
    }

    private long reasonableMinimumMillis(ScenarioNode node) {
        int visibleWords = countWords(node.message());
        for (ScenarioOption option : node.options()) {
            visibleWords += countWords(option.text());
        }
        return Math.max(MIN_REASONABLE_RESPONSE_MILLIS, visibleWords * MILLIS_PER_VISIBLE_WORD);
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private record PathStep(
            ScenarioNode node,
            ScenarioOption pickedOption,
            AttemptAnswer answer,
            Instant turnReceivedAt
    ) {

        String auditNote() {
            return pickedOption == null
                    ? "Turno " + node.id() + " sem resposta (timeout)."
                    : pickedOption.auditNote();
        }
    }
}
