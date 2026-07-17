package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

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
    private static final int MAX_PUBLIC_ERROR_LENGTH = 500;
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)Bearer\\s+[^\\s,;]+");
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(authorization|token|secret|api[-_]?key)(\\s*[=:]\\s*)[^\\s&,;]+"
    );

    private final OutboxEventRepository outboxEventRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final CurrentEmpresaService currentEmpresaService;
    private final OutboxProcessor outboxProcessor;
    private final ObjectMapper objectMapper;

    public OutboxResultDeliveryService(
            OutboxEventRepository outboxEventRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            CurrentEmpresaService currentEmpresaService,
            OutboxProcessor outboxProcessor,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.currentEmpresaService = currentEmpresaService;
        this.outboxProcessor = outboxProcessor;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ResultDeliveryResponse> listDeliveries(
            ResultDeliveryStatus status,
            String simulationId,
            Integer versionNumber
    ) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        List<OutboxEventEntity> events = status == null
                ? outboxEventRepository.findByEmpresaIdAndEventTypeOrderByCreatedAtDesc(empresaId, RESULT_READY_EVENT)
                : outboxEventRepository.findByEmpresaIdAndEventTypeAndStatusOrderByCreatedAtDesc(
                        empresaId,
                        RESULT_READY_EVENT,
                        toOutboxStatus(status)
                );

        return events.stream()
                .map(this::toResponse)
                .filter(response -> matchesSimulationFilter(
                        empresaId,
                        response,
                        simulationId,
                        versionNumber
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResultDeliveryResponse> listReadyForRetry() {
        return outboxEventRepository
                .findByEmpresaIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        currentEmpresaService.requiredEmpresaId(),
                        List.of(OutboxEventEntity.OutboxEventStatus.PENDING, OutboxEventEntity.OutboxEventStatus.RETRYING),
                        Instant.now()
                )
                .stream()
                .filter(event -> RESULT_READY_EVENT.equals(event.getEventType()))
                .map(this::toResponse)
                .toList();
    }

    // Sem @Transactional: o OutboxProcessor faz a entrega HTTP fora de qualquer transação
    // aberta e persiste o resultado em transações curtas próprias.
    public ProcessReadyDeliveriesResponse processReadyDeliveries() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        int processed = outboxProcessor.processReadyEventsForEmpresa(empresaId);
        List<ResultDeliveryResponse> deliveries = listReadyForRetry();
        return new ProcessReadyDeliveriesResponse(processed, deliveries);
    }

    // Sem @Transactional: idem processReadyDeliveries — a entrega HTTP roda fora de transação.
    public ReprocessDeliveryResponse reprocessDelivery(Long deliveryId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        outboxProcessor.reprocessEvent(deliveryId, empresaId);
        OutboxEventEntity event = outboxEventRepository.findByIdAndEmpresaId(deliveryId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega de resultado não encontrada."));
        return new ReprocessDeliveryResponse(toResponse(event));
    }

    private boolean matchesSimulationFilter(
            String empresaId,
            ResultDeliveryResponse response,
            String simulationId,
            Integer versionNumber
    ) {
        if ((simulationId == null || simulationId.isBlank()) && versionNumber == null) {
            return true;
        }
        CandidateAttemptEntity attempt = candidateAttemptRepository
                .findByEmpresaIdAndId(empresaId, response.attemptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        boolean matchesSimulation = simulationId == null
                || simulationId.isBlank()
                || simulationId.equals(attempt.getSimulationId());
        boolean matchesVersion = versionNumber == null
                || versionNumber.equals(attempt.getSimulationVersionNumber());
        return matchesSimulation && matchesVersion;
    }

    private ResultDeliveryResponse toResponse(OutboxEventEntity event) {
        CandidateAttemptEntity attempt = candidateAttemptRepository
                .findByEmpresaIdAndId(event.getEmpresaId(), event.getAggregateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        return new ResultDeliveryResponse(
                event.getId(),
                attempt.getId(),
                attempt.getResultId(),
                maskWebhookUrl(webhookUrl(event)),
                toResultDeliveryStatus(event.getStatus()),
                event.getAttempts(),
                event.getNextAttemptAt(),
                event.getLastAttemptAt(),
                event.getSentAt(),
                sanitizeLastError(event.getLastError()),
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

    private String maskWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return webhookUrl;
        }
        try {
            URI uri = URI.create(webhookUrl);
            return new URI(
                    uri.getScheme(),
                    null,
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    null,
                    null
            ).toASCIIString();
        } catch (IllegalArgumentException | URISyntaxException exception) {
            return null;
        }
    }

    private String sanitizeLastError(String lastError) {
        if (lastError == null || lastError.isBlank()) {
            return lastError;
        }
        String sanitized = BEARER_PATTERN.matcher(lastError).replaceAll("Bearer [REDACTED]");
        sanitized = SECRET_PATTERN.matcher(sanitized).replaceAll("$1$2[REDACTED]");
        return sanitized.length() <= MAX_PUBLIC_ERROR_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_PUBLIC_ERROR_LENGTH) + "...";
    }

    private OutboxEventEntity.OutboxEventStatus toOutboxStatus(ResultDeliveryStatus status) {
        return OutboxEventEntity.OutboxEventStatus.valueOf(status.name());
    }

    private ResultDeliveryStatus toResultDeliveryStatus(OutboxEventEntity.OutboxEventStatus status) {
        return ResultDeliveryStatus.valueOf(status.name());
    }
}
