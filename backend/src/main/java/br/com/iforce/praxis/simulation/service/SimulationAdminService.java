package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.auth.service.HealthVerticalService;

import br.com.iforce.praxis.term.service.TermAcceptanceService;

import br.com.iforce.praxis.simulation.dto.CloneSimulationVersionResponse;

import br.com.iforce.praxis.simulation.dto.CompetencyWeightDto;

import br.com.iforce.praxis.simulation.dto.CreateNodeRequest;

import br.com.iforce.praxis.simulation.dto.CreateOptionRequest;

import br.com.iforce.praxis.simulation.dto.CreateSimulationDraftRequest;

import br.com.iforce.praxis.simulation.dto.CreateSimulationRequest;

import br.com.iforce.praxis.simulation.dto.GupyPreflightCheckResponse;

import br.com.iforce.praxis.simulation.dto.GupyPreflightResponse;

import br.com.iforce.praxis.simulation.dto.PublishSimulationResponse;

import br.com.iforce.praxis.simulation.dto.SimulationSummaryResponse;

import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;

import br.com.iforce.praxis.simulation.dto.SimulationVersionDetailResponse;

import br.com.iforce.praxis.simulation.dto.UpdateBlueprintRequest;

import br.com.iforce.praxis.simulation.dto.UpdateNodeRequest;

import br.com.iforce.praxis.simulation.dto.UpdateOptionRequest;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import br.com.iforce.praxis.shared.model.MediaType;

import br.com.iforce.praxis.shared.web.ApiException;

import br.com.iforce.praxis.simulation.model.GupyPreflightCheckStatus;

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


/**
 * Cérebro da autoria e administração das provas (simulações).
 *
 * <p>Na visão do processo, concentra as regras de negócio do "estúdio" de
 * provas: criar uma prova, editar o rascunho (plano, etapas e respostas),
 * validar, clonar e publicar versões, além de excluir provas. Cada prova
 * evolui em versões — as publicadas são imutáveis e a edição acontece sempre
 * em um rascunho. Toda alteração relevante fica registrada na trilha de
 * auditoria, garantindo rastreabilidade de quem mudou o quê.</p>
 */
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
    private final CurrentEmpresaService currentEmpresaService;
    private final HealthVerticalService healthVerticalService;
    private final TermAcceptanceService termAcceptanceService;

    public SimulationAdminService(
            SimulationVersionRepository simulationVersionRepository,
            SimulationRepository simulationRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            SimulationValidationService simulationValidationService,
            GupyPreflightService gupyPreflightService,
            SimulationMapperService simulationMapperService,
            AuditEventService auditEventService,
            CurrentEmpresaService currentEmpresaService,
            HealthVerticalService healthVerticalService,
            TermAcceptanceService termAcceptanceService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.simulationRepository = simulationRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationValidationService = simulationValidationService;
        this.gupyPreflightService = gupyPreflightService;
        this.simulationMapperService = simulationMapperService;
        this.auditEventService = auditEventService;
        this.currentEmpresaService = currentEmpresaService;
        this.healthVerticalService = healthVerticalService;
        this.termAcceptanceService = termAcceptanceService;
    }

    /**
     * Lista as provas ativas da empresa, trazendo a versão mais recente de cada.
     *
     * <p>Alimenta o painel administrativo de provas.</p>
     *
     * @return o resumo de cada prova ativa
     */
    @Transactional(readOnly = true)
    public List<SimulationSummaryResponse> listActiveSimulations() {
        return simulationRepository.findByEmpresaIdOrderByCreatedAtDesc(
                        currentEmpresaService.requiredEmpresaId())
                .stream()
                .map(this::toLatestVersionSummary)
                .flatMap(List::stream)
                .toList();
    }

    /**
     * Cria uma prova nova com a primeira versão (v1) em rascunho.
     *
     * <p>Fluxo do processo: valida os pesos das competências, cria a prova com
     * uma etapa inicial padrão e registra a criação na trilha de auditoria.</p>
     *
     * @param request dados iniciais da prova (nome, competências, pesos, etc.)
     * @return os detalhes da versão recém-criada
     */
    @Transactional
    public SimulationVersionDetailResponse createSimulation(CreateSimulationRequest request) {
        simulationValidationService.validateWeights(request.competencies());

        Instant createdAt = Instant.now();
        SimulationEntity simulationEntity = new SimulationEntity();
        simulationEntity.setId(generateSimulationId(request.name()));
        simulationEntity.setEmpresaId(currentEmpresaService.requiredEmpresaId());
        simulationEntity.setName(request.name().trim());
        simulationEntity.setDescription(createDescription(request));
        simulationEntity.setCriticalSituation(trimToNull(request.criticalSituation()));
        simulationEntity.setResultUse(trimToNull(request.objective()));
        simulationEntity.setCreatedAt(createdAt);

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
        rootNodeEntity.setMessage(defaultIfBlank(request.criticalSituation(), "Descreva a situação crítica da primeira etapa."));
        rootNodeEntity.setTimeLimitSeconds(null);
        versionEntity.getNodes().add(rootNodeEntity);

        simulationEntity.getVersions().add(versionEntity);
        SimulationEntity savedSimulationEntity = simulationRepository.save(simulationEntity);
        auditEventService.appendSimulationVersionEvent(
                savedSimulationEntity.getEmpresaId(),
                savedSimulationEntity.getId(),
                versionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_DRAFT_CREATED,
                "Teste criado com versão inicial em rascunho.",
                "{\"status\":\"draft\"}"
        );

        return toDetailResponse(versionEntity);
    }

    /**
     * Carrega os detalhes de uma versão da prova (para a tela de autoria).
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @return os detalhes completos da versão
     */
    @Transactional(readOnly = true)
    public SimulationVersionDetailResponse loadVersion(String simulationId, int versionNumber) {
        return toDetailResponse(findVersion(simulationId, versionNumber));
    }

    /**
     * Cria uma prova em rascunho a partir do plano da avaliação.
     *
     * <p>Fluxo do processo: garante que os nomes das competências não se
     * repetem, aplica o plano informado (etapa inicial e competências) e
     * registra a criação na trilha de auditoria.</p>
     *
     * @param request o plano da avaliação que dá origem ao rascunho
     * @return o resumo da prova criada
     */
    @Transactional
    public SimulationSummaryResponse createDraftSimulation(CreateSimulationDraftRequest request) {
        Instant createdAt = Instant.now();
        SimulationEntity simulationEntity = new SimulationEntity();
        simulationEntity.setId(generateSimulationId(request.name()));
        simulationEntity.setEmpresaId(currentEmpresaService.requiredEmpresaId());
        simulationEntity.setName(request.name().trim());
        simulationEntity.setDescription(truncateDescription(request.description().trim()));
        simulationEntity.setCriticalSituation(trimToNull(request.criticalSituation()));
        simulationEntity.setResultUse(trimToNull(request.resultUse()));
        simulationEntity.setCreatedAt(createdAt);

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
                savedSimulationEntity.getEmpresaId(),
                savedSimulationEntity.getId(),
                versionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_DRAFT_CREATED,
                "Versão inicial criada a partir do plano da avaliação.",
                "{\"status\":\"draft\"}"
        );

        return toSummaryResponse(savedSimulationEntity, versionEntity);
    }

    /**
     * Obtém os detalhes de uma versão da prova no formato da tela de autoria.
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @return os detalhes completos da versão
     */
    @Transactional(readOnly = true)
    public SimulationVersionDetailResponse getSimulationVersion(String simulationId, int versionNumber) {
        return simulationMapperService.toVersionDetail(findVersion(simulationId, versionNumber));
    }

    /**
     * Roda a validação de qualidade de uma versão da prova.
     *
     * <p>Devolve os impedimentos e avisos encontrados, ajudando a equipe a
     * corrigir a prova antes de publicar.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @return o relatório de validação
     */
    // readOnly: validar NÃO pode persistir nada. O validate() normaliza rootNodeId em memória
    // (setRootNodeId) como rede de segurança para dados legados; sob uma transação de escrita o
    // dirty-checking gravaria essa mudança — mutando silenciosamente uma versão PUBLICADA (que
    // deve ser imutável). Com readOnly, o Hibernate usa FlushMode.MANUAL e nada é persistido.
    @Transactional(readOnly = true)
    public SimulationValidationResponse validateVersion(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        return simulationValidationService.validate(simulationVersionEntity);
    }

    /**
     * Atualiza o plano da avaliação de uma versão em rascunho.
     *
     * <p>Fluxo do processo: valida os pesos, garante que a versão está em
     * rascunho (publicada não pode ser editada), aplica as mudanças de etapa
     * inicial, competências, pesos e textos descritivos, e registra a
     * alteração na trilha de auditoria.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param request os novos dados do plano
     * @return o resumo atualizado da prova
     */
    @Transactional
    public SimulationSummaryResponse updateBlueprint(String simulationId, int versionNumber, UpdateBlueprintRequest request) {
        simulationValidationService.validateBlueprintWeights(request.competencies());
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        requireUniqueCompetencyNames(request.competencies()
                .stream()
                .map(UpdateBlueprintRequest.CompetencyRequest::name)
                .toList());
        simulationMapperService.applyBlueprintUpdate(versionEntity, request);
        SimulationEntity simulationEntity = versionEntity.getSimulation();
        simulationEntity.setCriticalSituation(trimToNull(request.criticalSituation()));
        simulationEntity.setResultUse(trimToNull(request.resultUse()));
        simulationEntity.setDescription(createPlanningDescription(
                simulationEntity.getName(),
                request.criticalSituation(),
                request.competencies().stream().map(UpdateBlueprintRequest.CompetencyRequest::name).toList(),
                request.resultUse()
        ));
        SimulationVersionEntity savedVersionEntity = simulationVersionRepository.save(versionEntity);

        auditEventService.appendSimulationVersionEvent(
                savedVersionEntity.getSimulation().getEmpresaId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_VERSION_BLUEPRINT_UPDATED,
                "Plano do teste atualizado.",
                "{\"rootNodeId\":\"" + escapeJson(savedVersionEntity.getRootNodeId())
                        + "\",\"competencyCount\":" + savedVersionEntity.getCompetencies().size() + "}"
        );

        return toSummaryResponse(savedVersionEntity.getSimulation(), savedVersionEntity);
    }

    /**
     * Adiciona uma nova etapa (cena) ao rascunho da prova.
     *
     * <p>Fluxo do processo: garante que a versão está em rascunho, valida o
     * conteúdo (toda etapa que não é de encerramento precisa de fala; etapas
     * de encerramento não têm destino por tempo) e registra a inclusão na
     * trilha de auditoria.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param request o conteúdo da nova etapa
     * @return o identificador da etapa criada
     */
    @Transactional
    public String addNode(String simulationId, int versionNumber, CreateNodeRequest request) {
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        String nodeId = nextNodeId(versionEntity);

        SimulationNodeEntity nodeEntity = new SimulationNodeEntity();
        nodeEntity.setSimulationVersion(versionEntity);
        nodeEntity.setNodeId(nodeId);
        nodeEntity.setTurnIndex(nextTurnIndex(versionEntity));
        nodeEntity.setSpeaker("Cliente");
        boolean isFinal = request.isFinal();
        String message = trimToNull(request.clientMessage());
        if (!isFinal && message == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A fala da etapa e obrigatoria.");
        }
        if (isFinal && trimToNull(request.timeoutNextNodeId()) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etapas de encerramento não podem ter uma etapa de destino para quando o tempo se esgota.");
        }
        nodeEntity.setMessage(message == null ? "" : message);
        nodeEntity.setTimeLimitSeconds(request.timeLimitSeconds());
        assertNodeExistsWhenProvided(versionEntity, request.timeoutNextNodeId());
        nodeEntity.setTimeoutNextNodeId(isFinal ? null : trimToNull(request.timeoutNextNodeId()));
        nodeEntity.setFinal(isFinal);
        nodeEntity.setReportText(trimToNull(request.reportText()));
        nodeEntity.setPositionX(request.positionX());
        nodeEntity.setPositionY(request.positionY());
        nodeEntity.setPlainTextDescription(trimToNull(request.plainTextDescription()));
        nodeEntity.setAudioDescriptionUrl(trimToNull(request.audioDescriptionUrl()));
        applyMedia(request.mediaUrl(), request.mediaType(), nodeEntity::setMediaUrl, nodeEntity::setMediaType);
        versionEntity.getNodes().add(nodeEntity);

        simulationVersionRepository.save(versionEntity);
        auditEventService.appendSimulationVersionEvent(
                versionEntity.getSimulation().getEmpresaId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_NODE_ADDED,
                "Etapa adicionada.",
                "{\"nodeId\":\"" + escapeJson(nodeId) + "\"}"
        );

        return nodeId;
    }

    /**
     * Atualiza o conteúdo de uma etapa existente no rascunho da prova.
     *
     * <p>Garante que a versão está em rascunho, aplica as alterações
     * informadas (mantém obrigatória a fala de etapas que não são de
     * encerramento) e registra a mudança na trilha de auditoria.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param nodeId identificador da etapa
     * @param request os novos dados da etapa
     */
    @Transactional
    public void updateNode(String simulationId, int versionNumber, String nodeId, UpdateNodeRequest request) {
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        SimulationNodeEntity nodeEntity = findNode(versionEntity, nodeId);

        if (request.isFinal() != null) {
            nodeEntity.setFinal(request.isFinal());
            if (request.isFinal()) {
                nodeEntity.setTimeoutNextNodeId(null);
                nodeEntity.getOptions().clear();
            }
        }
        if (request.clientMessage() != null) {
            String message = trimToNull(request.clientMessage());
            if (!nodeEntity.isFinal() && message == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A fala da etapa e obrigatoria.");
            }
            nodeEntity.setMessage(message == null ? "" : message);
        }
        if (request.timeLimitSeconds() != null) {
            nodeEntity.setTimeLimitSeconds(request.timeLimitSeconds());
        }
        if (request.reportText() != null) {
            nodeEntity.setReportText(trimToNull(request.reportText()));
        }
        if (request.positionX() != null) {
            nodeEntity.setPositionX(request.positionX());
        }
        if (request.positionY() != null) {
            nodeEntity.setPositionY(request.positionY());
        }
        if (request.timeoutNextNodeId() != null) {
            if (nodeEntity.isFinal() && trimToNull(request.timeoutNextNodeId()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etapas de encerramento não podem ter uma etapa de destino para quando o tempo se esgota.");
            }
            assertNodeExistsWhenProvided(versionEntity, request.timeoutNextNodeId());
            nodeEntity.setTimeoutNextNodeId(nodeEntity.isFinal() ? null : trimToNull(request.timeoutNextNodeId()));
        }
        if (request.plainTextDescription() != null) {
            nodeEntity.setPlainTextDescription(trimToNull(request.plainTextDescription()));
        }
        if (request.audioDescriptionUrl() != null) {
            nodeEntity.setAudioDescriptionUrl(trimToNull(request.audioDescriptionUrl()));
        }
        if (request.mediaUrl() != null) {
            applyMedia(request.mediaUrl(), request.mediaType(), nodeEntity::setMediaUrl, nodeEntity::setMediaType);
        }
        if (!nodeEntity.isFinal() && trimToNull(nodeEntity.getMessage()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A fala da etapa e obrigatoria.");
        }

        simulationVersionRepository.save(versionEntity);
        auditEventService.appendSimulationVersionEvent(
                versionEntity.getSimulation().getEmpresaId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_NODE_UPDATED,
                "Etapa atualizada.",
                "{\"nodeId\":\"" + escapeJson(nodeId) + "\"}"
        );
    }

    /**
     * Remove uma etapa do rascunho da prova.
     *
     * <p>Não permite remover a etapa inicial. Registra a remoção na trilha de
     * auditoria.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param nodeId identificador da etapa a remover
     */
    @Transactional
    public void deleteNode(String simulationId, int versionNumber, String nodeId) {
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        if (Objects.equals(versionEntity.getRootNodeId(), nodeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A primeira etapa da avaliação não pode ser removida.");
        }

        SimulationNodeEntity nodeEntity = findNode(versionEntity, nodeId);
        versionEntity.getNodes().remove(nodeEntity);
        simulationVersionRepository.save(versionEntity);
        auditEventService.appendSimulationVersionEvent(
                versionEntity.getSimulation().getEmpresaId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_NODE_DELETED,
                "Etapa removida.",
                "{\"nodeId\":\"" + escapeJson(nodeId) + "\"}"
        );
    }

    /**
     * Adiciona uma resposta (alternativa) a uma etapa do rascunho.
     *
     * <p>Fluxo do processo: garante que a versão está em rascunho, que a etapa
     * não é de encerramento, que o limite de respostas por etapa não foi
     * atingido e que os pontos por competência são válidos. Registra a
     * inclusão na trilha de auditoria.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param nodeId identificador da etapa
     * @param request o conteúdo da nova resposta (texto, destino e pontos)
     * @return o identificador da resposta criada
     */
    @Transactional
    public String addOption(String simulationId, int versionNumber, String nodeId, CreateOptionRequest request) {
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        SimulationNodeEntity nodeEntity = findNode(versionEntity, nodeId);
        if (nodeEntity.isFinal()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Etapas de encerramento não aceitam respostas.");
        }
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
        optionEntity.setPlainTextDescription(trimToNull(request.plainTextDescription()));
        optionEntity.setAudioDescriptionUrl(trimToNull(request.audioDescriptionUrl()));
        applyMedia(request.mediaUrl(), request.mediaType(), optionEntity::setMediaUrl, optionEntity::setMediaType);
        applyCompetencyScores(optionEntity, request.competencyLevels());
        nodeEntity.getOptions().add(optionEntity);

        simulationVersionRepository.save(versionEntity);
        auditEventService.appendSimulationVersionEvent(
                versionEntity.getSimulation().getEmpresaId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_OPTION_ADDED,
                "Resposta adicionada ao teste.",
                "{\"nodeId\":\"" + escapeJson(nodeId) + "\",\"optionId\":\"" + escapeJson(optionId) + "\"}"
        );

        return optionId;
    }

    /**
     * Atualiza uma resposta (alternativa) de uma etapa do rascunho.
     *
     * <p>Aplica apenas os campos informados (os demais permanecem como
     * estavam), validando destino e pontos por competência quando alterados,
     * e registra a mudança na trilha de auditoria.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param nodeId identificador da etapa
     * @param optionId identificador da resposta
     * @param request os campos a atualizar na resposta
     */
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
        if (request.plainTextDescription() != null) {
            optionEntity.setPlainTextDescription(trimToNull(request.plainTextDescription()));
        }
        if (request.audioDescriptionUrl() != null) {
            optionEntity.setAudioDescriptionUrl(trimToNull(request.audioDescriptionUrl()));
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
                versionEntity.getSimulation().getEmpresaId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_OPTION_UPDATED,
                "Resposta atualizada no teste.",
                "{\"nodeId\":\"" + escapeJson(nodeId) + "\",\"optionId\":\"" + escapeJson(optionId) + "\"}"
        );
    }

    /**
     * Remove uma resposta (alternativa) de uma etapa do rascunho.
     *
     * <p>Registra a remoção na trilha de auditoria.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param nodeId identificador da etapa
     * @param optionId identificador da resposta a remover
     */
    @Transactional
    public void deleteOption(String simulationId, int versionNumber, String nodeId, String optionId) {
        SimulationVersionEntity versionEntity = findAndAssertDraft(simulationId, versionNumber);
        SimulationNodeEntity nodeEntity = findNode(versionEntity, nodeId);
        SimulationOptionEntity optionEntity = findOption(nodeEntity, optionId);

        nodeEntity.getOptions().remove(optionEntity);
        simulationVersionRepository.save(versionEntity);
        auditEventService.appendSimulationVersionEvent(
                versionEntity.getSimulation().getEmpresaId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_OPTION_DELETED,
                "Resposta removida do teste.",
                "{\"nodeId\":\"" + escapeJson(nodeId) + "\",\"optionId\":\"" + escapeJson(optionId) + "\"}"
        );
    }

    /**
     * Exclui definitivamente uma prova e todas as suas versões.
     *
     * <p>Ação irreversível. Recusa a exclusão se houver candidatos
     * (tentativas) vinculados à prova, preservando o histórico das
     * avaliações.</p>
     *
     * @param simulationId identificador da prova a remover
     */
    @Transactional
    public void deleteSimulation(String simulationId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        SimulationEntity simulationEntity = simulationRepository.findByEmpresaIdAndId(empresaId, simulationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos este teste."));

        long candidateAttempts = candidateAttemptRepository.countByEmpresaIdAndSimulationId(empresaId, simulationId);
        if (candidateAttempts > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível excluir o teste porque existem tentativas de candidatos vinculadas a ele."
            );
        }

        simulationRepository.delete(simulationEntity);
    }

    /**
     * Cria a próxima versão em rascunho a partir de uma versão publicada.
     *
     * <p>Permite evoluir a prova preservando intacta a versão publicada (que é
     * imutável): copia o conteúdo para uma nova versão em rascunho e registra
     * a clonagem na trilha de auditoria.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão publicada a clonar
     * @return os dados da nova versão em rascunho
     */
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
                savedClonedVersionEntity.getSimulation().getEmpresaId(),
                savedClonedVersionEntity.getSimulation().getId(),
                savedClonedVersionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_CLONED,
                "Versão publicada clonada para novo rascunho.",
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
    public CloneSimulationVersionResponse cloneVersionToTenant(Long sourceVersionId, String destinationEmpresaId) {
        SimulationVersionEntity sourceVersionEntity = simulationVersionRepository.findById(sourceVersionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versao de origem nao encontrada."));
        requireStatus(
                sourceVersionEntity,
                "Somente versoes publicadas podem ser clonadas para outro tenant.",
                SimulationVersionStatus.PUBLISHED
        );

        SimulationEntity sourceSimulationEntity = sourceVersionEntity.getSimulation();
        SimulationEntity destinationSimulationEntity = new SimulationEntity();
        destinationSimulationEntity.setId(generateSimulationId(sourceSimulationEntity.getName()));
        destinationSimulationEntity.setEmpresaId(destinationEmpresaId);
        destinationSimulationEntity.setName(sourceSimulationEntity.getName());
        destinationSimulationEntity.setDescription(sourceSimulationEntity.getDescription());
        destinationSimulationEntity.setCriticalSituation(sourceSimulationEntity.getCriticalSituation());
        destinationSimulationEntity.setResultUse(sourceSimulationEntity.getResultUse());
        destinationSimulationEntity.setCreatedAt(Instant.now());

        SimulationVersionEntity clonedVersionEntity = cloneVersion(sourceVersionEntity, 1);
        clonedVersionEntity.setSimulation(destinationSimulationEntity);
        destinationSimulationEntity.getVersions().add(clonedVersionEntity);

        SimulationEntity savedSimulationEntity = simulationRepository.save(destinationSimulationEntity);
        auditEventService.appendSimulationVersionEvent(
                destinationEmpresaId,
                savedSimulationEntity.getId(),
                clonedVersionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_CLONED,
                "Versao de marketplace clonada para tenant comprador.",
                "{\"sourceVersionId\":" + sourceVersionEntity.getId()
                        + ",\"sourceSimulationId\":\"" + escapeJson(sourceSimulationEntity.getId()) + "\"}"
        );

        return new CloneSimulationVersionResponse(
                savedSimulationEntity.getId(),
                sourceVersionEntity.getVersionNumber(),
                clonedVersionEntity.getVersionNumber(),
                clonedVersionEntity.getStatus()
        );
    }


    /**
     * Publica uma versão da prova, tornando-a disponível para uso real.
     *
     * <p>Fluxo do processo, com várias travas de segurança: só publica se a
     * validação não acusar impedimentos; só aceita versões em rascunho (ou já
     * publicadas); exige que a checagem de prontidão da Gupy (preflight) passe;
     * e, na vertical de saúde, exige o aceite do termo de uso. Ao publicar,
     * arquiva automaticamente as outras versões publicadas (mantendo apenas
     * uma no ar) e registra a publicação na trilha de auditoria.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão a publicar
     * @return o resultado da publicação
     */
    @Transactional
    public PublishSimulationResponse publishVersion(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);

        // Uma versão publicada é imutável. Republicá-la é idempotente: retorna o estado atual
        // SEM recarimbar publishedAt nem re-arquivar as demais versões (o comportamento anterior
        // sobrescrevia publishedAt = now, mutando a data de publicação de uma versão imutável).
        if (simulationVersionEntity.getStatus() == SimulationVersionStatus.PUBLISHED) {
            return new PublishSimulationResponse(
                    simulationVersionEntity.getSimulation().getId(),
                    simulationVersionEntity.getVersionNumber(),
                    simulationVersionEntity.getStatus(),
                    simulationVersionEntity.getPublishedAt()
            );
        }

        SimulationValidationResponse validationResponse = simulationValidationService.validate(simulationVersionEntity);

        if (!validationResponse.publishable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Publicação bloqueada por itens críticos do validador.");
        }

        if (simulationVersionEntity.getStatus() != SimulationVersionStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente versões em rascunho podem ser publicadas.");
        }

        GupyPreflightResponse gupyPreflightResponse = gupyPreflightService.evaluate(simulationVersionEntity);
        if (!gupyPreflightResponse.ok()) {
            Map<String, String> blockers = new LinkedHashMap<>();
            for (GupyPreflightCheckResponse check : gupyPreflightResponse.checks()) {
                if (GupyPreflightCheckStatus.BLOCKER.equals(check.status())) {
                    blockers.put(check.code().getDescricao(), check.message());
                }
            }
            throw new ApiException(HttpStatus.CONFLICT, "Preflight Gupy bloqueou a publicação.", blockers);
        }

        if (healthVerticalService.isHealthVertical(simulationVersionEntity.getSimulation().getEmpresaId())
                && !termAcceptanceService.isHealthUseAcceptedByCurrentUser()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Aceite o termo de uso na vertical de saúde antes de publicar."
            );
        }

        archiveOtherPublishedVersions(simulationVersionEntity);

        Instant publishedAt = Instant.now();
        simulationVersionEntity.setStatus(SimulationVersionStatus.PUBLISHED);
        simulationVersionEntity.setPublishedAt(publishedAt);
        SimulationVersionEntity savedSimulationVersionEntity = simulationVersionRepository.save(simulationVersionEntity);
        auditEventService.appendSimulationVersionEvent(
                savedSimulationVersionEntity.getSimulation().getEmpresaId(),
                savedSimulationVersionEntity.getSimulation().getId(),
                savedSimulationVersionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_PUBLISHED,
                "Versão do teste publicada.",
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
            description += " - Erro crítico: " + request.criticalError().trim();
        }

        return description.length() > 1000 ? description.substring(0, 1000) : description;
    }

    private String createPlanningDescription(
            String role,
            String criticalSituation,
            List<String> competencies,
            String resultUse
    ) {
        StringBuilder description = new StringBuilder();
        appendLine(description, "Cargo", role);
        appendLine(description, "Situação crítica", criticalSituation);
        if (competencies != null && !competencies.isEmpty()) {
            appendLine(description, "Competências", String.join(", ", competencies));
        }
        appendLine(description, "Uso do resultado", resultUse);
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
                "Somente versoes em rascunho podem ser editadas.",
                SimulationVersionStatus.DRAFT
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
            competencyEntity.setTargetScore(competency.normalizedTargetScore());
            competencyEntity.setTier(competency.normalizedTier());
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a pontuação de pelo menos uma competência.");
        }

        Set<String> existingCompetencies = versionEntity.getCompetencies()
                .stream()
                .map(competency -> competency.getName().toLowerCase(Locale.ROOT))
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        for (Map.Entry<String, Integer> entry : competencyLevels.entrySet()) {
            String competencyName = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
            Integer score = entry.getValue();
            if (!existingCompetencies.contains(competencyName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competência informada não existe na versão.");
            }
            if (score == null || score < 0 || score > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A pontuação de competência deve ficar entre 0 e 100.");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa do teste não encontrada."));
    }

    private SimulationOptionEntity findOption(SimulationNodeEntity nodeEntity, String optionId) {
        return nodeEntity.getOptions()
                .stream()
                .filter(option -> Objects.equals(option.getOptionId(), optionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resposta não encontrada."));
    }

    private void assertCanAddOption(SimulationNodeEntity nodeEntity) {
        if (nodeEntity.getOptions().size() >= MAX_OPTIONS_PER_NODE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cada etapa pode ter no máximo 4 respostas.");
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
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return simulationVersionRepository
                .findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(empresaId, simulationId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos esta versão do teste."));
    }

    private void archiveOtherPublishedVersions(SimulationVersionEntity targetVersion) {
        String empresaId = targetVersion.getSimulation().getEmpresaId();
        String simulationId = targetVersion.getSimulation().getId();

        simulationVersionRepository
                .findBySimulationEmpresaIdAndSimulationIdAndStatusOrderByPublishedAtDesc(
                        empresaId,
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
        String empresaId = currentEmpresaService.requiredEmpresaId();
        long attemptsCreated = candidateAttemptRepository.countByEmpresaIdAndSimulationVersionId(empresaId, versionEntity.getId());
        long attemptsCompleted = candidateAttemptRepository.countByEmpresaIdAndSimulationVersionIdAndStatus(
                empresaId,
                versionEntity.getId(),
                AttemptStatus.COMPLETED
        );
        double completionRatePercent = attemptsCreated == 0
                ? 0.0
                : Math.round((attemptsCompleted * 10000.0) / attemptsCreated) / 100.0;

        // A versao exibida na lista e sempre a mais recente, que pode ser um rascunho criado
        // para edicao enquanto uma versao publicada anterior continua no ar servindo links de
        // candidatos. Sinalizamos essa versao publicada ativa para o painel nao parecer "fora do ar".
        Integer livePublishedVersionNumber = simulationEntity.getVersions()
                .stream()
                .filter(version -> version.getStatus() == SimulationVersionStatus.PUBLISHED)
                .map(SimulationVersionEntity::getVersionNumber)
                .max(Integer::compareTo)
                .orElse(null);

        return new SimulationSummaryResponse(
                simulationEntity.getId(),
                simulationEntity.getName(),
                simulationEntity.getDescription(),
                simulationEntity.getCriticalSituation(),
                simulationEntity.getResultUse(),
                versionEntity.getVersionNumber(),
                versionEntity.getStatus(),
                livePublishedVersionNumber,
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competências duplicadas não são permitidas.");
            }
        }

        if (normalizedNames.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ao menos uma competência é obrigatória.");
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
            clonedCompetencyEntity.setTargetScore(sourceCompetencyEntity.getTargetScore());
            clonedCompetencyEntity.setTier(sourceCompetencyEntity.getTier());
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
        clonedNodeEntity.setTimeoutNextNodeId(sourceNodeEntity.getTimeoutNextNodeId());
        clonedNodeEntity.setFinal(sourceNodeEntity.isFinal());
        clonedNodeEntity.setReportText(sourceNodeEntity.getReportText());
        clonedNodeEntity.setPositionX(sourceNodeEntity.getPositionX());
        clonedNodeEntity.setPositionY(sourceNodeEntity.getPositionY());
        clonedNodeEntity.setPlainTextDescription(sourceNodeEntity.getPlainTextDescription());
        clonedNodeEntity.setAudioDescriptionUrl(sourceNodeEntity.getAudioDescriptionUrl());
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
        clonedOptionEntity.setPlainTextDescription(sourceOptionEntity.getPlainTextDescription());
        clonedOptionEntity.setAudioDescriptionUrl(sourceOptionEntity.getAudioDescriptionUrl());
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

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private record CompetencyScoreUpdate(String name, int score) {
    }
}
