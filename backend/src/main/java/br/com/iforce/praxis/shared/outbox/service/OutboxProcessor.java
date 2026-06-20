package br.com.iforce.praxis.shared.outbox.service;

import br.com.iforce.praxis.gupy.delivery.service.ResultWebhookClient;
import br.com.iforce.praxis.gupy.delivery.service.GupyOutboundUrlValidator;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.GupyTestResultMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.notification.service.ResultDeliveryDlqAlertService;
import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

@Slf4j
@Component
public class OutboxProcessor {

    private static final int MAX_ATTEMPT_COUNT = 5;
    private static final String RESULT_READY_EVENT = "RESULT_READY";
    private static final String ATTEMPT_STARTED_EVENT = "ATTEMPT_STARTED";
    private static final String ATTEMPT_ABANDONED_EVENT = "ATTEMPT_ABANDONED";

    private final OutboxEventRepository outboxEventRepository;
    private final ResultWebhookClient resultWebhookClient;
    private final ObjectMapper objectMapper;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final GupyTestResultMapper gupyTestResultMapper;
    private final GupyOutboundUrlValidator outboundUrlValidator;
    private final ResultDeliveryDlqAlertService dlqAlertService;

    public OutboxProcessor(
        OutboxEventRepository outboxEventRepository,
        ResultWebhookClient resultWebhookClient,
        ObjectMapper objectMapper,
        CandidateAttemptRepository candidateAttemptRepository,
        SimulationCatalogService simulationCatalogService,
        GupyTestResultMapper gupyTestResultMapper,
        GupyOutboundUrlValidator outboundUrlValidator,
        ResultDeliveryDlqAlertService dlqAlertService
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.resultWebhookClient = resultWebhookClient;
        this.objectMapper = objectMapper;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.gupyTestResultMapper = gupyTestResultMapper;
        this.outboundUrlValidator = outboundUrlValidator;
        this.dlqAlertService = dlqAlertService;
    }

    @Transactional
    public void processReadyEvents() {
        List<OutboxEventEntity> readyEvents = outboxEventRepository
            .findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                Arrays.asList(
                    OutboxEventEntity.OutboxEventStatus.PENDING,
                    OutboxEventEntity.OutboxEventStatus.RETRYING
                ),
                Instant.now()
            );

        if (readyEvents.isEmpty()) {
            return;
        }

        log.info("Processando {} eventos do outbox", readyEvents.size());

        for (OutboxEventEntity event : readyEvents) {
            processEvent(event);
        }
    }

    @Transactional
    public int processReadyEventsForTenant(String tenantId) {
        List<OutboxEventEntity> readyEvents = outboxEventRepository
            .findByTenantIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                tenantId,
                Arrays.asList(
                    OutboxEventEntity.OutboxEventStatus.PENDING,
                    OutboxEventEntity.OutboxEventStatus.RETRYING
                ),
                Instant.now()
            );

        for (OutboxEventEntity event : readyEvents) {
            processEvent(event);
        }
        return readyEvents.size();
    }

    @Transactional
    public void reprocessEvent(Long eventId, String tenantId) {
        OutboxEventEntity event = outboxEventRepository.findByIdAndTenantId(eventId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento de entrega não encontrado."));
        if (event.getStatus() == OutboxEventEntity.OutboxEventStatus.SENT) {
            return;
        }
        event.setStatus(OutboxEventEntity.OutboxEventStatus.PENDING);
        event.setNextAttemptAt(Instant.now());
        processEvent(event);
    }

    private void processEvent(OutboxEventEntity event) {
        try {
            event.setLastAttemptAt(Instant.now());
            event.setAttempts(event.getAttempts() + 1);
            if (RESULT_READY_EVENT.equals(event.getEventType())) {
                processResultReadyEvent(event);
            } else if (ATTEMPT_STARTED_EVENT.equals(event.getEventType())
                    || ATTEMPT_ABANDONED_EVENT.equals(event.getEventType())) {
                processAttemptEngagementEvent(event);
            }

            event.setStatus(OutboxEventEntity.OutboxEventStatus.SENT);
            event.setNextAttemptAt(null);
            event.setSentAt(Instant.now());
            event.setLastError(null);
        } catch (Exception ex) {
            handleEventFailure(event, ex);
        }
    }

    private void processResultReadyEvent(OutboxEventEntity event) {
        JsonNode payload = parsePayload(event.getPayload());

        String webhookUrl = payload.get("webhookUrl").asText();
        TestResultResponse testResult = null;
        JsonNode testResultNode = payload.get("testResult");
        if (testResultNode != null && !testResultNode.isNull()) {
            testResult = toTestResult(testResultNode);
        }

        if (testResult == null) {
            // Migrated event: need to fetch test result from database
            String attemptId = payload.get("attemptId").asText();
            testResult = fetchTestResult(attemptId, event.getTenantId());
        }

        outboundUrlValidator.validate(webhookUrl);
        log.debug("Enviando resultado para webhook: {}", webhookUrl);
        resultWebhookClient.postResult(webhookUrl, testResult);
    }

    private void processAttemptEngagementEvent(OutboxEventEntity event) {
        JsonNode payload = parsePayload(event.getPayload());

        String webhookUrl = payload.get("webhookUrl").asText();
        JsonNode eventPayload = payload.get("eventPayload");
        if (eventPayload == null || eventPayload.isNull()) {
            throw new IllegalArgumentException("Erro interno ao processar a entrega.");
        }

        outboundUrlValidator.validate(webhookUrl);
        log.debug("Enviando evento {} para webhook: {}", event.getEventType(), webhookUrl);
        resultWebhookClient.postPayload(webhookUrl, eventPayload);
    }

    private TestResultResponse fetchTestResult(String attemptId, String tenantId) {
        Optional<CandidateAttemptEntity> attempt = candidateAttemptRepository.findByTenantIdAndId(tenantId, attemptId);
        if (attempt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado.");
        }

        CandidateAttemptEntity candidateAttemptEntity = attempt.get();
        PublishedSimulation simulation = getSimulation(candidateAttemptEntity);
        return gupyTestResultMapper.toResponse(candidateAttemptEntity, simulation);
    }

    private TestResultResponse toTestResult(JsonNode testResultNode) {
        try {
            return objectMapper.readerFor(TestResultResponse.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(testResultNode);
        } catch (Exception exception) {
            throw new RuntimeException("Erro interno ao processar a entrega.", exception);
        }
    }

    private PublishedSimulation getSimulation(CandidateAttemptEntity candidateAttemptEntity) {
        if (candidateAttemptEntity.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(candidateAttemptEntity.getSimulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão da simulação não encontrada."));
        }

        return simulationCatalogService.findPublishedById(candidateAttemptEntity.getTenantId(), candidateAttemptEntity.getSimulationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulação publicada não encontrada."));
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new RuntimeException("Erro interno ao processar a entrega.", e);
        }
    }

    private void handleEventFailure(OutboxEventEntity event, Exception exception) {
        String errorMessage = limitMessage(exception.getMessage());
        event.setLastError(errorMessage);
        log.warn("Falha ao processar evento {} (tentativa {}): {}", event.getId(), event.getAttempts(), errorMessage);

        boolean isContractError = exception instanceof RestClientResponseException responseException
            && responseException.getStatusCode().is4xxClientError();

        if (isContractError || event.getAttempts() >= MAX_ATTEMPT_COUNT) {
            event.setStatus(OutboxEventEntity.OutboxEventStatus.DLQ);
            event.setNextAttemptAt(null);
            log.error("Evento {} movido para DLQ após {} tentativas", event.getId(), event.getAttempts());
            dlqAlertService.alertTenantAdmins(event);
            return;
        }

        event.setStatus(OutboxEventEntity.OutboxEventStatus.RETRYING);
        event.setNextAttemptAt(Instant.now().plusSeconds(calculateRetryDelay(event.getAttempts())));
    }

    private long calculateRetryDelay(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> 1L;
            case 2 -> 4L;
            case 3 -> 16L;
            case 4 -> 64L;
            default -> 256L;
        };
    }

    private String limitMessage(String message) {
        if (message == null) {
            return "Falha desconhecida no processamento do evento.";
        }
        if (message.length() <= 1200) {
            return message;
        }
        return message.substring(0, 1200);
    }
}
