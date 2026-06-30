package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.audit.service.AuditMetadata;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.billing.model.SubscriptionStatus;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaSubscriptionEntity;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaSubscriptionRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;


import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.List;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class DelinquencyServiceTest {

    @Mock private EmpresaSubscriptionRepository subscriptionRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private AuditEventService auditEventService;

    private DelinquencyService service;

    @BeforeEach
    void setUp() {
        service = new DelinquencyService(subscriptionRepository, empresaRepository, auditEventService,
                new AuditMetadata(new ObjectMapper()));
    }

    @Test
    void suspendsDelinquentEmpresaPastGraceAndAudits() {
        Instant now = Instant.now();
        EmpresaSubscriptionEntity subscription = new EmpresaSubscriptionEntity();
        subscription.setEmpresaId("t1");
        subscription.setStatus(SubscriptionStatus.DELINQUENT);
        subscription.setGraceUntil(now.minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByStatusAndGraceUntilBefore(eq(SubscriptionStatus.DELINQUENT), any()))
                .thenReturn(List.of(subscription));

        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("t1");
        empresa.setStatus(EmpresaStatus.INADIMPLENTE);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));

        int suspended = service.suspendDelinquentPastGrace(now);

        assertThat(suspended).isEqualTo(1);
        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.SUSPENSO);
        verify(auditEventService).auditAdminAction(
                eq("SYSTEM"), eq("t1"), eq(AuditEventType.ADMIN_EMPRESA_SUSPENDED), anyString(), anyString());
    }

    @Test
    void skipsAlreadyBlockedEmpresa() {
        EmpresaSubscriptionEntity subscription = new EmpresaSubscriptionEntity();
        subscription.setEmpresaId("t1");
        subscription.setStatus(SubscriptionStatus.DELINQUENT);
        subscription.setGraceUntil(Instant.now().minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByStatusAndGraceUntilBefore(eq(SubscriptionStatus.DELINQUENT), any()))
                .thenReturn(List.of(subscription));

        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("t1");
        empresa.setStatus(EmpresaStatus.CANCELADO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));

        int suspended = service.suspendDelinquentPastGrace(Instant.now());

        assertThat(suspended).isZero();
        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.CANCELADO);
    }
}
