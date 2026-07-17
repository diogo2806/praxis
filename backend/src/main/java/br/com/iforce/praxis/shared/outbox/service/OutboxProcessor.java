package br.com.iforce.praxis.shared.outbox.service;

import br.com.iforce.praxis.gupy.delivery.service.GupyOutboundUrlValidator;
import br.com.iforce.praxis.gupy.delivery.service.ResultWebhookClient;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.GupyTestResultMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.integration.IntegrationManagementService;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.service.AttemptEngagementWebhookService;
import br.com.iforce.praxis.shared.integration.service.ConfirmableGenericWebhookDeliveryService;
import br.com.iforce.praxis.shared.notification.service.ResultDeliveryDlqAlertService;
import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class OutboxProcessor {

    private static final int MAX_ATTEMPT_COUNT = 5;
    private static final int BATCH_SIZE = 100;
    private static final Duration STUCK_PROCESSING_TIMEOUT = Duration.ofMinutes(5);
    private static final String RESULT_READY_EVENT = "RESULT_READY";
    private static final String ATTEMPT_STARTED_EVENT = "ATTEMPT_STARTED";
    private static final String ATTEMPT_ABANDONED_EVENT = "ATTEMPT_ABANDONED";
    private static final String DELIVERY_STATE = "deliveryState";
    private static final String GUPY_DESTINATION = "GUPY";
    private static final String CUSTOM_API_DESTINATION = "CUSTOM_API";

    private final OutboxEventRepository outboxEventRepository;
    private final ResultWebhookClient resultWebhookClient;
    private final ObjectMapper objectMapper;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final GupyTestResultMapper gupyTestResultMapper;
    private final GupyOutboundUrlValidator outboundUrlValidator;
    private final ResultDeliveryDlqAlertService dlqAlertService;
    private final ConfirmableGenericWebhookDeliveryService genericWebhookDeliveryService;
    private final AttemptEngagementWebhookService attemptEngagementWebhookService;
    private final IntegrationManagementService integrationManagementService;
    private final TransactionTemplate txTemplate;

    public OutboxProcessor(
            OutboxEventRepository outboxEventRepository,
            ResultWebhookClient resultWebhookClient,
            ObjectMapper objectMapper,
            CandidateAttemptRepository candidateAttemptRepository,
            SimulationCatalogService simulationCatalogService,
            GupyTestResultMapper gupyTestResultMapper,
            GupyOutboundUrlValidator outboundUrlValidator,
            ResultDeliveryDlqAlertService dlqAlertService,
            ConfirmableGenericWebhookDeliveryService genericWebhookDeliveryService,
            AttemptEngagementWebhookService attemptEngagementWebhookService,
            IntegrationManagementService integrationManagementService,
            PlatformTransactionManager transactionManager
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.resultWebhookClient = resultWebhookClient;
        this.objectMapper = objectMapper;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.gupyTestResultMapper = gupyTestResultMapper;
        this.outboundUrlValidator = outboundUrlValidator;
        this.dlqAlertService = dlqAlertService;
        this.genericWebhookDeliveryService = genericWebhookDeliveryService;
        this.attemptEngagementWebhookService = attemptEngagementWebhookService;
        this.integrationManagementService = integrationManagementService;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    public void processReadyEvents() {
        List<Long> claimedIds = claimBatch();
        if (claimedIds.isEmpty()) {
            return;
        }
        log.info("Processando {} eventos do outbox", claimedIds.size());
        claimedIds.forEach(this::deliverAndFinalize);
    }

    public int processReadyEventsForEmpresa(String empresaId) {
        List<Long> claimedIds = claimBatchForEmpresa(empresaId);
        claimedIds.forEach(this::deliverAndFinalize);
        return claimedIds.size();
    }

    public void reprocessEvent(Long eventId, String empresaId) {
        Long claimedId = txTemplate.execute(status -> {
            OutboxEventEntity event = outboxEventRepository.findByIdAndEmpresaIdForUpdate(eventId, empresaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento de entrega não encontrado."));
            if (event.getStatus() == OutboxEventEntity.OutboxEventStatus.SENT) {
                return null;
            }
            if (event.getStatus() == OutboxEventEntity.OutboxEventStatus.PROCESSING
                    && event.getLastAttemptAt() != null
                    && event.getLastAttemptAt().isAfter(stuckBefore())) {
                return null;
            }
            markClaimed(event);
            return event.getId();
        });
        if (claimedId != null) {
            deliverAndFinalize(claimedId);
        }
    }

    private List<Long> claimBatch() {
        return txTemplate.execute(status -> {
            List<OutboxEventEntity> batch = outboxEventRepository.claimReadyBatch(
                    readyStatuses(), Instant.now(), stuckBefore(), BATCH_SIZE);
            batch.forEach(this::markClaimed);
            return batch.stream().map(OutboxEventEntity::getId).toList();
        });
    }

    private List<Long> claimBatchForEmpresa(String empresaId) {
        return txTemplate.execute(status -> {
            List<OutboxEventEntity> batch = outboxEventRepository.claimReadyBatchForEmpresa(
                    empresaId, readyStatuses(), Instant.now(), stuckBefore(), BATCH_SIZE);
            batch.forEach(this::markClaimed);
            return batch.stream().map(OutboxEventEntity::getId).toList();
        });
    }

    private void markClaimed(OutboxEventEntity event) {
        event.setStatus(OutboxEventEntity.OutboxEventStatus.PROCESSING);
        event.setAttempts(event.getAttempts() + 1);
        event.setLastAttemptAt(Instant.now());
    }

    private List<String> readyStatuses() {
        return List.of(
                OutboxEventEntity.OutboxEventStatus.PENDING.name(),
                OutboxEventEntity.OutboxEventStatus.RETRYING.name());
    }

    private Instant stuckBefore() {
        return Instant.now().minus(STUCK_PROCESSING_TIMEOUT);
    }

    private void deliverAndFinalize(Long eventId) {
        OutboxEventEntity snapshot = txTemplate.execute(status ->
                outboxEventRepository.findById(eventId).orElse(null));
        if (snapshot == null) {
            return;
        }

        try {
            dispatch(snapshot);
            txTemplate.executeWithoutResult(status -> {
                OutboxEventEntity event = outboxEventRepository.findById(eventId).orElseThrow();
                event.setStatus(OutboxEventEntity.OutboxEventStatus.SENT);
                event.setNextAttemptAt(null);
                event.setSentAt(Instant.now());
                event.setLastError(null);
            });
        } catch (Exception exception) {
            txTemplate.executeWithoutResult(status -> {
                OutboxEventEntity event = outboxEventRepository.findById(eventId).orElseThrow();
                handleEventFailure(event, exception);
                if (exception instanceof DestinationDeliveryException destinationException) {
                    Throwable cause = destinationException.getCause() == null
                            ? destinationException
                            : destinationException.getCause();
                    applyDestinationState(
                            event,
                            destinationException.destination(),
                            event.getStatus().name(),
                            cause.getMessage(),
                            null,
                            httpStatus(cause));
                }
            });
        }
    }

    private void dispatch(OutboxEventEntity event) {
        if (RESULT_READY_EVENT.equals(event.getEventType())) {
            processResultReadyEvent(event);
            return;
        }
        if (ATTEMPT_STARTED_EVENT.equals(event.getEventType())
                || ATTEMPT_ABANDONED_EVENT.equals(event.getEventType())) {
            processAttemptEngagementEvent(event);
            return;
        }
        throw new UnsupportedOutboxEventTypeException(event.getId(), event.getEventType());
    }

    private void processResultReadyEvent(OutboxEventEntity event) {
        JsonNode payload = parsePayload(event.getPayload());
        TestResultResponse testResult = resolveTestResult(payload, event);

        String webhookUrl = textOrNull(payload.get("webhookUrl"));
        if (webhookUrl != null && !webhookUrl.isBlank() && !isDestinationSent(payload, GUPY_DESTINATION)) {
            try {
                outboundUrlValidator.validate(webhookUrl);
                log.debug("Enviando resultado para webhook Gupy: {}", webhookUrl);
                resultWebhookClient.postResult(webhookUrl, testResult);
                recordGupyActivityBestEffort(event.getEmpresaId());
                persistDestinationSent(event.getId(), GUPY_DESTINATION, null);
            } catch (Exception exception) {
                persistDestinationFailure(event.getId(), GUPY_DESTINATION, exception);
                throw new DestinationDeliveryException(GUPY_DESTINATION, exception);
            }
        }

        JsonNode refreshedPayload = reloadPayload(event.getId());
        if (!isDestinationSent(refreshedPayload, CUSTOM_API_DESTINATION)) {
            try {
                deliverGenericWebhook(event);
                persistDestinationSent(event.getId(), CUSTOM_API_DESTINATION, null);
            } catch (Exception exception) {
                persistDestinationFailure(event.getId(), CUSTOM_API_DESTINATION, exception);
                throw new DestinationDeliveryException(CUSTOM_API_DESTINATION, exception);
            }
        }
    }

    private TestResultResponse resolveTestResult(JsonNode payload, OutboxEventEntity event) {
        JsonNode testResultNode = payload.get("testResult");
        if (testResultNode != null && !testResultNode.isNull()) {
            return toTestResult(testResultNode);
        }
        JsonNode attemptId = payload.get("attemptId");
        String resolvedAttemptId = attemptId == null || attemptId.isNull()
                ? event.getAggregateId()
                : attemptId.asText();
        return fetchTestResult(resolvedAttemptId, event.getEmpresaId());
    }

    private void recordGupyActivityBestEffort(String empresaId) {
        try {
            integrationManagementService.recordActivity(empresaId, IntegrationProvider.GUPY);
        } catch (Exception exception) {
            log.warn("Falha ao registrar atividade da integração Gupy (ignorada): {}", exception.getMessage());
        }
    }

    private void deliverGenericWebhook(OutboxEventEntity event) {
        candidateAttemptRepository.findByEmpresaIdAndId(event.getEmpresaId(), event.getAggregateId())
                .ifPresent(attempt -> genericWebhookDeliveryService.deliverResultReady(event.getEmpresaId(), attempt));
    }

    private void processAttemptEngagementEvent(OutboxEventEntity event) {
        JsonNode payload = parsePayload(event.getPayload());
        try {
            attemptEngagementWebhookService.deliver(event.getEmpresaId(), event.getEventType(), payload);
            persistDestinationSent(event.getId(), CUSTOM_API_DESTINATION, null);
        } catch (Exception exception) {
            persistDestinationFailure(event.getId(), CUSTOM_API_DESTINATION, exception);
            throw new DestinationDeliveryException(CUSTOM_API_DESTINATION, exception);
        }
    }

    private void persistDestinationSent(Long eventId, String destination, Integer httpStatus) {
        txTemplate.executeWithoutResult(status -> {
            OutboxEventEntity event = outboxEventRepository.findById(eventId).orElseThrow();
            applyDestinationState(event, destination, "SENT", null, Instant.now(), httpStatus);
        });
    }

    private void persistDestinationFailure(Long eventId, String destination, Exception exception) {
        txTemplate.executeWithoutResult(status -> {
            OutboxEventEntity event = outboxEventRepository.findById(eventId).orElseThrow();
            applyDestinationState(
                    event,
                    destination,
                    "FAILED",
                    limitMessage(exception.getMessage()),
                    null,
                    httpStatus(exception));
        });
    }

    private void applyDestinationState(
            OutboxEventEntity event,
            String destination,
            String status,
            String lastError,
            Instant confirmedAt,
            Integer httpStatus
    ) {
        ObjectNode payload = (ObjectNode) parsePayload(event.getPayload());
        ObjectNode deliveryState = payload.withObject(DELIVERY_STATE);
        ObjectNode destinationState = deliveryState.withObject(destination);
        destinationState.put("status", status);
        destinationState.put("attempts", event.getAttempts());
        if (lastError == null) {
            destinationState.putNull("lastError");
        } else {
            destinationState.put("lastError", limitMessage(lastError));
        }
        if (confirmedAt == null) {
            destinationState.putNull("confirmedAt");
        } else {
            destinationState.put("confirmedAt", confirmedAt.toString());
        }
        if (httpStatus == null) {
            destinationState.putNull("httpStatus");
        } else {
            destinationState.put("httpStatus", httpStatus);
        }
        event.setPayload(payload.toString());
    }

    private JsonNode reloadPayload(Long eventId) {
        return txTemplate.execute(status -> outboxEventRepository.findById(eventId)
                .map(OutboxEventEntity::getPayload)
                .map(this::parsePayload)
                .orElseGet(objectMapper::createObjectNode));
    }

    private boolean isDestinationSent(JsonNode payload, String destination) {
        return "SENT".equals(payload.path(DELIVERY_STATE).path(destination).path("status").asText());
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private Integer httpStatus(Throwable throwable) {
        if (throwable instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode().value();
        }
        return null;
    }

    private TestResultResponse fetchTestResult(String attemptId, String empresaId) {
        CandidateAttemptEntity attempt = candidateAttemptRepository.findByEmpresaIdAndId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado."));
        return gupyTestResultMapper.toResponse(attempt, getSimulation(attempt));
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

    private PublishedSimulation getSimulation(CandidateAttemptEntity attempt) {
        if (attempt.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(attempt.getSimulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos esta versão do teste."));
        }
        return simulationCatalogService.findPublishedById(attempt.getEmpresaId(), attempt.getSimulationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos o teste publicado."));
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new RuntimeException("Erro interno ao processar a entrega.", exception);
        }
    }

    private void handleEventFailure(OutboxEventEntity event, Exception exception) {
        Throwable cause = exception instanceof DestinationDeliveryException && exception.getCause() != null
                ? exception.getCause()
                : exception;
        String errorMessage = limitMessage(cause.getMessage());
        event.setLastError(errorMessage);
        log.warn("Falha ao processar evento {} (tentativa {}): {}", event.getId(), event.getAttempts(), errorMessage);

        boolean permanentContractError = cause instanceof RestClientResponseException responseException
                && responseException.getStatusCode().is4xxClientError()
                && responseException.getStatusCode().value() != 408
                && responseException.getStatusCode().value() != 429;

        if (permanentContractError || event.getAttempts() >= MAX_ATTEMPT_COUNT) {
            event.setStatus(OutboxEventEntity.OutboxEventStatus.DLQ);
            event.setNextAttemptAt(null);
            dlqAlertService.alertEmpresaAdmins(event);
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
        return message.length() <= 1200 ? message : message.substring(0, 1200);
    }

    private static final class UnsupportedOutboxEventTypeException extends RuntimeException {
        private UnsupportedOutboxEventTypeException(Long eventId, String eventType) {
            super("Tipo de evento do outbox não suportado: " + String.valueOf(eventType)
                    + " (evento " + eventId + ").");
        }
    }

    private static final class DestinationDeliveryException extends RuntimeException {
        private final String destination;

        private DestinationDeliveryException(String destination, Throwable cause) {
            super(cause == null ? null : cause.getMessage(), cause);
            this.destination = destination;
        }

        private String destination() {
            return destination;
        }
    }
}
