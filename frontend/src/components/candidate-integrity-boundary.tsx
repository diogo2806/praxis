import { useEffect, useRef, useState, type ReactNode } from "react";
import {
  closeCandidateIntegritySession,
  closeCandidateIntegritySessionKeepalive,
  recordCandidateIntegrityEvent,
  sendCandidateIntegrityHeartbeat,
  startCandidateIntegritySession,
  type CandidateIntegrityEventType,
  type CandidateIntegrityInputMode,
} from "@/lib/api/candidate-integrity";
import { PraxisApiError } from "@/lib/api/praxis";

const CLIENT_SESSION_PREFIX = "praxis:integrity-session:";
const INTEGRITY_SESSION_HEADER = "X-Praxis-Integrity-Session";

type BoundaryState = "starting" | "active" | "blocked" | "error";

type CandidateIntegrityBoundaryProps = {
  token: string;
  children: ReactNode;
};

export function CandidateIntegrityBoundary({ token, children }: CandidateIntegrityBoundaryProps) {
  const [state, setState] = useState<BoundaryState>("starting");
  const [message, setMessage] = useState("Preparando uma sessão segura para a avaliação.");
  const [retryNonce, setRetryNonce] = useState(0);
  const sessionIdRef = useRef<string | null>(null);
  const inputModeRef = useRef<CandidateIntegrityInputMode>("UNKNOWN");
  const sequenceRef = useRef(0);
  const closedRef = useRef(false);

  useEffect(() => {
    const previousFetch = window.fetch;
    const attemptPath = `/candidate/attempts/${encodeURIComponent(token)}`;

    const fetchWithIntegritySession: typeof window.fetch = async (input, init) => {
      const requestUrl =
        typeof input === "string"
          ? input
          : input instanceof URL
            ? input.toString()
            : input.url;
      const pathname = new URL(requestUrl, window.location.origin).pathname;
      const protectedRequest =
        pathname.endsWith(attemptPath) || pathname.endsWith(`${attemptPath}/answers`);
      const sessionId = sessionIdRef.current;
      if (!sessionId || !protectedRequest) {
        return previousFetch.call(window, input, init);
      }

      const headers = new Headers(input instanceof Request ? input.headers : undefined);
      new Headers(init?.headers).forEach((value, name) => headers.set(name, value));
      headers.set(INTEGRITY_SESSION_HEADER, sessionId);
      return previousFetch.call(window, input, { ...init, headers });
    };

    window.fetch = fetchWithIntegritySession;
    return () => {
      if (window.fetch === fetchWithIntegritySession) {
        window.fetch = previousFetch;
      }
    };
  }, [token]);

  useEffect(() => {
    let disposed = false;
    let heartbeatId: number | undefined;
    let mutationObserver: MutationObserver | undefined;
    let heartbeatFailures = 0;
    let lastPresentedStage = "";

    setState("starting");
    setMessage("Preparando uma sessão segura para a avaliação.");
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
        if (disposed) return;
        sessionIdRef.current = session.sessionId;
        setState("active");
        setMessage("");

        const heartbeat = async () => {
          if (closedRef.current) return;
          try {
            await sendCandidateIntegrityHeartbeat(token, session.sessionId);
            heartbeatFailures = 0;
          } catch (error) {
            heartbeatFailures += 1;
            if (error instanceof PraxisApiError && error.status === 409) {
              setState("blocked");
              setMessage(error.message);
              return;
            }
            if (heartbeatFailures >= 3) {
              setState("error");
              setMessage("A conexão com a sessão da avaliação foi interrompida. Verifique a internet e tente retomar.");
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
        setState(blocked ? "blocked" : "error");
        setMessage(
          error instanceof Error
            ? error.message
            : "Não foi possível iniciar a sessão segura da avaliação.",
        );
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
    };
  }, [retryNonce, token]);

  if (state === "active") {
    return children;
  }

  const blocked = state === "blocked";
  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10 text-foreground">
      <section className="w-full max-w-lg rounded-2xl border border-border bg-card p-8 text-center shadow-sm">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
          {state === "starting" ? "Preparando" : blocked ? "Sessão já aberta" : "Conexão interrompida"}
        </p>
        <h1 className="mt-3 text-2xl font-semibold tracking-tight">
          {state === "starting"
            ? "Validando a sessão da avaliação."
            : blocked
              ? "A avaliação está aberta em outra sessão."
              : "Não foi possível manter a sessão da avaliação."}
        </h1>
        <p className="mt-3 text-sm leading-6 text-muted-foreground">{message}</p>
        {state !== "starting" ? (
          <button
            type="button"
            onClick={() => setRetryNonce((value) => value + 1)}
            className="mt-6 inline-flex min-h-11 items-center justify-center rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90"
          >
            Tentar retomar
          </button>
        ) : null}
        <p className="mt-5 text-xs leading-5 text-muted-foreground">
          A verificação é técnica, não altera sua pontuação e não toma decisão automática sobre sua candidatura.
        </p>
      </section>
    </main>
  );
}

function getOrCreateClientSessionId(token: string): string {
  const storageKey = `${CLIENT_SESSION_PREFIX}${token}`;
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

function createRandomId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;
}
