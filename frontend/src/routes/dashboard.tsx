import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  AlertTriangle,
  BarChart3,
  CalendarClock,
  CheckCircle2,
  ClipboardCheck,
  Gauge,
  PlayCircle,
  RefreshCw,
  Route as RouteIcon,
  Send,
  TrendingUp,
  Users,
  type LucideIcon,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import {
  getDashboardAnalytics,
  type DashboardActivityPoint,
  type DashboardAnalyticsResponse,
  type DashboardParticipationSummary,
} from "@/lib/api/dashboard-analytics";
import { DashboardCompatibilityError, getDashboard } from "@/lib/api/dashboard-strict";
import { type DashboardActionSeverity, type DashboardResponse } from "@/lib/api/praxis";
import { canonicalAppRoute } from "@/lib/canonical-app-route";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/dashboard")({
  head: () => ({
    meta: [
      { title: "Dashboard - Práxis" },
      {
        name: "description",
        content: "Indicadores, tendências e pontos de atenção do processo de avaliação.",
      },
    ],
  }),
  component: DashboardPage,
});

function DashboardPage() {
  const dashboardQuery = useQuery({
    queryKey: ["dashboard"],
    queryFn: getDashboard,
    retry: false,
  });
  const analyticsQuery = useQuery({
    queryKey: ["dashboard", "analytics"],
    queryFn: getDashboardAnalytics,
    retry: false,
  });

  const reload = () => {
    void Promise.all([dashboardQuery.refetch(), analyticsQuery.refetch()]);
  };

  return (
    <AppShell>
      {dashboardQuery.isLoading ? (
        <LoadingState />
      ) : dashboardQuery.isError ? (
        <ErrorState error={dashboardQuery.error} onReload={reload} />
      ) : dashboardQuery.data ? (
        <DashboardContent
          dashboard={dashboardQuery.data}
          analytics={analyticsQuery.data ?? null}
          analyticsLoading={analyticsQuery.isLoading}
          analyticsError={analyticsQuery.isError}
          onReload={reload}
        />
      ) : null}
    </AppShell>
  );
}

function DashboardContent({
  dashboard,
  analytics,
  analyticsLoading,
  analyticsError,
  onReload,
}: {
  dashboard: DashboardResponse;
  analytics: DashboardAnalyticsResponse | null;
  analyticsLoading: boolean;
  analyticsError: boolean;
  onReload: () => void;
}) {
  return (
    <main className="mx-auto max-w-7xl space-y-6">
      <DashboardHeader dashboard={dashboard} onReload={onReload} />
      <PriorityAction actions={dashboard.recommendedActions} />
      <DashboardMetrics dashboard={dashboard} analytics={analytics} />

      {analyticsLoading ? (
        <AnalyticsLoadingState />
      ) : analyticsError || !analytics ? (
        <AnalyticsUnavailable />
      ) : (
        <AnalyticsGrid analytics={analytics} />
      )}

      <OperationStructure dashboard={dashboard} />
    </main>
  );
}

function DashboardHeader({ dashboard, onReload }: { dashboard: DashboardResponse; onReload: () => void }) {
  return (
    <header className="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-3xl font-semibold text-foreground">Dashboard da {dashboard.empresaName}</h1>
        <p className="mt-1 flex items-center gap-2 text-sm text-muted-foreground">
          <CalendarClock className="h-4 w-4" />
          Indicadores da operação e movimentação dos últimos 30 dias.
        </p>
      </div>
      <button
        type="button"
        onClick={onReload}
        className="inline-flex min-h-10 items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
      >
        <RefreshCw className="h-4 w-4" />
        Atualizar
      </button>
    </header>
  );
}

function PriorityAction({ actions }: { actions: DashboardResponse["recommendedActions"] }) {
  const pendingActions = actions.filter((action) => action.severity !== "success");
  const firstAction = pendingActions[0];

  if (!firstAction) {
    return (
      <section className="flex items-start gap-3 rounded-md border border-success/35 bg-success/10 p-4">
        <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-success" />
        <div>
          <h2 className="font-semibold text-foreground">Sem bloqueios prioritários</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            A operação não possui uma pendência crítica identificada neste momento.
          </p>
        </div>
      </section>
    );
  }

  return (
    <section className="rounded-md border border-primary/30 bg-primary/5 p-4" aria-labelledby="priority-action-title">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex min-w-0 gap-3">
          <ActionIcon severity={firstAction.severity} />
          <div>
            <div className="text-xs font-semibold uppercase tracking-wide text-primary">Atenção necessária</div>
            <h2 id="priority-action-title" className="mt-1 text-lg font-semibold text-foreground">
              {firstAction.title}
            </h2>
            <p className="mt-1 max-w-3xl text-sm text-muted-foreground">{firstAction.description}</p>
            {pendingActions.length > 1 && (
              <p className="mt-2 text-xs text-muted-foreground">
                Há mais {pendingActions.length - 1} {pendingActions.length === 2 ? "pendência" : "pendências"} aguardando ação.
              </p>
            )}
          </div>
        </div>
        <a
          href={canonicalProcessRoute(firstAction.route)}
          className="inline-flex min-h-10 shrink-0 items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          {firstAction.buttonLabel}
        </a>
      </div>
    </section>
  );
}

function DashboardMetrics({
  dashboard,
  analytics,
}: {
  dashboard: DashboardResponse;
  analytics: DashboardAnalyticsResponse | null;
}) {
  const summary = analytics?.participations;

  return (
    <section aria-labelledby="dashboard-indicators-title">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 id="dashboard-indicators-title" className="text-lg font-semibold">Indicadores principais</h2>
        <span className="text-xs text-muted-foreground">Execuções de avaliações da empresa</span>
      </div>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <DashboardMetricCard
          title="Participações criadas"
          value={summary?.total ?? "—"}
          hint="Total histórico de execuções"
          icon={Users}
          to="/participacoes"
        />
        <DashboardMetricCard
          title="Em andamento"
          value={summary?.inProgress ?? dashboard.candidatesInProgress}
          hint="Execuções iniciadas e ainda abertas"
          icon={PlayCircle}
          to="/participacoes"
        />
        <DashboardMetricCard
          title="Concluídas em 30 dias"
          value={dashboard.completedAttemptsLast30Days}
          hint="Resultados gerados no período"
          icon={ClipboardCheck}
          to="/results"
        />
        <DashboardMetricCard
          title="Taxa de conclusão"
          value={summary ? formatPercent(summary.completionRatePercent) : "—"}
          hint="Entre participações encerradas"
          icon={TrendingUp}
          to="/results"
        />
      </div>
    </section>
  );
}

function DashboardMetricCard({
  title,
  value,
  hint,
  icon: Icon,
  to,
}: {
  title: string;
  value: number | string;
  hint: string;
  icon: LucideIcon;
  to: "/participacoes" | "/results";
}) {
  return (
    <Link to={to} className="block rounded-md border border-border bg-card p-4 transition hover:border-primary/40 hover:bg-accent">
      <div className="flex items-center justify-between gap-3">
        <div className="text-xs uppercase text-muted-foreground">{title}</div>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </div>
      <div className="mt-2 text-3xl font-semibold tabular-nums text-foreground">
        {typeof value === "number" ? value.toLocaleString("pt-BR") : value}
      </div>
      <div className="mt-1 text-xs text-muted-foreground">{hint}</div>
    </Link>
  );
}

function AnalyticsGrid({ analytics }: { analytics: DashboardAnalyticsResponse }) {
  return (
    <section className="grid gap-4 xl:grid-cols-2" aria-label="Análises do dashboard">
      <ActivityChart activity={analytics.activity} />
      <StatusBreakdown summary={analytics.participations} />
      <ParticipationFunnel summary={analytics.participations} />
      <PeriodQuality summary={analytics.participations} />
    </section>
  );
}

function ActivityChart({ activity }: { activity: DashboardActivityPoint[] }) {
  const maximum = Math.max(1, ...activity.flatMap((point) => [point.created, point.completed]));

  return (
    <figure className="rounded-md border border-border bg-card p-4" aria-labelledby="activity-chart-title">
      <figcaption>
        <h2 id="activity-chart-title" className="text-lg font-semibold text-foreground">Movimentação nos últimos 30 dias</h2>
        <p className="mt-1 text-xs text-muted-foreground">Participações criadas e concluídas por dia.</p>
      </figcaption>
      <div className="mt-4 flex flex-wrap gap-4 text-xs text-muted-foreground" aria-hidden="true">
        <span className="inline-flex items-center gap-2"><span className="h-2.5 w-2.5 rounded-sm bg-primary/40" />Criadas</span>
        <span className="inline-flex items-center gap-2"><span className="h-2.5 w-2.5 rounded-sm bg-success" />Concluídas</span>
      </div>
      <div className="mt-4 overflow-x-auto pb-2">
        <div className="flex h-56 min-w-[760px] items-end gap-1 border-b border-border px-1">
          {activity.map((point, index) => (
            <div key={point.date} className="flex h-full min-w-5 flex-1 flex-col justify-end gap-1">
              <div
                className="flex h-44 items-end justify-center gap-0.5"
                aria-label={`${formatChartDate(point.date)}: ${point.created} criadas e ${point.completed} concluídas`}
                title={`${formatChartDate(point.date)}: ${point.created} criadas, ${point.completed} concluídas`}
              >
                <div
                  className="w-2 rounded-t-sm bg-primary/40"
                  style={{ height: chartBarHeight(point.created, maximum) }}
                />
                <div
                  className="w-2 rounded-t-sm bg-success"
                  style={{ height: chartBarHeight(point.completed, maximum) }}
                />
              </div>
              <span className="h-5 text-center text-[10px] text-muted-foreground">
                {index % 5 === 0 || index === activity.length - 1 ? formatChartDay(point.date) : ""}
              </span>
            </div>
          ))}
        </div>
      </div>
    </figure>
  );
}

function StatusBreakdown({ summary }: { summary: DashboardParticipationSummary }) {
  const items = [
    { label: "Não iniciadas", value: summary.notStarted, className: "bg-muted-foreground/45" },
    { label: "Em andamento", value: summary.inProgress, className: "bg-primary" },
    { label: "Concluídas", value: summary.completed, className: "bg-success" },
    { label: "Abandonadas ou expiradas", value: summary.abandoned + summary.expired, className: "bg-danger" },
  ];

  return (
    <figure className="rounded-md border border-border bg-card p-4" aria-labelledby="status-chart-title">
      <figcaption>
        <h2 id="status-chart-title" className="text-lg font-semibold text-foreground">Situação das participações</h2>
        <p className="mt-1 text-xs text-muted-foreground">Distribuição histórica das execuções de avaliações.</p>
      </figcaption>
      <div className="mt-5 space-y-5">
        {items.map((item) => {
          const percentage = summary.total > 0 ? (item.value / summary.total) * 100 : 0;
          return (
            <div key={item.label}>
              <div className="mb-1.5 flex items-center justify-between gap-3 text-sm">
                <span className="text-foreground">{item.label}</span>
                <span className="tabular-nums text-muted-foreground">
                  {item.value.toLocaleString("pt-BR")} · {formatPercent(percentage)}
                </span>
              </div>
              <div className="h-2.5 overflow-hidden rounded-full bg-muted">
                <div
                  className={cn("h-full rounded-full", item.className)}
                  style={{ width: `${Math.min(100, Math.max(0, percentage))}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </figure>
  );
}

function ParticipationFunnel({ summary }: { summary: DashboardParticipationSummary }) {
  const stages = [
    { label: "Criadas", value: summary.total },
    { label: "Iniciadas", value: summary.started },
    { label: "Concluídas", value: summary.completed },
  ];

  return (
    <figure className="rounded-md border border-border bg-card p-4" aria-labelledby="funnel-title">
      <figcaption>
        <h2 id="funnel-title" className="text-lg font-semibold text-foreground">Funil de participação</h2>
        <p className="mt-1 text-xs text-muted-foreground">Conversão entre criação, início e conclusão.</p>
      </figcaption>
      <div className="mt-5 space-y-4">
        {stages.map((stage, index) => {
          const percentage = summary.total > 0 ? (stage.value / summary.total) * 100 : 0;
          return (
            <div key={stage.label} className="flex items-center gap-3">
              <div className="w-24 shrink-0 text-sm text-foreground">{stage.label}</div>
              <div className="h-10 flex-1 overflow-hidden rounded-md bg-muted">
                <div
                  className={cn(
                    "flex h-full min-w-12 items-center justify-end rounded-md px-3 text-xs font-semibold",
                    index === 0
                      ? "bg-primary/25 text-foreground"
                      : index === 1
                        ? "bg-primary/55 text-primary-foreground"
                        : "bg-primary text-primary-foreground",
                  )}
                  style={{ width: `${Math.max(percentage, stage.value > 0 ? 8 : 0)}%` }}
                >
                  {stage.value.toLocaleString("pt-BR")}
                </div>
              </div>
              <div className="w-14 text-right text-xs tabular-nums text-muted-foreground">
                {formatPercent(percentage)}
              </div>
            </div>
          );
        })}
      </div>
    </figure>
  );
}

function PeriodQuality({ summary }: { summary: DashboardParticipationSummary }) {
  return (
    <section className="rounded-md border border-border bg-card p-4" aria-labelledby="period-quality-title">
      <h2 id="period-quality-title" className="text-lg font-semibold text-foreground">Qualidade do período</h2>
      <p className="mt-1 text-xs text-muted-foreground">Leitura rápida das participações encerradas.</p>
      <div className="mt-5 grid gap-3 sm:grid-cols-2">
        <div className="rounded-md bg-muted/45 p-4">
          <div className="flex items-center gap-2 text-xs uppercase text-muted-foreground">
            <Gauge className="h-4 w-4" /> Resultado médio
          </div>
          <div className="mt-2 text-3xl font-semibold tabular-nums text-foreground">
            {summary.averageScoreLast30Days == null ? "—" : `${summary.averageScoreLast30Days.toFixed(1)}%`}
          </div>
          <p className="mt-1 text-xs text-muted-foreground">Somente conclusões dos últimos 30 dias.</p>
        </div>
        <div className="rounded-md bg-muted/45 p-4">
          <div className="flex items-center gap-2 text-xs uppercase text-muted-foreground">
            <AlertTriangle className="h-4 w-4" /> Abandono
          </div>
          <div className="mt-2 text-3xl font-semibold tabular-nums text-foreground">
            {formatPercent(summary.dropOffRatePercent)}
          </div>
          <p className="mt-1 text-xs text-muted-foreground">Abandonadas ou expiradas entre as encerradas.</p>
        </div>
      </div>
    </section>
  );
}

function OperationStructure({ dashboard }: { dashboard: DashboardResponse }) {
  return (
    <section className="grid gap-4 xl:grid-cols-2" aria-label="Estrutura e atalhos da operação">
      <div className="rounded-md border border-border bg-card p-4">
        <h2 className="text-lg font-semibold text-foreground">Estrutura disponível</h2>
        <p className="mt-1 text-xs text-muted-foreground">Conteúdo pronto para utilização nos processos.</p>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <StructureItem icon={ClipboardCheck} label="Avaliações ativas" value={dashboard.activeSimulations} />
          <StructureItem icon={RouteIcon} label="Jornadas publicadas" value={dashboard.assessmentJourneys.published} />
          <StructureItem icon={AlertTriangle} label="Jornadas em rascunho" value={dashboard.assessmentJourneys.draft} />
        </div>
      </div>

      <div className="rounded-md border border-border bg-card p-4">
        <h2 className="text-lg font-semibold text-foreground">Atalhos operacionais</h2>
        <p className="mt-1 text-xs text-muted-foreground">Acesse diretamente as principais etapas do processo.</p>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <QuickAction to="/avaliacoes" icon={ClipboardCheck} label="Gerenciar avaliações" />
          <QuickAction to="/jornadas" icon={RouteIcon} label="Gerenciar jornadas" />
          <QuickAction to="/participacoes" icon={Send} label="Criar participação" />
          <QuickAction to="/results" icon={BarChart3} label="Analisar resultados" />
        </div>
      </div>
    </section>
  );
}

function StructureItem({ icon: Icon, label, value }: { icon: LucideIcon; label: string; value: number }) {
  return (
    <div className="rounded-md bg-muted/45 p-4">
      <Icon className="h-4 w-4 text-muted-foreground" />
      <div className="mt-3 text-2xl font-semibold tabular-nums text-foreground">{value.toLocaleString("pt-BR")}</div>
      <div className="mt-1 text-xs text-muted-foreground">{label}</div>
    </div>
  );
}

function QuickAction({
  to,
  icon: Icon,
  label,
}: {
  to: "/avaliacoes" | "/jornadas" | "/participacoes" | "/results";
  icon: LucideIcon;
  label: string;
}) {
  return (
    <Link
      to={to}
      className="flex min-h-12 items-center gap-3 rounded-md border border-border px-3 py-2 text-sm font-medium text-foreground hover:border-primary/40 hover:bg-accent"
    >
      <Icon className="h-4 w-4 text-primary" />
      {label}
    </Link>
  );
}

function AnalyticsLoadingState() {
  return (
    <section className="grid gap-4 xl:grid-cols-2" aria-label="Carregando indicadores analíticos">
      {Array.from({ length: 4 }).map((_, index) => (
        <div key={index} className="h-72 animate-pulse rounded-md border border-border bg-muted" />
      ))}
    </section>
  );
}

function AnalyticsUnavailable() {
  return (
    <StateBanner tone="warn" title="Indicadores analíticos temporariamente indisponíveis.">
      Os números principais continuam disponíveis. Atualize a página para tentar carregar os gráficos novamente.
    </StateBanner>
  );
}

function LoadingState() {
  return (
    <section className="space-y-4">
      <div className="rounded-md border border-border bg-card p-6">
        <div className="text-sm font-medium">Carregando dashboard...</div>
        <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="h-28 animate-pulse rounded-md bg-muted" />
          ))}
        </div>
      </div>
      <AnalyticsLoadingState />
    </section>
  );
}

function ErrorState({ error, onReload }: { error: unknown; onReload: () => void }) {
  const incompatible = error instanceof DashboardCompatibilityError;
  const message = error instanceof Error ? error.message : "O dashboard não pôde ser carregado nesta tentativa.";
  return (
    <StateBanner
      tone={incompatible ? "warn" : "danger"}
      title={incompatible ? "Dashboard indisponível nesta versão do sistema." : "Não foi possível carregar o dashboard."}
      action={
        <button
          type="button"
          onClick={onReload}
          className="rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium"
        >
          Tentar novamente
        </button>
      }
    >
      {incompatible
        ? "A interface e o serviço conectado estão em versões diferentes. Atualize a aplicação e tente novamente."
        : message}
    </StateBanner>
  );
}

function ActionIcon({ severity }: { severity: DashboardActionSeverity }) {
  if (severity === "success") return <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-success" />;
  return (
    <AlertTriangle
      className={cn(
        "mt-0.5 h-5 w-5 shrink-0",
        severity === "danger" ? "text-danger" : severity === "warning" ? "text-warning" : "text-primary",
      )}
    />
  );
}

function canonicalProcessRoute(route: string) {
  const canonical = canonicalAppRoute(route);
  if (canonical === "/notifications") return "/monitoramento";
  if (canonical === "/enviar-link") return "/participacoes";
  return canonical;
}

function chartBarHeight(value: number, maximum: number) {
  if (value <= 0) return "0%";
  return `${Math.max(5, (value / maximum) * 100)}%`;
}

function formatPercent(value: number) {
  return `${value.toLocaleString("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`;
}

function formatChartDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "long",
    timeZone: "UTC",
  }).format(new Date(`${value}T00:00:00Z`));
}

function formatChartDay(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    timeZone: "UTC",
  }).format(new Date(`${value}T00:00:00Z`));
}
