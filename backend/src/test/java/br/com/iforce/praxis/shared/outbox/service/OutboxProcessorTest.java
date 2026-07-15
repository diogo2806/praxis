package br.com.iforce.praxis.shared.outbox.service;

import br.com.iforce.praxis.gupy.delivery.service.GupyOutboundUrlValidator;
import br.com.iforce.praxis.gupy.delivery.service.ResultWebhookClient;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.GupyTestResultMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.integration.IntegrationManagementService;
import br.com.iforce.praxis.shared.integration.service.AttemptEngagementWebhookService;
import br.com.iforce.praxis.shared.integration.service.ConfirmableGenericWebhookDeliveryService;
import br.com.iforce.praxis.shared.notification.service.ResultDeliveryDlqAlertService;
import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;
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
    @Mock
    private ConfirmableGenericWebhookDeliveryService genericWebhookDeliveryService;
    @Mock
    private AttemptEngagementWebhookService attemptEngagementWebhookService;
    @Mock
    private IntegrationManagementService integrationManagementService;
    @Mock
    private PlatformTransactionManager transactionManager;

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
                dlqAlertService,
                genericWebhookDeliveryService,
                attemptEngagementWebhookService,
                integrationManagementService,
                transactionManager
        );
    }

    private void givenClaimed(OutboxEventEntity event) {
        when(outboxEventRepository.claimReadyBatch(anyList(), any(Instant.class), any(Instant.class), anyInt()))
                .thenReturn(List.of(event));
        when(outboxEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
    }

    @Test
    void shouldProcessPendingEvent() {
        OutboxEventEntity event = createPendingEvent();
        givenClaimed(event);

        outboxProcessor.processReadyEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.SENT);
        assertThat(destinationStatus(event, "GUPY")).isEqualTo("SENT");
        assertThat(destinationStatus(event, "CUSTOM_API")).isEqualTo("SENT");
        verify(resultWebhookClient).postResult(anyString(), any(TestResultResponse.class));
    }

    @Test
    void shouldRetryOnlyCustomApiAfterGupyWasConfirmed() {
        OutboxEventEntity event = createPendingEvent();
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("att_123");
        givenClaimed(event);
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", "att_123"))
                .thenReturn(Optional.of(attempt));
        doThrow(new ConfirmableGenericWebhookDeliveryService.CustomWebhookDeliveryException("indisponível"))
                .doNothing()
                .when(genericWebhookDeliveryService)
                .deliverResultReady("empresa-1", attempt);

        outboxProcessor.processReadyEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.RETRYING);
        assertThat(destinationStatus(event, "GUPY")).isEqualTo("SENT");
        assertThat(destinationStatus(event, "CUSTOM_API")).isEqualTo("RETRYING");

        outboxProcessor.processReadyEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.SENT);
        assertThat(destinationStatus(event, "CUSTOM_API")).isEqualTo("SENT");
        verify(resultWebhookClient, times(1)).postResult(anyString(), any(TestResultResponse.class));
        verify(genericWebhookDeliveryService, times(2)).deliverResultReady("empresa-1", attempt);
    }

    @Test
    void shouldRetryGupyOnServerErrorWithoutCallingCustomApi() {
        OutboxEventEntity event = createPendingEvent();
        givenClaimed(event);
        doThrow(new RestClientResponseException("Server error", 500, "Internal Server Error", null, null, null))
                .when(resultWebhookClient).postResult(anyString(), any(TestResultResponse.class));

        outboxProcessor.processReadyEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.RETRYING);
        assertThat(destinationStatus(event, "GUPY")).isEqualTo("RETRYING");
        verifyNoInteractions(genericWebhookDeliveryService);
    }

    @Test
    void shouldMoveDestinationToDeadLetterQueueOnClientError() {
        OutboxEventEntity event = createPendingEvent();
        givenClaimed(event);
        doThrow(new RestClientResponseException("Bad request", 400, "Bad Request", null, null, null))
                .when(resultWebhookClient).postResult(anyString(), any(TestResultResponse.class));

        outboxProcessor.processReadyEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.DLQ);
        assertThat(destinationStatus(event, "GUPY")).isEqualTo("DLQ");
        verify(dlqAlertService).alertEmpresaAdmins(event);
    }

    @Test
    void shouldFetchTestResultForMigratedEvent() {
        OutboxEventEntity event = createPendingEvent();
        event.setPayload("{\"webhookUrl\":\"https://example.com/webhook\",\"attemptId\":\"att_123\"}");
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setSimulationVersionId(100L);
        PublishedSimulation simulation = mock(PublishedSimulation.class);
        TestResultResponse testResult = mock(TestResultResponse.class);
        givenClaimed(event);
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", "att_123"))
                .thenReturn(Optional.of(attempt));
        when(simulationCatalogService.findByVersionId(100L)).thenReturn(Optional.of(simulation));
        when(gupyTestResultMapper.toResponse(attempt, simulation)).thenReturn(testResult);

        outboxProcessor.processReadyEvents();

        verify(resultWebhookClient).postResult("https://example.com/webhook", testResult);
        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.SENT);
    }

    @Test
    void shouldDeliverAttemptEngagementOnlyThroughCustomApi() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(3L);
        event.setEmpresaId("empresa-1");
        event.setEventType("ATTEMPT_STARTED");
        event.setAggregateType("CandidateAttempt");
        event.setAggregateId("att_123");
        event.setPayload("""
                {
                  "event": "ATTEMPT_STARTED",
                  "tenantId": "empresa-1",
                  "attemptId": "att_123"
                }
                """);
        event.setStatus(OutboxEventEntity.OutboxEventStatus.PENDING);
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now());
        givenClaimed(event);

        outboxProcessor.processReadyEvents();

        verify(attemptEngagementWebhookService).deliver(eq("empresa-1"), eq("ATTEMPT_STARTED"), any(JsonNode.class));
        verify(resultWebhookClient, never()).postPayload(anyString(), any(JsonNode.class));
        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.SENT);
        assertThat(destinationStatus(event, "CUSTOM_API")).isEqualTo("SENT");
    }

    @Test
    void shouldRetryUnsupportedEventTypeWithoutMarkingItAsSent() {
        OutboxEventEntity event = createUnsupportedEvent(77L, 0);
        givenClaimed(event);

        outboxProcessor.processReadyEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.RETRYING);
        assertThat(event.getSentAt()).isNull();
        assertThat(event.getNextAttemptAt()).isNotNull();
        assertThat(event.getLastError())
                .contains("UNSUPPORTED_EVENT")
                .contains("77");
        verifyNoInteractions(resultWebhookClient, genericWebhookDeliveryService, attemptEngagementWebhookService);
    }

    @Test
    void shouldMoveUnsupportedEventTypeToDeadLetterQueueAfterMaxAttempts() {
        OutboxEventEntity event = createUnsupportedEvent(78L, 4);
        givenClaimed(event);

        outboxProcessor.processReadyEvents();

        assertThat(event.getAttempts()).isEqualTo(5);
        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.DLQ);
        assertThat(event.getSentAt()).isNull();
        assertThat(event.getNextAttemptAt()).isNull();
        assertThat(event.getLastError())
                .contains("UNSUPPORTED_EVENT")
                .contains("78");
        verify(dlqAlertService).alertEmpresaAdmins(event);
        verifyNoInteractions(resultWebhookClient, genericWebhookDeliveryService, attemptEngagementWebhookService);
    }

    @Test
    void shouldSkipProcessingWhenNoEventsReady() {
        when(outboxEventRepository.claimReadyBatch(anyList(), any(Instant.class), any(Instant.class), anyInt()))
                .thenReturn(List.of());

        outboxProcessor.processReadyEvents();

        verifyNoInteractions(resultWebhookClient, genericWebhookDeliveryService, attemptEngagementWebhookService);
    }

    private String destinationStatus(OutboxEventEntity event, String destination) {
        try {
            return objectMapper.readTree(event.getPayload())
                    .path("deliveryState")
                    .path(destination)
                    .path("status")
                    .asText();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private OutboxEventEntity createPendingEvent() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(1L);
        event.setEmpresaId("empresa-1");
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

    private OutboxEventEntity createUnsupportedEvent(Long id, int attempts) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(id);
        event.setEmpresaId("empresa-1");
        event.setEventType("UNSUPPORTED_EVENT");
        event.setAggregateType("UnknownAggregate");
        event.setAggregateId("unknown-aggregate");
        event.setPayload("{}");
        event.setStatus(OutboxEventEntity.OutboxEventStatus.PENDING);
        event.setAttempts(attempts);
        event.setNextAttemptAt(Instant.now());
        event.setCreatedAt(Instant.now());
        return event;
    }
}
