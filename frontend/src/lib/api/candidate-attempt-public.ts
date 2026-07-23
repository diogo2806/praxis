import { apiRequest } from "@/lib/api/http";
import { getCandidateIntegritySession } from "@/lib/api/candidate-integrity";
import {
  PraxisApiError,
  type CandidateAttemptResponse,
  type CandidateNodeResponse,
  type SubmitAnswerRequest,
  type SubmitAnswerResponse,
} from "@/lib/api/praxis-legacy";

const INTEGRITY_SESSION_HEADER = "X-Praxis-Integrity-Session";
const HEALTH_CONSENT_REQUIRED_STATUS = 428;

export async function getCandidateAttempt(token: string): Promise<CandidateAttemptResponse> {
  try {
    return await apiRequest<CandidateAttemptResponse>(
      `/candidate/attempts/${encodeURIComponent(token)}`,
      {
        headers: integrityHeaders(token),
      },
      {
        authenticated: false,
        fallbackMessage: "Não foi possível carregar a avaliação.",
      },
    );
  } catch (error) {
    if (error instanceof PraxisApiError && error.status === HEALTH_CONSENT_REQUIRED_STATUS) {
      return healthConsentRequiredAttempt(token);
    }
    throw error;
  }
}

export function submitCandidateAnswer(
  token: string,
  body: SubmitAnswerRequest,
): Promise<SubmitAnswerResponse> {
  return apiRequest<SubmitAnswerResponse>(
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

function healthConsentRequiredAttempt(token: string): CandidateAttemptResponse {
  const consentGateStage: CandidateNodeResponse = {
    id: "health-consent-required",
    numero: 0,
    pessoa: "",
    descricao: "",
    tempoLimiteSegundos: null,
    alternativas: [],
  };

  return {
    participacaoId: token,
    avaliacaoNome: "Avaliação",
    status: "nao_iniciada",
    finalizado: false,
    redirectUrl: null,
    acaoSugeridaFrontend: "INICIAR",
    progresso: {
      passoAtual: 0,
      passosEstimados: 0,
      percentual: 0,
    },
    etapaAtual: consentGateStage,
    verticalSaude: true,
  };
}

function integrityHeaders(token: string): HeadersInit {
  const sessionId = getCandidateIntegritySession(token);
  return sessionId ? { [INTEGRITY_SESSION_HEADER]: sessionId } : {};
}
