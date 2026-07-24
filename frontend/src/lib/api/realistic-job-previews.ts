import { apiRequest } from "@/lib/api/http";

export type PreviewScopeType = "JOB" | "ROLE" | "JOURNEY";
export type PreviewDisplayPosition = "BEFORE" | "AFTER" | "BOTH";
export type PreviewDisplayStage = "BEFORE" | "AFTER";
export type PreviewMediaType = "IMAGE" | "AUDIO" | "VIDEO";

export interface PreviewMediaItem {
  type: PreviewMediaType;
  url: string;
  alternativeText: string;
  transcriptUrl?: string;
}

export interface PreviewContent {
  responsibilities: string;
  autonomy: string;
  pressureContext: string;
  contactFrequency: string;
  criticalSituations: string;
  routineDescription: string;
  workConditions: string;
  positiveAspects: string;
  alternativeText: string;
  media: PreviewMediaItem[];
  scenarioNodeIds: string[];
}

export interface CreatePreviewInput {
  scopeType: PreviewScopeType;
  scopeKey: string;
  title: string;
  displayPosition: PreviewDisplayPosition;
  acknowledgementRequired: boolean;
  content: PreviewContent;
}

export interface PreviewSummary {
  id: string;
  scopeType: PreviewScopeType;
  scopeKey: string;
  title: string;
  displayPosition: PreviewDisplayPosition;
  acknowledgementRequired: boolean;
  activeVersionNumber: number | null;
  draftVersionNumber: number | null;
  updatedAt: string;
}

export interface PreviewVersion {
  previewId: string;
  versionId: string;
  versionNumber: number;
  status: string;
  title: string;
  displayPosition: PreviewDisplayPosition;
  acknowledgementRequired: boolean;
  content: PreviewContent;
  publishedAt: string | null;
}

export interface CandidatePreview {
  previewId: string;
  versionId: string;
  versionNumber: number;
  title: string;
  displayStage: PreviewDisplayStage;
  acknowledgementRequired: boolean;
  content: PreviewContent;
  informationalNotice: string;
}

export interface PreviewMetrics {
  previewId: string;
  versionNumber: number;
  presentations: number;
  acknowledgements: number;
  voluntaryWithdrawals: number;
  acknowledgementRatePercent: number;
  withdrawalRatePercent: number;
  averageClarity: number | null;
  averageRealism: number | null;
  averageExpectationCompatibility: number | null;
  sampleSuppressed: boolean;
  minimumSample: number;
  privacyNotice: string;
}

export function listRealisticJobPreviews() {
  return apiRequest<PreviewSummary[]>("/api/v1/realistic-job-previews");
}

export function createRealisticJobPreview(input: CreatePreviewInput) {
  return apiRequest<PreviewVersion>("/api/v1/realistic-job-previews", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function getRealisticJobPreviewDraft(previewId: string) {
  return apiRequest<PreviewVersion>(`/api/v1/realistic-job-previews/${previewId}/draft`);
}

export function updateRealisticJobPreviewDraft(previewId: string, input: Omit<CreatePreviewInput, "scopeType" | "scopeKey">) {
  return apiRequest<PreviewVersion>(`/api/v1/realistic-job-previews/${previewId}/draft`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function publishRealisticJobPreview(previewId: string) {
  return apiRequest<PreviewVersion>(`/api/v1/realistic-job-previews/${previewId}/publish`, { method: "POST" });
}

export function getRealisticJobPreviewMetrics(previewId: string, versionNumber?: number) {
  const query = versionNumber === undefined ? "" : `?versionNumber=${versionNumber}`;
  return apiRequest<PreviewMetrics>(`/api/v1/realistic-job-previews/${previewId}/metrics${query}`);
}

export function presentCandidateRealisticPreview(token: string, stage: PreviewDisplayStage) {
  return apiRequest<CandidatePreview>(
    `/api/v1/candidate/${encodeURIComponent(token)}/realistic-preview/present?stage=${stage}`,
    { method: "POST" },
  );
}

export function reactToCandidateRealisticPreview(
  token: string,
  versionId: string,
  stage: PreviewDisplayStage,
  input: {
    acknowledged: boolean;
    voluntaryWithdrawal: boolean;
    clarityScore?: number;
    realismScore?: number;
    expectationCompatibilityScore?: number;
  },
) {
  return apiRequest<void>(
    `/api/v1/candidate/${encodeURIComponent(token)}/realistic-preview/${versionId}/reaction?stage=${stage}`,
    { method: "POST", body: JSON.stringify(input) },
  );
}
