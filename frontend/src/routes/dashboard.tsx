import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  AlertTriangle,
  BarChart3,
  CalendarClock,
  CheckCircle2,
  ClipboardCheck,
  RefreshCw,
  Route as RouteIcon,
  Users,
  type LucideIcon,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { DashboardCompatibilityError, getDashboard } from "@/lib/api/dashboard-strict";
import { type DashboardActionSeverity, type DashboardResponse } from "@/lib/api/praxis";
import { canonicalAppRoute } from "@/lib/canonical-app-route";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/dashboard")({
  head: () => ({
    meta: [
      { title: "Painel - Práxis" },
      {
        name: "description",
        content: "Resumo das pendências e dos números essenciais do processo de avaliação.",
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

  return (
    <AppShell>
      {dashboardQuery.isLoading ? (
        <LoadingState />
      ) : dashboardQuery.isError ? (
        <ErrorState error={dashboardQuery.error} onReload={() => dashboardQuery.refetch()} />
      ) : dashboardQuery.data ? (
        <DashboardContent dashboard={dashboardQuery.data} onReload={() => dashboardQuery.refetch()} />
      ) : null}
    </AppShell>
  );
}

function DashboardContent({ dashboard, onReload }: { dashboard: DashboardResponse; onReload: () => void }) {
  return (
    <main className="mx-auto max-w-7xl space-y-6">
      <DashboardHeader dashboard={dashboard} onReload={onReload} />
      <PriorityAction actions={dashboard.recommendedActions} />

      <section aria-labelledby="dashboard-summary-title">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 id="dashboard-summary-title" className="text-lg font-semibold">Resumo do processo</h2>
          <span className="text-xs text-muted-foreground">Cada número abre a tela responsável por essa etapa</span>
        </div>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <DashboardMetricCard
            title="Avaliações ativas"
            value={dashboard.activeSimulations}
            hint="Conteúdo publicado e disponível"
            icon={ClipboardCheck}
            to="/avaliacoes"
          />
          <DashboardMetricCard
            title="Jornadas"
            value={dashboard.assessmentJourneys.total}
            hint={`${dashboard.assessmentJourneys.published} publicadas · ${dashboard.assessmentJourneys.draft} rascunhos`}
            icon={RouteIcon}
            to="/jornadas"
          />
          <DashboardMetricCard
            title="Participações em andamento"
            value={dashboard.candidatesInProgress}
            hint="Convites e processos ainda abertos"
            icon={Users}
            to="/participacoes"
          />
          <DashboardMetricCard
            title="Resultados em 30 dias"
            value={dashboard.completedAttemptsLast30Days}
            hint="Participações concluídas para análise"
            icon={BarChart3}
            to="/results"
          />
        </div>
      </section>

      <LatestResultsTable dashboard={dashboard} />
    </main>
  );
}

function DashboardHeader({ dashboard, onReload }: { dashboard: DashboardResponse; onReload: () => void }) {
  return (
    <header className="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-3xl font-semibold text-foreground">Painel da {dashboard.empresaName}</h1>
        <p className="mt-1 flex items-center gap-2 text-sm text-muted-foreground">
          <CalendarClock className="h-4 w-4" />
          Veja primeiro o que precisa de ação. Cada etapa tem uma única tela responsável.
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
  const firstAction = actions[0];

  if (!firstAction) {
    return (
      <section className="flex items-start gap-3 rounded-md border border-success/35 bg-success/10 p-4">
        <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-success" />
        <div>
          <h2 className="font-semibold text-foreground">Nenhuma pendência prioritária</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Continue acompanhando Participações e abra Resultados quando uma avaliação for concluída.
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
            <div className="text-xs font-semibold uppercase tracking-wide text-primary">Próxima ação</div>
            <h2 id="priority-action-title" className="mt-1 text-lg font-semibold text-foreground">
              {firstAction.title}
            </h2>
            <p className="mt-1 max-w-3xl text-sm text-muted-foreground">{firstAction.description}</p>
            {actions.length > 1 && (
              <p className="mt-2 text-xs text-muted-foreground">
                Há mais {actions.length - 1} {actions.length === 2 ? "pendência" : "pendências"} nas telas responsáveis.
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

function DashboardMetricCard({
  title,
  value,
  hint,
  icon: Icon,
  to,
}: {
  title: string;
  value: number;
  hint: string;
  icon: LucideIcon;
  to: "/avaliacoes" | "/jornadas" | "/participacoes" | "/results";
}) {
  return (
    <Link to={to} className="block rounded-md border border-border bg-card p-4 transition hover:border-primary/40 hover:bg-accent">
      <div className="flex items-center justify-between gap-3">
        <div className="text-xs uppercase text-muted-foreground">{title}</div>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </div>
      <div className="mt-2 text-3xl font-semibold tabular-nums text-foreground">
        {value.toLocaleString("pt-BR")}
      </div>
      <div className="mt-1 text-xs text-muted-foreground">{hint}</div>
    </Link>
  );
}

function LatestResultsTable({ dashboard }: { dashboard: DashboardResponse }) {
  const latestResults = dashboard.latestResults.slice(0, 5);
  return (
    <section className="rounded-md border border-border bg-card" aria-labelledby="latest-results-title">
      <div className="flex items-center justify-between gap-3 border-b border-border p-4">
        <div>
          <h2 id="latest-results-title" className="text-lg font-semibold">Resultados recentes</h2>
          <p className="mt-0.5 text-xs text-muted-foreground">Somente os cinco registros mais recentes.</p>
        </div>
        <Link to="/results" className="text-sm font-medium text-primary hover:underline">Ver todos</Link>
      </div>

      {latestResults.length === 0 ? (
        <div className="p-4 text-sm text-muted-foreground">Nenhum resultado disponível ainda.</div>
      ) : (
        <div className="overflow-x-auto" data-no-pagination>
          <table className="w-full text-sm">
            <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
              <tr>
                <th className="px-4 py-3 text-left font-medium">Participante</th>
                <th className="px-4 py-3 text-left font-medium">Processo</th>
                <th className="px-4 py-3 text-left font-medium">Situação</th>
                <th className="px-4 py-3 text-left font-medium">Data</th>
                <th className="px-4 py-3 text-right font-medium">Ação</th>
              </tr>
            </thead>
            <tbody>
              {latestResults.map((result) => (
                <tr key={result.attemptId} className="border-b border-border last:border-0">
                  <td className="px-4 py-3 font-medium">{result.candidateName}</td>
                  <td className="px-4 py-3 text-muted-foreground">{result.simulationOrJourneyName}</td>
                  <td className="px-4 py-3">
                    <div>{attemptStatusLabel(result.status)}</div>
                    {result.result != null && (
                      <div className="mt-0.5 text-xs tabular-nums text-muted-foreground">{result.result}%</div>
                    )}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">{formatDate(result.date)}</td>
                  <td className="px-4 py-3 text-right">
                    {result.status === "completed" ? (
                      <Link
                        to="/results/$attemptId"
                        params={{ attemptId: result.attemptId }}
                        className="font-medium text-primary hover:underline"
                      >
                        {result.actionLabel}
                      </Link>
                    ) : (
                      <Link to="/participacoes" className="font-medium text-primary hover:underline">
                        Acompanhar
                      </Link>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function LoadingState() {
  return (
    <section className="rounded-md border border-border bg-card p-6">
      <div className="text-sm font-medium">Carregando seu painel...</div>
      <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="h-28 animate-pulse rounded-md bg-muted" />
        ))}
      </div>
    </section>
  );
}

function ErrorState({ error, onReload }: { error: unknown; onReload: () => void }) {
  const incompatible = error instanceof DashboardCompatibilityError;
  const message = error instanceof Error ? error.message : "O painel não pôde ser carregado nesta tentativa.";
  return (
    <StateBanner
      tone={incompatible ? "warn" : "danger"}
      title={incompatible ? "Painel indisponível nesta versão do sistema." : "Não foi possível carregar o painel."}
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

function attemptStatusLabel(status: string) {
  return {
    notStarted: "Não iniciado",
    inProgress: "Em andamento",
    completed: "Concluído",
    expired: "Expirado",
    abandoned: "Abandonado",
  }[status] ?? status;
}

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" }).format(date);
}
