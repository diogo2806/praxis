package br.com.iforce.praxis.shared.integration.dto;

import br.com.iforce.praxis.shared.integration.model.IntegrationAction;

import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;

import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;

import br.com.iforce.praxis.shared.integration.model.IntegrationType;


import java.time.Instant;

import java.util.List;


public record IntegrationResponse(
        IntegrationProvider provider,
        String name,
        String description,
        IntegrationType type,
        IntegrationStatus status,
        Instant lastSyncAt,
        Instant configuredAt,
        String errorMessage,
        String tokenPreview,
        List<IntegrationAction> availableActions
) {
}
