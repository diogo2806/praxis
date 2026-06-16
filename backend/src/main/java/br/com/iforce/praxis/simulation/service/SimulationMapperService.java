package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimulationMapperService {

    public PublishedSimulation toPublishedSimulation(SimulationVersionEntity simulationVersionEntity) {
        List<SimulationCompetencyEntity> sortedCompetencies = simulationVersionEntity.getCompetencies().stream()
                .sorted(Comparator.comparing(SimulationCompetencyEntity::getName))
                .toList();

        List<String> competencies = sortedCompetencies.stream()
                .map(SimulationCompetencyEntity::getName)
                .toList();

        Map<String, Double> competencyWeights = new LinkedHashMap<>();
        sortedCompetencies.forEach(competency -> competencyWeights.put(competency.getName(), competency.getWeight()));

        List<ScenarioNode> nodes = simulationVersionEntity.getNodes().stream()
                .sorted(Comparator.comparingInt(SimulationNodeEntity::getTurnIndex))
                .map(this::toScenarioNode)
                .toList();

        return new PublishedSimulation(
                simulationVersionEntity.getId(),
                simulationVersionEntity.getVersionNumber(),
                simulationVersionEntity.getSimulation().getId(),
                simulationVersionEntity.getSimulation().getName(),
                simulationVersionEntity.getSimulation().getDescription(),
                competencies,
                competencyWeights,
                simulationVersionEntity.getRootNodeId(),
                nodes
        );
    }

    private ScenarioNode toScenarioNode(SimulationNodeEntity simulationNodeEntity) {
        List<ScenarioOption> options = simulationNodeEntity.getOptions().stream()
                .sorted(Comparator.comparing(SimulationOptionEntity::getOptionId))
                .map(this::toScenarioOption)
                .toList();

        return new ScenarioNode(
                simulationNodeEntity.getNodeId(),
                simulationNodeEntity.getTurnIndex(),
                simulationNodeEntity.getSpeaker(),
                simulationNodeEntity.getMessage(),
                simulationNodeEntity.getTimeLimitSeconds(),
                options
        );
    }

    private ScenarioOption toScenarioOption(SimulationOptionEntity simulationOptionEntity) {
        Map<String, Integer> competencyScores = new LinkedHashMap<>();
        simulationOptionEntity.getCompetencyScores().stream()
                .sorted(Comparator.comparing(OptionCompetencyScoreEntity::getCompetencyName))
                .forEach(score -> competencyScores.put(score.getCompetencyName(), score.getScore()));

        return new ScenarioOption(
                simulationOptionEntity.getOptionId(),
                simulationOptionEntity.getText(),
                simulationOptionEntity.getNextNodeId(),
                competencyScores,
                simulationOptionEntity.isCritical(),
                simulationOptionEntity.getAuditNote()
        );
    }
}
