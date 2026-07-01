package br.com.iforce.praxis.marketplace.dto;

import java.time.Instant;
import java.util.List;

public record MessageThreadResponse(
        Long id,
        Long listingId,
        Long professionalId,
        String requesterTenantId,
        Instant createdAt,
        List<MessageResponse> messages
) {
}
