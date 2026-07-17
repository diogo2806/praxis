package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.gupy.delivery.dto.ResultDeliveryResponse;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import br.com.iforce.praxis.shared.outbox.service.OutboxProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxResultDeliverySecurityTest {

    private static final String EMPRESA_ID = "empresa-1";

    @Test
    void removesQueryFragmentAndCredentialsFromOperationalResponse() {
        OutboxEventRepository outboxRepository = mock(OutboxEventRepository.class);
        CandidateAttemptRepository attemptRepository = mock(CandidateAttemptRepository.class);
        CurrentEmpresaService currentEmpresaService = mock(CurrentEmpresaService.class);
        OutboxProcessor outboxProcessor = mock(OutboxProcessor.class);
        OutboxResultDeliveryService service = new OutboxResultDeliveryService(
                outboxRepository,
                attemptRepository,
                currentEmpresaService,
                outboxProcessor,
                new ObjectMapper()
        );

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(1L);
        event.setEmpresaId(EMPRESA_ID);
        event.setEventType("RESULT_READY");
        event.setAggregateType("CandidateAttempt");
        event.setAggregateId("attempt-1");
        event.setPayload("{\"webhookUrl\":\"https://cliente.gupy.io/result?token=segredo#interno\"}");
        event.setStatus(OutboxEventEntity.OutboxEventStatus.RETRYING);
        event.setAttempts(2);
        event.setCreatedAt(Instant.parse("2026-07-17T10:00:00Z"));
        event.setLastError("HTTP 401 Authorization=segredo Bearer token-super-secreto");

        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("attempt-1");
        attempt.setEmpresaId(EMPRESA_ID);
        attempt.setResultId("result-1");

        when(currentEmpresaService.requiredEmpresaId()).thenReturn(EMPRESA_ID);
        when(outboxRepository.findByEmpresaIdAndEventTypeOrderByCreatedAtDesc(EMPRESA_ID, "RESULT_READY"))
                .thenReturn(List.of(event));
        when(attemptRepository.findByEmpresaIdAndId(EMPRESA_ID, "attempt-1"))
                .thenReturn(Optional.of(attempt));

        ResultDeliveryResponse response = service.listDeliveries(null, null, null).getFirst();

        assertThat(response.webhookUrl()).isEqualTo("https://cliente.gupy.io/result");
        assertThat(response.webhookUrl()).doesNotContain("segredo", "token=", "#interno");
        assertThat(response.lastError()).contains("[REDACTED]");
        assertThat(response.lastError()).doesNotContain("token-super-secreto", "Authorization=segredo");
    }
}
