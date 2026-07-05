import { useMemo, useState, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { LanguageSelector } from "@/components/language-selector";
import { getCandidateAttempt, type CandidateAttemptResponse } from "@/lib/api/praxis";
import { useLanguage } from "@/lib/language-context";
import { CandidateExperience } from "@/routes/candidato";

type CandidateAccessCopy = ReturnType<typeof useLanguage>["t"]["candidateAccess"];

export const Route = createFileRoute("/candidato/$token")({ component: TokenCandidatePage });

function TokenCandidatePage() {
  const { token } = Route.useParams();
  const { t } = useLanguage();
  const copy = t.candidateAccess;
  const [ready, setReady] = useState(false);
  const attempt = useQuery({ queryKey: ["candidate-attempt", token], queryFn: () => getCandidateAttempt(token), enabled: ready });
  const terminal = useMemo(() => !attempt.data || attempt.data.etapaAtual ? null : statusCopy(attempt.data, copy), [attempt.data, copy]);
  if (!ready) return <Start copy={copy} onStart={() => setReady(true)} />;
  if (attempt.isLoading) return <Shell><Status label={copy.loadingLabel} title={copy.loadingTitle} description={copy.loadingDescription} /></Shell>;
  if (terminal) return <Shell><Status {...terminal} notice={copy.privacyNotice} /></Shell>;
  return <CandidateExperience token={token} />;
}

function Start({ copy, onStart }: { copy: CandidateAccessCopy; onStart: () => void }) {
  const [accepted, setAccepted] = useState(false);
  return <Shell><section className="rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-8">
    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">{copy.beforeStart}</p>
    <h1 className="mt-3 text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">{copy.startTitle}</h1>
    <p className="mt-3 text-sm leading-6 text-muted-foreground">{copy.startDescription}</p>
    <div className="mt-5 rounded-xl border border-border bg-background/60 p-4 text-sm text-muted-foreground"><p className="font-medium text-foreground">{copy.instructionsTitle}</p><ul className="mt-2 list-disc space-y-1 pl-5">{copy.instructions.map((item) => <li key={item}>{item}</li>)}</ul></div>
    <label className="mt-5 flex items-start gap-3 rounded-xl border border-border bg-background/60 p-4 text-sm leading-6 text-foreground"><input type="checkbox" className="mt-1" checked={accepted} onChange={(event) => setAccepted(event.target.checked)} /><span>{copy.consent}</span></label>
    <button type="button" disabled={!accepted} onClick={onStart} className="mt-6 inline-flex w-full items-center justify-center rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60">{copy.startButton}</button>
  </section></Shell>;
}

type Terminal = { label: string; title: string; description: string; tone: "success" | "warning" | "muted" };
function statusCopy(attempt: CandidateAttemptResponse, copy: CandidateAccessCopy): Terminal {
  if (attempt.status === "concluida") return { ...copy.completed, tone: "success" };
  if (attempt.status === "expirada") return { ...copy.expired, tone: "warning" };
  if (attempt.status === "abandonada") return { ...copy.abandoned, tone: "warning" };
  return { ...copy.closed, tone: "muted" };
}
function Status({ label, title, description, tone = "muted", notice }: Terminal & { notice?: string }) {
  const color = tone === "success" ? "text-emerald-600" : tone === "warning" ? "text-amber-600" : "text-muted-foreground";
  return <section className="rounded-2xl border border-border bg-card p-8 text-center shadow-sm"><p className={`text-xs font-semibold uppercase tracking-[0.18em] ${color}`}>{label}</p><h1 className="mt-3 text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">{title}</h1><p className="mx-auto mt-3 max-w-lg text-sm leading-6 text-muted-foreground">{description}</p>{notice && <div className="mx-auto mt-6 max-w-lg rounded-xl border border-border bg-background/60 p-4 text-xs leading-5 text-muted-foreground">{notice}</div>}</section>;
}
function Shell({ children }: { children: ReactNode }) { return <main className="relative flex min-h-screen items-center justify-center bg-background px-4 py-10 text-foreground"><div className="absolute right-4 top-4"><LanguageSelector /></div><div className="w-full max-w-2xl">{children}</div></main>; }
