package br.com.iforce.praxis.shared.outbox.service;

import br.com.iforce.praxis.gupy.delivery.service.GupyOutboundUrlValidator;
import br.com.iforce.praxis.gupy.delivery.service.ResultWebhookClient;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.GupyTestResultMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.notification.service.ResultDeliveryDlqAlertService;
import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ResultWebhookClient resultWebhookClient;

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;

    @Mock
    private SimulationCatalogService simulationCatalogService;

    @Mock
    private GupyTestResultMapper gupyTestResultMapper;

    @Mock
    private GupyOutboundUrlValidator outboundUrlValidator;

    @Mock
    private ResultDeliveryDlqAlertService dlqAlertService;

    private OutboxProcessor outboxProcessor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        outboxProcessor = new OutboxProcessor(
            outboxEventRepository,
            resultWebhookClient,
            objectMapper,
            candidateAttemptRepository,
            simulationCatalogService,
            gupyTestResultMapper,
            outboundUrlValidator,
            dlqAlertService
        );
    }

    @Test
    void shouldProcessPendingEvent() {
        OutboxEventEntity event = createPendingEvent();
        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of(event));

        outboxProcessor.processReadyEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.SENT);
        assertThat(event.getNextAttemptAt()).isNull();
        verify(resultWebhookClient).postResult(anyString(), any(TestResultResponse.class));
    }

    @Test
    void shouldRetryOnServerError() {
        OutboxEventEntity event = createPendingEvent();
        event.setAttempts(0);
        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of(event));
        doThrow(new RestClientResponseException("Server error", 500, "Internal Server Error", null, null, null))
            .when(resultWebhookClient).postResult(anyString(), any(TestResultResponse.class));

        outboxProcessor.processReadyEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.RETRYING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isNotNull();
    }

    @Test
    void shouldMoveToDeadLetterQueueOnClientError() {
        OutboxEventEntity event = createPendingEvent();
        event.setAttempts(0);
        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of(event));
        doThrow(new RestClientResponseException("Bad request", 400, "Bad Request", null, null, null))
            .when(resultWebhookClient).postResult(anyString(), any(TestResultResponse.class));

        outboxProcessor.processReadyEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.DLQ);
        assertThat(event.getNextAttemptAt()).isNull();
        verify(dlqAlertService).alertTenantAdmins(event);
    }

    @Test
    void shouldMoveToDeadLetterQueueAfterMaxAttempts() {
        OutboxEventEntity event = createPendingEvent();
        event.setAttempts(5);
        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of(event));
        doThrow(new RuntimeException("Network error"))
            .when(resultWebhookClient).postResult(anyString(), any(TestResultResponse.class));

        outboxProcessor.processReadyEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.DLQ);
        assertThat(event.getNextAttemptAt()).isNull();
        verify(dlqAlertService).alertTenantAdmins(event);
    }

    @Test
    void shouldCalculateExponentialBackoffDelay() {
        OutboxEventEntity event = createPendingEvent();
        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of(event));
        doThrow(new RuntimeException("Error"))
            .when(resultWebhookClient).postResult(anyString(), any(TestResultResponse.class));

        Instant beforeRetry = Instant.now();
        for (int attempt = 1; attempt <= 5; attempt++) {
            event.setAttempts(attempt - 1);
            outboxProcessor.processReadyEvents();
            if (attempt < 5) {
                Instant nextRetry = event.getNextAttemptAt();
                long delaySeconds = nextRetry.getEpochSecond() - beforeRetry.getEpochSecond();
                long expectedDelay = switch (attempt) {
                    case 1 -> 1L;
                    case 2 -> 4L;
                    case 3 -> 16L;
                    case 4 -> 64L;
                    default -> 256L;
                };
                assertThat(delaySeconds).isGreaterThanOrEqualTo(expectedDelay - 1)
                    .isLessThanOrEqualTo(expectedDelay + 2);
            }
        }
    }

    @Test
    void shouldFetchTestResultForMigratedEvent() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setTenantId("tenant-1");
        event.setEventType("RESULT_READY");
        event.setAggregateType("CandidateAttempt");
        event.setAggregateId("att_123");
        event.setPayload("{\"webhookUrl\":\"https://example.com/webhook\",\"attemptId\":\"att_123\"}");
        event.setStatus(OutboxEventEntity.OutboxEventStatus.PENDING);
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now());

        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setSimulationVersionId(100L);
        PublishedSimulation simulation = mock(PublishedSimulation.class);
        TestResultResponse testResult = mock(TestResultResponse.class);

        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of(event));
        when(candidateAttemptRepository.findByTenantIdAndId("tenant-1", "att_123"))
            .thenReturn(Optional.of(attempt));
        when(simulationCatalogService.findByVersionId(100L))
            .thenReturn(Optional.of(simulation));
        when(gupyTestResultMapper.toResponse(attempt, simulation))
            .thenReturn(testResult);

        outboxProcessor.processReadyEvents();

        verify(gupyTestResultMapper).toResponse(attempt, simulation);
        verify(resultWebhookClient).postResult("https://example.com/webhook", testResult);
        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.SENT);
    }

    @Test
    void shouldPostAttemptEngagementEventPayload() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setTenantId("tenant-1");
        event.setEventType("ATTEMPT_STARTED");
        event.setAggregateType("CandidateAttempt");
        event.setAggregateId("att_123");
        event.setPayload("""
                {
                  "webhookUrl": "https://example.com/webhook",
                  "eventPayload": {
                    "event_type": "ATTEMPT_STARTED",
                    "attempt_id": "att_123",
                    "status": "inProgress"
                  }
                }
                """);
        event.setStatus(OutboxEventEntity.OutboxEventStatus.PENDING);
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now());

        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of(event));

        outboxProcessor.processReadyEvents();

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(resultWebhookClient).postPayload(eq("https://example.com/webhook"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().toString()).contains("ATTEMPT_STARTED");
        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.SENT);
    }

    @Test
    void shouldNotPostWhenWebhookUrlFailsOutboundValidation() {
        OutboxEventEntity event = createPendingEvent();
        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of(event));
        when(outboundUrlValidator.validate("https://example.com/webhook"))
            .thenThrow(new IllegalArgumentException("URL externa invalida."));

        outboxProcessor.processReadyEvents();

        verify(resultWebhookClient, never()).postResult(anyString(), any(TestResultResponse.class));
        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.RETRYING);
    }

    @Test
    void shouldSkipProcessingWhenNoEventsReady() {
        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of());

        outboxProcessor.processReadyEvents();

        verify(resultWebhookClient, never()).postResult(anyString(), any(TestResultResponse.class));
    }

    private OutboxEventEntity createPendingEvent() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(1L);
        event.setTenantId("tenant-1");
        event.setEventType("RESULT_READY");
        event.setAggregateType("CandidateAttempt");
        event.setAggregateId("att_123");
        event.setPayload("{\"webhookUrl\":\"https://example.com/webhook\",\"testResult\":{\"score\":85}}");
        event.setStatus(OutboxEventEntity.OutboxEventStatus.PENDING);
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now());
        event.setCreatedAt(Instant.now());
        return event;
    }
}
