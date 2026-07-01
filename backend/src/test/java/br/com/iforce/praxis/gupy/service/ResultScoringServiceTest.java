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

import org.junit.jupiter.api.Test;


import java.time.Instant;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;


import static org.assertj.core.api.Assertions.assertThat;


class ResultScoringServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private final ResultScoringService resultScoringService = new ResultScoringService(
            new PraxisProperties("http://localhost:8080", 168, 24, 70, 15, 0.001)
    );

    @Test
    void test1_equalCeilingsAcrossDifferentLengthPathsBothReachOneHundred() {
        // Caminho curto (1 turno) e caminho longo (3 turnos): escolhendo sempre a melhor opção,
        // ambos devem normalizar para 100, provando que o teto é igual entre caminhos.
        PublishedSimulation shortPath = linearDominantSimulation(1);
        PublishedSimulation longPath = linearDominantSimulation(3);

        ScoreCalculationResult shortResult = resultScoringService.calculate(shortPath, bestChoices(1));
        ScoreCalculationResult longResult = resultScoringService.calculate(longPath, bestChoices(3));

        assertThat(shortResult.score()).isEqualTo(100);
        assertThat(longResult.score()).isEqualTo(100);
        assertThat(longResult.decision()).isEqualTo(ResultDecision.RECOMMEND_INTERVIEW);
    }

    @Test
    void test2_pickingHalfOfPossiblePointsYieldsFiftyPercentForCompetency() {
        PublishedSimulation simulation = singleNodeSimulation(
                List.of("Empatia"),
                Map.of("Empatia", 1.0),
                option("opcao-cheia", null, Map.of("Empatia", 100), false),
                option("opcao-metade", null, Map.of("Empatia", 50), false)
        );

        ScoreCalculationResult result = resultScoringService.calculate(
                simulation, Map.of("turno-1", AttemptAnswer.answered("turno-1", "opcao-metade", T0)));

        assertThat(result.score()).isEqualTo(50);
        assertThat(result.resultItems()).singleElement()
                .extracting(ResultItem::name, ResultItem::score)
                .containsExactly("Empatia", 50);
        assertThat(result.decision()).isEqualTo(ResultDecision.NO_RECOMMENDATION);
    }

    @Test
    void test3_weightsApplyToFinalScore() {
        // Empatia peso 0.4 nota 100, Resolução peso 0.6 nota 0 → final = 40.
        PublishedSimulation simulation = singleNodeSimulation(
                List.of("Empatia", "Resolução"),
                Map.of("Empatia", 0.4, "Resolução", 0.6),
                option("opcao-empatica", null, Map.of("Empatia", 100, "Resolução", 0), false),
                option("opcao-resolutiva", null, Map.of("Empatia", 0, "Resolução", 100), false)
        );

        ScoreCalculationResult result = resultScoringService.calculate(
                simulation, Map.of("turno-1", AttemptAnswer.answered("turno-1", "opcao-empatica", T0)));

        assertThat(result.score()).isEqualTo(40);
    }

    @Test
    void resultItemTierComesFromPublishedSimulationConfiguration() {
        PublishedSimulation simulation = new PublishedSimulation(
                12L, 1, "sim-single", "Single", "Descricao",
                List.of("Empatia"),
                Map.of("Empatia", 1.0),
                Map.of("Empatia", ResultTier.MINOR),
                "turno-1",
                List.of(new ScenarioNode("turno-1", 1, "Cliente", "Mensagem", 30, List.of(
                        option("opcao-alta", null, Map.of("Empatia", 100), false),
                        option("opcao-baixa", null, Map.of("Empatia", 20), false)
                )))
        );

        ScoreCalculationResult result = resultScoringService.calculate(
                simulation, Map.of("turno-1", AttemptAnswer.answered("turno-1", "opcao-alta", T0)));

        assertThat(result.resultItems()).singleElement()
                .extracting(ResultItem::tier)
                .isEqualTo(ResultTier.MINOR);
    }

    @Test
    void test4_weightsAreRenormalizedWhenCompetencyIsAbsentFromPath() {
        // X (peso 0.3) não aparece no caminho; Empatia (0.4) e Resolução (0.3) presentes em 100%.
        // Sem renormalização daria 70; com renormalização os pesos presentes somam 1.0 → 100.
        PublishedSimulation simulation = singleNodeSimulation(
                List.of("Liderança", "Empatia", "Resolução"),
                Map.of("Liderança", 0.3, "Empatia", 0.4, "Resolução", 0.3),
                option("opcao-boa", null, Map.of("Empatia", 50, "Resolução", 50), false),
                option("opcao-fraca", null, Map.of("Empatia", 10, "Resolução", 10), false)
        );

        ScoreCalculationResult result = resultScoringService.calculate(
                simulation, Map.of("turno-1", AttemptAnswer.answered("turno-1", "opcao-boa", T0)));

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.resultItems()).extracting(ResultItem::name)
                .containsExactlyInAnyOrder("Empatia", "Resolução");
    }

    @Test
    void test5_criticalOptionRequiresHumanReviewWithoutZeroingScore() {
        PublishedSimulation simulation = singleNodeSimulation(
                List.of("Empatia"),
                Map.of("Empatia", 1.0),
                option("opcao-critica", null, Map.of("Empatia", 80), true),
                option("opcao-ok", null, Map.of("Empatia", 100), false)
        );

        ScoreCalculationResult result = resultScoringService.calculate(
                simulation, Map.of("turno-1", AttemptAnswer.answered("turno-1", "opcao-critica", T0)));

        assertThat(result.humanReviewRequired()).isTrue();
        assertThat(result.decision()).isEqualTo(ResultDecision.REVIEW_REQUIRED);
        assertThat(result.score()).isEqualTo(80); // 80/100, NÃO zerado.
    }

    @Test
    void test6_timeoutCountsAsLevelZeroWithoutBreakingCalculation() {
        // Turno 1 respondido com 100; turno 2 em timeout (nível 0). Empatia: raw 100 / max 200 = 50%.
        PublishedSimulation simulation = twoNodeSingleCompetencySimulation();

        Map<String, AttemptAnswer> answers = new LinkedHashMap<>();
        answers.put("turno-1", AttemptAnswer.answered("turno-1", "n1-melhor", T0));
        answers.put("turno-2", AttemptAnswer.timedOut("turno-2", T0.plusSeconds(60)));

        ScoreCalculationResult result = resultScoringService.calculate(simulation, answers);

        assertThat(result.score()).isEqualTo(50);
        assertThat(result.humanReviewRequired()).isFalse();
        assertThat(result.auditTrail()).contains("timeout");
    }

    @Test
    void test7_fastHighScoringCriticalAnswerIsLowReliability() {
        PublishedSimulation simulation = singleNodeSimulation(
                List.of("Empatia"),
                Map.of("Empatia", 1.0),
                option("opcao-alta", null, Map.of("Empatia", 100), true),
                option("opcao-baixa", null, Map.of("Empatia", 20), false)
        );

        ScoreCalculationResult result = resultScoringService.calculate(
                simulation,
                Map.of("turno-1", AttemptAnswer.answered("turno-1", "opcao-alta", T0.plusMillis(500))),
                T0
        );

        assertThat(result.reliabilityLevel()).isEqualTo(ReliabilityLevel.LOW_RELIABILITY);
    }

    @Test
    void fastAnswerUsesServerReceivedAtEvenWhenClientAnsweredAtClaimsPlausibleTime() {
        PublishedSimulation simulation = singleNodeSimulation(
                List.of("Empatia"),
                Map.of("Empatia", 1.0),
                option("opcao-alta", null, Map.of("Empatia", 100), true),
                option("opcao-baixa", null, Map.of("Empatia", 20), false)
        );

        ScoreCalculationResult result = resultScoringService.calculate(
                simulation,
                Map.of("turno-1", AttemptAnswer.answered(
                        "turno-1",
                        "opcao-alta",
                        T0.plusSeconds(20),
                        T0.plusMillis(500)
                )),
                T0,
                Map.of("turno-1", T0)
        );

        assertThat(result.reliabilityLevel()).isEqualTo(ReliabilityLevel.LOW_RELIABILITY);
    }

    @Test
    void test8_highScoringCriticalAnswerKeepsNormalReliabilityWhenTimeIsPlausible() {
        PublishedSimulation simulation = singleNodeSimulation(
                List.of("Empatia"),
                Map.of("Empatia", 1.0),
                option("opcao-alta", null, Map.of("Empatia", 100), true),
                option("opcao-baixa", null, Map.of("Empatia", 20), false)
        );

        ScoreCalculationResult result = resultScoringService.calculate(
                simulation,
                Map.of("turno-1", AttemptAnswer.answered("turno-1", "opcao-alta", T0.plusSeconds(2))),
                T0
        );

        assertThat(result.reliabilityLevel()).isEqualTo(ReliabilityLevel.NORMAL);
    }

    private Map<String, AttemptAnswer> bestChoices(int turns) {
        Map<String, AttemptAnswer> answers = new LinkedHashMap<>();
        for (int turn = 1; turn <= turns; turn++) {
            answers.put("turno-" + turn, AttemptAnswer.answered("turno-" + turn, "t" + turn + "-melhor", T0.plusSeconds(turn)));
        }
        return answers;
    }

    private PublishedSimulation linearDominantSimulation(int turns) {
        List<ScenarioNode> nodes = new java.util.ArrayList<>();
        for (int turn = 1; turn <= turns; turn++) {
            String nextNodeId = turn < turns ? "turno-" + (turn + 1) : null;
            nodes.add(new ScenarioNode(
                    "turno-" + turn,
                    turn,
                    "Cliente",
                    "Mensagem " + turn,
                    30,
                    List.of(
                            option("t" + turn + "-melhor", nextNodeId, Map.of("Empatia", 80, "Resolução", 60), false),
                            option("t" + turn + "-fraca", null, Map.of("Empatia", 10, "Resolução", 10), false)
                    )
            ));
        }

        return new PublishedSimulation(
                10L, 1, "sim-linear", "Linear", "Descricao",
                List.of("Empatia", "Resolução"),
                Map.of("Empatia", 0.5, "Resolução", 0.5),
                Map.of("Empatia", ResultTier.MAJOR, "Resolução", ResultTier.MAJOR),
                "turno-1",
                nodes
        );
    }

    private PublishedSimulation twoNodeSingleCompetencySimulation() {
        return new PublishedSimulation(
                11L, 1, "sim-timeout", "Timeout", "Descricao",
                List.of("Empatia"),
                Map.of("Empatia", 1.0),
                Map.of("Empatia", ResultTier.MAJOR),
                "turno-1",
                List.of(
                        new ScenarioNode("turno-1", 1, "Cliente", "M1", 30, List.of(
                                option("n1-melhor", "turno-2", Map.of("Empatia", 100), false),
                                option("n1-fraca", "turno-2", Map.of("Empatia", 20), false)
                        )),
                        new ScenarioNode("turno-2", 2, "Cliente", "M2", 30, List.of(
                                option("n2-melhor", null, Map.of("Empatia", 100), false),
                                option("n2-fraca", null, Map.of("Empatia", 30), false)
                        ))
                )
        );
    }

    private PublishedSimulation singleNodeSimulation(
            List<String> competencies,
            Map<String, Double> weights,
            ScenarioOption... options
    ) {
        return new PublishedSimulation(
                12L, 1, "sim-single", "Single", "Descricao",
                competencies,
                weights,
                competencyTiers(competencies),
                "turno-1",
                List.of(new ScenarioNode("turno-1", 1, "Cliente", "Mensagem", 30, List.of(options)))
        );
    }

    private Map<String, ResultTier> competencyTiers(List<String> competencies) {
        Map<String, ResultTier> tiers = new LinkedHashMap<>();
        for (String competency : competencies) {
            tiers.put(competency, ResultTier.MAJOR);
        }
        return tiers;
    }

    private ScenarioOption option(String id, String nextNodeId, Map<String, Integer> scores, boolean critical) {
        return new ScenarioOption(id, "Texto " + id, nextNodeId, scores, critical, "Nota de auditoria " + id);
    }
}
