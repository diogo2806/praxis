import { apiRequest } from "@/lib/api/http";
import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";

export type CriterionType = "NUMERIC" | "CATEGORY";

export interface QualityFilters {
  simulationId: string;
  versionNumber?: number;
  gupyJobId?: number;
  from?: string;
  to?: string;
}

export interface CategoryMean {
  category: string;
  sampleSize: number;
  averageScore: number | null;
  suppressed: boolean;
}

export interface AssessmentQualityReport {
  generatedAt: string;
  scope: QualityFilters;
  observed: {
    sampleSize: number;
    completed: number;
    abandonedOrExpired: number;
    completionRatePercent: number;
    meanScore: number | null;
    standardDeviation: number | null;
    percentile10: number | null;
    percentile25: number | null;
    median: number | null;
    percentile75: number | null;
    percentile90: number | null;
    averageDurationSeconds: number | null;
    pauseEvents: number;
  };
  scoreDistribution: Array<{
    fromInclusive: number;
    toInclusive: number;
    count: number;
    percentage: number;
  }>;
  alternatives: Array<{
    nodeId: string;
    optionId: string;
    selectedCount: number;
    selectionPercent: number;
    averageFinalScore: number | null;
    discriminationDifference: number | null;
    diagnostic: string;
    evidenceType: string;
  }>;
  paths: Array<{
    pathFingerprint: string;
    nodeIds: string[];
    count: number;
    frequencyPercent: number;
    averageScore: number | null;
    averageDurationSeconds: number | null;
    evidenceType: string;
  }>;
  scenarios: Array<{
    nodeId: string;
    presentations: number;
    answers: number;
    timeouts: number;
    averageResponseSeconds: number | null;
    videoPauseEvents: number;
    evidenceType: string;
  }>;
  competencies: Array<{
    competency: string;
    sampleSize: number;
    mean: number | null;
    standardDeviation: number | null;
    standardError: number | null;
    confidenceLow95: number | null;
    confidenceHigh95: number | null;
    precisionLevel: string;
    evidenceType: string;
  }>;
  externalCriteria: Array<{
    criterionCode: string;
    criterionLabel: string;
    criterionType: CriterionType;
    sampleSize: number;
    pearsonCorrelation: number | null;
    categoryMeans: CategoryMean[];
    interpretation: string;
    evidenceType: string;
  }>;
  sensitiveAnalysis: null | {
    groupCriterionCode: string;
    minimumSample: number;
    groups: CategoryMean[];
    suppressedGroups: number;
    purpose: string;
    legalBasis: string;
    auditId: string;
  };
  recommendations: Array<{
    code: string;
    severity: string;
    title: string;
    detail: string;
    evidenceType: string;
  }>;
  warnings: string[];
  methodology: {
    minimumGeneralSample: number;
    minimumSensitiveGroupSample: number;
    scoreScale: string;
    percentileMethod: string;
    precisionMethod: string;
    discriminationMethod: string;
    limitations: string[];
  };
}

export interface ExternalCriterionInput {
  candidateAttemptId: string;
  criterionCode: string;
  criterionLabel: string;
  criterionType: CriterionType;
  numericValue?: number;
  categoryValue?: string;
  observedAt?: string;
}

export interface ExternalCriterion extends ExternalCriterionInput {
  id: string;
  observedAt: string;
  createdAt: string;
}

export function getAssessmentQualityReport(filters: QualityFilters) {
  return apiRequest<AssessmentQualityReport>(
    `/api/v1/assessment-quality/report?${qualityQuery(filters).toString()}`,
  );
}

export function getSensitiveQualityReport(
  filters: QualityFilters,
  input: {
    groupCriterionCode: string;
    purpose: string;
    legalBasis: string;
    minimumSample: number;
  },
) {
  return apiRequest<AssessmentQualityReport>(
    `/api/v1/assessment-quality/sensitive-report?${qualityQuery(filters).toString()}`,
    { method: "POST", body: JSON.stringify(input) },
  );
}

export function saveExternalCriterion(input: ExternalCriterionInput) {
  return apiRequest<ExternalCriterion>("/api/v1/assessment-quality/external-criteria", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function listExternalCriteria(simulationId?: string, versionNumber?: number) {
  const query = new URLSearchParams();
  if (simulationId) query.set("simulationId", simulationId);
  if (versionNumber !== undefined) query.set("versionNumber", String(versionNumber));
  return apiRequest<ExternalCriterion[]>(
    `/api/v1/assessment-quality/external-criteria${query.size ? `?${query}` : ""}`,
  );
}

export async function downloadAssessmentQualityReport(filters: QualityFilters) {
  const token = getSession().token;
  const headers = new Headers({ Accept: "text/csv" });
  if (token && token !== "praxis-security-disabled") headers.set("Authorization", `Bearer ${token}`);
  const response = await fetch(
    `${getApiBaseUrl()}/api/v1/assessment-quality/technical-report.csv?${qualityQuery(filters)}`,
    { headers },
  );
  if (!response.ok) throw new Error((await response.text()) || `Falha HTTP ${response.status}.`);
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `qualidade-avaliacao-${filters.simulationId}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

function qualityQuery(filters: QualityFilters) {
  const query = new URLSearchParams({ simulationId: filters.simulationId });
  if (filters.versionNumber !== undefined) query.set("versionNumber", String(filters.versionNumber));
  if (filters.gupyJobId !== undefined) query.set("gupyJobId", String(filters.gupyJobId));
  if (filters.from) query.set("from", new Date(filters.from).toISOString());
  if (filters.to) query.set("to", new Date(filters.to).toISOString());
  return query;
}
