package br.com.iforce.praxis.shared.integration.dto;

import java.util.Map;

public record ConfigureIntegrationRequest(
        Map<String, Object> credentials,
        Map<String, Object> settings
) {
}
