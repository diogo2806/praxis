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

type CandidateLinkPayload = Omit<
  CandidateLinkResponse,
  "linkExpiresAt" | "remainingDays" | "linkStatus"
> & {
  linkExpiresAt?: string | null;
  remainingDays?: number | null;
  linkStatus?: string | null;
};

type CandidateLinkPagePayload = Omit<CandidateLinkPageResponse, "items"> & {
  items: CandidateLinkPayload[];
};

const CANDIDATE_LINK_PAGE_SIZE = 100;
const LEGACY_LINK_TTL_MILLISECONDS = 7 * 24 * 60 * 60 * 1000;

export async function searchCandidateLinks(
  page: number,
  blind = false,
  filters: CandidateLinkFilters = {},
): Promise<CandidateLinkPageResponse> {
  const params = new URLSearchParams();
  params.set("page", String(Math.max(0, page)));
  params.set("size", String(CANDIDATE_LINK_PAGE_SIZE));
  params.set("blind", String(blind));
  if (filters.status) params.set("status", filters.status);
  if (filters.simulationId) params.set("simulationId", filters.simulationId);
  if (filters.versionNumber != null) params.set("versionNumber", String(filters.versionNumber));
  if (filters.candidate?.trim()) params.set("candidate", filters.candidate.trim());

  const response = await apiRequest<CandidateLinkPagePayload>(
    `/api/v1/candidate-links/page?${params.toString()}`,
  );

  return {
    ...response,
    items: response.items.map(normalizeCandidateLink),
  };
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

function normalizeCandidateLink(link: CandidateLinkPayload): CandidateLinkResponse {
  const linkExpiresAt = normalizeLinkExpiresAt(link.linkExpiresAt, link.createdAt);
  const remainingDays = normalizeRemainingDays(link.remainingDays, linkExpiresAt);

  return {
    ...link,
    linkExpiresAt,
    remainingDays,
    linkStatus: normalizeLinkStatus(link.linkStatus, remainingDays),
  };
}

function normalizeLinkExpiresAt(value: string | null | undefined, createdAt: string): string {
  if (value && Number.isFinite(Date.parse(value))) {
    return value;
  }

  const createdAtMilliseconds = Date.parse(createdAt);
  if (Number.isFinite(createdAtMilliseconds)) {
    return new Date(createdAtMilliseconds + LEGACY_LINK_TTL_MILLISECONDS).toISOString();
  }

  return new Date(0).toISOString();
}

function normalizeRemainingDays(value: number | null | undefined, linkExpiresAt: string): number {
  if (typeof value === "number" && Number.isFinite(value) && value >= 0) {
    return Math.floor(value);
  }

  const expiresAtMilliseconds = Date.parse(linkExpiresAt);
  if (!Number.isFinite(expiresAtMilliseconds)) {
    return 0;
  }

  const remainingMilliseconds = expiresAtMilliseconds - Date.now();
  if (remainingMilliseconds <= 0) {
    return 0;
  }

  return Math.max(1, Math.ceil(remainingMilliseconds / (24 * 60 * 60 * 1000)));
}

function normalizeLinkStatus(
  value: string | null | undefined,
  remainingDays: number,
): CandidateLinkStatus {
  const normalizedValue = value?.replace(/[\s_-]/g, "").toLowerCase();

  if (normalizedValue === "active") {
    return "active";
  }
  if (normalizedValue === "expiringsoon") {
    return "expiringSoon";
  }
  if (normalizedValue === "expired") {
    return "expired";
  }

  if (remainingDays === 0) {
    return "expired";
  }
  return remainingDays <= 3 ? "expiringSoon" : "active";
}
