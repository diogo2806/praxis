import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";

export interface DirectCandidateLinkRequest {
  simulationId: string;
  candidateName: string;
  candidateEmail: string;
  accommodationTimeMultiplier?: number | null;
  applicationCycleId: string;
}

export interface DirectCandidateLinkResponse {
  attemptId: string;
  candidateUrl: string;
  simulationName: string;
}

export function createNewCandidateLink(body: DirectCandidateLinkRequest) {
  return request<DirectCandidateLinkResponse>("/api/v1/candidate-links", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function resendExistingCandidateLink(attemptId: string) {
  return request<DirectCandidateLinkResponse>(
    `/api/v1/candidate-links/${encodeURIComponent(attemptId)}/resend`,
    { method: "POST" },
  );
}

async function request<T>(path: string, init: RequestInit): Promise<T> {
  const session = getSession();
  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(session.token ? { Authorization: `Bearer ${session.token}` } : {}),
      ...init.headers,
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
      // Mantém a mensagem HTTP quando o corpo não é JSON.
    }
    throw new Error(message);
  }

  return response.json() as Promise<T>;
}
