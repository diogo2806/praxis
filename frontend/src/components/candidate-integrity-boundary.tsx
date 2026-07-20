import { useEffect, useRef, useState, type ReactNode } from "react";
import {
  clearCandidateIntegritySession,
  closeCandidateIntegritySession,
  closeCandidateIntegritySessionKeepalive,
  recordCandidateIntegrityEvent,
  sendCandidateIntegrityHeartbeat,
  setCandidateIntegritySession,
  startCandidateIntegritySession,
  type CandidateIntegrityEventType,
  type CandidateIntegrityInputMode,
} from "@/lib/api/candidate-integrity";
import { PraxisApiError } from "@/lib/api/praxis";
import { useLanguage } from "@/lib/language-context";

const CLIENT_SESSION_PREFIX = "praxis:integrity-session:";

type BoundaryState = "starting" | "active" | "blocked" | "error";
type ErrorReason = "start" | "connection";

type CandidateIntegrityBoundaryProps = {
  token: string;
  children: ReactNode;
};

const integrityCopy = {
  "pt-BR": {
    preparingLabel: "Preparando",
    preparingTitle: "Validando a sessão da avaliação.",
    preparingDescription: "Preparando uma sessão segura para a avaliação.",
    blockedLabel: "Sessão já aberta",
    blockedTitle: "A avaliação está aberta em outra sessão.",
    blockedDescription:
      "Feche a outra janela ou aguarde alguns instantes para que a sessão anterior expire.",
    errorLabel: "Conexão interrompida",
    errorTitle: "Não foi possível manter a sessão da avaliação.",
    startErrorDescription:
      "Não foi possível iniciar a sessão segura. Verifique a conexão e tente novamente.",
    connectionErrorDescription:
      "A conexão com a sessão foi interrompida. Verifique a internet e tente retomar.",
    retry: "Tentar retomar",
    notice:
      "A verificação é técnica, não altera sua pontuação e não toma decisão automática sobre sua candidatura.",
  },
  en: {
    preparingLabel: "Preparing",
    preparingTitle: "Validating the assessment session.",
    preparingDescription: "Preparing a secure session for the assessment.",
    blockedLabel: "Session already open",
    blockedTitle: "The assessment is open in another session.",
    blockedDescription:
      "Close the other window or wait a moment for the previous session to expire.",
    errorLabel: "Connection interrupted",
    errorTitle: "The assessment session could not be maintained.",
    startErrorDescription:
      "The secure session could not be started. Check your connection and try again.",
    connectionErrorDescription:
      "The session connection was interrupted. Check your internet connection and try to resume.",
    retry: "Try to resume",
    notice:
      "This is a technical check. It does not change your score or make an automated decision about your application.",
  },
  "es-MX": {
    preparingLabel: "Preparando",
    preparingTitle: "Validando la sesión de la evaluación.",
    preparingDescription: "Preparando una sesión segura para la evaluación.",
    blockedLabel: "Sesión ya abierta",
    blockedTitle: "La evaluación está abierta en otra sesión.",
    blockedDescription:
      "Cierra la otra ventana o espera unos instantes para que la sesión anterior expire.",
    errorLabel: "Conexión interrumpida",
    errorTitle: "No fue posible mantener la sesión de la evaluación.",
    startErrorDescription:
      "No fue posible iniciar la sesión segura. Revisa la conexión e inténtalo de nuevo.",
    connectionErrorDescription:
      "La conexión con la sesión se interrumpió. Revisa tu conexión a internet e intenta retomarla.",
    retry: "Intentar retomar",
    notice:
      "La verificación es técnica, no cambia tu puntuación ni toma una decisión automática sobre tu candidatura.",
  },
} as const;

export function CandidateIntegrityBoundary({ token, children }: CandidateIntegrityBoundaryProps) {
  const { language } = useLanguage();
  const copy = integrityCopy[language];
  const [state, setState] = useState<BoundaryState>("starting");
  const [errorReason, setErrorReason] = useState<ErrorReason>("start");
  const [retryNonce, setRetryNonce] = useState(0);
  const sessionIdRef = useRef<string | null>(null);
  const inputModeRef = useRef<CandidateIntegrityInputMode>("UNKNOWN");
  const sequenceRef = useRef(0);
  const closedRef = useRef(false);

  useEffect(() => {
    let disposed = false;
    let heartbeatId: number | undefined;
    let mutationObserver: MutationObserver | undefined;
    let heartbeatFailures = 0;
    let lastPresentedStage = "";

    setState("starting");
    setErrorReason("start");
    sessionIdRef.current = null;
    sequenceRef.current = 0;
    closedRef.current = false;

    const clientSessionId = getOrCreateClientSessionId(token);

    const record = (
      eventType: CandidateIntegrityEventType,
      detail?: "IMAGE" | "AUDIO" | "OTHER",
      inputMode: CandidateIntegrityInputMode | null = inputModeRef.current,
    ) => {
      const sessionId = sessionIdRef.current;
      if (!sessionId || closedRef.current) return;
      sequenceRef.current += 1;
      void recordCandidateIntegrityEvent(token, {
        sessionId,
        eventType,
        occurredAt: new Date().toISOString(),
        inputMode,
        visibilityState:
          eventType === "TAB_VISIBLE"
            ? "VISIBLE"
            : eventType === "TAB_HIDDEN"
              ? "HIDDEN"
              : null,
        sequenceNumber: sequenceRef.current,
        detail: detail ?? null,
      }).catch(() => {
        // Telemetria complementar não interfere no envio da resposta do candidato.
      });
    };

    const updateInputMode = (nextMode: CandidateIntegrityInputMode) => {
      if (inputModeRef.current === nextMode) return;
      inputModeRef.current = nextMode;
      record("INPUT_MODE_CHANGED", undefined, nextMode);
    };

    const closeSession = () => {
      const sessionId = sessionIdRef.current;
      if (!sessionId || closedRef.current) return;
      closedRef.current = true;
      clearCandidateIntegritySession(token, sessionId);
      if (heartbeatId !== undefined) window.clearInterval(heartbeatId);
      void closeCandidateIntegritySession(token, sessionId).catch(() => {
        // O TTL do backend encerra a sessão caso a chamada final não seja entregue.
      });
    };

    const handleVisibility = () => {
      record(document.visibilityState === "hidden" ? "TAB_HIDDEN" : "TAB_VISIBLE");
    };
    const handlePointer = (event: PointerEvent) => {
      updateInputMode(event.pointerType === "touch" ? "TOUCH" : "POINTER");
    };
    const handleTouch = () => updateInputMode("TOUCH");
    const handleKeyboard = () => updateInputMode("KEYBOARD");
    const handleAssetLoaded = (event: Event) => {
      const target = event.target;
      if (target instanceof HTMLImageElement) record("ASSET_LOADED", "IMAGE");
      if (target instanceof HTMLAudioElement) record("ASSET_LOADED", "AUDIO");
    };
    const handleStimulusStarted = (event: Event) => {
      if (event.target instanceof HTMLAudioElement) record("STIMULUS_STARTED", "AUDIO");
    };
    const handleCandidateClick = (event: MouseEvent) => {
      const target = event.target;
      if (!(target instanceof Element)) return;
      if (target.closest(".sc-confirm button")) {
        record("RESPONSE_CONFIRMED");
      } else if (target.closest(".opt")) {
        record("RESPONSE_SELECTED");
      }
    };
    const handlePageHide = () => {
      const sessionId = sessionIdRef.current;
      if (!sessionId || closedRef.current) return;
      closedRef.current = true;
      clearCandidateIntegritySession(token, sessionId);
      closeCandidateIntegritySessionKeepalive(token, sessionId);
    };

    const observeCandidateStage = () => {
      const stage = document.querySelector<HTMLElement>(".scenario .stage")?.textContent?.trim() ?? "";
      if (stage && stage !== lastPresentedStage) {
        lastPresentedStage = stage;
        record("NODE_PRESENTED", undefined, null);
      }
      if (document.querySelector(".cand-status .cs-label.done")) {
        closeSession();
      }
    };

    void startCandidateIntegritySession(token, clientSessionId, inputModeRef.current)
      .then((session) => {
        if (disposed) {
          void closeCandidateIntegritySession(token, session.sessionId).catch(() => undefined);
          return;
        }
        sessionIdRef.current = session.sessionId;
        setCandidateIntegritySession(token, session.sessionId);
        setState("active");

        const heartbeat = async () => {
          if (closedRef.current) return;
          try {
            await sendCandidateIntegrityHeartbeat(token, session.sessionId);
            heartbeatFailures = 0;
          } catch (error) {
            heartbeatFailures += 1;
            if (error instanceof PraxisApiError && error.status === 409) {
              clearCandidateIntegritySession(token, session.sessionId);
              setErrorReason("connection");
              setState("error");
              return;
            }
            if (heartbeatFailures >= 3) {
              clearCandidateIntegritySession(token, session.sessionId);
              setErrorReason("connection");
              setState("error");
            }
          }
        };

        heartbeatId = window.setInterval(
          () => void heartbeat(),
          Math.max(5, session.heartbeatIntervalSeconds) * 1000,
        );

        document.addEventListener("visibilitychange", handleVisibility);
        window.addEventListener("pointerdown", handlePointer, true);
        window.addEventListener("touchstart", handleTouch, true);
        window.addEventListener("keydown", handleKeyboard, true);
        window.addEventListener("load", handleAssetLoaded, true);
        window.addEventListener("loadeddata", handleAssetLoaded, true);
        window.addEventListener("play", handleStimulusStarted, true);
        window.addEventListener("click", handleCandidateClick, true);
        window.addEventListener("pagehide", handlePageHide);

        mutationObserver = new MutationObserver(observeCandidateStage);
        mutationObserver.observe(document.body, { childList: true, subtree: true });
        window.setTimeout(observeCandidateStage, 0);
      })
      .catch((error) => {
        if (disposed) return;
        const blocked = error instanceof PraxisApiError && error.status === 409;
        setErrorReason("start");
        setState(blocked ? "blocked" : "error");
      });

    return () => {
      disposed = true;
      if (heartbeatId !== undefined) window.clearInterval(heartbeatId);
      mutationObserver?.disconnect();
      document.removeEventListener("visibilitychange", handleVisibility);
      window.removeEventListener("pointerdown", handlePointer, true);
      window.removeEventListener("touchstart", handleTouch, true);
      window.removeEventListener("keydown", handleKeyboard, true);
      window.removeEventListener("load", handleAssetLoaded, true);
      window.removeEventListener("loadeddata", handleAssetLoaded, true);
      window.removeEventListener("play", handleStimulusStarted, true);
      window.removeEventListener("click", handleCandidateClick, true);
      window.removeEventListener("pagehide", handlePageHide);

      const sessionId = sessionIdRef.current;
      if (sessionId && !closedRef.current) {
        closedRef.current = true;
        clearCandidateIntegritySession(token, sessionId);
        void closeCandidateIntegritySession(token, sessionId).catch(() => undefined);
      }
    };
  }, [retryNonce, token]);

  if (state === "active") {
    return children;
  }

  const isStarting = state === "starting";
  const isBlocked = state === "blocked";
  const label = isStarting
    ? copy.preparingLabel
    : isBlocked
      ? copy.blockedLabel
      : copy.errorLabel;
  const title = isStarting
    ? copy.preparingTitle
    : isBlocked
      ? copy.blockedTitle
      : copy.errorTitle;
  const description = isStarting
    ? copy.preparingDescription
    : isBlocked
      ? copy.blockedDescription
      : errorReason === "connection"
        ? copy.connectionErrorDescription
        : copy.startErrorDescription;

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10 text-foreground">
      <section className="w-full max-w-lg rounded-2xl border border-border bg-card p-8 text-center shadow-sm">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">{label}</p>
        <h1 className="mt-3 text-2xl font-semibold tracking-tight">{title}</h1>
        <p
          className="mt-3 text-sm leading-6 text-muted-foreground"
          role={isStarting ? "status" : "alert"}
        >
          {description}
        </p>
        {!isStarting ? (
          <button
            type="button"
            onClick={() => setRetryNonce((value) => value + 1)}
            className="mt-6 inline-flex min-h-11 items-center justify-center rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90"
          >
            {copy.retry}
          </button>
        ) : null}
        <p className="mt-5 text-xs leading-5 text-muted-foreground">{copy.notice}</p>
      </section>
    </main>
  );
}

function getOrCreateClientSessionId(token: string): string {
  const storageKey = storageKeyForToken(token);
  try {
    const stored = window.sessionStorage.getItem(storageKey);
    if (stored) return stored;
    const generated = `web:${createRandomId()}`;
    window.sessionStorage.setItem(storageKey, generated);
    return generated;
  } catch {
    return `web:${createRandomId()}`;
  }
}

function storageKeyForToken(token: string): string {
  let hash = 2_166_136_261;
  for (let index = 0; index < token.length; index += 1) {
    hash ^= token.charCodeAt(index);
    hash = Math.imul(hash, 16_777_619);
  }
  return `${CLIENT_SESSION_PREFIX}${(hash >>> 0).toString(36)}`;
}

function createRandomId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;
}
