import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";

export type AnswerKeyAssignmentRole = "EXPERT" | "APPROVER";

export interface AnswerKeyRound {
  id: string;
  simulationId: string;
  versionNumber: number;
  roundNumber: number;
  status: "DRAFT" | "IN_REVIEW" | "CHANGES_REQUESTED" | "APPROVED";
  minimumExperts: number;
  minimumConsensus: number;
  createdBy: string;
  createdAt: string;
  approvedBy?: string | null;
  approvedAt?: string | null;
}

export interface AnswerKeyAssignment {
  userId: string;
  role: AnswerKeyAssignmentRole;
  status: "INVITED" | "IN_PROGRESS" | "SUBMITTED" | "APPROVED";
  invitedAt: string;
  submittedAt?: string | null;
}

export interface AnswerKeyEvidence {
  nodeId: string;
  task: string;
  risk: string;
  competency: string;
  indicator: string;
  weight: number;
  updatedBy: string;
  updatedAt: string;
}

export interface AnswerKeyOptionConsensus {
  nodeId: string;
  optionId: string;
  optionText: string;
  reviewCount: number;
  averageScore: number;
  dispersion: number;
  consensus: number;
  status: "READY" | "MISSING_REVIEWS" | "LOW_CONSENSUS" | "AMBIGUOUS";
  behavioralJustifications: string[];
}

export interface AnswerKeyReviewEvent {
  eventType: string;
  actorUserId: string;
  eventDataJson: string;
  occurredAt: string;
}

export interface AnswerKeyReviewSummary {
  round: AnswerKeyRound;
  approvable: boolean;
  expectedOptions: number;
  reviewedOptions: number;
  submittedExperts: number;
  blockers: string[];
  warnings: string[];
  assignments: AnswerKeyAssignment[];
  evidence: AnswerKeyEvidence[];
  options: AnswerKeyOptionConsensus[];
  history: AnswerKeyReviewEvent[];
}

export interface CreateAnswerKeyRoundInput {
  minimumExperts: number;
  minimumConsensus: number;
}

export interface AnswerKeyEvidenceInput {
  task: string;
  risk: string;
  competency: string;
  indicator: string;
  weight: number;
}

export interface AnswerKeyOptionReviewInput {
  effectivenessScore: number;
  behavioralJustification: string;
  competencyScores: Record<string, number>;
}

function basePath(simulationId: string, versionNumber: number) {
  return `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/answer-key-review`;
}

export async function createAnswerKeyRound(
  simulationId: string,
  versionNumber: number,
  input: CreateAnswerKeyRoundInput,
) {
  return request<AnswerKeyRound>(`${basePath(simulationId, versionNumber)}/rounds`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function getLatestAnswerKeyReview(simulationId: string, versionNumber: number) {
  return request<AnswerKeyReviewSummary>(
    `${basePath(simulationId, versionNumber)}/rounds/latest`,
  );
}

export async function inviteAnswerKeyReviewer(
  simulationId: string,
  versionNumber: number,
  roundId: string,
  userId: string,
  role: AnswerKeyAssignmentRole,
) {
  return request<AnswerKeyReviewSummary>(
    `${basePath(simulationId, versionNumber)}/rounds/${roundId}/assignments`,
    {
      method: "POST",
      body: JSON.stringify({ userId, role }),
    },
  );
}

export async function saveAnswerKeyEvidence(
  simulationId: string,
  versionNumber: number,
  roundId: string,
  nodeId: string,
  input: AnswerKeyEvidenceInput,
) {
  return request<AnswerKeyReviewSummary>(
    `${basePath(simulationId, versionNumber)}/rounds/${roundId}/evidence/${encodeURIComponent(nodeId)}`,
    {
      method: "PUT",
      body: JSON.stringify(input),
    },
  );
}

export async function saveAnswerKeyOptionReview(
  simulationId: string,
  versionNumber: number,
  roundId: string,
  nodeId: string,
  optionId: string,
  input: AnswerKeyOptionReviewInput,
) {
  return request<AnswerKeyReviewSummary>(
    `${basePath(simulationId, versionNumber)}/rounds/${roundId}/options/${encodeURIComponent(nodeId)}/${encodeURIComponent(optionId)}/reviews/me`,
    {
      method: "PUT",
      body: JSON.stringify(input),
    },
  );
}

export async function submitAnswerKeyReview(
  simulationId: string,
  versionNumber: number,
  roundId: string,
) {
  return request<AnswerKeyReviewSummary>(
    `${basePath(simulationId, versionNumber)}/rounds/${roundId}/submit`,
    { method: "POST" },
  );
}

export async function approveAnswerKeyReview(
  simulationId: string,
  versionNumber: number,
  roundId: string,
) {
  return request<AnswerKeyReviewSummary>(
    `${basePath(simulationId, versionNumber)}/rounds/${roundId}/approve`,
    { method: "POST" },
  );
}

export async function downloadAnswerKeyTechnicalReport(
  simulationId: string,
  versionNumber: number,
  roundId: string,
) {
  const response = await authorizedFetch(
    `${basePath(simulationId, versionNumber)}/rounds/${roundId}/report.csv`,
  );
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `gabarito-${simulationId}-v${versionNumber}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await authorizedFetch(path, init);
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return (await response.json()) as T;
}

function authorizedFetch(path: string, init?: RequestInit) {
  const token = getSession().token;
  const headers = new Headers(init?.headers);
  headers.set("Accept", "application/json");
  if (init?.body) headers.set("Content-Type", "application/json");
  if (token && token !== "praxis-security-disabled") {
    headers.set("Authorization", `Bearer ${token}`);
  }
  return fetch(`${getApiBaseUrl()}${path}`, { ...init, headers });
}

async function readError(response: Response) {
  const text = await response.text();
  if (!text) return `Falha HTTP ${response.status}.`;
  try {
    const payload = JSON.parse(text) as { message?: string; detail?: string };
    return payload.message ?? payload.detail ?? text;
  } catch {
    return text;
  }
}
