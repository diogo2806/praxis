package br.com.iforce.praxis.shared.outbox.service;

import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica um evento no outbox. Deve ser chamado dentro de uma transação existente.
     * O evento fica pendente para processamento assincrono depois.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String tenantId, String eventType, String aggregateType, String aggregateId, Object payload) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setTenantId(tenantId);
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setPayload(serializePayload(payload));
        event.setStatus(OutboxEventEntity.OutboxEventStatus.PENDING);
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now());
        event.setCreatedAt(Instant.now());

        outboxEventRepository.save(event);
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar payload do outbox event", e);
        }
    }
}
