package br.com.iforce.praxis.shared.notification.service;

import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.notification.persistence.repository.InAppNotificationRepository;
import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResultDeliveryDlqAlertServiceTest {

    @Test
    void incrementsOperationalMetricWhenEmpresaHasNoAdminRecipient() {
        UserRepository userRepository = mock(UserRepository.class);
        CandidateAttemptRepository candidateAttemptRepository = mock(CandidateAttemptRepository.class);
        InAppNotificationRepository notificationRepository = mock(InAppNotificationRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ResultDeliveryDlqAlertService service = new ResultDeliveryDlqAlertService(
                userRepository,
                candidateAttemptRepository,
                notificationRepository,
                new ObjectMapper(),
                meterRegistry
        );

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(10L);
        event.setEmpresaId("empresa-sem-admin");
        event.setAggregateId("attempt-10");

        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-sem-admin", "attempt-10"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmpresaIdAndRole("empresa-sem-admin", "EMPRESA"))
                .thenReturn(List.of());

        service.alertEmpresaAdmins(event);

        assertThat(meterRegistry.counter("praxis.outbox.dlq.unassigned").count()).isEqualTo(1.0);
        verifyNoInteractions(notificationRepository);
    }
}
