import { apiRequest } from "@/lib/api/http";

const BASE = "/api/v1/participation-operations";

export type ParticipationRef = {
  type: "individual" | "journey";
  id: string;
};

export type SavedView = {
  id: string;
  ownerUserId: string;
  name: string;
  shared: boolean;
  filters: Record<string, unknown>;
  sort: Record<string, unknown>;
  columns: string[];
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type SavedViewRequest = {
  name: string;
  shared: boolean;
  filters: Record<string, unknown>;
  sort: Record<string, unknown>;
  columns: string[];
};

export type ParticipationTag = {
  id: string;
  name: string;
  color: string;
  description: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type BulkAction = "RESEND" | "EXTEND" | "CANCEL" | "ADD_TAG" | "REMOVE_TAG" | "EXPORT";
export type SelectionMode = "EXPLICIT" | "FILTER";

export type BulkFilter = {
  simulationId?: string;
  candidate?: string;
  processStatus?: string;
  linkStatus?: string;
  attention?: boolean;
};

export type BulkPreviewRequest = {
  action: BulkAction;
  selectionMode: SelectionMode;
  selected: ParticipationRef[];
  filter: BulkFilter;
  additionalDays?: number;
  tagId?: string;
  justification?: string;
};

export type BulkPreview = {
  selectedCount: number;
  eligibleCount: number;
  excludedCount: number;
  action: BulkAction;
  impact: string;
  excluded: Array<{
    participationType: string;
    participationId: string;
    reason: string;
  }>;
};

export type BulkJob = {
  id: string;
  action: BulkAction;
  selectionMode: SelectionMode;
  status: "PENDING" | "RUNNING" | "COMPLETED" | "COMPLETED_WITH_ERRORS" | "FAILED";
  totalItems: number;
  processedItems: number;
  succeededItems: number;
  skippedItems: number;
  failedItems: number;
  progressPercent: number;
  justification: string | null;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
  items: Array<{
    participationType: string;
    participationId: string;
    status: "PENDING" | "SUCCEEDED" | "SKIPPED" | "FAILED";
    reason: string | null;
    processedAt: string | null;
  }>;
};

export function listSavedViews() {
  return apiRequest<SavedView[]>(`${BASE}/views`);
}

export function createSavedView(request: SavedViewRequest) {
  return apiRequest<SavedView>(`${BASE}/views`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function updateSavedView(id: string, request: SavedViewRequest) {
  return apiRequest<SavedView>(`${BASE}/views/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export function deleteSavedView(id: string) {
  return apiRequest<void>(`${BASE}/views/${encodeURIComponent(id)}`, { method: "DELETE" });
}

export function listParticipationTags() {
  return apiRequest<ParticipationTag[]>(`${BASE}/tags`);
}

export function createParticipationTag(request: {
  name: string;
  color: string;
  description?: string;
}) {
  return apiRequest<ParticipationTag>(`${BASE}/tags`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function previewParticipationBulk(request: BulkPreviewRequest) {
  return apiRequest<BulkPreview>(`${BASE}/bulk/preview`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function createParticipationBulkJob(
  request: BulkPreviewRequest & { idempotencyKey: string },
) {
  return apiRequest<BulkJob>(`${BASE}/bulk/jobs`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function listParticipationBulkJobs() {
  return apiRequest<BulkJob[]>(`${BASE}/bulk/jobs`);
}

export function getParticipationBulkJob(id: string) {
  return apiRequest<BulkJob>(`${BASE}/bulk/jobs/${encodeURIComponent(id)}`);
}

export async function downloadParticipationBulkReport(id: string) {
  const csv = await apiRequest<string>(`${BASE}/bulk/jobs/${encodeURIComponent(id)}/report.csv`, {
    headers: { Accept: "text/csv" },
  });
  const blob = new Blob([csv], { type: "text/csv;charset=UTF-8" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `participation-bulk-${id}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}
