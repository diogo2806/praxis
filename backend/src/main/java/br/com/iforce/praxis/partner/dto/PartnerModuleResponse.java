package br.com.iforce.praxis.partner.dto;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;

import java.time.Instant;

public final class PartnerModuleResponse {

    private PartnerModuleResponse() {
    }

    public record Specialist(
            Long id,
            String name,
            String email,
            UserStatus status,
            Instant createdAt
    ) {
    }

    public record Client(
            String id,
            String name,
            String externalCompanyId,
            IntegrationProvider provider,
            boolean active,
            boolean tokenConfigured,
            long assignedTests,
            Instant createdAt
    ) {
    }

    public record CatalogItem(
            String simulationId,
            String name,
            String description,
            boolean assigned
    ) {
    }

    public record Token(
            String clientId,
            IntegrationProvider provider,
            String token,
            Instant createdAt
    ) {
    }
}
