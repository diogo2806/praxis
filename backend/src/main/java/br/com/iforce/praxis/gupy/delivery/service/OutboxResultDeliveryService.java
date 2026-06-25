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

/**
 * Gerencia a fila de entrega dos resultados de prova para a Gupy.
 *
 * <p>Na visão do processo, quando uma prova termina, o resultado fica
 * registrado para ser enviado de volta à Gupy (o endereço de webhook dela). O
 * envio é assíncrono e com novas tentativas automáticas em caso de falha. Este
 * componente é o painel de controle dessa fila: permite consultar o que já foi
 * entregue, o que está aguardando reenvio e o que falhou, além de disparar o
 * reenvio em lote ou de uma entrega específica.</p>
 */
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

    /**
     * Lista as entregas de resultado da empresa atual para acompanhamento.
     *
     * <p>Permite filtrar pela situação da entrega e/ou pela prova e versão
     * específicas. É apenas consulta.</p>
     *
     * @param status situação desejada (ou nulo para todas)
     * @param simulationId prova específica (opcional)
     * @param versionNumber versão específica da prova (opcional)
     * @return as entregas que atendem aos filtros
     */
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

    /**
     * Lista as entregas que já estão prontas para uma nova tentativa de envio.
     *
     * <p>São as entregas pendentes ou em retentativa cujo horário agendado
     * para nova tentativa já passou.</p>
     *
     * @return as entregas prontas para reenvio
     */
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

    /**
     * Processa, de uma vez, todas as entregas prontas da empresa atual.
     *
     * <p>Dispara o envio de cada entrega cujo prazo de espera já venceu e
     * devolve quantas foram processadas, junto com as que ainda restam.</p>
     *
     * @return o total processado e as entregas ainda pendentes
     */
    // Sem @Transactional: o OutboxProcessor faz a entrega HTTP fora de qualquer transação
    // aberta e persiste o resultado em transações curtas próprias.
    public ProcessReadyDeliveriesResponse processReadyDeliveries() {
        String tenantId = currentTenantService.requiredTenantId();
        int processed = outboxProcessor.processReadyEventsForTenant(tenantId);
        List<ResultDeliveryResponse> deliveries = listReadyForRetry();
        return new ProcessReadyDeliveriesResponse(processed, deliveries);
    }

    /**
     * Reenvia manualmente uma entrega específica.
     *
     * <p>Força uma nova tentativa de envio do resultado para a Gupy e devolve
     * a situação atualizada da entrega.</p>
     *
     * @param deliveryId identificador da entrega a reenviar
     * @return a situação atualizada da entrega após a tentativa
     */
    // Sem @Transactional: idem processReadyDeliveries — a entrega HTTP roda fora de transação.
    public ReprocessDeliveryResponse reprocessDelivery(Long deliveryId) {
        String tenantId = currentTenantService.requiredTenantId();
        outboxProcessor.reprocessEvent(deliveryId, tenantId);
        OutboxEventEntity event = outboxEventRepository.findByIdAndTenantId(deliveryId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega de resultado não encontrada."));
        return new ReprocessDeliveryResponse(toResponse(event));
    }

    private boolean matchesSimulationFilter(ResultDeliveryResponse response, String simulationId, Integer versionNumber) {
        if ((simulationId == null || simulationId.isBlank()) && versionNumber == null) {
            return true;
        }
        CandidateAttemptEntity attempt = candidateAttemptRepository.findById(response.attemptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        boolean matchesSimulation = simulationId == null || simulationId.isBlank() || simulationId.equals(attempt.getSimulationId());
        boolean matchesVersion = versionNumber == null || versionNumber.equals(attempt.getSimulationVersionNumber());
        return matchesSimulation && matchesVersion;
    }

    private ResultDeliveryResponse toResponse(OutboxEventEntity event) {
        CandidateAttemptEntity attempt = candidateAttemptRepository.findByTenantIdAndId(event.getTenantId(), event.getAggregateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        return new ResultDeliveryResponse(
                event.getId(),
                attempt.getId(),
                attempt.getResultId(),
                webhookUrl(event),
                toResultDeliveryStatus(event.getStatus()),
                event.getAttempts(),
                event.getNextAttemptAt(),
                event.getLastAttemptAt(),
                event.getSentAt(),
                event.getLastError(),
                event.getCreatedAt()
        );
    }

    private String webhookUrl(OutboxEventEntity event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            JsonNode webhookUrl = payload.get("webhookUrl");
            return webhookUrl == null ? null : webhookUrl.asText();
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno.", exception);
        }
    }

    private OutboxEventEntity.OutboxEventStatus toOutboxStatus(ResultDeliveryStatus status) {
        return OutboxEventEntity.OutboxEventStatus.valueOf(status.name());
    }

    private ResultDeliveryStatus toResultDeliveryStatus(OutboxEventEntity.OutboxEventStatus status) {
        return ResultDeliveryStatus.valueOf(status.name());
    }
}
