package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.model.ValidationIssueSeverity;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;

class ComparableSimulationValidationServiceTest {

    private final ComparableSimulationValidationService service =
            new ComparableSimulationValidationService(
                    new PraxisProperties("https://praxis.example.com", 168, 24, 70, 15, 0.001)
            );

    @Test
    void allowsPublicationWhenPathsUseDifferentMaximums() {
        SimulationVersionEntity version = versionWithDifferentPathMaximums();

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isTrue();
        assertThat(response.blockerCount()).isZero();
        assertThat(response.issues())
                .noneMatch(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER
                        && issue.message().contains("bases máximas diferentes"));
    }

    private SimulationVersionEntity versionWithDifferentPathMaximums() {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setId("sim-comparabilidade");
        simulation.setEmpresaId("empresa-1");
        simulation.setName("Comparabilidade");
        simulation.setDescription("Teste");
        simulation.setCreatedAt(Instant.now());

        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setSimulation(simulation);
        version.setVersionNumber(1);
        version.setRootNodeId("raiz");
        version.setCreatedAt(Instant.now());

        SimulationCompetencyEntity competency = new SimulationCompetencyEntity();
        competency.setSimulationVersion(version);
        competency.setName("Comunicação");
        competency.setWeight(1.0);
        version.setCompetencies(new LinkedHashSet<>());
        version.getCompetencies().add(competency);

        SimulationNodeEntity root = node(version, "raiz", 1, false, null);
        SimulationNodeEntity pathA = node(version, "caminho-a", 2, false, null);
        SimulationNodeEntity pathB = node(version, "caminho-b", 2, false, null);
        SimulationNodeEntity endA = node(version, "fim-a", 3, true, "Fim A");
        SimulationNodeEntity endB = node(version, "fim-b", 3, true, "Fim B");

        root.getOptions().add(option(root, "raiz-a", "caminho-a", 0));
        root.getOptions().add(option(root, "raiz-b", "caminho-b", 0));
        root.setTimeoutNextNodeId("caminho-a");

        pathA.getOptions().add(option(pathA, "a-1", "fim-a", 100));
        pathA.getOptions().add(option(pathA, "a-2", "fim-a", 20));
        pathA.setTimeoutNextNodeId("fim-a");

        pathB.getOptions().add(option(pathB, "b-1", "fim-b", 50));
        pathB.getOptions().add(option(pathB, "b-2", "fim-b", 10));
        pathB.setTimeoutNextNodeId("fim-b");

        version.setNodes(new LinkedHashSet<>());
        version.getNodes().add(root);
        version.getNodes().add(pathA);
        version.getNodes().add(pathB);
        version.getNodes().add(endA);
        version.getNodes().add(endB);
        return version;
    }

    private SimulationNodeEntity node(
            SimulationVersionEntity version,
            String id,
            int turn,
            boolean terminal,
            String reportText
    ) {
        SimulationNodeEntity node = new SimulationNodeEntity();
        node.setSimulationVersion(version);
        node.setNodeId(id);
        node.setTurnIndex(turn);
        node.setSpeaker("Gestor");
        node.setMessage(terminal ? "Encerramento" : "Como você agiria?");
        node.setFinal(terminal);
        node.setReportText(reportText);
        node.setOptions(new LinkedHashSet<>());
        return node;
    }

    private SimulationOptionEntity option(
            SimulationNodeEntity node,
            String id,
            String nextNodeId,
            int score
    ) {
        SimulationOptionEntity option = new SimulationOptionEntity();
        option.setSimulationNode(node);
        option.setOptionId(id);
        option.setText("Alternativa " + id);
        option.setNextNodeId(nextNodeId);
        option.setAuditNote("Critério definido");
        option.setCompetencyScores(new LinkedHashSet<>());

        OptionCompetencyScoreEntity competencyScore = new OptionCompetencyScoreEntity();
        competencyScore.setSimulationOption(option);
        competencyScore.setCompetencyName("Comunicação");
        competencyScore.setScore(score);
        option.getCompetencyScores().add(competencyScore);
        return option;
    }
}
