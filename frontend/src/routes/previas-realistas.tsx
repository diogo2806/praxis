import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { BarChart3, BriefcaseBusiness, CheckCircle2, Plus, Send } from "lucide-react";
import { useState, type ReactNode } from "react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  createRealisticJobPreview,
  getRealisticJobPreviewMetrics,
  listRealisticJobPreviews,
  publishRealisticJobPreview,
  type CreatePreviewInput,
  type PreviewMetrics,
  type PreviewSummary,
} from "@/lib/api/realistic-job-previews";

export const Route = createFileRoute("/previas-realistas")({
  head: () => ({ meta: [{ title: "Prévias realistas - Práxis" }] }),
  component: RealisticJobPreviewsPage,
});

const EMPTY_CONTENT = {
  responsibilities: "",
  autonomy: "",
  pressureContext: "",
  contactFrequency: "",
  criticalSituations: "",
  routineDescription: "",
  workConditions: "",
  positiveAspects: "",
  alternativeText: "",
  media: [],
  scenarioNodeIds: [],
};

function RealisticJobPreviewsPage() {
  const queryClient = useQueryClient();
  const previews = useQuery({ queryKey: ["realistic-job-previews"], queryFn: listRealisticJobPreviews });
  const [form, setForm] = useState<CreatePreviewInput>({
    scopeType: "JOB",
    scopeKey: "",
    title: "",
    displayPosition: "BEFORE",
    acknowledgementRequired: false,
    content: { ...EMPTY_CONTENT },
  });
  const [scenarioIds, setScenarioIds] = useState("");
  const [media, setMedia] = useState({ type: "IMAGE" as const, url: "", alternativeText: "", transcriptUrl: "" });
  const [metrics, setMetrics] = useState<PreviewMetrics | null>(null);

  const createMutation = useMutation({
    mutationFn: () => createRealisticJobPreview({
      ...form,
      content: {
        ...form.content,
        scenarioNodeIds: scenarioIds.split(",").map((item) => item.trim()).filter(Boolean),
        media: media.url ? [{ ...media, transcriptUrl: media.transcriptUrl || undefined }] : [],
      },
    }),
    onSuccess: () => {
      setForm({ scopeType: "JOB", scopeKey: "", title: "", displayPosition: "BEFORE", acknowledgementRequired: false, content: { ...EMPTY_CONTENT } });
      setScenarioIds("");
      setMedia({ type: "IMAGE", url: "", alternativeText: "", transcriptUrl: "" });
      void queryClient.invalidateQueries({ queryKey: ["realistic-job-previews"] });
    },
  });

  const publishMutation = useMutation({
    mutationFn: publishRealisticJobPreview,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["realistic-job-previews"] }),
  });
  const metricsMutation = useMutation({
    mutationFn: ({ id, version }: { id: string; version?: number }) => getRealisticJobPreviewMetrics(id, version),
    onSuccess: setMetrics,
  });
  const error = previews.error ?? createMutation.error ?? publishMutation.error ?? metricsMutation.error;

  function setContent(field: keyof typeof EMPTY_CONTENT, value: string) {
    setForm((current) => ({ ...current, content: { ...current.content, [field]: value } }));
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header>
          <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">Employer branding com transparência</div>
          <h1 className="mt-1 font-display text-3xl">Prévias realistas da vaga</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
            Apresente aspectos positivos e difíceis do trabalho antes ou depois da avaliação. O conteúdo é informativo e nunca altera a nota.
          </p>
        </header>

        {error && <StateBanner tone="danger" title="Não foi possível concluir a operação">{error instanceof Error ? error.message : String(error)}</StateBanner>}

        <Card title="Nova prévia" icon={<Plus className="h-5 w-5 text-primary" />}>
          <div className="grid gap-3 md:grid-cols-4">
            <Field label="Escopo"><select className="input" value={form.scopeType} onChange={(event) => setForm({ ...form, scopeType: event.target.value as CreatePreviewInput["scopeType"] })}><option value="JOB">Vaga</option><option value="ROLE">Cargo</option><option value="JOURNEY">Jornada</option></select></Field>
            <Field label="Identificador do escopo"><input className="input" value={form.scopeKey} onChange={(event) => setForm({ ...form, scopeKey: event.target.value })} /></Field>
            <Field label="Título"><input className="input" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></Field>
            <Field label="Exibição"><select className="input" value={form.displayPosition} onChange={(event) => setForm({ ...form, displayPosition: event.target.value as CreatePreviewInput["displayPosition"] })}><option value="BEFORE">Antes</option><option value="AFTER">Depois</option><option value="BOTH">Antes e depois</option></select></Field>
          </div>
          <label className="mt-3 flex items-center gap-2 text-sm"><input type="checkbox" checked={form.acknowledgementRequired} onChange={(event) => setForm({ ...form, acknowledgementRequired: event.target.checked })} />Exigir ciência informativa</label>
          <div className="mt-5 grid gap-3 md:grid-cols-2">
            <TextArea label="Responsabilidades" value={form.content.responsibilities} onChange={(value) => setContent("responsibilities", value)} />
            <TextArea label="Autonomia" value={form.content.autonomy} onChange={(value) => setContent("autonomy", value)} />
            <TextArea label="Pressão e ritmo" value={form.content.pressureContext} onChange={(value) => setContent("pressureContext", value)} />
            <TextArea label="Frequência de contato" value={form.content.contactFrequency} onChange={(value) => setContent("contactFrequency", value)} />
            <TextArea label="Situações críticas" value={form.content.criticalSituations} onChange={(value) => setContent("criticalSituations", value)} />
            <TextArea label="Rotina" value={form.content.routineDescription} onChange={(value) => setContent("routineDescription", value)} />
            <TextArea label="Condições de trabalho" value={form.content.workConditions} onChange={(value) => setContent("workConditions", value)} />
            <TextArea label="Aspectos positivos" value={form.content.positiveAspects} onChange={(value) => setContent("positiveAspects", value)} />
          </div>
          <div className="mt-3 grid gap-3 md:grid-cols-2">
            <TextArea label="Alternativa textual completa" value={form.content.alternativeText} onChange={(value) => setContent("alternativeText", value)} />
            <Field label="IDs de cenários relacionados, separados por vírgula"><input className="input" value={scenarioIds} onChange={(event) => setScenarioIds(event.target.value)} /></Field>
          </div>
          <div className="mt-5 grid gap-3 md:grid-cols-4">
            <Field label="Mídia"><select className="input" value={media.type} onChange={(event) => setMedia({ ...media, type: event.target.value as typeof media.type })}><option value="IMAGE">Imagem</option><option value="AUDIO">Áudio</option><option value="VIDEO">Vídeo</option></select></Field>
            <Field label="URL da mídia"><input className="input" value={media.url} onChange={(event) => setMedia({ ...media, url: event.target.value })} /></Field>
            <Field label="Texto alternativo"><input className="input" value={media.alternativeText} onChange={(event) => setMedia({ ...media, alternativeText: event.target.value })} /></Field>
            <Field label="URL da transcrição"><input className="input" value={media.transcriptUrl} onChange={(event) => setMedia({ ...media, transcriptUrl: event.target.value })} /></Field>
          </div>
          <Button className="mt-5" disabled={createMutation.isPending || !form.scopeKey || !form.title || !form.content.positiveAspects || !form.content.criticalSituations || !form.content.workConditions} onClick={() => createMutation.mutate()}>
            Criar rascunho versionado
          </Button>
        </Card>

        <Card title="Prévias cadastradas" icon={<BriefcaseBusiness className="h-5 w-5 text-primary" />}>
          <div className="grid gap-4 lg:grid-cols-2">
            {(previews.data ?? []).map((preview) => <PreviewCard key={preview.id} preview={preview} onPublish={() => publishMutation.mutate(preview.id)} onMetrics={() => metricsMutation.mutate({ id: preview.id, version: preview.activeVersionNumber ?? undefined })} />)}
            {!previews.isLoading && (previews.data?.length ?? 0) === 0 && <p className="text-sm text-muted-foreground">Nenhuma prévia cadastrada.</p>}
          </div>
        </Card>

        {metrics && <Card title="Métricas agregadas" icon={<BarChart3 className="h-5 w-5 text-primary" />}>
          <div className="grid gap-3 sm:grid-cols-3 lg:grid-cols-6"><Metric label="Visualizações" value={metrics.presentations} /><Metric label="Ciência" value={`${metrics.acknowledgementRatePercent}%`} /><Metric label="Desistência" value={`${metrics.withdrawalRatePercent}%`} /><Metric label="Clareza" value={format(metrics.averageClarity)} /><Metric label="Realismo" value={format(metrics.averageRealism)} /><Metric label="Compatibilidade" value={format(metrics.averageExpectationCompatibility)} /></div>
          <p className="mt-4 text-sm text-muted-foreground">{metrics.privacyNotice}</p>
        </Card>}
      </main>
    </AppShell>
  );
}

function PreviewCard({ preview, onPublish, onMetrics }: { preview: PreviewSummary; onPublish: () => void; onMetrics: () => void }) {
  return <article className="rounded-xl border border-border p-4"><div className="flex items-start justify-between gap-3"><div><h3 className="font-semibold">{preview.title}</h3><p className="text-sm text-muted-foreground">{preview.scopeType} · {preview.scopeKey}</p></div>{preview.activeVersionNumber ? <span className="inline-flex items-center gap-1 text-xs text-emerald-700"><CheckCircle2 className="h-4 w-4" />v{preview.activeVersionNumber}</span> : <span className="text-xs text-amber-700">Não publicada</span>}</div><div className="mt-4 flex flex-wrap gap-2"><Button size="sm" onClick={onPublish}><Send className="mr-1 h-4 w-4" />Publicar v{preview.draftVersionNumber}</Button><Button size="sm" variant="outline" disabled={!preview.activeVersionNumber} onClick={onMetrics}>Métricas</Button></div></article>;
}

function Card({ title, icon, children }: { title: string; icon: ReactNode; children: ReactNode }) {
  return <section className="rounded-xl border border-border bg-card p-5"><div className="mb-4 flex items-center gap-2">{icon}<h2 className="font-semibold">{title}</h2></div>{children}</section>;
}
function Field({ label, children }: { label: string; children: ReactNode }) { return <label className="block text-sm font-medium"><span className="mb-1 block">{label}</span>{children}</label>; }
function TextArea({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) { return <Field label={label}><textarea className="min-h-24 w-full rounded-md border border-input bg-background px-3 py-2" value={value} onChange={(event) => onChange(event.target.value)} /></Field>; }
function Metric({ label, value }: { label: string; value: string | number }) { return <div className="rounded-lg border border-border p-3"><div className="text-xs uppercase text-muted-foreground">{label}</div><div className="mt-1 text-xl font-semibold">{value}</div></div>; }
function format(value: number | null) { return value === null ? "Suprimido" : value.toLocaleString("pt-BR", { maximumFractionDigits: 2 }); }
