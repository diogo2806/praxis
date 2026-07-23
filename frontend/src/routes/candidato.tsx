import { createFileRoute, Link, Outlet, useChildMatches } from "@tanstack/react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useCallback, useEffect, useRef, useState, type CSSProperties } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState } from "@/components/praxis-ui";
import {
  getCandidateAttempt,
  HEALTH_CONSENT_VERSION,
  PraxisApiError,
  recordHealthConsent,
  requestHumanReview,
  submitCandidateAnswer,
  type CandidateAttemptResponse,
  type CandidateNodeResponse,
  type MediaType,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

const candidateStyles = `
  .cand-root {
    --bg: oklch(0.985 0.006 85);
    --bg-alt: oklch(0.965 0.009 235);
    --surface: oklch(1 0 0);
    --c-ink: oklch(0.24 0.02 240);
    --c-muted: oklch(0.49 0.018 240);
    --c-faint: oklch(0.62 0.015 240);
    --line: oklch(0.9 0.01 240);
    --line-soft: oklch(0.93 0.008 240);
    --c-primary: oklch(0.5 0.1 233);
    --c-primary-deep: oklch(0.4 0.09 238);
    --c-gold: oklch(0.76 0.13 80);
    --c-gold-deep: oklch(0.62 0.12 76);
    --c-success: oklch(0.6 0.13 150);
    --c-danger: oklch(0.58 0.18 28);
    --r-sm: 0.5rem;
    --r: 0.75rem;
    --r-lg: 1.1rem;
    --r-pill: 999px;
    --font-display: 'Source Serif 4', Georgia, 'Times New Roman', serif;
    --font-sans: 'IBM Plex Sans', system-ui, -apple-system, 'Segoe UI', sans-serif;
    --font-mono: 'IBM Plex Mono', ui-monospace, 'SFMono-Regular', monospace;
    --shadow-lg: 0 40px 80px -40px oklch(0.30 0.06 245 / 0.40);
    min-height: 100vh;
    background: var(--bg);
    font-family: var(--font-sans);
    color: var(--c-ink);
    line-height: 1.6;
    font-size: 17px;
    -webkit-font-smoothing: antialiased;
    padding: clamp(1rem, 4vw, 2.5rem);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }
  .cand-root.hc {
    --bg: #000;
    --bg-alt: #111;
    --surface: #000;
    --c-ink: #fff;
    --c-muted: #ccc;
    --c-faint: #aaa;
    --line: #444;
    --line-soft: #333;
    --c-primary: #6cf;
    --c-primary-deep: #4af;
    --c-gold: #fc0;
    --c-gold-deep: #da0;
    --c-success: #6c6;
    --c-danger: #f66;
    --shadow-lg: none;
    background: #000;
    color: #fff;
  }
  .cand-root.large-text { font-size: 20px; }
  @media (max-width: 560px) { .cand-root { font-size: 16px; } }
  .cand-a11y {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 0.5rem;
    margin-bottom: 1.2rem;
  }
  .cand-a11y button {
    font-family: var(--font-sans);
    font-size: 0.82rem;
    font-weight: 500;
    padding: 0.5rem 0.9rem;
    border-radius: var(--r-pill);
    border: 1px solid var(--line);
    background: var(--surface);
    color: var(--c-ink);
    cursor: pointer;
    transition: border-color .15s, background .15s;
  }
  .cand-a11y button:hover { border-color: var(--c-primary); color: var(--c-primary); }
  .cand-a11y button[aria-pressed="true"] {
    border-color: var(--c-primary);
    background: oklch(0.5 0.1 233 / 0.08);
    color: var(--c-primary);
  }
  .cand-root.hc .cand-a11y button[aria-pressed="true"] {
    background: #222;
    color: #6cf;
    border-color: #6cf;
  }
  .scenario {
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--r-lg);
    box-shadow: var(--shadow-lg);
    overflow: hidden;
    width: 100%;
    max-width: 620px;
  }
  .sc-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.9rem 1.1rem;
    border-bottom: 1px solid var(--line-soft);
    background: linear-gradient(var(--bg-alt), transparent);
  }
  .sc-id { display: flex; align-items: center; gap: 0.65rem; }
  .avatar {
    width: 2.2rem;
    height: 2.2rem;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--c-primary), var(--c-primary-deep));
    color: white;
    display: grid;
    place-items: center;
    font-weight: 600;
    font-size: 0.8rem;
    font-family: var(--font-mono);
  }
  .sc-id .who { font-weight: 600; font-size: 0.92rem; line-height: 1.1; }
  .sc-id .stage {
    font-family: var(--font-mono);
    font-size: 0.68rem;
    color: var(--c-faint);
    letter-spacing: 0.04em;
  }
  .sc-timer {
    font-family: var(--font-mono);
    font-weight: 500;
    font-size: 0.95rem;
    color: var(--c-muted);
    display: flex;
    align-items: center;
    gap: 0.4rem;
  }
  .sc-timer .tdot {
    width: 0.45rem;
    height: 0.45rem;
    border-radius: 50%;
    background: var(--c-gold);
    animation: cand-pulse 1.6s ease-in-out infinite;
  }
  @keyframes cand-pulse { 50% { opacity: 0.35; } }
  .sc-body { padding: 1.15rem 1.2rem 1.3rem; }
  .sc-tag {
    font-family: var(--font-mono);
    font-size: 0.66rem;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: var(--c-danger);
    font-weight: 500;
  }
  .sc-msg {
    font-family: var(--font-display);
    font-size: 1.18rem;
    line-height: 1.35;
    margin: 0.55rem 0 1.1rem;
    color: var(--c-ink);
  }
  .sc-opts { display: flex; flex-direction: column; gap: 0.55rem; }
  .opt {
    display: flex;
    gap: 0.7rem;
    align-items: flex-start;
    text-align: left;
    width: 100%;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--r);
    padding: 0.7rem 0.8rem;
    cursor: pointer;
    font: inherit;
    font-size: 0.9rem;
    color: var(--c-ink);
    transition: border-color .15s, background .15s, transform .1s;
    line-height: 1.35;
  }
  .opt:hover { border-color: var(--c-primary); background: oklch(0.5 0.1 233 / 0.04); }
  .opt .key {
    flex: none;
    width: 1.5rem;
    height: 1.5rem;
    border-radius: 0.4rem;
    border: 1px solid var(--line);
    display: grid;
    place-items: center;
    font-family: var(--font-mono);
    font-size: 0.78rem;
    font-weight: 600;
    color: var(--c-muted);
    transition: .15s;
  }
  .opt:hover .key { border-color: var(--c-primary); color: var(--c-primary); }
  .opt.picked { border-color: var(--c-primary); background: oklch(0.5 0.1 233 / 0.06); }
  .opt.picked .key { background: var(--c-primary); color: white; border-color: var(--c-primary); }
  .opt:disabled { opacity: 0.6; cursor: not-allowed; }
  .cand-root.hc .opt:hover, .cand-root.hc .opt.picked { background: #222; }
  .cand-root.hc .opt.picked .key { background: #6cf; color: #000; }
  .sc-note {
    margin-top: 1rem;
    font-size: 0.82rem;
    color: var(--c-faint);
    display: flex;
    gap: 0.55rem;
    align-items: flex-start;
    min-height: 1.2rem;
  }
  .sc-note svg {
    flex: none;
    width: 1rem;
    height: 1rem;
    margin-top: 0.15rem;
    stroke: var(--c-gold-deep);
  }
  .sc-confirm {
    margin-top: 1rem;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.5rem;
  }
  .sc-confirm .confirm-hint {
    font-size: 0.82rem;
    color: var(--c-faint);
    font-family: var(--font-sans);
  }
  .sc-confirm button, .cand-consent .cc-btn, .cand-review .rev-btn {
    font-family: var(--font-sans);
    font-weight: 600;
    font-size: 0.94rem;
    padding: 0.72rem 1.6rem;
    border-radius: var(--r-pill);
    cursor: pointer;
    border: 1px solid transparent;
    background: var(--c-primary);
    color: white;
    box-shadow: 0 8px 20px -10px oklch(0.5 0.1 233 / 0.7);
    transition: transform .15s, box-shadow .2s, background .2s;
  }
  .sc-confirm button:hover, .cand-consent .cc-btn:hover, .cand-review .rev-btn:hover {
    background: var(--c-primary-deep);
  }
  .sc-confirm button:active { transform: translateY(1px); }
  .sc-confirm button:disabled, .cand-consent .cc-btn:disabled, .cand-review .rev-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
  .sc-error, .cand-consent .cc-err, .cand-review .rev-err {
    margin-top: 0.7rem;
    font-size: 0.82rem;
    font-weight: 500;
    color: var(--c-danger);
    text-align: center;
  }
  .sc-media { margin: 0.6rem 0; display: flex; justify-content: center; }
  .sc-media img {
    max-height: 12rem;
    width: auto;
    border-radius: var(--r);
    border: 1px solid var(--line);
    object-fit: contain;
  }
  .sc-media audio { width: 100%; }
  .cand-progress {
    width: 100%;
    max-width: 620px;
    margin-bottom: 0.8rem;
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
  }
  .cand-progress .cp-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-family: var(--font-mono);
    font-size: 0.75rem;
    color: var(--c-muted);
    font-weight: 500;
  }
  .cand-progress .cp-track {
    height: 0.38rem;
    border-radius: var(--r-pill);
    background: oklch(0.5 0.06 240 / 0.1);
    overflow: hidden;
  }
  .cand-progress .cp-fill {
    height: 100%;
    border-radius: var(--r-pill);
    background: linear-gradient(90deg, var(--c-primary), oklch(0.62 0.12 215));
    transition: width 1s linear;
  }
  .cand-progress .cp-fill.low {
    background: linear-gradient(90deg, oklch(0.7 0.14 60), oklch(0.58 0.18 28));
  }
  .cand-status, .cand-consent {
    width: 100%;
    max-width: 620px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--r-lg);
    box-shadow: var(--shadow-lg);
  }
  .cand-status { padding: 2.5rem 1.5rem; text-align: center; }
  .cand-status .cs-label, .cand-consent .cc-label {
    font-family: var(--font-mono);
    font-size: 0.66rem;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    font-weight: 500;
  }
  .cand-status .cs-label.loading, .cand-consent .cc-label { color: var(--c-muted); }
  .cand-status .cs-label.error { color: var(--c-danger); }
  .cand-status .cs-label.done { color: var(--c-success); }
  .cand-status h1, .cand-consent h1 {
    font-family: var(--font-display);
    font-weight: 500;
    color: var(--c-ink);
    line-height: 1.15;
  }
  .cand-status h1 { font-size: 1.8rem; margin: 0.7rem 0 0; }
  .cand-consent h1 { font-size: 1.5rem; margin: 0.5rem 0 0; }
  .cand-status p {
    margin-top: 0.7rem;
    font-size: 0.92rem;
    color: var(--c-muted);
    max-width: 44ch;
    margin-inline: auto;
  }
  .cand-status .cs-note {
    margin: 1.2rem auto 0;
    max-width: 48ch;
    padding: 0.9rem 1rem;
    border: 1px solid var(--line-soft);
    border-radius: var(--r);
    background: var(--bg-alt);
    color: var(--c-muted);
    font-size: 0.82rem;
    text-align: left;
  }
  .cand-review { margin-top: 1.2rem; }
  .cand-review a, .cand-review button.link-btn {
    font-family: var(--font-sans);
    font-size: 0.88rem;
    font-weight: 500;
    color: var(--c-primary);
    text-decoration: underline;
    cursor: pointer;
    background: none;
    border: none;
  }
  .cand-review textarea {
    width: 100%;
    max-width: 24rem;
    margin: 0.5rem auto;
    display: block;
    border: 1px solid var(--line);
    border-radius: var(--r);
    padding: 0.6rem 0.8rem;
    font: inherit;
    font-size: 0.88rem;
    resize: none;
    background: var(--surface);
    color: var(--c-ink);
  }
  .cand-review .rev-btn { font-size: 0.88rem; padding: 0.55rem 1.2rem; }
  .cand-review .rev-ok {
    color: var(--c-success);
    font-size: 0.88rem;
    font-weight: 500;
    margin-top: 0.5rem;
  }
  .cand-footer {
    width: 100%;
    max-width: 620px;
    margin-top: 1.2rem;
    padding-top: 0.8rem;
    border-top: 1px solid var(--line-soft);
    font-size: 0.74rem;
    color: var(--c-faint);
    text-align: center;
    line-height: 1.55;
  }
  .cand-consent { padding: 1.5rem; text-align: left; }
  .cand-consent .cc-body {
    margin-top: 1rem;
    font-size: 0.9rem;
    color: var(--c-muted);
    line-height: 1.6;
  }
  .cand-consent .cc-body p + p { margin-top: 0.7rem; }
  .cand-consent .cc-body strong { color: var(--c-ink); }
  .cand-consent .cc-body ul { margin: 0.5rem 0; padding-left: 1.2rem; }
  .cand-consent .cc-body li + li { margin-top: 0.3rem; }
  .cand-consent .cc-checks {
    margin-top: 1.2rem;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }
  .cand-consent .cc-check {
    display: flex;
    align-items: flex-start;
    gap: 0.7rem;
    padding: 0.7rem 0.8rem;
    border: 1px solid var(--line);
    border-radius: var(--r);
    font-size: 0.88rem;
    color: var(--c-ink);
  }
  .cand-consent .cc-check input { margin-top: 0.15rem; }
  .cand-consent .cc-actions { margin-top: 1.2rem; }
`;

export const Route = createFileRoute("/candidato")({
  head: () => ({
    meta: [
      { title: "Avaliação do participante - Práxis" },
      {
        name: "description",
        content: "Experiência mobile-first com timer, respostas claras, retomada e acessibilidade.",
      },
    ],
  }),
  component: CandidateRouteLayout,
});

const SUBMIT_ERROR_MESSAGES: Record<number, string> = {
  400: "Não consegui registrar agora. Toque na resposta de novo, por favor.",
  409: "O tempo desta etapa terminou. Atualize a página para conferir a etapa atual.",
};

const LOAD_ERROR_MESSAGES: Record<number, string> = {
  400: "Não consegui abrir esta avaliação. Verifique o link recebido e tente novamente.",
  404: "Não encontrei esta avaliação. Verifique o link recebido e tente novamente.",
  409: "Esta avaliação não está mais disponível.",
};

const ACCESSIBILITY_STORAGE_KEY = "praxis:candidate-accessibility";
const OPTION_LETTERS = ["A", "B", "C", "D", "E", "F", "G", "H"];

type CandidateAccessibilityPreferences = {
  highContrast: boolean;
  largeText: boolean;
  dyslexiaFont: boolean;
};

const DEFAULT_ACCESSIBILITY_PREFERENCES: CandidateAccessibilityPreferences = {
  highContrast: false,
  largeText: false,
  dyslexiaFont: false,
};

function loadAccessibilityPreferences(): CandidateAccessibilityPreferences {
  if (typeof window === "undefined") {
    return DEFAULT_ACCESSIBILITY_PREFERENCES;
  }
  try {
    const stored = window.localStorage.getItem(ACCESSIBILITY_STORAGE_KEY);
    if (!stored) {
      return DEFAULT_ACCESSIBILITY_PREFERENCES;
    }
    const parsed = JSON.parse(stored) as Partial<CandidateAccessibilityPreferences>;
    return {
      highContrast: Boolean(parsed.highContrast),
      largeText: Boolean(parsed.largeText),
      dyslexiaFont: Boolean(parsed.dyslexiaFont),
    };
  } catch {
    return DEFAULT_ACCESSIBILITY_PREFERENCES;
  }
}

function friendlyApiErrorMessage(
  error: unknown,
  messagesByStatus: Record<number, string>,
  fallback: string,
) {
  if (error instanceof PraxisApiError) {
    return messagesByStatus[error.status] ?? fallback;
  }
  return fallback;
}

function CandidateRouteLayout() {
  const childMatches = useChildMatches();
  if (childMatches.length > 0) {
    return <Outlet />;
  }
  return <CandidateEntryPage />;
}

function CandidateEntryPage() {
  const [token, setToken] = useState("");
  const normalizedToken = token.trim();

  return (
    <AppShell>
      <EmptyState
        title="Código de acesso obrigatório"
        description="Para abrir a avaliação, use o código de acesso enviado pelo convite. Cole aqui ou abra o link do e-mail."
        actions={
          <div className="grid gap-2 sm:grid-cols-[minmax(0,1fr)_auto_auto]">
            <input
              className="input"
              placeholder="Código de acesso"
              value={token}
              onChange={(event) => setToken(event.target.value)}
            />
            <Link
              to="/candidato/$token"
              params={{ token: normalizedToken || "_" }}
              className={`rounded-md bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90 ${
                !normalizedToken ? "pointer-events-none opacity-50" : ""
              }`}
            >
              Abrir avaliação
            </Link>
            <Link
              to="/"
              className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
            >
              Voltar ao painel
            </Link>
          </div>
        }
      />
    </AppShell>
  );
}

function CandidateMedia({
  mediaUrl,
  mediaType,
  accessibleDescription,
  audioLabel,
}: {
  mediaUrl: string;
  mediaType: MediaType | null;
  accessibleDescription?: string | null;
  audioLabel: string;
}) {
  if (mediaType === "AUDIO") {
    return (
      <audio
        controls
        src={mediaUrl}
        className="w-full"
        aria-label={audioLabel}
        onClick={(event) => event.stopPropagation()}
      >
        Seu navegador não suporta áudio.
      </audio>
    );
  }

  return (
    <img
      src={mediaUrl}
      alt={accessibleDescription?.trim() || ""}
      className="max-h-48 w-auto rounded-md border border-border object-contain"
    />
  );
}

function formatTimer(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remainder).padStart(2, "0")}`;
}

function FocusedCandidateExperience({ token }: { token: string }) {
  const [liveAttempt, setLiveAttempt] = useState<CandidateAttemptResponse | null>(null);
  const [remaining, setRemaining] = useState(30);
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);
  const [accessibilityPreferences, setAccessibilityPreferences] = useState(
    loadAccessibilityPreferences,
  );
  const [submittingAnswer, setSubmittingAnswer] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [healthConsentGiven, setHealthConsentGiven] = useState(false);
  const stylesInjected = useRef(false);
  const { highContrast, largeText, dyslexiaFont } = accessibilityPreferences;

  useEffect(() => {
    if (stylesInjected.current) return;
    stylesInjected.current = true;
    const style = document.createElement("style");
    style.textContent = candidateStyles;
    document.head.appendChild(style);
    return () => {
      document.head.removeChild(style);
    };
  }, []);

  const attemptQuery = useQuery({
    queryKey: ["candidate-attempt", token],
    queryFn: () => getCandidateAttempt(token),
  });

  useEffect(() => {
    if (attemptQuery.data) {
      setLiveAttempt(attemptQuery.data);
    }
  }, [attemptQuery.data]);

  useEffect(() => {
    window.localStorage.setItem(
      ACCESSIBILITY_STORAGE_KEY,
      JSON.stringify(accessibilityPreferences),
    );
  }, [accessibilityPreferences]);

  const attempt = liveAttempt ?? attemptQuery.data;
  const currentNode = attempt?.etapaAtual ?? null;
  const finished = Boolean(attempt && (attempt.finalizado || !currentNode));
  const needsHealthConsent =
    Boolean(attempt?.verticalSaude) &&
    !healthConsentGiven &&
    !attempt?.healthConsentValid &&
    !finished;
  const timeLimit = Math.max(
    1,
    currentNode?.tempoLimiteSegundosAcomodado ?? currentNode?.tempoLimiteSegundos ?? 30,
  );
  const timePercentage = finished ? 0 : Math.max(0, Math.min(100, (remaining / timeLimit) * 100));
  const currentStep = attempt?.progresso?.passoAtual ?? currentNode?.numero ?? 1;
  const totalSteps = Math.max(currentStep, attempt?.progresso?.passosEstimados ?? currentStep);
  const selectedOption = currentNode?.alternativas.find((option) => option.id === selectedOptionId);

  useEffect(() => {
    const redirectUrl = attempt?.redirectUrl;
    if (!finished || !redirectUrl) return;

    const timeout = window.setTimeout(() => {
      window.location.assign(redirectUrl);
    }, 1200);
    return () => window.clearTimeout(timeout);
  }, [attempt?.redirectUrl, finished]);

  const submitAnswer = useCallback(
    async (node: CandidateNodeResponse, optionId: string | null, timedOut: boolean) => {
      if (!attempt) return;
      setSubmittingAnswer(true);
      setSubmitError(null);

      const sendAnswer = async (timeExpired: boolean) => {
        const response = await submitCandidateAnswer(token, {
          etapaId: node.id,
          etapaNumero: node.numero,
          respostaId: timeExpired ? null : optionId,
          respondidaEm: new Date().toISOString(),
          tempoEsgotado: timeExpired,
        });
        setLiveAttempt({
          participacaoId: response.participacaoId,
          avaliacaoNome: attempt.avaliacaoNome,
          status: response.status,
          finalizado: response.finalizado,
          redirectUrl: response.redirectUrl ?? attempt.redirectUrl ?? null,
          acaoSugeridaFrontend: response.finalizado
            ? "VER_RESULTADOS"
            : attempt.acaoSugeridaFrontend,
          progresso: response.progresso,
          etapaAtual: response.etapaAtual,
          verticalSaude: attempt.verticalSaude,
          healthConsentValid: attempt.healthConsentValid,
          healthConsentNoticeVersion: attempt.healthConsentNoticeVersion,
        });
        setSelectedOptionId(null);
        void attemptQuery.refetch();
      };

      try {
        await sendAnswer(timedOut);
      } catch (error) {
        if (!timedOut && error instanceof PraxisApiError && error.status === 409) {
          try {
            await sendAnswer(true);
            return;
          } catch (retryError) {
            setSubmitError(
              friendlyApiErrorMessage(
                retryError,
                SUBMIT_ERROR_MESSAGES,
                "Tivemos um problema ao salvar. Tente novamente.",
              ),
            );
            setSelectedOptionId(null);
            return;
          }
        }
        setSubmitError(
          friendlyApiErrorMessage(
            error,
            SUBMIT_ERROR_MESSAGES,
            "Tivemos um problema ao salvar. Tente novamente.",
          ),
        );
        setSelectedOptionId(null);
      } finally {
        setSubmittingAnswer(false);
      }
    },
    [attempt, attemptQuery, token],
  );

  useEffect(() => {
    setRemaining(timeLimit);
    setSelectedOptionId(null);
  }, [currentNode?.numero, timeLimit]);

  useEffect(() => {
    if (finished || submittingAnswer || !currentNode || needsHealthConsent) return;
    const id = window.setInterval(() => {
      setRemaining((value) => Math.max(0, value - 1));
    }, 1000);
    return () => window.clearInterval(id);
  }, [submittingAnswer, currentNode, finished, needsHealthConsent]);

  useEffect(() => {
    if (
      remaining === 0 &&
      !finished &&
      currentNode &&
      !selectedOptionId &&
      !submittingAnswer &&
      !needsHealthConsent
    ) {
      void submitAnswer(currentNode, null, true);
    }
  }, [
    currentNode,
    finished,
    remaining,
    selectedOptionId,
    submitAnswer,
    submittingAnswer,
    needsHealthConsent,
  ]);

  const rootClass = cn("cand-root", highContrast && "hc", largeText && "large-text");
  const fontStyle = {
    fontFamily: dyslexiaFont
      ? "'OpenDyslexic', 'Atkinson Hyperlegible', Verdana, Arial, sans-serif"
      : undefined,
  } satisfies CSSProperties;

  return (
    <main className={rootClass} style={fontStyle}>
      <div className="cand-a11y">
        <button
          type="button"
          onClick={() =>
            setAccessibilityPreferences((value) => ({
              ...value,
              highContrast: !value.highContrast,
            }))
          }
          aria-pressed={highContrast}
          title="Alterna para alto contraste"
        >
          Alto contraste
        </button>
        <button
          type="button"
          onClick={() =>
            setAccessibilityPreferences((value) => ({
              ...value,
              largeText: !value.largeText,
            }))
          }
          aria-pressed={largeText}
          title="Aumenta o tamanho do texto"
        >
          Texto {largeText ? "normal" : "maior"}
        </button>
        <button
          type="button"
          onClick={() =>
            setAccessibilityPreferences((value) => ({
              ...value,
              dyslexiaFont: !value.dyslexiaFont,
            }))
          }
          aria-pressed={dyslexiaFont}
          title="Troca para uma fonte mais fácil de ler"
        >
          Fonte para dislexia
        </button>
      </div>

      {attemptQuery.isLoading ? (
        <div className="cand-status">
          <div className="cs-label loading">Carregando</div>
          <h1>Preparando sua avaliação.</h1>
        </div>
      ) : attemptQuery.isError && !attempt ? (
        <div className="cand-status">
          <div className="cs-label error">Acesso indisponível</div>
          <h1>Não foi possível carregar a avaliação.</h1>
          <p>
            {friendlyApiErrorMessage(
              attemptQuery.error,
              LOAD_ERROR_MESSAGES,
              "Verifique o link recebido e tente novamente.",
            )}
          </p>
        </div>
      ) : needsHealthConsent ? (
        <HealthConsentGate
          token={token}
          noticeVersion={attempt?.healthConsentNoticeVersion ?? HEALTH_CONSENT_VERSION}
          onConsented={async () => {
            const refreshedAttempt = await attemptQuery.refetch();
            if (refreshedAttempt.data?.healthConsentValid) {
              setLiveAttempt(refreshedAttempt.data);
              setHealthConsentGiven(true);
            }
          }}
        />
      ) : currentNode && !finished ? (
        <>
          <div className="cand-progress">
            <div className="cp-info">
              <span>
                Cenário {currentStep}/{totalSteps}
              </span>
              <span>{remaining}s</span>
            </div>
            <div className="cp-track">
              <div
                className={cn("cp-fill", timePercentage <= 35 && "low")}
                style={{ width: `${timePercentage}%` }}
              />
            </div>
          </div>

          <div className="scenario" aria-label="Cenário da avaliação">
            <div className="sc-top">
              <div className="sc-id">
                <div className="avatar">
                  {(currentNode.pessoa ?? "P").substring(0, 2).toUpperCase()}
                </div>
                <div>
                  <div className="who">{currentNode.pessoa ?? "Participante"}</div>
                  <div className="stage">
                    Cenário {currentStep}/{totalSteps} · {attempt?.avaliacaoNome ?? "avaliação"}
                  </div>
                </div>
              </div>
              <div className="sc-timer">
                <span className="tdot" />
                {formatTimer(remaining)}
              </div>
            </div>

            <div className="sc-body">
              {currentNode.pessoa && <div className="sc-tag">{currentNode.pessoa}</div>}
              <p
                className="sc-msg"
                aria-label={currentNode.descricaoAcessivel || currentNode.descricao}
              >
                {currentNode.descricao}
              </p>

              {currentNode.audioDescricaoUrl && (
                <div className="sc-media">
                  <audio
                    controls
                    src={currentNode.audioDescricaoUrl}
                    aria-label="Audiodescrição do cenário"
                  >
                    Seu navegador não suporta áudio.
                  </audio>
                </div>
              )}
              {currentNode.midiaUrl && (
                <div className="sc-media">
                  <CandidateMedia
                    mediaUrl={currentNode.midiaUrl}
                    mediaType={currentNode.tipoMidia ?? null}
                    accessibleDescription={currentNode.descricaoAcessivel}
                    audioLabel="Áudio do cenário"
                  />
                </div>
              )}

              <div className="sc-opts" role="group" aria-label="Como você agiria?">
                {currentNode.alternativas.map((option, idx) => (
                  <div key={option.id}>
                    <button
                      type="button"
                      className={cn("opt", selectedOptionId === option.id && "picked")}
                      onClick={() => {
                        setSubmitError(null);
                        setSelectedOptionId(option.id);
                      }}
                      disabled={submittingAnswer}
                      aria-label={option.descricaoAcessivel || option.texto}
                      aria-pressed={selectedOptionId === option.id}
                    >
                      <span className="key">{OPTION_LETTERS[idx] ?? String(idx + 1)}</span>
                      <span>{option.texto}</span>
                    </button>
                    {option.audioDescricaoUrl && (
                      <div className="sc-media">
                        <audio
                          controls
                          src={option.audioDescricaoUrl}
                          aria-label={`Audiodescrição da alternativa ${
                            OPTION_LETTERS[idx] ?? idx + 1
                          }`}
                        >
                          Seu navegador não suporta áudio.
                        </audio>
                      </div>
                    )}
                    {option.mediaUrl && (
                      <div className="sc-media">
                        <CandidateMedia
                          mediaUrl={option.mediaUrl}
                          mediaType={option.tipoMidia ?? null}
                          accessibleDescription={option.descricaoAcessivel}
                          audioLabel={`Áudio da alternativa ${OPTION_LETTERS[idx] ?? idx + 1}`}
                        />
                      </div>
                    )}
                  </div>
                ))}
              </div>

              {selectedOption && (
                <div className="sc-confirm" aria-live="polite">
                  <span className="confirm-hint">Confirme para enviar sua resposta final.</span>
                  <button
                    type="button"
                    onClick={() => submitAnswer(currentNode, selectedOption.id, false)}
                    disabled={submittingAnswer}
                  >
                    {submittingAnswer ? "Registrando..." : "Confirmar resposta final"}
                  </button>
                </div>
              )}
              {submitError && (
                <div className="sc-error" aria-live="assertive">
                  {submitError}
                </div>
              )}

              <p className="sc-note">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <path d="M12 2 4 5v6c0 5 3.4 8.3 8 11 4.6-2.7 8-6 8-11V5z" />
                </svg>
                <span>
                  Escolha uma alternativa. Você pode trocar antes de confirmar; depois de confirmar,
                  a resposta é final.
                </span>
              </p>
            </div>
          </div>
        </>
      ) : (
        <div className="cand-status">
          <div className="cs-label done">Participação finalizada</div>
          <h1>Obrigado por participar.</h1>
          <p>
            {attempt?.redirectUrl
              ? "Avaliação concluída. Redirecionando você de volta para a Gupy..."
              : "O resultado será processado e entregue para a equipe responsável."}
          </p>
          <div className="cs-note">
            Esta avaliação mede como você age em uma situação de trabalho, por competência. Ela é
            apoio à decisão: quem decide sobre a sua candidatura é uma pessoa, não um sistema
            automático. Você pode pedir que uma pessoa revise o resultado.
          </div>
          <HumanReviewRequest attemptId={token} />
        </div>
      )}

      <div className="cand-footer">
        {attempt?.verticalSaude
          ? "Esta é uma atividade educativa. Seus dados são tratados para esta finalidade, conforme a LGPD e a política de privacidade da empresa responsável. Não é diagnóstico nem substitui avaliação profissional. A decisão é de uma pessoa, não de um sistema automático, e você pode pedir revisão humana."
          : "Seus dados são tratados para fins desta avaliação, conforme a LGPD e a política de privacidade da empresa responsável. A pontuação segue critérios definidos antes da avaliação, sem IA julgando você. A decisão sobre a sua candidatura é tomada por uma pessoa, não por um sistema automático, e você pode solicitar revisão humana do resultado."}
      </div>
    </main>
  );
}

function HumanReviewRequest({ attemptId }: { attemptId: string }) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const mutation = useMutation({
    mutationFn: () => requestHumanReview(attemptId, reason),
  });

  if (mutation.isSuccess) {
    return (
      <div className="cand-review" aria-live="polite">
        <p className="rev-ok">
          Pedido de revisão registrado. Uma pessoa da equipe responsável vai analisar.
        </p>
      </div>
    );
  }

  return (
    <div className="cand-review">
      {!open ? (
        <button type="button" className="link-btn" onClick={() => setOpen(true)}>
          Solicitar revisão humana
        </button>
      ) : (
        <div>
          <textarea
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            rows={3}
            maxLength={1000}
            placeholder="Se quiser, conte por que (opcional)."
          />
          <button
            type="button"
            className="rev-btn"
            disabled={mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? "Enviando..." : "Enviar pedido"}
          </button>
          {mutation.isError && (
            <p className="rev-err" aria-live="assertive">
              Não consegui registrar agora. Tente novamente em instantes.
            </p>
          )}
        </div>
      )}
    </div>
  );
}

function HealthConsentGate({
  token,
  noticeVersion,
  onConsented,
}: {
  token: string;
  noticeVersion: string;
  onConsented: () => Promise<void> | void;
}) {
  const [agreed, setAgreed] = useState(false);
  const [onBehalfOfMinor, setOnBehalfOfMinor] = useState(false);
  const mutation = useMutation({
    mutationFn: () => recordHealthConsent(token, onBehalfOfMinor, noticeVersion),
    onSuccess: onConsented,
  });

  return (
    <div className="cand-consent">
      <div className="cc-label">Antes de começar</div>
      <h1>Uso dos seus dados nesta atividade</h1>
      <div className="cc-body">
        <p>
          Esta atividade é um <strong>exercício educativo de tomada de decisão</strong>. Ela
          apresenta situações do dia a dia para você praticar escolhas. {" "}
          <strong>
            Não é uma consulta, não é diagnóstico e não substitui a orientação de um profissional de
            saúde.
          </strong>
        </p>
        <p>
          Para realizar a atividade, a empresa responsável vai tratar respostas suas que podem
          revelar informações relacionadas à sua saúde ou aos seus hábitos. Esses dados serão usados {" "}
          <strong>somente</strong> para gerar o resultado educativo desta atividade e para as
          finalidades descritas na política de privacidade da empresa responsável.
        </p>
        <ul>
          <li>A pontuação segue critérios definidos antes da atividade. Não há IA julgando você.</li>
          <li>
            Seus dados não serão usados para decidir, sozinhos e de forma automatizada, sobre
            tratamento, atendimento ou acesso a serviços.
          </li>
          <li>Você pode pedir que uma pessoa revise o resultado.</li>
          <li>
            Você pode acessar, corrigir ou excluir seus dados e revogar este consentimento a qualquer
            momento, pelo canal indicado pela empresa responsável. A revogação não afeta atividades
            já realizadas.
          </li>
        </ul>
      </div>

      <div className="cc-checks">
        <label className="cc-check">
          <input
            type="checkbox"
            checked={agreed}
            onChange={(event) => setAgreed(event.target.checked)}
          />
          <span>
            Li e concordo que a empresa responsável trate os dados sensíveis de saúde informados por
            mim nesta atividade, para as finalidades educativas descritas acima.
          </span>
        </label>
        <label className="cc-check">
          <input
            type="checkbox"
            checked={onBehalfOfMinor}
            onChange={(event) => setOnBehalfOfMinor(event.target.checked)}
          />
          <span>Estou concordando como responsável legal pela pessoa sob minha responsabilidade.</span>
        </label>
      </div>

      <div className="cc-actions">
        <button
          type="button"
          className="cc-btn"
          disabled={!agreed || mutation.isPending}
          onClick={() => mutation.mutate()}
        >
          {mutation.isPending ? "Registrando consentimento..." : "Concordar e continuar"}
        </button>
        {mutation.isError && (
          <p className="cc-err" aria-live="assertive">
            Não consegui registrar o consentimento agora. Tente novamente em instantes.
          </p>
        )}
      </div>
    </div>
  );
}

export function CandidateExperience({ token }: { token: string }) {
  return <FocusedCandidateExperience token={token} />;
}
