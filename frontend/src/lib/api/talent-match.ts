import { apiRequest } from "@/lib/api/http";

export type NormativeGroupStatus = "DRAFT" | "ACTIVE" | "INELIGIBLE" | "ARCHIVED";
export type DecisionThresholdStatus = "DRAFT" | "APPROVED" | "REVOKED" | "EXPIRED";

export interface CompetencyTargetProfileDto {
  competencyName: string;
  targetScore: number;
  source: string;
  warning: string;
}

export interface CompetencyScoreDto {
  competencyName: string;
  score: number;
}

export interface NormativeMetricDto {
  competencyName: string;
  sampleSize: number;
  mean: number;
  standardDeviation: number;
}

export interface NormativeReferenceResponse {
  id: number;
  name: string;
  jobTitle: string;
  seniority: string | null;
  gupyJobId: number | null;
  populationDescription: string;
  periodStart: string;
  periodEnd: string;
  versionNumber: number;
  sampleSize: number;
  minimumSample: number;
  eligible: boolean;
  status: NormativeGroupStatus;
  limitation: string | null;
  metrics: NormativeMetricDto[];
}

export interface DecisionThresholdResponse {
  id: number;
  score: number;
  populationDescription: string;
  justification: string;
  evidence: string;
  validFrom: string;
  validUntil: string | null;
  status: DecisionThresholdStatus;
  approvedBy: string | null;
  warning: string | null;
}

export interface CandidateReferenceSnapshotDto {
  targetProfile: CompetencyTargetProfileDto[];
  normativeReference: NormativeReferenceResponse | null;
  decisionThreshold: DecisionThresholdResponse | null;
  capturedAt: string;
}

export interface CandidateRadarDto {
  attemptId: string;
  candidateName: string;
  generalScore: number;
  normativePercentile: number | null;
  meetsDecisionThreshold: boolean | null;
  referenceSnapshot: CandidateReferenceSnapshotDto;
  competencies: CompetencyScoreDto[];
}

export interface TalentMatchResponse {
  simulationId: string;
  versionNumber: number;
  targetProfile: CompetencyTargetProfileDto[];
  normativeReference: NormativeReferenceResponse | null;
  decisionThreshold: DecisionThresholdResponse | null;
  warnings: string[];
  candidates: CandidateRadarDto[];
}

export interface TalentReferenceConfigurationResponse {
  targetProfile: CompetencyTargetProfileDto[];
  normativeGroups: NormativeReferenceResponse[];
  decisionThresholds: DecisionThresholdResponse[];
  warnings: string[];
}

export interface NormativeGroupRequest {
  name: string;
  jobTitle: string;
  seniority?: string | null;
  gupyJobId?: number | null;
  populationDescription: string;
  periodStart: string;
  periodEnd: string;
  minimumSample: number;
  pathCompatibilityConfirmed: boolean;
  activate: boolean;
}

export interface DecisionThresholdRequest {
  score: number;
  populationDescription: string;
  justification: string;
  evidence: string;
  validFrom: string;
  validUntil?: string | null;
  approve: boolean;
}

export function getTalentMatch(
  simulationId: string,
  versionNumber: number,
  attemptIds: string[],
  blind = false,
) {
  const searchParams = new URLSearchParams();
  searchParams.set("attemptIds", attemptIds.join(","));
  if (blind) searchParams.set("blind", "true");

  return apiRequest<TalentMatchResponse>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/talent-match?${searchParams.toString()}`,
    undefined,
    { fallbackMessage: "Não foi possível carregar o Talent Match." },
  );
}

export function getTalentReferenceConfiguration(simulationId: string, versionNumber: number) {
  return apiRequest<TalentReferenceConfigurationResponse>(
    `/api/v1/results/talent-references/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}`,
    undefined,
    { fallbackMessage: "Não foi possível carregar as referências do Talent Match." },
  );
}

export function configureNormativeGroup(
  simulationId: string,
  versionNumber: number,
  body: NormativeGroupRequest,
) {
  return apiRequest<NormativeReferenceResponse>(
    `/api/v1/results/talent-references/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/normative-groups`,
    { method: "POST", body: JSON.stringify(body) },
    { fallbackMessage: "Não foi possível salvar o grupo normativo." },
  );
}

export function configureDecisionThreshold(
  simulationId: string,
  versionNumber: number,
  body: DecisionThresholdRequest,
) {
  return apiRequest<DecisionThresholdResponse>(
    `/api/v1/results/talent-references/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/decision-thresholds`,
    { method: "POST", body: JSON.stringify(body) },
    { fallbackMessage: "Não foi possível salvar a nota de corte." },
  );
}
