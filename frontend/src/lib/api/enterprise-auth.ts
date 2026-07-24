import { apiRequest } from "@/lib/api/http";

export type EnterpriseProtocol = "OIDC";

export interface IdentityProviderInput {
  displayName: string;
  protocol: EnterpriseProtocol;
  issuerUri: string;
  clientId: string;
  clientSecretEnvVar: string;
  redirectUri: string;
  frontendSuccessUri: string;
  allowedEmailDomains: string[];
  scopes: string;
  defaultRole: string;
  jitProvisioningEnabled: boolean;
  enforceSso: boolean;
  requireMfa: boolean;
  acceptedMfaAmrValues: string[];
  active: boolean;
}

export interface IdentityProvider extends IdentityProviderInput {
  id: string;
  lastTestStatus?: "SUCCESS" | "ERROR";
  lastTestMessage?: string;
  lastTestedAt?: string;
  updatedAt: string;
}

export interface EnterpriseDiscovery {
  ssoAvailable: boolean;
  ssoRequired: boolean;
  passwordLoginAllowed: boolean;
  providerId?: string;
  providerName?: string;
  protocol?: EnterpriseProtocol;
  startUrl?: string;
  mfaRequired: boolean;
  message: string;
}

export interface EnterpriseAuditEvent {
  id: string;
  eventType: string;
  outcome: string;
  actorIdentifier?: string;
  ipAddress?: string;
  userAgent?: string;
  detail?: string;
  occurredAt: string;
}

export function listIdentityProviders() {
  return apiRequest<IdentityProvider[]>("/api/v1/enterprise-auth/providers");
}

export function createIdentityProvider(input: IdentityProviderInput) {
  return apiRequest<IdentityProvider>("/api/v1/enterprise-auth/providers", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateIdentityProvider(id: string, input: IdentityProviderInput) {
  return apiRequest<IdentityProvider>(`/api/v1/enterprise-auth/providers/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function testIdentityProvider(id: string) {
  return apiRequest<{
    success: boolean;
    issuer: string;
    authorizationEndpoint?: string;
    tokenEndpoint?: string;
    jwksUri?: string;
    message: string;
    testedAt: string;
  }>(`/api/v1/enterprise-auth/providers/${id}/test`, { method: "POST" });
}

export function listEnterpriseAuthAuditEvents(limit = 100) {
  return apiRequest<EnterpriseAuditEvent[]>(`/api/v1/enterprise-auth/audit-events?limit=${limit}`);
}

export function discoverEnterpriseAccess(email: string) {
  return apiRequest<EnterpriseDiscovery>(
    `/api/v1/enterprise-auth/discovery?email=${encodeURIComponent(email)}`,
  );
}

export function startEnterpriseLogin(
  providerId: string,
  input: { returnUri: string; email?: string },
) {
  return apiRequest<{ authorizationUrl: string; expiresAt: string }>(
    `/api/v1/enterprise-auth/providers/${providerId}/start`,
    { method: "POST", body: JSON.stringify(input) },
  );
}
