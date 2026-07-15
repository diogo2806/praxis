package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.gupy.delivery.dto.CallbackDeliveryResponse;
import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;
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
public class CallbackDeliveryService {

    private static final String CALLBACK_EVENT = "GUPY_CALLBACK_CONFIRMATION";
    private static final String CALLBACK_DESTINATION = "GUPY_CALLBACK";

    private final OutboxEventRepository outboxEventRepository;
    private final CurrentEmpresaService currentEmpresaService;
    private final OutboxProcessor outboxProcessor;
    private final ObjectMapper objectMapper;

    public CallbackDeliveryService(
            OutboxEventRepository outboxEventRepository,
            CurrentEmpresaService currentEmpresaService,
            OutboxProcessor outboxProcessor,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.currentEmpresaService = currentEmpresaService;
        this.outboxProcessor = outboxProcessor;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<CallbackDeliveryResponse> listDeliveries(ResultDeliveryStatus status) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        List<OutboxEventEntity> events = status == null
                ? outboxEventRepository.findByEmpresaIdAndEventTypeOrderByCreatedAtDesc(empresaId, CALLBACK_EVENT)
                : outboxEventRepository.findByEmpresaIdAndEventTypeAndStatusOrderByCreatedAtDesc(
                        empresaId,
                        CALLBACK_EVENT,
                        OutboxEventEntity.OutboxEventStatus.valueOf(status.name())
                );
        return events.stream().map(this::toResponse).toList();
    }

    public CallbackDeliveryResponse reprocess(Long deliveryId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        OutboxEventEntity event = findCallbackDelivery(deliveryId, empresaId);
        outboxProcessor.reprocessEvent(event.getId(), empresaId);
        return toResponse(findCallbackDelivery(deliveryId, empresaId));
    }

    private OutboxEventEntity findCallbackDelivery(Long deliveryId, String empresaId) {
        OutboxEventEntity event = outboxEventRepository.findByIdAndEmpresaId(deliveryId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Entrega de callback não encontrada."
                ));
        if (!CALLBACK_EVENT.equals(event.getEventType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega de callback não encontrada.");
        }
        return event;
    }

    private CallbackDeliveryResponse toResponse(OutboxEventEntity event) {
        JsonNode payload = parsePayload(event.getPayload());
        JsonNode destinationState = payload.path("deliveryState").path(CALLBACK_DESTINATION);
        return new CallbackDeliveryResponse(
                event.getId(),
                event.getAggregateId(),
                textOrNull(payload.get("callbackUrl")),
                ResultDeliveryStatus.valueOf(event.getStatus().name()),
                event.getAttempts(),
                integerOrNull(destinationState.get("httpStatus")),
                event.getNextAttemptAt(),
                event.getLastAttemptAt(),
                instantOrNull(destinationState.get("confirmedAt")),
                firstNonBlank(textOrNull(destinationState.get("lastError")), event.getLastError()),
                event.getCreatedAt()
        );
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno.", exception);
        }
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() || node.asText().isBlank() ? null : node.asText();
    }

    private Integer integerOrNull(JsonNode node) {
        return node == null || node.isNull() || !node.canConvertToInt() ? null : node.asInt();
    }

    private Instant instantOrNull(JsonNode node) {
        String value = textOrNull(node);
        return value == null ? null : Instant.parse(value);
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
