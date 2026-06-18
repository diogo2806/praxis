package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.candidate.dto.CandidateLinkResponse;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;
import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaRequest;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaResponse;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.delivery.service.GupyCompletionCallbackService;
import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.dto.CreateCandidateResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestra o ciclo de vida da tentativa do candidato: idempotência na criação, persistência,
 * disparo de entrega à Gupy e montagem das respostas REST. As transições de estado ficam em
 * {@link AttemptStateMachine}, o mapeamento entidade↔domínio em {@link CandidateAttemptMapper} e o
 * cálculo de score em {@link ResultScoringService}.
 */
@Service
public class CandidateAttemptService {

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final GupyCompletionCallbackService gupyCompletionCallbackService;
    private final OutboxService outboxService;
    private final JwtService jwtService;
    private final PraxisProperties praxisProperties;
    private final SimulationCatalogService simulationCatalogService;
    private final CandidateAttemptMapper candidateAttemptMapper;
    private final AttemptStateMachine attemptStateMachine;
    private final GupyTestResultMapper gupyTestResultMapper;

    public CandidateAttemptService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            GupyCompletionCallbackService gupyCompletionCallbackService,
            OutboxService outboxService,
            JwtService jwtService,
            PraxisProperties praxisProperties,
            SimulationCatalogService simulationCatalogService,
            CandidateAttemptMapper candidateAttemptMapper,
            AttemptStateMachine attemptStateMachine,
            GupyTestResultMapper gupyTestResultMapper
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.gupyCompletionCallbackService = gupyCompletionCallbackService;
        this.outboxService = outboxService;
        this.jwtService = jwtService;
        this.praxisProperties = praxisProperties;
        this.simulationCatalogService = simulationCatalogService;
        this.candidateAttemptMapper = candidateAttemptMapper;
        this.attemptStateMachine = attemptStateMachine;
        this.gupyTestResultMapper = gupyTestResultMapper;
    }

    @Transactional
    public CreateCandidateResponse createOrReuse(
            CreateCandidateRequest request,
            GupyAuthService.GupyTenantContext tenantContext
    ) {
        assertCompanyMatchesToken(request.companyId(), tenantContext);
        String tenantId = tenantContext.tenantId();

        PublishedSimulation publishedSimulation = simulationCatalogService.findPublishedById(tenantId, request.testId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teste publicado nao encontrado."));

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulacao publicada nao encontrada."));

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
                            "Resultado ainda nao finalizado.",
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
                            "{\"simulationId\":\"" + request.simulationId()
                                    + "\",\"candidateEmail\":\"" + request.candidateEmail().trim() + "\"}"
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
    public List<CandidateLinkResponse> listCompanyLinks() {
        String tenantId = TenantSecurity.requiredTenant();
        return candidateAttemptRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 200))
                .stream()
                .map(this::toCandidateLinkResponse)
                .toList();
    }

    private CandidateLinkResponse toCandidateLinkResponse(CandidateAttemptEntity entity) {
        PublishedSimulation simulation = findSimulation(entity);
        return new CandidateLinkResponse(
                entity.getId(),
                candidatePageUrl(entity),
                entity.getCandidateName(),
                entity.getCandidateEmail(),
                entity.getSimulationId(),
                simulation.name(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
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
                "Tentativa criada pela integracao Gupy.",
                "{\"resultId\":\"" + candidateAttemptEntity.getResultId()
                        + "\",\"testId\":\"" + request.testId()
                        + "\",\"simulationVersionId\":" + publishedSimulation.versionId()
                        + ",\"simulationVersionNumber\":" + publishedSimulation.versionNumber() + "}"
        );
        return candidateAttemptEntity;
    }

    @Transactional
    public ParticipacaoResponse findCandidateAttempt(String attemptToken) {
        CandidateAttemptEntity candidateAttemptEntity = findAttemptEntityByToken(attemptToken);
        CandidateAttempt attempt = attemptStateMachine.expireIfNeeded(candidateAttemptMapper.toDomain(candidateAttemptEntity));
        if (!attemptStateMachine.isTerminalBlocked(attempt.status())) {
            attempt = attemptStateMachine.startIfNeeded(attempt);
        }
        CandidateAttempt savedAttempt = persist(attempt, candidateAttemptEntity);

        PublishedSimulation simulation = findSimulation(savedAttempt);
        ScenarioNode currentNode = attemptStateMachine.isTerminalBlocked(savedAttempt.status())
                ? null
                : findCurrentNode(savedAttempt, simulation).orElse(null);

        return new ParticipacaoResponse(
                savedAttempt.id(),
                simulation.name(),
                publicStatus(savedAttempt.status()),
                savedAttempt.status() == AttemptStatus.COMPLETED,
                candidateAttemptMapper.toEtapaAtualResponse(currentNode)
        );
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public RegistrarRespostaResponse submitAnswer(String attemptToken, RegistrarRespostaRequest request) {
        String tenantId = TenantSecurity.requiredTenant();
        String attemptId = resolveAttemptId(attemptToken);
        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository
                .findByTenantIdAndIdForUpdate(tenantId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa nao encontrada."));
        CandidateAttempt attempt = attemptStateMachine.expireIfNeeded(candidateAttemptMapper.toDomain(candidateAttemptEntity));
        if (attemptStateMachine.isTerminalBlocked(attempt.status())) {
            persist(attempt, candidateAttemptEntity);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tentativa expirada ou abandonada.");
        }
        attempt = attemptStateMachine.startIfNeeded(attempt);
        PublishedSimulation simulation = findSimulation(attempt);

        Optional<RegistrarRespostaResponse> duplicateResponse = handleDuplicate(attempt, simulation, request);
        if (duplicateResponse.isPresent()) {
            return duplicateResponse.get();
        }

        ScenarioNode currentNode = findCurrentNode(attempt, simulation)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Tentativa sem turno pendente."));
        String etapaId = request.etapaId();
        if (etapaId != null && !etapaId.isBlank() && !currentNode.id().equals(etapaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A etapa informada nao e a etapa atual da participacao.");
        }

        AttemptAnswer answer = buildAnswer(currentNode, request);
        Map<String, AttemptAnswer> answersByNodeId = new LinkedHashMap<>(attempt.answersByNodeId());
        answersByNodeId.put(currentNode.id(), answer);

        ScenarioOption selectedOption = answer.timedOut() ? null : findOption(currentNode, answer.optionId());
        CandidateAttempt updatedAttempt = attemptStateMachine.applyAnswer(
                attempt, simulation, answersByNodeId, selectedOption, answer.timedOut());

        CandidateAttempt savedAttempt = persist(updatedAttempt, candidateAttemptEntity);
        auditAnswerSubmission(candidateAttemptEntity.getTenantId(), candidateAttemptEntity.getId(), answer, savedAttempt);
        if (savedAttempt.status() == AttemptStatus.COMPLETED) {
            gupyCompletionCallbackService.notifyCompletionIfNeeded(candidateAttemptEntity);
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
                candidateAttemptMapper.toEtapaAtualResponse(nextNode)
        );
    }

    @Transactional(readOnly = true)
    public TestResultResponse findResult(
            String resultId,
            String companyId,
            GupyAuthService.GupyTenantContext tenantContext
    ) {
        if (companyId == null || companyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "company_id e obrigatorio.");
        }

        assertCompanyMatchesToken(companyId, tenantContext);
        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository
                .findByTenantIdAndResultId(tenantContext.tenantId(), resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado de teste nao encontrado."));
        if (!companyId.trim().equals(candidateAttemptEntity.getCompanyId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado de teste nao encontrado.");
        }

        CandidateAttempt attempt = candidateAttemptMapper.toDomain(candidateAttemptEntity);
        PublishedSimulation simulation = findSimulation(attempt);
        return gupyTestResultMapper.toResponse(attempt, simulation);
    }

    private void assertCompanyMatchesToken(String companyId, GupyAuthService.GupyTenantContext tenantContext) {
        if (companyId == null || companyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "company_id e obrigatorio.");
        }
        if (!companyId.trim().equals(tenantContext.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "company_id nao pertence ao token informado.");
        }
    }

    private Optional<RegistrarRespostaResponse> handleDuplicate(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            RegistrarRespostaRequest request
    ) {
        ScenarioNode requestNode = resolveRequestNode(attempt, simulation, request.etapaId()).orElse(null);
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta etapa ja foi respondida com outra alternativa.");
        }

        ScenarioNode currentNode = attempt.status() == AttemptStatus.COMPLETED
                ? null
                : findCurrentNode(attempt, simulation).orElse(null);
        return Optional.of(new RegistrarRespostaResponse(
                attempt.id(),
                publicStatus(attempt.status()),
                true,
                attempt.status() == AttemptStatus.COMPLETED,
                candidateAttemptMapper.toEtapaAtualResponse(currentNode)
        ));
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

    private AttemptAnswer buildAnswer(ScenarioNode currentNode, RegistrarRespostaRequest request) {
        if (request.tempoEsgotado()) {
            return AttemptAnswer.timedOut(currentNode.id(), Instant.now());
        }
        if (request.respostaId() == null || request.respostaId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha uma resposta antes de continuar.");
        }
        String internalOptionId = candidateAttemptMapper.resolveInternalOptionId(currentNode, request.respostaId());
        findOption(currentNode, internalOptionId);
        return AttemptAnswer.answered(currentNode.id(), internalOptionId, Instant.now());
    }

    private ScenarioOption findOption(ScenarioNode node, String optionId) {
        return node.options().stream()
                .filter(option -> option.id().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resposta invalida para a etapa atual."));
    }

    private void auditAnswerSubmission(String tenantId, String attemptId, AttemptAnswer answer, CandidateAttempt updatedAttempt) {
        String message = answer.timedOut()
                ? "Turno " + answer.nodeId() + " encerrado por timeout (sem resposta)."
                : "Resposta salva para o turno " + answer.nodeId() + ".";
        String metadata = answer.timedOut()
                ? "{\"nodeId\":\"" + answer.nodeId() + "\",\"timedOut\":true}"
                : "{\"nodeId\":\"" + answer.nodeId() + "\",\"optionId\":\"" + answer.optionId() + "\"}";
        auditEventService.appendCandidateAttemptEvent(tenantId, attemptId, AuditEventType.ANSWER_SUBMITTED, message, metadata);

        if (updatedAttempt.status() == AttemptStatus.COMPLETED) {
            auditEventService.appendCandidateAttemptEvent(
                    tenantId,
                    attemptId,
                    AuditEventType.ATTEMPT_COMPLETED,
                    "Tentativa finalizada com score deterministico.",
                    "{\"score\":" + updatedAttempt.score()
                            + ",\"humanReviewRequired\":" + updatedAttempt.humanReviewRequired() + "}"
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
            "RESULT_READY",
            "CandidateAttempt",
            candidateAttemptEntity.getId(),
            payload
        );
    }

    private PublishedSimulation findSimulation(CandidateAttempt attempt) {
        if (attempt.simulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(attempt.simulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versao da simulacao nao encontrada."));
        }

        return simulationCatalogService.findPublishedById(attempt.tenantId(), attempt.simulationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulacao publicada nao encontrada."));
    }

    private PublishedSimulation findSimulation(CandidateAttemptEntity candidateAttemptEntity) {
        if (candidateAttemptEntity.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(candidateAttemptEntity.getSimulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versao da simulacao nao encontrada."));
        }

        return simulationCatalogService.findPublishedById(candidateAttemptEntity.getTenantId(), candidateAttemptEntity.getSimulationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulacao publicada nao encontrada."));
    }

    private CandidateAttemptEntity findAttemptEntityByToken(String attemptToken) {
        String tenantId = TenantSecurity.requiredTenant();
        String attemptId = resolveAttemptId(attemptToken);
        return candidateAttemptRepository.findByTenantIdAndId(tenantId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa nao encontrada."));
    }

    private String resolveAttemptId(String attemptToken) {
        try {
            return jwtService.parseCandidateAttemptToken(attemptToken).attemptId();
        } catch (RuntimeException exception) {
            // Compatibilidade com links antigos que expunham o ID interno.
            return attemptToken;
        }
    }

    private Optional<ScenarioNode> findCurrentNode(CandidateAttempt attempt, PublishedSimulation simulation) {
        String currentNodeId = simulation.rootNodeId();

        while (currentNodeId != null) {
            ScenarioNode node = simulationCatalogService.findNode(simulation, currentNodeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Grafo da simulacao invalido."));

            AttemptAnswer answer = attempt.answersByNodeId().get(node.id());
            if (answer == null) {
                return Optional.of(node);
            }
            if (answer.timedOut() || answer.optionId() == null) {
                // Timeout encerra a trilha: não há próximo turno.
                return Optional.empty();
            }

            ScenarioOption option = node.options().stream()
                    .filter(candidateOption -> candidateOption.id().equals(answer.optionId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Resposta salva aponta para alternativa invalida."));

            currentNodeId = option.nextNodeId();
        }

        return Optional.empty();
    }

    private String candidateApiUrl(CandidateAttemptEntity candidateAttemptEntity) {
        return praxisProperties.publicBaseUrl() + "/candidate/attempts/" + publicCandidateToken(candidateAttemptEntity);
    }

    private String candidatePageUrl(CandidateAttemptEntity candidateAttemptEntity) {
        return praxisProperties.publicBaseUrl() + "/candidato/" + publicCandidateToken(candidateAttemptEntity);
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
}
