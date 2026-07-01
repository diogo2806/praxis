package br.com.iforce.praxis.shared.integration.dto;

import java.time.Instant;


public record IntegrationTokenResponse(
        String provider,
        boolean configured,
        Instant createdAt
) {
}
