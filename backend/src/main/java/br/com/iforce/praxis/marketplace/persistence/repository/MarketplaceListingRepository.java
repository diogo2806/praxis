package br.com.iforce.praxis.marketplace.persistence.repository;

import br.com.iforce.praxis.marketplace.model.ListingCategory;
import br.com.iforce.praxis.marketplace.model.ListingStatus;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceListingEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListingEntity, Long> {

    List<MarketplaceListingEntity> findByProfessionalIdOrderByCreatedAtDesc(Long professionalId);

    /** Fila de moderação: anúncios por status. */
    List<MarketplaceListingEntity> findByStatusOrderByCreatedAtAsc(ListingStatus status);

    Optional<MarketplaceListingEntity> findByIdAndStatus(Long id, ListingStatus status);

    /**
     * Busca da vitrine pública. Filtra por status, categoria opcional, faixa de preço opcional
     * e texto opcional (título/descrição). Ordenação fica a cargo do {@link Pageable}.
     */
    @Query("""
            SELECT l FROM MarketplaceListingEntity l, MarketplaceProfessionalEntity p
            WHERE l.status = :status
              AND p.id = l.professionalId
              AND (:category IS NULL OR l.category = :category)
              AND (:minPriceCents IS NULL OR l.priceCents >= :minPriceCents)
              AND (:maxPriceCents IS NULL OR l.priceCents <= :maxPriceCents)
              AND (:minRating IS NULL OR p.averageRating >= :minRating)
              AND (:text IS NULL
                   OR LOWER(l.title) LIKE LOWER(CONCAT('%', :text, '%'))
                   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :text, '%')))
            """)
    Page<MarketplaceListingEntity> search(
            @Param("status") ListingStatus status,
            @Param("category") ListingCategory category,
            @Param("minPriceCents") Long minPriceCents,
            @Param("maxPriceCents") Long maxPriceCents,
            @Param("minRating") BigDecimal minRating,
            @Param("text") String text,
            Pageable pageable);
}
