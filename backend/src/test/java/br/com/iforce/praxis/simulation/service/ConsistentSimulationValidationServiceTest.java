package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsistentSimulationValidationServiceTest {

    private final ConsistentSimulationValidationService service = new ConsistentSimulationValidationService(
            new PraxisProperties("http://localhost:8080", 168, 24, 70, 15, 0.001)
    );

    @Test
    void acceptsOptionWithoutNextNodeAsEndOfAssessment() {
        SimulationVersionEntity version = validVersion(false);
        SimulationNodeEntity root = findRoot(version);
        root.getOptions().forEach(option -> option.setNextNodeId(null));

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isTrue();
        assertThat(response.issues())
                .noneMatch(issue -> issue.message().contains("resposta está sem destino"));
    }

    @Test
    void blocksDirectEndWithoutReportText() {
        SimulationVersionEntity version = validVersion(false);
        SimulationNodeEntity root = findRoot(version);
        SimulationOptionEntity directEnd = root.getOptions().stream()
                .filter(option -> option.getNextNodeId() == null)
                .findFirst()
                .orElseThrow();
        directEnd.setAuditNote("");

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isFalse();
        assertThat(response.issues())
                .anyMatch(issue -> issue.nodeId().equals("turno-1")
                        && issue.message().contains("sem texto de relatório"));
    }

    @Test
    void doesNotRequireTimeoutDestinationWhenStageHasNoTimeLimit() {
        SimulationVersionEntity version = validVersion(false);

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isTrue();
        assertThat(response.issues())
                .noneMatch(issue -> issue.message().contains("destino para tempo esgotado"));
    }

    @Test
    void acceptsMissingTimeoutDestinationAsEndOfAssessment() {
        SimulationVersionEntity version = validVersion(true);

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isTrue();
        assertThat(response.issues())
                .noneMatch(issue -> issue.message().contains("destino para tempo esgotado"));
    }

    @Test
    void keepsBlockerWhenTimeoutDestinationDoesNotExist() {
        SimulationVersionEntity version = validVersion(true);
        SimulationNodeEntity root = findRoot(version);
        root.setTimeoutNextNodeId("turno-inexistente");

        SimulationValidationResponse response = service.validate(version);

        assertThat(response.publishable()).isFalse();
        assertThat(response.issues())
                .anyMatch(issue -> issue.message().contains("destino de tempo esgotado")
                        && issue.message().contains("não existe"));
    }

    private SimulationNodeEntity findRoot(SimulationVersionEntity version) {
        return version.getNodes().stream()
                .filter(node -> "turno-1".equals(node.getNodeId()))
                .findFirst()
                .orElseThrow();
    }

    private SimulationVersionEntity validVersion(boolean timed) {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setId("sim-validacao-consistente");

        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setSimulation(simulation);
        version.setVersionNumber(1);
        version.setRootNodeId("turno-1");

        SimulationCompetencyEntity competency = new SimulationCompetencyEntity();
        competency.setSimulationVersion(version);
        competency.setName("Comunicação");
        competency.setWeight(1.0);
        version.getCompetencies().add(competency);

        SimulationNodeEntity root = new SimulationNodeEntity();
        root.setSimulationVersion(version);
        root.setNodeId("turno-1");
        root.setTurnIndex(1);
        root.setSpeaker("Cliente");
        root.setMessage("Como você responderia?");
        root.setTimeLimitSeconds(timed ? 30 : null);
        root.getOptions().add(option(root, "opcao-1", "fim"));
        root.getOptions().add(option(root, "opcao-2", null));

        SimulationNodeEntity end = new SimulationNodeEntity();
        end.setSimulationVersion(version);
        end.setNodeId("fim");
        end.setTurnIndex(2);
        end.setSpeaker("Sistema");
        end.setMessage("");
        end.setFinal(true);
        end.setReportText("Participação concluída.");

        version.getNodes().add(root);
        version.getNodes().add(end);
        return version;
    }

    private SimulationOptionEntity option(
            SimulationNodeEntity node,
            String optionId,
            String nextNodeId
    ) {
        SimulationOptionEntity option = new SimulationOptionEntity();
        option.setSimulationNode(node);
        option.setOptionId(optionId);
        option.setText("Alternativa " + optionId);
        option.setNextNodeId(nextNodeId);
        option.setAuditNote("Relatório do encerramento.");
        return option;
    }
}
