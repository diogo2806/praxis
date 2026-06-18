package br.com.iforce.praxis.shared.outbox.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.GupyTestResultMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class OutboxProcessor {

    private static final int MAX_ATTEMPT_COUNT = 5;
    private static final String RESULT_READY_EVENT = "RESULT_READY";

    private final OutboxEventRepository outboxEventRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final GupyTestResultMapper gupyTestResultMapper;

    public OutboxProcessor(
        OutboxEventRepository outboxEventRepository,
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        CandidateAttemptRepository candidateAttemptRepository,
        SimulationCatalogService simulationCatalogService,
        GupyTestResultMapper gupyTestResultMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.gupyTestResultMapper = gupyTestResultMapper;
    }

    /**
     * Processa eventos pendentes de todos os tenants a cada 5 segundos.
     */
    @Scheduled(fixedDelay = 5000)
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

    private void processEvent(OutboxEventEntity event) {
        try {
            if (RESULT_READY_EVENT.equals(event.getEventType())) {
                processResultReadyEvent(event);
            }

            event.setStatus(OutboxEventEntity.OutboxEventStatus.SENT);
            event.setNextAttemptAt(null);
        } catch (Exception ex) {
            handleEventFailure(event, ex);
        }
    }

    private void processResultReadyEvent(OutboxEventEntity event) {
        JsonNode payload = parsePayload(event.getPayload());

        String webhookUrl = payload.get("webhookUrl").asText();
        Object testResult = payload.get("testResult");

        if (testResult == null) {
            // Migrated event: need to fetch test result from database
            String attemptId = payload.get("attemptId").asText();
            testResult = fetchTestResult(attemptId, event.getTenantId());
        }

        log.debug("Enviando resultado para webhook: {}", webhookUrl);
        restTemplate.postForObject(webhookUrl, testResult, Void.class);
    }

    private Object fetchTestResult(String attemptId, String tenantId) {
        Optional<CandidateAttemptEntity> attempt = candidateAttemptRepository.findByTenantIdAndId(tenantId, attemptId);
        if (attempt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CandidateAttempt nao encontrado: " + attemptId);
        }

        CandidateAttemptEntity candidateAttemptEntity = attempt.get();
        PublishedSimulation simulation = getSimulation(candidateAttemptEntity);
        return gupyTestResultMapper.toResponse(candidateAttemptEntity, simulation);
    }

    private PublishedSimulation getSimulation(CandidateAttemptEntity candidateAttemptEntity) {
        if (candidateAttemptEntity.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(candidateAttemptEntity.getSimulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versao da simulacao nao encontrada."));
        }

        return simulationCatalogService.findPublishedById(candidateAttemptEntity.getTenantId(), candidateAttemptEntity.getSimulationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulacao publicada nao encontrada."));
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao deserializar payload do outbox event", e);
        }
    }

    private void handleEventFailure(OutboxEventEntity event, Exception exception) {
        event.setAttempts(event.getAttempts() + 1);

        String errorMessage = limitMessage(exception.getMessage());
        log.warn("Falha ao processar evento {} (tentativa {}): {}", event.getId(), event.getAttempts(), errorMessage);

        boolean isContractError = exception instanceof RestClientResponseException responseException
            && responseException.getStatusCode().is4xxClientError();

        if (isContractError || event.getAttempts() >= MAX_ATTEMPT_COUNT) {
            event.setStatus(OutboxEventEntity.OutboxEventStatus.DLQ);
            event.setNextAttemptAt(null);
            log.error("Evento {} movido para DLQ após {} tentativas", event.getId(), event.getAttempts());
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
