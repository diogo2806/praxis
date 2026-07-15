package br.com.iforce.praxis.shared.outbox.service;

import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 *
 * Se a entrega falhar, o evento fica na fila para tentar novamente. Isso garante que:
 * - Integrações externas (Gupy, Recrutei, webhooks) sempre recebem as notificações
 * - Mesmo que um serviço externo esteja indisponível, não perdemos dados
 * - A operação do usuário termina rápido (não aguarda chamadas externas)
 */
@Service
public class OutboxService {

    private static final String ATTEMPT_STARTED_EVENT = "ATTEMPT_STARTED";
    private static final String ATTEMPT_ABANDONED_EVENT = "ATTEMPT_ABANDONED";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra um evento na fila para processamento posterior.
     *
     * Armazena o evento com status "pendente" para ser entregue mais tarde.
     * O evento permanece no banco até ser processado com sucesso.
     *
     * Este método DEVE ser chamado dentro de uma transação de banco de dados existente.
     * Isso garante que se algo der errado após publicar o evento, o banco inteiro
     * volta atrás (rollback), evitando inconsistências.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String empresaId, String eventType, String aggregateType, String aggregateId, Object payload) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setEmpresaId(empresaId);
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setPayload(serializePayload(normalizePayload(eventType, payload)));
        event.setStatus(OutboxEventEntity.OutboxEventStatus.PENDING);
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now());
        event.setCreatedAt(Instant.now());

        outboxEventRepository.save(event);
    }

    /**
     * Mantém o campo atual {@code event} e acrescenta o alias legado
     * {@code event_type} somente nos eventos proprietários de engajamento.
     */
    private Object normalizePayload(String eventType, Object payload) {
        if (!(ATTEMPT_STARTED_EVENT.equals(eventType) || ATTEMPT_ABANDONED_EVENT.equals(eventType))
                || !(payload instanceof Map<?, ?> source)) {
            return payload;
        }

        Map<Object, Object> compatible = new LinkedHashMap<>(source);
        compatible.putIfAbsent("event", eventType);
        compatible.putIfAbsent("event_type", eventType);
        return compatible;
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Erro interno ao preparar entrega.", e);
        }
    }
}
