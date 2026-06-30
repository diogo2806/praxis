package br.com.iforce.praxis.shared.integration.dto;

import java.time.Instant;

public record GenerateIntegrationTokenResponse(
        String provider,
        String token,
        String tokenPreview,
        Instant createdAt
) {
}
