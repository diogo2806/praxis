import { createFileRoute, Link, Outlet, useNavigate, useRouterState } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { BarChart3, CheckCircle2, Clock3, ExternalLink, RefreshCw, Search, TimerOff } from "lucide-react";
import { useMemo } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner } from "@/components/praxis-ui";
import {
  listResults,
  listSimulations,
  type AttemptStatus,
  type ResultListItemResponse,
  type ResultsPageResponse,
} from "@/lib/api/praxis";
import { useLanguage } from "@/lib/language-context";
import type { Language } from "@/lib/translations";

const PAGE_SIZE = 20;

type ResultsCopy = {
  locale: string;
  eyebrow: string;
  title: string;
  description: string;
  reload: string;
  filters: string;
  searchPlaceholder: string;
  assessment: string;
  status: string;
  period: string;
  last7Days: string;
  last30Days: string;
  last90Days: string;
  integration: string;
  emptyTitle: string;
  emptyDescription: string;
  generateAssessmentLink: string;
  completed: string;
  inProgress: string;
  expired: string;
  average: string;
  noFinalResult: string;
  tableTitle: string;
  candidate: string;
  highlightedCompetency: string;
  result: string;
  action: string;
  viewDetails: string;
  followUp: string;
  resultSingular: string;
  resultPlural: string;
  previous: string;
  pageOf: string;
  next: string;
  loading: string;
  errorTitle: string;
  retry: string;
  errorDescription: string;
  statuses: Record<AttemptStatus, string>;
};

const resultsCopy: Record<Language, ResultsCopy> = {
  "pt-BR": {
    locale: "pt-BR",
    eyebrow: "Práxis / Resultados",
    title: "Resultados",
    description: "Acompanhe candidatos, status da avaliação e resultados finais quando eles estiverem disponíveis.",
    reload: "Recarregar",
    filters: "Filtros",
    searchPlaceholder: "Buscar candidato...",
    assessment: "Avaliação",
    status: "Status",
    period: "Período",
    last7Days: "Últimos 7 dias",
    last30Days: "Últimos 30 dias",
    last90Days: "Últimos 90 dias",
    integration: "Integração",
    emptyTitle: "Nenhum resultado encontrado.",
    emptyDescription: "Assim que candidatos concluírem avaliações, os resultados aparecerão aqui.",
    generateAssessmentLink: "Gerar link de avaliação",
    completed: "Concluídos",
    inProgress: "Em andamento",
    expired: "Expirados",
    average: "Média geral",
    noFinalResult: "Sem resultado final",
    tableTitle: "Tabela de resultados",
    candidate: "Candidato",
    highlightedCompetency: "Competência destaque",
    result: "Resultado",
    action: "Ação",
    viewDetails: "Ver detalhes",
    followUp: "Acompanhar",
    resultSingular: "resultado",
    resultPlural: "resultados",
    previous: "Anterior",
    pageOf: "Página {current} de {total}",
    next: "Próxima",
    loading: "Carregando resultados...",
    errorTitle: "Não foi possível carregar os resultados.",
    retry: "Recarregar",
    errorDescription: "Tente novamente.",
    statuses: {
      notStarted: "Criado",
      inProgress: "Em andamento",
      completed: "Concluído",
      expired: "Expirado",
      abandoned: "Abandonado",
    },
  },
  en: {
    locale: "en-US",
    eyebrow: "Práxis / Results",
    title: "Results",
    description: "Track candidates, assessment status, and final results when they are available.",
    reload: "Reload",
    filters: "Filters",
    searchPlaceholder: "Search candidate...",
    assessment: "Assessment",
    status: "Status",
    period: "Period",
    last7Days: "Last 7 days",
    last30Days: "Last 30 days",
    last90Days: "Last 90 days",
    integration: "Integration",
    emptyTitle: "No results found.",
    emptyDescription: "Results will appear here as candidates complete assessments.",
    generateAssessmentLink: "Generate assessment link",
    completed: "Completed",
    inProgress: "In progress",
    expired: "Expired",
    average: "Overall average",
    noFinalResult: "No final result",
    tableTitle: "Results table",
    candidate: "Candidate",
    highlightedCompetency: "Highlighted competency",
    result: "Result",
    action: "Action",
    viewDetails: "View details",
    followUp: "Follow up",
    resultSingular: "result",
    resultPlural: "results",
    previous: "Previous",
    pageOf: "Page {current} of {total}",
    next: "Next",
    loading: "Loading results...",
    errorTitle: "Could not load the results.",
    retry: "Reload",
    errorDescription: "Try again.",
    statuses: {
      notStarted: "Created",
      inProgress: "In progress",
      completed: "Completed",
      expired: "Expired",
      abandoned: "Abandoned",
    },
  },
  "es-MX": {
    locale: "es-MX",
    eyebrow: "Práxis / Resultados",
    title: "Resultados",
    description: "Acompaña a las personas candidatas, el estado de la evaluación y los resultados finales cuando estén disponibles.",
    reload: "Recargar",
    filters: "Filtros",
    searchPlaceholder: "Buscar candidato...",
    assessment: "Evaluación",
    status: "Estado",
    period: "Período",
    last7Days: "Últimos 7 días",
    last30Days: "Últimos 30 días",
    last90Days: "Últimos 90 días",
    integration: "Integración",
    emptyTitle: "No se encontraron resultados.",
    emptyDescription: "Los resultados aparecerán aquí cuando las personas candidatas completen las evaluaciones.",
    generateAssessmentLink: "Generar enlace de evaluación",
    completed: "Completados",
    inProgress: "En curso",
    expired: "Vencidos",
    average: "Promedio general",
    noFinalResult: "Sin resultado final",
    tableTitle: "Tabla de resultados",
    candidate: "Candidato",
    highlightedCompetency: "Competencia destacada",
    result: "Resultado",
    action: "Acción",
    viewDetails: "Ver detalles",
    followUp: "Acompañar",
    resultSingular: "resultado",
    resultPlural: "resultados",
    previous: "Anterior",
    pageOf: "Página {current} de {total}",
    next: "Siguiente",
    loading: "Cargando resultados...",
    errorTitle: "No se pudieron cargar los resultados.",
    retry: "Recargar",
    errorDescription: "Inténtalo de nuevo.",
    statuses: {
      notStarted: "Creado",
      inProgress: "En curso",
      completed: "Completado",
      expired: "Vencido",
      abandoned: "Abandonado",
    },
  },
};

export const Route = createFileRoute("/results")({
  validateSearch: (search: Record<string, unknown>) => ({
    search: typeof search.search === "string" ? search.search : "",
    simulationId: typeof search.simulationId === "string" ? search.simulationId : "",
    status: isAttemptStatus(search.status) ? search.status : "",
    period: isPeriod(search.period) ? search.period : "",
    integrationProvider: typeof search.integrationProvider === "string" ? search.integrationProvider : "",
    page: typeof search.page === "number" && Number.isInteger(search.page) && search.page >= 0 ? search.page : 0,
  }),
  head: () => ({
    meta: [
      { title: "Resultados - Práxis" },
      { name: "description", content: "Lista, filtros e análise de resultados de candidatos avaliados no Práxis." },
    ],
  }),
  component: ResultsPage,
});

function ResultsPage() {
  const isResultsIndex = useRouterState({ select: (state) => state.location.pathname === "/results" });
  return isResultsIndex ? <ResultsIndexPage /> : <Outlet />;
}

function ResultsIndexPage() {
  const { language } = useLanguage();
  const copy = resultsCopy[language];
  const filters = Route.useSearch();
  const navigate = useNavigate({ from: "/results" });
  const periodRange = useMemo(() => rangeForPeriod(filters.period), [filters.period]);
  const updateFilters = (next: Partial<typeof filters>, resetPage = true) => {
    void navigate({
      to: "/results",
      replace: true,
      search: (current) => ({ ...current, ...next, page: resetPage ? 0 : next.page ?? current.page }),
    });
  };
  const resultsQuery = useQuery({
    queryKey: ["results", filters],
    queryFn: () =>
      listResults({
        search: filters.search.trim() || undefined,
        simulationId: filters.simulationId || undefined,
        status: filters.status || undefined,
        integrationProvider: filters.integrationProvider || undefined,
        periodStart: periodRange.start,
        periodEnd: periodRange.end,
        page: filters.page,
        size: PAGE_SIZE,
      }),
    retry: false,
  });
  const simulationsQuery = useQuery({ queryKey: ["simulations"], queryFn: listSimulations, retry: false });

  return (
    <AppShell>
      <div className="space-y-6">
        <ResultsHeader copy={copy} onReload={() => resultsQuery.refetch()} />
        <ResultsFilters
          copy={copy}
          search={filters.search}
          simulationId={filters.simulationId}
          status={filters.status}
          period={filters.period}
          integrationProvider={filters.integrationProvider}
          simulations={simulationsQuery.data ?? []}
          onSearch={(value) => updateFilters({ search: value })}
          onSimulationId={(value) => updateFilters({ simulationId: value })}
          onStatus={(value) => updateFilters({ status: value })}
          onPeriod={(value) => updateFilters({ period: value })}
          onIntegrationProvider={(value) => updateFilters({ integrationProvider: value })}
        />
        {resultsQuery.isLoading ? (
          <ResultsLoadingState copy={copy} />
        ) : resultsQuery.isError ? (
          <ResultsErrorState copy={copy} onReload={() => resultsQuery.refetch()} />
        ) : resultsQuery.data ? (
          <ResultsContent copy={copy} data={resultsQuery.data} page={filters.page} onPageChange={(page) => updateFilters({ page }, false)} />
        ) : null}
      </div>
    </AppShell>
  );
}

function ResultsHeader({ copy, onReload }: { copy: ResultsCopy; onReload: () => void }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div>
        <div className="text-xs uppercase text-primary">{copy.eyebrow}</div>
        <h1 className="mt-1 text-3xl font-semibold">{copy.title}</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">{copy.description}</p>
      </div>
      <button type="button" onClick={onReload} className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent">
        <RefreshCw className="h-4 w-4" />
        {copy.reload}
      </button>
    </div>
  );
}

function ResultsFilters({
  copy,
  search,
  simulationId,
  status,
  period,
  integrationProvider,
  simulations,
  onSearch,
  onSimulationId,
  onStatus,
  onPeriod,
  onIntegrationProvider,
}: {
  copy: ResultsCopy;
  search: string;
  simulationId: string;
  status: AttemptStatus | "";
  period: string;
  integrationProvider: string;
  simulations: Array<{ id: string; name: string }>;
  onSearch: (value: string) => void;
  onSimulationId: (value: string) => void;
  onStatus: (value: AttemptStatus | "") => void;
  onPeriod: (value: string) => void;
  onIntegrationProvider: (value: string) => void;
}) {
  return (
    <section className="rounded-md border border-border bg-card p-4">
      <div className="mb-3 text-sm font-semibold">{copy.filters}</div>
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(240px,1.4fr)_repeat(4,minmax(160px,1fr))]">
        <label className="relative block">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input value={search} onChange={(event) => onSearch(event.target.value)} placeholder={copy.searchPlaceholder} className="h-10 w-full rounded-md border border-border bg-background pl-9 pr-3 text-sm" />
        </label>
        <select value={simulationId} onChange={(event) => onSimulationId(event.target.value)} className="h-10 rounded-md border border-border bg-background px-3 text-sm">
          <option value="">{copy.assessment}</option>
          {simulations.map((simulation) => (
            <option key={simulation.id} value={simulation.id}>
              {simulation.name}
            </option>
          ))}
        </select>
        <select value={status} onChange={(event) => onStatus(event.target.value as AttemptStatus | "")} className="h-10 rounded-md border border-border bg-background px-3 text-sm">
          <option value="">{copy.status}</option>
          {statusOptions(copy).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <select value={period} onChange={(event) => onPeriod(event.target.value)} className="h-10 rounded-md border border-border bg-background px-3 text-sm">
          <option value="">{copy.period}</option>
          <option value="7">{copy.last7Days}</option>
          <option value="30">{copy.last30Days}</option>
          <option value="90">{copy.last90Days}</option>
        </select>
        <select value={integrationProvider} onChange={(event) => onIntegrationProvider(event.target.value)} className="h-10 rounded-md border border-border bg-background px-3 text-sm">
          <option value="">{copy.integration}</option>
          <option value="GUPY">Gupy</option>
          <option value="RECRUTEI">Recrutei</option>
          <option value="API">API</option>
          <option value="MANUAL">Manual</option>
        </select>
      </div>
    </section>
  );
}

function ResultsContent({ copy, data, page, onPageChange }: { copy: ResultsCopy; data: ResultsPageResponse; page: number; onPageChange: (page: number) => void }) {
  if (data.items.length === 0) {
    return (
      <>
        <ResultsSummaryCards copy={copy} data={data} />
        <EmptyState
          title={copy.emptyTitle}
          description={copy.emptyDescription}
          actions={
            <Link to="/candidate-links/new" className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
              {copy.generateAssessmentLink}
              <ExternalLink className="h-4 w-4" />
            </Link>
          }
        />
      </>
    );
  }

  return (
    <>
      <ResultsSummaryCards copy={copy} data={data} />
      <ResultsTable copy={copy} items={data.items} />
      <ResultsPagination copy={copy} data={data} page={page} onPageChange={onPageChange} />
    </>
  );
}

function ResultsSummaryCards({ copy, data }: { copy: ResultsCopy; data: ResultsPageResponse }) {
  const cards = [
    { title: copy.completed, value: data.summary.completed, icon: CheckCircle2 },
    { title: copy.inProgress, value: data.summary.inProgress, icon: Clock3 },
    { title: copy.expired, value: data.summary.expired, icon: TimerOff },
    { title: copy.average, value: data.summary.averageScore == null ? copy.noFinalResult : `${data.summary.averageScore}%`, icon: BarChart3 },
  ];

  return (
    <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      {cards.map((card) => (
        <div key={card.title} className="rounded-md border border-border bg-card p-4">
          <div className="flex items-center justify-between gap-3">
            <div className="text-xs uppercase text-muted-foreground">{card.title}</div>
            <card.icon className="h-4 w-4 text-muted-foreground" />
          </div>
          <div className="mt-2 text-3xl font-semibold tabular-nums">{card.value}</div>
        </div>
      ))}
    </section>
  );
}

function ResultsTable({ copy, items }: { copy: ResultsCopy; items: ResultListItemResponse[] }) {
  return (
    <section className="rounded-md border border-border bg-card">
      <div className="border-b border-border p-4">
        <h2 className="text-lg font-semibold">{copy.tableTitle}</h2>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[980px] text-sm">
          <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
            <tr>
              <th className="px-4 py-3 text-left font-medium">{copy.candidate}</th>
              <th className="px-4 py-3 text-left font-medium">{copy.assessment}</th>
              <th className="px-4 py-3 text-left font-medium">{copy.status}</th>
              <th className="px-4 py-3 text-left font-medium">{copy.highlightedCompetency}</th>
              <th className="px-4 py-3 text-left font-medium">{copy.result}</th>
              <th className="px-4 py-3 text-left font-medium">{copy.integration}</th>
              <th className="px-4 py-3 text-right font-medium">{copy.action}</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.attemptId} className="border-b border-border last:border-0 hover:bg-accent/35">
                <td className="px-4 py-3">
                  <div className="font-medium">{item.candidateName}</div>
                  <div className="mt-0.5 text-xs text-muted-foreground">{item.candidateEmail}</div>
                </td>
                <td className="px-4 py-3 text-muted-foreground">{item.simulationTitle}</td>
                <td className="px-4 py-3">
                  <ResultStatusBadge copy={copy} status={item.status} />
                </td>
                <td className="px-4 py-3">{isFinalResultStatus(item.status) ? item.highlightCompetency ?? "-" : copy.noFinalResult}</td>
                <td className="px-4 py-3 font-medium tabular-nums">{formatResultScore(item.status, item.overallScore, copy)}</td>
                <td className="px-4 py-3 text-muted-foreground">{providerLabel(item.integrationProvider)}</td>
                <td className="px-4 py-3 text-right">
                  <Link to="/results/$attemptId" params={{ attemptId: item.attemptId }} className="text-sm font-medium text-primary hover:underline">
                    {item.status === "completed" ? copy.viewDetails : copy.followUp}
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function ResultsPagination({ copy, data, page, onPageChange }: { copy: ResultsCopy; data: ResultsPageResponse; page: number; onPageChange: (page: number) => void }) {
  const resultsLabel = data.totalItems === 1 ? copy.resultSingular : copy.resultPlural;
  const currentPage = data.totalPages === 0 ? 0 : page + 1;

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
      <div className="text-muted-foreground">{data.totalItems.toLocaleString(copy.locale)} {resultsLabel}</div>
      <div className="flex items-center gap-2">
        <button type="button" disabled={page <= 0} onClick={() => onPageChange(page - 1)} className="rounded-md border border-border bg-card px-3 py-2 disabled:cursor-not-allowed disabled:opacity-50">
          {copy.previous}
        </button>
        <span className="text-muted-foreground">{copy.pageOf.replace("{current}", String(currentPage)).replace("{total}", String(data.totalPages))}</span>
        <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => onPageChange(page + 1)} className="rounded-md border border-border bg-card px-3 py-2 disabled:cursor-not-allowed disabled:opacity-50">
          {copy.next}
        </button>
      </div>
    </div>
  );
}

function ResultsLoadingState({ copy }: { copy: ResultsCopy }) {
  return (
    <section className="rounded-md border border-border bg-card p-6">
      <div className="text-sm font-medium">{copy.loading}</div>
      <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="h-24 animate-pulse rounded-md bg-muted" />
        ))}
      </div>
    </section>
  );
}

function ResultsErrorState({ copy, onReload }: { copy: ResultsCopy; onReload: () => void }) {
  return (
    <StateBanner
      tone="danger"
      title={copy.errorTitle}
      action={
        <button type="button" onClick={onReload} className="rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium">
          {copy.retry}
        </button>
      }
    >
      {copy.errorDescription}
    </StateBanner>
  );
}

function ResultStatusBadge({ copy, status }: { copy: ResultsCopy; status: AttemptStatus }) {
  const meta = resultStatusMeta(status, copy);
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-[11px] font-medium ${meta.className}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {meta.label}
    </span>
  );
}

function statusOptions(copy: ResultsCopy): Array<{ value: AttemptStatus; label: string }> {
  return [
    { value: "notStarted", label: copy.statuses.notStarted },
    { value: "inProgress", label: copy.statuses.inProgress },
    { value: "completed", label: copy.statuses.completed },
    { value: "expired", label: copy.statuses.expired },
    { value: "abandoned", label: copy.statuses.abandoned },
  ];
}

function resultStatusMeta(status: AttemptStatus, copy: ResultsCopy) {
  return (
    {
      notStarted: { label: copy.statuses.notStarted, className: "border-border bg-muted text-foreground" },
      inProgress: { label: copy.statuses.inProgress, className: "border-primary/25 bg-primary/10 text-foreground" },
      completed: { label: copy.statuses.completed, className: "border-success/25 bg-success/10 text-foreground" },
      abandoned: { label: copy.statuses.abandoned, className: "border-danger/25 bg-danger/10 text-foreground" },
      expired: { label: copy.statuses.expired, className: "border-warning/35 bg-warning/15 text-warning-foreground" },
    } satisfies Record<AttemptStatus, { label: string; className: string }>
  )[status];
}

function isFinalResultStatus(status: AttemptStatus) {
  return status === "completed";
}

function formatResultScore(status: AttemptStatus, score: number | null, copy: ResultsCopy) {
  return !isFinalResultStatus(status) || score == null ? copy.noFinalResult : `${score}%`;
}

function rangeForPeriod(period: string) {
  if (!period) return { start: undefined, end: undefined };
  const days = Number(period);
  if (!Number.isFinite(days)) return { start: undefined, end: undefined };
  const end = new Date();
  const start = new Date(end);
  start.setDate(start.getDate() - days);
  return { start: start.toISOString(), end: end.toISOString() };
}

function providerLabel(value: string | null) {
  return { GUPY: "Gupy", RECRUTEI: "Recrutei", API: "API", MANUAL: "Manual" }[value ?? ""] ?? "-";
}

function isAttemptStatus(value: unknown): value is AttemptStatus {
  return typeof value === "string" && ["notStarted", "inProgress", "completed", "expired", "abandoned"].includes(value);
}

function isPeriod(value: unknown): value is "7" | "30" | "90" {
  return value === "7" || value === "30" || value === "90";
}
