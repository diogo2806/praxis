package br.com.iforce.praxis.marketplace.persistence.repository;

import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceMessageThreadEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketplaceMessageThreadRepository extends JpaRepository<MarketplaceMessageThreadEntity, Long> {

    Optional<MarketplaceMessageThreadEntity> findByListingIdAndRequesterTenantId(
            Long listingId,
            String requesterTenantId
    );

    List<MarketplaceMessageThreadEntity> findByRequesterTenantIdOrderByCreatedAtDesc(String requesterTenantId);

    List<MarketplaceMessageThreadEntity> findByProfessionalIdOrderByCreatedAtDesc(Long professionalId);
}
