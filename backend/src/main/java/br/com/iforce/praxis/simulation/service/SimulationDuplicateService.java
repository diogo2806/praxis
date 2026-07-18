package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.simulation.dto.DuplicateSimulationRequest;
import br.com.iforce.praxis.simulation.dto.SimulationVersionDetailResponse;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationRepository;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Cria uma nova avaliação independente usando uma versão existente da própria empresa como base.
 * A origem permanece inalterada e a cópia sempre nasce como versão 1 em rascunho.
 */
@Service
public class SimulationDuplicateService {

    private final SimulationVersionRepository simulationVersionRepository;
    private final SimulationRepository simulationRepository;
    private final SimulationMapperService simulationMapperService;
    private final AuditEventService auditEventService;
    private final CurrentEmpresaService currentEmpresaService;

    public SimulationDuplicateService(
            SimulationVersionRepository simulationVersionRepository,
            SimulationRepository simulationRepository,
            SimulationMapperService simulationMapperService,
            AuditEventService auditEventService,
            CurrentEmpresaService currentEmpresaService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.simulationRepository = simulationRepository;
        this.simulationMapperService = simulationMapperService;
        this.auditEventService = auditEventService;
        this.currentEmpresaService = currentEmpresaService;
    }

    @Transactional
    public SimulationVersionDetailResponse duplicate(
            String simulationId,
            int versionNumber,
            DuplicateSimulationRequest request
    ) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        SimulationVersionEntity sourceVersion = simulationVersionRepository
                .findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                        empresaId,
                        simulationId,
                        versionNumber
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Não encontramos a avaliação usada como modelo."
                ));

        Instant createdAt = Instant.now();
        SimulationEntity sourceSimulation = sourceVersion.getSimulation();
        SimulationEntity duplicateSimulation = new SimulationEntity();
        duplicateSimulation.setId(generateSimulationId(request.name()));
        duplicateSimulation.setEmpresaId(empresaId);
        duplicateSimulation.setName(request.name().trim());
        duplicateSimulation.setDescription(sourceSimulation.getDescription());
        duplicateSimulation.setCriticalSituation(sourceSimulation.getCriticalSituation());
        duplicateSimulation.setResultUse(sourceSimulation.getResultUse());
        duplicateSimulation.setCreatedAt(createdAt);

        SimulationVersionEntity duplicateVersion = copyVersion(
                sourceVersion,
                duplicateSimulation,
                createdAt
        );
        duplicateSimulation.getVersions().add(duplicateVersion);

        SimulationEntity savedSimulation = simulationRepository.save(duplicateSimulation);
        auditEventService.appendSimulationVersionEvent(
                empresaId,
                savedSimulation.getId(),
                duplicateVersion.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_CLONED,
                "Avaliação duplicada como novo rascunho reutilizável.",
                "{\"sourceSimulationId\":\"" + escapeJson(simulationId)
                        + "\",\"sourceVersionNumber\":" + versionNumber + "}"
        );

        return simulationMapperService.toVersionDetail(duplicateVersion);
    }

    private SimulationVersionEntity copyVersion(
            SimulationVersionEntity sourceVersion,
            SimulationEntity targetSimulation,
            Instant createdAt
    ) {
        SimulationVersionEntity targetVersion = new SimulationVersionEntity();
        targetVersion.setSimulation(targetSimulation);
        targetVersion.setVersionNumber(1);
        targetVersion.setStatus(SimulationVersionStatus.DRAFT);
        targetVersion.setRootNodeId(sourceVersion.getRootNodeId());
        targetVersion.setCreatedAt(createdAt);
        targetVersion.setPublishedAt(null);

        for (SimulationCompetencyEntity sourceCompetency : sourceVersion.getCompetencies()) {
            SimulationCompetencyEntity targetCompetency = new SimulationCompetencyEntity();
            targetCompetency.setSimulationVersion(targetVersion);
            targetCompetency.setName(sourceCompetency.getName());
            targetCompetency.setWeight(sourceCompetency.getWeight());
            targetCompetency.setTargetScore(sourceCompetency.getTargetScore());
            targetCompetency.setTier(sourceCompetency.getTier());
            targetVersion.getCompetencies().add(targetCompetency);
        }

        for (SimulationNodeEntity sourceNode : sourceVersion.getNodes()) {
            targetVersion.getNodes().add(copyNode(sourceNode, targetVersion));
        }
        return targetVersion;
    }

    private SimulationNodeEntity copyNode(
            SimulationNodeEntity sourceNode,
            SimulationVersionEntity targetVersion
    ) {
        SimulationNodeEntity targetNode = new SimulationNodeEntity();
        targetNode.setSimulationVersion(targetVersion);
        targetNode.setNodeId(sourceNode.getNodeId());
        targetNode.setTurnIndex(sourceNode.getTurnIndex());
        targetNode.setSpeaker(sourceNode.getSpeaker());
        targetNode.setMessage(sourceNode.getMessage());
        targetNode.setTimeLimitSeconds(sourceNode.getTimeLimitSeconds());
        targetNode.setTimeoutNextNodeId(sourceNode.getTimeoutNextNodeId());
        targetNode.setFinal(sourceNode.isFinal());
        targetNode.setReportText(sourceNode.getReportText());
        targetNode.setPositionX(sourceNode.getPositionX());
        targetNode.setPositionY(sourceNode.getPositionY());
        targetNode.setPlainTextDescription(sourceNode.getPlainTextDescription());
        targetNode.setAudioDescriptionUrl(sourceNode.getAudioDescriptionUrl());
        targetNode.setMediaUrl(sourceNode.getMediaUrl());
        targetNode.setMediaType(sourceNode.getMediaType());

        for (SimulationOptionEntity sourceOption : sourceNode.getOptions()) {
            targetNode.getOptions().add(copyOption(sourceOption, targetNode));
        }
        return targetNode;
    }

    private SimulationOptionEntity copyOption(
            SimulationOptionEntity sourceOption,
            SimulationNodeEntity targetNode
    ) {
        SimulationOptionEntity targetOption = new SimulationOptionEntity();
        targetOption.setSimulationNode(targetNode);
        targetOption.setOptionId(sourceOption.getOptionId());
        targetOption.setText(sourceOption.getText());
        targetOption.setNextNodeId(sourceOption.getNextNodeId());
        targetOption.setCritical(sourceOption.isCritical());
        targetOption.setAuditNote(sourceOption.getAuditNote());
        targetOption.setPlainTextDescription(sourceOption.getPlainTextDescription());
        targetOption.setAudioDescriptionUrl(sourceOption.getAudioDescriptionUrl());
        targetOption.setMediaUrl(sourceOption.getMediaUrl());
        targetOption.setMediaType(sourceOption.getMediaType());

        for (OptionCompetencyScoreEntity sourceScore : sourceOption.getCompetencyScores()) {
            OptionCompetencyScoreEntity targetScore = new OptionCompetencyScoreEntity();
            targetScore.setSimulationOption(targetOption);
            targetScore.setCompetencyName(sourceScore.getCompetencyName());
            targetScore.setScore(sourceScore.getScore());
            targetOption.getCompetencyScores().add(targetScore);
        }
        return targetOption;
    }

    private String generateSimulationId(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String base = normalized.isBlank() ? "avaliacao" : normalized;
        if (base.length() > 80) {
            base = base.substring(0, 80).replaceAll("-$", "");
        }

        String id;
        do {
            id = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        } while (simulationRepository.existsById(id));
        return id;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
