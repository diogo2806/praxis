package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.marketplace.model.PayoutStatus;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplacePayoutEntity;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplacePayoutRepository;
import br.com.iforce.praxis.shared.notification.model.InAppNotificationType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
/**
 * Administra a liberação dos repasses financeiros devidos aos profissionais.
 *
 * <p>No processo do marketplace, este serviço cuida do momento em que um valor deixa o
 * escrow administrativo e passa a ser considerado liberado para o vendedor.</p>
 */
public class MarketplacePayoutService {

    private final MarketplacePayoutRepository payoutRepository;
    private final MarketplaceNotificationService notificationService;

    public MarketplacePayoutService(
            MarketplacePayoutRepository payoutRepository,
            MarketplaceNotificationService notificationService
    ) {
        this.payoutRepository = payoutRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    /**
     * Libera todos os repasses cujo prazo de retenção já terminou.
     *
     * <p>Esse passo reduz risco operacional após a venda e formaliza que o pedido cumpriu o
     * período definido antes da disponibilização do valor ao profissional.</p>
     */
    public int releaseReadyEscrowPayouts() {
        List<MarketplacePayoutEntity> ready = payoutRepository.findByStatusAndEscrowReleaseAtBefore(
                PayoutStatus.ESCROW,
                Instant.now()
        );
        Instant releasedAt = Instant.now();
        for (MarketplacePayoutEntity payout : ready) {
            payout.setStatus(PayoutStatus.RELEASED);
            payout.setReleasedAt(releasedAt);
            notificationService.notifyProfessional(
                    payout.getProfessionalId(),
                    InAppNotificationType.MARKETPLACE_PAYOUT_RELEASED,
                    "Repasse marketplace liberado",
                    "Um repasse de marketplace saiu do escrow administrativo."
            );
        }
        return ready.size();
    }
}
