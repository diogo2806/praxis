package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.simulation.dto.CompetencyWeightDto;
import br.com.iforce.praxis.simulation.dto.SimulationVersionDetailResponse;
import br.com.iforce.praxis.simulation.dto.UpdateBlueprintRequest;
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

    public void applyInitialBlueprint(SimulationVersionEntity simulationVersionEntity, String rootNodeId, List<String> competencies) {
        simulationVersionEntity.setRootNodeId(rootNodeId.trim());
        simulationVersionEntity.getCompetencies().clear();

        List<String> uniqueCompetencies = competencies
                .stream()
                .map(String::trim)
                .filter(competency -> !competency.isBlank())
                .distinct()
                .toList();

        double weight = 1.0 / uniqueCompetencies.size();
        uniqueCompetencies.forEach(competency -> addCompetency(simulationVersionEntity, competency, weight));
    }

    public void applyBlueprintUpdate(SimulationVersionEntity simulationVersionEntity, UpdateBlueprintRequest request) {
        simulationVersionEntity.setRootNodeId(request.rootNodeId().trim());
        simulationVersionEntity.getCompetencies().clear();

        request.competencies()
                .stream()
                .map(competency -> new NormalizedCompetency(competency.name().trim(), competency.weight()))
                .forEach(competency -> addCompetency(simulationVersionEntity, competency.name(), competency.weight()));
    }

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

    public SimulationVersionDetailResponse toVersionDetail(SimulationVersionEntity simulationVersionEntity) {
        List<CompetencyWeightDto> competencies = simulationVersionEntity.getCompetencies().stream()
                .sorted(Comparator.comparing(SimulationCompetencyEntity::getName))
                .map(competency -> new CompetencyWeightDto(competency.getName(), competency.getWeight()))
                .toList();

        List<SimulationVersionDetailResponse.NodeDto> nodes = simulationVersionEntity.getNodes().stream()
                .sorted(Comparator.comparingInt(SimulationNodeEntity::getTurnIndex))
                .map(this::toNodeDetail)
                .toList();

        return new SimulationVersionDetailResponse(
                simulationVersionEntity.getSimulation().getId(),
                simulationVersionEntity.getSimulation().getName(),
                simulationVersionEntity.getSimulation().getDescription(),
                simulationVersionEntity.getSimulation().getCriticalSituation(),
                simulationVersionEntity.getSimulation().getResultUse(),
                simulationVersionEntity.getVersionNumber(),
                simulationVersionEntity.getStatus(),
                new SimulationVersionDetailResponse.BlueprintDto(
                        simulationVersionEntity.getRootNodeId(),
                        competencies
                ),
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
                simulationNodeEntity.getMediaUrl(),
                simulationNodeEntity.getMediaType(),
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
                simulationOptionEntity.getAuditNote(),
                simulationOptionEntity.getMediaUrl(),
                simulationOptionEntity.getMediaType()
        );
    }

    private SimulationVersionDetailResponse.NodeDto toNodeDetail(SimulationNodeEntity simulationNodeEntity) {
        List<SimulationVersionDetailResponse.OptionDto> options = simulationNodeEntity.getOptions().stream()
                .sorted(Comparator.comparing(SimulationOptionEntity::getOptionId))
                .map(this::toOptionDetail)
                .toList();

        return new SimulationVersionDetailResponse.NodeDto(
                simulationNodeEntity.getNodeId(),
                simulationNodeEntity.getTurnIndex(),
                simulationNodeEntity.getSpeaker(),
                simulationNodeEntity.getMessage(),
                simulationNodeEntity.getTimeLimitSeconds(),
                simulationNodeEntity.getMediaUrl(),
                simulationNodeEntity.getMediaType(),
                options
        );
    }

    private SimulationVersionDetailResponse.OptionDto toOptionDetail(SimulationOptionEntity simulationOptionEntity) {
        Map<String, Integer> competencyLevels = new LinkedHashMap<>();
        simulationOptionEntity.getCompetencyScores().stream()
                .sorted(Comparator.comparing(OptionCompetencyScoreEntity::getCompetencyName))
                .forEach(score -> competencyLevels.put(score.getCompetencyName(), score.getScore()));

        return new SimulationVersionDetailResponse.OptionDto(
                simulationOptionEntity.getOptionId(),
                simulationOptionEntity.getText(),
                competencyLevels,
                simulationOptionEntity.isCritical(),
                simulationOptionEntity.getNextNodeId(),
                simulationOptionEntity.getAuditNote(),
                simulationOptionEntity.getMediaUrl(),
                simulationOptionEntity.getMediaType()
        );
    }

    private void addCompetency(SimulationVersionEntity simulationVersionEntity, String name, double weight) {
        SimulationCompetencyEntity competencyEntity = new SimulationCompetencyEntity();
        competencyEntity.setSimulationVersion(simulationVersionEntity);
        competencyEntity.setName(name);
        competencyEntity.setWeight(weight);
        simulationVersionEntity.getCompetencies().add(competencyEntity);
    }

    private record NormalizedCompetency(String name, double weight) {
    }
}
