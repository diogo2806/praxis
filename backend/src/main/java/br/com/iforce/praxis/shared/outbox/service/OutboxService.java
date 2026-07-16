package br.com.iforce.praxis.shared.outbox.service;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gerencia a fila de eventos para entrega assíncrona.
 *
 * Quando uma ação importante acontece no sistema (criar candidato, atualizar resultado, etc),
 * em vez de tentar enviar a notificação imediatamente, registra o evento em um fila (outbox).
 * Um processador em background pega esses eventos e tenta entregá-los.
 */
@Service
public class OutboxService {

    private static final String ATTEMPT_STARTED_EVENT = "ATTEMPT_STARTED";
    private static final String ATTEMPT_ABANDONED_EVENT = "ATTEMPT_ABANDONED";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final CandidateAttemptRepository candidateAttemptRepository;

    @Autowired
    public OutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            CandidateAttemptRepository candidateAttemptRepository
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.candidateAttemptRepository = candidateAttemptRepository;
    }

    /** Construtor mantido para testes unitários isolados do outbox. */
    OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.candidateAttemptRepository = null;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String empresaId, String eventType, String aggregateType, String aggregateId, Object payload) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setEmpresaId(empresaId);
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setPayload(serializePayload(normalizePayload(eventType, aggregateId, payload)));
        event.setStatus(OutboxEventEntity.OutboxEventStatus.PENDING);
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now());
        event.setCreatedAt(Instant.now());

        outboxEventRepository.save(event);
    }

    /**
     * Mantém aliases usados por consumidores operacionais antigos. A URL fica
     * somente no registro interno do outbox e é removida antes da entrega ao
     * webhook CUSTOM_API.
     */
    private Object normalizePayload(String eventType, String aggregateId, Object payload) {
        if (!isEngagementEvent(eventType) || !(payload instanceof Map<?, ?> source)) {
            return payload;
        }

        Map<Object, Object> compatible = new LinkedHashMap<>(source);
        compatible.putIfAbsent("event", eventType);
        compatible.putIfAbsent("event_type", eventType);
        legacyResultWebhookUrl(aggregateId).ifPresent(url -> compatible.putIfAbsent("webhookUrl", url));
        return compatible;
    }

    private java.util.Optional<String> legacyResultWebhookUrl(String aggregateId) {
        if (candidateAttemptRepository == null || aggregateId == null || aggregateId.isBlank()) {
            return java.util.Optional.empty();
        }
        return candidateAttemptRepository.findById(aggregateId)
                .map(attempt -> attempt.getResultWebhookUrl())
                .filter(url -> url != null && !url.isBlank());
    }

    private boolean isEngagementEvent(String eventType) {
        return ATTEMPT_STARTED_EVENT.equals(eventType) || ATTEMPT_ABANDONED_EVENT.equals(eventType);
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Erro interno ao preparar entrega.", e);
        }
    }
}
