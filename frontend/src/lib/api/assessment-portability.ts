import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";

export interface AssessmentPackageOrigin {
  sourceSystem: string;
  sourceAssessmentId: string;
  sourceVersionNumber: number;
  exportedBy: string;
  exportedAt: string;
}

export interface AssessmentPackageEnvelope {
  formatVersion: string;
  exportedAt: string;
  contentHash: string;
  manifest: {
    origin: AssessmentPackageOrigin;
    assessment: {
      name: string;
      description: string;
      criticalSituation?: string | null;
      resultUse?: string | null;
    };
    version: {
      rootNodeId: string;
      competencies: Array<{
        name: string;
        weight: number;
        targetScore?: number | null;
        tier?: string | null;
      }>;
      nodes: Array<Record<string, unknown>>;
    };
    mediaAssets: Array<{
      assetId: string;
      url: string;
      mediaType: string;
      declaredSizeBytes: number;
      sha256: string;
      license: string;
      origin: string;
      embedded: boolean;
    }>;
  };
}

export interface PackageValidationProblem {
  path: string;
  code: string;
  message: string;
}

export interface PackageValidationResponse {
  valid: boolean;
  importable: boolean;
  calculatedHash: string;
  errors: PackageValidationProblem[];
  warnings: PackageValidationProblem[];
  competenciesRequiringConfirmation: string[];
  plannedIdMapping: Record<string, string>;
}

export interface ImportPackageResponse {
  simulationId: string;
  versionNumber: number;
  status: string;
  sourceAssessmentId: string;
  sourceVersionNumber: number;
  packageHash: string;
  idMapping: Record<string, string>;
  importedCompetencies: string[];
}

export async function exportAssessmentPackage(simulationId: string, versionNumber: number) {
  const response = await authorizedFetch(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/package`,
  );
  if (!response.ok) throw new Error(await readError(response));
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `praxis-${simulationId}-v${versionNumber}.json`;
  anchor.click();
  URL.revokeObjectURL(url);
}

export async function validateAssessmentPackage(packageEnvelope: AssessmentPackageEnvelope) {
  return request<PackageValidationResponse>("/api/v1/simulation-packages/validate", {
    method: "POST",
    body: JSON.stringify(packageEnvelope),
  });
}

export async function importAssessmentPackage(input: {
  packageEnvelope: AssessmentPackageEnvelope;
  newAssessmentName: string;
  confirmed: boolean;
  confirmCompetencies: boolean;
}) {
  return request<ImportPackageResponse>("/api/v1/simulation-packages/import", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

async function request<T>(path: string, init: RequestInit): Promise<T> {
  const response = await authorizedFetch(path, init);
  if (!response.ok) throw new Error(await readError(response));
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
