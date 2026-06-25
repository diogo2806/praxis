package br.com.iforce.praxis.shared.outbox.service;

import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
     *
     * @param tenantId A empresa que gerou o evento
     * @param eventType Tipo de evento (ex: ATTEMPT_COMPLETED, RESULT_PROCESSED)
     * @param aggregateType Entidade afetada (ex: CANDIDATE_ATTEMPT, INTEGRATION_RESULT)
     * @param aggregateId ID único do registro afetado
     * @param payload Dados completos do evento em formato JSON
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

    /**
     * Converte os dados do evento para formato JSON.
     *
     * Transforma o objeto de dados em texto JSON para armazenar no banco de dados
     * de forma compacta. Quando o evento for processado, o JSON será decodificado
     * novamente para a estrutura de dados original.
     *
     * @param payload Objeto contendo os dados do evento
     * @return Representação em JSON do payload
     */
    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Erro interno ao preparar entrega.", e);
        }
    }
}
