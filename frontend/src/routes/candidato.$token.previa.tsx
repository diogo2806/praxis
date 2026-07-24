import { useMutation, useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { ArrowRight, CheckCircle2, Volume2 } from "lucide-react";
import { useState } from "react";

import { LanguageSelector } from "@/components/language-selector";
import {
  presentCandidateRealisticPreview,
  reactToCandidateRealisticPreview,
  type PreviewDisplayStage,
  type PreviewMediaItem,
} from "@/lib/api/realistic-job-previews";

export const Route = createFileRoute("/candidato/$token/previa")({
  validateSearch: (search: Record<string, unknown>) => ({
    stage: search.stage === "AFTER" ? "AFTER" as const : "BEFORE" as const,
    next: typeof search.next === "string" ? search.next : undefined,
  }),
  component: CandidateRealisticPreviewPage,
});

function CandidateRealisticPreviewPage() {
  const { token } = Route.useParams();
  const { stage, next } = Route.useSearch();
  const preview = useQuery({
    queryKey: ["candidate-realistic-preview", token, stage],
    queryFn: () => presentCandidateRealisticPreview(token, stage),
    retry: false,
  });
  const [acknowledged, setAcknowledged] = useState(false);
  const [voluntaryWithdrawal, setVoluntaryWithdrawal] = useState(false);
  const [clarityScore, setClarityScore] = useState<number>();
  const [realismScore, setRealismScore] = useState<number>();
  const [compatibilityScore, setCompatibilityScore] = useState<number>();

  const reaction = useMutation({
    mutationFn: () => reactToCandidateRealisticPreview(token, preview.data!.versionId, stage, {
      acknowledged,
      voluntaryWithdrawal,
      clarityScore,
      realismScore,
      expectationCompatibilityScore: compatibilityScore,
    }),
    onSuccess: () => {
      if (next && next.startsWith("/")) window.location.assign(next);
    },
  });

  if (preview.isLoading) return <Shell><Status title="Carregando a prévia da função" description="Estamos preparando o conteúdo aplicável a esta etapa." /></Shell>;
  if (preview.isError || !preview.data) return <Shell><Status title="Prévia não disponível" description={preview.error instanceof Error ? preview.error.message : "Nenhuma prévia foi configurada para esta etapa."} /></Shell>;

  const data = preview.data;
  const content = data.content;
  const canContinue = !data.acknowledgementRequired || acknowledged;

  return <Shell>
    <article className="space-y-6 rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-8">
      <header>
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">Prévia realista · versão {data.versionNumber}</p>
        <h1 className="mt-3 text-2xl font-semibold sm:text-3xl">{data.title}</h1>
        <p className="mt-3 rounded-xl border border-border bg-background/60 p-4 text-sm leading-6 text-muted-foreground">{data.informationalNotice}</p>
      </header>

      <div className="grid gap-4 md:grid-cols-2">
        <Section title="Responsabilidades" text={content.responsibilities} />
        <Section title="Autonomia" text={content.autonomy} />
        <Section title="Pressão e ritmo" text={content.pressureContext} />
        <Section title="Contato com pessoas" text={content.contactFrequency} />
        <Section title="Situações críticas" text={content.criticalSituations} />
        <Section title="Rotina" text={content.routineDescription} />
        <Section title="Condições de trabalho" text={content.workConditions} />
        <Section title="Aspectos positivos" text={content.positiveAspects} />
      </div>

      {content.media.length > 0 && <section aria-labelledby="preview-media-title"><h2 id="preview-media-title" className="font-semibold">Conteúdo multimídia acessível</h2><div className="mt-3 space-y-4">{content.media.map((item) => <Media key={`${item.type}-${item.url}`} item={item} />)}</div></section>}

      <details className="rounded-xl border border-border bg-background/60 p-4"><summary className="cursor-pointer font-semibold">Alternativa textual completa</summary><p className="mt-3 whitespace-pre-line text-sm leading-6 text-muted-foreground">{content.alternativeText}</p></details>

      <section className="space-y-4 rounded-xl border border-border bg-background/60 p-4" aria-labelledby="preview-reaction-title">
        <h2 id="preview-reaction-title" className="font-semibold">Sua percepção, opcional</h2>
        <div className="grid gap-4 sm:grid-cols-3"><Rating label="Clareza" value={clarityScore} onChange={setClarityScore} /><Rating label="Realismo percebido" value={realismScore} onChange={setRealismScore} /><Rating label="Compatibilidade de expectativas" value={compatibilityScore} onChange={setCompatibilityScore} /></div>
        <label className="flex items-start gap-3 text-sm"><input type="checkbox" className="mt-1" checked={voluntaryWithdrawal} onChange={(event) => setVoluntaryWithdrawal(event.target.checked)} /><span>Após conhecer a rotina, prefiro não continuar. Esta escolha é registrada separadamente da pontuação.</span></label>
        {data.acknowledgementRequired && <label className="flex items-start gap-3 text-sm"><input type="checkbox" className="mt-1" checked={acknowledged} onChange={(event) => setAcknowledged(event.target.checked)} /><span>Estou ciente de que este conteúdo é informativo e não representa aceite contratual.</span></label>}
      </section>

      {reaction.isError && <p role="alert" className="rounded-lg border border-danger/30 bg-danger/10 p-3 text-sm">{reaction.error instanceof Error ? reaction.error.message : "Não foi possível registrar a reação."}</p>}
      {reaction.isSuccess && !next && <p role="status" className="flex items-center gap-2 rounded-lg border border-success/30 bg-success/10 p-3 text-sm"><CheckCircle2 className="h-4 w-4" />Reação registrada.</p>}
      <button type="button" disabled={!canContinue || reaction.isPending} onClick={() => reaction.mutate()} className="inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground disabled:cursor-not-allowed disabled:opacity-60">{reaction.isPending ? "Registrando..." : next ? "Continuar" : "Registrar percepção"}<ArrowRight className="h-4 w-4" /></button>
    </article>
  </Shell>;
}

function Media({ item }: { item: PreviewMediaItem }) {
  if (item.type === "IMAGE") return <figure><img src={item.url} alt={item.alternativeText} className="max-h-96 w-full rounded-xl object-contain" /><figcaption className="mt-2 text-sm text-muted-foreground">{item.alternativeText}</figcaption></figure>;
  if (item.type === "AUDIO") return <div><div className="mb-2 flex items-center gap-2 text-sm font-medium"><Volume2 className="h-4 w-4" />{item.alternativeText}</div><audio controls className="w-full" src={item.url}>{item.alternativeText}</audio>{item.transcriptUrl && <a className="mt-2 inline-flex text-sm text-primary underline" href={item.transcriptUrl} target="_blank" rel="noreferrer">Abrir transcrição</a>}</div>;
  return <div><video controls className="max-h-96 w-full rounded-xl" src={item.url}>{item.alternativeText}</video><p className="mt-2 text-sm text-muted-foreground">{item.alternativeText}</p>{item.transcriptUrl && <a className="mt-2 inline-flex text-sm text-primary underline" href={item.transcriptUrl} target="_blank" rel="noreferrer">Abrir transcrição</a>}</div>;
}

function Section({ title, text }: { title: string; text: string }) { return <section className="rounded-xl border border-border p-4"><h2 className="font-semibold">{title}</h2><p className="mt-2 whitespace-pre-line text-sm leading-6 text-muted-foreground">{text}</p></section>; }
function Rating({ label, value, onChange }: { label: string; value?: number; onChange: (value: number | undefined) => void }) { return <label className="text-sm font-medium">{label}<select className="input mt-1" value={value ?? ""} onChange={(event) => onChange(event.target.value ? Number(event.target.value) : undefined)}><option value="">Não responder</option>{[1, 2, 3, 4, 5].map((score) => <option key={score} value={score}>{score}</option>)}</select></label>; }
function Status({ title, description }: { title: string; description: string }) { return <section className="rounded-2xl border border-border bg-card p-8 text-center"><h1 className="text-2xl font-semibold">{title}</h1><p className="mt-3 text-sm text-muted-foreground">{description}</p></section>; }
function Shell({ children }: { children: React.ReactNode }) { return <main className="relative min-h-screen bg-background px-4 py-10 text-foreground"><div className="absolute right-4 top-4"><LanguageSelector /></div><div className="mx-auto w-full max-w-5xl pt-10">{children}</div></main>; }
