package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.billing.dto.DunningNotice;

import br.com.iforce.praxis.billing.model.BillingEventType;

import br.com.iforce.praxis.billing.model.DunningStage;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaBillingEventEntity;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaSubscriptionEntity;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaBillingEventRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaSubscriptionRepository;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;


import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.List;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class BillingDunningServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

    @Mock private EmpresaRepository empresaRepository;
    @Mock private EmpresaSubscriptionRepository subscriptionRepository;
    @Mock private EmpresaBillingEventRepository eventRepository;
    @Mock private BillingDunningNotificationSender notificationSender;

    private BillingDunningService service;

    @BeforeEach
    void setUp() {
        service = new BillingDunningService(
                empresaRepository, subscriptionRepository, eventRepository, notificationSender, 20);
    }

    private EmpresaEntity empresa(String id, EmpresaStatus status) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(id);
        empresa.setName("Cliente " + id);
        empresa.setCorporateEmail(id + "@cliente.com");
        empresa.setPhone("+5511999990000");
        empresa.setStatus(status);
        return empresa;
    }

    @Test
    void notifyPaymentFailureRecordsEventAndSendsRetryNotice() {
        Instant graceUntil = NOW.plus(7, ChronoUnit.DAYS);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa("t1", EmpresaStatus.INADIMPLENTE)));
        EmpresaSubscriptionEntity subscription = new EmpresaSubscriptionEntity();
        subscription.setEmpresaId("t1");
        subscription.setGraceUntil(graceUntil);
        when(subscriptionRepository.findFirstByEmpresaIdOrderByCreatedAtDesc("t1"))
                .thenReturn(Optional.of(subscription));

        service.notifyPaymentFailure("t1");

        ArgumentCaptor<EmpresaBillingEventEntity> eventCaptor =
                ArgumentCaptor.forClass(EmpresaBillingEventEntity.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(BillingEventType.DUNNING_NOTIFIED);
        assertThat(eventCaptor.getValue().getMpStatus()).isEqualTo(DunningStage.PAYMENT_FAILED.name());

        ArgumentCaptor<DunningNotice> noticeCaptor = ArgumentCaptor.forClass(DunningNotice.class);
        verify(notificationSender).sendRetryNotice(noticeCaptor.capture());
        DunningNotice notice = noticeCaptor.getValue();
        assertThat(notice.stage()).isEqualTo(DunningStage.PAYMENT_FAILED);
        assertThat(notice.status()).isEqualTo(EmpresaStatus.INADIMPLENTE);
        assertThat(notice.graceUntil()).isEqualTo(graceUntil);
    }

    @Test
    void notifyPaymentFailureSkipsAlreadyBlockedEmpresa() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa("t1", EmpresaStatus.SUSPENSO)));

        service.notifyPaymentFailure("t1");

        verify(eventRepository, never()).save(any());
        verify(notificationSender, never()).sendRetryNotice(any());
    }

    @Test
    void remindSkipsClientsAlreadyNotifiedWithinInterval() {
        when(empresaRepository.findByStatuses(
                List.of(EmpresaStatus.PENDENTE_PAGAMENTO, EmpresaStatus.INADIMPLENTE)))
                .thenReturn(List.of(empresa("t1", EmpresaStatus.INADIMPLENTE), empresa("t2", EmpresaStatus.PENDENTE_PAGAMENTO)));
        when(eventRepository.existsByEmpresaIdAndEventTypeAndCreatedAtAfter(
                eq("t1"), eq(BillingEventType.DUNNING_NOTIFIED), any())).thenReturn(false);
        when(eventRepository.existsByEmpresaIdAndEventTypeAndCreatedAtAfter(
                eq("t2"), eq(BillingEventType.DUNNING_NOTIFIED), any())).thenReturn(true);
        when(subscriptionRepository.findFirstByEmpresaIdOrderByCreatedAtDesc("t1"))
                .thenReturn(Optional.empty());

        int reminded = service.remindClientsBeforeSuspension(NOW);

        assertThat(reminded).isEqualTo(1);
        ArgumentCaptor<DunningNotice> noticeCaptor = ArgumentCaptor.forClass(DunningNotice.class);
        verify(notificationSender).sendRetryNotice(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().empresaId()).isEqualTo("t1");
        assertThat(noticeCaptor.getValue().stage()).isEqualTo(DunningStage.RETRY_REMINDER);
        verify(eventRepository).save(any(EmpresaBillingEventEntity.class));
    }
}
