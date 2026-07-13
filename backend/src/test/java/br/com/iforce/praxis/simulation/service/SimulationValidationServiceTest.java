package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.simulation.dto.CompetencyWeightDto;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                        && issue.message().contains("pesos das competências precisam somar 100%"));
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
    void normalizesLegacyRootNodeIdBeforeValidation() {
        SimulationVersionEntity version = singleNodeVersion(Map.of("Empatia", 1.0));
        version.setRootNodeId("etapa-1");

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isTrue();
        assertThat(version.getRootNodeId()).isEqualTo("turno-1");
        assertThat(response.issues()).extracting(ValidationIssueResponse::message)
                .noneMatch(message -> message.contains("etapa inicial"));
    }

    @Test
    void infersSingleNodeWithoutIncomingEdgesAsRootWhenStoredRootIsInvalid() {
        SimulationVersionEntity version = singleNodeVersion(Map.of("Empatia", 1.0));
        version.setRootNodeId("turno-inexistente");

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isTrue();
        assertThat(version.getRootNodeId()).isEqualTo("turno-1");
        assertThat(response.issues()).extracting(ValidationIssueResponse::message)
                .noneMatch(message -> message.contains("etapa inicial"));
    }

    @Test
    void blocksPublicationWhenDepthExceedsMaximumTurns() {
        SimulationVersionEntity version = linearVersion(21);

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isFalse();
        assertThat(response.issues())
                .anyMatch(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER
                        && issue.message().contains("limite de 20 etapas"));
    }

    @Test
    void allowsDepthUpToMaximumTurns() {
        SimulationVersionEntity version = linearVersion(20);

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.issues()).extracting(ValidationIssueResponse::message)
                .noneMatch(message -> message.contains("limite de 20 etapas"));
    }

    @Test
    void allowsPublicationWhenTerminalPathsHaveDifferentRawPointCeilings() {
        SimulationVersionEntity version = branchingVersionWithUnbalancedPathScores();

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isTrue();
        assertThat(response.issues())
                .noneMatch(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER
                        && issue.message().contains("pontuação máxima diferente"));
    }

    @Test
    void warnsWithoutBlockingWhenAPathDoesNotScoreConfiguredCompetency() {
        SimulationVersionEntity version = branchingVersionWithMissingCompetencyOnOnePath();

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isTrue();
        assertThat(response.issues())
                .anyMatch(issue -> issue.severity() == ValidationIssueSeverity.WARNING
                        && issue.nodeId().equals("fim-b")
                        && issue.message().contains("não pontua a competência \"Resolucao\""));
        assertThat(response.issues())
                .noneMatch(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER);
    }

    @Test
    void allowsPublicationWhenACompetencyIsZeroedOnEveryPath() {
        SimulationVersionEntity version = baseVersion("turno-1");
        version.getCompetencies().add(competency(version, "Empatia", 0.34));
        version.getCompetencies().add(competency(version, "Lideranca", 0.33));
        version.getCompetencies().add(competency(version, "Negociacao", 0.33));

        SimulationNodeEntity root = node(version, "turno-1", 1);
        root.setTimeoutNextNodeId("turno-2");
        SimulationOptionEntity optionA = option(root, "opcao-a", "turno-3", List.of("Lideranca"), 6);
        SimulationOptionEntity optionB = option(root, "opcao-b", "turno-4", List.of("Empatia"), 5);
        addZeroScore(optionA, "Empatia");
        addZeroScore(optionA, "Negociacao");
        addZeroScore(optionB, "Lideranca");
        addZeroScore(optionB, "Negociacao");
        root.getOptions().add(optionA);
        root.getOptions().add(optionB);
        version.getNodes().add(root);
        version.getNodes().add(finalNode(version, "turno-2", 2));
        version.getNodes().add(finalNode(version, "turno-3", 2));
        version.getNodes().add(finalNode(version, "turno-4", 2));

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isTrue();
        assertThat(response.blockerCount()).isZero();
        assertThat(response.issues())
                .filteredOn(issue -> issue.severity() == ValidationIssueSeverity.WARNING
                        && issue.message().contains("não pontua a competência \"Negociacao\""))
                .hasSize(3);
    }

    @Test
    void blocksPublicationWhenOptionScoresUnconfiguredCompetency() {
        SimulationVersionEntity version = baseVersion("turno-1");
        version.getCompetencies().add(competency(version, "Empatia", 1.0));

        SimulationNodeEntity node = node(version, "turno-1", 1);
        node.setTimeoutNextNodeId("fim");
        node.getOptions().add(option(node, "opcao-a", "fim", List.of("Resolucao")));
        node.getOptions().add(option(node, "opcao-b", "fim", List.of("Resolucao")));
        version.getNodes().add(node);
        version.getNodes().add(finalNode(version, "fim", 2));

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isFalse();
        assertThat(response.issues())
                .anyMatch(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER
                        && issue.nodeId().equals("turno-1")
                        && issue.message().contains("\"Resolucao\"")
                        && issue.message().contains("configurada"));
    }

    @Test
    void blocksPublicationWhenConfiguredCompetencyIsNeverScored() {
        SimulationVersionEntity version = singleNodeVersion(Map.of("Empatia", 0.5, "Resolucao", 0.5));
        for (SimulationNodeEntity node : version.getNodes()) {
            for (SimulationOptionEntity option : node.getOptions()) {
                option.getCompetencyScores().removeIf(score -> "Resolucao".equals(score.getCompetencyName()));
            }
        }

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isFalse();
        assertThat(response.issues())
                .anyMatch(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER
                        && issue.nodeId().equals("Resolucao")
                        && issue.message().contains("nenhuma resposta"));
    }

    @Test
    void blocksPublicationWhenNonFinalOptionHasNoDestination() {
        SimulationVersionEntity version = baseVersion("turno-1");
        version.getCompetencies().add(competency(version, "Empatia", 1.0));

        SimulationNodeEntity node = node(version, "turno-1", 1);
        node.getOptions().add(option(node, "opcao-a", null, List.of("Empatia")));
        node.getOptions().add(option(node, "opcao-b", null, List.of("Empatia")));
        version.getNodes().add(node);

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isFalse();
        assertThat(response.issues())
                .anyMatch(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER
                        && issue.nodeId().equals("turno-1")
                        && issue.message().contains("está sem destino"));
    }

    @Test
    void rejectsNullWeightOnCreateOrUpdateValidation() {
        assertThatThrownBy(() -> service.validateWeights(List.of(
                new CompetencyWeightDto("Empatia", null, 70, null)
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Peso de competência não pode ser nulo");
    }

    @Test
    void rejectsNegativeWeightOnCreateOrUpdateValidation() {
        assertThatThrownBy(() -> service.validateWeights(List.of(
                new CompetencyWeightDto("Empatia", -0.1, 70, null),
                new CompetencyWeightDto("Resolucao", 1.1, 70, null)
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Peso de competência não pode ser negativo");
    }

    private SimulationVersionEntity singleNodeVersion(Map<String, Double> weights) {
        SimulationVersionEntity version = baseVersion("turno-1");
        weights.forEach((name, weight) -> version.getCompetencies().add(competency(version, name, weight)));

        SimulationNodeEntity node = node(version, "turno-1", 1);
        node.setTimeoutNextNodeId("fim");
        node.getOptions().add(option(node, "opcao-a", "fim", weights.keySet()));
        node.getOptions().add(option(node, "opcao-b", "fim", weights.keySet()));
        version.getNodes().add(node);
        version.getNodes().add(finalNode(version, "fim", 2));
        return version;
    }

    private SimulationVersionEntity linearVersion(int turns) {
        SimulationVersionEntity version = baseVersion("turno-1");
        version.getCompetencies().add(competency(version, "Empatia", 0.5));
        version.getCompetencies().add(competency(version, "Resolução", 0.5));

        for (int turn = 1; turn < turns; turn++) {
            String nextNodeId = turn < turns - 1 ? "turno-" + (turn + 1) : "fim";
            SimulationNodeEntity node = node(version, "turno-" + turn, turn);
            node.setTimeoutNextNodeId(nextNodeId);
            node.getOptions().add(option(node, "opcao-a-" + turn, nextNodeId, List.of("Empatia", "Resolução")));
            node.getOptions().add(option(node, "opcao-b-" + turn, nextNodeId, List.of("Empatia", "Resolução")));
            version.getNodes().add(node);
        }
        version.getNodes().add(finalNode(version, "fim", turns));
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
        highScoreTerminal.setTimeoutNextNodeId("fim-a");
        highScoreTerminal.getOptions().add(option(highScoreTerminal, "opcao-a1", "fim-a", List.of("Empatia", "Resolucao"), 90));
        highScoreTerminal.getOptions().add(option(highScoreTerminal, "opcao-a2", "fim-a", List.of("Empatia", "Resolucao"), 90));

        SimulationNodeEntity lowScoreTerminal = node(version, "turno-2b", 2);
        lowScoreTerminal.setTimeoutNextNodeId("fim-b");
        lowScoreTerminal.getOptions().add(option(lowScoreTerminal, "opcao-b1", "fim-b", List.of("Empatia", "Resolucao"), 50));
        lowScoreTerminal.getOptions().add(option(lowScoreTerminal, "opcao-b2", "fim-b", List.of("Empatia", "Resolucao"), 50));

        version.getNodes().add(root);
        version.getNodes().add(highScoreTerminal);
        version.getNodes().add(lowScoreTerminal);
        version.getNodes().add(finalNode(version, "fim-a", 3));
        version.getNodes().add(finalNode(version, "fim-b", 3));
        return version;
    }

    private SimulationVersionEntity branchingVersionWithMissingCompetencyOnOnePath() {
        SimulationVersionEntity version = baseVersion("turno-1");
        version.getCompetencies().add(competency(version, "Empatia", 0.5));
        version.getCompetencies().add(competency(version, "Resolucao", 0.5));

        SimulationNodeEntity root = node(version, "turno-1", 1);
        root.setTimeoutNextNodeId("turno-2a");
        root.getOptions().add(option(root, "opcao-a", "turno-2a", List.of("Empatia")));
        root.getOptions().add(option(root, "opcao-b", "turno-2b", List.of("Empatia")));

        SimulationNodeEntity coveredTerminal = node(version, "turno-2a", 2);
        coveredTerminal.setTimeoutNextNodeId("fim-a");
        coveredTerminal.getOptions().add(option(coveredTerminal, "opcao-a1", "fim-a", List.of("Resolucao"), 50));
        coveredTerminal.getOptions().add(option(coveredTerminal, "opcao-a2", "fim-a", List.of("Resolucao"), 50));

        SimulationNodeEntity uncoveredTerminal = node(version, "turno-2b", 2);
        uncoveredTerminal.setTimeoutNextNodeId("fim-b");
        uncoveredTerminal.getOptions().add(option(uncoveredTerminal, "opcao-b1", "fim-b", List.of("Empatia"), 50));
        uncoveredTerminal.getOptions().add(option(uncoveredTerminal, "opcao-b2", "fim-b", List.of("Empatia"), 50));

        version.getNodes().add(root);
        version.getNodes().add(coveredTerminal);
        version.getNodes().add(uncoveredTerminal);
        version.getNodes().add(finalNode(version, "fim-a", 3));
        version.getNodes().add(finalNode(version, "fim-b", 3));
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

    private SimulationNodeEntity finalNode(SimulationVersionEntity version, String nodeId, int turnIndex) {
        SimulationNodeEntity node = node(version, nodeId, turnIndex);
        node.setFinal(true);
        node.setReportText("Relatorio final");
        return node;
    }

    private void addZeroScore(SimulationOptionEntity option, String competencyName) {
        OptionCompetencyScoreEntity score = new OptionCompetencyScoreEntity();
        score.setSimulationOption(option);
        score.setCompetencyName(competencyName);
        score.setScore(0);
        option.getCompetencyScores().add(score);
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
