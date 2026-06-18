package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.gupy.delivery.dto.ProcessReadyDeliveriesResponse;
import br.com.iforce.praxis.gupy.delivery.dto.ReprocessDeliveryResponse;
import br.com.iforce.praxis.gupy.delivery.dto.ResultDeliveryResponse;
import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import br.com.iforce.praxis.shared.outbox.service.OutboxProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxResultDeliveryService {

    private static final String RESULT_READY_EVENT = "RESULT_READY";

    private final OutboxEventRepository outboxEventRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final CurrentTenantService currentTenantService;
    private final OutboxProcessor outboxProcessor;
    private final ObjectMapper objectMapper;

    public OutboxResultDeliveryService(
            OutboxEventRepository outboxEventRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            CurrentTenantService currentTenantService,
            OutboxProcessor outboxProcessor,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.currentTenantService = currentTenantService;
        this.outboxProcessor = outboxProcessor;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ResultDeliveryResponse> listDeliveries(
            ResultDeliveryStatus status,
            String simulationId,
            Integer versionNumber
    ) {
        String tenantId = currentTenantService.requiredTenantId();
        List<OutboxEventEntity> events = status == null
                ? outboxEventRepository.findByTenantIdAndEventTypeOrderByCreatedAtDesc(tenantId, RESULT_READY_EVENT)
                : outboxEventRepository.findByTenantIdAndEventTypeAndStatusOrderByCreatedAtDesc(
                        tenantId,
                        RESULT_READY_EVENT,
                        toOutboxStatus(status)
                );

        return events.stream()
                .map(this::toResponse)
                .filter(response -> matchesSimulationFilter(response, simulationId, versionNumber))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResultDeliveryResponse> listReadyForRetry() {
        return outboxEventRepository
                .findByTenantIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        currentTenantService.requiredTenantId(),
                        List.of(OutboxEventEntity.OutboxEventStatus.PENDING, OutboxEventEntity.OutboxEventStatus.RETRYING),
                        Instant.now()
                )
                .stream()
                .filter(event -> RESULT_READY_EVENT.equals(event.getEventType()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProcessReadyDeliveriesResponse processReadyDeliveries() {
        String tenantId = currentTenantService.requiredTenantId();
        int processed = outboxProcessor.processReadyEventsForTenant(tenantId);
        List<ResultDeliveryResponse> deliveries = listReadyForRetry();
        return new ProcessReadyDeliveriesResponse(processed, deliveries);
    }

    @Transactional
    public ReprocessDeliveryResponse reprocessDelivery(Long deliveryId) {
        String tenantId = currentTenantService.requiredTenantId();
        outboxProcessor.reprocessEvent(deliveryId, tenantId);
        OutboxEventEntity event = outboxEventRepository.findByIdAndTenantId(deliveryId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega de resultado nao encontrada."));
        return new ReprocessDeliveryResponse(toResponse(event));
    }

    private boolean matchesSimulationFilter(ResultDeliveryResponse response, String simulationId, Integer versionNumber) {
        if ((simulationId == null || simulationId.isBlank()) && versionNumber == null) {
            return true;
        }
        CandidateAttemptEntity attempt = candidateAttemptRepository.findById(response.attemptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa nao encontrada."));
        boolean matchesSimulation = simulationId == null || simulationId.isBlank() || simulationId.equals(attempt.getSimulationId());
        boolean matchesVersion = versionNumber == null || versionNumber.equals(attempt.getSimulationVersionNumber());
        return matchesSimulation && matchesVersion;
    }

    private ResultDeliveryResponse toResponse(OutboxEventEntity event) {
        CandidateAttemptEntity attempt = candidateAttemptRepository.findByTenantIdAndId(event.getTenantId(), event.getAggregateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa nao encontrada."));
        return new ResultDeliveryResponse(
                event.getId(),
                attempt.getId(),
                attempt.getResultId(),
                webhookUrl(event),
                toResultDeliveryStatus(event.getStatus()),
                event.getAttempts(),
                event.getNextAttemptAt(),
                null,
                event.getStatus() == OutboxEventEntity.OutboxEventStatus.SENT ? event.getCreatedAt() : null,
                null,
                event.getCreatedAt()
        );
    }

    private String webhookUrl(OutboxEventEntity event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            JsonNode webhookUrl = payload.get("webhookUrl");
            return webhookUrl == null ? null : webhookUrl.asText();
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payload do outbox invalido.", exception);
        }
    }

    private OutboxEventEntity.OutboxEventStatus toOutboxStatus(ResultDeliveryStatus status) {
        return OutboxEventEntity.OutboxEventStatus.valueOf(status.name());
    }

    private ResultDeliveryStatus toResultDeliveryStatus(OutboxEventEntity.OutboxEventStatus status) {
        return ResultDeliveryStatus.valueOf(status.name());
    }
}
