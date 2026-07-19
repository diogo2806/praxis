import { apiRequest } from "@/lib/api/http";

export type GupyHomologationStatus =
  | "BLOCKED"
  | "READY_FOR_EXTERNAL_VALIDATION"
  | "EVIDENCE_READY";

export type GupyHomologationCheckStatus = "OK" | "PENDING" | "BLOCKER";

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
    lastGupyAttemptAt: string | null;
    lastAuthenticatedRequestAt: string | null;
  };
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
