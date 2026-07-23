import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  CheckCircle2,
  CopyPlus,
  GitCompareArrows,
  LibraryBig,
  Search,
  Send,
  ShieldCheck,
  Star,
} from "lucide-react";
import { useMemo, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  createAssessmentTemplate,
  instantiateAssessmentTemplate,
  listAssessmentTemplates,
  reviewAssessmentTemplate,
  submitAssessmentTemplate,
  toggleAssessmentTemplateFavorite,
  type AssessmentTemplate,
  type CreateTemplateInput,
  type TemplateFilters,
  type TemplateScope,
} from "@/lib/api/assessment-template-catalog";
import { getSimulationVersion } from "@/lib/api/praxis";

export const Route = createFileRoute("/avaliacoes/modelos")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Biblioteca de modelos - Práxis" },
      {
        name: "description",
        content:
          "Pesquise, compare, favorite, revise e reutilize modelos governados de avaliação.",
      },
    ],
  }),
  component: AssessmentTemplateCatalogPage,
});

function AssessmentTemplateCatalogPage() {
  const search = Route.useSearch();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<TemplateFilters>({});
  const [comparison, setComparison] = useState<string[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<AssessmentTemplate | null>(null);
  const [newAssessmentName, setNewAssessmentName] = useState("");
  const [reviewNote, setReviewNote] = useState("");
  const queryKey = ["assessment-template-catalog", filters];

  const templatesQuery = useQuery({
    queryKey,
    queryFn: () => listAssessmentTemplates(filters),
  });
  const sourceQuery = useQuery({
    queryKey: ["assessment-template-source", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: Boolean(search.simulationId && search.versionNumber),
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["assessment-template-catalog"] });

  const favoriteMutation = useMutation({
    mutationFn: toggleAssessmentTemplateFavorite,
    onSuccess: refresh,
  });
  const submitMutation = useMutation({
    mutationFn: submitAssessmentTemplate,
    onSuccess: refresh,
  });
  const reviewMutation = useMutation({
    mutationFn: (input: { id: string; decision: "APPROVED" | "REJECTED" }) =>
      reviewAssessmentTemplate(input.id, input.decision, reviewNote),
    onSuccess: () => {
      setReviewNote("");
      void refresh();
    },
  });
  const instantiateMutation = useMutation({
    mutationFn: (input: { id: string; name: string }) =>
      instantiateAssessmentTemplate(input.id, input.name),
    onSuccess: () => {
      setSelectedTemplate(null);
      setNewAssessmentName("");
    },
  });

  const comparedTemplates = useMemo(
    () => (templatesQuery.data ?? []).filter((template) => comparison.includes(template.id)),
    [comparison, templatesQuery.data],
  );
  const operationError =
    favoriteMutation.error ??
    submitMutation.error ??
    reviewMutation.error ??
    instantiateMutation.error;

  function toggleCompare(id: string) {
    setComparison((current) => {
      if (current.includes(id)) return current.filter((item) => item !== id);
      if (current.length >= 3) return [...current.slice(1), id];
      return [...current, id];
    });
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Reutilização com governança
            </div>
            <h1 className="mt-1 font-display text-3xl">Biblioteca de modelos</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Modelos aprovados aceleram a autoria sem vincular avaliações futuras ao conteúdo original.
              Cada uso gera uma cópia independente em rascunho e preserva a origem na auditoria.
            </p>
          </div>
          <Button asChild variant="outline">
            <Link to="/avaliacoes">Central de Avaliações</Link>
          </Button>
        </header>

        {operationError && <ErrorBanner error={operationError} />}
        {sourceQuery.data && search.simulationId && search.versionNumber && (
          <CreateTemplateCard
            source={sourceQuery.data}
            simulationId={search.simulationId}
            versionNumber={search.versionNumber}
            onCreated={refresh}
          />
        )}

        <FilterPanel filters={filters} setFilters={setFilters} />

        {templatesQuery.isLoading ? (
          <LoadingCard />
        ) : templatesQuery.error ? (
          <ErrorBanner error={templatesQuery.error} />
        ) : templatesQuery.data?.length ? (
          <section className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
            {templatesQuery.data.map((template) => (
              <TemplateCard
                key={template.id}
                template={template}
                compared={comparison.includes(template.id)}
                toggleCompare={() => toggleCompare(template.id)}
                toggleFavorite={() => favoriteMutation.mutate(template.id)}
                select={() => {
                  setSelectedTemplate(template);
                  setNewAssessmentName(`${template.title} - cópia`);
                }}
                submit={() => submitMutation.mutate(template.id)}
                review={(decision) => reviewMutation.mutate({ id: template.id, decision })}
                reviewNote={reviewNote}
                setReviewNote={setReviewNote}
              />
            ))}
          </section>
        ) : (
          <StateBanner tone="warning" title="Nenhum modelo encontrado">
            Ajuste os filtros ou publique uma versão como modelo interno da empresa.
          </StateBanner>
        )}

        {comparedTemplates.length > 1 && <ComparisonTable templates={comparedTemplates} />}

        {selectedTemplate && (
          <InstantiatePanel
            template={selectedTemplate}
            name={newAssessmentName}
            setName={setNewAssessmentName}
            close={() => setSelectedTemplate(null)}
            instantiate={() =>
              instantiateMutation.mutate({ id: selectedTemplate.id, name: newAssessmentName.trim() })
            }
            pending={instantiateMutation.isPending}
          />
        )}

        {instantiateMutation.data && (
          <StateBanner tone="ok" title="Rascunho independente criado">
            A avaliação <strong>{instantiateMutation.data.simulationId}</strong>, versão {instantiateMutation.data.versionNumber}, foi criada em {instantiateMutation.data.status}.
          </StateBanner>
        )}
      </main>
    </AppShell>
  );
}

function FilterPanel({
  filters,
  setFilters,
}: {
  filters: TemplateFilters;
  setFilters: (filters: TemplateFilters) => void;
}) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-center gap-2">
        <Search className="h-5 w-5 text-primary" />
        <h2 className="font-semibold">Pesquisar e filtrar</h2>
      </div>
      <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <FilterInput label="Busca geral" value={filters.query} onChange={(value) => setFilters({ ...filters, query: value })} />
        <FilterInput label="Cargo" value={filters.jobRole} onChange={(value) => setFilters({ ...filters, jobRole: value })} />
        <FilterInput label="Área" value={filters.businessArea} onChange={(value) => setFilters({ ...filters, businessArea: value })} />
        <FilterInput label="Competência" value={filters.competency} onChange={(value) => setFilters({ ...filters, competency: value })} />
        <FilterInput label="Senioridade" value={filters.seniority} onChange={(value) => setFilters({ ...filters, seniority: value })} />
        <FilterInput label="Setor" value={filters.sector} onChange={(value) => setFilters({ ...filters, sector: value })} />
        <FilterInput label="Idioma" value={filters.languageCode} onChange={(value) => setFilters({ ...filters, languageCode: value })} />
        <FilterInput label="Complexidade" value={filters.complexity} onChange={(value) => setFilters({ ...filters, complexity: value })} />
      </div>
      <label className="mt-4 flex items-center gap-2 text-sm">
        <input
          type="checkbox"
          checked={Boolean(filters.favoriteOnly)}
          onChange={(event) => setFilters({ ...filters, favoriteOnly: event.target.checked })}
        />
        Mostrar somente favoritos
      </label>
    </section>
  );
}

function TemplateCard({
  template,
  compared,
  toggleCompare,
  toggleFavorite,
  select,
  submit,
  review,
  reviewNote,
  setReviewNote,
}: {
  template: AssessmentTemplate;
  compared: boolean;
  toggleCompare: () => void;
  toggleFavorite: () => void;
  select: () => void;
  submit: () => void;
  review: (decision: "APPROVED" | "REJECTED") => void;
  reviewNote: string;
  setReviewNote: (value: string) => void;
}) {
  return (
    <article className="flex flex-col rounded-xl border border-border bg-card p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="flex flex-wrap gap-2 text-xs font-semibold uppercase tracking-wide text-primary">
            <span>{template.scope}</span><span>v{template.templateVersion}</span><span>{template.status}</span>
          </div>
          <h2 className="mt-2 text-lg font-semibold">{template.title}</h2>
          <p className="mt-2 line-clamp-3 text-sm leading-6 text-muted-foreground">{template.summary}</p>
        </div>
        <button type="button" onClick={toggleFavorite} aria-label={template.favorite ? "Remover dos favoritos" : "Adicionar aos favoritos"} className="rounded p-2 hover:bg-accent">
          <Star className={`h-5 w-5 ${template.favorite ? "fill-current text-amber-500" : "text-muted-foreground"}`} />
        </button>
      </div>
      <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
        <Metric label="Cargo" value={template.jobRole} />
        <Metric label="Senioridade" value={template.seniority} />
        <Metric label="Cenários" value={String(template.preview.scenarioCount)} />
        <Metric label="Duração" value={`${template.durationMinutes} min`} />
        <Metric label="Idioma" value={template.languageCode} />
        <Metric label="Complexidade" value={template.complexity} />
      </dl>
      <div className="mt-4 flex flex-wrap gap-1.5">
        {template.preview.competencyCoverage.map((competency) => <span key={competency} className="rounded-full bg-primary/10 px-2.5 py-1 text-xs text-primary">{competency}</span>)}
      </div>
      <div className="mt-auto flex flex-wrap gap-2 pt-5">
        <Button onClick={select} disabled={template.status !== "APPROVED"}><CopyPlus className="mr-2 h-4 w-4" />Usar modelo</Button>
        <Button variant="outline" onClick={toggleCompare}><GitCompareArrows className="mr-2 h-4 w-4" />{compared ? "Remover" : "Comparar"}</Button>
        {(template.status === "DRAFT" || template.status === "REJECTED") && <Button variant="outline" onClick={submit}><Send className="mr-2 h-4 w-4" />Enviar para revisão</Button>}
      </div>
      {template.status === "IN_REVIEW" && (
        <div className="mt-4 space-y-3 border-t border-border pt-4">
          <textarea value={reviewNote} onChange={(event) => setReviewNote(event.target.value)} placeholder="Parecer da revisão" className="min-h-20 w-full rounded-md border border-input bg-background px-3 py-2 text-sm" />
          <div className="flex gap-2">
            <Button size="sm" onClick={() => review("APPROVED")}><CheckCircle2 className="mr-2 h-4 w-4" />Aprovar</Button>
            <Button size="sm" variant="outline" onClick={() => review("REJECTED")}>Rejeitar</Button>
          </div>
        </div>
      )}
    </article>
  );
}

function ComparisonTable({ templates }: { templates: AssessmentTemplate[] }) {
  const rows: Array<[string, (template: AssessmentTemplate) => string]> = [
    ["Cargo", (item) => item.jobRole],
    ["Área", (item) => item.businessArea],
    ["Senioridade", (item) => item.seniority],
    ["Setor", (item) => item.sector],
    ["Duração", (item) => `${item.durationMinutes} min`],
    ["Cenários", (item) => String(item.preview.scenarioCount)],
    ["Alternativas", (item) => String(item.preview.optionCount)],
    ["Competências", (item) => item.preview.competencyCoverage.join(", ")],
    ["Acessibilidade", (item) => item.preview.accessibilityRequirements.join(", ") || "Sem requisito adicional"],
    ["Limitações", (item) => item.usageLimitations],
  ];
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-center gap-2"><GitCompareArrows className="h-5 w-5 text-primary" /><h2 className="font-semibold">Comparação de modelos</h2></div>
      <div className="mt-4 overflow-x-auto"><table className="w-full min-w-[720px] text-left text-sm"><thead><tr><th className="pb-3">Critério</th>{templates.map((template) => <th key={template.id} className="pb-3">{template.title}</th>)}</tr></thead><tbody>{rows.map(([label, value]) => <tr key={label} className="border-t border-border"><th className="py-3 pr-4 font-semibold">{label}</th>{templates.map((template) => <td key={template.id} className="max-w-xs py-3 pr-4 align-top text-muted-foreground">{value(template)}</td>)}</tr>)}</tbody></table></div>
    </section>
  );
}

function CreateTemplateCard({
  source,
  simulationId,
  versionNumber,
  onCreated,
}: {
  source: Awaited<ReturnType<typeof getSimulationVersion>>;
  simulationId: string;
  versionNumber: number;
  onCreated: () => void;
}) {
  const initial: CreateTemplateInput = {
    sourceSimulationId: simulationId,
    sourceVersionNumber: versionNumber,
    scope: "INTERNAL",
    title: source.name,
    summary: source.description,
    jobRole: "",
    businessArea: "",
    seniority: "",
    sector: "",
    durationMinutes: 20,
    languageCode: "pt-BR",
    complexity: "INTERMEDIARIA",
    methodologyEvidence: "",
    usageLimitations: "",
    competencies: source.blueprint.competencies.map((item) => item.name),
  };
  const [form, setForm] = useState(initial);
  const mutation = useMutation({ mutationFn: createAssessmentTemplate, onSuccess: onCreated });
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-center gap-2"><LibraryBig className="h-5 w-5 text-primary" /><h2 className="font-semibold">Publicar versão como modelo</h2></div>
      <p className="mt-2 text-sm text-muted-foreground">Origem: {source.name}, versão {versionNumber}. O modelo inicia em rascunho e exige revisão antes do uso.</p>
      {mutation.error && <div className="mt-3 text-sm text-destructive">{mutation.error.message}</div>}
      <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <FormInput label="Título" value={form.title} onChange={(value) => setForm({ ...form, title: value })} />
        <FormInput label="Cargo" value={form.jobRole} onChange={(value) => setForm({ ...form, jobRole: value })} />
        <FormInput label="Área" value={form.businessArea} onChange={(value) => setForm({ ...form, businessArea: value })} />
        <FormInput label="Senioridade" value={form.seniority} onChange={(value) => setForm({ ...form, seniority: value })} />
        <FormInput label="Setor" value={form.sector} onChange={(value) => setForm({ ...form, sector: value })} />
        <FormInput label="Duração (min)" type="number" value={String(form.durationMinutes)} onChange={(value) => setForm({ ...form, durationMinutes: Number(value) })} />
        <FormInput label="Idioma" value={form.languageCode} onChange={(value) => setForm({ ...form, languageCode: value })} />
        <FormInput label="Complexidade" value={form.complexity} onChange={(value) => setForm({ ...form, complexity: value })} />
      </div>
      <div className="mt-3 grid gap-3 lg:grid-cols-3">
        <FormTextarea label="Resumo" value={form.summary} onChange={(value) => setForm({ ...form, summary: value })} />
        <FormTextarea label="Evidências metodológicas" value={form.methodologyEvidence} onChange={(value) => setForm({ ...form, methodologyEvidence: value })} />
        <FormTextarea label="Limitações de uso" value={form.usageLimitations} onChange={(value) => setForm({ ...form, usageLimitations: value })} />
      </div>
      <label className="mt-3 block text-sm font-medium">Escopo<select value={form.scope} onChange={(event) => setForm({ ...form, scope: event.target.value as TemplateScope })} className="mt-1 w-full max-w-xs rounded-md border border-input bg-background px-3 py-2"><option value="INTERNAL">Somente minha empresa</option><option value="SHARED">Compartilhado</option><option value="OFFICIAL">Oficial Práxis</option></select></label>
      <Button className="mt-4" onClick={() => mutation.mutate(form)} disabled={mutation.isPending || !form.jobRole || !form.methodologyEvidence || !form.usageLimitations}><ShieldCheck className="mr-2 h-4 w-4" />Cadastrar modelo</Button>
    </section>
  );
}

function InstantiatePanel({ template, name, setName, close, instantiate, pending }: { template: AssessmentTemplate; name: string; setName: (value: string) => void; close: () => void; instantiate: () => void; pending: boolean }) {
  return <section className="rounded-xl border-2 border-primary/40 bg-card p-5"><h2 className="font-semibold">Criar avaliação a partir de {template.title}</h2><p className="mt-2 text-sm text-muted-foreground">A cópia será independente e nascerá como versão 1 em rascunho.</p><label className="mt-4 block text-sm font-medium">Nome da nova avaliação<input value={name} onChange={(event) => setName(event.target.value)} className="mt-1 w-full rounded-md border border-input bg-background px-3 py-2" /></label><div className="mt-4 flex gap-2"><Button onClick={instantiate} disabled={pending || !name.trim()}>Criar rascunho</Button><Button variant="outline" onClick={close}>Cancelar</Button></div></section>;
}

function FilterInput({ label, value, onChange }: { label: string; value?: string; onChange: (value: string) => void }) { return <label className="text-sm"><span className="font-medium">{label}</span><input value={value ?? ""} onChange={(event) => onChange(event.target.value)} className="mt-1 w-full rounded-md border border-input bg-background px-3 py-2" /></label>; }
function FormInput({ label, value, onChange, type = "text" }: { label: string; value: string; onChange: (value: string) => void; type?: string }) { return <label className="text-sm"><span className="font-medium">{label}</span><input type={type} value={value} onChange={(event) => onChange(event.target.value)} className="mt-1 w-full rounded-md border border-input bg-background px-3 py-2" /></label>; }
function FormTextarea({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) { return <label className="text-sm"><span className="font-medium">{label}</span><textarea value={value} onChange={(event) => onChange(event.target.value)} className="mt-1 min-h-24 w-full rounded-md border border-input bg-background px-3 py-2" /></label>; }
function Metric({ label, value }: { label: string; value: string }) { return <div><dt className="text-xs uppercase tracking-wide text-muted-foreground">{label}</dt><dd className="mt-1 font-medium">{value}</dd></div>; }
function LoadingCard() { return <section className="rounded-xl border border-border bg-card py-12 text-center text-sm text-muted-foreground">Carregando modelos...</section>; }
function ErrorBanner({ error }: { error: unknown }) { return <StateBanner tone="danger" title="Não foi possível concluir a operação">{error instanceof Error ? error.message : "Tente novamente."}</StateBanner>; }
