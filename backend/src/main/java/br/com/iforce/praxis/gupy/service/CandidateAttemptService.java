package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.candidate.dto.CandidateAttemptResponse;
import br.com.iforce.praxis.candidate.dto.CandidateNodeResponse;
import br.com.iforce.praxis.candidate.dto.CandidateOptionResponse;
import br.com.iforce.praxis.candidate.dto.SubmitAnswerRequest;
import br.com.iforce.praxis.candidate.dto.SubmitAnswerResponse;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.dto.CreateCandidateResponse;
import br.com.iforce.praxis.gupy.dto.TestResultItemResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.delivery.service.ResultDeliveryService;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ResultDecision;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CandidateAttemptService {

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final ResultDeliveryService resultDeliveryService;
    private final PraxisProperties praxisProperties;
    private final SimulationCatalogService simulationCatalogService;

    public CandidateAttemptService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            ResultDeliveryService resultDeliveryService,
            PraxisProperties praxisProperties,
            SimulationCatalogService simulationCatalogService
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.resultDeliveryService = resultDeliveryService;
        this.praxisProperties = praxisProperties;
        this.simulationCatalogService = simulationCatalogService;
    }

    @Transactional
    public CreateCandidateResponse createOrReuse(CreateCandidateRequest request) {
        simulationCatalogService.findPublishedById(request.testId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teste publicado nao encontrado."));

        String idempotencyKey = request.companyId() + "|" + request.documentId() + "|" + request.testId();
        Optional<CandidateAttemptEntity> existingCandidateAttemptEntity =
                candidateAttemptRepository.findByIdempotencyKey(idempotencyKey);
        CandidateAttemptEntity candidateAttemptEntity = existingCandidateAttemptEntity
                .orElseGet(() -> createAndAuditAttempt(idempotencyKey, request));

        return new CreateCandidateResponse(
                candidateUrl(candidateAttemptEntity.getId()),
                candidateAttemptEntity.getResultId(),
                candidateAttemptEntity.getId()
        );
    }

    private CandidateAttemptEntity createAndAuditAttempt(String idempotencyKey, CreateCandidateRequest request) {
        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository.save(createAttemptEntity(idempotencyKey, request));
        auditEventService.appendCandidateAttemptEvent(
                candidateAttemptEntity.getId(),
                AuditEventType.ATTEMPT_CREATED,
                "Tentativa criada pela integracao Gupy.",
                "{\"resultId\":\"" + candidateAttemptEntity.getResultId() + "\",\"testId\":\"" + request.testId() + "\"}"
        );
        return candidateAttemptEntity;
    }

    @Transactional(readOnly = true)
    public CandidateAttemptResponse findCandidateAttempt(String attemptId) {
        CandidateAttempt attempt = toDomain(findAttemptEntityById(attemptId));
        PublishedSimulation simulation = findSimulation(attempt.simulationId());
        ScenarioNode currentNode = findCurrentNode(attempt, simulation).orElse(null);

        return new CandidateAttemptResponse(
                attempt.id(),
                simulation.name(),
                attempt.status(),
                attempt.status() == AttemptStatus.COMPLETED,
                toCandidateNodeResponse(currentNode)
        );
    }

    @Transactional
    public SubmitAnswerResponse submitAnswer(String attemptId, SubmitAnswerRequest request) {
        CandidateAttemptEntity candidateAttemptEntity = findAttemptEntityById(attemptId);
        CandidateAttempt attempt = toDomain(candidateAttemptEntity);
        PublishedSimulation simulation = findSimulation(attempt.simulationId());

        Optional<AttemptAnswer> existingAnswer = Optional.ofNullable(attempt.answersByNodeId().get(request.nodeId()));
        if (existingAnswer.isPresent()) {
            if (!existingAnswer.get().optionId().equals(request.optionId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Turno ja respondido com outra alternativa.");
            }

            ScenarioNode currentNodeAfterDuplicate = findCurrentNode(attempt, simulation).orElse(null);
            return new SubmitAnswerResponse(
                    attempt.id(),
                    attempt.status(),
                    true,
                    attempt.status() == AttemptStatus.COMPLETED,
                    toCandidateNodeResponse(currentNodeAfterDuplicate)
            );
        }

        ScenarioNode currentNode = findCurrentNode(attempt, simulation)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Tentativa sem turno pendente."));

        if (!currentNode.id().equals(request.nodeId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Turno informado nao e o turno atual da tentativa.");
        }

        ScenarioOption selectedOption = currentNode.options().stream()
                .filter(option -> option.id().equals(request.optionId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alternativa invalida para o turno atual."));

        Map<String, AttemptAnswer> answersByNodeId = new LinkedHashMap<>(attempt.answersByNodeId());
        answersByNodeId.put(request.nodeId(), new AttemptAnswer(request.nodeId(), request.optionId(), Instant.now()));

        CandidateAttempt updatedAttempt = updateAttemptAfterAnswer(attempt, simulation, answersByNodeId, selectedOption);
        applyDomainToEntity(updatedAttempt, candidateAttemptEntity);
        CandidateAttemptEntity savedCandidateAttemptEntity = candidateAttemptRepository.save(candidateAttemptEntity);
        auditAnswerSubmission(savedCandidateAttemptEntity.getId(), request, updatedAttempt);
        if (updatedAttempt.status() == AttemptStatus.COMPLETED) {
            resultDeliveryService.enqueueIfNeeded(savedCandidateAttemptEntity);
        }
        CandidateAttempt savedAttempt = toDomain(savedCandidateAttemptEntity);
        ScenarioNode nextNode = findCurrentNode(savedAttempt, simulation).orElse(null);

        return new SubmitAnswerResponse(
                savedAttempt.id(),
                savedAttempt.status(),
                false,
                savedAttempt.status() == AttemptStatus.COMPLETED,
                toCandidateNodeResponse(nextNode)
        );
    }

    private void auditAnswerSubmission(String attemptId, SubmitAnswerRequest request, CandidateAttempt updatedAttempt) {
        auditEventService.appendCandidateAttemptEvent(
                attemptId,
                AuditEventType.ANSWER_SUBMITTED,
                "Resposta salva para o turno " + request.nodeId() + ".",
                "{\"nodeId\":\"" + request.nodeId() + "\",\"optionId\":\"" + request.optionId() + "\"}"
        );

        if (updatedAttempt.status() == AttemptStatus.COMPLETED) {
            auditEventService.appendCandidateAttemptEvent(
                    attemptId,
                    AuditEventType.ATTEMPT_COMPLETED,
                    "Tentativa finalizada com score deterministico.",
                    "{\"score\":" + updatedAttempt.score()
                            + ",\"humanReviewRequired\":" + updatedAttempt.humanReviewRequired() + "}"
            );
        }
    }

    @Transactional(readOnly = true)
    public TestResultResponse findResult(String resultId) {
        CandidateAttemptEntity candidateAttemptEntity = candidateAttemptRepository.findByResultId(resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado de teste nao encontrado."));
        CandidateAttempt attempt = toDomain(candidateAttemptEntity);

        List<TestResultItemResponse> results = attempt.results().stream()
                .map(item -> new TestResultItemResponse(item.name(), item.score(), item.tier()))
                .toList();

        return new TestResultResponse(
                attempt.resultId(),
                attempt.status(),
                attempt.score(),
                results,
                attempt.decision(),
                attempt.humanReviewRequired(),
                attempt.companyResultString()
        );
    }

    private CandidateAttemptEntity createAttemptEntity(String idempotencyKey, CreateCandidateRequest request) {
        String hash = Integer.toUnsignedString(idempotencyKey.hashCode());
        CandidateAttempt initialAttempt = new CandidateAttempt(
                "att_" + hash,
                "res_" + hash,
                request.testId(),
                idempotencyKey,
                request.candidateName(),
                request.candidateEmail(),
                AttemptStatus.NOT_STARTED,
                null,
                List.of(
                        new ResultItem("Empatia", 0, ResultTier.MAJOR),
                        new ResultItem("Resolução de conflito", 0, ResultTier.MAJOR),
                        new ResultItem("Aderência à política", 0, ResultTier.MINOR)
                ),
                Map.of(),
                ResultDecision.IN_PROGRESS,
                false,
                "Resultado ainda nao finalizado. A trilha auditavel sera preenchida apos a conclusao da simulacao.",
                Instant.now()
        );

        CandidateAttemptEntity candidateAttemptEntity = new CandidateAttemptEntity();
        applyDomainToEntity(initialAttempt, candidateAttemptEntity);
        candidateAttemptEntity.setCallbackUrl(request.callbackUrl() == null ? null : request.callbackUrl().toString());
        candidateAttemptEntity.setResultWebhookUrl(request.resultWebhookUrl() == null ? null : request.resultWebhookUrl().toString());
        return candidateAttemptEntity;
    }

    private void applyDomainToEntity(CandidateAttempt attempt, CandidateAttemptEntity candidateAttemptEntity) {
        candidateAttemptEntity.setId(attempt.id());
        candidateAttemptEntity.setResultId(attempt.resultId());
        candidateAttemptEntity.setSimulationId(attempt.simulationId());
        candidateAttemptEntity.setIdempotencyKey(attempt.idempotencyKey());
        candidateAttemptEntity.setCandidateName(attempt.candidateName());
        candidateAttemptEntity.setCandidateEmail(attempt.candidateEmail());
        candidateAttemptEntity.setStatus(attempt.status());
        candidateAttemptEntity.setScore(attempt.score());
        candidateAttemptEntity.setDecision(attempt.decision());
        candidateAttemptEntity.setHumanReviewRequired(attempt.humanReviewRequired());
        candidateAttemptEntity.setCompanyResultString(attempt.companyResultString());
        candidateAttemptEntity.setCreatedAt(attempt.createdAt());

        candidateAttemptEntity.getAnswers().clear();
        for (AttemptAnswer answer : attempt.answersByNodeId().values()) {
            AttemptAnswerEntity attemptAnswerEntity = new AttemptAnswerEntity();
            attemptAnswerEntity.setCandidateAttempt(candidateAttemptEntity);
            attemptAnswerEntity.setNodeId(answer.nodeId());
            attemptAnswerEntity.setOptionId(answer.optionId());
            attemptAnswerEntity.setAnsweredAt(answer.answeredAt());
            candidateAttemptEntity.getAnswers().add(attemptAnswerEntity);
        }

        candidateAttemptEntity.getResultItems().clear();
        for (ResultItem resultItem : attempt.results()) {
            ResultItemEntity resultItemEntity = new ResultItemEntity();
            resultItemEntity.setCandidateAttempt(candidateAttemptEntity);
            resultItemEntity.setName(resultItem.name());
            resultItemEntity.setScore(resultItem.score());
            resultItemEntity.setTier(resultItem.tier());
            candidateAttemptEntity.getResultItems().add(resultItemEntity);
        }
    }

    private CandidateAttempt toDomain(CandidateAttemptEntity candidateAttemptEntity) {
        Map<String, AttemptAnswer> answersByNodeId = new LinkedHashMap<>();
        candidateAttemptEntity.getAnswers().stream()
                .sorted(Comparator.comparing(AttemptAnswerEntity::getAnsweredAt))
                .forEach(answer -> answersByNodeId.put(
                        answer.getNodeId(),
                        new AttemptAnswer(answer.getNodeId(), answer.getOptionId(), answer.getAnsweredAt())
                ));

        List<ResultItem> resultItems = candidateAttemptEntity.getResultItems().stream()
                .sorted(Comparator.comparing(ResultItemEntity::getName))
                .map(resultItemEntity -> new ResultItem(
                        resultItemEntity.getName(),
                        resultItemEntity.getScore(),
                        resultItemEntity.getTier()
                ))
                .toList();

        return new CandidateAttempt(
                candidateAttemptEntity.getId(),
                candidateAttemptEntity.getResultId(),
                candidateAttemptEntity.getSimulationId(),
                candidateAttemptEntity.getIdempotencyKey(),
                candidateAttemptEntity.getCandidateName(),
                candidateAttemptEntity.getCandidateEmail(),
                candidateAttemptEntity.getStatus(),
                candidateAttemptEntity.getScore(),
                resultItems,
                answersByNodeId,
                candidateAttemptEntity.getDecision(),
                candidateAttemptEntity.isHumanReviewRequired(),
                candidateAttemptEntity.getCompanyResultString(),
                candidateAttemptEntity.getCreatedAt()
        );
    }

    private CandidateAttempt updateAttemptAfterAnswer(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            Map<String, AttemptAnswer> answersByNodeId,
            ScenarioOption selectedOption
    ) {
        boolean completed = selectedOption.nextNodeId() == null;
        AttemptStatus status = completed ? AttemptStatus.COMPLETED : AttemptStatus.IN_PROGRESS;
        Integer score = completed ? calculateScore(selectedOption) : null;
        List<ResultItem> results = completed ? toResultItems(selectedOption) : attempt.results();
        boolean humanReviewRequired = completed && selectedOption.critical();
        ResultDecision decision = resolveDecision(completed, humanReviewRequired);
        String companyResultString = buildCompanyResultString(simulation, selectedOption, score, humanReviewRequired);

        return new CandidateAttempt(
                attempt.id(),
                attempt.resultId(),
                attempt.simulationId(),
                attempt.idempotencyKey(),
                attempt.candidateName(),
                attempt.candidateEmail(),
                status,
                score,
                results,
                answersByNodeId,
                decision,
                humanReviewRequired,
                companyResultString,
                attempt.createdAt()
        );
    }

    private PublishedSimulation findSimulation(String simulationId) {
        return simulationCatalogService.findPublishedById(simulationId)
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

            ScenarioOption option = node.options().stream()
                    .filter(candidateOption -> candidateOption.id().equals(answer.optionId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Resposta salva aponta para alternativa invalida."));

            currentNodeId = option.nextNodeId();
        }

        return Optional.empty();
    }

    private CandidateNodeResponse toCandidateNodeResponse(ScenarioNode node) {
        if (node == null) {
            return null;
        }

        List<CandidateOptionResponse> options = node.options().stream()
                .map(option -> new CandidateOptionResponse(option.id(), option.text()))
                .toList();

        return new CandidateNodeResponse(
                node.id(),
                node.turnIndex(),
                node.speaker(),
                node.message(),
                node.timeLimitSeconds(),
                options
        );
    }

    private int calculateScore(ScenarioOption selectedOption) {
        int total = selectedOption.competencyScores().values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        return Math.round((float) total / selectedOption.competencyScores().size());
    }

    private List<ResultItem> toResultItems(ScenarioOption selectedOption) {
        List<ResultItem> resultItems = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : selectedOption.competencyScores().entrySet()) {
            ResultTier tier = "Aderência à política".equals(entry.getKey()) ? ResultTier.MINOR : ResultTier.MAJOR;
            resultItems.add(new ResultItem(entry.getKey(), entry.getValue(), tier));
        }
        return resultItems;
    }

    private ResultDecision resolveDecision(boolean completed, boolean humanReviewRequired) {
        if (!completed) {
            return ResultDecision.IN_PROGRESS;
        }
        if (humanReviewRequired) {
            return ResultDecision.REVIEW_REQUIRED;
        }
        return ResultDecision.RECOMMEND_INTERVIEW;
    }

    private String buildCompanyResultString(
            PublishedSimulation simulation,
            ScenarioOption selectedOption,
            Integer score,
            boolean humanReviewRequired
    ) {
        if (score == null) {
            return "Resultado ainda nao finalizado.";
        }

        String reviewLine = humanReviewRequired
                ? "Revisão humana obrigatória: alternativa crítica selecionada."
                : "Sem blocker crítico na trilha respondida.";

        return "# Práxis — Resultado\n\n"
                + "Simulação: " + simulation.name() + "\n\n"
                + "Score geral: " + score + "/100\n\n"
                + reviewLine + "\n\n"
                + "Trilha auditável: " + selectedOption.auditNote();
    }

    private String candidateUrl(String attemptId) {
        return praxisProperties.publicBaseUrl() + "/candidate/attempts/" + attemptId;
    }
}
