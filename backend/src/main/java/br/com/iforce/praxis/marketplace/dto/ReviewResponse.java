package br.com.iforce.praxis.marketplace.dto;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long orderId,
        Long listingId,
        Long professionalId,
        short rating,
        String comment,
        Instant createdAt
) {
}
