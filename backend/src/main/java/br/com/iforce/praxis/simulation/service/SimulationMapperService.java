package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;

import br.com.iforce.praxis.gupy.model.ResultTier;

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

import java.util.LinkedHashSet;

import java.util.List;

import java.util.Locale;

import java.util.Map;

import java.util.Set;


/**
 * Tradutor entre o formato de armazenamento da prova e os formatos de uso.
 *
 * <p>Na visão do processo, é o "tradutor" que converte a prova entre três
 * mundos: como ela é guardada no banco, como é exibida na tela de autoria e
 * como é entregue ao candidato/integrações já publicada. Também aplica o
 * plano da avaliação (competências e pesos) ao montar ou editar uma prova.
 * Não contém regra de negócio sensível: apenas organiza e converte os
 * dados.</p>
 */
@Service
public class SimulationMapperService {

    /**
     * Aplica o plano inicial a uma prova recém-criada.
     *
     * <p>Define a etapa inicial e cadastra as competências informadas,
     * distribuindo o peso igualmente entre elas (cada competência começa com
     * a mesma importância).</p>
     *
     * @param simulationVersionEntity a versão da prova a configurar
     * @param rootNodeId a etapa por onde a prova começa
     * @param competencies as competências avaliadas
     */
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

    /**
     * Atualiza o plano da avaliação de uma prova em edição.
     *
     * <p>Ajusta a etapa inicial e reconcilia a lista de competências:
     * atualiza as que continuam, adiciona as novas e remove as que saíram —
     * preservando o peso e a meta de cada uma. A reconciliação é feita "no
     * lugar" para evitar conflitos no banco quando um nome de competência é
     * mantido.</p>
     *
     * @param simulationVersionEntity a versão da prova a atualizar
     * @param request o novo plano (etapa inicial, competências, pesos e metas)
     */
    public void applyBlueprintUpdate(SimulationVersionEntity simulationVersionEntity, UpdateBlueprintRequest request) {
        simulationVersionEntity.setRootNodeId(request.rootNodeId().trim());

        List<NormalizedCompetency> requestedCompetencies = request.competencies()
                .stream()
                .map(competency -> new NormalizedCompetency(
                        competency.name().trim(),
                        competency.weight(),
                        competency.normalizedTargetScore(),
                        competency.normalizedTier()
                ))
                .toList();

        // Reconciliamos a coleção no lugar (atualizando/removendo/inserindo) em vez de
        // limpar e recriar. Um clear() seguido de re-inserção agenda DELETEs e INSERTs na
        // mesma transação, e o Hibernate executa os INSERTs antes dos DELETEs, violando a
        // unique constraint uk_simulation_competency quando o nome de uma competência é mantido.
        Map<String, SimulationCompetencyEntity> existingByName = new LinkedHashMap<>();
        for (SimulationCompetencyEntity competencyEntity : simulationVersionEntity.getCompetencies()) {
            existingByName.put(normalizeName(competencyEntity.getName()), competencyEntity);
        }

        Set<String> requestedNames = new LinkedHashSet<>();
        for (NormalizedCompetency requested : requestedCompetencies) {
            String key = normalizeName(requested.name());
            requestedNames.add(key);
            SimulationCompetencyEntity existing = existingByName.get(key);
            if (existing != null) {
                existing.setName(requested.name());
                existing.setWeight(requested.weight());
                existing.setTargetScore(requested.targetScore());
                existing.setTier(requested.tier());
            } else {
                addCompetency(
                        simulationVersionEntity,
                        requested.name(),
                        requested.weight(),
                        requested.targetScore(),
                        requested.tier()
                );
            }
        }

        simulationVersionEntity.getCompetencies()
                .removeIf(competencyEntity -> !requestedNames.contains(normalizeName(competencyEntity.getName())));
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Converte a prova armazenada no formato usado na execução (publicada).
     *
     * <p>É a forma como a prova é entregue ao candidato e às integrações:
     * competências com pesos, etapas e respostas já organizadas. Uso interno
     * por outros serviços ao aplicar a prova.</p>
     *
     * @param simulationVersionEntity a versão da prova armazenada
     * @return a prova no formato de execução
     */
    public PublishedSimulation toPublishedSimulation(SimulationVersionEntity simulationVersionEntity) {
        List<SimulationCompetencyEntity> sortedCompetencies = simulationVersionEntity.getCompetencies().stream()
                .sorted(Comparator.comparing(SimulationCompetencyEntity::getName))
                .toList();

        List<String> competencies = sortedCompetencies.stream()
                .map(SimulationCompetencyEntity::getName)
                .toList();

        Map<String, Double> competencyWeights = new LinkedHashMap<>();
        sortedCompetencies.forEach(competency -> competencyWeights.put(competency.getName(), competency.getWeight()));

        Map<String, ResultTier> competencyTiers = new LinkedHashMap<>();
        sortedCompetencies.forEach(competency -> competencyTiers.put(competency.getName(), competency.getTier()));

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
                competencyTiers,
                simulationVersionEntity.getRootNodeId(),
                nodes
        );
    }

    /**
     * Converte a prova armazenada no formato detalhado da tela de autoria.
     *
     * <p>Inclui tudo o que o autor precisa ver e editar: dados gerais da
     * prova, competências com pesos e metas, etapas, respostas e posições no
     * editor visual.</p>
     *
     * @param simulationVersionEntity a versão da prova armazenada
     * @return os detalhes completos da versão para a tela de autoria
     */
    public SimulationVersionDetailResponse toVersionDetail(SimulationVersionEntity simulationVersionEntity) {
        List<CompetencyWeightDto> competencies = simulationVersionEntity.getCompetencies().stream()
                .sorted(Comparator.comparing(SimulationCompetencyEntity::getName))
                .map(competency -> new CompetencyWeightDto(
                        competency.getName(),
                        competency.getWeight(),
                        competency.getTargetScore(),
                        competency.getTier()
                ))
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
                simulationNodeEntity.getTimeoutNextNodeId(),
                simulationNodeEntity.isFinal(),
                simulationNodeEntity.getReportText(),
                simulationNodeEntity.getPlainTextDescription(),
                simulationNodeEntity.getAudioDescriptionUrl(),
                simulationNodeEntity.getMediaUrl(),
                simulationNodeEntity.getMediaType(),
                simulationNodeEntity.getMediaTranscript(),
                simulationNodeEntity.getMediaCaptionsUrl(),
                simulationNodeEntity.getMediaVersion(),
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
                simulationOptionEntity.getPlainTextDescription(),
                simulationOptionEntity.getAudioDescriptionUrl(),
                simulationOptionEntity.getMediaUrl(),
                simulationOptionEntity.getMediaType(),
                simulationOptionEntity.getMediaTranscript(),
                simulationOptionEntity.getMediaCaptionsUrl(),
                simulationOptionEntity.getMediaVersion()
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
                simulationNodeEntity.getTimeoutNextNodeId(),
                simulationNodeEntity.isFinal(),
                simulationNodeEntity.getReportText(),
                simulationNodeEntity.getPositionX(),
                simulationNodeEntity.getPositionY(),
                simulationNodeEntity.getPlainTextDescription(),
                simulationNodeEntity.getAudioDescriptionUrl(),
                simulationNodeEntity.getMediaUrl(),
                simulationNodeEntity.getMediaType(),
                simulationNodeEntity.getMediaTranscript(),
                simulationNodeEntity.getMediaCaptionsUrl(),
                simulationNodeEntity.getMediaVersion(),
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
                simulationOptionEntity.getPlainTextDescription(),
                simulationOptionEntity.getAudioDescriptionUrl(),
                simulationOptionEntity.getMediaUrl(),
                simulationOptionEntity.getMediaType(),
                simulationOptionEntity.getMediaTranscript(),
                simulationOptionEntity.getMediaCaptionsUrl(),
                simulationOptionEntity.getMediaVersion()
        );
    }

    private void addCompetency(SimulationVersionEntity simulationVersionEntity, String name, double weight) {
        addCompetency(simulationVersionEntity, name, weight, 70, ResultTier.MAJOR);
    }

    private void addCompetency(
            SimulationVersionEntity simulationVersionEntity,
            String name,
            double weight,
            int targetScore,
            ResultTier tier
    ) {
        SimulationCompetencyEntity competencyEntity = new SimulationCompetencyEntity();
        competencyEntity.setSimulationVersion(simulationVersionEntity);
        competencyEntity.setName(name);
        competencyEntity.setWeight(weight);
        competencyEntity.setTargetScore(targetScore);
        competencyEntity.setTier(tier);
        simulationVersionEntity.getCompetencies().add(competencyEntity);
    }

    private record NormalizedCompetency(String name, double weight, int targetScore, ResultTier tier) {
    }
}
