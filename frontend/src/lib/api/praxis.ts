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
export type ResultTier = "major" | "minor";

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
  /** Quando verdadeiro, o empresa opera na vertical de saúde: o fluxo coleta consentimento (LGPD). */
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

export type CalibrationFlag = "OK" | "FRACO" | "REVISAR";

export interface OptionDiscriminationDto {
  nodeId: string;
  optionId: string;
  optionLabel: string;
  discriminationIndex: number;
  difficultyIndex: number;
  flag: CalibrationFlag;
}

export interface CompetencyCalibrationDto {
  competencyName: string;
  averageScore: number;
  stdDeviation: number;
}

export interface CalibrationReportResponse {
  sampleSize: number;
  minimumSampleRequired: number;
  sufficientSample: boolean;
  items: OptionDiscriminationDto[];
  competencies: CompetencyCalibrationDto[];
}

export interface SimulationSummaryResponse {
  id: string;
  name: string;
  description: string;
  criticalSituation: string | null;
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
  criticalSituation: string;
  resultUse?: string;
}

export interface UpdateBlueprintCompetencyRequest {
  name: string;
  weight: number;
  targetScore?: number | null;
  tier?: ResultTier | null;
}

export interface UpdateBlueprintRequest {
  rootNodeId: string;
  competencies: UpdateBlueprintCompetencyRequest[];
  criticalSituation: string | null;
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
  criticalSituation: string | null;
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
  eventType: AuditEventType | string;
  message: string;
  metadata: string | null;
  createdAt: string;
}

export type AssessmentJourneyStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
export type AssessmentJourneyAttemptStatus =
  | "CREATED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "EXPIRED"
  | "ABANDONED";
export type AssessmentJourneyStepStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED" | "SKIPPED";

export interface JourneyStepResponse {
  id: number;
  simulationId: string;
  simulationName: string;
  simulationVersionNumber: number;
  sequenceKey: string;
  orderIndex: number;
  required: boolean;
}

export interface AssessmentJourneySequenceResponse {
  sequenceKey: string;
  steps: JourneyStepResponse[];
}

export interface AssessmentJourneySummaryResponse {
  id: string;
  name: string;
  description: string | null;
  status: AssessmentJourneyStatus;
  stepCount: number;
  sequenceCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface AssessmentJourneyDetailResponse {
  id: string;
  name: string;
  description: string | null;
  status: AssessmentJourneyStatus;
  createdAt: string;
  updatedAt: string;
  publishedAt: string | null;
  sequences: AssessmentJourneySequenceResponse[];
}

export interface CreateAssessmentJourneyRequest {
  name: string;
  description?: string | null;
}

export interface UpdateAssessmentJourneyRequest {
  name?: string | null;
  description?: string | null;
}

export interface AddJourneyStepRequest {
  simulationId: string;
  sequenceKey?: string | null;
  orderIndex?: number | null;
  required?: boolean | null;
}

export interface UpdateJourneyStepRequest {
  sequenceKey?: string | null;
  orderIndex?: number | null;
  required?: boolean | null;
}

export interface JourneyAttemptStepResponse {
  id: number;
  journeyStepId: number;
  simulationId: string;
  simulationName: string;
  simulationVersionNumber: number;
  orderIndex: number;
  required: boolean;
  status: AssessmentJourneyStepStatus;
  candidateAttemptId: string | null;
  candidateUrl: string | null;
  startedAt: string | null;
  completedAt: string | null;
}

export interface AssessmentJourneyAttemptResponse {
  id: string;
  journeyId: string;
  journeyName: string;
  candidateName: string;
  candidateEmail: string;
  sequenceKey: string;
  status: AssessmentJourneyAttemptStatus;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  steps: JourneyAttemptStepResponse[];
}

export interface CreateJourneyAttemptRequest {
  journeyId: string;
  candidateName: string;
  candidateEmail: string;
  sequenceKey?: string | null;
}

export interface JourneyConsolidatedResultResponse {
  journeyAttemptId: string;
  journeyId: string;
  journeyName: string;
  candidateName: string;
  candidateEmail: string;
  sequenceKey: string;
  status: AssessmentJourneyAttemptStatus;
  startedAt: string | null;
  completedAt: string | null;
  tests: Array<{
    simulationId: string;
    simulationName: string;
    simulationVersionNumber: number;
    required: boolean;
    stepStatus: AssessmentJourneyStepStatus;
    candidateAttemptId: string | null;
    attemptStatus: AttemptStatus | null;
    score: number | null;
    competencies: Array<{ name: string; score: number }>;
  }>;
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
  empresaId: string;
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

export type DashboardIntegrationStatus =
  | "CONECTADA"
  | "PENDENTE"
  | "ERRO"
  | "DESATIVADA"
  | "NAO_CONFIGURADA";

export type DashboardActionSeverity = "info" | "warning" | "success" | "danger";

export interface DashboardIntegrationStatusItem {
  provider: string;
  name: string;
  status: DashboardIntegrationStatus;
  lastSyncAt: string | null;
  action: string | null;
}

export type IntegrationCenterProvider = "GUPY" | "RECRUTEI" | "CUSTOM_API";
export type IntegrationCenterType = "ATS" | "API" | "WEBHOOK" | "CUSTOM";
export type IntegrationCenterStatus =
  | "CONECTADA"
  | "PENDENTE"
  | "ERRO"
  | "DESATIVADA"
  | "NAO_CONFIGURADA";
export type IntegrationCenterAction =
  | "CONFIGURE"
  | "VIEW"
  | "DISCONNECT"
  | "SYNC"
  | "VIEW_ERROR"
  | "RETRY"
  | "EDIT"
  | "REACTIVATE"
  | "VIEW_DOCS"
  | "GENERATE_TOKEN";

export interface IntegrationCenterItem {
  provider: IntegrationCenterProvider;
  name: string;
  description: string;
  type: IntegrationCenterType;
  status: IntegrationCenterStatus;
  lastSyncAt: string | null;
  configuredAt: string | null;
  errorMessage: string | null;
  tokenPreview: string | null;
  availableActions: IntegrationCenterAction[];
}

export interface GenerateIntegrationTokenResponse {
  provider: string;
  token: string;
  tokenPreview: string;
  createdAt: string;
}

export interface ConfigureIntegrationRequest {
  credentials: Record<string, unknown>;
  settings: Record<string, unknown>;
}

export interface DashboardResponse {
  empresaId: string;
  empresaName: string;
  activeSimulations: number;
  assessmentJourneys: {
    total: number;
    published: number;
    draft: number;
  };
  candidatesInProgress: number;
  completedAttemptsLast30Days: number;
  latestResults: Array<{
    attemptId: string;
    candidateName: string;
    simulationOrJourneyName: string;
    status: AttemptStatus;
    date: string;
    result: number | null;
    actionLabel: string;
    actionRoute: string;
  }>;
  journeys: Array<{
    id: string;
    name: string;
    status: AssessmentJourneyStatus;
    candidatesInProgress: number;
    actionLabel: string;
    actionRoute: string;
  }>;
  integrations: DashboardIntegrationStatusItem[];
  billing: {
    plan: CommercialPlanType;
    status: EmpresaStatus;
    creditBalance: number;
    usedInPeriod: number;
    subscriptionStatus: SubscriptionStatus | null;
    nextRenewalAt: string | null;
    commercialCondition: string | null;
  };
  recommendedActions: Array<{
    type:
      | "CREATE_FIRST_SIMULATION"
      | "CREATE_FIRST_JOURNEY"
      | "CONFIGURE_INTEGRATION"
      | "PUBLISH_DRAFT_JOURNEY"
      | "BUY_CREDITS"
      | "VIEW_RESULTS";
    title: string;
    description: string;
    severity: DashboardActionSeverity;
    buttonLabel: string;
    route: string;
  }>;
}

export type EmpresaConfigType = "COMPETENCY" | "ANSWER_TIME_LIMIT";

export interface EmpresaConfigOption {
  value: string;
  label: string;
  locked: boolean;
  selectedByDefault: boolean;
}

export interface EmpresaConfig {
  competencies: EmpresaConfigOption[];
  answerTimeLimits: EmpresaConfigOption[];
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
    path.startsWith("/api/admin") ||
    path.startsWith("/api/v1/simulations") ||
    path.startsWith("/api/v1/dashboard") ||
    path.startsWith("/api/v1/empresa-config") ||
    path.startsWith("/api/v1/account") ||
    path.startsWith("/api/v1/company-profile") ||
    path.startsWith("/api/v1/integrations") ||
    path.startsWith("/api/v1/gupy/result-deliveries") ||
    path.startsWith("/api/v1/results") ||
    path.startsWith("/api/v1/notifications") ||
    path.startsWith("/api/v1/audit") ||
    path.startsWith("/api/v1/terms") ||
    path.startsWith("/api/v1/assessment-journeys") ||
    path.startsWith("/api/v1/assessment-journey-attempts") ||
    path.startsWith("/api/v1/candidate-links") ||
    path.startsWith("/api/v1/billing")
  );
}

export function getEmpresaConfig() {
  return request<EmpresaConfig>("/api/v1/empresa-config");
}

export function updateEmpresaConfig(configType: EmpresaConfigType, options: EmpresaConfigOption[]) {
  return request<EmpresaConfigOption[]>(`/api/v1/empresa-config/${configType}`, {
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

// --- Conector universal (webhook personalizado + API pública) ---

export type IntegrationStatusValue =
  | "CONECTADA"
  | "PENDENTE"
  | "ERRO"
  | "DESATIVADA"
  | "NAO_CONFIGURADA";

export type WebhookEvent = "RESULT_READY" | "ATTEMPT_STARTED";

export interface GenericWebhookConfigResponse {
  webhookUrl: string | null;
  secretPreview: string | null;
  events: string[];
  status: IntegrationStatusValue;
  lastDeliveryAt: string | null;
  lastError: string | null;
}

export interface ConfigureGenericWebhookRequest {
  webhookUrl: string;
  events: string[];
}

export interface WebhookTestResponse {
  delivered: boolean;
  httpStatus: number | null;
  responseSnippet: string | null;
}

export interface PublicApiTokenResponse {
  token: string;
  tokenPreview: string;
}

export interface WebhookSecretResponse {
  secret: string;
  secretPreview: string;
}

export function getGenericWebhook() {
  return request<GenericWebhookConfigResponse>("/api/v1/integrations/custom-api/webhook");
}

export function configureGenericWebhook(body: ConfigureGenericWebhookRequest) {
  return request<GenericWebhookConfigResponse>("/api/v1/integrations/custom-api/webhook", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function rotateGenericWebhookSecret() {
  return request<WebhookSecretResponse>(
    "/api/v1/integrations/custom-api/webhook/secret/rotate",
    { method: "POST" },
  );
}

export function testGenericWebhook() {
  return request<WebhookTestResponse>("/api/v1/integrations/custom-api/webhook/test", {
    method: "POST",
  });
}

export function generatePublicApiToken() {
  return request<PublicApiTokenResponse>("/api/v1/integrations/custom-api/api-token", {
    method: "POST",
  });
}

export function revokePublicApiToken() {
  return request<void>("/api/v1/integrations/custom-api/api-token", { method: "DELETE" });
}

export async function getDashboard() {
  try {
    return await request<DashboardResponse>("/api/v1/dashboard");
  } catch (error) {
    if (error instanceof PraxisApiError && error.status === 404) {
      return getDashboardFallback();
    }
    throw error;
  }
}

async function getDashboardFallback(): Promise<DashboardResponse> {
  const session = getSession();
  const [
    account,
    companyProfile,
    simulations,
    journeys,
    integrationTokens,
    candidateLinks,
  ] = await Promise.all([
    getCurrentAccount().catch(() => ({
      id: 0,
      empresaId: session.empresaId ?? "",
      name: session.userName,
      email: "",
      roles: [] as string[],
    })),
    getCompanyProfile().catch(() => null),
    listSimulations().catch(() => []),
    listAssessmentJourneys().catch(() => []),
    listIntegrationTokens().catch(() => []),
    listCandidateLinks().catch(() => []),
  ]);

  const now = Date.now();
  const last30Days = now - 30 * 24 * 60 * 60 * 1000;
  const candidatesInProgress = candidateLinks.filter((link) =>
    ["notStarted", "inProgress", "paused"].includes(link.status),
  ).length;
  const completedAttemptsLast30Days = candidateLinks.filter((link) => {
    if (link.status !== "completed") return false;
    const createdAt = new Date(link.createdAt).getTime();
    return Number.isFinite(createdAt) && createdAt >= last30Days;
  }).length;

  return {
    empresaId: account.empresaId,
    empresaName: companyProfile?.tradeName ?? companyProfile?.legalName ?? account.name,
    activeSimulations: simulations.filter(
      (simulation) =>
        simulation.status === "published" || simulation.livePublishedVersionNumber != null,
    ).length,
    assessmentJourneys: {
      total: journeys.length,
      published: journeys.filter((journey) => journey.status === "PUBLISHED").length,
      draft: journeys.filter((journey) => journey.status === "DRAFT").length,
    },
    candidatesInProgress,
    completedAttemptsLast30Days,
    latestResults: candidateLinks.slice(0, 5).map((link) => ({
      attemptId: link.attemptId,
      candidateName: link.candidateName,
      simulationOrJourneyName: link.simulationName,
      status: link.status,
      date: link.createdAt,
      result: null,
      actionLabel: link.status === "completed" ? "Ver detalhes" : "Acompanhar",
      actionRoute: link.status === "completed" ? "/results" : "/candidate-links/new",
    })),
    journeys: journeys.slice(0, 5).map((journey) => ({
      id: journey.id,
      name: journey.name,
      status: journey.status,
      candidatesInProgress: 0,
      actionLabel: journey.status === "DRAFT" ? "Continuar edição" : "Ver jornada",
      actionRoute: "/assessment-journeys",
    })),
    integrations: integrationStatusesFromTokens(integrationTokens),
    billing: {
      plan: "ENTERPRISE",
      status: "ATIVO",
      creditBalance: 0,
      usedInPeriod: completedAttemptsLast30Days,
      subscriptionStatus: null,
      nextRenewalAt: null,
      commercialCondition: "Sob contrato",
    },
    recommendedActions: dashboardFallbackActions(
      simulations.length,
      journeys.filter((journey) => journey.status === "DRAFT").length,
      integrationTokens,
      completedAttemptsLast30Days,
    ),
  };
}

function integrationStatusesFromTokens(
  tokens: IntegrationTokenResponse[],
): DashboardIntegrationStatusItem[] {
  const configured = new Set(tokens.filter((token) => token.configured).map((token) => token.provider));
  return [
    {
      provider: "GUPY",
      name: "Gupy",
      status: configured.has("gupy") ? "CONECTADA" : "NAO_CONFIGURADA",
      lastSyncAt: null,
      action: configured.has("gupy") ? null : "CONFIGURAR",
    },
    {
      provider: "RECRUTEI",
      name: "Recrutei",
      status: configured.has("recrutei") ? "CONECTADA" : "NAO_CONFIGURADA",
      lastSyncAt: null,
      action: configured.has("recrutei") ? null : "CONFIGURAR",
    },
    {
      provider: "CUSTOM_API",
      name: "API personalizada",
      status: "DESATIVADA",
      lastSyncAt: null,
      action: "CONFIGURAR",
    },
  ];
}

function dashboardFallbackActions(
  simulationCount: number,
  draftJourneyCount: number,
  integrationTokens: IntegrationTokenResponse[],
  completedAttemptsLast30Days: number,
): DashboardResponse["recommendedActions"] {
  const actions: DashboardResponse["recommendedActions"] = [];
  if (simulationCount === 0) {
    actions.push({
      type: "CREATE_FIRST_SIMULATION",
      title: "Crie sua primeira avaliação",
      description: "Você ainda não possui avaliações cadastradas para gerar links de candidatos.",
      severity: "info",
      buttonLabel: "Criar avaliação",
      route: "/simulations/new",
    });
  }
  if (draftJourneyCount > 0) {
    actions.push({
      type: "PUBLISH_DRAFT_JOURNEY",
      title: `Você possui ${draftJourneyCount} jornada${draftJourneyCount === 1 ? "" : "s"} em rascunho`,
      description: "Publique as jornadas prontas para permitir convites de candidatos.",
      severity: "warning",
      buttonLabel: "Publicar jornada",
      route: "/assessment-journeys",
    });
  }
  if (!integrationTokens.some((token) => token.configured)) {
    actions.push({
      type: "CONFIGURE_INTEGRATION",
      title: "Configure integrações",
      description: "Conecte provedores externos quando quiser automatizar convites e resultados.",
      severity: "warning",
      buttonLabel: "Configurar integração",
      route: "/integrations",
    });
  }
  actions.push({
    type: "VIEW_RESULTS",
    title: "Sua operação está ativa",
    description: `${completedAttemptsLast30Days} avaliações concluídas nos últimos 30 dias.`,
    severity: "success",
    buttonLabel: "Ver resultados",
    route: "/results",
  });
  return actions.slice(0, 4);
}

export interface ResultsListFilters {
  search?: string;
  simulationId?: string;
  status?: AttemptStatus;
  integrationProvider?: string;
  periodStart?: string;
  periodEnd?: string;
  page?: number;
  size?: number;
}

export interface ResultListItemResponse {
  attemptId: string;
  candidateName: string;
  candidateEmail: string;
  simulationId: string;
  simulationTitle: string;
  status: AttemptStatus;
  startedAt: string | null;
  finishedAt: string | null;
  overallScore: number | null;
  highlightCompetency: string | null;
  integrationProvider: string | null;
}

export interface ResultsSummaryResponse {
  completed: number;
  inProgress: number;
  expired: number;
  averageScore: number | null;
}

export interface ResultsPageResponse {
  items: ResultListItemResponse[];
  summary: ResultsSummaryResponse;
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface ResultDetailResponse {
  attemptId: string;
  candidate: {
    name: string;
    email: string;
    externalId: string | null;
  };
  simulation: {
    id: string;
    title: string;
    versionNumber: number | null;
  };
  status: AttemptStatus;
  startedAt: string | null;
  finishedAt: string | null;
  overallScore: number | null;
  competencies: Array<{
    name: string;
    score: number;
    level: "ALTO" | "MEDIO" | "BAIXO" | string;
    summary: string;
  }>;
  answers: Array<{
    stepTitle: string;
    question: string;
    answer: string | null;
    score: number | null;
  }>;
  humanDecision: {
    status: string | null;
    decidedBy: string | null;
    decidedAt: string | null;
    note: string | null;
  };
}

export interface RegisterResultDecisionRequest {
  decision: HumanDecision;
  note?: string;
}

export async function listResults(filters: ResultsListFilters = {}) {
  const params = new URLSearchParams();
  if (filters.search) params.set("search", filters.search);
  if (filters.simulationId) params.set("simulationId", filters.simulationId);
  if (filters.status) params.set("status", filters.status);
  if (filters.integrationProvider) params.set("integrationProvider", filters.integrationProvider);
  if (filters.periodStart) params.set("periodStart", filters.periodStart);
  if (filters.periodEnd) params.set("periodEnd", filters.periodEnd);
  params.set("page", String(filters.page ?? 0));
  params.set("size", String(filters.size ?? 20));
  try {
    return await request<ResultsPageResponse>(`/api/v1/results?${params.toString()}`);
  } catch (error) {
    if (error instanceof PraxisApiError && error.status === 404) {
      return listResultsFallback(filters);
    }
    throw error;
  }
}

export async function getResultDetail(attemptId: string) {
  try {
    return await request<ResultDetailResponse>(`/api/v1/results/${encodeURIComponent(attemptId)}`);
  } catch (error) {
    if (error instanceof PraxisApiError && error.status === 404) {
      return getResultDetailFallback(attemptId);
    }
    throw error;
  }
}

export async function registerResultDecision(attemptId: string, body: RegisterResultDecisionRequest) {
  try {
    return await request<void>(`/api/v1/results/${encodeURIComponent(attemptId)}/decision`, {
      method: "POST",
      body: JSON.stringify(body),
    });
  } catch (error) {
    if (error instanceof PraxisApiError && error.status === 404) {
      return registerCandidateDisposition(attemptId, {
        decision: body.decision,
        reason: body.note,
      });
    }
    throw error;
  }
}

async function listResultsFallback(filters: ResultsListFilters): Promise<ResultsPageResponse> {
  const page = Math.max(filters.page ?? 0, 0);
  const size = Math.max(filters.size ?? 20, 1);
  const search = filters.search?.trim().toLowerCase();
  const periodStart = filters.periodStart ? new Date(filters.periodStart).getTime() : null;
  const periodEnd = filters.periodEnd ? new Date(filters.periodEnd).getTime() : null;
  const allItems = (await listCandidateLinks()).filter((link) => {
    if (search) {
      const haystack = `${link.candidateName} ${link.candidateEmail ?? ""}`.toLowerCase();
      if (!haystack.includes(search)) return false;
    }
    if (filters.simulationId && link.simulationId !== filters.simulationId) return false;
    if (filters.status && link.status !== filters.status) return false;
    if (filters.integrationProvider && filters.integrationProvider !== "MANUAL") return false;
    const createdAt = new Date(link.createdAt).getTime();
    if (periodStart != null && Number.isFinite(createdAt) && createdAt < periodStart) return false;
    if (periodEnd != null && Number.isFinite(createdAt) && createdAt > periodEnd) return false;
    return true;
  });
  const items = allItems
    .slice(page * size, page * size + size)
    .map<ResultListItemResponse>((link) => ({
      attemptId: link.attemptId,
      candidateName: link.candidateName,
      candidateEmail: link.candidateEmail ?? "",
      simulationId: link.simulationId,
      simulationTitle: link.simulationName,
      status: link.status,
      startedAt: null,
      finishedAt: link.status === "completed" ? link.createdAt : null,
      overallScore: null,
      highlightCompetency: null,
      integrationProvider: "MANUAL",
    }));
  const completed = allItems.filter((item) => item.status === "completed").length;
  const inProgress = allItems.filter((item) =>
    ["notStarted", "inProgress", "paused"].includes(item.status),
  ).length;
  const expired = allItems.filter((item) => item.status === "expired").length;

  return {
    items,
    summary: { completed, inProgress, expired, averageScore: null },
    page,
    size,
    totalItems: allItems.length,
    totalPages: Math.ceil(allItems.length / size),
  };
}

async function getResultDetailFallback(attemptId: string): Promise<ResultDetailResponse> {
  const report = await getEvidenceReport(attemptId);
  return {
    attemptId: report.attemptId,
    candidate: {
      name: report.candidateName,
      email: report.candidateEmail,
      externalId: null,
    },
    simulation: {
      id: report.simulationId,
      title: report.simulationName ?? "Avaliação",
      versionNumber: report.versionNumber,
    },
    status: report.finishedAt ? "completed" : "inProgress",
    startedAt: report.startedAt,
    finishedAt: report.finishedAt,
    overallScore: report.generalScore,
    competencies: report.competencies.map((competency) => ({
      name: competency.name,
      score: competency.score,
      level: resultLevel(competency.score),
      summary: competencySummary(competency.name, competency.score),
    })),
    answers: report.path.map((step) => ({
      stepTitle: `Situação ${step.turnIndex}`,
      question: step.prompt,
      answer: step.timedOut ? "Tempo esgotado" : step.answeredOptionText,
      score:
        Object.keys(step.competencyPoints).length === 0
          ? null
          : Object.values(step.competencyPoints).reduce((sum, value) => sum + value, 0),
    })),
    humanDecision: {
      status: report.humanDecision?.decision ?? null,
      decidedBy: report.humanDecision?.decidedByUserId ?? null,
      decidedAt: report.humanDecision?.decidedAt ?? null,
      note: report.humanDecision?.reason ?? null,
    },
  };
}

function resultLevel(score: number) {
  if (score >= 80) return "ALTO";
  if (score >= 60) return "MEDIO";
  return "BAIXO";
}

function competencySummary(name: string, score: number) {
  if (score >= 80) return `Demonstrou forte aderência em ${name}.`;
  if (score >= 60) return `Apresentou desempenho adequado, com espaço para aprofundar ${name}.`;
  return `Requer atenção e análise humana cuidadosa em ${name}.`;
}

export function getIntegrationsStatus() {
  return request<DashboardIntegrationStatusItem[]>("/api/v1/integrations/status");
}

export function deleteIntegrationToken(provider: IntegrationProvider) {
  return request<void>(`/api/v1/integrations/tokens/${encodeURIComponent(provider)}`, {
    method: "DELETE",
  });
}

export function listIntegrations() {
  return request<IntegrationCenterItem[]>("/api/v1/integrations");
}

export function configureIntegration(
  provider: IntegrationCenterProvider,
  body: ConfigureIntegrationRequest,
) {
  return request<IntegrationCenterItem>(
    `/api/v1/integrations/${encodeURIComponent(provider)}/configure`,
    {
      method: "POST",
      body: JSON.stringify(body),
    },
  );
}

export function disconnectIntegration(provider: IntegrationCenterProvider) {
  return request<IntegrationCenterItem>(
    `/api/v1/integrations/${encodeURIComponent(provider)}/disconnect`,
    { method: "POST" },
  );
}

export function syncIntegration(provider: IntegrationCenterProvider) {
  return request<IntegrationCenterItem>(
    `/api/v1/integrations/${encodeURIComponent(provider)}/sync`,
    { method: "POST" },
  );
}

export function getIntegration(provider: IntegrationCenterProvider) {
  return request<IntegrationCenterItem>(`/api/v1/integrations/${encodeURIComponent(provider)}`);
}

export function reactivateIntegration(provider: IntegrationCenterProvider) {
  return request<IntegrationCenterItem>(
    `/api/v1/integrations/${encodeURIComponent(provider)}/reactivate`,
    { method: "POST" },
  );
}

export function generateIntegrationToken(provider: IntegrationCenterProvider) {
  return request<GenerateIntegrationTokenResponse>(
    `/api/v1/integrations/${encodeURIComponent(provider)}/tokens`,
    { method: "POST" },
  );
}

export function rotateIntegrationProviderToken(provider: IntegrationCenterProvider) {
  return request<GenerateIntegrationTokenResponse>(
    `/api/v1/integrations/${encodeURIComponent(provider)}/tokens/rotate`,
    { method: "POST" },
  );
}

export function revokeIntegrationProviderToken(provider: IntegrationCenterProvider) {
  return request<void>(
    `/api/v1/integrations/${encodeURIComponent(provider)}/tokens`,
    { method: "DELETE" },
  );
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

export function listAssessmentJourneys() {
  return request<AssessmentJourneySummaryResponse[]>("/api/v1/assessment-journeys");
}

export function createAssessmentJourney(body: CreateAssessmentJourneyRequest) {
  return request<AssessmentJourneyDetailResponse>("/api/v1/assessment-journeys", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function getAssessmentJourney(journeyId: string) {
  return request<AssessmentJourneyDetailResponse>(
    `/api/v1/assessment-journeys/${encodeURIComponent(journeyId)}`,
  );
}

export function updateAssessmentJourney(journeyId: string, body: UpdateAssessmentJourneyRequest) {
  return request<AssessmentJourneyDetailResponse>(
    `/api/v1/assessment-journeys/${encodeURIComponent(journeyId)}`,
    { method: "PATCH", body: JSON.stringify(body) },
  );
}

export function addAssessmentJourneyStep(journeyId: string, body: AddJourneyStepRequest) {
  return request<AssessmentJourneyDetailResponse>(
    `/api/v1/assessment-journeys/${encodeURIComponent(journeyId)}/steps`,
    { method: "POST", body: JSON.stringify(body) },
  );
}

export function updateAssessmentJourneyStep(
  journeyId: string,
  stepId: number,
  body: UpdateJourneyStepRequest,
) {
  return request<AssessmentJourneyDetailResponse>(
    `/api/v1/assessment-journeys/${encodeURIComponent(journeyId)}/steps/${stepId}`,
    { method: "PATCH", body: JSON.stringify(body) },
  );
}

export function deleteAssessmentJourneyStep(journeyId: string, stepId: number) {
  return request<void>(
    `/api/v1/assessment-journeys/${encodeURIComponent(journeyId)}/steps/${stepId}`,
    { method: "DELETE" },
  );
}

export function publishAssessmentJourney(journeyId: string) {
  return request<AssessmentJourneyDetailResponse>(
    `/api/v1/assessment-journeys/${encodeURIComponent(journeyId)}/publish`,
    { method: "POST" },
  );
}

export function archiveAssessmentJourney(journeyId: string) {
  return request<AssessmentJourneyDetailResponse>(
    `/api/v1/assessment-journeys/${encodeURIComponent(journeyId)}/archive`,
    { method: "POST" },
  );
}

export function listAssessmentJourneyAttempts(journeyId: string) {
  return request<AssessmentJourneyAttemptResponse[]>(
    `/api/v1/assessment-journeys/${encodeURIComponent(journeyId)}/attempts`,
  );
}

export function createAssessmentJourneyAttempt(body: CreateJourneyAttemptRequest) {
  return request<AssessmentJourneyAttemptResponse>("/api/v1/assessment-journey-attempts", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function getAssessmentJourneyAttempt(attemptId: string) {
  return request<AssessmentJourneyAttemptResponse>(
    `/api/v1/assessment-journey-attempts/${encodeURIComponent(attemptId)}`,
  );
}

export function startAssessmentJourneyAttempt(attemptId: string) {
  return request<AssessmentJourneyAttemptResponse>(
    `/api/v1/assessment-journey-attempts/${encodeURIComponent(attemptId)}/start`,
    { method: "POST" },
  );
}

export function startAssessmentJourneyAttemptStep(attemptId: string, stepId: number) {
  return request<AssessmentJourneyAttemptResponse>(
    `/api/v1/assessment-journey-attempts/${encodeURIComponent(attemptId)}/steps/${stepId}/start`,
    { method: "POST" },
  );
}

export function completeAssessmentJourneyAttemptStep(attemptId: string, stepId: number) {
  return request<AssessmentJourneyAttemptResponse>(
    `/api/v1/assessment-journey-attempts/${encodeURIComponent(attemptId)}/steps/${stepId}/complete`,
    { method: "POST" },
  );
}

export function getAssessmentJourneyResult(attemptId: string) {
  return request<JourneyConsolidatedResultResponse>(
    `/api/v1/assessment-journey-attempts/${encodeURIComponent(attemptId)}/result`,
  );
}

export function getPublicAssessmentJourneyAttempt(attemptId: string) {
  return request<AssessmentJourneyAttemptResponse>(
    `/candidate/journey-attempts/${encodeURIComponent(attemptId)}`,
  );
}

export function startPublicAssessmentJourneyAttempt(attemptId: string) {
  return request<AssessmentJourneyAttemptResponse>(
    `/candidate/journey-attempts/${encodeURIComponent(attemptId)}/start`,
    { method: "POST" },
  );
}

export function startPublicAssessmentJourneyAttemptStep(attemptId: string, stepId: number) {
  return request<AssessmentJourneyAttemptResponse>(
    `/candidate/journey-attempts/${encodeURIComponent(attemptId)}/steps/${stepId}/start`,
    { method: "POST" },
  );
}

export function completePublicAssessmentJourneyAttemptStep(attemptId: string, stepId: number) {
  return request<AssessmentJourneyAttemptResponse>(
    `/candidate/journey-attempts/${encodeURIComponent(attemptId)}/steps/${stepId}/complete`,
    { method: "POST" },
  );
}

export function getPublicAssessmentJourneyResult(attemptId: string) {
  return request<JourneyConsolidatedResultResponse>(
    `/candidate/journey-attempts/${encodeURIComponent(attemptId)}/result`,
  );
}

export function deleteSimulation(simulationId: string) {
  return request<void>(`/api/v1/simulations/${encodeURIComponent(simulationId)}`, {
    method: "DELETE",
  });
}

export function createSimulationDraft(body: CreateSimulationDraftRequest) {
  return request<SimulationSummaryResponse>("/api/v1/simulations/drafts", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export type QuickStartCategory =
  | "ATENDIMENTO"
  | "LIDERANCA"
  | "VENDAS"
  | "COMPLIANCE"
  | "ONBOARDING";

export interface QuickStartTemplateSummaryResponse {
  category: QuickStartCategory;
  title: string;
  description: string;
  nodeCount: number;
}

export interface QuickStartCreatedResponse {
  simulationId: string;
  versionNumber: number;
  redirectTo: string;
}

export function getQuickStartTemplates() {
  return request<QuickStartTemplateSummaryResponse[]>("/api/v1/simulations/quick-start/templates");
}

export function createFromQuickStart(category: QuickStartCategory) {
  return request<QuickStartCreatedResponse>("/api/v1/simulations/quick-start", {
    method: "POST",
    body: JSON.stringify({ category }),
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

export function getCalibrationReport(simulationId: string, versionNumber: number) {
  return request<CalibrationReportResponse>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/calibration`,
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

// ---------------------------------------------------------------------------
// Painel administrativo da plataforma (perfil ADMIN)
// Cliente da plataforma = EmpresaEntity. Rotas sob /api/admin exigem papel ADMIN
// e recebem o empresa alvo explicitamente.
// ---------------------------------------------------------------------------

export type CommercialPlanType = "AVULSO" | "PROFISSIONAL" | "ENTERPRISE";
export type EmpresaStatus = "ATIVO" | "EM_TESTE" | "SUSPENSO" | "CANCELADO";
export type AdminUserStatus = "ATIVO" | "CONVIDADO" | "BLOQUEADO";

export interface EmpresaAdminSummary {
  empresaId: string;
  name: string;
  tradeName: string | null;
  taxId: string | null;
  corporateEmail: string | null;
  commercialPlanType: CommercialPlanType;
  status: EmpresaStatus;
  completedAttemptsInPeriod: number;
  createdAt: string;
}

export interface AdminUser {
  id: number;
  name: string;
  email: string;
  roles: string[];
  status: AdminUserStatus;
  lastLoginAt: string | null;
  createdAt: string | null;
}

export interface EmpresaAdminDetail {
  empresaId: string;
  name: string;
  tradeName: string | null;
  legalName: string | null;
  taxId: string | null;
  corporateEmail: string | null;
  phone: string | null;
  website: string | null;
  healthVertical: boolean;
  commercialPlanType: CommercialPlanType;
  commercialCondition: string | null;
  status: EmpresaStatus;
  completedAttemptsInPeriod: number;
  users: AdminUser[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateEmpresaAdminRequest {
  name: string;
  tradeName?: string | null;
  legalName?: string | null;
  taxId?: string | null;
  corporateEmail?: string | null;
  phone?: string | null;
  website?: string | null;
  healthVertical: boolean;
  companyId?: string | null;
  commercialPlanType: CommercialPlanType;
  commercialCondition?: string | null;
  initialStatus?: EmpresaStatus | null;
  responsibleName: string;
  responsibleEmail: string;
  sendInvite: boolean;
}

export interface UpdateEmpresaAdminRequest {
  name?: string | null;
  tradeName?: string | null;
  legalName?: string | null;
  taxId?: string | null;
  corporateEmail?: string | null;
  phone?: string | null;
  website?: string | null;
  healthVertical?: boolean | null;
  commercialPlanType?: CommercialPlanType | null;
  commercialCondition?: string | null;
}

export interface CreateEmpresaAdminResponse {
  empresa: EmpresaAdminDetail;
  responsibleUserId: number;
  inviteUrl: string | null;
}

export interface InviteUserAdminResponse {
  user: AdminUser;
  inviteUrl: string | null;
}

export interface EmpresaUsage {
  empresaId: string;
  periodStart: string;
  periodEnd: string;
  completedAttempts: number;
  completedAttemptsLast7Days: number;
  completedAttemptsLast30Days: number;
  completedAttemptsAllTime: number;
  lastCompletedAttemptAt: string | null;
}

export interface AdminAuditEvent {
  id: number;
  actorUserId: string | null;
  empresaId: string;
  aggregateType: string;
  aggregateId: string;
  eventType: string;
  message: string;
  metadata: string;
  createdAt: string;
}

export interface AdminDashboard {
  periodStart: string;
  periodEnd: string;
  totalEmpresas: number;
  activeEmpresas: number;
  trialEmpresas: number;
  suspendedEmpresas: number;
  canceledEmpresas: number;
  totalCompletedAttempts: number;
  topUsageEmpresas: { empresaId: string; name: string; completedAttempts: number }[];
  recentEmpresas: EmpresaAdminSummary[];
  attentionEmpresas: EmpresaAdminSummary[];
}

export function getAdminDashboard() {
  return request<AdminDashboard>("/api/admin/dashboard");
}

export function listAdminEmpresas(filters?: {
  search?: string;
  status?: EmpresaStatus;
  plan?: CommercialPlanType;
}) {
  const params = new URLSearchParams();
  if (filters?.search) params.set("search", filters.search);
  if (filters?.status) params.set("status", filters.status);
  if (filters?.plan) params.set("plan", filters.plan);
  const query = params.toString();
  return request<EmpresaAdminSummary[]>(`/api/admin/empresas${query ? `?${query}` : ""}`);
}

export function createAdminEmpresa(body: CreateEmpresaAdminRequest) {
  return request<CreateEmpresaAdminResponse>("/api/admin/empresas", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function getAdminEmpresa(empresaId: string) {
  return request<EmpresaAdminDetail>(`/api/admin/empresas/${encodeURIComponent(empresaId)}`);
}

export function updateAdminEmpresa(empresaId: string, body: UpdateEmpresaAdminRequest) {
  return request<EmpresaAdminDetail>(`/api/admin/empresas/${encodeURIComponent(empresaId)}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

export function suspendAdminEmpresa(empresaId: string, reason: string) {
  return request<EmpresaAdminDetail>(`/api/admin/empresas/${encodeURIComponent(empresaId)}/suspend`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

export function reactivateAdminEmpresa(
  empresaId: string,
  reason: string,
  targetStatus?: EmpresaStatus,
) {
  return request<EmpresaAdminDetail>(
    `/api/admin/empresas/${encodeURIComponent(empresaId)}/reactivate`,
    {
      method: "POST",
      body: JSON.stringify({ reason, targetStatus }),
    },
  );
}

export function cancelAdminEmpresa(empresaId: string, reason: string) {
  return request<EmpresaAdminDetail>(`/api/admin/empresas/${encodeURIComponent(empresaId)}/cancel`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

export function getAdminEmpresaUsage(empresaId: string) {
  return request<EmpresaUsage>(`/api/admin/empresas/${encodeURIComponent(empresaId)}/usage`);
}

export function getAdminEmpresaAudit(empresaId: string) {
  return request<AdminAuditEvent[]>(`/api/admin/empresas/${encodeURIComponent(empresaId)}/audit`);
}

export function listAdminEmpresaUsers(empresaId: string) {
  return request<AdminUser[]>(`/api/admin/empresas/${encodeURIComponent(empresaId)}/users`);
}

export function inviteAdminEmpresaUser(empresaId: string, body: { name: string; email: string }) {
  return request<InviteUserAdminResponse>(
    `/api/admin/empresas/${encodeURIComponent(empresaId)}/users/invite`,
    { method: "POST", body: JSON.stringify(body) },
  );
}

export function resendAdminEmpresaUserInvite(empresaId: string, userId: number) {
  return request<InviteUserAdminResponse>(
    `/api/admin/empresas/${encodeURIComponent(empresaId)}/users/${userId}/resend-invite`,
    { method: "POST" },
  );
}

export function blockAdminEmpresaUser(empresaId: string, userId: number) {
  return request<AdminUser>(
    `/api/admin/empresas/${encodeURIComponent(empresaId)}/users/${userId}/block`,
    { method: "POST" },
  );
}

export function unblockAdminEmpresaUser(empresaId: string, userId: number) {
  return request<AdminUser>(
    `/api/admin/empresas/${encodeURIComponent(empresaId)}/users/${userId}/unblock`,
    { method: "POST" },
  );
}

// ---------------------------------------------------------------------------
// Equipe — gerenciamento de usuários pelo próprio cliente
// ---------------------------------------------------------------------------

export type TeamUserStatus = "ATIVO" | "CONVIDADO" | "BLOQUEADO";

export interface TeamUser {
  id: number;
  name: string;
  email: string;
  roles: string[];
  status: TeamUserStatus;
  lastLoginAt: string | null;
  createdAt: string | null;
}

export interface InviteTeamUserResponse {
  user: TeamUser;
  inviteUrl: string;
}

export function listTeamUsers() {
  return request<TeamUser[]>("/api/v1/team");
}

export function inviteTeamUser(body: { name: string; email: string }) {
  return request<InviteTeamUserResponse>("/api/v1/team/invite", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function resendTeamUserInvite(userId: number) {
  return request<InviteTeamUserResponse>(`/api/v1/team/${userId}/resend-invite`, {
    method: "POST",
  });
}

export function blockTeamUser(userId: number) {
  return request<TeamUser>(`/api/v1/team/${userId}/block`, { method: "POST" });
}

export function unblockTeamUser(userId: number) {
  return request<TeamUser>(`/api/v1/team/${userId}/unblock`, { method: "POST" });
}

// ---------------------------------------------------------------------------
// Cobrança Mercado Pago (Parte B) — perfil ADMIN
// ---------------------------------------------------------------------------

export type SubscriptionStatus = "PENDING" | "AUTHORIZED" | "DELINQUENT" | "PAUSED" | "CANCELLED";

export interface SubscriptionPlan {
  id: number;
  code: string;
  name: string;
  planType: CommercialPlanType;
  priceCents: number;
  currency: string;
  creditAmount: number | null;
}

export interface BillingEvent {
  id: number;
  eventType: string;
  mpResourceType: string | null;
  mpResourceId: string | null;
  mpStatus: string | null;
  amountCents: number | null;
  currency: string | null;
  createdAt: string;
}

export interface EmpresaBillingOverview {
  empresaId: string;
  commercialPlanType: CommercialPlanType;
  status: EmpresaStatus;
  creditBalance: number;
  subscription: {
    id: number;
    status: SubscriptionStatus;
    mpPreapprovalId: string | null;
    initPoint: string | null;
    currentPeriodEnd: string | null;
    lastPaymentAt: string | null;
    graceUntil: string | null;
  } | null;
  events: BillingEvent[];
}

export interface CheckoutResult {
  kind: string;
  mpResourceId: string | null;
  initPoint: string | null;
  externalReference: string;
}

export function listBillingPlans() {
  return request<SubscriptionPlan[]>("/api/admin/billing/plans");
}

export function getEmpresaBilling(empresaId: string) {
  return request<EmpresaBillingOverview>(
    `/api/admin/empresas/${encodeURIComponent(empresaId)}/billing`,
  );
}

export function createCreditCheckout(empresaId: string, planId: number) {
  return request<CheckoutResult>(
    `/api/admin/empresas/${encodeURIComponent(empresaId)}/billing/credits/checkout?planId=${planId}`,
    { method: "POST" },
  );
}

export function createEmpresaSubscription(empresaId: string, planId: number) {
  return request<CheckoutResult>(
    `/api/admin/empresas/${encodeURIComponent(empresaId)}/billing/subscription?planId=${planId}`,
    { method: "POST" },
  );
}

export function syncEmpresaBilling(empresaId: string, resourceType: string, resourceId: string) {
  return request<EmpresaBillingOverview>(
    `/api/admin/empresas/${encodeURIComponent(empresaId)}/billing/sync`,
    { method: "POST", body: JSON.stringify({ resourceType, resourceId }) },
  );
}

// ---------------------------------------------------------------------------
// Billing · Cliente (GET /api/v1/billing)
// ---------------------------------------------------------------------------

export type FinancialStatus =
  | "REGULAR"
  | "PENDENTE_PAGAMENTO"
  | "INADIMPLENTE"
  | "SEM_CREDITO"
  | "CANCELADO";

export type BillingAction =
  | "BUY_CREDITS"
  | "VIEW_HISTORY"
  | "VIEW_SUBSCRIPTION"
  | "UPDATE_PAYMENT"
  | "CONTACT_SUPPORT";

export interface ClientBillingUsage {
  completedLast7Days: number;
  completedLast30Days: number;
  completedAllTime: number;
}

export interface ClientBillingSubscription {
  status: SubscriptionStatus;
  currentPeriodEnd: string | null;
  lastPaymentAt: string | null;
  graceUntil: string | null;
}

export interface ClientBillingResponse {
  empresaId: string;
  plan: CommercialPlanType;
  empresaStatus: EmpresaStatus;
  financialStatus: FinancialStatus;
  creditBalance: number;
  usage: ClientBillingUsage;
  subscription: ClientBillingSubscription | null;
  availableActions: BillingAction[];
  events: BillingEvent[];
}

export function getClientBilling() {
  return request<ClientBillingResponse>("/api/v1/billing");
}

export type AcceptInviteRequest = {
  token: string;
  newPassword: string;
  confirmPassword: string;
};

export type LoginResponse = {
  token: string;
  userId: number;
  empresaId: string;
  name: string;
  roles: string[];
};

export function acceptInvite(requestBody: AcceptInviteRequest) {
  return request<LoginResponse>("/api/v1/auth/invite/accept", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export type ForgotPasswordRequest = {
  // EMPRESA informa empresaId + email; ADMIN informa apenas email (empresa PLATFORM).
  empresaId?: string;
  email: string;
};

export type ResetPasswordTokenResponse = {
  valid: boolean;
  expiresAt: string;
  userName: string;
};

export type ResetPasswordRequest = {
  token: string;
  newPassword: string;
  confirmPassword: string;
};

/**
 * Solicita o e-mail de recuperação de senha. A resposta é sempre a mesma, independentemente
 * de a conta existir, para nunca revelar a existência de usuários, e-mails ou empresas.
 */
export function requestPasswordReset(requestBody: ForgotPasswordRequest) {
  return request<{ message: string }>("/api/v1/auth/password/forgot", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

/** Valida o token recebido no link antes de coletar a nova senha. */
export function validatePasswordResetToken(token: string) {
  return request<ResetPasswordTokenResponse>(
    `/api/v1/auth/password/reset/${encodeURIComponent(token)}`,
  );
}

/** Conclui a redefinição de senha a partir do token. */
export function resetPassword(requestBody: ResetPasswordRequest) {
  return request<{ message: string }>("/api/v1/auth/password/reset", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}
