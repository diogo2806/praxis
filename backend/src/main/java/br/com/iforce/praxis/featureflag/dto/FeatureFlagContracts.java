package br.com.iforce.praxis.featureflag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FeatureFlagContracts {

    private FeatureFlagContracts() {
    }

    @Schema(description = "Definição governada de uma feature flag")
    public record UpsertRequest(
            @NotBlank
            @Pattern(regexp = "[a-z][a-z0-9.-]{2,119}", message = "use chave minúscula com pontos, hífens ou números")
            String key,
            @NotBlank @Size(max = 1000) String description,
            @NotBlank @Size(max = 120) String owner,
            boolean defaultEnabled,
            Boolean globalOverride,
            boolean active,
            boolean killSwitch,
            boolean frontendExposed,
            boolean temporary,
            Instant expiresAt,
            @Size(max = 2000) String removalPlan,
            Set<String> environments,
            Set<String> companyIds,
            Set<String> plans,
            Set<String> userIds,
            Set<String> roles,
            @Min(0) @Max(100) int rolloutPercentage,
            boolean affectsScoring
    ) {
    }

    public record ToggleRequest(@NotNull Boolean enabled) {
    }

    public record EvaluationRequest(
            String companyId,
            String plan,
            String userId,
            Set<String> roles,
            String environment,
            String stableIdentifier
    ) {
    }

    public record EvaluationResponse(
            String key,
            boolean enabled,
            String reason,
            String variant,
            int rolloutBucket,
            Instant evaluatedAt
    ) {
    }

    public record Response(
            String id,
            String key,
            String description,
            String owner,
            boolean defaultEnabled,
            Boolean globalOverride,
            boolean active,
            boolean killSwitch,
            boolean frontendExposed,
            boolean temporary,
            Instant expiresAt,
            String removalPlan,
            Set<String> environments,
            Set<String> companyIds,
            Set<String> plans,
            Set<String> userIds,
            Set<String> roles,
            int rolloutPercentage,
            boolean affectsScoring,
            String status,
            String createdBy,
            String updatedBy,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
    }

    public record FrontendFlagsResponse(Map<String, Boolean> flags, Instant evaluatedAt) {
    }

    public record MetricRequest(
            @NotBlank String variant,
            @NotBlank String metric,
            double value
    ) {
    }

    public record MetricResponse(
            String flagKey,
            String variant,
            String metric,
            long sampleCount,
            double totalValue,
            double averageValue,
            Instant updatedAt
    ) {
    }

    public record GovernanceSummary(
            List<Response> flags,
            List<Response> expiredFlags,
            long activeCount,
            long killSwitchCount,
            Instant generatedAt
    ) {
    }
}
