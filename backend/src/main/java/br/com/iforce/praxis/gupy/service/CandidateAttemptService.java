package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.candidate.dto.CandidateAttemptMonitoringResponse;
import br.com.iforce.praxis.candidate.dto.CandidateLinkResponse;
import br.com.iforce.praxis.candidate.service.BlindMasking;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;
import br.com.iforce.praxis.candidate.dto.EtapaAtualResponse;
import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse;
import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse.ProgressoResponse;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaRequest;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaResponse;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.auth.service.HealthVerticalService;
import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.dto.CreateCandidateResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.shared.integration.IntegrationTenantContext;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ReliabilityLevel;
import br.com.iforce.praxis.gupy.model.ResultDecision;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.outbox.service.OutboxService;
import br.com.iforce.praxis.shared.security.TenantSecurity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Orquestra o ciclo de vida da tentativa do candidato: idempotência na criação, persistência,
 * disparo de entrega à Gupy e montagem das respostas REST. As transições de estado ficam em
 * {@link AttemptStateMachine}, o mapeamento entidade↔domínio em {@link CandidateAttemptMapper} e o
 * cálculo de score em {@link ResultScoringService}.
 */
@Service
public class CandidateAttemptService {

    private static final String RESULT_READY_EVENT = "RESULT_READY";
    private static final String ATTEMPT_STARTED_EVENT = "ATTEMPT_STARTED";
    private static final String ATTEMPT_ABANDONED_EVENT = "ATTEMPT_ABANDONED";
    private static final String CANDIDATE_ATTEMPT_AGGREGATE = "CandidateAttempt";
    private static final long CLIENT_CLOCK_FUTURE_SKEW_SECONDS = 5;
    private static final long TRUSTED_CLIENT_ANSWER_ARRIVAL_CAP_SECONDS = 10 * 60;

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;
    private final OutboxService outboxService;
    private final JwtService jwtService;
    private final PraxisProperties praxisProperties;
    private final SimulationCatalogService simulationCatalogService;
    private final CandidateAttemptMapper candidateAttemptMapper;
    private final AttemptStateMachine attemptStateMachine;
    private final GupyTestResultMapper gupyTestResultMapper;
    private final HealthVerticalService healthVerticalService;

    public CandidateAttemptService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata,
            OutboxService outboxService,
            JwtService jwtService,
            PraxisProperties praxisProperties,
            SimulationCatalogService simulationCatalogService,
            CandidateAttemptMapper candidateAttemptMapper,
            AttemptStateMachine attemptStateMachine,
            GupyTestResultMapper gupyTestResultMapper,
            HealthVerticalService healthVerticalService
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
        this.outboxService = outboxService;
        this.jwtService = jwtService;
        this.praxisProperties = praxisProperties;
        this.simulationCatalogService = simulationCatalogService;
        this.candidateAttemptMapper = candidateAttemptMapper;
        this.attemptStateMachine = attemptStateMachine;
        this.gupyTestResultMapper = gupyTestResultMapper;
        this.healthVerticalService = healthVerticalService;
    }

    @Transactional
    public CreateCandidateResponse createOrReuse(
            CreateCandidateRequest request,
            IntegrationTenantContext tenantContext
    ) {
        assertCompanyMatchesToken(request.companyId(), tenantContext);
        String tenantId = tenantContext.tenantId();

        PublishedSimulation publishedSimulation = simulationCatalogService.findPublishedById(tenantId, request.testId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teste publicado não encontrado."));

        String idempotencyKey = tenantId + "|" + tenantContext.companyId() + "|" + request.documentId() + "|" + request.testId();
        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository
                .findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                .orElseGet(() -> createAndAuditAttemptSafely(tenantId, idempotencyKey, request, publishedSimulation));

        return new CreateCandidateResponse(
                candidateApiUrl(candidateAttemptEntity),
                candidateAttemptEntity.getResultId()
        );
    }

    @Transactional
    public CreateCandidateLinkResponse createCompanyLink(CreateCandidateLinkRequest request) {
        String tenantId = TenantSecurity.requiredTenant();

        PublishedSimulation publishedSimulation = simulationCatalogService
                .findPublishedById(tenantId, request.simulationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulação publicada não encontrada."));

        String idempotencyKey = tenantId + "|company|" + request.candidateEmail().trim() + "|" + request.simulationId();

        CandidateAttemptEntity entity = candidateAttemptRepository
                .findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                .orElseGet(() -> {
                    CandidateAttempt attempt = new CandidateAttempt(
                            "att_" + UUID.randomUUID().toString().replace("-", ""),
                            "res_" + UUID.randomUUID().toString().replace("-", ""),
                            tenantId,
                            tenantId,
                            publishedSimulation.id(),
                            publishedSimulation.versionId(),
                            publishedSimulation.versionNumber(),
                            idempotencyKey,
                            request.candidateName().trim(),
                            request.candidateEmail().trim(),
                            AttemptStatus.NOT_STARTED,
                            null,
                            publishedSimulation.competencies().stream()
                                    .map(c -> new ResultItem(c, 0, ResultTier.MAJOR))
                                    .toList(),
                            Map.of(),
                            ResultDecision.IN_PROGRESS,
                            false,
                            ReliabilityLevel.NORMAL,
                            normalizeAccommodationMultiplier(request.accommodationTimeMultiplier()),
                            "Resultado ainda não finalizado.",
                            Instant.now(),
                            null,
                            null
                    );

                    CandidateAttemptEntity newEntity = new CandidateAttemptEntity();
                    candidateAttemptMapper.applyDomainToEntity(attempt, newEntity);
                    CandidateAttemptEntity saved = candidateAttemptRepository.save(newEntity);

                    auditEventService.appendCandidateAttemptEvent(
                            tenantId,
                            saved.getId(),
                            AuditEventType.ATTEMPT_CREATED,
                            "Tentativa criada pela empresa para envio direto ao candidato.",
                            auditMetadata.of(
                                    "simulationId", request.simulationId(),
                                    "candidateEmail", request.candidateEmail().trim()
                            )
                    );

                    return saved;
                });

        return new CreateCandidateLinkResponse(
                entity.getId(),
                candidatePageUrl(entity),
                publishedSimulation.name()
        );
    }

    @Transactional(readOnly = true)
    public List<CandidateLinkResponse> listCompanyLinks(boolean blind) {
        String tenantId = TenantSecurity.requiredTenant();
        return candidateAttemptRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 200))
                .stream()
                .map(entity -> toCandidateLinkResponse(entity, blind))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CandidateAttemptMonitoringResponse> listLiveAttempts() {
        String tenantId = TenantSecurity.requiredTenant();
        Instant now = Instant.now();
        return candidateAttemptRepository.findByTenantIdAndStatusInOrderByCreatedAtDesc(
                        tenantId,
                        List.of(AttemptStatus.IN_PROGRESS, AttemptStatus.PAUSED),
                        PageRequest.of(0, 100)
                )
                .stream()
                .map(entity -> toMonitoringResponse(entity, now))
                .toList();
    }

    private CandidateLinkResponse toCandidateLinkResponse(CandidateAttemptEntity entity, boolean blind) {
        PublishedSimulation simulation = findSimulation(entity);
        // Modo cego: o backend não envia nome/e-mail (defesa em profundidade — não basta mascarar no cliente).
        String candidateName = blind ? BlindMasking.maskedName(entity.getId()) : entity.getCandidateName();
        String candidateEmail = blind ? null : entity.getCandidateEmail();
        return new CandidateLinkResponse(
                entity.getId(),
                candidatePageUrl(entity),
                candidateName,
                candidateEmail,
                entity.getSimulationId(),
                simulation.name(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    private CandidateAttemptMonitoringResponse toMonitoringResponse(CandidateAttemptEntity entity, Instant now) {
        PublishedSimulation simulation = findSimulation(entity);
        CandidateAttempt attempt = candidateAttemptMapper.toDomain(entity);
        ScenarioNode currentNode = findCurrentNode(attempt, simulation).orElse(null);
        ProgressoResponse progress = progressFor(attempt, simulation, currentNode);
        Instant lastSignalAt = lastSignalAt(entity);
        Instant startedAt = entity.getStartedAt() == null ? entity.getCreatedAt() : entity.getStartedAt();

        return new CandidateAttemptMonitoringResponse(
                entity.getId(),
                entity.getCandidateName(),
                entity.getCandidateEmail(),
                entity.getSimulationId(),
                simulation.name(),
                simulation.versionNumber(),
                entity.getStatus(),
                progress.passoAtual(),
                progress.passosEstimados(),
                progress.percentual(),
                Duration.between(startedAt, now).toSeconds(),
                lastSignalAt,
                entity.getStatus() == AttemptStatus.IN_PROGRESS
                        && !lastSignalAt.isBefore(now.minus(Duration.ofMinutes(5)))
        );
    }

    private Instant lastSignalAt(CandidateAttemptEntity entity) {
        Instant base = entity.getStartedAt() == null ? entity.getCreatedAt() : entity.getStartedAt();
        return entity.getAnswers().stream()
                .map(answer -> answer.getAnsweredAt() == null ? base : answer.getAnsweredAt())
                .max(Comparator.naturalOrder())
                .orElse(base);
    }

    private CandidateAttemptEntity createAndAuditAttemptSafely(
            String tenantId,
            String idempotencyKey,
            CreateCandidateRequest request,
            PublishedSimulation publishedSimulation
    ) {
        try {
            return createAndAuditAttempt(tenantId, idempotencyKey, request, publishedSimulation);
        } catch (DataIntegrityViolationException exception) {
            // Outra requisicao concorrente criou a tentativa com a mesma chave: reaproveita.
            return candidateAttemptRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                    .orElseThrow(() -> exception);
        }
    }

    private CandidateAttemptEntity createAndAuditAttempt(
            String tenantId,
            String idempotencyKey,
            CreateCandidateRequest request,
            PublishedSimulation publishedSimulation
    ) {
        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository.save(
                candidateAttemptMapper.newEntity(tenantId, idempotencyKey, request, publishedSimulation)
        );
        auditEventService.appendCandidateAttemptEvent(
                tenantId,
                candidateAttemptEntity.getId(),
                AuditEventType.ATTEMPT_CREATED,
                "Tentativa criada pela integração Gupy.",
                auditMetadata.of(
                        "resultId", candidateAttemptEntity.getResultId(),
                        "testId", request.testId(),
                        "simulationVersionId", publishedSimulation.versionId(),
                        "simulationVersionNumber", publishedSimulation.versionNumber()
                )
        );
        return candidateAttemptEntity;
    }

    @Transactional
    public ParticipacaoResponse findCandidateAttempt(String attemptToken) {
        CandidateAttemptEntity candidateAttemptEntity = findAttemptEntityByToken(attemptToken);
        CandidateAttempt originalAttempt = candidateAttemptMapper.toDomain(candidateAttemptEntity);
        AttemptStatus originalStatus = originalAttempt.status();
        CandidateAttempt attempt = attemptStateMachine.expireIfNeeded(originalAttempt);
        AttemptStatus statusAfterExpiration = attempt.status();
        if (!attemptStateMachine.isTerminalBlocked(attempt.status())) {
            attempt = attemptStateMachine.startIfNeeded(attempt);
        }
        CandidateAttempt savedAttempt = persist(attempt, candidateAttemptEntity);
        publishEngagementTransitionIfNeeded(originalStatus, statusAfterExpiration, savedAttempt);

        PublishedSimulation simulation = findSimulation(savedAttempt);
        ScenarioNode currentNode = attemptStateMachine.isTerminalBlocked(savedAttempt.status())
                ? null
                : findCurrentNode(savedAttempt, simulation).orElse(null);

        return new ParticipacaoResponse(
                savedAttempt.id(),
                simulation.name(),
                publicStatus(savedAttempt.status()),
                savedAttempt.status() == AttemptStatus.COMPLETED,
                suggestedFrontendAction(savedAttempt.status()),
                progressFor(savedAttempt, simulation, currentNode),
                candidateAttemptMapper.toEtapaAtualResponse(currentNode, savedAttempt.accommodationTimeMultiplier()),
                healthVerticalService.isHealthVertical(candidateAttemptEntity.getTenantId())
        );
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public RegistrarRespostaResponse submitAnswer(String attemptToken, RegistrarRespostaRequest request) {
        Instant receivedAt = Instant.now();
        String tenantId = TenantSecurity.requiredTenant();
        String attemptId = resolveAttemptId(attemptToken);
        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository
                .findByTenantIdAndIdForUpdate(tenantId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        CandidateAttempt originalAttempt = candidateAttemptMapper.toDomain(candidateAttemptEntity);
        AttemptStatus originalStatus = originalAttempt.status();
        CandidateAttempt attempt = attemptStateMachine.expireIfNeeded(originalAttempt);
        AttemptStatus statusAfterExpiration = attempt.status();
        if (attemptStateMachine.isTerminalBlocked(attempt.status())) {
            CandidateAttempt savedAttempt = persist(attempt, candidateAttemptEntity);
            publishEngagementTransitionIfNeeded(originalStatus, statusAfterExpiration, savedAttempt);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tentativa expirada ou abandonada.");
        }
        attempt = attemptStateMachine.startIfNeeded(attempt);
        PublishedSimulation simulation = findSimulation(attempt);

        Optional<RegistrarRespostaResponse> reconciledResponse = reconcileLateTimedOutAnswer(
                attempt, simulation, request, candidateAttemptEntity, originalStatus, statusAfterExpiration, receivedAt);
        if (reconciledResponse.isPresent()) {
            return reconciledResponse.get();
        }

        Optional<RegistrarRespostaResponse> duplicateResponse = handleDuplicate(attempt, simulation, request);
        if (duplicateResponse.isPresent()) {
            return duplicateResponse.get();
        }

        ScenarioNode currentNode = findCurrentNode(attempt, simulation)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Não há etapa pendente para esta tentativa."));
        String etapaId = request.etapaId();
        if (etapaId != null && !etapaId.isBlank() && !currentNode.id().equals(etapaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A etapa informada não é a etapa atual da participação.");
        }

        AttemptAnswer answer = buildAnswer(attempt, simulation, currentNode, request, receivedAt);
        Map<String, AttemptAnswer> answersByNodeId = new LinkedHashMap<>(attempt.answersByNodeId());
        answersByNodeId.put(currentNode.id(), answer);

        ScenarioOption selectedOption = answer.timedOut() ? null : findOption(currentNode, answer.optionId());
        boolean reachedEnd = reachedEnd(simulation, currentNode, selectedOption, answer.timedOut());
        CandidateAttempt updatedAttempt = attemptStateMachine.applyAnswer(
                attempt, simulation, answersByNodeId, selectedOption, reachedEnd);

        CandidateAttempt savedAttempt = persist(updatedAttempt, candidateAttemptEntity);
        publishEngagementTransitionIfNeeded(originalStatus, statusAfterExpiration, savedAttempt);
        auditAnswerSubmission(candidateAttemptEntity.getTenantId(), candidateAttemptEntity.getId(), answer, savedAttempt);
        if (savedAttempt.status() == AttemptStatus.COMPLETED) {
            publishResultReadyEvent(candidateAttemptEntity);
        }

        ScenarioNode nextNode = savedAttempt.status() == AttemptStatus.COMPLETED
                ? null
                : findCurrentNode(savedAttempt, simulation).orElse(null);

        return new RegistrarRespostaResponse(
                savedAttempt.id(),
                publicStatus(savedAttempt.status()),
                false,
                savedAttempt.status() == AttemptStatus.COMPLETED,
                progressFor(savedAttempt, simulation, nextNode),
                candidateAttemptMapper.toEtapaAtualResponse(nextNode, savedAttempt.accommodationTimeMultiplier())
        );
    }

    public record AttemptWithSimulation(CandidateAttempt attempt, PublishedSimulation simulation) {
    }

    @Transactional(readOnly = true)
    public AttemptWithSimulation findAttemptResult(
            String resultId,
            String companyId,
            IntegrationTenantContext tenantContext
    ) {
        if (companyId == null || companyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "company_id é obrigatório.");
        }

        assertCompanyMatchesToken(companyId, tenantContext);
        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository
                .findByTenantIdAndResultId(tenantContext.tenantId(), resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado de teste não encontrado."));
        if (!companyId.trim().equals(candidateAttemptEntity.getCompanyId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado de teste não encontrado.");
        }

        CandidateAttempt attempt = candidateAttemptMapper.toDomain(candidateAttemptEntity);
        PublishedSimulation simulation = findSimulation(attempt);
        return new AttemptWithSimulation(attempt, simulation);
    }

    @Transactional(readOnly = true)
    public TestResultResponse findResult(
            String resultId,
            String companyId,
            IntegrationTenantContext tenantContext
    ) {
        AttemptWithSimulation result = findAttemptResult(resultId, companyId, tenantContext);
        return gupyTestResultMapper.toResponse(result.attempt(), result.simulation());
    }

    private void assertCompanyMatchesToken(String companyId, IntegrationTenantContext tenantContext) {
        if (companyId == null || companyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "company_id é obrigatório.");
        }
        if (!companyId.trim().equals(tenantContext.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "company_id não pertence ao token informado.");
        }
    }

    private Optional<RegistrarRespostaResponse> handleDuplicate(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            RegistrarRespostaRequest request
    ) {
        ScenarioNode requestNode = resolveRequestNode(attempt, simulation, request).orElse(null);
        if (requestNode == null) {
            return Optional.empty();
        }

        AttemptAnswer existingAnswer = attempt.answersByNodeId().get(requestNode.id());
        if (existingAnswer == null) {
            return Optional.empty();
        }

        String internalOptionId = candidateAttemptMapper.resolveInternalOptionId(requestNode, request.respostaId());
        boolean sameAnswer = request.tempoEsgotado()
                ? existingAnswer.timedOut()
                : !existingAnswer.timedOut() && internalOptionId != null && internalOptionId.equals(existingAnswer.optionId());
        if (!sameAnswer) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta etapa já tem uma resposta final confirmada.");
        }

        ScenarioNode currentNode = attempt.status() == AttemptStatus.COMPLETED
                ? null
                : findCurrentNode(attempt, simulation).orElse(null);
        return Optional.of(new RegistrarRespostaResponse(
                attempt.id(),
                publicStatus(attempt.status()),
                true,
                attempt.status() == AttemptStatus.COMPLETED,
                progressFor(attempt, simulation, currentNode),
                candidateAttemptMapper.toEtapaAtualResponse(currentNode, attempt.accommodationTimeMultiplier())
        ));
    }

    private ProgressoResponse progressFor(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            ScenarioNode currentNode
    ) {
        int answeredSteps = attempt.answersByNodeId().size();
        int remainingSteps = currentNode == null ? 0 : maxRemainingDepth(simulation, currentNode.id(), new HashSet<>());
        int estimatedSteps = Math.max(1, answeredSteps + remainingSteps);
        int currentStep = currentNode == null ? estimatedSteps : Math.min(estimatedSteps, answeredSteps + 1);
        int percent = currentNode == null
                ? 100
                : Math.min(100, Math.max(1, Math.round((currentStep * 100f) / estimatedSteps)));

        return new ProgressoResponse(currentStep, estimatedSteps, percent);
    }

    private int maxRemainingDepth(PublishedSimulation simulation, String nodeId, Set<String> visitedNodeIds) {
        if (nodeId == null || !visitedNodeIds.add(nodeId)) {
            return 0;
        }

        ScenarioNode node = simulationCatalogService.findNode(simulation, nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno."));
        if (node.isFinal()) {
            return 0;
        }

        int maxChildDepth = node.options().stream()
                .map(ScenarioOption::nextNodeId)
                .filter(nextNodeId -> nextNodeId != null)
                .mapToInt(nextNodeId -> maxRemainingDepth(simulation, nextNodeId, new HashSet<>(visitedNodeIds)))
                .max()
                .orElse(0);
        int timeoutDepth = maxRemainingDepth(simulation, node.timeoutNextNodeId(), new HashSet<>(visitedNodeIds));

        return 1 + Math.max(maxChildDepth, timeoutDepth);
    }

    private Optional<ScenarioNode> resolveRequestNode(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            String requestedNodeId
    ) {
        if (requestedNodeId != null && !requestedNodeId.isBlank()) {
            return simulationCatalogService.findNode(simulation, requestedNodeId);
        }
        return findCurrentNode(attempt, simulation);
    }

    private Optional<ScenarioNode> resolveRequestNode(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            RegistrarRespostaRequest request
    ) {
        if (request.etapaId() != null && !request.etapaId().isBlank()) {
            return simulationCatalogService.findNode(simulation, request.etapaId());
        }
        if (request.etapaNumero() != null) {
            return simulation.nodes().stream()
                    .filter(node -> node.turnIndex() == request.etapaNumero())
                    .findFirst();
        }
        return findCurrentNode(attempt, simulation);
    }

    private Optional<RegistrarRespostaResponse> reconcileLateTimedOutAnswer(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            RegistrarRespostaRequest request,
            CandidateAttemptEntity candidateAttemptEntity,
            AttemptStatus originalStatus,
            AttemptStatus statusAfterExpiration,
            Instant receivedAt
    ) {
        if (request.tempoEsgotado()) {
            return Optional.empty();
        }

        ScenarioNode requestNode = resolveRequestNode(attempt, simulation, request).orElse(null);
        if (requestNode == null) {
            return Optional.empty();
        }

        AttemptAnswer existingAnswer = attempt.answersByNodeId().get(requestNode.id());
        if (existingAnswer == null || !existingAnswer.timedOut()) {
            return Optional.empty();
        }
        if (!isLastRecordedAnswer(attempt, existingAnswer)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta etapa já direcionou etapas seguintes e não pode mais ser conciliada."
            );
        }

        AttemptAnswer answer = buildAnswer(attempt, simulation, requestNode, request, receivedAt);
        Map<String, AttemptAnswer> answersByNodeId = new LinkedHashMap<>(attempt.answersByNodeId());
        answersByNodeId.put(requestNode.id(), answer);

        ScenarioOption selectedOption = findOption(requestNode, answer.optionId());
        boolean reachedEnd = reachedEnd(simulation, requestNode, selectedOption, false);
        CandidateAttempt updatedAttempt = attemptStateMachine.applyAnswer(
                attempt, simulation, answersByNodeId, selectedOption, reachedEnd);

        CandidateAttempt savedAttempt = persist(updatedAttempt, candidateAttemptEntity);
        publishEngagementTransitionIfNeeded(originalStatus, statusAfterExpiration, savedAttempt);
        auditAnswerSubmission(candidateAttemptEntity.getTenantId(), candidateAttemptEntity.getId(), answer, savedAttempt);
        if (savedAttempt.status() == AttemptStatus.COMPLETED) {
            publishResultReadyEvent(candidateAttemptEntity);
        }

        ScenarioNode nextNode = savedAttempt.status() == AttemptStatus.COMPLETED
                ? null
                : findCurrentNode(savedAttempt, simulation).orElse(null);

        return Optional.of(new RegistrarRespostaResponse(
                savedAttempt.id(),
                publicStatus(savedAttempt.status()),
                false,
                savedAttempt.status() == AttemptStatus.COMPLETED,
                progressFor(savedAttempt, simulation, nextNode),
                candidateAttemptMapper.toEtapaAtualResponse(nextNode, savedAttempt.accommodationTimeMultiplier())
        ));
    }

    private boolean isLastRecordedAnswer(CandidateAttempt attempt, AttemptAnswer answer) {
        return attempt.answersByNodeId().values().stream()
                .filter(candidateAnswer -> candidateAnswer.answeredAt() != null)
                .noneMatch(candidateAnswer -> candidateAnswer.answeredAt().isAfter(answer.answeredAt()));
    }

    private AttemptAnswer buildAnswer(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            ScenarioNode currentNode,
            RegistrarRespostaRequest request,
            Instant receivedAt
    ) {
        Instant answeredAt = trustedAnsweredAt(attempt, simulation, currentNode, request, receivedAt);
        if (request.tempoEsgotado()) {
            return AttemptAnswer.timedOut(currentNode.id(), answeredAt);
        }
        if (request.respostaId() == null || request.respostaId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha uma resposta antes de continuar.");
        }
        String internalOptionId = candidateAttemptMapper.resolveInternalOptionId(currentNode, request.respostaId());
        findOption(currentNode, internalOptionId);
        return AttemptAnswer.answered(currentNode.id(), internalOptionId, answeredAt);
    }

    private Instant trustedAnsweredAt(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            ScenarioNode currentNode,
            RegistrarRespostaRequest request,
            Instant receivedAt
    ) {
        Instant nodeStartedAt = resolveNodeStartedAt(attempt, simulation, currentNode);
        Instant clientAnsweredAt = request.respondidaEm();
        Instant answeredAt = clientAnsweredAt == null ? receivedAt : clientAnsweredAt;

        if (answeredAt.isBefore(nodeStartedAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O horário da resposta é anterior ao início da etapa.");
        }
        if (clientAnsweredAt != null && clientAnsweredAt.isAfter(receivedAt.plusSeconds(CLIENT_CLOCK_FUTURE_SKEW_SECONDS))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O horário da resposta está no futuro.");
        }

        Integer timeLimitSeconds = accommodatedTimeLimitSeconds(currentNode, attempt);
        if (timeLimitSeconds == null) {
            return answeredAt;
        }

        Instant frontendDeadline = nodeStartedAt.plusSeconds(timeLimitSeconds);
        Instant serverArrivalDeadline = frontendDeadline.plusSeconds(praxisProperties.answerGracePeriodSeconds());
        if (clientAnsweredAt != null) {
            if (clientAnsweredAt.isAfter(frontendDeadline)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tempo da etapa esgotado.");
            }
            Instant trustedClientArrivalDeadline = frontendDeadline.plusSeconds(
                    Math.max(
                            praxisProperties.answerGracePeriodSeconds(),
                            TRUSTED_CLIENT_ANSWER_ARRIVAL_CAP_SECONDS
                    )
            );
            if (receivedAt.isAfter(trustedClientArrivalDeadline)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Resposta chegou após a janela de tolerância da etapa.");
            }
            return answeredAt;
        }
        if (receivedAt.isAfter(serverArrivalDeadline)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resposta chegou após a janela de tolerância da etapa.");
        }
        if (clientAnsweredAt == null && answeredAt.isAfter(serverArrivalDeadline)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tempo da etapa esgotado.");
        }

        return answeredAt;
    }

    private Instant resolveNodeStartedAt(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            ScenarioNode targetNode
    ) {
        Instant currentStartedAt = attempt.startedAt() == null ? attempt.createdAt() : attempt.startedAt();
        String currentNodeId = simulation.rootNodeId();
        Set<String> visitedNodeIds = new HashSet<>();

        while (currentNodeId != null && visitedNodeIds.add(currentNodeId)) {
            ScenarioNode node = simulationCatalogService.findNode(simulation, currentNodeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno."));
            if (node.id().equals(targetNode.id())) {
                return currentStartedAt;
            }

            AttemptAnswer answer = attempt.answersByNodeId().get(node.id());
            if (answer == null) {
                return currentStartedAt;
            }
            currentStartedAt = answer.answeredAt() == null ? currentStartedAt : answer.answeredAt();
            if (answer.timedOut() || answer.optionId() == null) {
                currentNodeId = node.timeoutNextNodeId();
                continue;
            }

            ScenarioOption option = node.options().stream()
                    .filter(candidateOption -> candidateOption.id().equals(answer.optionId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno."));
            currentNodeId = option.nextNodeId();
        }

        return currentStartedAt;
    }

    private Integer accommodatedTimeLimitSeconds(ScenarioNode node, CandidateAttempt attempt) {
        if (node.timeLimitSeconds() == null) {
            return null;
        }
        return attempt.accommodationTimeMultiplier()
                .multiply(BigDecimal.valueOf(node.timeLimitSeconds()))
                .setScale(0, RoundingMode.CEILING)
                .intValue();
    }

    private ScenarioOption findOption(ScenarioNode node, String optionId) {
        return node.options().stream()
                .filter(option -> option.id().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resposta inválida para a etapa atual."));
    }

    private void auditAnswerSubmission(String tenantId, String attemptId, AttemptAnswer answer, CandidateAttempt updatedAttempt) {
        String message = answer.timedOut()
                ? "Etapa " + answer.nodeId() + " encerrada por tempo esgotado, sem resposta."
                : "Resposta salva para a etapa " + answer.nodeId() + ".";
        String metadata = answer.timedOut()
                ? auditMetadata.of("nodeId", answer.nodeId(), "timedOut", true)
                : auditMetadata.of("nodeId", answer.nodeId(), "optionId", answer.optionId());
        auditEventService.appendCandidateAttemptEvent(tenantId, attemptId, AuditEventType.ANSWER_SUBMITTED, message, metadata);

        if (updatedAttempt.status() == AttemptStatus.COMPLETED) {
            auditEventService.appendCandidateAttemptEvent(
                    tenantId,
                    attemptId,
                    AuditEventType.ATTEMPT_COMPLETED,
                    "Tentativa finalizada com pontuação calculada.",
                    auditMetadata.of(
                            "score", updatedAttempt.score(),
                            "humanReviewRequired", updatedAttempt.humanReviewRequired()
                    )
            );
        }
    }

    private CandidateAttempt persist(CandidateAttempt attempt, CandidateAttemptEntity candidateAttemptEntity) {
        candidateAttemptMapper.applyDomainToEntity(attempt, candidateAttemptEntity);
        CandidateAttemptEntity saved = candidateAttemptRepository.save(candidateAttemptEntity);
        return candidateAttemptMapper.toDomain(saved);
    }

    private void publishResultReadyEvent(CandidateAttemptEntity candidateAttemptEntity) {
        if (candidateAttemptEntity.getResultWebhookUrl() == null || candidateAttemptEntity.getResultWebhookUrl().isBlank()) {
            return;
        }

        PublishedSimulation simulation = findSimulation(candidateAttemptEntity);
        TestResultResponse testResult = gupyTestResultMapper.toResponse(candidateAttemptEntity, simulation);

        var payload = Map.of(
            "webhookUrl", candidateAttemptEntity.getResultWebhookUrl(),
            "testResult", testResult
        );

        outboxService.publish(
            candidateAttemptEntity.getTenantId(),
            RESULT_READY_EVENT,
            CANDIDATE_ATTEMPT_AGGREGATE,
            candidateAttemptEntity.getId(),
            payload
        );
    }

    private void publishEngagementTransitionIfNeeded(
            AttemptStatus originalStatus,
            AttemptStatus statusAfterExpiration,
            CandidateAttempt savedAttempt
    ) {
        if (statusAfterExpiration == AttemptStatus.NOT_STARTED && savedAttempt.status() != AttemptStatus.NOT_STARTED) {
            publishAttemptEngagementEvent(savedAttempt, ATTEMPT_STARTED_EVENT, savedAttempt.startedAt());
        }
        if ((originalStatus == AttemptStatus.IN_PROGRESS || originalStatus == AttemptStatus.PAUSED)
                && savedAttempt.status() == AttemptStatus.ABANDONED) {
            publishAttemptEngagementEvent(savedAttempt, ATTEMPT_ABANDONED_EVENT, savedAttempt.finishedAt());
        }
    }

    private void publishAttemptEngagementEvent(CandidateAttempt attempt, String eventType, Instant occurredAt) {
        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository
                .findByTenantIdAndId(attempt.tenantId(), attempt.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        if (candidateAttemptEntity.getResultWebhookUrl() == null || candidateAttemptEntity.getResultWebhookUrl().isBlank()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("webhookUrl", candidateAttemptEntity.getResultWebhookUrl());
        payload.put("eventPayload", engagementPayload(attempt, eventType, occurredAt));

        outboxService.publish(
                attempt.tenantId(),
                eventType,
                CANDIDATE_ATTEMPT_AGGREGATE,
                attempt.id(),
                payload
        );
    }

    private Map<String, Object> engagementPayload(CandidateAttempt attempt, String eventType, Instant occurredAt) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("name", attempt.candidateName());
        candidate.put("email", attempt.candidateEmail());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event_type", eventType);
        payload.put("attempt_id", attempt.id());
        payload.put("result_id", attempt.resultId());
        payload.put("company_id", attempt.companyId());
        payload.put("test_id", attempt.simulationId());
        payload.put("simulation_version_id", attempt.simulationVersionId());
        payload.put("simulation_version_number", attempt.simulationVersionNumber());
        payload.put("status", attempt.status().getDescricao());
        payload.put("occurred_at", occurredAt);
        payload.put("started_at", attempt.startedAt());
        payload.put("finished_at", attempt.finishedAt());
        payload.put("candidate", candidate);
        return payload;
    }

    private PublishedSimulation findSimulation(CandidateAttempt attempt) {
        if (attempt.simulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(attempt.simulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão da simulação não encontrada."));
        }

        return simulationCatalogService.findPublishedById(attempt.tenantId(), attempt.simulationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulação publicada não encontrada."));
    }

    private PublishedSimulation findSimulation(CandidateAttemptEntity candidateAttemptEntity) {
        if (candidateAttemptEntity.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(candidateAttemptEntity.getSimulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão da simulação não encontrada."));
        }

        return simulationCatalogService.findPublishedById(candidateAttemptEntity.getTenantId(), candidateAttemptEntity.getSimulationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulação publicada não encontrada."));
    }

    private CandidateAttemptEntity findAttemptEntityByToken(String attemptToken) {
        String tenantId = TenantSecurity.requiredTenant();
        String attemptId = resolveAttemptId(attemptToken);
        return candidateAttemptRepository.findByTenantIdAndId(tenantId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
    }

    private String resolveAttemptId(String attemptToken) {
        if (attemptToken == null || attemptToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token da tentativa é obrigatório.");
        }
        try {
            return jwtService.parseCandidateAttemptToken(attemptToken).attemptId();
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token da tentativa do candidato inválido."
            );
        }
    }

    private Optional<ScenarioNode> findCurrentNode(CandidateAttempt attempt, PublishedSimulation simulation) {
        String currentNodeId = simulation.rootNodeId();

        while (currentNodeId != null) {
            ScenarioNode node = simulationCatalogService.findNode(simulation, currentNodeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno."));
            if (node.isFinal()) {
                return Optional.empty();
            }

            AttemptAnswer answer = attempt.answersByNodeId().get(node.id());
            if (answer == null) {
                return Optional.of(node);
            }
            if (answer.timedOut() || answer.optionId() == null) {
                currentNodeId = node.timeoutNextNodeId();
                continue;
            }

            ScenarioOption option = node.options().stream()
                    .filter(candidateOption -> candidateOption.id().equals(answer.optionId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno."));

            currentNodeId = option.nextNodeId();
        }

        return Optional.empty();
    }

    private boolean reachedEnd(
            PublishedSimulation simulation,
            ScenarioNode currentNode,
            ScenarioOption selectedOption,
            boolean timedOut
    ) {
        String nextNodeId = timedOut
                ? currentNode.timeoutNextNodeId()
                : selectedOption == null ? null : selectedOption.nextNodeId();
        return nextNodeId == null || isFinalNode(simulation, nextNodeId);
    }

    private boolean isFinalNode(PublishedSimulation simulation, String nodeId) {
        return simulationCatalogService.findNode(simulation, nodeId)
                .map(ScenarioNode::isFinal)
                .orElse(false);
    }

    private String candidateApiUrl(CandidateAttemptEntity candidateAttemptEntity) {
        return praxisProperties.publicBaseUrl() + "/candidate/attempts/" + publicCandidateToken(candidateAttemptEntity);
    }

    private String candidatePageUrl(CandidateAttemptEntity candidateAttemptEntity) {
        return praxisProperties.candidatePageBaseUrl() + "/candidato/" + publicCandidateToken(candidateAttemptEntity);
    }

    private String publicCandidateToken(CandidateAttemptEntity candidateAttemptEntity) {
        return jwtService.generateCandidateAttemptToken(
                candidateAttemptEntity.getTenantId(),
                candidateAttemptEntity.getId(),
                praxisProperties.attemptLinkTtlHours()
        );
    }

    private String publicStatus(AttemptStatus status) {
        return switch (status) {
            case NOT_STARTED -> "nao_iniciada";
            case IN_PROGRESS -> "em_andamento";
            case PAUSED -> "pausada";
            case COMPLETED -> "concluida";
            case ABANDONED -> "abandonada";
            case EXPIRED -> "expirada";
            case FAILED -> "falhou";
        };
    }

    private String suggestedFrontendAction(AttemptStatus status) {
        return switch (status) {
            case NOT_STARTED -> "INICIAR";
            case IN_PROGRESS, PAUSED -> "CONTINUAR_TESTE";
            case COMPLETED, ABANDONED, EXPIRED, FAILED -> "VER_RESULTADOS";
        };
    }

    private BigDecimal normalizeAccommodationMultiplier(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ONE) < 0) {
            return BigDecimal.ONE;
        }
        if (value.compareTo(BigDecimal.valueOf(9.99)) > 0) {
            return BigDecimal.valueOf(9.99);
        }
        return value;
    }
}
