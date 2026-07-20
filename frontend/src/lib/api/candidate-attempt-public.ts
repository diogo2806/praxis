import { apiRequest } from "@/lib/api/http";
import { getCandidateIntegritySession } from "@/lib/api/candidate-integrity";
import type {
  CandidateAttemptResponse,
  SubmitAnswerRequest,
  SubmitAnswerResponse,
} from "@/lib/api/praxis-legacy";

const INTEGRITY_SESSION_HEADER = "X-Praxis-Integrity-Session";

export function getCandidateAttempt(token: string): Promise<CandidateAttemptResponse> {
  return apiRequest<CandidateAttemptResponse>(
    `/candidate/attempts/${encodeURIComponent(token)}`,
    {
      headers: integrityHeaders(token),
    },
    {
      authenticated: false,
      fallbackMessage: "Não foi possível carregar a avaliação.",
    },
  );
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

function integrityHeaders(token: string): HeadersInit {
  const sessionId = getCandidateIntegritySession(token);
  return sessionId ? { [INTEGRITY_SESSION_HEADER]: sessionId } : {};
}
