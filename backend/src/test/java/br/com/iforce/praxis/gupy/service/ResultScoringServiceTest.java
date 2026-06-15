package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.gupy.model.ScoreCalculationResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class ResultScoringServiceTest {

    private final ResultScoringService resultScoringService = new ResultScoringService();

    @Test
    void normalizesScoreAcrossAnsweredPathInsteadOfUsingOnlyFinalOption() {
        PublishedSimulation simulation = twoTurnSimulation(false);

        ScoreCalculationResult result = resultScoringService.calculate(
                simulation,
                Map.of(
                        "turno-1", new AttemptAnswer("turno-1", "opcao-abre-caminho", Instant.parse("2026-01-01T10:00:00Z")),
                        "turno-2", new AttemptAnswer("turno-2", "opcao-final-alta", Instant.parse("2026-01-01T10:01:00Z"))
                )
        );

        assertThat(result.score()).isEqualTo(75);
        assertThat(result.resultItems())
                .extracting(ResultItem::name, ResultItem::score, ResultItem::tier)
                .containsExactly(
                        tuple("Empatia", 60, ResultTier.MAJOR),
                        tuple("Aderencia a politica", 90, ResultTier.MINOR)
                );
        assertThat(result.auditTrail()).contains("Primeiro turno").contains("Turno final");
    }

    @Test
    void criticalOptionAnywhereInPathRequiresHumanReviewWithoutZeroingScore() {
        PublishedSimulation simulation = twoTurnSimulation(true);

        ScoreCalculationResult result = resultScoringService.calculate(
                simulation,
                Map.of(
                        "turno-1", new AttemptAnswer("turno-1", "opcao-abre-caminho", Instant.parse("2026-01-01T10:00:00Z")),
                        "turno-2", new AttemptAnswer("turno-2", "opcao-final-alta", Instant.parse("2026-01-01T10:01:00Z"))
                )
        );

        assertThat(result.score()).isEqualTo(75);
        assertThat(result.humanReviewRequired()).isTrue();
    }

    private PublishedSimulation twoTurnSimulation(boolean firstOptionCritical) {
        ScenarioOption firstOption = new ScenarioOption(
                "opcao-abre-caminho",
                "Acolhe parcialmente e encaminha para validacao.",
                "turno-2",
                Map.of(
                        "Empatia", 20,
                        "Aderencia a politica", 80
                ),
                firstOptionCritical,
                "Primeiro turno"
        );
        ScenarioOption finalOption = new ScenarioOption(
                "opcao-final-alta",
                "Fecha com comunicacao clara e processo correto.",
                null,
                Map.of(
                        "Empatia", 100,
                        "Aderencia a politica", 100
                ),
                false,
                "Turno final"
        );

        return new PublishedSimulation(
                "sim-teste",
                "Simulacao teste",
                "Descricao",
                List.of("Empatia", "Aderencia a politica"),
                "turno-1",
                List.of(
                        new ScenarioNode(
                                "turno-1",
                                1,
                                "Cliente",
                                "Mensagem inicial",
                                30,
                                List.of(firstOption)
                        ),
                        new ScenarioNode(
                                "turno-2",
                                2,
                                "Cliente",
                                "Mensagem final",
                                30,
                                List.of(finalOption)
                        )
                )
        );
    }
}
