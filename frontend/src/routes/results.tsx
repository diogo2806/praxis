import { createFileRoute, Link, Outlet, useNavigate, useRouterState } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { ExternalLink, RefreshCw, Search, SlidersHorizontal, X } from "lucide-react";
import { useMemo, useState } from "react";
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
import { cn } from "@/lib/utils";

const PAGE_SIZE = 20;

type ResultsCopy = {
  locale: string;
  title: string;
  description: string;
  reload: string;
  filters: string;
  hideFilters: string;
  clearFilters: string;
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
  candidate: string;
  process: string;
  situation: string;
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
  noFinalResult: string;
  statuses: Record<AttemptStatus, string>;
};

const resultsCopy: Record<Language, ResultsCopy> = {
  "pt-BR": {
    locale: "pt-BR",
    title: "Resultados",
    description: "Acompanhe cada participante e abra o registro somente quando precisar analisar as evidências.",
    reload: "Atualizar",
    filters: "Filtros",
    hideFilters: "Ocultar filtros",
    clearFilters: "Limpar filtros",
    searchPlaceholder: "Buscar participante",
    assessment: "Avaliação",
    status: "Situação",
    period: "Período",
    last7Days: "Últimos 7 dias",
    last30Days: "Últimos 30 dias",
    last90Days: "Últimos 90 dias",
    integration: "Origem",
    emptyTitle: "Nenhum resultado encontrado",
    emptyDescription: "Os registros aparecerão aqui quando uma participação for criada.",
    generateAssessmentLink: "Convidar participante",
    candidate: "Participante",
    process: "Processo",
    situation: "Situação",
    action: "Ação",
    viewDetails: "Analisar",
    followUp: "Acompanhar",
    resultSingular: "resultado",
    resultPlural: "resultados",
    previous: "Anterior",
    pageOf: "Página {current} de {total}",
    next: "Próxima",
    loading: "Carregando resultados...",
    errorTitle: "Não foi possível carregar os resultados",
    retry: "Tentar novamente",
    errorDescription: "Verifique a conexão e tente novamente.",
    noFinalResult: "Aguardando conclusão",
    statuses: {
      notStarted: "Não iniciado",
      inProgress: "Em andamento",
      completed: "Concluído",
      expired: "Expirado",
      abandoned: "Abandonado",
    },
  },
  en: {
    locale: "en-US",
    title: "Results",
    description: "Track each participant and open a record only when you need to review the evidence.",
    reload: "Refresh",
    filters: "Filters",
    hideFilters: "Hide filters",
    clearFilters: "Clear filters",
    searchPlaceholder: "Search participant",
    assessment: "Assessment",
    status: "Status",
    period: "Period",
    last7Days: "Last 7 days",
    last30Days: "Last 30 days",
    last90Days: "Last 90 days",
    integration: "Source",
    emptyTitle: "No results found",
    emptyDescription: "Records will appear here when a participation is created.",
    generateAssessmentLink: "Invite participant",
    candidate: "Participant",
    process: "Process",
    situation: "Status",
    action: "Action",
    viewDetails: "Review",
    followUp: "Follow up",
    resultSingular: "result",
    resultPlural: "results",
    previous: "Previous",
    pageOf: "Page {current} of {total}",
    next: "Next",
    loading: "Loading results...",
    errorTitle: "Could not load results",
    retry: "Try again",
    errorDescription: "Check the connection and try again.",
    noFinalResult: "Waiting for completion",
    statuses: {
      notStarted: "Not started",
      inProgress: "In progress",
      completed: "Completed",
      expired: "Expired",
      abandoned: "Abandoned",
    },
  },
  "es-MX": {
    locale: "es-MX",
    title: "Resultados",
    description: "Acompaña a cada participante y abre el registro solo cuando necesites revisar las evidencias.",
    reload: "Actualizar",
    filters: "Filtros",
    hideFilters: "Ocultar filtros",
    clearFilters: "Limpiar filtros",
    searchPlaceholder: "Buscar participante",
    assessment: "Evaluación",
    status: "Estado",
    period: "Período",
    last7Days: "Últimos 7 días",
    last30Days: "Últimos 30 días",
    last90Days: "Últimos 90 días",
    integration: "Origen",
    emptyTitle: "No se encontraron resultados",
    emptyDescription: "Los registros aparecerán aquí cuando se cree una participación.",
    generateAssessmentLink: "Invitar participante",
    candidate: "Participante",
    process: "Proceso",
    situation: "Estado",
    action: "Acción",
    viewDetails: "Analizar",
    followUp: "Acompañar",
    resultSingular: "resultado",
    resultPlural: "resultados",
    previous: "Anterior",
    pageOf: "Página {current} de {total}",
    next: "Siguiente",
    loading: "Cargando resultados...",
    errorTitle: "No se pudieron cargar los resultados",
    retry: "Intentar de nuevo",
    errorDescription: "Verifica la conexión e inténtalo de nuevo.",
    noFinalResult: "Esperando finalización",
    statuses: {
      notStarted: "No iniciado",
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
      { name: "description", content: "Acompanhe participantes e analise os resultados das avaliações." },
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
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(
    Boolean(filters.simulationId || filters.status || filters.period || filters.integrationProvider),
  );
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
  const advancedFilterCount = [
    filters.simulationId,
    filters.status,
    filters.period,
    filters.integrationProvider,
  ].filter(Boolean).length;

  function clearAdvancedFilters() {
    updateFilters({ simulationId: "", status: "", period: "", integrationProvider: "" });
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-7xl space-y-5">
        <header className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-semibold">{copy.title}</h1>
            <p className="mt-1 max-w-2xl text-sm text-muted-foreground">{copy.description}</p>
          </div>
          <button
            type="button"
            onClick={() => resultsQuery.refetch()}
            className="inline-flex min-h-10 items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
          >
            <RefreshCw className="h-4 w-4" />
            {copy.reload}
          </button>
        </header>

        <section className="rounded-md border border-border bg-card p-4">
          <div className="flex flex-wrap items-center gap-2">
            <label className="relative min-w-[240px] flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                value={filters.search}
                onChange={(event) => updateFilters({ search: event.target.value })}
                placeholder={copy.searchPlaceholder}
                className="h-10 w-full rounded-md border border-border bg-background pl-9 pr-3 text-sm"
              />
            </label>
            <button
              type="button"
              aria-expanded={showAdvancedFilters}
              onClick={() => setShowAdvancedFilters((current) => !current)}
              className={cn(
                "inline-flex min-h-10 items-center gap-2 rounded-md border px-3 py-2 text-sm font-medium",
                showAdvancedFilters || advancedFilterCount > 0
                  ? "border-primary/40 bg-primary/10 text-primary"
                  : "border-border bg-background hover:bg-accent",
              )}
            >
              <SlidersHorizontal className="h-4 w-4" />
              {showAdvancedFilters ? copy.hideFilters : copy.filters}
              {advancedFilterCount > 0 && (
                <span className="rounded-full bg-primary px-1.5 py-0.5 text-[10px] text-primary-foreground">
                  {advancedFilterCount}
                </span>
              )}
            </button>
          </div>

          {showAdvancedFilters && (
            <div className="mt-4 border-t border-border pt-4">
              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                <select
                  value={filters.simulationId}
                  onChange={(event) => updateFilters({ simulationId: event.target.value })}
                  className="h-10 rounded-md border border-border bg-background px-3 text-sm"
                  aria-label={copy.assessment}
                >
                  <option value="">{copy.assessment}</option>
                  {(simulationsQuery.data ?? []).map((simulation) => (
                    <option key={simulation.id} value={simulation.id}>
                      {simulation.name}
                    </option>
                  ))}
                </select>
                <select
                  value={filters.status}
                  onChange={(event) => updateFilters({ status: event.target.value as AttemptStatus | "" })}
                  className="h-10 rounded-md border border-border bg-background px-3 text-sm"
                  aria-label={copy.status}
                >
                  <option value="">{copy.status}</option>
                  {statusOptions(copy).map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
                <select
                  value={filters.period}
                  onChange={(event) => updateFilters({ period: event.target.value })}
                  className="h-10 rounded-md border border-border bg-background px-3 text-sm"
                  aria-label={copy.period}
                >
                  <option value="">{copy.period}</option>
                  <option value="7">{copy.last7Days}</option>
                  <option value="30">{copy.last30Days}</option>
                  <option value="90">{copy.last90Days}</option>
                </select>
                <select
                  value={filters.integrationProvider}
                  onChange={(event) => updateFilters({ integrationProvider: event.target.value })}
                  className="h-10 rounded-md border border-border bg-background px-3 text-sm"
                  aria-label={copy.integration}
                >
                  <option value="">{copy.integration}</option>
                  <option value="GUPY">Gupy</option>
                  <option value="RECRUTEI">Recrutei</option>
                  <option value="API">API</option>
                  <option value="MANUAL">Manual</option>
                </select>
              </div>
              {advancedFilterCount > 0 && (
                <button
                  type="button"
                  onClick={clearAdvancedFilters}
                  className="mt-3 inline-flex min-h-9 items-center gap-2 rounded-md border border-border bg-background px-3 py-1.5 text-xs font-medium hover:bg-accent"
                >
                  <X className="h-3.5 w-3.5" />
                  {copy.clearFilters}
                </button>
              )}
            </div>
          )}
        </section>

        {resultsQuery.isLoading ? (
          <ResultsLoadingState copy={copy} />
        ) : resultsQuery.isError ? (
          <ResultsErrorState copy={copy} onReload={() => resultsQuery.refetch()} />
        ) : resultsQuery.data ? (
          <ResultsContent
            copy={copy}
            data={resultsQuery.data}
            page={filters.page}
            onPageChange={(page) => updateFilters({ page }, false)}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

function ResultsContent({
  copy,
  data,
  page,
  onPageChange,
}: {
  copy: ResultsCopy;
  data: ResultsPageResponse;
  page: number;
  onPageChange: (page: number) => void;
}) {
  if (data.items.length === 0) {
    return (
      <EmptyState
        title={copy.emptyTitle}
        description={copy.emptyDescription}
        actions={
          <Link
            to="/enviar-link"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            {copy.generateAssessmentLink}
            <ExternalLink className="h-4 w-4" />
          </Link>
        }
      />
    );
  }

  return (
    <div className="space-y-4">
      <ResultsTable copy={copy} items={data.items} />
      <ResultsPagination copy={copy} data={data} page={page} onPageChange={onPageChange} />
    </div>
  );
}

function ResultsTable({ copy, items }: { copy: ResultsCopy; items: ResultListItemResponse[] }) {
  return (
    <section className="overflow-hidden rounded-md border border-border bg-card">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[760px] text-sm">
          <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
            <tr>
              <th className="px-4 py-3 text-left font-medium">{copy.candidate}</th>
              <th className="px-4 py-3 text-left font-medium">{copy.process}</th>
              <th className="px-4 py-3 text-left font-medium">{copy.situation}</th>
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
                  <div className="mt-1 text-xs font-medium tabular-nums text-muted-foreground">
                    {formatResultScore(item.status, item.overallScore, copy)}
                  </div>
                  {isFinalResultStatus(item.status) && item.highlightCompetency && (
                    <div className="mt-1 text-[11px] text-muted-foreground">{item.highlightCompetency}</div>
                  )}
                </td>
                <td className="px-4 py-3 text-muted-foreground">{providerLabel(item.integrationProvider)}</td>
                <td className="px-4 py-3 text-right">
                  <Link
                    to="/results/$attemptId"
                    params={{ attemptId: item.attemptId }}
                    className="text-sm font-medium text-primary hover:underline"
                  >
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

function ResultsPagination({
  copy,
  data,
  page,
  onPageChange,
}: {
  copy: ResultsCopy;
  data: ResultsPageResponse;
  page: number;
  onPageChange: (page: number) => void;
}) {
  const resultsLabel = data.totalItems === 1 ? copy.resultSingular : copy.resultPlural;
  const currentPage = data.totalPages === 0 ? 0 : page + 1;

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
      <div className="text-muted-foreground">
        {data.totalItems.toLocaleString(copy.locale)} {resultsLabel}
      </div>
      <div className="flex items-center gap-2">
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
          className="rounded-md border border-border bg-card px-3 py-2 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {copy.previous}
        </button>
        <span className="text-muted-foreground">
          {copy.pageOf.replace("{current}", String(currentPage)).replace("{total}", String(data.totalPages))}
        </span>
        <button
          type="button"
          disabled={page + 1 >= data.totalPages}
          onClick={() => onPageChange(page + 1)}
          className="rounded-md border border-border bg-card px-3 py-2 disabled:cursor-not-allowed disabled:opacity-50"
        >
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
      <div className="mt-4 space-y-3">
        {Array.from({ length: 5 }).map((_, index) => (
          <div key={index} className="h-14 animate-pulse rounded-md bg-muted" />
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
        <button
          type="button"
          onClick={onReload}
          className="rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium"
        >
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
