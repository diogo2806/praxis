import {
  submitCandidateAnswer,
  type CandidateAttemptResponse,
  type CandidateNodeResponse,
  type SubmitAnswerRequest,
} from "@/lib/api/praxis";

const STORAGE_PREFIX = "praxis.candidateOffline.";

export interface OfflineAnswer {
  id: string;
  etapaId?: string | null;
  etapaNumero: number;
  respostaId?: string | null;
  respondidaEm: string;
  tempoEsgotado: boolean;
  createdAt: string;
}

interface CandidateOfflineState {
  attempt: CandidateAttemptResponse | null;
  pendingAnswers: OfflineAnswer[];
}

export interface FlushCandidateAnswersResult {
  attempt: CandidateAttemptResponse | null;
  submitted: number;
  pending: number;
}

export function loadCandidateOfflineState(token: string): CandidateOfflineState {
  if (typeof window === "undefined") {
    return emptyState();
  }

  try {
    const raw = window.localStorage.getItem(storageKey(token));
    if (!raw) return emptyState();
    const parsed = JSON.parse(raw) as Partial<CandidateOfflineState>;
    return {
      attempt: parsed.attempt ?? null,
      pendingAnswers: Array.isArray(parsed.pendingAnswers) ? parsed.pendingAnswers : [],
    };
  } catch {
    return emptyState();
  }
}

export function saveCandidateAttemptSnapshot(token: string, attempt: CandidateAttemptResponse) {
  const state = loadCandidateOfflineState(token);
  writeCandidateOfflineState(token, {
    ...state,
    attempt: mergeAttemptSnapshot(attempt, state.attempt),
  });
}

export function enqueueCandidateAnswer(token: string, answer: Omit<OfflineAnswer, "id" | "createdAt">) {
  const state = loadCandidateOfflineState(token);
  const duplicate = state.pendingAnswers.some(
    (pending) =>
      pending.etapaId === answer.etapaId &&
      pending.etapaNumero === answer.etapaNumero &&
      pending.respostaId === answer.respostaId &&
      pending.tempoEsgotado === answer.tempoEsgotado,
  );

  if (duplicate) {
    return state.pendingAnswers.length;
  }

  const nextAnswer: OfflineAnswer = {
    ...answer,
    id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    createdAt: new Date().toISOString(),
  };
  writeCandidateOfflineState(token, {
    ...state,
    pendingAnswers: [...state.pendingAnswers, nextAnswer],
  });
  requestBackgroundSync();
  return state.pendingAnswers.length + 1;
}

export async function flushCandidateAnswers(token: string): Promise<FlushCandidateAnswersResult> {
  let state = loadCandidateOfflineState(token);
  let submitted = 0;
  let latestAttempt = state.attempt;

  while (state.pendingAnswers.length > 0) {
    const [answer, ...remaining] = state.pendingAnswers;
    const response = await submitCandidateAnswer(token, toSubmitRequest(answer));
    latestAttempt = responseToAttempt(response, latestAttempt);
    state = {
      attempt: mergeAttemptSnapshot(latestAttempt, state.attempt),
      pendingAnswers: remaining,
    };
    writeCandidateOfflineState(token, state);
    submitted += 1;
  }

  return {
    attempt: latestAttempt,
    submitted,
    pending: state.pendingAnswers.length,
  };
}

export function buildOptimisticAttempt(
  attempt: CandidateAttemptResponse,
  currentNode: CandidateNodeResponse,
  optionId: string | null,
  timedOut: boolean,
): CandidateAttemptResponse {
  const nextNodeId = timedOut
    ? currentNode.proximaEtapaTempoEsgotadoId
    : currentNode.alternativas.find((option) => option.id === optionId)?.proximaEtapaId;
  const nextNode = findOfflineNode(attempt, nextNodeId);
  const finalizado = !nextNode;
  const passoAtual = finalizado
    ? Math.max(attempt.progresso.passosEstimados, currentNode.numero)
    : nextNode.numero;

  return {
    ...attempt,
    status: finalizado ? "concluida" : "em_andamento",
    finalizado,
    progresso: {
      passoAtual,
      passosEstimados: Math.max(attempt.progresso.passosEstimados, passoAtual),
      percentual: finalizado
        ? 100
        : Math.min(99, Math.max(attempt.progresso.percentual, Math.round((passoAtual * 100) / Math.max(1, attempt.progresso.passosEstimados)))),
    },
    etapaAtual: nextNode,
  };
}

export function getPendingCandidateAnswerCount(token: string) {
  return loadCandidateOfflineState(token).pendingAnswers.length;
}

function responseToAttempt(
  response: {
    participacaoId: string;
    status: CandidateAttemptResponse["status"];
    finalizado: boolean;
    progresso: CandidateAttemptResponse["progresso"];
    etapaAtual: CandidateAttemptResponse["etapaAtual"];
  },
  previous: CandidateAttemptResponse | null,
): CandidateAttemptResponse {
  return {
    participacaoId: response.participacaoId,
    avaliacaoNome: previous?.avaliacaoNome ?? "Praxis",
    status: response.status,
    finalizado: response.finalizado,
    acaoSugeridaFrontend: previous?.acaoSugeridaFrontend,
    progresso: response.progresso,
    etapaAtual: response.etapaAtual,
    etapasOffline: previous?.etapasOffline,
  };
}

function mergeAttemptSnapshot(
  attempt: CandidateAttemptResponse,
  previous: CandidateAttemptResponse | null,
): CandidateAttemptResponse {
  return {
    ...attempt,
    etapasOffline: attempt.etapasOffline?.length ? attempt.etapasOffline : previous?.etapasOffline,
  };
}

function findOfflineNode(attempt: CandidateAttemptResponse, nodeId?: string | null) {
  if (!nodeId) return null;
  return attempt.etapasOffline?.find((node) => node.id === nodeId) ?? null;
}

function toSubmitRequest(answer: OfflineAnswer): SubmitAnswerRequest {
  return {
    etapaId: answer.etapaId,
    etapaNumero: answer.etapaNumero,
    respostaId: answer.respostaId,
    respondidaEm: answer.respondidaEm,
    tempoEsgotado: answer.tempoEsgotado,
  };
}

function writeCandidateOfflineState(token: string, state: CandidateOfflineState) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(storageKey(token), JSON.stringify(state));
}

function requestBackgroundSync() {
  if (typeof window === "undefined") return;
  if (window.navigator.serviceWorker?.controller) {
    window.navigator.serviceWorker.ready
      .then((readyRegistration) => {
        const syncRegistration = readyRegistration as ServiceWorkerRegistration & {
          sync?: { register: (tag: string) => Promise<void> };
        };
        return syncRegistration.sync?.register("praxis-sync-candidate-answers");
      })
      .catch(() => undefined);
  }
}

function storageKey(token: string) {
  return `${STORAGE_PREFIX}${token}`;
}

function emptyState(): CandidateOfflineState {
  return {
    attempt: null,
    pendingAnswers: [],
  };
}
