package br.com.iforce.praxis.enterpriseauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class EnterpriseAuthDtos {

    private EnterpriseAuthDtos() {
    }

    public enum Protocol {
        OIDC
    }

    public record SaveIdentityProviderRequest(
            @NotBlank @Size(max = 160) String displayName,
            @NotNull Protocol protocol,
            @NotBlank @Size(max = 1000) String issuerUri,
            @NotBlank @Size(max = 500) String clientId,
            @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,159}$") String clientSecretEnvVar,
            @NotBlank @Size(max = 1000) String redirectUri,
            @NotBlank @Size(max = 1000) String frontendSuccessUri,
            @NotEmpty List<@NotBlank @Pattern(regexp = "^[A-Za-z0-9.-]+$") String> allowedEmailDomains,
            @NotBlank @Size(max = 500) String scopes,
            @NotBlank @Size(max = 80) String defaultRole,
            boolean jitProvisioningEnabled,
            boolean enforceSso,
            boolean requireMfa,
            List<@NotBlank @Size(max = 40) String> acceptedMfaAmrValues,
            boolean active
    ) {
    }

    public record IdentityProviderResponse(
            String id,
            String displayName,
            Protocol protocol,
            String issuerUri,
            String clientId,
            String clientSecretEnvVar,
            String redirectUri,
            String frontendSuccessUri,
            List<String> allowedEmailDomains,
            String scopes,
            String defaultRole,
            boolean jitProvisioningEnabled,
            boolean enforceSso,
            boolean requireMfa,
            List<String> acceptedMfaAmrValues,
            boolean active,
            String lastTestStatus,
            String lastTestMessage,
            Instant lastTestedAt,
            Instant updatedAt
    ) {
    }

    public record DiscoveryResponse(
            boolean ssoAvailable,
            boolean ssoRequired,
            boolean passwordLoginAllowed,
            String providerId,
            String providerName,
            Protocol protocol,
            String startUrl,
            boolean mfaRequired,
            String message
    ) {
    }

    public record StartLoginRequest(
            @NotBlank @Size(max = 1000) String returnUri,
            @Size(max = 320) String email
    ) {
    }

    public record StartLoginResponse(
            String authorizationUrl,
            Instant expiresAt
    ) {
    }

    public record CallbackResponse(
            String token,
            String tokenType,
            long expiresInSeconds,
            String email,
            String displayName,
            String empresaId,
            List<String> roles,
            boolean mfaVerified,
            String returnUri
    ) {
    }

    public record ProviderTestResponse(
            boolean success,
            String issuer,
            String authorizationEndpoint,
            String tokenEndpoint,
            String jwksUri,
            String message,
            Instant testedAt
    ) {
    }

    public record AuditEventResponse(
            String id,
            String eventType,
            String outcome,
            String actorIdentifier,
            String ipAddress,
            String userAgent,
            String detail,
            Instant occurredAt
    ) {
    }
}
