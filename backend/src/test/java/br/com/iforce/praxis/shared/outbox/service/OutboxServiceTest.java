package br.com.iforce.praxis.shared.outbox.service;

import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxService outboxService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        outboxService = new OutboxService(outboxEventRepository, objectMapper);
    }

    @Test
    void shouldPublishEventWithPayload() {
        String tenantId = "tenant-1";
        String eventType = "RESULT_READY";
        String aggregateType = "CandidateAttempt";
        String aggregateId = "att_123";
        Map<String, Object> payload = Map.of(
            "webhookUrl", "https://example.com/webhook",
            "testResult", Map.of("score", 85)
        );

        outboxService.publish(tenantId, eventType, aggregateType, aggregateId, payload);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEventEntity savedEvent = captor.getValue();
        assertThat(savedEvent.getTenantId()).isEqualTo(tenantId);
        assertThat(savedEvent.getEventType()).isEqualTo(eventType);
        assertThat(savedEvent.getAggregateType()).isEqualTo(aggregateType);
        assertThat(savedEvent.getAggregateId()).isEqualTo(aggregateId);
        assertThat(savedEvent.getStatus()).isEqualTo(OutboxEventEntity.OutboxEventStatus.PENDING);
        assertThat(savedEvent.getAttempts()).isZero();
        assertThat(savedEvent.getPayload()).contains("webhookUrl");
        assertThat(savedEvent.getCreatedAt()).isNotNull();
        assertThat(savedEvent.getNextAttemptAt()).isNotNull();
    }

    @Test
    void shouldSerializePayloadAsJson() {
        String tenantId = "tenant-1";
        Map<String, String> payload = Map.of("key", "value");

        outboxService.publish(tenantId, "TEST_EVENT", "TestAggregate", "agg_1", payload);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEventEntity savedEvent = captor.getValue();
        assertThat(savedEvent.getPayload()).contains("\"key\"");
        assertThat(savedEvent.getPayload()).contains("\"value\"");
    }

    @Test
    void shouldHandleComplexPayloadStructure() {
        String tenantId = "tenant-1";
        Map<String, Object> payload = Map.of(
            "webhookUrl", "https://api.example.com/callback",
            "attemptId", "att_456",
            "testResult", Map.of(
                "score", 92,
                "competencies", new String[]{"Problem Solving", "Communication"}
            )
        );

        outboxService.publish(tenantId, "RESULT_READY", "CandidateAttempt", "att_456", payload);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEventEntity savedEvent = captor.getValue();
        assertThat(savedEvent.getPayload()).contains("webhookUrl");
        assertThat(savedEvent.getPayload()).contains("testResult");
        assertThat(savedEvent.getPayload()).contains("competencies");
    }
}
