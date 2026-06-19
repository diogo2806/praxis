package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.dto.ValidationIssueResponse;
import br.com.iforce.praxis.simulation.model.ValidationIssueSeverity;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationValidationServiceTest {

    private final SimulationValidationService service = new SimulationValidationService(
            new PraxisProperties("http://localhost:8080", 168, 24, 70, 15, 0.001)
    );

    @Test
    void blocksPublicationWhenCompetencyWeightsDoNotSumToOne() {
        SimulationVersionEntity version = singleNodeVersion(Map.of("Empatia", 0.3, "Resolução", 0.3));

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isFalse();
        assertThat(response.issues())
                .anyMatch(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER
                        && issue.message().contains("soma dos pesos"));
    }

    @Test
    void allowsPublicationWhenWeightsSumToOneWithinTolerance() {
        SimulationVersionEntity version = singleNodeVersion(Map.of("Empatia", 0.5, "Resolução", 0.5));

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isTrue();
        assertThat(response.issues()).extracting(ValidationIssueResponse::message)
                .noneMatch(message -> message.contains("soma dos pesos"));
    }

    @Test
    void blocksPublicationWhenDepthExceedsMaximumTurns() {
        SimulationVersionEntity version = linearVersion(11);

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isFalse();
        assertThat(response.issues())
                .anyMatch(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER
                        && issue.message().contains("profundidade máxima"));
    }

    @Test
    void allowsDepthUpToMaximumTurns() {
        SimulationVersionEntity version = linearVersion(10);

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.issues()).extracting(ValidationIssueResponse::message)
                .noneMatch(message -> message.contains("profundidade máxima"));
    }

    @Test
    void blocksPublicationWhenTerminalPathsHaveDifferentMaxScores() {
        SimulationVersionEntity version = branchingVersionWithUnbalancedPathScores();

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isFalse();
        assertThat(response.issues())
                .anyMatch(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER
                        && issue.message().contains("score maximo diferente"));
    }

    private SimulationVersionEntity singleNodeVersion(Map<String, Double> weights) {
        SimulationVersionEntity version = baseVersion("turno-1");
        weights.forEach((name, weight) -> version.getCompetencies().add(competency(version, name, weight)));

        SimulationNodeEntity node = node(version, "turno-1", 1);
        node.getOptions().add(option(node, "opcao-a", null, weights.keySet()));
        node.getOptions().add(option(node, "opcao-b", null, weights.keySet()));
        version.getNodes().add(node);
        return version;
    }

    private SimulationVersionEntity linearVersion(int turns) {
        SimulationVersionEntity version = baseVersion("turno-1");
        version.getCompetencies().add(competency(version, "Empatia", 0.5));
        version.getCompetencies().add(competency(version, "Resolução", 0.5));

        for (int turn = 1; turn <= turns; turn++) {
            String nextNodeId = turn < turns ? "turno-" + (turn + 1) : null;
            SimulationNodeEntity node = node(version, "turno-" + turn, turn);
            node.setTimeoutNextNodeId(nextNodeId);
            node.getOptions().add(option(node, "opcao-a-" + turn, nextNodeId, List.of("Empatia", "Resolução")));
            node.getOptions().add(option(node, "opcao-b-" + turn, nextNodeId, List.of("Empatia", "Resolução")));
            version.getNodes().add(node);
        }
        return version;
    }

    private SimulationVersionEntity branchingVersionWithUnbalancedPathScores() {
        SimulationVersionEntity version = baseVersion("turno-1");
        version.getCompetencies().add(competency(version, "Empatia", 0.5));
        version.getCompetencies().add(competency(version, "Resolucao", 0.5));

        SimulationNodeEntity root = node(version, "turno-1", 1);
        root.setTimeoutNextNodeId("turno-2a");
        root.getOptions().add(option(root, "opcao-a", "turno-2a", List.of("Empatia", "Resolucao")));
        root.getOptions().add(option(root, "opcao-b", "turno-2b", List.of("Empatia", "Resolucao")));

        SimulationNodeEntity highScoreTerminal = node(version, "turno-2a", 2);
        highScoreTerminal.getOptions().add(option(highScoreTerminal, "opcao-a1", null, List.of("Empatia", "Resolucao"), 90));
        highScoreTerminal.getOptions().add(option(highScoreTerminal, "opcao-a2", null, List.of("Empatia", "Resolucao"), 90));

        SimulationNodeEntity lowScoreTerminal = node(version, "turno-2b", 2);
        lowScoreTerminal.getOptions().add(option(lowScoreTerminal, "opcao-b1", null, List.of("Empatia", "Resolucao"), 50));
        lowScoreTerminal.getOptions().add(option(lowScoreTerminal, "opcao-b2", null, List.of("Empatia", "Resolucao"), 50));

        version.getNodes().add(root);
        version.getNodes().add(highScoreTerminal);
        version.getNodes().add(lowScoreTerminal);
        return version;
    }

    private SimulationVersionEntity baseVersion(String rootNodeId) {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setId("sim-test");
        simulation.setName("Sim Test");
        simulation.setDescription("Descricao");

        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setSimulation(simulation);
        version.setVersionNumber(1);
        version.setRootNodeId(rootNodeId);
        return version;
    }

    private SimulationCompetencyEntity competency(SimulationVersionEntity version, String name, double weight) {
        SimulationCompetencyEntity competency = new SimulationCompetencyEntity();
        competency.setSimulationVersion(version);
        competency.setName(name);
        competency.setWeight(weight);
        return competency;
    }

    private SimulationNodeEntity node(SimulationVersionEntity version, String nodeId, int turnIndex) {
        SimulationNodeEntity node = new SimulationNodeEntity();
        node.setSimulationVersion(version);
        node.setNodeId(nodeId);
        node.setTurnIndex(turnIndex);
        node.setSpeaker("Cliente");
        node.setMessage("Mensagem");
        return node;
    }

    private SimulationOptionEntity option(
            SimulationNodeEntity node,
            String optionId,
            String nextNodeId,
            Iterable<String> competencies
    ) {
        SimulationOptionEntity option = new SimulationOptionEntity();
        option.setSimulationNode(node);
        option.setOptionId(optionId);
        option.setText("Texto");
        option.setNextNodeId(nextNodeId);
        option.setCritical(false);
        option.setAuditNote("Nota");
        for (String competency : competencies) {
            OptionCompetencyScoreEntity score = new OptionCompetencyScoreEntity();
            score.setSimulationOption(option);
            score.setCompetencyName(competency);
            score.setScore(50);
            option.getCompetencyScores().add(score);
        }
        return option;
    }

    private SimulationOptionEntity option(
            SimulationNodeEntity node,
            String optionId,
            String nextNodeId,
            Iterable<String> competencies,
            int scoreValue
    ) {
        SimulationOptionEntity option = new SimulationOptionEntity();
        option.setSimulationNode(node);
        option.setOptionId(optionId);
        option.setText("Texto");
        option.setNextNodeId(nextNodeId);
        option.setCritical(false);
        option.setAuditNote("Nota");
        for (String competency : competencies) {
            OptionCompetencyScoreEntity score = new OptionCompetencyScoreEntity();
            score.setSimulationOption(option);
            score.setCompetencyName(competency);
            score.setScore(scoreValue);
            option.getCompetencyScores().add(score);
        }
        return option;
    }
}
