package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.PayoutStatus;

import java.time.Instant;

public record PayoutSummaryResponse(
        Long id,
        Long orderId,
        String listingTitle,
        long amountCents,
        PayoutStatus status,
        Instant escrowReleaseAt,
        Instant releasedAt,
        Instant createdAt
) {
}
