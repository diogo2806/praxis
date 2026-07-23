import type {
  CandidateAttemptContract,
  SubmitAnswerRequestContract,
  SubmitAnswerResponseContract,
} from "@/lib/api/candidate-attempt-contract";
import { getCandidateIntegritySession } from "@/lib/api/candidate-integrity";
import { apiRequest } from "@/lib/api/http";

const INTEGRITY_SESSION_HEADER = "X-Praxis-Integrity-Session";
export const HEALTH_CONSENT_VERSION = "2026-06-01";

export interface HealthConsentStatusResponse {
  healthVertical: boolean;
  required: boolean;
  valid: boolean;
  noticeVersion: string | null;
}

export type CandidateAttemptResponse = CandidateAttemptContract & {
  healthConsentValid: boolean;
  healthConsentNoticeVersion: string | null;
};

export function getHealthConsentStatus(token: string): Promise<HealthConsentStatusResponse> {
  return apiRequest<HealthConsentStatusResponse>(
    `/candidate/attempts/${encodeURIComponent(token)}/health-consent`,
    undefined,
    {
      authenticated: false,
      fallbackMessage: "Não foi possível verificar o consentimento de saúde.",
    },
  );
}

export async function getCandidateAttempt(token: string): Promise<CandidateAttemptResponse> {
  const consentStatus = await getHealthConsentStatus(token);
  if (consentStatus.required && !consentStatus.valid) {
    return pendingHealthConsentAttempt(consentStatus);
  }

  const attempt = await apiRequest<CandidateAttemptContract>(
    `/candidate/attempts/${encodeURIComponent(token)}`,
    { headers: integrityHeaders(token) },
    {
      authenticated: false,
      fallbackMessage: "Não foi possível carregar a avaliação.",
    },
  );
  return {
    ...attempt,
    healthConsentValid: consentStatus.valid,
    healthConsentNoticeVersion: consentStatus.noticeVersion,
  };
}

export function recordHealthConsent(
  token: string,
  onBehalfOfMinor = false,
  version = HEALTH_CONSENT_VERSION,
): Promise<void> {
  return apiRequest<void>(
    `/candidate/attempts/${encodeURIComponent(token)}/health-consent`,
    {
      method: "POST",
      body: JSON.stringify({ version, onBehalfOfMinor }),
    },
    {
      authenticated: false,
      fallbackMessage: "Não foi possível registrar o consentimento de saúde.",
    },
  );
}

export function revokeHealthConsent(token: string): Promise<void> {
  return apiRequest<void>(
    `/candidate/attempts/${encodeURIComponent(token)}/health-consent`,
    { method: "DELETE" },
    {
      authenticated: false,
      fallbackMessage: "Não foi possível revogar o consentimento de saúde.",
    },
  );
}

export function submitCandidateAnswer(
  token: string,
  body: SubmitAnswerRequestContract,
): Promise<SubmitAnswerResponseContract> {
  return apiRequest<SubmitAnswerResponseContract>(
    `/candidate/attempts/${encodeURIComponent(token)}/answers`,
    {
      method: "POST",
      headers: integrityHeaders(token),
      body: JSON.stringify(body),
    },
    {
      authenticated: false,
      fallbackMessage: "Não foi possível registrar a resposta.",
    },
  );
}

export type SubmitAnswerRequest = SubmitAnswerRequestContract;
export type SubmitAnswerResponse = SubmitAnswerResponseContract;

function pendingHealthConsentAttempt(status: HealthConsentStatusResponse): CandidateAttemptResponse {
  return {
    participacaoId: "health-consent-pending",
    avaliacaoNome: "",
    status: "nao_iniciada",
    finalizado: false,
    redirectUrl: null,
    acaoSugeridaFrontend: "INICIAR",
    progresso: { passoAtual: 1, passosEstimados: 1, percentual: 0 },
    etapaAtual: {
      id: "health-consent",
      numero: 1,
      pessoa: "Participante",
      descricao: "",
      tempoLimiteSegundos: null,
      alternativas: [],
    },
    verticalSaude: status.healthVertical,
    healthConsentValid: false,
    healthConsentNoticeVersion: status.noticeVersion,
  };
}

function integrityHeaders(token: string): HeadersInit {
  const sessionId = getCandidateIntegritySession(token);
  return sessionId ? { [INTEGRITY_SESSION_HEADER]: sessionId } : {};
}
