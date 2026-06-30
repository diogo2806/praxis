import { createFileRoute, Link, Outlet, useRouterState } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { BarChart3, CheckCircle2, Clock3, ExternalLink, RefreshCw, Search, TimerOff } from "lucide-react";
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

export const Route = createFileRoute("/results")({
  head: () => ({
    meta: [
      { title: "Resultados - Praxis" },
      {
        name: "description",
        content: "Lista, filtros e análise de resultados de candidatos avaliados no Praxis.",
      },
    ],
  }),
  component: ResultsPage,
});

const PAGE_SIZE = 20;

const statusOptions: Array<{ value: AttemptStatus; label: string }> = [
  { value: "notStarted", label: "Criado" },
  { value: "inProgress", label: "Em andamento" },
  { value: "completed", label: "Concluído" },
  { value: "expired", label: "Expirado" },
  { value: "abandoned", label: "Abandonado" },
];

function ResultsPage() {
  const isResultsIndex = useRouterState({
    select: (state) => state.location.pathname === "/results",
  });

  return isResultsIndex ? <ResultsIndexPage /> : <Outlet />;
}

function ResultsIndexPage() {
  const [search, setSearch] = useState("");
  const [simulationId, setSimulationId] = useState("");
  const [status, setStatus] = useState<AttemptStatus | "">("");
  const [period, setPeriod] = useState("");
  const [integrationProvider, setIntegrationProvider] = useState("");
  const [page, setPage] = useState(0);

  const periodRange = useMemo(() => rangeForPeriod(period), [period]);
  const resultsQuery = useQuery({
    queryKey: ["results", { search, simulationId, status, period, integrationProvider, page }],
    queryFn: () =>
      listResults({
        search: search.trim() || undefined,
        simulationId: simulationId || undefined,
        status: status || undefined,
        integrationProvider: integrationProvider || undefined,
        periodStart: periodRange.start,
        periodEnd: periodRange.end,
        page,
        size: PAGE_SIZE,
      }),
    retry: false,
  });
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });

  return (
    <AppShell>
      <div className="space-y-6">
        <ResultsHeader onReload={() => resultsQuery.refetch()} />
        <ResultsFilters
          search={search}
          simulationId={simulationId}
          status={status}
          period={period}
          integrationProvider={integrationProvider}
          simulations={simulationsQuery.data ?? []}
          onSearch={(value) => {
            setSearch(value);
            setPage(0);
          }}
          onSimulationId={(value) => {
            setSimulationId(value);
            setPage(0);
          }}
          onStatus={(value) => {
            setStatus(value);
            setPage(0);
          }}
          onPeriod={(value) => {
            setPeriod(value);
            setPage(0);
          }}
          onIntegrationProvider={(value) => {
            setIntegrationProvider(value);
            setPage(0);
          }}
        />

        {resultsQuery.isLoading ? (
          <ResultsLoadingState />
        ) : resultsQuery.isError ? (
          <ResultsErrorState onReload={() => resultsQuery.refetch()} />
        ) : resultsQuery.data ? (
          <ResultsContent data={resultsQuery.data} page={page} onPageChange={setPage} />
        ) : null}
      </div>
    </AppShell>
  );
}

function ResultsHeader({ onReload }: { onReload: () => void }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div>
        <div className="text-xs uppercase text-primary">Práxis / Resultados</div>
        <h1 className="mt-1 text-3xl font-semibold">Resultados</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Acompanhe os candidatos avaliados, veja status, pontuação geral e aderência por competência.
        </p>
      </div>
      <button
        type="button"
        onClick={onReload}
        className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
      >
        <RefreshCw className="h-4 w-4" />
        Recarregar
      </button>
    </div>
  );
}

function ResultsFilters({
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
      <div className="mb-3 text-sm font-semibold">Filtros</div>
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(240px,1.4fr)_repeat(4,minmax(160px,1fr))]">
        <label className="relative block">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            value={search}
            onChange={(event) => onSearch(event.target.value)}
            placeholder="Buscar candidato..."
            className="h-10 w-full rounded-md border border-border bg-background pl-9 pr-3 text-sm"
          />
        </label>
        <select
          value={simulationId}
          onChange={(event) => onSimulationId(event.target.value)}
          className="h-10 rounded-md border border-border bg-background px-3 text-sm"
        >
          <option value="">Avaliação</option>
          {simulations.map((simulation) => (
            <option key={simulation.id} value={simulation.id}>
              {simulation.name}
            </option>
          ))}
        </select>
        <select
          value={status}
          onChange={(event) => onStatus(event.target.value as AttemptStatus | "")}
          className="h-10 rounded-md border border-border bg-background px-3 text-sm"
        >
          <option value="">Status</option>
          {statusOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <select
          value={period}
          onChange={(event) => onPeriod(event.target.value)}
          className="h-10 rounded-md border border-border bg-background px-3 text-sm"
        >
          <option value="">Período</option>
          <option value="7">Últimos 7 dias</option>
          <option value="30">Últimos 30 dias</option>
          <option value="90">Últimos 90 dias</option>
        </select>
        <select
          value={integrationProvider}
          onChange={(event) => onIntegrationProvider(event.target.value)}
          className="h-10 rounded-md border border-border bg-background px-3 text-sm"
        >
          <option value="">Integração</option>
          <option value="GUPY">Gupy</option>
          <option value="RECRUTEI">Recrutei</option>
          <option value="API">API</option>
          <option value="MANUAL">Manual</option>
        </select>
      </div>
    </section>
  );
}

function ResultsContent({
  data,
  page,
  onPageChange,
}: {
  data: ResultsPageResponse;
  page: number;
  onPageChange: (page: number) => void;
}) {
  if (data.items.length === 0) {
    return (
      <>
        <ResultsSummaryCards data={data} />
        <EmptyState
          title="Nenhum resultado encontrado."
          description="Assim que candidatos concluírem avaliações, os resultados aparecerão aqui."
          actions={
            <Link
              to="/candidate-links/new"
              className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Gerar link de avaliação
              <ExternalLink className="h-4 w-4" />
            </Link>
          }
        />
      </>
    );
  }

  return (
    <>
      <ResultsSummaryCards data={data} />
      <ResultsTable items={data.items} />
      <ResultsPagination data={data} page={page} onPageChange={onPageChange} />
    </>
  );
}

function ResultsSummaryCards({ data }: { data: ResultsPageResponse }) {
  const cards = [
    { title: "Concluídos", value: data.summary.completed, icon: CheckCircle2 },
    { title: "Em andamento", value: data.summary.inProgress, icon: Clock3 },
    { title: "Expirados", value: data.summary.expired, icon: TimerOff },
    {
      title: "Média geral",
      value: data.summary.averageScore == null ? "-" : `${data.summary.averageScore}%`,
      icon: BarChart3,
    },
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

function ResultsTable({ items }: { items: ResultListItemResponse[] }) {
  return (
    <section className="rounded-md border border-border bg-card">
      <div className="border-b border-border p-4">
        <h2 className="text-lg font-semibold">Tabela de resultados</h2>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[980px] text-sm">
          <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
            <tr>
              <th className="px-4 py-3 text-left font-medium">Candidato</th>
              <th className="px-4 py-3 text-left font-medium">Avaliação</th>
              <th className="px-4 py-3 text-left font-medium">Status</th>
              <th className="px-4 py-3 text-left font-medium">Competência destaque</th>
              <th className="px-4 py-3 text-left font-medium">Resultado</th>
              <th className="px-4 py-3 text-left font-medium">Integração</th>
              <th className="px-4 py-3 text-right font-medium">Ação</th>
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
                  <ResultStatusBadge status={item.status} />
                </td>
                <td className="px-4 py-3">{item.highlightCompetency ?? "-"}</td>
                <td className="px-4 py-3 font-medium tabular-nums">
                  {item.overallScore == null ? "-" : `${item.overallScore}%`}
                </td>
                <td className="px-4 py-3 text-muted-foreground">{providerLabel(item.integrationProvider)}</td>
                <td className="px-4 py-3 text-right">
                  <Link
                    to="/results/$attemptId"
                    params={{ attemptId: item.attemptId }}
                    className="text-sm font-medium text-primary hover:underline"
                  >
                    {item.status === "completed" ? "Ver detalhes" : "Acompanhar"}
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
  data,
  page,
  onPageChange,
}: {
  data: ResultsPageResponse;
  page: number;
  onPageChange: (page: number) => void;
}) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
      <div className="text-muted-foreground">
        {data.totalItems.toLocaleString("pt-BR")} resultado{data.totalItems === 1 ? "" : "s"}
      </div>
      <div className="flex items-center gap-2">
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
          className="rounded-md border border-border bg-card px-3 py-2 disabled:cursor-not-allowed disabled:opacity-50"
        >
          Anterior
        </button>
        <span className="text-muted-foreground">
          Página {data.totalPages === 0 ? 0 : page + 1} de {data.totalPages}
        </span>
        <button
          type="button"
          disabled={page + 1 >= data.totalPages}
          onClick={() => onPageChange(page + 1)}
          className="rounded-md border border-border bg-card px-3 py-2 disabled:cursor-not-allowed disabled:opacity-50"
        >
          Próxima
        </button>
      </div>
    </div>
  );
}

function ResultsLoadingState() {
  return (
    <section className="rounded-md border border-border bg-card p-6">
      <div className="text-sm font-medium">Carregando resultados...</div>
      <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="h-24 animate-pulse rounded-md bg-muted" />
        ))}
      </div>
    </section>
  );
}

function ResultsErrorState({ onReload }: { onReload: () => void }) {
  return (
    <StateBanner
      tone="danger"
      title="Não foi possível carregar os resultados."
      action={
        <button
          type="button"
          onClick={onReload}
          className="rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium"
        >
          Recarregar
        </button>
      }
    >
      Tente novamente.
    </StateBanner>
  );
}

function ResultStatusBadge({ status }: { status: AttemptStatus }) {
  const meta = resultStatusMeta(status);
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-[11px] font-medium ${meta.className}`}
    >
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {meta.label}
    </span>
  );
}

function resultStatusMeta(status: AttemptStatus) {
  return (
    {
      notStarted: {
        label: "Criado",
        className: "border-border bg-muted text-foreground",
      },
      inProgress: {
        label: "Em andamento",
        className: "border-primary/25 bg-primary/10 text-foreground",
      },
      paused: {
        label: "Pausado",
        className: "border-warning/35 bg-warning/15 text-warning-foreground",
      },
      completed: {
        label: "Concluído",
        className: "border-success/25 bg-success/10 text-foreground",
      },
      abandoned: {
        label: "Abandonado",
        className: "border-danger/25 bg-danger/10 text-foreground",
      },
      expired: {
        label: "Expirado",
        className: "border-warning/35 bg-warning/15 text-warning-foreground",
      },
      failed: {
        label: "Falhou",
        className: "border-danger/25 bg-danger/10 text-foreground",
      },
    } satisfies Record<AttemptStatus, { label: string; className: string }>
  )[status];
}

function rangeForPeriod(period: string) {
  if (!period) {
    return { start: undefined, end: undefined };
  }
  const days = Number(period);
  if (!Number.isFinite(days)) {
    return { start: undefined, end: undefined };
  }
  const end = new Date();
  const start = new Date(end);
  start.setDate(start.getDate() - days);
  return { start: start.toISOString(), end: end.toISOString() };
}

function providerLabel(value: string | null) {
  return {
    GUPY: "Gupy",
    RECRUTEI: "Recrutei",
    API: "API",
    MANUAL: "Manual",
  }[value ?? ""] ?? "-";
}
