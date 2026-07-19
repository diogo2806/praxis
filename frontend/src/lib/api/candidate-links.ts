import { apiRequest } from "@/lib/api/http";
import type { AttemptStatus } from "@/lib/api/praxis-legacy";

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
  | "RESENT_EXISTING_LINK"
  | "EXTENDED_LINK_VALIDITY";

export type CandidateLinkStatus = "active" | "expiringSoon" | "expired";

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
  linkIssuedAt: string;
  linkExpiresAt: string;
  remainingDays: number;
  linkStatus: CandidateLinkStatus;
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
  return apiRequest<CandidateLinkPageResponse>(
    `/api/v1/candidate-links/page?${params.toString()}`,
  );
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
  return apiRequest<CreateCandidateLinkResponse>("/api/v1/candidate-links", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function resendCandidateLink(attemptId: string) {
  return apiRequest<CreateCandidateLinkResponse>(
    `/api/v1/candidate-links/${encodeURIComponent(attemptId)}/resend`,
    { method: "POST" },
  );
}

export function extendCandidateLink(attemptId: string, additionalDays: number) {
  return apiRequest<CreateCandidateLinkResponse>(
    `/api/v1/candidate-links/${encodeURIComponent(attemptId)}/extend`,
    {
      method: "POST",
      body: JSON.stringify({ additionalDays }),
    },
  );
}
