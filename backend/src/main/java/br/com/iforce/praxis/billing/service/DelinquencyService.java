package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.TenantStatus;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.billing.model.SubscriptionStatus;
import br.com.iforce.praxis.billing.persistence.entity.TenantSubscriptionEntity;
import br.com.iforce.praxis.billing.persistence.repository.TenantSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Suspende automaticamente clientes PROFISSIONAL cuja inadimplência ultrapassou a carência
 * configurável ({@code mp.grace-period-days}). A suspensão é registrada na auditoria como ação
 * do sistema. O histórico de assinatura é preservado.
 */
@Service
public class DelinquencyService {

    private static final Logger log = LoggerFactory.getLogger(DelinquencyService.class);
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;

    public DelinquencyService(
            TenantSubscriptionRepository subscriptionRepository,
            TenantRepository tenantRepository,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.tenantRepository = tenantRepository;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
    }

    /** Roda periodicamente (configurável via {@code mp.delinquency-cron}). */
    @Scheduled(cron = "${mp.delinquency-cron:0 0 * * * *}")
    @Transactional
    public void suspendExpiredGracePeriods() {
        suspendDelinquentPastGrace(Instant.now());
    }

    /** Núcleo testável: suspende inadimplentes cuja carência venceu antes de {@code moment}. */
    @Transactional
    public int suspendDelinquentPastGrace(Instant moment) {
        List<TenantSubscriptionEntity> expired = subscriptionRepository
                .findByStatusAndGraceUntilBefore(SubscriptionStatus.DELINQUENT, moment);
        int suspended = 0;
        for (TenantSubscriptionEntity subscription : expired) {
            String tenantId = subscription.getTenantId();
            boolean changed = tenantRepository.findById(tenantId).map(tenant -> {
                if (tenant.getStatus().blocksAccess()) {
                    return false;
                }
                tenant.setStatus(TenantStatus.SUSPENSO);
                tenant.setUpdatedAt(Instant.now());
                return true;
            }).orElse(false);

            if (changed) {
                auditEventService.auditAdminAction(SYSTEM_ACTOR, tenantId,
                        AuditEventType.ADMIN_TENANT_SUSPENDED,
                        "Cliente suspenso automaticamente após carência de inadimplência.",
                        auditMetadata.of("reason", "Inadimplência após carência",
                                "graceUntil", String.valueOf(subscription.getGraceUntil())));
                suspended++;
            }
        }
        if (suspended > 0) {
            log.info("Inadimplência: {} cliente(s) suspenso(s) após carência.", suspended);
        }
        return suspended;
    }
}
