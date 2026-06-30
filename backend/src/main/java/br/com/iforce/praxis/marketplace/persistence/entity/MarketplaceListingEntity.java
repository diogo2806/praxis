package br.com.iforce.praxis.marketplace.persistence.entity;

import br.com.iforce.praxis.marketplace.model.ListingCategory;
import br.com.iforce.praxis.marketplace.model.ListingStatus;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Anúncio de um teste à venda no marketplace.
 *
 * <p>Referencia uma {@code SimulationVersionEntity} já existente (conteúdo calibrado) em vez de
 * duplicar o domínio de simulação. Note os tipos das FKs: {@code source_simulation_id} é
 * {@code VARCHAR(120)} (PK de {@code simulations}) e {@code source_version_id} é {@code BIGINT}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "marketplace_listings")
public class MarketplaceListingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    /** FK para {@code simulations(id)} — VARCHAR. */
    @Column(name = "source_simulation_id", nullable = false, length = 120)
    private String sourceSimulationId;

    /** FK para {@code simulation_versions(id)} — BIGINT. */
    @Column(name = "source_version_id", nullable = false)
    private Long sourceVersionId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 60)
    private ListingCategory category;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    /** Ids de nós (PK BIGINT de {@code simulation_nodes}) liberados para preview público. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "marketplace_listing_preview_nodes",
            joinColumns = @JoinColumn(name = "listing_id")
    )
    @Column(name = "node_id", nullable = false)
    private Set<Long> previewNodeIds = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ListingStatus status = ListingStatus.DRAFT;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
