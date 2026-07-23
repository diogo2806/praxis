import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";

export type ReminderTargetState = "NOT_OPENED" | "NOT_STARTED" | "IN_PROGRESS";

export interface CampaignParticipantInput {
  rowNumber: number;
  name: string;
  email: string;
  consentConfirmed: boolean;
  accommodationMultiplier: number;
}

export interface CsvRowDiagnostic {
  rowNumber: number;
  name: string;
  email: string;
  valid: boolean;
  errors: string[];
  warnings: string[];
}

export interface CsvPreviewResponse {
  headers: string[];
  totalRows: number;
  validRows: number;
  invalidRows: number;
  availableCapacity: number;
  planLimitExceeded: boolean;
  rows: CsvRowDiagnostic[];
  validParticipants: CampaignParticipantInput[];
}

export interface ReminderRuleInput {
  reminderIndex: number;
  sendAfterHours: number;
  targetState: ReminderTargetState;
  subjectTemplate: string;
  bodyTemplate: string;
}

export interface CampaignParticipant {
  id: string;
  rowNumber: number;
  candidateName: string;
  maskedEmail: string;
  consentConfirmed: boolean;
  attemptId?: string | null;
  candidateUrl?: string | null;
  linkExpiresAt?: string | null;
  participationStatus: string;
  communicationStatus: string;
  lastError?: string | null;
  openedAt?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  expiredAt?: string | null;
  cancelledAt?: string | null;
}

export interface ParticipationCampaign {
  id: string;
  name: string;
  simulationId: string;
  applicationCycleId: string;
  applicationContext?: string | null;
  status: string;
  initialSendAt: string;
  linkValidityDays: number;
  consentRequired: boolean;
  allowExistingActive: boolean;
  retentionUntil: string;
  createdBy: string;
  createdAt: string;
  totals: {
    total: number;
    pending: number;
    delivered: number;
    failed: number;
    bounced: number;
    opened: number;
    notStarted: number;
    inProgress: number;
    completed: number;
    expired: number;
    cancelled: number;
  };
  participants: CampaignParticipant[];
  reminders: Array<Record<string, unknown>>;
}

export async function previewCampaignCsv(input: {
  simulationId: string;
  applicationCycleId: string;
  consentRequired: boolean;
  allowExistingActive: boolean;
  csvContent: string;
}) {
  return request<CsvPreviewResponse>("/api/v1/participation-campaigns/preview-csv", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function previewCampaignMessage(input: {
  subjectTemplate: string;
  bodyTemplate: string;
  sampleName: string;
  campaignName: string;
}) {
  return request<{ subject: string; body: string; variables: string[] }>(
    "/api/v1/participation-campaigns/preview-message",
    { method: "POST", body: JSON.stringify(input) },
  );
}

export async function createParticipationCampaign(input: {
  name: string;
  simulationId: string;
  applicationCycleId: string;
  applicationContext?: string;
  initialSendAt: string;
  linkValidityDays: number;
  consentRequired: boolean;
  allowExistingActive: boolean;
  subjectTemplate: string;
  bodyTemplate: string;
  retentionDays: number;
  participants: CampaignParticipantInput[];
  reminders: ReminderRuleInput[];
  idempotencyKey: string;
}) {
  return request<ParticipationCampaign>("/api/v1/participation-campaigns", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function listParticipationCampaigns() {
  return request<ParticipationCampaign[]>("/api/v1/participation-campaigns");
}

export async function campaignAction(campaignId: string, action: "pause" | "resume" | "cancel") {
  return request<ParticipationCampaign>(`/api/v1/participation-campaigns/${campaignId}/${action}`, {
    method: "POST",
  });
}

export async function downloadCampaignCsv(campaignId: string) {
  const response = await authorizedFetch(`/api/v1/participation-campaigns/${campaignId}/export.csv`);
  if (!response.ok) throw new Error(await readError(response));
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `campanha-${campaignId}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await authorizedFetch(path, init);
  if (!response.ok) throw new Error(await readError(response));
  return (await response.json()) as T;
}

function authorizedFetch(path: string, init?: RequestInit) {
  const token = getSession().token;
  const headers = new Headers(init?.headers);
  headers.set("Accept", "application/json");
  if (init?.body) headers.set("Content-Type", "application/json");
  if (token && token !== "praxis-security-disabled") headers.set("Authorization", `Bearer ${token}`);
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
