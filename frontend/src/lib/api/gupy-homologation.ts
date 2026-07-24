import { apiRequest } from "@/lib/api/http";

export type GupyHomologationStatus =
  | "BLOCKED"
  | "READY_FOR_EXTERNAL_VALIDATION"
  | "EVIDENCE_READY"
  | "HOMOLOGATED";

export type GupyHomologationCheckStatus = "OK" | "PENDING" | "BLOCKER";

export interface GupyHomologationExternalEvidence {
  callbackConfirmed: boolean;
  callbackConfirmedAt: string | null;
  callbackConfirmedBy: string | null;
  resultPagesConfirmed: boolean;
  resultPagesConfirmedAt: string | null;
  resultPagesConfirmedBy: string | null;
  gupyApproved: boolean;
  gupyApprovedAt: string | null;
  gupyApprovedBy: string | null;
  clientApproved: boolean;
  clientApprovedAt: string | null;
  clientApprovedBy: string | null;
  notes: string | null;
}

export interface GupyHomologationEvidenceRequest {
  callbackConfirmed: boolean;
  resultPagesConfirmed: boolean;
  gupyApproved: boolean;
  clientApproved: boolean;
  notes: string;
}

export interface GupyHomologationResponse {
  status: GupyHomologationStatus;
  readinessPercent: number;
  externalApprovalRequired: boolean;
  publicBaseUrl: string;
  generatedAt: string;
  metrics: {
    publishedTests: number;
    gupyAttempts: number;
    completedGupyAttempts: number;
    attemptsWithResultWebhook: number;
    sentResultWebhooks: number;
    resultWebhooksInDlq: number;
    resultEndpointQueries: number;
    validPercentageResults: number;
    lastGupyAttemptAt: string | null;
    lastAuthenticatedRequestAt: string | null;
  };
  externalEvidence: GupyHomologationExternalEvidence;
  endpoints: Array<{
    method: string;
    url: string;
    purpose: string;
  }>;
  checks: Array<{
    code: string;
    title: string;
    status: GupyHomologationCheckStatus;
    detail: string;
    external: boolean;
  }>;
}

export function getGupyHomologationStatus(): Promise<GupyHomologationResponse> {
  return apiRequest<GupyHomologationResponse>(
    "/api/v1/integrations/gupy/homologation",
  );
}

export function updateGupyHomologationEvidence(
  request: GupyHomologationEvidenceRequest,
): Promise<GupyHomologationResponse> {
  return apiRequest<GupyHomologationResponse>(
    "/api/v1/integrations/gupy/homologation/evidence",
    {
      method: "PUT",
      body: JSON.stringify(request),
    },
  );
}
