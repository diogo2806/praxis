package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.simulation.dto.ArchiveSimulationResponse;
import br.com.iforce.praxis.simulation.dto.CloneSimulationVersionResponse;
import br.com.iforce.praxis.simulation.dto.CompetencyWeightDto;
import br.com.iforce.praxis.simulation.dto.CreateNodeRequest;
import br.com.iforce.praxis.simulation.dto.CreateOptionRequest;
import br.com.iforce.praxis.simulation.dto.CreateSimulationDraftRequest;
import br.com.iforce.praxis.simulation.dto.CreateSimulationRequest;
import br.com.iforce.praxis.simulation.dto.GupyPreflightResponse;
import br.com.iforce.praxis.simulation.dto.PublishSimulationResponse;
import br.com.iforce.praxis.simulation.dto.SimulationSummaryResponse;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.dto.SimulationVersionDetailResponse;
import br.com.iforce.praxis.simulation.dto.SimulationVersionStatusResponse;
import br.com.iforce.praxis.simulation.dto.UpdateBlueprintRequest;
import br.com.iforce.praxis.simulation.dto.UpdateNodeRequest;
import br.com.iforce.praxis.simulation.dto.UpdateOptionRequest;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.model.MediaType;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class SimulationAdminService {

    private static final int MAX_OPTIONS_PER_NODE = 4;

    private final SimulationVersionRepository simulationVersionRepository;
    private final SimulationRepository simulationRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationValidationService simulationValidationService;
    private final GupyPreflightService gupyPreflightService;
    private final SimulationMapperService simulationMapperService;
    private final AuditEventService auditEventService;
    private final CurrentTenantService currentTenantService;

    public SimulationAdminService(
            SimulationVersionRepository simulationVersionRepository,
            SimulationRepository simulationRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            SimulationValidationService simulationValidationService,
            GupyPreflightService gupyPreflightService,
            SimulationMapperService simulationMapperService,
            AuditEventService auditEventService,
            CurrentTenantService currentTenantService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.simulationRepository = simulationRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationValidationService = simulationValidationService;
        this.gupyPreflightService = gupyPreflightService;
        this.simulationMapperService = simulationMapperService;
        this.auditEventService = auditEventService;
        this.currentTenantService = currentTenantService;
    }

    @Transactional(readOnly = true)
    public List<SimulationSummaryResponse> listActiveSimulations() {
        return simulationRepository.findByTenantIdAndArchivedFalseAndDeletedAtIsNullOrderByCreatedAtDesc(
                        currentTenantService.requiredTenantId())
                .stream()
                .map(this::toLatestVersionSummary)
                .flatMap(List::stream)
                .toList();
    }

    @Transactional
    public SimulationVersionDetailResponse createSimulation(CreateSimulationRequest request) {
        simulationValidationService.validateWeights(request.competencies());

        Instant createdAt = Instant.now();
        SimulationEntity simulationEntity = new SimulationEntity();
        simulationEntity.setId(generateSimulationId(request.name()));
        simulationEntity.setTenantId(currentTenantService.requiredTenantId());
        simulationEntity.setName(request.name().trim());
        simulationEntity.setDescription(createDescription(request));
        simulationEntity.setCreatedAt(createdAt);
        simulationEntity.setArchived(false);

        SimulationVersionEntity versionEntity = new SimulationVersionEntity();
        versionEntity.setSimulation(simulationEntity);
        versionEntity.setVersionNumber(1);
        versionEntity.setStatus(SimulationVersionStatus.DRAFT);
        versionEntity.setRootNodeId("turno-1");
        versionEntity.setCreatedAt(createdAt);

        applyCompetencyWeights(versionEntity, request.competencies());

        SimulationNodeEntity rootNodeEntity = new SimulationNodeEntity();
        rootNodeEntity.setSimulationVersion(versionEntity);
        rootNodeEntity.setNodeId(versionEntity.getRootNodeId());
        rootNodeEntity.setTurnIndex(1);
        rootNodeEntity.setSpeaker("Cliente");
        rootNodeEntity.setMessage(defaultIfBlank(request.criticalSituation(), "Descreva a situacao critica do primeiro turno."));
        rootNodeEntity.setTimeLimitSeconds(null);
        versionEntity.getNodes().add(rootNodeEntity);

        simulationEntity.getVersions().add(versionEntity);
        SimulationEntity savedSimulationEntity = simulationRepository.save(simulationEntity);
        auditEventService.appendSimulationVersionEvent(
                savedSimulationEntity.getTenantId(),
                savedSimulationEntity.getId(),
                versionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_DRAFT_CREATED,
                "Simulacao criada com versao inicial em rascunho.",
                "{\"status\":\"draft\"}"
        );

        return toDetailResponse(versionEntity);
    }

    @Transactional(readOnly = true)
    public SimulationVersionDetailResponse loadVersion(String simulationId, int versionNumber) {
        return toDetailResponse(findVersion(simulationId, versionNumber));
    }

    @Transactional
    public SimulationSummaryResponse createDraftSimulation(CreateSimulationDraftRequest request) {
        Instant createdAt = Instant.now();
        SimulationEntity simulationEntity = new SimulationEntity();
        simulationEntity.setId(generateSimulationId(request.name()));
        simulationEntity.setTenantId(currentTenantService.requiredTenantId());
        simulationEntity.setName(request.name().trim());
        simulationEntity.setDescription(createDraftDescription(request));
        simulationEntity.setCreatedAt(createdAt);
        simulationEntity.setArchived(false);

        SimulationVersionEntity versionEntity = new SimulationVersionEntity();
        versionEntity.setSimulation(simulationEntity);
        versionEntity.setVersionNumber(1);
        versionEntity.setStatus(SimulationVersionStatus.DRAFT);
        versionEntity.setCreatedAt(createdAt);

        requireUniqueCompetencyNames(request.competencies());
        simulationMapperService.applyInitialBlueprint(versionEntity, request.rootNodeId(), request.competencies());

        simulationEntity.getVersions().add(versionEntity);
        SimulationEntity savedSimulationEntity = simulationRepository.save(simulationEntity);
        auditEventService.appendSimulationVersionEvent(
                savedSimulationEntity.getTenantId(),
                savedSimulationEntity.getId(),
                versionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_DRAFT_CREATED,
                "Versao inicial criada a partir do plano da avaliacao.",
                "{\"status\":\"draft\"}"
        );

        return toSummaryResponse(savedSimulationEntity, versionEntity);
    }

    @Transactional(readOnly = true)
    public SimulationVersionDetailResponse getSimulationVersion(String simulationId, int versionNumber) {
        return simulationMapperService.toVersionDetail(findVersion(simulationId, versionNumber));
    }

    @Transactional(readOnly = true)
    public SimulationValidationResponse validateVersion(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        return simulationValidationService.validate(simulationVersionEntity);
    }

    @Transactional
    public SimulationSummaryResponse updateBlueprint(String simulationId, int versionNumber, UpdateBlueprintRequest request) {
        simulationValidationService.validateBlueprintWeights(request.competencies());
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        requireUniqueCompetencyNames(request.competencies()
                .stream()
                .map(UpdateBlueprintRequest.CompetencyRequest::name)
                .toList());
        simulationMapperService.applyBlueprintUpdate(versionEntity, request);
        SimulationVersionEntity savedVersionEntity = simulationVersionRepository.save(versionEntity);

        auditEventService.appendSimulationVersionEvent(
                savedVersionEntity.getSimulation().getTenantId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_VERSION_BLUEPRINT_UPDATED,
                "Plano da avaliacao atualizado.",
                "{\"rootNodeId\":\"" + escapeJson(savedVersionEntity.getRootNodeId())
                        + "\",\"competencyCount\":" + savedVersionEntity.getCompetencies().size() + "}"
        );

        return toSummaryResponse(savedVersionEntity.getSimulation(), savedVersionEntity);
    }

    @Transactional
    public String addNode(String simulationId, int versionNumber, CreateNodeRequest request) {
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        String nodeId = nextNodeId(versionEntity);

        SimulationNodeEntity nodeEntity = new SimulationNodeEntity();
        nodeEntity.setSimulationVersion(versionEntity);
        nodeEntity.setNodeId(nodeId);
        nodeEntity.setTurnIndex(nextTurnIndex(versionEntity));
        nodeEntity.setSpeaker("Cliente");
        nodeEntity.setMessage(request.clientMessage().trim());
        nodeEntity.setTimeLimitSeconds(request.timeLimitSeconds());
        applyMedia(request.mediaUrl(), request.mediaType(), nodeEntity::setMediaUrl, nodeEntity::setMediaType);
        versionEntity.getNodes().add(nodeEntity);

        simulationVersionRepository.save(versionEntity);
        auditEventService.appendSimulationVersionEvent(
                versionEntity.getSimulation().getTenantId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_NODE_ADDED,
                "No de simulacao adicionado.",
                "{\"nodeId\":\"" + escapeJson(nodeId) + "\"}"
        );

        return nodeId;
    }

    @Transactional
    public void updateNode(String simulationId, int versionNumber, String nodeId, UpdateNodeRequest request) {
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        SimulationNodeEntity nodeEntity = findNode(versionEntity, nodeId);

        if (request.clientMessage() != null) {
            nodeEntity.setMessage(request.clientMessage().trim());
        }
        if (request.timeLimitSeconds() != null) {
            nodeEntity.setTimeLimitSeconds(request.timeLimitSeconds());
        }
        if (request.mediaUrl() != null) {
            applyMedia(request.mediaUrl(), request.mediaType(), nodeEntity::setMediaUrl, nodeEntity::setMediaType);
        }

        simulationVersionRepository.save(versionEntity);
        auditEventService.appendSimulationVersionEvent(
                versionEntity.getSimulation().getTenantId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_NODE_UPDATED,
                "No de simulacao atualizado.",
                "{\"nodeId\":\"" + escapeJson(nodeId) + "\"}"
        );
    }

    @Transactional
    public void deleteNode(String simulationId, int versionNumber, String nodeId) {
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        if (Objects.equals(versionEntity.getRootNodeId(), nodeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No raiz nao pode ser removido.");
        }

        SimulationNodeEntity nodeEntity = findNode(versionEntity, nodeId);
        versionEntity.getNodes().remove(nodeEntity);
        simulationVersionRepository.save(versionEntity);
        auditEventService.appendSimulationVersionEvent(
                versionEntity.getSimulation().getTenantId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_NODE_DELETED,
                "No de simulacao removido.",
                "{\"nodeId\":\"" + escapeJson(nodeId) + "\"}"
        );
    }

    @Transactional
    public String addOption(String simulationId, int versionNumber, String nodeId, CreateOptionRequest request) {
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        SimulationNodeEntity nodeEntity = findNode(versionEntity, nodeId);
        assertCanAddOption(nodeEntity);
        assertCompetencyScores(versionEntity, request.competencyLevels());
        assertNodeExistsWhenProvided(versionEntity, request.nextNodeId());

        String optionId = nextOptionId(nodeEntity);
        SimulationOptionEntity optionEntity = new SimulationOptionEntity();
        optionEntity.setSimulationNode(nodeEntity);
        optionEntity.setOptionId(optionId);
        optionEntity.setText(request.text().trim());
        optionEntity.setNextNodeId(trimToNull(request.nextNodeId()));
        optionEntity.setCritical(request.isCritical());
        optionEntity.setAuditNote(defaultIfBlank(request.resultingTone(), ""));
        applyMedia(request.mediaUrl(), request.mediaType(), optionEntity::setMediaUrl, optionEntity::setMediaType);
        applyCompetencyScores(optionEntity, request.competencyLevels());
        nodeEntity.getOptions().add(optionEntity);

        simulationVersionRepository.save(versionEntity);
        auditEventService.appendSimulationVersionEvent(
                versionEntity.getSimulation().getTenantId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_OPTION_ADDED,
                "Alternativa de simulacao adicionada.",
                "{\"nodeId\":\"" + escapeJson(nodeId) + "\",\"optionId\":\"" + escapeJson(optionId) + "\"}"
        );

        return optionId;
    }

    @Transactional
    public void updateOption(
            String simulationId,
            int versionNumber,
            String nodeId,
            String optionId,
            UpdateOptionRequest request
    ) {
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        SimulationNodeEntity nodeEntity = findNode(versionEntity, nodeId);
        SimulationOptionEntity optionEntity = findOption(nodeEntity, optionId);

        if (request.text() != null) {
            optionEntity.setText(request.text().trim());
        }
        if (request.nextNodeId() != null) {
            assertNodeExistsWhenProvided(versionEntity, request.nextNodeId());
            optionEntity.setNextNodeId(trimToNull(request.nextNodeId()));
        }
        if (request.isCritical() != null) {
            optionEntity.setCritical(request.isCritical());
        }
        if (request.resultingTone() != null) {
            optionEntity.setAuditNote(defaultIfBlank(request.resultingTone(), ""));
        }
        if (request.mediaUrl() != null) {
            applyMedia(request.mediaUrl(), request.mediaType(), optionEntity::setMediaUrl, optionEntity::setMediaType);
        }
        if (request.competencyLevels() != null) {
            assertCompetencyScores(versionEntity, request.competencyLevels());
            replaceCompetencyScores(optionEntity, request.competencyLevels());
        }

        simulationVersionRepository.save(versionEntity);
        auditEventService.appendSimulationVersionEvent(
                versionEntity.getSimulation().getTenantId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_OPTION_UPDATED,
                "Alternativa de simulacao atualizada.",
                "{\"nodeId\":\"" + escapeJson(nodeId) + "\",\"optionId\":\"" + escapeJson(optionId) + "\"}"
        );
    }

    @Transactional
    public void deleteOption(String simulationId, int versionNumber, String nodeId, String optionId) {
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        SimulationNodeEntity nodeEntity = findNode(versionEntity, nodeId);
        SimulationOptionEntity optionEntity = findOption(nodeEntity, optionId);

        nodeEntity.getOptions().remove(optionEntity);
        simulationVersionRepository.save(versionEntity);
        auditEventService.appendSimulationVersionEvent(
                versionEntity.getSimulation().getTenantId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_OPTION_DELETED,
                "Alternativa de simulacao removida.",
                "{\"nodeId\":\"" + escapeJson(nodeId) + "\",\"optionId\":\"" + escapeJson(optionId) + "\"}"
        );
    }

    @Transactional
    public ArchiveSimulationResponse archiveSimulation(String simulationId, String deletedBy) {
        if (deletedBy == null || deletedBy.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado obrigatorio.");
        }

        String tenantId = currentTenantService.requiredTenantId();
        SimulationEntity simulationEntity = simulationRepository.findByTenantIdAndId(tenantId, simulationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulacao nao encontrada."));

        if (simulationEntity.isArchived() || simulationEntity.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Simulacao ja arquivada.");
        }

        Instant deletedAt = Instant.now();
        simulationEntity.setArchived(true);
        simulationEntity.setDeletedAt(deletedAt);
        simulationEntity.setDeletedBy(deletedBy);
        SimulationEntity savedSimulationEntity = simulationRepository.save(simulationEntity);
        auditEventService.appendSimulationEvent(
                savedSimulationEntity.getTenantId(),
                savedSimulationEntity.getId(),
                AuditEventType.SIMULATION_ARCHIVED,
                "Simulacao arquivada por soft delete.",
                "{\"archived\":true,\"deletedAt\":\"" + savedSimulationEntity.getDeletedAt()
                        + "\",\"deletedBy\":\"" + escapeJson(deletedBy) + "\"}"
        );

        return new ArchiveSimulationResponse(
                savedSimulationEntity.getId(),
                savedSimulationEntity.isArchived(),
                savedSimulationEntity.getDeletedAt(),
                savedSimulationEntity.getDeletedBy()
        );
    }

    @Transactional
    public CloneSimulationVersionResponse clonePublishedVersionToDraft(String simulationId, int versionNumber) {
        SimulationVersionEntity sourceVersionEntity = findVersion(simulationId, versionNumber);
        requireStatus(
                sourceVersionEntity,
                "Somente versoes publicadas podem ser clonadas para edicao.",
                SimulationVersionStatus.PUBLISHED
        );

        int newVersionNumber = nextVersionNumber(simulationId);
        SimulationVersionEntity clonedVersionEntity = cloneVersion(sourceVersionEntity, newVersionNumber);
        SimulationVersionEntity savedClonedVersionEntity = simulationVersionRepository.save(clonedVersionEntity);
        auditEventService.appendSimulationVersionEvent(
                savedClonedVersionEntity.getSimulation().getTenantId(),
                savedClonedVersionEntity.getSimulation().getId(),
                savedClonedVersionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_CLONED,
                "Versao publicada clonada para novo rascunho.",
                "{\"status\":\"" + savedClonedVersionEntity.getStatus().getDescricao()
                        + "\",\"sourceVersionNumber\":" + sourceVersionEntity.getVersionNumber() + "}"
        );

        return new CloneSimulationVersionResponse(
                savedClonedVersionEntity.getSimulation().getId(),
                sourceVersionEntity.getVersionNumber(),
                savedClonedVersionEntity.getVersionNumber(),
                savedClonedVersionEntity.getStatus()
        );
    }

    @Transactional
    public SimulationVersionStatusResponse submitVersionForReview(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        requireStatus(
                simulationVersionEntity,
                "Somente versoes em rascunho ou reprovadas podem ir para revisao.",
                SimulationVersionStatus.DRAFT,
                SimulationVersionStatus.REJECTED
        );

        SimulationValidationResponse validationResponse = simulationValidationService.validate(simulationVersionEntity);
        if (!validationResponse.publishable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Revisao bloqueada por itens criticos do validador.");
        }

        simulationVersionEntity.setStatus(SimulationVersionStatus.IN_REVIEW);
        SimulationVersionEntity savedSimulationVersionEntity = simulationVersionRepository.save(simulationVersionEntity);
        auditEventService.appendSimulationVersionEvent(
                savedSimulationVersionEntity.getSimulation().getTenantId(),
                savedSimulationVersionEntity.getSimulation().getId(),
                savedSimulationVersionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_SUBMITTED_FOR_REVIEW,
                "Versao de simulacao enviada para revisao.",
                "{\"status\":\"" + savedSimulationVersionEntity.getStatus().getDescricao()
                        + "\",\"warningCount\":" + validationResponse.warningCount() + "}"
        );

        return toStatusResponse(savedSimulationVersionEntity);
    }

    @Transactional
    public SimulationVersionStatusResponse approveVersion(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        requireStatus(
                simulationVersionEntity,
                "Somente versoes em revisao podem ser aprovadas.",
                SimulationVersionStatus.IN_REVIEW
        );

        simulationVersionEntity.setStatus(SimulationVersionStatus.APPROVED);
        SimulationVersionEntity savedSimulationVersionEntity = simulationVersionRepository.save(simulationVersionEntity);
        auditEventService.appendSimulationVersionEvent(
                savedSimulationVersionEntity.getSimulation().getTenantId(),
                savedSimulationVersionEntity.getSimulation().getId(),
                savedSimulationVersionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_APPROVED,
                "Versao de simulacao aprovada para publicacao.",
                "{\"status\":\"" + savedSimulationVersionEntity.getStatus().getDescricao() + "\"}"
        );

        return toStatusResponse(savedSimulationVersionEntity);
    }

    @Transactional
    public SimulationVersionStatusResponse rejectVersion(String simulationId, int versionNumber, String reason) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        requireStatus(
                simulationVersionEntity,
                "Somente versoes em revisao podem ser reprovadas.",
                SimulationVersionStatus.IN_REVIEW
        );

        simulationVersionEntity.setStatus(SimulationVersionStatus.REJECTED);
        SimulationVersionEntity savedSimulationVersionEntity = simulationVersionRepository.save(simulationVersionEntity);
        auditEventService.appendSimulationVersionEvent(
                savedSimulationVersionEntity.getSimulation().getTenantId(),
                savedSimulationVersionEntity.getSimulation().getId(),
                savedSimulationVersionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_REJECTED,
                "Versao de simulacao reprovada na revisao.",
                "{\"status\":\"" + savedSimulationVersionEntity.getStatus().getDescricao()
                        + "\",\"reason\":\"" + escapeJson(reason) + "\"}"
        );

        return toStatusResponse(savedSimulationVersionEntity);
    }

    @Transactional
    public PublishSimulationResponse publishVersion(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        SimulationValidationResponse validationResponse = simulationValidationService.validate(simulationVersionEntity);

        if (!validationResponse.publishable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Publicacao bloqueada por itens criticos do validador.");
        }

        if (simulationVersionEntity.getStatus() != SimulationVersionStatus.APPROVED
                && simulationVersionEntity.getStatus() != SimulationVersionStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Versao precisa estar aprovada antes da publicacao.");
        }

        GupyPreflightResponse gupyPreflightResponse = gupyPreflightService.evaluate(simulationVersionEntity);
        if (!gupyPreflightResponse.ok()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Preflight Gupy bloqueou a publicacao.");
        }

        archiveOtherPublishedVersions(simulationVersionEntity);

        Instant publishedAt = Instant.now();
        simulationVersionEntity.setStatus(SimulationVersionStatus.PUBLISHED);
        simulationVersionEntity.setPublishedAt(publishedAt);
        SimulationVersionEntity savedSimulationVersionEntity = simulationVersionRepository.save(simulationVersionEntity);
        auditEventService.appendSimulationVersionEvent(
                savedSimulationVersionEntity.getSimulation().getTenantId(),
                savedSimulationVersionEntity.getSimulation().getId(),
                savedSimulationVersionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_PUBLISHED,
                "Versao de simulacao publicada.",
                "{\"status\":\"" + savedSimulationVersionEntity.getStatus().getDescricao()
                        + "\",\"publishedAt\":\"" + savedSimulationVersionEntity.getPublishedAt()
                        + "\",\"warningCount\":" + validationResponse.warningCount() + "}"
        );

        return new PublishSimulationResponse(
                savedSimulationVersionEntity.getSimulation().getId(),
                savedSimulationVersionEntity.getVersionNumber(),
                savedSimulationVersionEntity.getStatus(),
                savedSimulationVersionEntity.getPublishedAt()
        );
    }

    private String createDescription(CreateSimulationRequest request) {
        String description = "Cargo: " + request.role().trim();
        if (request.seniority() != null && !request.seniority().isBlank()) {
            description += " - Senioridade: " + request.seniority().trim();
        }
        if (request.objective() != null && !request.objective().isBlank()) {
            description += " - Objetivo: " + request.objective().trim();
        }
        if (request.criticalError() != null && !request.criticalError().isBlank()) {
            description += " - Erro critico: " + request.criticalError().trim();
        }

        return description.length() > 1000 ? description.substring(0, 1000) : description;
    }

    private String createDraftDescription(CreateSimulationDraftRequest request) {
        boolean hasStructuredBlueprint = hasText(request.criticalSituation())
                || hasText(request.resultUse());

        if (!hasStructuredBlueprint) {
            return truncateDescription(request.description().trim());
        }

        StringBuilder description = new StringBuilder();
        appendLine(description, "Cargo", request.name());
        appendLine(description, "Situação crítica", request.criticalSituation());
        if (request.competencies() != null && !request.competencies().isEmpty()) {
            appendLine(description, "Competências", String.join(", ", request.competencies()));
        }
        appendLine(description, "Uso do resultado", request.resultUse());
        return truncateDescription(description.toString().trim());
    }

    private void appendLine(StringBuilder description, String label, String value) {
        if (!hasText(value)) {
            return;
        }
        if (!description.isEmpty()) {
            description.append('\n');
        }
        description.append(label).append(": ").append(value.trim());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String truncateDescription(String description) {
        return description.length() > 1000 ? description.substring(0, 997).stripTrailing() + "..." : description;
    }

    private SimulationVersionDetailResponse toDetailResponse(SimulationVersionEntity versionEntity) {
        return simulationMapperService.toVersionDetail(versionEntity);
    }

    private SimulationVersionEntity findAndAssertDraft(String simulationId, int versionNumber) {
        SimulationVersionEntity versionEntity = findVersion(simulationId, versionNumber);
        requireStatus(
                versionEntity,
                "Somente versoes em rascunho ou reprovadas podem ser editadas.",
                SimulationVersionStatus.DRAFT,
                SimulationVersionStatus.REJECTED
        );
        return versionEntity;
    }

    private void applyCompetencyWeights(
            SimulationVersionEntity versionEntity,
            List<CompetencyWeightDto> competencies
    ) {
        requireUniqueCompetencyNames(competencies.stream().map(CompetencyWeightDto::name).toList());
        versionEntity.getCompetencies().clear();
        for (CompetencyWeightDto competency : competencies) {
            SimulationCompetencyEntity competencyEntity = new SimulationCompetencyEntity();
            competencyEntity.setSimulationVersion(versionEntity);
            competencyEntity.setName(competency.name().trim());
            competencyEntity.setWeight(competency.weight());
            versionEntity.getCompetencies().add(competencyEntity);
        }
    }

    private void applyCompetencyScores(
            SimulationOptionEntity optionEntity,
            Map<String, Integer> competencyLevels
    ) {
        competencyLevels.forEach((competencyName, score) -> {
            OptionCompetencyScoreEntity scoreEntity = new OptionCompetencyScoreEntity();
            scoreEntity.setSimulationOption(optionEntity);
            scoreEntity.setCompetencyName(competencyName.trim());
            scoreEntity.setScore(score);
            optionEntity.getCompetencyScores().add(scoreEntity);
        });
    }

    private void replaceCompetencyScores(
            SimulationOptionEntity optionEntity,
            Map<String, Integer> competencyLevels
    ) {
        Map<String, CompetencyScoreUpdate> pendingScores = new LinkedHashMap<>();
        competencyLevels.forEach((competencyName, score) -> pendingScores.put(
                normalizeCompetencyName(competencyName),
                new CompetencyScoreUpdate(competencyName.trim(), score)
        ));

        optionEntity.getCompetencyScores().removeIf(scoreEntity -> {
            CompetencyScoreUpdate update = pendingScores.remove(normalizeCompetencyName(scoreEntity.getCompetencyName()));
            if (update == null) {
                return true;
            }

            scoreEntity.setCompetencyName(update.name());
            scoreEntity.setScore(update.score());
            return false;
        });

        pendingScores.values().forEach(update -> {
            OptionCompetencyScoreEntity scoreEntity = new OptionCompetencyScoreEntity();
            scoreEntity.setSimulationOption(optionEntity);
            scoreEntity.setCompetencyName(update.name());
            scoreEntity.setScore(update.score());
            optionEntity.getCompetencyScores().add(scoreEntity);
        });
    }

    private void assertCompetencyScores(
            SimulationVersionEntity versionEntity,
            Map<String, Integer> competencyLevels
    ) {
        if (competencyLevels == null || competencyLevels.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ao menos um score de competencia e obrigatorio.");
        }

        Set<String> existingCompetencies = versionEntity.getCompetencies()
                .stream()
                .map(competency -> competency.getName().toLowerCase(Locale.ROOT))
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        for (Map.Entry<String, Integer> entry : competencyLevels.entrySet()) {
            String competencyName = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
            Integer score = entry.getValue();
            if (!existingCompetencies.contains(competencyName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competencia informada nao existe na versao.");
            }
            if (score == null || score < 0 || score > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score de competencia deve estar entre 0 e 100.");
            }
        }
    }

    private String normalizeCompetencyName(String competencyName) {
        return competencyName == null ? "" : competencyName.trim().toLowerCase(Locale.ROOT);
    }

    private void assertNodeExistsWhenProvided(SimulationVersionEntity versionEntity, String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return;
        }
        findNode(versionEntity, nodeId.trim());
    }

    private SimulationNodeEntity findNode(SimulationVersionEntity versionEntity, String nodeId) {
        return versionEntity.getNodes()
                .stream()
                .filter(node -> Objects.equals(node.getNodeId(), nodeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No da simulacao nao encontrado."));
    }

    private SimulationOptionEntity findOption(SimulationNodeEntity nodeEntity, String optionId) {
        return nodeEntity.getOptions()
                .stream()
                .filter(option -> Objects.equals(option.getOptionId(), optionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alternativa nao encontrada."));
    }

    private void assertCanAddOption(SimulationNodeEntity nodeEntity) {
        if (nodeEntity.getOptions().size() >= MAX_OPTIONS_PER_NODE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cada turno pode ter no maximo 4 alternativas.");
        }
    }

    private int nextTurnIndex(SimulationVersionEntity versionEntity) {
        return versionEntity.getNodes()
                .stream()
                .mapToInt(SimulationNodeEntity::getTurnIndex)
                .max()
                .orElse(0) + 1;
    }

    private String nextNodeId(SimulationVersionEntity versionEntity) {
        int nextIndex = nextTurnIndex(versionEntity);
        String candidate = "turno-" + nextIndex;
        Set<String> nodeIds = versionEntity.getNodes()
                .stream()
                .map(SimulationNodeEntity::getNodeId)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        while (nodeIds.contains(candidate)) {
            nextIndex++;
            candidate = "turno-" + nextIndex;
        }
        return candidate;
    }

    private String nextOptionId(SimulationNodeEntity nodeEntity) {
        int nextIndex = nodeEntity.getOptions().size() + 1;
        String candidate = "opcao-" + nextIndex;
        Set<String> optionIds = nodeEntity.getOptions()
                .stream()
                .map(SimulationOptionEntity::getOptionId)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        while (optionIds.contains(candidate)) {
            nextIndex++;
            candidate = "opcao-" + nextIndex;
        }
        return candidate;
    }

    private void applyMedia(
            String mediaUrl,
            MediaType mediaType,
            Consumer<String> urlSetter,
            Consumer<MediaType> typeSetter
    ) {
        String normalizedUrl = trimToNull(mediaUrl);
        urlSetter.accept(normalizedUrl);
        typeSetter.accept(normalizedUrl == null ? null : mediaType);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SimulationVersionEntity findVersion(String simulationId, int versionNumber) {
        String tenantId = currentTenantService.requiredTenantId();
        return simulationVersionRepository
                .findBySimulationTenantIdAndSimulationIdAndVersionNumber(tenantId, simulationId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versao de simulacao nao encontrada."));
    }

    private void archiveOtherPublishedVersions(SimulationVersionEntity targetVersion) {
        String tenantId = targetVersion.getSimulation().getTenantId();
        String simulationId = targetVersion.getSimulation().getId();

        simulationVersionRepository
                .findBySimulationTenantIdAndSimulationIdAndStatusAndSimulationArchivedFalseAndSimulationDeletedAtIsNullOrderByPublishedAtDesc(
                        tenantId,
                        simulationId,
                        SimulationVersionStatus.PUBLISHED
                )
                .stream()
                .filter(version -> !Objects.equals(version.getId(), targetVersion.getId()))
                .forEach(version -> version.setStatus(SimulationVersionStatus.ARCHIVED));
    }

    private List<SimulationSummaryResponse> toLatestVersionSummary(SimulationEntity simulationEntity) {
        return simulationEntity.getVersions()
                .stream()
                .max(Comparator.comparingInt(SimulationVersionEntity::getVersionNumber))
                .map(version -> List.of(toSummaryResponse(simulationEntity, version)))
                .orElseGet(List::of);
    }

    private SimulationSummaryResponse toSummaryResponse(
            SimulationEntity simulationEntity,
            SimulationVersionEntity versionEntity
    ) {
        String tenantId = currentTenantService.requiredTenantId();
        long attemptsCreated = candidateAttemptRepository.countByTenantIdAndSimulationVersionId(tenantId, versionEntity.getId());
        long attemptsCompleted = candidateAttemptRepository.countByTenantIdAndSimulationVersionIdAndStatus(
                tenantId,
                versionEntity.getId(),
                AttemptStatus.COMPLETED
        );
        double completionRatePercent = attemptsCreated == 0
                ? 0.0
                : Math.round((attemptsCompleted * 10000.0) / attemptsCreated) / 100.0;

        return new SimulationSummaryResponse(
                simulationEntity.getId(),
                simulationEntity.getName(),
                simulationEntity.getDescription(),
                versionEntity.getVersionNumber(),
                versionEntity.getStatus(),
                versionEntity.getPublishedAt() != null ? versionEntity.getPublishedAt() : versionEntity.getCreatedAt(),
                versionEntity.getCompetencies()
                        .stream()
                        .map(SimulationCompetencyEntity::getName)
                        .sorted()
                        .toList(),
                attemptsCreated,
                attemptsCompleted,
                completionRatePercent
        );
    }

    private int nextVersionNumber(String simulationId) {
        return simulationVersionRepository.findFirstBySimulationIdOrderByVersionNumberDesc(simulationId)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
    }

    private void requireUniqueCompetencyNames(List<String> competencies) {
        Set<String> normalizedNames = new LinkedHashSet<>();
        for (String competency : competencies) {
            String normalizedName = competency.trim().toLowerCase(Locale.ROOT);
            if (normalizedName.isBlank()) {
                continue;
            }
            if (!normalizedNames.add(normalizedName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competencias duplicadas nao sao permitidas.");
            }
        }

        if (normalizedNames.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ao menos uma competencia e obrigatoria.");
        }
    }

    private String generateSimulationId(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String base = normalized.isBlank() ? "simulacao" : normalized;
        if (base.length() > 80) {
            base = base.substring(0, 80).replaceAll("-$", "");
        }

        String id;
        do {
            id = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        } while (simulationRepository.existsById(id));

        return id;
    }

    private SimulationVersionEntity cloneVersion(
            SimulationVersionEntity sourceVersionEntity,
            int newVersionNumber
    ) {
        SimulationVersionEntity clonedVersionEntity = new SimulationVersionEntity();
        clonedVersionEntity.setSimulation(sourceVersionEntity.getSimulation());
        clonedVersionEntity.setVersionNumber(newVersionNumber);
        clonedVersionEntity.setStatus(SimulationVersionStatus.DRAFT);
        clonedVersionEntity.setRootNodeId(sourceVersionEntity.getRootNodeId());
        clonedVersionEntity.setCreatedAt(Instant.now());

        for (SimulationCompetencyEntity sourceCompetencyEntity : sourceVersionEntity.getCompetencies()) {
            SimulationCompetencyEntity clonedCompetencyEntity = new SimulationCompetencyEntity();
            clonedCompetencyEntity.setSimulationVersion(clonedVersionEntity);
            clonedCompetencyEntity.setName(sourceCompetencyEntity.getName());
            clonedCompetencyEntity.setWeight(sourceCompetencyEntity.getWeight());
            clonedVersionEntity.getCompetencies().add(clonedCompetencyEntity);
        }

        for (SimulationNodeEntity sourceNodeEntity : sourceVersionEntity.getNodes()) {
            SimulationNodeEntity clonedNodeEntity = cloneNode(sourceNodeEntity, clonedVersionEntity);
            clonedVersionEntity.getNodes().add(clonedNodeEntity);
        }

        return clonedVersionEntity;
    }

    private SimulationNodeEntity cloneNode(
            SimulationNodeEntity sourceNodeEntity,
            SimulationVersionEntity clonedVersionEntity
    ) {
        SimulationNodeEntity clonedNodeEntity = new SimulationNodeEntity();
        clonedNodeEntity.setSimulationVersion(clonedVersionEntity);
        clonedNodeEntity.setNodeId(sourceNodeEntity.getNodeId());
        clonedNodeEntity.setTurnIndex(sourceNodeEntity.getTurnIndex());
        clonedNodeEntity.setSpeaker(sourceNodeEntity.getSpeaker());
        clonedNodeEntity.setMessage(sourceNodeEntity.getMessage());
        clonedNodeEntity.setTimeLimitSeconds(sourceNodeEntity.getTimeLimitSeconds());
        clonedNodeEntity.setMediaUrl(sourceNodeEntity.getMediaUrl());
        clonedNodeEntity.setMediaType(sourceNodeEntity.getMediaType());

        for (SimulationOptionEntity sourceOptionEntity : sourceNodeEntity.getOptions()) {
            SimulationOptionEntity clonedOptionEntity = cloneOption(sourceOptionEntity, clonedNodeEntity);
            clonedNodeEntity.getOptions().add(clonedOptionEntity);
        }

        return clonedNodeEntity;
    }

    private SimulationOptionEntity cloneOption(
            SimulationOptionEntity sourceOptionEntity,
            SimulationNodeEntity clonedNodeEntity
    ) {
        SimulationOptionEntity clonedOptionEntity = new SimulationOptionEntity();
        clonedOptionEntity.setSimulationNode(clonedNodeEntity);
        clonedOptionEntity.setOptionId(sourceOptionEntity.getOptionId());
        clonedOptionEntity.setText(sourceOptionEntity.getText());
        clonedOptionEntity.setNextNodeId(sourceOptionEntity.getNextNodeId());
        clonedOptionEntity.setCritical(sourceOptionEntity.isCritical());
        clonedOptionEntity.setAuditNote(sourceOptionEntity.getAuditNote());
        clonedOptionEntity.setMediaUrl(sourceOptionEntity.getMediaUrl());
        clonedOptionEntity.setMediaType(sourceOptionEntity.getMediaType());

        for (OptionCompetencyScoreEntity sourceScoreEntity : sourceOptionEntity.getCompetencyScores()) {
            OptionCompetencyScoreEntity clonedScoreEntity = new OptionCompetencyScoreEntity();
            clonedScoreEntity.setSimulationOption(clonedOptionEntity);
            clonedScoreEntity.setCompetencyName(sourceScoreEntity.getCompetencyName());
            clonedScoreEntity.setScore(sourceScoreEntity.getScore());
            clonedOptionEntity.getCompetencyScores().add(clonedScoreEntity);
        }

        return clonedOptionEntity;
    }

    private void requireStatus(
            SimulationVersionEntity simulationVersionEntity,
            String message,
            SimulationVersionStatus... allowedStatuses
    ) {
        for (SimulationVersionStatus allowedStatus : allowedStatuses) {
            if (simulationVersionEntity.getStatus() == allowedStatus) {
                return;
            }
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private SimulationVersionStatusResponse toStatusResponse(SimulationVersionEntity simulationVersionEntity) {
        return new SimulationVersionStatusResponse(
                simulationVersionEntity.getSimulation().getId(),
                simulationVersionEntity.getVersionNumber(),
                simulationVersionEntity.getStatus(),
                simulationVersionEntity.getPublishedAt()
        );
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private record CompetencyScoreUpdate(String name, int score) {
    }
}
