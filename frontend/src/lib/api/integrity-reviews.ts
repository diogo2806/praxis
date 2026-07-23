import { apiRequest } from "@/lib/api/http";
import type { AttemptStatus } from "@/lib/api/praxis-legacy";

export type IntegrityReviewStatus = "PENDING" | "DECIDED";
export type IntegrityReviewDecision =
  | "NO_IMPACT"
  | "TECHNICAL_ISSUE_CONFIRMED"
  | "REAPPLICATION_RECOMMENDED"
  | "PRIVACY_COMPLIANCE_REVIEW";

export interface IntegrityReviewQueueItem {
  attemptId: string;
  candidateName: string;
  candidateEmail: string;
  attemptStatus: AttemptStatus;
  alertCount: number;
  reviewStatus: IntegrityReviewStatus;
  decision: IntegrityReviewDecision | null;
  updatedAt: string;
  evidenceDiscardedAt: string | null;
}

export interface IntegrityReviewPage {
  items: IntegrityReviewQueueItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface IntegrityReviewDetail extends IntegrityReviewQueueItem {
  justification: string | null;
  sharedWithCompany: boolean;
  reviewedBy: string | null;
  decidedAt: string | null;
  retentionUntil: string;
  alerts: Array<{
    code: string;
    title: string;
    explanation: string;
    occurrences: number;
  }>;
  sessions: Array<{
    id: string;
    status: "ACTIVE" | "CLOSED" | "EXPIRED";
    startedAt: string;
    lastHeartbeatAt: string;
    closedAt: string | null;
    userAgentCategory: string;
    inputMode: string;
  }>;
  events: Array<{
    id: number;
    sessionId: string;
    eventType: string;
    occurredAt: string;
    receivedAt: string;
    inputMode: string | null;
    visibilityState: string | null;
    sequenceNumber: number | null;
    detail: string | null;
  }>;
  auditTrail: Array<{
    id: number;
    action: "QUEUE_CREATED" | "EVIDENCE_ACCESSED" | "DECISION_RECORDED" | "EVIDENCE_DISCARDED";
    actorUserId: string;
    details: string | null;
    createdAt: string;
  }>;
}

export interface IntegrityReviewSharedStatus {
  decision: IntegrityReviewDecision;
  decidedAt: string;
}

export function listIntegrityReviews(page = 0, size = 25) {
  return apiRequest<IntegrityReviewPage>(
    `/api/v1/integrity-reviews?page=${Math.max(0, page)}&size=${Math.max(1, size)}`,
  );
}

export function getIntegrityReview(attemptId: string) {
  return apiRequest<IntegrityReviewDetail>(
    `/api/v1/integrity-reviews/${encodeURIComponent(attemptId)}`,
  );
}

export function decideIntegrityReview(
  attemptId: string,
  body: {
    decision: IntegrityReviewDecision;
    justification: string;
    shareWithCompany: boolean;
  },
) {
  return apiRequest<IntegrityReviewDetail>(
    `/api/v1/integrity-reviews/${encodeURIComponent(attemptId)}/decision`,
    {
      method: "POST",
      body: JSON.stringify(body),
    },
  );
}

export function getIntegrityReviewSharedStatus(attemptId: string) {
  return apiRequest<IntegrityReviewSharedStatus | undefined>(
    `/api/v1/results/${encodeURIComponent(attemptId)}/integrity-status`,
  );
}
