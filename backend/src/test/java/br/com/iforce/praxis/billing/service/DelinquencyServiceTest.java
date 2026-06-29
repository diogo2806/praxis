package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.TenantStatus;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.billing.model.SubscriptionStatus;
import br.com.iforce.praxis.billing.persistence.entity.TenantSubscriptionEntity;
import br.com.iforce.praxis.billing.persistence.repository.TenantSubscriptionRepository;
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

    @Mock private TenantSubscriptionRepository subscriptionRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private AuditEventService auditEventService;

    private DelinquencyService service;

    @BeforeEach
    void setUp() {
        service = new DelinquencyService(subscriptionRepository, tenantRepository, auditEventService,
                new AuditMetadata(new ObjectMapper()));
    }

    @Test
    void suspendsDelinquentTenantPastGraceAndAudits() {
        Instant now = Instant.now();
        TenantSubscriptionEntity subscription = new TenantSubscriptionEntity();
        subscription.setTenantId("t1");
        subscription.setStatus(SubscriptionStatus.DELINQUENT);
        subscription.setGraceUntil(now.minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByStatusAndGraceUntilBefore(eq(SubscriptionStatus.DELINQUENT), any()))
                .thenReturn(List.of(subscription));

        TenantEntity tenant = new TenantEntity();
        tenant.setId("t1");
        tenant.setStatus(TenantStatus.INADIMPLENTE);
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant));

        int suspended = service.suspendDelinquentPastGrace(now);

        assertThat(suspended).isEqualTo(1);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENSO);
        verify(auditEventService).auditAdminAction(
                eq("SYSTEM"), eq("t1"), eq(AuditEventType.ADMIN_TENANT_SUSPENDED), anyString(), anyString());
    }

    @Test
    void skipsAlreadyBlockedTenant() {
        TenantSubscriptionEntity subscription = new TenantSubscriptionEntity();
        subscription.setTenantId("t1");
        subscription.setStatus(SubscriptionStatus.DELINQUENT);
        subscription.setGraceUntil(Instant.now().minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByStatusAndGraceUntilBefore(eq(SubscriptionStatus.DELINQUENT), any()))
                .thenReturn(List.of(subscription));

        TenantEntity tenant = new TenantEntity();
        tenant.setId("t1");
        tenant.setStatus(TenantStatus.CANCELADO);
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant));

        int suspended = service.suspendDelinquentPastGrace(Instant.now());

        assertThat(suspended).isZero();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.CANCELADO);
    }
}
