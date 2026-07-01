package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.audit.service.AuditMetadata;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.billing.model.SubscriptionStatus;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaSubscriptionEntity;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaSubscriptionRepository;

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

    private final EmpresaSubscriptionRepository subscriptionRepository;
    private final EmpresaRepository empresaRepository;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;

    public DelinquencyService(
            EmpresaSubscriptionRepository subscriptionRepository,
            EmpresaRepository empresaRepository,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.empresaRepository = empresaRepository;
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
        List<EmpresaSubscriptionEntity> expired = subscriptionRepository
                .findByStatusAndGraceUntilBefore(SubscriptionStatus.DELINQUENT, moment);
        int suspended = 0;
        for (EmpresaSubscriptionEntity subscription : expired) {
            String empresaId = subscription.getEmpresaId();
            boolean changed = empresaRepository.findById(empresaId).map(empresa -> {
                if (empresa.getStatus().blocksAccess()) {
                    return false;
                }
                empresa.setStatus(EmpresaStatus.SUSPENSO);
                empresa.setUpdatedAt(Instant.now());
                return true;
            }).orElse(false);

            if (changed) {
                auditEventService.auditAdminAction(SYSTEM_ACTOR, empresaId,
                        AuditEventType.ADMIN_EMPRESA_SUSPENDED,
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
