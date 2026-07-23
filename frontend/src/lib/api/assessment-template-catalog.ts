import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";

export type TemplateScope = "INTERNAL" | "SHARED" | "OFFICIAL";
export type TemplateStatus = "DRAFT" | "IN_REVIEW" | "APPROVED" | "REJECTED" | "ARCHIVED";

export interface AssessmentTemplatePreview {
  scenarioCount: number;
  terminalCount: number;
  optionCount: number;
  durationMinutes: number;
  competencyCoverage: string[];
  accessibilityRequirements: string[];
  rootNodeId: string;
}

export interface AssessmentTemplate {
  id: string;
  ownerEmpresaId: string;
  sourceEmpresaId: string;
  sourceSimulationId: string;
  sourceVersionNumber: number;
  templateVersion: number;
  scope: TemplateScope;
  status: TemplateStatus;
  title: string;
  summary: string;
  jobRole: string;
  businessArea: string;
  seniority: string;
  sector: string;
  durationMinutes: number;
  languageCode: string;
  complexity: string;
  methodologyEvidence: string;
  usageLimitations: string;
  authorUserId: string;
  reviewedBy?: string | null;
  reviewNote?: string | null;
  reviewedAt?: string | null;
  publishedAt?: string | null;
  favorite: boolean;
  preview: AssessmentTemplatePreview;
}

export interface TemplateFilters {
  query?: string;
  jobRole?: string;
  businessArea?: string;
  seniority?: string;
  sector?: string;
  competency?: string;
  languageCode?: string;
  complexity?: string;
  favoriteOnly?: boolean;
}

export interface CreateTemplateInput {
  sourceSimulationId: string;
  sourceVersionNumber: number;
  scope: TemplateScope;
  title: string;
  summary: string;
  jobRole: string;
  businessArea: string;
  seniority: string;
  sector: string;
  durationMinutes: number;
  languageCode: string;
  complexity: string;
  methodologyEvidence: string;
  usageLimitations: string;
  competencies: string[];
}

export interface InstantiateTemplateResponse {
  templateId: string;
  templateVersion: number;
  simulationId: string;
  versionNumber: number;
  status: string;
  sourceSimulationId: string;
  sourceVersionNumber: number;
}

export async function listAssessmentTemplates(filters: TemplateFilters = {}) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") params.set(key, String(value));
  });
  const suffix = params.size > 0 ? `?${params.toString()}` : "";
  return request<AssessmentTemplate[]>(`/api/v1/assessment-templates${suffix}`);
}

export async function createAssessmentTemplate(input: CreateTemplateInput) {
  return request<AssessmentTemplate>("/api/v1/assessment-templates", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function submitAssessmentTemplate(templateId: string) {
  return request<AssessmentTemplate>(`/api/v1/assessment-templates/${templateId}/submit`, {
    method: "POST",
  });
}

export async function reviewAssessmentTemplate(
  templateId: string,
  decision: "APPROVED" | "REJECTED",
  reviewNote: string,
) {
  return request<AssessmentTemplate>(`/api/v1/assessment-templates/${templateId}/review`, {
    method: "POST",
    body: JSON.stringify({ decision, reviewNote }),
  });
}

export async function toggleAssessmentTemplateFavorite(templateId: string) {
  return request<AssessmentTemplate>(`/api/v1/assessment-templates/${templateId}/favorite`, {
    method: "POST",
  });
}

export async function instantiateAssessmentTemplate(templateId: string, newAssessmentName: string) {
  return request<InstantiateTemplateResponse>(
    `/api/v1/assessment-templates/${templateId}/instantiate`,
    {
      method: "POST",
      body: JSON.stringify({ newAssessmentName }),
    },
  );
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getSession().token;
  const headers = new Headers(init?.headers);
  headers.set("Accept", "application/json");
  if (init?.body) headers.set("Content-Type", "application/json");
  if (token && token !== "praxis-security-disabled") {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const response = await fetch(`${getApiBaseUrl()}${path}`, { ...init, headers });
  if (!response.ok) throw new Error(await readError(response));
  return (await response.json()) as T;
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
