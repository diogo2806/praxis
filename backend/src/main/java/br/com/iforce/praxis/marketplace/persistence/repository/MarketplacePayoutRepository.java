package br.com.iforce.praxis.marketplace.persistence.repository;

import br.com.iforce.praxis.marketplace.model.PayoutStatus;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplacePayoutEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MarketplacePayoutRepository extends JpaRepository<MarketplacePayoutEntity, Long> {

    List<MarketplacePayoutEntity> findByProfessionalIdOrderByCreatedAtDesc(Long professionalId);

    Optional<MarketplacePayoutEntity> findByOrderId(Long orderId);

    List<MarketplacePayoutEntity> findByStatusAndEscrowReleaseAtBefore(
            PayoutStatus status,
            Instant escrowReleaseAt
    );
}
