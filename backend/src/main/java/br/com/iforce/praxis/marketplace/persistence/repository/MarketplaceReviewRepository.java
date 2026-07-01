package br.com.iforce.praxis.marketplace.persistence.repository;

import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceReviewEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketplaceReviewRepository extends JpaRepository<MarketplaceReviewEntity, Long> {

    Optional<MarketplaceReviewEntity> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    List<MarketplaceReviewEntity> findByListingIdOrderByCreatedAtDesc(Long listingId);

    List<MarketplaceReviewEntity> findByProfessionalIdOrderByCreatedAtDesc(Long professionalId);
}
