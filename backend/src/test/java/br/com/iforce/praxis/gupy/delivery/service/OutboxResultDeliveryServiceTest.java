package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.gupy.delivery.dto.ProcessReadyDeliveriesResponse;
import br.com.iforce.praxis.gupy.delivery.dto.ResultDeliveryResponse;
import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import br.com.iforce.praxis.shared.outbox.service.OutboxProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxResultDeliveryServiceTest {

    private static final String EMPRESA_ID = "empresa-1";
    private static final String EVENT_TYPE = "RESULT_READY";
    private static final Instant CREATED_AT = Instant.parse("2026-07-16T12:00:00Z");

    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final CandidateAttemptRepository candidateAttemptRepository = mock(CandidateAttemptRepository.class);
    private final CurrentEmpresaService currentEmpresaService = mock(CurrentEmpresaService.class);
    private final OutboxProcessor outboxProcessor = mock(OutboxProcessor.class);
    private final OutboxResultDeliveryService service = new OutboxResultDeliveryService(
            outboxEventRepository,
            candidateAttemptRepository,
            currentEmpresaService,
            outboxProcessor,
            new ObjectMapper()
    );

    @Test
    void listsDeliveriesAndMapsWebhookPayload() {
        OutboxEventEntity event = event(1L, "attempt-1", OutboxEventEntity.OutboxEventStatus.RETRYING, EVENT_TYPE);
        CandidateAttemptEntity attempt = attempt("attempt-1", "result-1", "simulation-1", 2);
        when(currentEmpresaService.requiredEmpresaId()).thenReturn(EMPRESA_ID);
        when(outboxEventRepository.findByEmpresaIdAndEventTypeOrderByCreatedAtDesc(EMPRESA_ID, EVENT_TYPE))
                .thenReturn(List.of(event));
        when(candidateAttemptRepository.findByEmpresaIdAndId(EMPRESA_ID, "attempt-1"))
                .thenReturn(Optional.of(attempt));

        List<ResultDeliveryResponse> deliveries = service.listDeliveries(null, null, null);

        assertThat(deliveries).singleElement().satisfies(delivery -> {
            assertThat(delivery.id()).isEqualTo(1L);
            assertThat(delivery.attemptId()).isEqualTo("attempt-1");
            assertThat(delivery.resultId()).isEqualTo("result-1");
            assertThat(delivery.webhookUrl()).isEqualTo("https://cliente.gupy.io/result-webhook");
            assertThat(delivery.status()).isEqualTo(ResultDeliveryStatus.RETRYING);
            assertThat(delivery.attemptCount()).isEqualTo(2);
        });
    }

    @Test
    void filtersDeliveriesBySimulationAndVersion() {
        OutboxEventEntity matchingEvent = event(1L, "attempt-1", OutboxEventEntity.OutboxEventStatus.PENDING, EVENT_TYPE);
        OutboxEventEntity otherEvent = event(2L, "attempt-2", OutboxEventEntity.OutboxEventStatus.PENDING, EVENT_TYPE);
        CandidateAttemptEntity matchingAttempt = attempt("attempt-1", "result-1", "simulation-target", 3);
        CandidateAttemptEntity otherAttempt = attempt("attempt-2", "result-2", "simulation-other", 3);
        when(currentEmpresaService.requiredEmpresaId()).thenReturn(EMPRESA_ID);
        when(outboxEventRepository.findByEmpresaIdAndEventTypeOrderByCreatedAtDesc(EMPRESA_ID, EVENT_TYPE))
                .thenReturn(List.of(matchingEvent, otherEvent));
        when(candidateAttemptRepository.findByEmpresaIdAndId(EMPRESA_ID, "attempt-1"))
                .thenReturn(Optional.of(matchingAttempt));
        when(candidateAttemptRepository.findByEmpresaIdAndId(EMPRESA_ID, "attempt-2"))
                .thenReturn(Optional.of(otherAttempt));
        when(candidateAttemptRepository.findById("attempt-1")).thenReturn(Optional.of(matchingAttempt));
        when(candidateAttemptRepository.findById("attempt-2")).thenReturn(Optional.of(otherAttempt));

        List<ResultDeliveryResponse> deliveries = service.listDeliveries(null, "simulation-target", 3);

        assertThat(deliveries).extracting(ResultDeliveryResponse::attemptId)
                .containsExactly("attempt-1");
    }

    @Test
    void usesStatusSpecificQueryWhenStatusIsProvided() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn(EMPRESA_ID);
        when(outboxEventRepository.findByEmpresaIdAndEventTypeAndStatusOrderByCreatedAtDesc(
                EMPRESA_ID,
                EVENT_TYPE,
                OutboxEventEntity.OutboxEventStatus.SENT
        )).thenReturn(List.of());

        assertThat(service.listDeliveries(ResultDeliveryStatus.SENT, null, null)).isEmpty();

        verify(outboxEventRepository).findByEmpresaIdAndEventTypeAndStatusOrderByCreatedAtDesc(
                EMPRESA_ID,
                EVENT_TYPE,
                OutboxEventEntity.OutboxEventStatus.SENT
        );
    }

    @Test
    void readyForRetryIgnoresOutboxEventsFromOtherDomains() {
        OutboxEventEntity resultEvent = event(1L, "attempt-1", OutboxEventEntity.OutboxEventStatus.PENDING, EVENT_TYPE);
        OutboxEventEntity unrelatedEvent = event(2L, "attempt-2", OutboxEventEntity.OutboxEventStatus.PENDING, "EMAIL_READY");
        CandidateAttemptEntity attempt = attempt("attempt-1", "result-1", "simulation-1", 1);
        when(currentEmpresaService.requiredEmpresaId()).thenReturn(EMPRESA_ID);
        when(outboxEventRepository.findByEmpresaIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(EMPRESA_ID),
                eq(List.of(OutboxEventEntity.OutboxEventStatus.PENDING, OutboxEventEntity.OutboxEventStatus.RETRYING)),
                any(Instant.class)
        )).thenReturn(List.of(resultEvent, unrelatedEvent));
        when(candidateAttemptRepository.findByEmpresaIdAndId(EMPRESA_ID, "attempt-1"))
                .thenReturn(Optional.of(attempt));

        List<ResultDeliveryResponse> deliveries = service.listReadyForRetry();

        assertThat(deliveries).extracting(ResultDeliveryResponse::id).containsExactly(1L);
    }

    @Test
    void processesReadyDeliveriesAndReturnsRemainingQueue() {
        OutboxEventEntity remainingEvent = event(3L, "attempt-3", OutboxEventEntity.OutboxEventStatus.RETRYING, EVENT_TYPE);
        CandidateAttemptEntity attempt = attempt("attempt-3", "result-3", "simulation-3", 1);
        when(currentEmpresaService.requiredEmpresaId()).thenReturn(EMPRESA_ID);
        when(outboxProcessor.processReadyEventsForEmpresa(EMPRESA_ID)).thenReturn(4);
        when(outboxEventRepository.findByEmpresaIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(EMPRESA_ID),
                eq(List.of(OutboxEventEntity.OutboxEventStatus.PENDING, OutboxEventEntity.OutboxEventStatus.RETRYING)),
                any(Instant.class)
        )).thenReturn(List.of(remainingEvent));
        when(candidateAttemptRepository.findByEmpresaIdAndId(EMPRESA_ID, "attempt-3"))
                .thenReturn(Optional.of(attempt));

        ProcessReadyDeliveriesResponse response = service.processReadyDeliveries();

        assertThat(response.processedCount()).isEqualTo(4);
        assertThat(response.deliveries()).extracting(ResultDeliveryResponse::id).containsExactly(3L);
        verify(outboxProcessor).processReadyEventsForEmpresa(EMPRESA_ID);
    }

    @Test
    void reprocessReturnsNotFoundWhenDeliveryDoesNotBelongToCurrentEmpresa() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn(EMPRESA_ID);
        when(outboxEventRepository.findByIdAndEmpresaId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reprocessDelivery(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Entrega de resultado não encontrada.");
                });

        verify(outboxProcessor).reprocessEvent(99L, EMPRESA_ID);
    }

    @Test
    void invalidPayloadIsReportedAsInternalServerErrorWithoutLeakingParserDetails() {
        OutboxEventEntity event = event(1L, "attempt-1", OutboxEventEntity.OutboxEventStatus.PENDING, EVENT_TYPE);
        event.setPayload("{invalid-json");
        CandidateAttemptEntity attempt = attempt("attempt-1", "result-1", "simulation-1", 1);
        when(currentEmpresaService.requiredEmpresaId()).thenReturn(EMPRESA_ID);
        when(outboxEventRepository.findByEmpresaIdAndEventTypeOrderByCreatedAtDesc(EMPRESA_ID, EVENT_TYPE))
                .thenReturn(List.of(event));
        when(candidateAttemptRepository.findByEmpresaIdAndId(EMPRESA_ID, "attempt-1"))
                .thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.listDeliveries(null, null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(exception.getReason()).isEqualTo("Ocorreu um erro interno.");
                });
    }

    private OutboxEventEntity event(
            Long id,
            String attemptId,
            OutboxEventEntity.OutboxEventStatus status,
            String eventType
    ) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(id);
        event.setEmpresaId(EMPRESA_ID);
        event.setEventType(eventType);
        event.setAggregateType("CandidateAttempt");
        event.setAggregateId(attemptId);
        event.setPayload("{\"webhookUrl\":\"https://cliente.gupy.io/result-webhook\"}");
        event.setStatus(status);
        event.setAttempts(2);
        event.setNextAttemptAt(CREATED_AT.plusSeconds(60));
        event.setLastAttemptAt(CREATED_AT);
        event.setCreatedAt(CREATED_AT);
        return event;
    }

    private CandidateAttemptEntity attempt(String id, String resultId, String simulationId, int versionNumber) {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId(id);
        attempt.setEmpresaId(EMPRESA_ID);
        attempt.setResultId(resultId);
        attempt.setSimulationId(simulationId);
        attempt.setSimulationVersionNumber(versionNumber);
        return attempt;
    }
}
