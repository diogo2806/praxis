import { apiRequest } from "@/lib/api/http";

export type LocaleStatus = "DRAFT" | "IN_REVIEW" | "APPROVED";

export interface NodeTranslation {
  nodeId: string;
  speaker: string;
  message: string;
  reportText?: string;
  plainTextDescription?: string;
  mediaTranscript?: string;
}

export interface OptionTranslation {
  nodeId: string;
  optionId: string;
  text: string;
  plainTextDescription?: string;
  mediaTranscript?: string;
}

export interface CompetencyTranslation {
  competencyName: string;
  displayName: string;
  reportText: string;
}

export interface LocaleContent {
  title: string;
  description: string;
  instructions: string;
  reportIntroduction: string;
  nodes: NodeTranslation[];
  options: OptionTranslation[];
  competencies: CompetencyTranslation[];
}

export interface LocaleSummary {
  locale: string;
  baseLocale: boolean;
  status: LocaleStatus;
  revision: number;
  translatedNodes: number;
  totalNodes: number;
  translatedOptions: number;
  totalOptions: number;
  translatedCompetencies: number;
  totalCompetencies: number;
  completenessPercent: number;
  updatedAt: string;
  reviewedBy?: string;
  approvedBy?: string;
}

export interface LocaleContentResponse {
  simulationVersionId: number;
  locale: string;
  baseLocale: boolean;
  status: LocaleStatus;
  revision: number;
  content: LocaleContent;
  validationErrors: string[];
  validationWarnings: string[];
}

export function configureSimulationLocales(
  versionId: number,
  input: { baseLocale: string; enabledLocales: string[] },
) {
  return apiRequest<LocaleSummary[]>(`/api/v1/simulation-versions/${versionId}/locales/configure`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function listSimulationLocales(versionId: number) {
  return apiRequest<LocaleSummary[]>(`/api/v1/simulation-versions/${versionId}/locales`);
}

export function getSimulationLocale(versionId: number, locale: string) {
  return apiRequest<LocaleContentResponse>(
    `/api/v1/simulation-versions/${versionId}/locales/${encodeURIComponent(locale)}`,
  );
}

export function saveSimulationLocale(versionId: number, locale: string, content: LocaleContent) {
  return apiRequest<LocaleContentResponse>(
    `/api/v1/simulation-versions/${versionId}/locales/${encodeURIComponent(locale)}`,
    { method: "PUT", body: JSON.stringify(content) },
  );
}

export function reviewSimulationLocale(versionId: number, locale: string) {
  return apiRequest<LocaleContentResponse>(
    `/api/v1/simulation-versions/${versionId}/locales/${encodeURIComponent(locale)}/review`,
    { method: "POST" },
  );
}

export function approveSimulationLocale(versionId: number, locale: string) {
  return apiRequest<LocaleContentResponse>(
    `/api/v1/simulation-versions/${versionId}/locales/${encodeURIComponent(locale)}/approve`,
    { method: "POST" },
  );
}

export function exportSimulationLocale(versionId: number, locale: string) {
  return apiRequest<unknown>(
    `/api/v1/simulation-versions/${versionId}/locales/${encodeURIComponent(locale)}/export`,
  );
}

export function importSimulationLocale(
  versionId: number,
  input: { locale: string; content: LocaleContent; replaceExisting: boolean },
) {
  return apiRequest<LocaleContentResponse>(`/api/v1/simulation-versions/${versionId}/locales/import`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}
