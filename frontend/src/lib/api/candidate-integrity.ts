import { apiRequest } from "@/lib/api/http";
import { getApiBaseUrl } from "@/lib/runtime-config";

export type CandidateIntegrityInputMode = "POINTER" | "TOUCH" | "KEYBOARD" | "UNKNOWN";

export type CandidateIntegrityEventType =
  | "TAB_HIDDEN"
  | "TAB_VISIBLE"
  | "INPUT_MODE_CHANGED"
  | "NODE_PRESENTED"
  | "ASSET_LOADED"
  | "STIMULUS_STARTED"
  | "RESPONSE_SELECTED"
  | "RESPONSE_CONFIRMED";

export interface CandidateIntegritySessionResponse {
  sessionId: string;
  resumed: boolean;
  heartbeatIntervalSeconds: number;
  expiresAfterSeconds: number;
}

export interface CandidateIntegrityEventRequest {
  sessionId: string;
  eventType: CandidateIntegrityEventType;
  occurredAt: string;
  inputMode?: CandidateIntegrityInputMode | null;
  visibilityState?: "VISIBLE" | "HIDDEN" | null;
  sequenceNumber?: number | null;
  detail?: "IMAGE" | "AUDIO" | "OTHER" | null;
}

function integrityPath(token: string, action: string): string {
  return `/candidate/attempts/${encodeURIComponent(token)}/integrity/${action}`;
}

export function startCandidateIntegritySession(
  token: string,
  clientSessionId: string,
  inputMode: CandidateIntegrityInputMode,
): Promise<CandidateIntegritySessionResponse> {
  return apiRequest<CandidateIntegritySessionResponse>(
    integrityPath(token, "session"),
    {
      method: "POST",
      body: JSON.stringify({
        clientSessionId,
        occurredAt: new Date().toISOString(),
        inputMode,
      }),
    },
    {
      authenticated: false,
      fallbackMessage: (status) =>
        status === 409
          ? "Esta avaliação já está aberta em outra sessão. Feche a outra janela ou aguarde alguns instantes."
          : "Não foi possível iniciar a sessão segura da avaliação.",
    },
  );
}

export function sendCandidateIntegrityHeartbeat(token: string, sessionId: string): Promise<void> {
  return apiRequest<void>(
    integrityPath(token, "heartbeat"),
    {
      method: "POST",
      body: JSON.stringify({ sessionId, occurredAt: new Date().toISOString() }),
    },
    { authenticated: false, fallbackMessage: "A sessão da avaliação não pôde ser renovada." },
  );
}

export function recordCandidateIntegrityEvent(
  token: string,
  event: CandidateIntegrityEventRequest,
): Promise<void> {
  return apiRequest<void>(
    integrityPath(token, "events"),
    { method: "POST", body: JSON.stringify(event) },
    { authenticated: false, fallbackMessage: "O evento técnico não pôde ser registrado." },
  );
}

export function closeCandidateIntegritySession(token: string, sessionId: string): Promise<void> {
  return apiRequest<void>(
    integrityPath(token, "close"),
    {
      method: "POST",
      body: JSON.stringify({ sessionId, occurredAt: new Date().toISOString() }),
    },
    { authenticated: false, fallbackMessage: "A sessão técnica não pôde ser encerrada." },
  );
}

export function closeCandidateIntegritySessionKeepalive(token: string, sessionId: string): void {
  void fetch(`${getApiBaseUrl()}${integrityPath(token, "close")}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({ sessionId, occurredAt: new Date().toISOString() }),
    keepalive: true,
  });
}
