import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";
import { PraxisApiError } from "@/lib/api/praxis";

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

export async function getGupyHomologationStatus(): Promise<GupyHomologationResponse> {
  const session = getSession();
  const response = await fetch(`${getApiBaseUrl()}/api/v1/integrations/gupy/homologation`, {
    headers: {
      "Content-Type": "application/json",
      ...(session.token ? { Authorization: `Bearer ${session.token}` } : {}),
    },
  });

  if (!response.ok) {
    let message = `Falha na API (${response.status})`;
    try {
      const body = (await response.json()) as {
        mensagem?: string;
        message?: string;
        error?: string;
      };
      message = body.mensagem ?? body.message ?? body.error ?? message;
    } catch {
      // Mantém a mensagem HTTP quando a resposta não vem em JSON.
    }
    throw new PraxisApiError(message, response.status);
  }

  return response.json() as Promise<GupyHomologationResponse>;
}
