package br.com.iforce.praxis.shared.integration.dto;

import java.time.Instant;


public record RotateIntegrationTokenResponse(
        String provider,
        boolean configured,
        Instant createdAt,
        String token
) {
}
