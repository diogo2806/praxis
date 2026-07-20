import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { ChevronDown, Scale, ShieldCheck } from "lucide-react";
import { CandidateIntegrityBoundary } from "@/components/candidate-integrity-boundary";
import { LanguageSelector } from "@/components/language-selector";
import {
  getCandidateAttempt,
  requestHumanReview,
  type CandidateAttemptResponse,
} from "@/lib/api/praxis";
import {
  requestDataSubjectRight,
  type DataSubjectRequestType,
} from "@/lib/api/data-subject-rights";
import { useLanguage } from "@/lib/language-context";
import { CandidateExperience } from "@/routes/candidato";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type CandidateAccessCopy = ReturnType<typeof useLanguage>["t"]["candidateAccess"];
type RequestState = "idle" | "sending" | "success" | "error";

const reviewCopy = {
  "pt-BR": {
    title: "Solicitar revisão humana do resultado",
    description:
      "Use este canal quando quiser que uma pessoa revise o resultado e as evidências da avaliação.",
    reasonLabel: "Motivo ou contexto (opcional)",
    reasonPlaceholder: "Explique o ponto que deve ser revisado.",
    button: "Solicitar revisão humana",
    sending: "Registrando revisão...",
    success: "Pedido de revisão registrado para análise pela empresa responsável.",
    error: "Não foi possível registrar o pedido de revisão.",
  },
  en: {
    title: "Request a human review of the result",
    description:
      "Use this channel when you want a person to review the assessment result and its evidence.",
    reasonLabel: "Reason or context (optional)",
    reasonPlaceholder: "Explain what should be reviewed.",
    button: "Request human review",
    sending: "Registering review...",
    success: "The review request was registered for analysis by the responsible company.",
    error: "The review request could not be registered.",
  },
  "es-MX": {
    title: "Solicitar revisión humana del resultado",
    description:
      "Usa este canal cuando quieras que una persona revise el resultado y las evidencias de la evaluación.",
    reasonLabel: "Motivo o contexto (opcional)",
    reasonPlaceholder: "Explica qué debe revisarse.",
    button: "Solicitar revisión humana",
    sending: "Registrando revisión...",
    success: "La solicitud de revisión fue registrada para análisis por la empresa responsable.",
    error: "No fue posible registrar la solicitud de revisión.",
  },
} as const;

export const Route = createFileRoute("/candidato/$token")({ component: TokenCandidatePage });

function TokenCandidatePage() {
  const { token } = Route.useParams();
  const { t } = useLanguage();
  const copy = t.candidateAccess;
  const [ready, setReady] = useState(false);
  const attempt = useQuery({
    queryKey: ["candidate-attempt", token],
    queryFn: () => getCandidateAttempt(token),
    enabled: ready,
  });
  const terminal = useMemo(
    () => (!attempt.data || attempt.data.etapaAtual ? null : statusCopy(attempt.data, copy)),
    [attempt.data, copy],
  );
  const redirectUrl = attempt.data?.finalizado ? attempt.data.redirectUrl ?? null : null;

  useEffect(() => {
    if (!redirectUrl) return;
    const timeout = window.setTimeout(() => window.location.assign(redirectUrl), 1200);
    return () => window.clearTimeout(timeout);
  }, [redirectUrl]);

  if (!ready) {
    return (
      <Shell>
        <div className="space-y-6">
          <Start copy={copy} onStart={() => setReady(true)} />
          <PrivacyAndRightsPanel token={token} copy={copy} />
        </div>
      </Shell>
    );
  }

  if (attempt.isLoading) {
    return (
      <Shell>
        <Status
          label={copy.loadingLabel}
          title={copy.loadingTitle}
          description={copy.loadingDescription}
        />
      </Shell>
    );
  }

  if (terminal) {
    return (
      <Shell>
        <div className="space-y-6">
          <Status {...terminal} notice={copy.privacyNotice} />
          <HumanReviewPanel token={token} />
          <PrivacyAndRightsPanel token={token} copy={copy} />
        </div>
      </Shell>
    );
  }

  return (
    <CandidateIntegrityBoundary token={token}>
      <CandidateExperience token={token} />
    </CandidateIntegrityBoundary>
  );
}

function Start({ copy, onStart }: { copy: CandidateAccessCopy; onStart: () => void }) {
  const [accepted, setAccepted] = useState(false);

  return (
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-8">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
        {copy.beforeStart}
      </p>
      <h1 className="mt-3 text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">
        {copy.startTitle}
      </h1>
      <p className="mt-3 text-sm leading-6 text-muted-foreground">{copy.startDescription}</p>
      <div className="mt-5 rounded-xl border border-border bg-background/60 p-4 text-sm text-muted-foreground">
        <p className="font-medium text-foreground">{copy.instructionsTitle}</p>
        <ul className="mt-2 list-disc space-y-1 pl-5">
          {copy.instructions.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </div>
      <label className="mt-5 flex items-start gap-3 rounded-xl border border-border bg-background/60 p-4 text-sm leading-6 text-foreground">
        <input
          type="checkbox"
          className="mt-1"
          checked={accepted}
          onChange={(event) => setAccepted(event.target.checked)}
        />
        <span>{copy.consent}</span>
      </label>
      <a
        href="/privacidade"
        className="mt-3 inline-flex text-sm font-medium text-primary underline-offset-4 hover:underline"
      >
        {copy.privacyLink}
      </a>
      <button
        type="button"
        disabled={!accepted}
        onClick={onStart}
        className="mt-6 inline-flex w-full items-center justify-center rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {copy.startButton}
      </button>
    </section>
  );
}

function PrivacyAndRightsPanel({ token, copy }: { token: string; copy: CandidateAccessCopy }) {
  const [requestType, setRequestType] =
    useState<DataSubjectRequestType>("confirmationAccess");
  const [contact, setContact] = useState("");
  const [details, setDetails] = useState("");
  const [state, setState] = useState<RequestState>("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function submitRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (contact.trim() && !EMAIL_PATTERN.test(contact.trim())) {
      setState("error");
      setErrorMessage(copy.requestValidationError);
      return;
    }

    setState("sending");
    setErrorMessage(null);
    try {
      await requestDataSubjectRight(token, { requestType, contact, details });
      setState("success");
      setDetails("");
    } catch (error) {
      setState("error");
      setErrorMessage(error instanceof Error ? error.message : copy.requestErrorTitle);
    }
  }

  return (
    <section className="rounded-2xl border border-border bg-card p-5 shadow-sm sm:p-6">
      <div className="flex items-start gap-3">
        <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-primary" aria-hidden />
        <div>
          <h2 className="text-base font-semibold text-foreground">{copy.privacySummaryTitle}</h2>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">
            {copy.privacySummaryDescription}
          </p>
          <a
            href="/privacidade"
            className="mt-2 inline-flex text-sm font-medium text-primary underline-offset-4 hover:underline"
          >
            {copy.privacyLink}
          </a>
        </div>
      </div>

      <details className="mt-5 rounded-xl border border-border bg-background/60">
        <summary className="flex cursor-pointer list-none items-center justify-between gap-3 px-4 py-3 text-sm font-semibold text-foreground">
          <span>{copy.rightsTitle}</span>
          <ChevronDown className="h-4 w-4 text-muted-foreground" aria-hidden />
        </summary>
        <form onSubmit={submitRequest} className="space-y-4 border-t border-border p-4">
          <p className="text-sm leading-6 text-muted-foreground">{copy.rightsDescription}</p>

          <label className="block text-sm font-medium text-foreground">
            {copy.requestTypeLabel}
            <select
              value={requestType}
              onChange={(event) =>
                setRequestType(event.target.value as DataSubjectRequestType)
              }
              className="mt-1.5 min-h-11 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:ring-2 focus:ring-ring"
            >
              {Object.entries(copy.requestTypes).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>

          <label className="block text-sm font-medium text-foreground">
            {copy.contactLabel}
            <input
              type="email"
              value={contact}
              maxLength={320}
              onChange={(event) => setContact(event.target.value)}
              placeholder={copy.contactPlaceholder}
              className="mt-1.5 min-h-11 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:ring-2 focus:ring-ring"
            />
          </label>

          <label className="block text-sm font-medium text-foreground">
            {copy.detailsLabel}
            <textarea
              value={details}
              maxLength={1000}
              rows={4}
              onChange={(event) => setDetails(event.target.value)}
              placeholder={copy.detailsPlaceholder}
              className="mt-1.5 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:ring-2 focus:ring-ring"
            />
          </label>

          {state === "success" ? (
            <div
              className="rounded-lg border border-success/30 bg-success/10 p-3 text-sm text-foreground"
              role="status"
            >
              <p className="font-semibold">{copy.requestSuccessTitle}</p>
              <p className="mt-1 text-muted-foreground">{copy.requestSuccessDescription}</p>
            </div>
          ) : null}

          {state === "error" ? (
            <div
              className="rounded-lg border border-danger/30 bg-danger/10 p-3 text-sm text-foreground"
              role="alert"
            >
              <p className="font-semibold">{copy.requestErrorTitle}</p>
              <p className="mt-1 text-muted-foreground">{errorMessage}</p>
            </div>
          ) : null}

          <button
            type="submit"
            disabled={state === "sending"}
            className="inline-flex min-h-11 w-full items-center justify-center rounded-lg border border-primary bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {state === "sending" ? copy.sendingRequest : copy.sendRequest}
          </button>
        </form>
      </details>
    </section>
  );
}

function HumanReviewPanel({ token }: { token: string }) {
  const { language } = useLanguage();
  const copy = reviewCopy[language];
  const [reason, setReason] = useState("");
  const [state, setState] = useState<RequestState>("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function submitReview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setState("sending");
    setErrorMessage(null);
    try {
      await requestHumanReview(token, reason);
      setState("success");
      setReason("");
    } catch (error) {
      setState("error");
      setErrorMessage(error instanceof Error ? error.message : copy.error);
    }
  }

  return (
    <section className="rounded-2xl border border-border bg-card p-5 shadow-sm sm:p-6">
      <div className="flex items-start gap-3">
        <Scale className="mt-0.5 h-5 w-5 shrink-0 text-primary" aria-hidden />
        <div>
          <h2 className="text-base font-semibold text-foreground">{copy.title}</h2>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">{copy.description}</p>
        </div>
      </div>
      <form onSubmit={submitReview} className="mt-4 space-y-3">
        <label className="block text-sm font-medium text-foreground">
          {copy.reasonLabel}
          <textarea
            value={reason}
            maxLength={1000}
            rows={3}
            onChange={(event) => setReason(event.target.value)}
            placeholder={copy.reasonPlaceholder}
            className="mt-1.5 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:ring-2 focus:ring-ring"
          />
        </label>
        {state === "success" ? (
          <p className="rounded-lg border border-success/30 bg-success/10 p-3 text-sm" role="status">
            {copy.success}
          </p>
        ) : null}
        {state === "error" ? (
          <p className="rounded-lg border border-danger/30 bg-danger/10 p-3 text-sm" role="alert">
            {errorMessage ?? copy.error}
          </p>
        ) : null}
        <button
          type="submit"
          disabled={state === "sending"}
          className="inline-flex min-h-11 w-full items-center justify-center rounded-lg border border-border bg-background px-4 py-2 text-sm font-semibold text-foreground transition hover:bg-accent disabled:cursor-not-allowed disabled:opacity-60"
        >
          {state === "sending" ? copy.sending : copy.button}
        </button>
      </form>
    </section>
  );
}

type Terminal = {
  label: string;
  title: string;
  description: string;
  tone: "success" | "warning" | "muted";
};

function statusCopy(attempt: CandidateAttemptResponse, copy: CandidateAccessCopy): Terminal {
  if (attempt.status === "concluida") return { ...copy.completed, tone: "success" };
  if (attempt.status === "expirada") return { ...copy.expired, tone: "warning" };
  if (attempt.status === "abandonada") return { ...copy.abandoned, tone: "warning" };
  return { ...copy.closed, tone: "muted" };
}

function Status({
  label,
  title,
  description,
  tone = "muted",
  notice,
}: Terminal & { notice?: string }) {
  const color =
    tone === "success"
      ? "text-emerald-600"
      : tone === "warning"
        ? "text-amber-600"
        : "text-muted-foreground";

  return (
    <section className="rounded-2xl border border-border bg-card p-8 text-center shadow-sm">
      <p className={`text-xs font-semibold uppercase tracking-[0.18em] ${color}`}>{label}</p>
      <h1 className="mt-3 text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">
        {title}
      </h1>
      <p className="mx-auto mt-3 max-w-lg text-sm leading-6 text-muted-foreground">
        {description}
      </p>
      {notice ? (
        <div className="mx-auto mt-6 max-w-lg rounded-xl border border-border bg-background/60 p-4 text-xs leading-5 text-muted-foreground">
          {notice}
        </div>
      ) : null}
    </section>
  );
}

function Shell({ children }: { children: ReactNode }) {
  return (
    <main className="relative min-h-screen bg-background px-4 py-10 text-foreground">
      <div className="absolute right-4 top-4">
        <LanguageSelector />
      </div>
      <div className="mx-auto w-full max-w-2xl pt-10">{children}</div>
    </main>
  );
}
