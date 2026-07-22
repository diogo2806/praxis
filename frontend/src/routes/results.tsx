import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link, Outlet, useNavigate, useRouterState } from "@tanstack/react-router";
import {
  BarChart3,
  ExternalLink,
  RefreshCw,
  Search,
  SlidersHorizontal,
  Users,
  X,
} from "lucide-react";
import { useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  listResults,
  listSimulations,
  type ResultListItemResponse,
  type ResultsPageResponse,
} from "@/lib/api/praxis";
import { useLanguage } from "@/lib/language-context";
import type { Language } from "@/lib/translations";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 20;

const copyByLanguage = {
  "pt-BR": {
    locale: "pt-BR",
    title: "Resultados",
    description: "Analise evidências somente depois que a participação estiver concluída.",
    reload: "Atualizar",
    compare: "Comparar participantes",
    participations: "Acompanhar participações",
    filters: "Filtros",
    hideFilters: "Ocultar filtros",
    clearFilters: "Limpar filtros",
    searchPlaceholder: "Buscar participante",
    assessment: "Todas as avaliações",
    period: "Todos os períodos",
    last7Days: "Últimos 7 dias",
    last30Days: "Últimos 30 dias",
    last90Days: "Últimos 90 dias",
    integration: "Todas as origens",
    emptyTitle: "Nenhum resultado concluído encontrado",
    emptyDescription:
      "Acompanhe o andamento em Participações. Os resultados aparecerão aqui após a conclusão.",
    candidate: "Participante",
    process: "Avaliação",
    score: "Resultado",
    source: "Origem",
    action: "Ação",
    analyze: "Analisar",
    previous: "Anterior",
    next: "Próxima",
    resultSingular: "resultado",
    resultPlural: "resultados",
    loading: "Carregando resultados concluídos...",
    errorTitle: "Não foi possível carregar os resultados",
    errorDescription: "Verifique a conexão e tente novamente.",
  },
  en: {
    locale: "en-US",
    title: "Results",
    description: "Review evidence only after a participation has been completed.",
    reload: "Refresh",
    compare: "Compare participants",
    participations: "Track participations",
    filters: "Filters",
    hideFilters: "Hide filters",
    clearFilters: "Clear filters",
    searchPlaceholder: "Search participant",
    assessment: "All assessments",
    period: "All periods",
    last7Days: "Last 7 days",
    last30Days: "Last 30 days",
    last90Days: "Last 90 days",
    integration: "All sources",
    emptyTitle: "No completed results found",
    emptyDescription:
      "Track progress in Participations. Results will appear here after completion.",
    candidate: "Participant",
    process: "Assessment",
    score: "Result",
    source: "Source",
    action: "Action",
    analyze: "Review",
    previous: "Previous",
    next: "Next",
    resultSingular: "result",
    resultPlural: "results",
    loading: "Loading completed results...",
    errorTitle: "Could not load results",
    errorDescription: "Check the connection and try again.",
  },
  "es-MX": {
    locale: "es-MX",
    title: "Resultados",
    description: "Analice evidencias solo después de que la participación haya concluido.",
    reload: "Actualizar",
    compare: "Comparar participantes",
    participations: "Acompañar participaciones",
    filters: "Filtros",
    hideFilters: "Ocultar filtros",
    clearFilters: "Limpiar filtros",
    searchPlaceholder: "Buscar participante",
    assessment: "Todas las evaluaciones",
    period: "Todos los períodos",
    last7Days: "Últimos 7 días",
    last30Days: "Últimos 30 días",
    last90Days: "Últimos 90 días",
    integration: "Todos los orígenes",
    emptyTitle: "No se encontraron resultados concluidos",
    emptyDescription:
      "Acompañe el avance en Participaciones. Los resultados aparecerán después de la conclusión.",
    candidate: "Participante",
    process: "Evaluación",
    score: "Resultado",
    source: "Origen",
    action: "Acción",
    analyze: "Analizar",
    previous: "Anterior",
    next: "Siguiente",
    resultSingular: "resultado",
    resultPlural: "resultados",
    loading: "Cargando resultados concluidos...",
    errorTitle: "No se pudieron cargar los resultados",
    errorDescription: "Verifique la conexión e inténtelo de nuevo.",
  },
} as const satisfies Record<Language, Record<string, string>>;

export const Route = createFileRoute("/results")({
  validateSearch: (search: Record<string, unknown>) => ({
    search: typeof search.search === "string" ? search.search : "",
    simulationId: typeof search.simulationId === "string" ? search.simulationId : "",
    period: isPeriod(search.period) ? search.period : "",
    integrationProvider:
      typeof search.integrationProvider === "string" ? search.integrationProvider : "",
    page:
      typeof search.page === "number" && Number.isInteger(search.page) && search.page >= 0
        ? search.page
        : 0,
  }),
  head: () => ({
    meta: [
      { title: "Resultados - Práxis" },
      { name: "description", content: "Analise resultados concluídos, evidências e comparações." },
    ],
  }),
  component: ResultsPage,
});

function ResultsPage() {
  const isResultsIndex = useRouterState({
    select: (state) => state.location.pathname === "/results",
  });
  return isResultsIndex ? <ResultsIndexPage /> : <Outlet />;
}

function ResultsIndexPage() {
  const { language } = useLanguage();
  const copy = copyByLanguage[language];
  const filters = Route.useSearch();
  const navigate = useNavigate({ from: "/results" });
  const [showFilters, setShowFilters] = useState(
    Boolean(filters.simulationId || filters.period || filters.integrationProvider),
  );
  const periodRange = useMemo(() => rangeForPeriod(filters.period), [filters.period]);

  function updateFilters(next: Partial<typeof filters>, resetPage = true) {
    void navigate({
      to: "/results",
      replace: true,
      search: (current) => ({
        ...current,
        ...next,
        page: resetPage ? 0 : (next.page ?? current.page),
      }),
    });
  }

  const resultsQuery = useQuery({
    queryKey: ["results", "completed", filters],
    queryFn: () =>
      listResults({
        search: filters.search.trim() || undefined,
        simulationId: filters.simulationId || undefined,
        status: "completed",
        integrationProvider: filters.integrationProvider || undefined,
        periodStart: periodRange.start,
        periodEnd: periodRange.end,
        page: filters.page,
        size: PAGE_SIZE,
      }),
    retry: false,
  });
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });
  const activeFilterCount = [
    filters.simulationId,
    filters.period,
    filters.integrationProvider,
  ].filter(Boolean).length;

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-5">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="text-3xl font-semibold">{copy.title}</h1>
            <p className="mt-1 max-w-2xl text-sm text-muted-foreground">{copy.description}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button asChild variant="outline" className="gap-2 bg-card">
              <Link to="/participacoes">
                <Users className="h-4 w-4" />
                {copy.participations}
              </Link>
            </Button>
            <Button asChild variant="outline" className="gap-2 bg-card">
              <Link
                to="/talent-match"
                search={{
                  simulationId: filters.simulationId || undefined,
                  versionNumber: undefined,
                }}
              >
                <BarChart3 className="h-4 w-4" />
                {copy.compare}
              </Link>
            </Button>
            <Button
              type="button"
              variant="outline"
              className="gap-2 bg-card"
              onClick={() => void resultsQuery.refetch()}
              disabled={resultsQuery.isFetching}
            >
              <RefreshCw className={cn("h-4 w-4", resultsQuery.isFetching && "animate-spin")} />
              {copy.reload}
            </Button>
          </div>
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
              aria-expanded={showFilters}
              onClick={() => setShowFilters((current) => !current)}
              className={cn(
                "inline-flex min-h-10 items-center gap-2 rounded-md border px-3 py-2 text-sm font-medium",
                showFilters || activeFilterCount > 0
                  ? "border-primary/40 bg-primary/10 text-primary"
                  : "border-border bg-background hover:bg-accent",
              )}
            >
              <SlidersHorizontal className="h-4 w-4" />
              {showFilters ? copy.hideFilters : copy.filters}
              {activeFilterCount > 0 && (
                <span className="rounded-full bg-primary px-1.5 py-0.5 text-[10px] text-primary-foreground">
                  {activeFilterCount}
                </span>
              )}
            </button>
          </div>

          {showFilters && (
            <div className="mt-4 border-t border-border pt-4">
              <div className="grid gap-3 sm:grid-cols-3">
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
                  value={filters.period}
                  onChange={(event) =>
                    updateFilters({ period: event.target.value as "" | "7" | "30" | "90" })
                  }
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
              {activeFilterCount > 0 && (
                <button
                  type="button"
                  onClick={() =>
                    updateFilters({ simulationId: "", period: "", integrationProvider: "" })
                  }
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
          <section className="rounded-md border border-border bg-card p-6 text-sm text-muted-foreground">
            {copy.loading}
          </section>
        ) : resultsQuery.isError ? (
          <StateBanner
            tone="danger"
            title={copy.errorTitle}
            action={
              <button
                type="button"
                onClick={() => void resultsQuery.refetch()}
                className="rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium"
              >
                {copy.reload}
              </button>
            }
          >
            {copy.errorDescription}
          </StateBanner>
        ) : resultsQuery.data ? (
          <ResultsContent
            data={resultsQuery.data}
            copy={copy}
            page={filters.page}
            onPageChange={(page) => updateFilters({ page }, false)}
          />
        ) : null}
      </main>
    </AppShell>
  );
}

function ResultsContent({
  data,
  copy,
  page,
  onPageChange,
}: {
  data: ResultsPageResponse;
  copy: (typeof copyByLanguage)[Language];
  page: number;
  onPageChange: (page: number) => void;
}) {
  if (data.items.length === 0) {
    return (
      <EmptyState
        title={copy.emptyTitle}
        description={copy.emptyDescription}
        actions={
          <Button asChild>
            <Link to="/participacoes">
              {copy.participations}
              <ExternalLink className="ml-2 h-4 w-4" />
            </Link>
          </Button>
        }
      />
    );
  }

  return (
    <div className="space-y-4">
      <ResultsTable items={data.items} copy={copy} />
      <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
        <div className="text-muted-foreground">
          {data.totalItems.toLocaleString(copy.locale)}{" "}
          {data.totalItems === 1 ? copy.resultSingular : copy.resultPlural}
        </div>
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            disabled={page <= 0}
            onClick={() => onPageChange(page - 1)}
          >
            {copy.previous}
          </Button>
          <span className="text-xs text-muted-foreground">
            {data.totalPages === 0 ? 0 : page + 1} / {data.totalPages}
          </span>
          <Button
            type="button"
            variant="outline"
            disabled={page + 1 >= data.totalPages}
            onClick={() => onPageChange(page + 1)}
          >
            {copy.next}
          </Button>
        </div>
      </div>
    </div>
  );
}

function ResultsTable({
  items,
  copy,
}: {
  items: ResultListItemResponse[];
  copy: (typeof copyByLanguage)[Language];
}) {
  return (
    <section className="overflow-hidden rounded-md border border-border bg-card">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[760px] text-sm">
          <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
            <tr>
              <th className="px-4 py-3 text-left font-medium">{copy.candidate}</th>
              <th className="px-4 py-3 text-left font-medium">{copy.process}</th>
              <th className="px-4 py-3 text-left font-medium">{copy.score}</th>
              <th className="px-4 py-3 text-left font-medium">{copy.source}</th>
              <th className="px-4 py-3 text-right font-medium">{copy.action}</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr
                key={item.attemptId}
                className="border-b border-border last:border-0 hover:bg-accent/35"
              >
                <td className="px-4 py-3">
                  <div className="font-medium">{item.candidateName}</div>
                  <div className="mt-0.5 text-xs text-muted-foreground">{item.candidateEmail}</div>
                </td>
                <td className="px-4 py-3 text-muted-foreground">{item.simulationTitle}</td>
                <td className="px-4 py-3">
                  <div className="font-semibold tabular-nums">
                    {item.overallScore == null ? "—" : `${item.overallScore}%`}
                  </div>
                  {item.highlightCompetency && (
                    <div className="mt-1 text-[11px] text-muted-foreground">
                      {item.highlightCompetency}
                    </div>
                  )}
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {providerLabel(item.integrationProvider)}
                </td>
                <td className="px-4 py-3 text-right">
                  <Link
                    to="/results/$attemptId"
                    params={{ attemptId: item.attemptId }}
                    className="font-medium text-primary hover:underline"
                    search={filters}
                  >
                    {copy.analyze}
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
  return { GUPY: "Gupy", RECRUTEI: "Recrutei", API: "API", MANUAL: "Manual" }[value ?? ""] ?? "—";
}

function isPeriod(value: unknown): value is "7" | "30" | "90" {
  return value === "7" || value === "30" || value === "90";
}
