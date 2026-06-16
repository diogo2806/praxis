package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.candidate.dto.CandidateAttemptResponse;
import br.com.iforce.praxis.candidate.dto.SubmitAnswerRequest;
import br.com.iforce.praxis.candidate.dto.SubmitAnswerResponse;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.delivery.service.GupyCompletionCallbackService;
import br.com.iforce.praxis.gupy.delivery.service.ResultDeliveryService;
import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.dto.CreateCandidateResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final ResultDeliveryService resultDeliveryService;
    private final PraxisProperties praxisProperties;
    private final SimulationCatalogService simulationCatalogService;
    private final CandidateAttemptMapper candidateAttemptMapper;
    private final AttemptStateMachine attemptStateMachine;
    private final GupyTestResultMapper gupyTestResultMapper;
    private final GupyAuthService gupyAuthService;

    public CandidateAttemptService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            GupyCompletionCallbackService gupyCompletionCallbackService,
            ResultDeliveryService resultDeliveryService,
            PraxisProperties praxisProperties,
            SimulationCatalogService simulationCatalogService,
            CandidateAttemptMapper candidateAttemptMapper,
            AttemptStateMachine attemptStateMachine,
            GupyTestResultMapper gupyTestResultMapper,
            GupyAuthService gupyAuthService
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.gupyCompletionCallbackService = gupyCompletionCallbackService;
        this.resultDeliveryService = resultDeliveryService;
        this.praxisProperties = praxisProperties;
        this.simulationCatalogService = simulationCatalogService;
        this.candidateAttemptMapper = candidateAttemptMapper;
        this.attemptStateMachine = attemptStateMachine;
        this.gupyTestResultMapper = gupyTestResultMapper;
        this.gupyAuthService = gupyAuthService;
    }

    @Transactional
    public CreateCandidateResponse createOrReuse(CreateCandidateRequest request) {
        GupyAuthService.GupyTenantContext tenantContext = gupyAuthService.resolveByCompanyId(request.companyId());
        String tenantId = tenantContext.tenantId();

        PublishedSimulation publishedSimulation = simulationCatalogService.findPublishedById(tenantId, request.testId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teste publicado nao encontrado."));

        String idempotencyKey = tenantId + "|" + request.companyId() + "|" + request.documentId() + "|" + request.testId();
        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository
                .findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                .orElseGet(() -> createAndAuditAttemptSafely(tenantId, idempotencyKey, request, publishedSimulation));

        return new CreateCandidateResponse(
                candidateUrl(candidateAttemptEntity.getId()),
                candidateAttemptEntity.getResultId(),
                candidateAttemptEntity.getId()
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
    public CandidateAttemptResponse findCandidateAttempt(String attemptId) {
        CandidateAttemptEntity candidateAttemptEntity = findAttemptEntityById(attemptId);
        CandidateAttempt attempt = attemptStateMachine.expireIfNeeded(candidateAttemptMapper.toDomain(candidateAttemptEntity));
        if (!attemptStateMachine.isTerminalBlocked(attempt.status())) {
            attempt = attemptStateMachine.startIfNeeded(attempt);
        }
        CandidateAttempt savedAttempt = persist(attempt, candidateAttemptEntity);

        PublishedSimulation simulation = findSimulation(savedAttempt);
        ScenarioNode currentNode = attemptStateMachine.isTerminalBlocked(savedAttempt.status())
                ? null
                : findCurrentNode(savedAttempt, simulation).orElse(null);

        return new CandidateAttemptResponse(
                savedAttempt.id(),
                simulation.name(),
                savedAttempt.status(),
                savedAttempt.status() == AttemptStatus.COMPLETED,
                candidateAttemptMapper.toCandidateNodeResponse(currentNode)
        );
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public SubmitAnswerResponse submitAnswer(String attemptId, SubmitAnswerRequest request) {
        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa nao encontrada."));
        CandidateAttempt attempt = attemptStateMachine.expireIfNeeded(candidateAttemptMapper.toDomain(candidateAttemptEntity));
        if (attemptStateMachine.isTerminalBlocked(attempt.status())) {
            persist(attempt, candidateAttemptEntity);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tentativa expirada ou abandonada.");
        }
        attempt = attemptStateMachine.startIfNeeded(attempt);
        PublishedSimulation simulation = findSimulation(attempt);

        Optional<SubmitAnswerResponse> duplicateResponse = handleDuplicate(attempt, simulation, request);
        if (duplicateResponse.isPresent()) {
            return duplicateResponse.get();
        }

        ScenarioNode currentNode = findCurrentNode(attempt, simulation)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Tentativa sem turno pendente."));
        if (!currentNode.id().equals(request.nodeId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Turno informado nao e o turno atual da tentativa.");
        }

        AttemptAnswer answer = buildAnswer(currentNode, request);
        Map<String, AttemptAnswer> answersByNodeId = new LinkedHashMap<>(attempt.answersByNodeId());
        answersByNodeId.put(request.nodeId(), answer);

        ScenarioOption selectedOption = answer.timedOut() ? null : findOption(currentNode, answer.optionId());
        CandidateAttempt updatedAttempt = attemptStateMachine.applyAnswer(
                attempt, simulation, answersByNodeId, selectedOption, answer.timedOut());

        CandidateAttempt savedAttempt = persist(updatedAttempt, candidateAttemptEntity);
        auditAnswerSubmission(candidateAttemptEntity.getTenantId(), candidateAttemptEntity.getId(), answer, savedAttempt);
        if (savedAttempt.status() == AttemptStatus.COMPLETED) {
            gupyCompletionCallbackService.notifyCompletionIfNeeded(candidateAttemptEntity);
            resultDeliveryService.enqueueWebhookDelivery(candidateAttemptEntity);
        }

        ScenarioNode nextNode = savedAttempt.status() == AttemptStatus.COMPLETED
                ? null
                : findCurrentNode(savedAttempt, simulation).orElse(null);

        return new SubmitAnswerResponse(
                savedAttempt.id(),
                savedAttempt.status(),
                false,
                savedAttempt.status() == AttemptStatus.COMPLETED,
                candidateAttemptMapper.toCandidateNodeResponse(nextNode)
        );
    }

    @Transactional(readOnly = true)
    public TestResultResponse findResult(String resultId, String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "company_id e obrigatorio.");
        }

        GupyAuthService.GupyTenantContext tenantContext = gupyAuthService.resolveByCompanyId(companyId);
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

    private Optional<SubmitAnswerResponse> handleDuplicate(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            SubmitAnswerRequest request
    ) {
        AttemptAnswer existingAnswer = attempt.answersByNodeId().get(request.nodeId());
        if (existingAnswer == null) {
            return Optional.empty();
        }

        boolean sameAnswer = request.timedOut()
                ? existingAnswer.timedOut()
                : !existingAnswer.timedOut() && request.optionId() != null && request.optionId().equals(existingAnswer.optionId());
        if (!sameAnswer) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Turno ja respondido com outra alternativa.");
        }

        ScenarioNode currentNode = attempt.status() == AttemptStatus.COMPLETED
                ? null
                : findCurrentNode(attempt, simulation).orElse(null);
        return Optional.of(new SubmitAnswerResponse(
                attempt.id(),
                attempt.status(),
                true,
                attempt.status() == AttemptStatus.COMPLETED,
                candidateAttemptMapper.toCandidateNodeResponse(currentNode)
        ));
    }

    private AttemptAnswer buildAnswer(ScenarioNode currentNode, SubmitAnswerRequest request) {
        if (request.timedOut()) {
            return AttemptAnswer.timedOut(request.nodeId(), Instant.now());
        }
        if (request.optionId() == null || request.optionId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alternativa obrigatoria quando nao houver timeout.");
        }
        // Valida a alternativa contra o turno atual antes de persistir.
        findOption(currentNode, request.optionId());
        return AttemptAnswer.answered(request.nodeId(), request.optionId(), Instant.now());
    }

    private ScenarioOption findOption(ScenarioNode node, String optionId) {
        return node.options().stream()
                .filter(option -> option.id().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alternativa invalida para o turno atual."));
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

    private PublishedSimulation findSimulation(CandidateAttempt attempt) {
        if (attempt.simulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(attempt.simulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versao da simulacao nao encontrada."));
        }

        return simulationCatalogService.findPublishedById(attempt.tenantId(), attempt.simulationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulacao publicada nao encontrada."));
    }

    private CandidateAttemptEntity findAttemptEntityById(String attemptId) {
        return candidateAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa nao encontrada."));
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

    private String candidateUrl(String attemptId) {
        return praxisProperties.publicBaseUrl() + "/candidate/attempts/" + attemptId;
    }
}
