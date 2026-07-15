import { getApiBaseUrl } from "@/lib/runtime-config";
import type { AttemptStatus } from "@/lib/api/praxis-legacy";
import { PraxisApiError } from "@/lib/api/praxis-legacy";

export interface CreateCandidateLinkRequest {
  simulationId: string;
  candidateName: string;
  candidateEmail: string;
  applicationCycleId: string;
  applicationContext?: string | null;
  accommodationTimeMultiplier?: number | null;
}

export type CandidateLinkOperation =
  | "CREATED_NEW_APPLICATION"
  | "REUSED_IDEMPOTENT_REQUEST"
  | "RESENT_EXISTING_LINK";

export interface CreateCandidateLinkResponse {
  attemptId: string;
  candidateUrl: string;
  simulationName: string;
  reused: boolean;
  operation: CandidateLinkOperation;
}

export interface CandidateLinkResponse {
  attemptId: string;
  candidateUrl: string;
  candidateName: string;
  candidateEmail: string | null;
  simulationId: string;
  simulationName: string;
  status: AttemptStatus;
  createdAt: string;
}

export function listCandidateLinks(blind = false) {
  return request<CandidateLinkResponse[]>(
    `/api/v1/candidate-links${blind ? "?blind=true" : ""}`,
  );
}

export function createCandidateLink(body: CreateCandidateLinkRequest) {
  return request<CreateCandidateLinkResponse>("/api/v1/candidate-links", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function resendCandidateLink(attemptId: string) {
  return request<CreateCandidateLinkResponse>(
    `/api/v1/candidate-links/${encodeURIComponent(attemptId)}/resend`,
    { method: "POST" },
  );
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
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

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
