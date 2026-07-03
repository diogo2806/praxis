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
 * Cobrador automático — suspende quem ficou inadimplente além do prazo de carência.
 *
 * <p>Na visão do processo, quando a mensalidade de um assinante (plano PROFISSIONAL) é recusada,
 * ele não é cortado na hora: ganha um prazo de carência para regularizar ({@code
 * mp.grace-period-days}). Este serviço é o vigia que roda de tempos em tempos e, ao encontrar
 * clientes cuja carência venceu sem pagamento, suspende o acesso deles. A suspensão fica registrada
 * na auditoria como uma ação "do sistema" (não de uma pessoa), e o histórico da assinatura é
 * preservado — nada é apagado.</p>
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

    /**
     * Tarefa agendada que dispara a varredura de inadimplentes de tempos em tempos.
     *
     * <p>Roda automaticamente na frequência configurada ({@code mp.delinquency-cron}, por padrão a
     * cada hora) e simplesmente aciona a suspensão dos clientes cuja carência já venceu neste
     * momento. Ninguém precisa clicar em nada para que aconteça.</p>
     */
    @Scheduled(cron = "${mp.delinquency-cron:0 0 * * * *}")
    @Transactional
    public void suspendExpiredGracePeriods() {
        suspendDelinquentPastGrace(Instant.now());
    }

    /**
     * Suspende os assinantes inadimplentes cuja carência venceu até o momento indicado.
     *
     * <p>Fluxo do processo: percorre as assinaturas em atraso cujo prazo de carência já passou e,
     * para cada cliente que ainda não estava bloqueado, marca o acesso como suspenso e registra a
     * ação na auditoria como uma decisão do sistema. Devolve quantos clientes foram efetivamente
     * suspensos nesta passada. É o mesmo trabalho da tarefa agendada, mas recebendo o "momento" por
     * fora — o que permite testá-lo com datas controladas.</p>
     *
     * @param moment instante de referência; suspende quem tem carência vencida antes dele
     * @return a quantidade de clientes suspensos nesta execução
     */
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
