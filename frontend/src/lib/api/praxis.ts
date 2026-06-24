import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";

export type AttemptStatus =
  | "notStarted"
  | "inProgress"
  | "paused"
  | "completed"
  | "abandoned"
  | "expired"
  | "failed";

export type ParticipacaoStatus =
  | "nao_iniciada"
  | "em_andamento"
  | "pausada"
  | "concluida"
  | "abandonada"
  | "expirada"
  | "falhou";

export type MediaType = "IMAGE" | "AUDIO";

export interface MediaUploadResponse {
  url: string;
  mediaType: MediaType;
  contentType: string;
  sizeBytes: number;
}

export interface CandidateOptionResponse {
  id: string;
  texto: string;
  descricaoAcessivel?: string | null;
  audioDescricaoUrl?: string | null;
  mediaUrl?: string | null;
  tipoMidia?: MediaType | null;
  proximaEtapaId?: string | null;
}

export interface CandidateNodeResponse {
  id: string;
  numero: number;
  pessoa: string;
  descricao: string;
  descricaoAcessivel?: string | null;
  tempoLimiteSegundos: number | null;
  tempoLimiteSegundosAcomodado?: number | null;
  audioDescricaoUrl?: string | null;
  midiaUrl?: string | null;
  tipoMidia?: MediaType | null;
  proximaEtapaTempoEsgotadoId?: string | null;
  alternativas: CandidateOptionResponse[];
}

export interface CandidateProgressResponse {
  passoAtual: number;
  passosEstimados: number;
  percentual: number;
}

export interface CandidateAttemptResponse {
  participacaoId: string;
  avaliacaoNome: string;
  status: ParticipacaoStatus;
  finalizado: boolean;
  acaoSugeridaFrontend?: "INICIAR" | "CONTINUAR_TESTE" | "VER_RESULTADOS";
  progresso: CandidateProgressResponse;
  etapaAtual: CandidateNodeResponse | null;
  /** Quando verdadeiro, o tenant opera na vertical de saúde: o fluxo coleta consentimento (LGPD). */
  verticalSaude?: boolean;
}

export interface SubmitAnswerRequest {
  etapaId?: string | null;
  etapaNumero?: number | null;
  respostaId?: string | null;
  respondidaEm?: string | null;
  tempoEsgotado: boolean;
}

export interface SubmitAnswerResponse {
  participacaoId: string;
  status: ParticipacaoStatus;
  repetida: boolean;
  finalizado: boolean;
  progresso: CandidateProgressResponse;
  etapaAtual: CandidateNodeResponse | null;
}

export type ValidationIssueSeverity = "warning" | "blocker";

export interface ValidationIssueResponse {
  severity: ValidationIssueSeverity;
  nodeId: string | null;
  message: string;
}

export interface SimulationValidationResponse {
  simulationId: string;
  versionNumber: number;
  publishable: boolean;
  blockerCount: number;
  warningCount: number;
  qualityScore: number;
  issues: ValidationIssueResponse[];
}

export interface SimulationMonitoringResponse {
  simulationId: string;
  versionNumber: number;
  attemptsCreated: number;
  attemptsNotStarted: number;
  attemptsInProgress: number;
  attemptsPaused: number;
  attemptsCompleted: number;
  attemptsAbandoned: number;
  attemptsExpired: number;
  attemptsFailed: number;
  completionRatePercent: number;
  dropOffRatePercent: number;
}

export interface SimulationSummaryResponse {
  id: string;
  name: string;
  description: string;
  criticalSituatioetapa: string | null;
  resultUse?: string | null;
  versionNumber: number;
  status: SimulationVersionStatus;
  /**
   * Número da versão publicada que está no ar, quando existe. Pode diferir de
   * `versionNumber` quando a versão exibida é um rascunho criado para edição
   * enquanto a versão publicada anterior continua atendendo links de candidatos.
   * `null` quando nenhuma versão da avaliação está publicada.
   */
  livePublishedVersionNumber: number | null;
  updatedAt: string;
  competencies: string[];
  attemptsCreated: number;
  attemptsCompleted: number;
  completionRatePercent: number;
}

export interface CreateSimulationDraftRequest {
  name: string;
  description: string;
  rootNodeId: string;
  competencies: string[];
  criticalSituatioetapa: string;
  resultUse?: string;
}

export interface UpdateBlueprintCompetencyRequest {
  name: string;
  weight: number;
  targetScore?: number | null;
}

export interface UpdateBlueprintRequest {
  rootNodeId: string;
  competencies: UpdateBlueprintCompetencyRequest[];
  criticalSituatioetapa: string | null;
  resultUse?: string | null;
}

export interface SimulationVersionOptionResponse {
  id: string;
  text: string;
  competencyLevels: Record<string, number>;
  isCritical: boolean;
  nextNodeId: string | null;
  auditNote: string;
  plainTextDescriptioetapa: string | null;
  audioDescriptionUrl?: string | null;
  mediaUrl: string | null;
  mediaType: MediaType | null;
}

export interface SimulationVersionNodeResponse {
  id: string;
  turnIndex: number;
  speaker: string;
  clientMessage: string;
  timeLimitSeconds: number | null;
  timeoutNextNodeId: string | null;
  isFinal: boolean;
  reportText: string | null;
  positionX: number | null;
  positionY: number | null;
  plainTextDescriptioetapa: string | null;
  audioDescriptionUrl?: string | null;
  mediaUrl: string | null;
  mediaType: MediaType | null;
  options: SimulationVersionOptionResponse[];
}

export interface SimulationVersionDetailResponse {
  simulationId: string;
  name: string;
  description: string;
  criticalSituatioetapa: string | null;
  resultUse?: string | null;
  versionNumber: number;
  status: SimulationVersionStatus;
  blueprint: {
    rootNodeId: string;
    competencies: UpdateBlueprintCompetencyRequest[];
  };
  nodes: SimulationVersionNodeResponse[];
}

export interface CreateNodeRequest {
  clientMessage?: string | null;
  timeLimitSeconds?: number | null;
  timeJustificatioetapa?: string | null;
  timeoutNextNodeId?: string | null;
  isFinal?: boolean;
  reportText?: string | null;
  positionX?: number | null;
  positionY?: number | null;
  plainTextDescriptioetapa?: string | null;
  audioDescriptionUrl?: string | null;
  mediaUrl?: string | null;
  mediaType?: MediaType | null;
}

export interface UpdateNodeRequest {
  clientMessage?: string;
  timeLimitSeconds?: number | null;
  timeJustificatioetapa?: string | null;
  timeoutNextNodeId?: string | null;
  isFinal?: boolean;
  reportText?: string | null;
  positionX?: number | null;
  positionY?: number | null;
  plainTextDescriptioetapa?: string | null;
  audioDescriptionUrl?: string | null;
  mediaUrl?: string | null;
  mediaType?: MediaType | null;
}

export interface CreateOptionRequest {
  text: string;
  competencyLevels: Record<string, number>;
  isBest?: boolean;
  isCritical: boolean;
  nextNodeId?: string | null;
  resultingTone?: string | null;
  plainTextDescriptioetapa?: string | null;
  audioDescriptionUrl?: string | null;
  mediaUrl?: string | null;
  mediaType?: MediaType | null;
}

export interface UpdateOptionRequest {
  text?: string;
  competencyLevels?: Record<string, number>;
  isBest?: boolean;
  isCritical?: boolean;
  nextNodeId?: string | null;
  resultingTone?: string | null;
  plainTextDescriptioetapa?: string | null;
  audioDescriptionUrl?: string | null;
  mediaUrl?: string | null;
  mediaType?: MediaType | null;
}

export type ResultDeliveryStatus = "pending" | "retrying" | "sent" | "dlq";

export interface ResultDeliveryResponse {
  id: number;
  attemptId: string;
  resultId: string;
  webhookUrl: string;
  status: ResultDeliveryStatus;
  attemptCount: number;
  nextAttemptAt: string | null;
  lastAttemptAt: string | null;
  sentAt: string | null;
  lastError: string | null;
  createdAt: string;
}

export type SimulationVersionStatus = "draft" | "published" | "archived";

export type AuditEventType =
  | "attemptCreated"
  | "attemptStarted"
  | "attemptAbandoned"
  | "attemptExpired"
  | "answerSubmitted"
  | "attemptCompleted"
  | "simulationVersionDraftCreated"
  | "simulationVersionBlueprintUpdated"
  | "simulationVersionCloned"
  | "simulationVersionPublished"
  | "simulationNodeAdded"
  | "simulationNodeUpdated"
  | "simulationNodeDeleted"
  | "simulationOptionAdded"
  | "simulationOptionUpdated"
  | "simulationOptionDeleted"
  | "simulationGupyIntegrationActivated"
  | "simulationArchived";

export interface AuditEventResponse {
  id: number;
  aggregateType: string;
  aggregateId: string;
  eventType: AuditEventType;
  message: string;
  metadata: string | null;
  createdAt: string;
}


export interface CloneSimulationVersionResponse {
  simulationId: string;
  sourceVersionNumber: number;
  newVersionNumber: number;
  status: SimulationVersionStatus;
}

export interface PublishSimulationResponse {
  simulationId: string;
  versionNumber: number;
  status: SimulationVersionStatus;
  publishedAt: string | null;
}

export type GupyPreflightCheckCode = "publicBaseUrl" | "integrationToken" | "simulationValidation";

export type GupyPreflightCheckStatus = "ok" | "warning" | "blocker";

export interface GupyPreflightCheckResponse {
  code: GupyPreflightCheckCode;
  status: GupyPreflightCheckStatus;
  message: string;
}

export interface GupyPreflightResponse {
  simulationId: string;
  versionNumber: number;
  ok: boolean;
  checks: GupyPreflightCheckResponse[];
}

export interface PrivacyComplianceResponse {
  legalBases: {
    name: string;
    description: string;
  }[];
  retentionDays: number;
  retentionPolicy: string;
  controllerContact: {
    controllerName: string;
    serviceEmail: string | null;
    serviceUrl: string | null;
    dataProtectionOfficerContact: string | null;
    reviewInstructions: string;
  };
  reviewChannel: string;
  reviewSla: string;
  automatedDecisionWithoutReviewAllowed: boolean;
}

export interface AccountResponse {
  id: number;
  tenantId: string;
  name: string;
  email: string;
  roles: string[];
}

export interface CompanyProfileResponse {
  tradeName: string | null;
  legalName: string | null;
  taxId: string | null;
  corporateEmail: string | null;
  phone: string | null;
  website: string | null;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export type IntegrationProvider = "gupy" | "recrutei";

export interface IntegrationTokenResponse {
  provider: IntegrationProvider;
  configured: boolean;
  createdAt: string | null;
}

export interface RotateIntegrationTokenResponse extends IntegrationTokenResponse {
  token: string;
}

export type TenantConfigType =
  | "COMPETENCY"
  | "ANSWER_TIME_LIMIT";

export interface TenantConfigOption {
  value: string;
  label: string;
  locked: boolean;
  selectedByDefault: boolean;
}

export interface TenantConfig {
  competencies: TenantConfigOption[];
  answerTimeLimits: TenantConfigOption[];
}

export class PraxisApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "PraxisApiError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const session = getSession();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (session.token && isAdminPath(path)) {
    headers.Authorization = `Bearer ${session.token}`;
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...init,
    headers: {
      ...headers,
      ...init?.headers,
    },
  });

  if (!response.ok) {
    let message = `Falha na API (${response.status})`;
    try {
      const body = (await response.json()) as {
        mensagem?: string;
        message?: string;
        error?: string;
      };
      message = body.mensagem ?? body.message ?? body.error ?? message;
    } catch {
      // Mantem a mensagem HTTP padrao quando a resposta nao vem em JSON.
    }
    throw new PraxisApiError(message, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return response.json() as Promise<T>;
  }

  const text = await response.text();
  return (text.length > 0 ? text : undefined) as T;
}

export async function uploadMedia(file: File): Promise<MediaUploadResponse> {
  const session = getSession();
  const headers: Record<string, string> = {};
  if (session.token) {
    headers.Authorization = `Bearer ${session.token}`;
  }
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${getApiBaseUrl()}/api/v1/media`, {
    method: "POST",
    headers,
    body: formData,
  });

  if (!response.ok) {
    let message = `Falha no upload da mídia (${response.status})`;
    try {
      const body = (await response.json()) as { message?: string; error?: string };
      message = body.message ?? body.error ?? message;
    } catch {
      // Mantem a mensagem HTTP padrao quando a resposta nao vem em JSON.
    }
    throw new PraxisApiError(message, response.status);
  }

  return response.json() as Promise<MediaUploadResponse>;
}

function isAdminPath(path: string) {
  return (
    path.startsWith("/api/v1/simulations") ||
    path.startsWith("/api/v1/tenant-config") ||
    path.startsWith("/api/v1/account") ||
    path.startsWith("/api/v1/company-profile") ||
    path.startsWith("/api/v1/integrations") ||
    path.startsWith("/api/v1/gupy/result-deliveries") ||
    path.startsWith("/api/v1/notifications") ||
    path.startsWith("/api/v1/audit") ||
    path.startsWith("/api/v1/terms") ||
    path.startsWith("/api/v1/candidate-links")
  );
}

export function getTenantConfig() {
  return request<TenantConfig>("/api/v1/tenant-config");
}

export function updateTenantConfig(configType: TenantConfigType, options: TenantConfigOption[]) {
  return request<TenantConfigOption[]>(`/api/v1/tenant-config/${configType}`, {
    method: "PUT",
    body: JSON.stringify({ options }),
  });
}

export function getCurrentAccount() {
  return request<AccountResponse>("/api/v1/account/me");
}

export function getCompanyProfile() {
  return request<CompanyProfileResponse>("/api/v1/company-profile");
}

export function changePassword(body: ChangePasswordRequest) {
  return request<AccountResponse>("/api/v1/account/password", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function listIntegrationTokens() {
  return request<IntegrationTokenResponse[]>("/api/v1/integrations/tokens");
}

export function rotateIntegrationToken(provider: IntegrationProvider) {
  return request<RotateIntegrationTokenResponse>(
    `/api/v1/integrations/tokens/${encodeURIComponent(provider)}/rotate`,
    { method: "POST" },
  );
}

export function deleteIntegrationToken(provider: IntegrationProvider) {
  return request<void>(`/api/v1/integrations/tokens/${encodeURIComponent(provider)}`, {
    method: "DELETE",
  });
}

export function getCandidateAttempt(attemptId: string) {
  return request<CandidateAttemptResponse>(`/candidate/attempts/${encodeURIComponent(attemptId)}`);
}

export function submitCandidateAnswer(attemptId: string, body: SubmitAnswerRequest) {
  return request<SubmitAnswerResponse>(
    `/candidate/attempts/${encodeURIComponent(attemptId)}/answers`,
    {
      method: "POST",
      body: JSON.stringify(body),
    },
  );
}

export function listSimulations() {
  return request<SimulationSummaryResponse[]>("/api/v1/simulations");
}

export function deleteSimulation(simulationId: string) {
  return request<void>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}`,
    { method: "DELETE" },
  );
}

export function createSimulationDraft(body: CreateSimulationDraftRequest) {
  return request<SimulationSummaryResponse>("/api/v1/simulations/drafts", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function getSimulationVersion(simulationId: string, versionNumber: number) {
  return request<SimulationVersionDetailResponse>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}`,
  );
}

export function getSimulationValidation(simulationId: string, versionNumber: number) {
  return request<SimulationValidationResponse>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/validation`,
  );
}

export function updateSimulationBlueprint(
  simulationId: string,
  versionNumber: number,
  body: UpdateBlueprintRequest,
) {
  return request<SimulationSummaryResponse>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/blueprint`,
    {
      method: "PATCH",
      body: JSON.stringify(body),
    },
  );
}

export function createSimulationNode(
  simulationId: string,
  versionNumber: number,
  body: CreateNodeRequest,
) {
  return request<string>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/nodes`,
    {
      method: "POST",
      body: JSON.stringify(body),
    },
  );
}

export function updateSimulationNode(
  simulationId: string,
  versionNumber: number,
  nodeId: string,
  body: UpdateNodeRequest,
) {
  return request<void>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/nodes/${encodeURIComponent(nodeId)}`,
    {
      method: "PUT",
      body: JSON.stringify(body),
    },
  );
}

export function deleteSimulationNode(simulationId: string, versionNumber: number, nodeId: string) {
  return request<void>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/nodes/${encodeURIComponent(nodeId)}`,
    { method: "DELETE" },
  );
}

export function createSimulationOption(
  simulationId: string,
  versionNumber: number,
  nodeId: string,
  body: CreateOptionRequest,
) {
  return request<string>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/nodes/${encodeURIComponent(nodeId)}/options`,
    {
      method: "POST",
      body: JSON.stringify(body),
    },
  );
}

export function updateSimulationOption(
  simulationId: string,
  versionNumber: number,
  nodeId: string,
  optionId: string,
  body: UpdateOptionRequest,
) {
  return request<void>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/nodes/${encodeURIComponent(nodeId)}/options/${encodeURIComponent(optionId)}`,
    {
      method: "PUT",
      body: JSON.stringify(body),
    },
  );
}

export function deleteSimulationOption(
  simulationId: string,
  versionNumber: number,
  nodeId: string,
  optionId: string,
) {
  return request<void>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/nodes/${encodeURIComponent(nodeId)}/options/${encodeURIComponent(optionId)}`,
    { method: "DELETE" },
  );
}

export function getSimulationMonitoring(simulationId: string, versionNumber: number) {
  return request<SimulationMonitoringResponse>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/monitoring`,
  );
}

export function listResultDeliveries(params?: {
  status?: ResultDeliveryStatus;
  simulationId?: string;
  versionNumber?: number;
}) {
  const searchParams = new URLSearchParams();
  if (params?.status) {
    searchParams.set("status", params.status);
  }
  if (params?.simulationId && params.versionNumber) {
    searchParams.set("simulationId", params.simulationId);
    searchParams.set("versionNumber", String(params.versionNumber));
  }
  const search = searchParams.size > 0 ? `?${searchParams.toString()}` : "";
  return request<ResultDeliveryResponse[]>(`/api/v1/gupy/result-deliveries${search}`);
}

export function listSimulationVersionAuditEvents(simulationId: string, versionNumber: number) {
  return request<AuditEventResponse[]>(
    `/api/v1/audit/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}`,
  );
}

export function cloneSimulationVersionToDraft(simulationId: string, versionNumber: number) {
  return request<CloneSimulationVersionResponse>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/clone-draft`,
    { method: "POST" },
  );
}


export function publishSimulationVersion(simulationId: string, versionNumber: number) {
  return request<PublishSimulationResponse>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/publish`,
    { method: "POST" },
  );
}

export function getGupyPreflight(simulationId: string, versionNumber: number) {
  return request<GupyPreflightResponse>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/gupy-preflight`,
  );
}

export function getPrivacyCompliance() {
  return request<PrivacyComplianceResponse>("/api/v1/privacy/compliance");
}

export interface CreateCandidateLinkRequest {
  simulationId: string;
  candidateName: string;
  candidateEmail: string;
  accommodationTimeMultiplier?: number | null;
}

export interface CreateCandidateLinkResponse {
  attemptId: string;
  candidateUrl: string;
  simulationName: string;
}

export interface CandidateLinkResponse {
  attemptId: string;
  candidateUrl: string;
  candidateName: string;
  // Null no modo cego: o servidor não envia o e-mail enquanto o blind está ativo (REQ-L3).
  candidateEmail: string | null;
  simulationId: string;
  simulationName: string;
  status: AttemptStatus;
  createdAt: string;
}

export interface CandidateAttemptMonitoringResponse {
  attemptId: string;
  candidateName: string;
  candidateEmail: string;
  simulationId: string;
  simulationName: string;
  versionNumber: number;
  status: AttemptStatus;
  currentTurn: number;
  estimatedTurns: number;
  progressPercent: number;
  elapsedSeconds: number;
  lastSignalAt: string;
  active: boolean;
}

export interface CompetencyBenchmarkDto {
  competencyName: string;
  targetScore: number;
}

export interface CompetencyScoreDto {
  competencyName: string;
  score: number;
}

export interface CandidateRadarDto {
  attemptId: string;
  candidateName: string;
  generalScore: number;
  competencies: CompetencyScoreDto[];
}

export interface TalentMatchResponse {
  simulationId: string;
  versionNumber: number;
  benchmark: CompetencyBenchmarkDto[];
  candidates: CandidateRadarDto[];
}

export function listCandidateLinks(blind = false) {
  return request<CandidateLinkResponse[]>(`/api/v1/candidate-links${blind ? "?blind=true" : ""}`);
}

export function listLiveAttempts() {
  return request<CandidateAttemptMonitoringResponse[]>("/api/v1/candidate-links/live-attempts");
}

export function createCandidateLink(body: CreateCandidateLinkRequest) {
  return request<CreateCandidateLinkResponse>("/api/v1/candidate-links", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export type HumanDecision = "advanced" | "rejected" | "hired" | "onHold";

export interface RegisterDispositionRequest {
  decision: HumanDecision;
  reason?: string;
}

/**
 * Registra a decisão humana sobre o candidato (REQ-L1). O score é apenas apoio: a decisão
 * final é sempre de uma pessoa, e este registro append-only guarda quem decidiu e por quê.
 */
export function registerCandidateDisposition(attemptId: string, body: RegisterDispositionRequest) {
  return request<void>(`/api/v1/candidate-links/${encodeURIComponent(attemptId)}/disposition`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export interface EvidenceScoringDeclaration {
  deterministic: boolean;
  usesArtificialIntelligence: boolean;
  usesTrainingData: boolean;
  statement: string;
  formula: string;
  recommendInterviewThreshold: number;
}

export interface EvidenceCompetency {
  name: string;
  score: number;
  tier: string;
  weight: number | null;
}

export interface EvidencePathStep {
  turnIndex: number;
  nodeId: string;
  speaker: string;
  prompt: string;
  answeredOptionId: string | null;
  answeredOptionText: string | null;
  timedOut: boolean;
  critical: boolean;
  competencyPoints: Record<string, number>;
  answeredAt: string;
}

export interface EvidenceHumanDecision {
  decision: string | null;
  decidedByUserId: string | null;
  reason: string | null;
  decidedAt: string;
}

export interface EvidenceReport {
  attemptId: string;
  candidateName: string;
  candidateEmail: string;
  simulationId: string;
  simulationName: string | null;
  versionNumber: number | null;
  versionId: number | null;
  generalScore: number | null;
  decision: string;
  reliabilityLevel: string;
  humanReviewRequired: boolean;
  summaryMarkdown: string;
  startedAt: string | null;
  finishedAt: string | null;
  declaration: EvidenceScoringDeclaration;
  competencies: EvidenceCompetency[];
  path: EvidencePathStep[];
  humanDecision: EvidenceHumanDecision | null;
  auditTrail: unknown[];
}

/**
 * Relatório de transparência do scoring (REQ-L4): declaração de cálculo determinístico (sem IA,
 * sem dados de treino), fórmula, caminho do candidato, pontos por competência, trilha append-only e
 * a decisão humana. Documento entregável para compliance/jurídico.
 */
export function getEvidenceReport(attemptId: string) {
  return request<EvidenceReport>(
    `/api/v1/candidate-links/${encodeURIComponent(attemptId)}/evidence-report`,
  );
}

export interface TermResponse {
  type: string;
  version: string;
  text: string;
}

export interface TermAcceptanceStatusResponse {
  type: string;
  currentVersion: string;
  accepted: boolean;
  acceptedVersion: string | null;
  acceptedAt: string | null;
}

export function getResponsibilityTerm() {
  return request<TermResponse>("/api/v1/terms/responsibility");
}

export function getResponsibilityAcceptance() {
  return request<TermAcceptanceStatusResponse>("/api/v1/terms/responsibility/acceptance");
}

export function acceptResponsibilityTerm(version: string) {
  return request<TermAcceptanceStatusResponse>("/api/v1/terms/responsibility/acceptance", {
    method: "POST",
    body: JSON.stringify({ version }),
  });
}

export function getHealthUseTerm() {
  return request<TermResponse>("/api/v1/terms/health-use");
}

export function getHealthUseAcceptance() {
  return request<TermAcceptanceStatusResponse>("/api/v1/terms/health-use/acceptance");
}

export function acceptHealthUseTerm(version: string) {
  return request<TermAcceptanceStatusResponse>("/api/v1/terms/health-use/acceptance", {
    method: "POST",
    body: JSON.stringify({ version }),
  });
}

/**
 * Pedido de revisão humana do candidato (REQ-L5 / LGPD art. 20). Rota pública: o candidato
 * acessa pelo próprio link, sem autenticação.
 */
export function requestHumanReview(attemptId: string, reason?: string) {
  return request<void>(`/candidate/attempts/${encodeURIComponent(attemptId)}/review-request`, {
    method: "POST",
    body: JSON.stringify({ reason: reason && reason.trim() ? reason.trim() : null }),
  });
}

/** Versão do aviso de consentimento de saúde (Minuta A) exibido ao participante. */
export const HEALTH_CONSENT_VERSION = "2026-06-01";

/**
 * Registra o consentimento do participante para tratamento de dado sensível de saúde na vertical
 * educativa (LGPD, arts. 11 e 14). Rota pública, sem autenticação.
 */
export function recordHealthConsent(attemptId: string, onBehalfOfMinor = false) {
  return request<void>(`/candidate/attempts/${encodeURIComponent(attemptId)}/health-consent`, {
    method: "POST",
    body: JSON.stringify({ version: HEALTH_CONSENT_VERSION, onBehalfOfMinor }),
  });
}

export function getTalentMatch(
  simulationId: string,
  versionNumber: number,
  attemptIds: string[],
  blind = false,
) {
  const searchParams = new URLSearchParams();
  searchParams.set("attemptIds", attemptIds.join(","));
  if (blind) {
    searchParams.set("blind", "true");
  }

  return request<TalentMatchResponse>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/talent-match?${searchParams.toString()}`,
  );
}
