package br.com.iforce.praxis.shared.outbox.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.GupyTestResultMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
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
import org.springframework.web.client.RestTemplate;

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
    private RestTemplate restTemplate;

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;

    @Mock
    private SimulationCatalogService simulationCatalogService;

    @Mock
    private GupyTestResultMapper gupyTestResultMapper;

    private OutboxProcessor outboxProcessor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        outboxProcessor = new OutboxProcessor(
            outboxEventRepository,
            restTemplate,
            objectMapper,
            candidateAttemptRepository,
            simulationCatalogService,
            gupyTestResultMapper
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
        verify(restTemplate).postForObject(anyString(), any(), eq(Void.class));
    }

    @Test
    void shouldRetryOnServerError() {
        OutboxEventEntity event = createPendingEvent();
        event.setAttempts(0);
        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of(event));
        when(restTemplate.postForObject(anyString(), any(), eq(Void.class)))
            .thenThrow(new RestClientResponseException("Server error", 500, "Internal Server Error", null, null, null));

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
        when(restTemplate.postForObject(anyString(), any(), eq(Void.class)))
            .thenThrow(new RestClientResponseException("Bad request", 400, "Bad Request", null, null, null));

        outboxProcessor.processReadyEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.DLQ);
        assertThat(event.getNextAttemptAt()).isNull();
    }

    @Test
    void shouldMoveToDeadLetterQueueAfterMaxAttempts() {
        OutboxEventEntity event = createPendingEvent();
        event.setAttempts(5);
        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of(event));
        when(restTemplate.postForObject(anyString(), any(), eq(Void.class)))
            .thenThrow(new RuntimeException("Network error"));

        outboxProcessor.processReadyEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.DLQ);
        assertThat(event.getNextAttemptAt()).isNull();
    }

    @Test
    void shouldCalculateExponentialBackoffDelay() {
        OutboxEventEntity event = createPendingEvent();
        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of(event));
        when(restTemplate.postForObject(anyString(), any(), eq(Void.class)))
            .thenThrow(new RuntimeException("Error"));

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
        PublishedSimulation simulation = mock(PublishedSimulation.class);
        TestResultResponse testResult = mock(TestResultResponse.class);

        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of(event));
        when(candidateAttemptRepository.findByTenantIdAndId("tenant-1", "att_123"))
            .thenReturn(Optional.of(attempt));
        when(simulationCatalogService.findByVersionId(null))
            .thenReturn(Optional.of(simulation));
        when(gupyTestResultMapper.toResponse(attempt, simulation))
            .thenReturn(testResult);

        outboxProcessor.processReadyEvents();

        verify(gupyTestResultMapper).toResponse(attempt, simulation);
        verify(restTemplate).postForObject("https://example.com/webhook", testResult, Void.class);
        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.SENT);
    }

    @Test
    void shouldSkipProcessingWhenNoEventsReady() {
        when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            anyList(), any(Instant.class)
        )).thenReturn(List.of());

        outboxProcessor.processReadyEvents();

        verify(restTemplate, never()).postForObject(anyString(), any(), any());
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
