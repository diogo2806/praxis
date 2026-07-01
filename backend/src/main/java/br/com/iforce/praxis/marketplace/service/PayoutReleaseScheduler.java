package br.com.iforce.praxis.marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "praxis.marketplace-payout-scheduler-enabled", havingValue = "true", matchIfMissing = true)
/**
 * Agenda a liberação periódica dos repasses que já cumpriram o prazo de retenção.
 *
 * <p>Na operação do marketplace, este componente automatiza uma tarefa recorrente para que
 * valores em escrow sejam promovidos a repasses liberados sem intervenção manual.</p>
 */
public class PayoutReleaseScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayoutReleaseScheduler.class);

    private final MarketplacePayoutService payoutService;

    public PayoutReleaseScheduler(MarketplacePayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @Scheduled(cron = "${praxis.marketplace-payout-release-cron:0 */15 * * * *}")
    /**
     * Executa a verificação automática dos repasses prontos para liberação.
     */
    public void releaseReadyEscrowPayouts() {
        int releasedCount = payoutService.releaseReadyEscrowPayouts();
        if (releasedCount > 0) {
            LOGGER.info("Liberados {} repasses marketplace em escrow.", releasedCount);
        }
    }
}
