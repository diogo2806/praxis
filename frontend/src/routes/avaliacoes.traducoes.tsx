import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { CheckCircle2, Download, Languages, Upload } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  approveSimulationLocale,
  configureSimulationLocales,
  exportSimulationLocale,
  getSimulationLocale,
  importSimulationLocale,
  listSimulationLocales,
  reviewSimulationLocale,
  saveSimulationLocale,
  type LocaleContent,
  type LocaleSummary,
} from "@/lib/api/simulation-localization";

export const Route = createFileRoute("/avaliacoes/traducoes")({
  head: () => ({ meta: [{ title: "Idiomas da avaliação - Práxis" }] }),
  component: SimulationTranslationsPage,
});

function SimulationTranslationsPage() {
  const queryClient = useQueryClient();
  const [versionInput, setVersionInput] = useState("");
  const [versionId, setVersionId] = useState<number>();
  const [baseLocale, setBaseLocale] = useState("pt-BR");
  const [enabledLocales, setEnabledLocales] = useState("pt-BR,en,es-MX");
  const [selectedLocale, setSelectedLocale] = useState("");
  const [editorJson, setEditorJson] = useState("");
  const [importJson, setImportJson] = useState("");

  const locales = useQuery({
    queryKey: ["simulation-locales", versionId],
    queryFn: () => listSimulationLocales(versionId!),
    enabled: versionId !== undefined,
  });
  const content = useQuery({
    queryKey: ["simulation-locale-content", versionId, selectedLocale],
    queryFn: () => getSimulationLocale(versionId!, selectedLocale),
    enabled: versionId !== undefined && Boolean(selectedLocale),
  });

  useEffect(() => {
    if (content.data) setEditorJson(JSON.stringify(content.data.content, null, 2));
  }, [content.data]);

  const configure = useMutation({
    mutationFn: () => configureSimulationLocales(versionId!, {
      baseLocale,
      enabledLocales: enabledLocales.split(",").map((item) => item.trim()).filter(Boolean),
    }),
    onSuccess: (data) => {
      setSelectedLocale(data.find((item) => !item.baseLocale)?.locale ?? data[0]?.locale ?? "");
      void queryClient.invalidateQueries({ queryKey: ["simulation-locales", versionId] });
    },
  });
  const save = useMutation({
    mutationFn: () => saveSimulationLocale(versionId!, selectedLocale, JSON.parse(editorJson) as LocaleContent),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["simulation-locales", versionId] });
      void queryClient.invalidateQueries({ queryKey: ["simulation-locale-content", versionId, selectedLocale] });
    },
  });
  const review = useMutation({
    mutationFn: () => reviewSimulationLocale(versionId!, selectedLocale),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["simulation-locales", versionId] }),
  });
  const approve = useMutation({
    mutationFn: () => approveSimulationLocale(versionId!, selectedLocale),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["simulation-locales", versionId] }),
  });
  const exportPackage = useMutation({
    mutationFn: () => exportSimulationLocale(versionId!, selectedLocale),
    onSuccess: (data) => downloadJson(data, `praxis-traducao-${versionId}-${selectedLocale}.json`),
  });
  const importPackage = useMutation({
    mutationFn: () => {
      const parsed = JSON.parse(importJson) as { locale?: string; content?: LocaleContent };
      if (!parsed.locale || !parsed.content) throw new Error("O pacote precisa conter locale e content.");
      return importSimulationLocale(versionId!, {
        locale: parsed.locale,
        content: parsed.content,
        replaceExisting: true,
      });
    },
    onSuccess: (data) => {
      setSelectedLocale(data.locale);
      void queryClient.invalidateQueries({ queryKey: ["simulation-locales", versionId] });
    },
  });

  const operationError = locales.error ?? content.error ?? configure.error ?? save.error ?? review.error
    ?? approve.error ?? exportPackage.error ?? importPackage.error;

  return <AppShell>
    <main className="mx-auto max-w-7xl space-y-6">
      <header>
        <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">Conteúdo versionado por idioma</div>
        <h1 className="mt-1 font-display text-3xl">Idiomas da avaliação</h1>
        <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
          Traduza textos, acessibilidade e relatórios preservando os mesmos nós, alternativas, pesos, destinos e fórmula de pontuação.
        </p>
      </header>

      {operationError && <StateBanner tone="danger" title="Não foi possível concluir a operação">{operationError instanceof Error ? operationError.message : String(operationError)}</StateBanner>}

      <Card title="Versão e idiomas" icon={<Languages className="h-5 w-5 text-primary" />}>
        <div className="grid gap-3 md:grid-cols-3">
          <Field label="ID da versão"><input className="input" type="number" min={1} value={versionInput} onChange={(event) => setVersionInput(event.target.value)} /></Field>
          <Field label="Idioma base"><input className="input" value={baseLocale} onChange={(event) => setBaseLocale(event.target.value)} /></Field>
          <Field label="Idiomas habilitados"><input className="input" value={enabledLocales} onChange={(event) => setEnabledLocales(event.target.value)} /></Field>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <Button variant="outline" disabled={!versionInput} onClick={() => setVersionId(Number(versionInput))}>Carregar versão</Button>
          <Button disabled={!versionId || configure.isPending} onClick={() => configure.mutate()}>Configurar idiomas</Button>
        </div>
      </Card>

      {versionId && <Card title="Status de tradução" icon={<CheckCircle2 className="h-5 w-5 text-primary" />}>
        <div className="grid gap-3 md:grid-cols-3">
          {(locales.data ?? []).map((locale) => <LocaleCard key={locale.locale} locale={locale} selected={locale.locale === selectedLocale} onSelect={() => setSelectedLocale(locale.locale)} />)}
        </div>
      </Card>}

      {content.data && <Card title={`Editor ${selectedLocale} · revisão ${content.data.revision}`} icon={<Languages className="h-5 w-5 text-primary" />}>
        {content.data.validationErrors.map((item) => <StateBanner key={item} tone="danger" title="Estrutura inválida">{item}</StateBanner>)}
        {content.data.validationWarnings.map((item) => <StateBanner key={item} tone="warning" title="Acessibilidade incompleta">{item}</StateBanner>)}
        <p className="mb-3 text-sm text-muted-foreground">O pacote contém apenas campos traduzíveis. IDs ausentes, duplicados ou desconhecidos bloqueiam revisão e aprovação.</p>
        <textarea className="min-h-[34rem] w-full rounded-xl border border-input bg-background p-4 font-mono text-xs" value={editorJson} onChange={(event) => setEditorJson(event.target.value)} readOnly={content.data.baseLocale || content.data.status === "APPROVED"} spellCheck={false} />
        <div className="mt-4 flex flex-wrap gap-2">
          <Button disabled={content.data.baseLocale || content.data.status === "APPROVED" || save.isPending} onClick={() => save.mutate()}>Salvar rascunho</Button>
          <Button variant="outline" disabled={content.data.baseLocale || content.data.status !== "DRAFT" || review.isPending} onClick={() => review.mutate()}>Enviar para revisão</Button>
          <Button variant="outline" disabled={content.data.baseLocale || content.data.status !== "IN_REVIEW" || approve.isPending} onClick={() => approve.mutate()}>Aprovar idioma</Button>
          <Button variant="outline" disabled={exportPackage.isPending} onClick={() => exportPackage.mutate()}><Download className="mr-2 h-4 w-4" />Exportar JSON</Button>
        </div>
      </Card>}

      {versionId && <Card title="Importar pacote revisado" icon={<Upload className="h-5 w-5 text-primary" />}>
        <p className="mb-3 text-sm text-muted-foreground">Cole o JSON exportado. O importador preserva IDs e rejeita qualquer divergência estrutural.</p>
        <textarea className="min-h-52 w-full rounded-xl border border-input bg-background p-4 font-mono text-xs" value={importJson} onChange={(event) => setImportJson(event.target.value)} spellCheck={false} />
        <Button className="mt-4" disabled={!importJson || importPackage.isPending} onClick={() => importPackage.mutate()}>Importar e validar</Button>
      </Card>}
    </main>
  </AppShell>;
}

function LocaleCard({ locale, selected, onSelect }: { locale: LocaleSummary; selected: boolean; onSelect: () => void }) {
  return <button type="button" onClick={onSelect} className={`rounded-xl border p-4 text-left ${selected ? "border-primary bg-primary/10" : "border-border bg-background hover:bg-accent"}`}>
    <div className="flex items-center justify-between gap-3"><strong>{locale.locale}</strong><span className="text-xs">{locale.status}</span></div>
    <div className="mt-2 text-sm text-muted-foreground">{locale.baseLocale ? "Idioma base" : `Revisão ${locale.revision}`} · {locale.completenessPercent}%</div>
    <div className="mt-3 h-2 overflow-hidden rounded bg-muted"><div className="h-full bg-primary" style={{ width: `${locale.completenessPercent}%` }} /></div>
  </button>;
}

function Card({ title, icon, children }: { title: string; icon: ReactNode; children: ReactNode }) { return <section className="rounded-xl border border-border bg-card p-5"><div className="mb-4 flex items-center gap-2">{icon}<h2 className="font-semibold">{title}</h2></div>{children}</section>; }
function Field({ label, children }: { label: string; children: ReactNode }) { return <label className="block text-sm font-medium"><span className="mb-1 block">{label}</span>{children}</label>; }
function downloadJson(data: unknown, filename: string) { const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" }); const url = URL.createObjectURL(blob); const anchor = document.createElement("a"); anchor.href = url; anchor.download = filename; anchor.click(); URL.revokeObjectURL(url); }
