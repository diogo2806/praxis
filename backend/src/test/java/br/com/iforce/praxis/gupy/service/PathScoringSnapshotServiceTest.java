package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PathScoringSnapshotServiceTest {

    private final PathScoringSnapshotService service = new PathScoringSnapshotService();

    @Test
    void recordsRawMaximumNormalizedAndAlgorithmVersionForActualPath() {
        ScenarioOption pathA = option("path-a", "a", Map.of("Comunicação", 10, "Decisão", 0));
        ScenarioOption pathB = option("path-b", "b", Map.of("Comunicação", 0, "Decisão", 10));
        ScenarioNode root = node("root", List.of(pathA, pathB));

        ScenarioOption strong = option("strong", null, Map.of("Comunicação", 50, "Decisão", 20));
        ScenarioOption weak = option("weak", null, Map.of("Comunicação", 10, "Decisão", 0));
        ScenarioNode branchA = node("a", List.of(strong, weak));
        ScenarioNode branchB = node("b", List.of(option(
                "alternative",
                null,
                Map.of("Comunicação", 20, "Decisão", 40)
        )));

        PublishedSimulation simulation = simulation(List.of(root, branchA, branchB));
        Instant now = Instant.now();
        Map<String, AttemptAnswer> answers = Map.of(
                "root", AttemptAnswer.answered("root", "path-a", now),
                "a", AttemptAnswer.answered("a", "strong", now.plusSeconds(10))
        );

        PathScoringSnapshotService.ScoringSnapshot snapshot = service.calculate(simulation, answers, 88);

        assertThat(snapshot.rawScore()).isEqualTo(80);
        assertThat(snapshot.pathMaximumScore()).isEqualTo(90);
        assertThat(snapshot.normalizedScore()).isEqualTo(88);
        assertThat(snapshot.algorithmVersion()).isEqualTo("path-normalized-v2");
    }

    @Test
    void timeoutAddsPathMaximumButNoRawPoints() {
        ScenarioNode root = new ScenarioNode(
                "root",
                1,
                "Gestor",
                "Decida",
                30,
                "end",
                false,
                null,
                null,
                null,
                null,
                null,
                List.of(option("answer", "end", Map.of("Comunicação", 70, "Decisão", 30)))
        );
        ScenarioNode end = new ScenarioNode(
                "end",
                2,
                "Sistema",
                "Fim",
                null,
                null,
                true,
                "Encerrado",
                null,
                null,
                null,
                null,
                List.of()
        );
        PublishedSimulation simulation = simulation(List.of(root, end));
        Map<String, AttemptAnswer> answers = Map.of(
                "root", AttemptAnswer.timedOut("root", Instant.now())
        );

        PathScoringSnapshotService.ScoringSnapshot snapshot = service.calculate(simulation, answers, 0);

        assertThat(snapshot.rawScore()).isZero();
        assertThat(snapshot.pathMaximumScore()).isEqualTo(100);
        assertThat(snapshot.normalizedScore()).isZero();
    }

    private PublishedSimulation simulation(List<ScenarioNode> nodes) {
        return new PublishedSimulation(
                10L,
                3,
                "sim-1",
                "Teste",
                "Descrição",
                List.of("Comunicação", "Decisão"),
                Map.of("Comunicação", 0.6, "Decisão", 0.4),
                Map.of("Comunicação", ResultTier.MAJOR, "Decisão", ResultTier.MAJOR),
                "root",
                nodes
        );
    }

    private ScenarioNode node(String id, List<ScenarioOption> options) {
        return new ScenarioNode(id, 1, "Gestor", "Decida", null, options);
    }

    private ScenarioOption option(String id, String nextNodeId, Map<String, Integer> scores) {
        return new ScenarioOption(id, id, nextNodeId, scores, false, "Critério");
    }
}
