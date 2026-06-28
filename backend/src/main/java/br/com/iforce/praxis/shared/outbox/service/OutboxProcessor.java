package br.com.iforce.praxis.shared.outbox.service;

import br.com.iforce.praxis.gupy.delivery.service.ResultWebhookClient;
import br.com.iforce.praxis.gupy.delivery.service.GupyOutboundUrlValidator;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.GupyTestResultMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.notification.service.ResultDeliveryDlqAlertService;
import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.time.Duration;
import java.time.Instant;

/**
 * Processa e entrega eventos armazenados na fila do outbox.
 *
 * Funciona como um "carteiro" que busca eventos pendentes de entrega (como resultados
 * de provas, status de candidatos) e os envia para os webhooks cadastrados nas integrações.
 *
 * Se uma entrega falhar:
 * - Tenta novamente com mais tempo de espera entre as tentativas
 * - Após 5 tentativas sem sucesso, move o evento para "Dead Letter Queue" (DLQ)
 * - Avisa os administradores da empresa que há eventos nãoentregáveis
 *
 * Processa em lotes de 100 eventos por vez para não sobrecarregar o sistema.
 * Isso garante que todas as integrações externas sempre recebem as notificações,
 * mesmo que o servidor seja reiniciado ou haja indisponibilidades temporárias.
 */
@Slf4j
@Component
public class OutboxProcessor {

    private static final int MAX_ATTEMPT_COUNT = 5;
    private static final int BATCH_SIZE = 100;
    /**
     * Eventos PROCESSING mais antigos que isto são considerados órfãos (a instância que os
     * reivindicou provavelmente morreu) e podem ser reivindicados novamente.
     */
    private static final Duration STUCK_PROCESSING_TIMEOUT = Duration.ofMinutes(5);
    private static final String RESULT_READY_EVENT = "RESULT_READY";
    private static final String ATTEMPT_STARTED_EVENT = "ATTEMPT_STARTED";
    private static final String ATTEMPT_ABANDONED_EVENT = "ATTEMPT_ABANDONED";

    private final OutboxEventRepository outboxEventRepository;
    private final ResultWebhookClient resultWebhookClient;
    private final ObjectMapper objectMapper;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final GupyTestResultMapper gupyTestResultMapper;
    private final GupyOutboundUrlValidator outboundUrlValidator;
    private final ResultDeliveryDlqAlertService dlqAlertService;
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
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Processa todos os eventos pendentes de entrega no sistema.
     *
     * Busca um lote de até 100 eventos que estão prontos para serem entregues,
     * bloqueia eles (para outra instância não processar em paralelo), e tenta
     * enviar para os webhooks correspondentes.
     *
     * Se a tentativa falhar, agenda uma nova tentativa com backoff (espera progressiva).
     * Se depois de 5 tentativas ainda não funcionar, desiste e marca como irrecuperável (DLQ).
     */
    public void processReadyEvents() {
        List<Long> claimedIds = claimBatch();
        if (claimedIds.isEmpty()) {
            return;
        }

        log.info("Processando {} eventos do outbox", claimedIds.size());

        for (Long id : claimedIds) {
            deliverAndFinalize(id);
        }
    }

    /**
     * Processa eventos pendentes de uma empresa específica.
     *
     * Semelhante a {@link #processReadyEvents()}, mas lida apenas com eventos
     * da empresa informada. Usado quando se quer reprocessar eventos de uma
     * empresa em particular ou disparar processamento sob demanda.
     *
     * @param tenantId A empresa cujos eventos serão processados
     * @return Quantidade de eventos que foram processados
     */
    public int processReadyEventsForTenant(String tenantId) {
        List<Long> claimedIds = claimBatchForTenant(tenantId);
        for (Long id : claimedIds) {
            deliverAndFinalize(id);
        }
        return claimedIds.size();
    }

    /**
     * Reprocessa um evento específico que falhou anteriormente.
     *
     * Usado quando um evento ficou preso em "Dead Letter Queue" (DLQ) porque
     * falhou 5 vezes. Um administrador pode corrigir o problema (por exemplo,
     * corrigir a URL do webhook) e chamar este método para tentar novamente.
     *
     * Se o evento já foi entregue com sucesso, ignora o reprocessamento.
     *
     * @param eventId ID do evento a reprocessar
     * @param tenantId A empresa que o evento pertence (para isolamento de dados)
     */
    public void reprocessEvent(Long eventId, String tenantId) {
        Long claimedId = txTemplate.execute(status -> {
            OutboxEventEntity event = outboxEventRepository.findByIdAndTenantId(eventId, tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento de entrega não encontrado."));
            if (event.getStatus() == OutboxEventEntity.OutboxEventStatus.SENT) {
                return null;
            }
            markClaimed(event);
            return event.getId();
        });
        if (claimedId == null) {
            return;
        }
        deliverAndFinalize(claimedId);
    }

    /** TX CURTA: trava o lote, marca PROCESSING e incrementa a tentativa. O commit libera a trava. */
    private List<Long> claimBatch() {
        return txTemplate.execute(status -> {
            List<OutboxEventEntity> batch = outboxEventRepository.claimReadyBatch(
                readyStatuses(),
                Instant.now(),
                stuckBefore(),
                BATCH_SIZE
            );
            batch.forEach(this::markClaimed);
            return batch.stream().map(OutboxEventEntity::getId).toList();
        });
    }

    /** TX CURTA por tenant: trava e marca PROCESSING. */
    private List<Long> claimBatchForTenant(String tenantId) {
        return txTemplate.execute(status -> {
            List<OutboxEventEntity> batch = outboxEventRepository.claimReadyBatchForTenant(
                tenantId,
                readyStatuses(),
                Instant.now(),
                stuckBefore(),
                BATCH_SIZE
            );
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
            OutboxEventEntity.OutboxEventStatus.RETRYING.name()
        );
    }

    private Instant stuckBefore() {
        return Instant.now().minus(STUCK_PROCESSING_TIMEOUT);
    }

    /** FORA de transação: faz o POST e só depois grava o resultado em uma TX CURTA. */
    private void deliverAndFinalize(Long eventId) {
        OutboxEventEntity snapshot = txTemplate.execute(s ->
            outboxEventRepository.findById(eventId).orElse(null));
        if (snapshot == null) {
            return;
        }
        try {
            dispatch(snapshot); // HTTP aqui, SEM transação aberta
            txTemplate.executeWithoutResult(s -> {
                OutboxEventEntity event = outboxEventRepository.findById(eventId).orElseThrow();
                event.setStatus(OutboxEventEntity.OutboxEventStatus.SENT);
                event.setNextAttemptAt(null);
                event.setSentAt(Instant.now());
                event.setLastError(null);
            });
        } catch (Exception ex) {
            txTemplate.executeWithoutResult(s -> {
                OutboxEventEntity event = outboxEventRepository.findById(eventId).orElseThrow();
                handleEventFailure(event, ex);
            });
        }
    }

    /** Apenas o roteamento do POST — sem mexer no status. */
    private void dispatch(OutboxEventEntity event) {
        if (RESULT_READY_EVENT.equals(event.getEventType())) {
            processResultReadyEvent(event);
        } else if (ATTEMPT_STARTED_EVENT.equals(event.getEventType())
                || ATTEMPT_ABANDONED_EVENT.equals(event.getEventType())) {
            processAttemptEngagementEvent(event);
        }
    }

    private void processResultReadyEvent(OutboxEventEntity event) {
        JsonNode payload = parsePayload(event.getPayload());

        String webhookUrl = payload.get("webhookUrl").asText();
        TestResultResponse testResult = null;
        JsonNode testResultNode = payload.get("testResult");
        if (testResultNode != null && !testResultNode.isNull()) {
            testResult = toTestResult(testResultNode);
        }

        if (testResult == null) {
            // Migrated event: need to fetch test result from database
            String attemptId = payload.get("attemptId").asText();
            testResult = fetchTestResult(attemptId, event.getTenantId());
        }

        outboundUrlValidator.validate(webhookUrl);
        log.debug("Enviando resultado para webhook: {}", webhookUrl);
        resultWebhookClient.postResult(webhookUrl, testResult);
    }

    private void processAttemptEngagementEvent(OutboxEventEntity event) {
        JsonNode payload = parsePayload(event.getPayload());

        String webhookUrl = payload.get("webhookUrl").asText();
        JsonNode eventPayload = payload.get("eventPayload");
        if (eventPayload == null || eventPayload.isNull()) {
            throw new IllegalArgumentException("Erro interno ao processar a entrega.");
        }

        outboundUrlValidator.validate(webhookUrl);
        log.debug("Enviando evento {} para webhook: {}", event.getEventType(), webhookUrl);
        resultWebhookClient.postPayload(webhookUrl, eventPayload);
    }

    private TestResultResponse fetchTestResult(String attemptId, String tenantId) {
        Optional<CandidateAttemptEntity> attempt = candidateAttemptRepository.findByTenantIdAndId(tenantId, attemptId);
        if (attempt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado.");
        }

        CandidateAttemptEntity candidateAttemptEntity = attempt.get();
        PublishedSimulation simulation = getSimulation(candidateAttemptEntity);
        return gupyTestResultMapper.toResponse(candidateAttemptEntity, simulation);
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

    private PublishedSimulation getSimulation(CandidateAttemptEntity candidateAttemptEntity) {
        if (candidateAttemptEntity.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(candidateAttemptEntity.getSimulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos esta versão do teste."));
        }

        return simulationCatalogService.findPublishedById(candidateAttemptEntity.getTenantId(), candidateAttemptEntity.getSimulationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos o teste publicado."));
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new RuntimeException("Erro interno ao processar a entrega.", e);
        }
    }

    private void handleEventFailure(OutboxEventEntity event, Exception exception) {
        String errorMessage = limitMessage(exception.getMessage());
        event.setLastError(errorMessage);
        log.warn("Falha ao processar evento {} (tentativa {}): {}", event.getId(), event.getAttempts(), errorMessage);

        boolean isContractError = exception instanceof RestClientResponseException responseException
            && responseException.getStatusCode().is4xxClientError();

        if (isContractError || event.getAttempts() >= MAX_ATTEMPT_COUNT) {
            event.setStatus(OutboxEventEntity.OutboxEventStatus.DLQ);
            event.setNextAttemptAt(null);
            log.error("Evento {} movido para DLQ após {} tentativas", event.getId(), event.getAttempts());
            dlqAlertService.alertTenantAdmins(event);
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
