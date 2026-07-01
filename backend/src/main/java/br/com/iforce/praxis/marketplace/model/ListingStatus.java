package br.com.iforce.praxis.marketplace.model;

/**
 * Ciclo de vida de um anúncio (listing) de teste no marketplace.
 *
 * <p>{@code DRAFT} → {@code PENDING_REVIEW} (submetido pelo profissional) →
 * {@code APPROVED}/{@code REJECTED} (moderação). {@code SUSPENDED} retira da vitrine um
 * anúncio antes aprovado.</p>
 */
public enum ListingStatus {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    SUSPENDED
}
