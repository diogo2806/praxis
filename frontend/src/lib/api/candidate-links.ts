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

export interface CandidateLinkPageResponse {
  items: CandidateLinkResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CandidateLinkFilters {
  status?: AttemptStatus;
  simulationId?: string;
  versionNumber?: number;
  candidate?: string;
}

const CANDIDATE_LINK_PAGE_SIZE = 100;

export function searchCandidateLinks(
  page: number,
  blind = false,
  filters: CandidateLinkFilters = {},
) {
  const params = new URLSearchParams();
  params.set("page", String(Math.max(0, page)));
  params.set("size", String(CANDIDATE_LINK_PAGE_SIZE));
  params.set("blind", String(blind));
  if (filters.status) params.set("status", filters.status);
  if (filters.simulationId) params.set("simulationId", filters.simulationId);
  if (filters.versionNumber != null) params.set("versionNumber", String(filters.versionNumber));
  if (filters.candidate?.trim()) params.set("candidate", filters.candidate.trim());
  return request<CandidateLinkPageResponse>(`/api/v1/candidate-links/page?${params.toString()}`);
}

export async function listCandidateLinks(
  blind = false,
  filters: CandidateLinkFilters = {},
): Promise<CandidateLinkResponse[]> {
  const firstPage = await searchCandidateLinks(0, blind, filters);
  const linksByAttempt = new Map(
    firstPage.items.map((item) => [item.attemptId, item] as const),
  );

  for (let page = 1; page < firstPage.totalPages; page += 1) {
    const response = await searchCandidateLinks(page, blind, filters);
    for (const item of response.items) {
      linksByAttempt.set(item.attemptId, item);
    }
  }

  return Array.from(linksByAttempt.values()).sort(
    (left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt),
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
