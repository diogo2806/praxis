import { apiRequest } from "@/lib/api/http";

export type FeatureFlagStatus = "ACTIVE" | "INACTIVE" | "KILLED" | "EXPIRED";

export interface FeatureFlagResponse {
  id: string;
  key: string;
  description: string;
  owner: string;
  defaultEnabled: boolean;
  globalOverride: boolean | null;
  active: boolean;
  killSwitch: boolean;
  frontendExposed: boolean;
  temporary: boolean;
  expiresAt: string | null;
  removalPlan: string | null;
  environments: string[];
  companyIds: string[];
  plans: string[];
  userIds: string[];
  roles: string[];
  rolloutPercentage: number;
  affectsScoring: boolean;
  status: FeatureFlagStatus;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface FeatureFlagGovernanceSummary {
  flags: FeatureFlagResponse[];
  expiredFlags: FeatureFlagResponse[];
  activeCount: number;
  killSwitchCount: number;
  generatedAt: string;
}

export interface FeatureFlagUpsertRequest {
  key: string;
  description: string;
  owner: string;
  defaultEnabled: boolean;
  globalOverride: boolean | null;
  active: boolean;
  killSwitch: boolean;
  frontendExposed: boolean;
  temporary: boolean;
  expiresAt: string | null;
  removalPlan: string | null;
  environments: string[];
  companyIds: string[];
  plans: string[];
  userIds: string[];
  roles: string[];
  rolloutPercentage: number;
  affectsScoring: false;
}

export interface FeatureFlagEvaluationResponse {
  key: string;
  enabled: boolean;
  reason: string;
  variant: "ON" | "OFF";
  rolloutBucket: number;
  evaluatedAt: string;
}

export function getFeatureFlagGovernance(search = "", active?: boolean) {
  const params = new URLSearchParams();
  if (search.trim()) params.set("search", search.trim());
  if (active != null) params.set("active", String(active));
  const suffix = params.size > 0 ? `?${params.toString()}` : "";
  return apiRequest<FeatureFlagGovernanceSummary>(`/api/admin/feature-flags${suffix}`);
}

export function createFeatureFlag(request: FeatureFlagUpsertRequest) {
  return apiRequest<FeatureFlagResponse>("/api/admin/feature-flags", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function updateFeatureFlag(id: string, request: FeatureFlagUpsertRequest) {
  return apiRequest<FeatureFlagResponse>(`/api/admin/feature-flags/${id}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export function toggleFeatureFlag(id: string, enabled: boolean) {
  return apiRequest<FeatureFlagResponse>(`/api/admin/feature-flags/${id}/active`, {
    method: "POST",
    body: JSON.stringify({ enabled }),
  });
}

export function toggleFeatureFlagKillSwitch(id: string, enabled: boolean) {
  return apiRequest<FeatureFlagResponse>(`/api/admin/feature-flags/${id}/kill-switch`, {
    method: "POST",
    body: JSON.stringify({ enabled }),
  });
}

export function evaluateFeatureFlag(id: string, stableIdentifier: string, companyId?: string) {
  return apiRequest<FeatureFlagEvaluationResponse>(`/api/admin/feature-flags/${id}/evaluate`, {
    method: "POST",
    body: JSON.stringify({ stableIdentifier, companyId }),
  });
}
