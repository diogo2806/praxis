import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import {
  Activity,
  AlertTriangle,
  Bell,
  CheckCircle2,
  Clock3,
  PlugZap,
  RefreshCw,
  Send,
  UserRoundSearch,
  Wifi,
  WifiOff,
  XCircle,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Button } from "@/components/ui/button";
import {
  listIntegrations,
  listResultDeliveries,
  listSimulations,
  type IntegrationCenterItem,
  type IntegrationCenterStatus,
} from "@/lib/api/praxis";
import {
  type MonitoringAttempt,
  type MonitoringAttemptStatus,
  searchMonitoringAttempts,
} from "@/lib/api/monitoring";
import { listNotifications } from "@/lib/api/operations";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/monitoramento")({
  head: () => ({
    meta: [
      { title: "Monitoramento operacional - Práxis" },
      {
        name: "description",
        content: "Central operacional para acompanhar tentativas, integrações e entregas.",
      },
    ],
  }),
  component: MonitoringPage,
});

type StatusFilter = MonitoringAttemptStatus | "all";

const statusOptions: Array<{ value: StatusFilter; label: string }> = [
  { value: "all", label: "Todos os estados" },
  { value: "notStarted", label: "Não iniciadas" },
  { value: "inProgress", label: "Em andamento" },
  { value: "completed", label: "Concluídas" },
  { value: "abandoned", label: "Abandonadas" },
  { value: "expired", label: "Expiradas" },
];

function MonitoringPage() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<StatusFilter>("all");
  const [simulationId, setSimulationId] = useState("");
  const [candidate, setCandidate] = useState("");
  const [autoRefresh, setAutoRefresh] = useState(true);
  const refreshInterval = autoRefresh ? 30_000 : false;

  const attemptsQuery = useQuery({
    queryKey: ["monitoring-attempts", page, status, simulationId, candidate],
    queryFn: () =>
      searchMonitoringAttempts({
        page,
        size: 25,
        status,
        simulationId: simulationId || undefined,
        candidate: candidate || undefined,
      }),
    refetchInterval: refreshInterval,
  });
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    refetchInterval: refreshInterval,
  });
  const deliveriesQuery = useQuery({
    queryKey: ["result-deliveries"],
    queryFn: () => listResultDeliveries(),
    refetchInterval: refreshInterval,
  });
  const notificationsQuery = useQuery({
    queryKey: ["notifications"],
    queryFn: listNotifications,
    refetchInterval: autoRefresh ? 60_000 : false,
  });
  const integrationsQuery = useQuery({
    queryKey: ["integrations", "monitoring"],
    queryFn: listIntegrations,
    refetchInterval: refreshInterval,
  });

  const attemptsPage = attemptsQuery.data;
  const attempts = attemptsPage?.items ?? [];
  const simulations = (simulationsQuery.data ?? []).filter(
    (simulation) => simulation.status === "published",
  );
  const integrations = integrationsQuery.data ?? [];
  const failedDeliveries = (deliveriesQuery.data ?? []).filter(
    (delivery) => delivery.status === "dlq",
  ).length;
  const retryingDeliveries = (deliveriesQuery.data ?? []).filter(
    (delivery) => delivery.status === "retrying" || delivery.status === "pending",
  ).length;
  const unreadNotifications = (notificationsQuery.data ?? []).filter(
    (notification) => !notification.readAt,
  ).length;
  const activeAttempts = attempts.filter(
    (attempt) => attempt.status === "inProgress" && attempt.active,
  ).length;
  const staleAttempts = attempts.filter(
    (attempt) => attempt.status === "inProgress" && !attempt.active,
  ).length;
  const integrationAlerts = integrations.filter(
    (integration) => integration.status === "ERRO" || integration.status === "PENDENTE",
  ).length;
  const connectedIntegrations = integrations.filter(
    (integration) => integration.status === "CONECTADA",
  ).length;
  const isRefreshing =
    attemptsQuery.isFetching ||
    simulationsQuery.isFetching ||
    deliveriesQuery.isFetching ||
    notificationsQuery.isFetching ||
    integrationsQuery.isFetching;
  const lastUpdatedAt = useMemo(
    () =>
      Math.max(
        attemptsQuery.dataUpdatedAt,
        deliveriesQuery.dataUpdatedAt,
        notificationsQuery.dataUpdatedAt,
        integrationsQuery.dataUpdatedAt,
      ),
    [
      attemptsQuery.dataUpdatedAt,
      deliveriesQuery.dataUpdatedAt,
      integrationsQuery.dataUpdatedAt,
      notificationsQuery.dataUpdatedAt,
    ],
  );

  function resetAnd(action: () => void) {
    setPage(0);
    action();
  }

  async function refreshAll() {
    await Promise.all([
      attemptsQuery.refetch(),
      simulationsQuery.refetch(),
      deliveriesQuery.refetch(),
      notificationsQuery.refetch(),
      integrationsQuery.refetch(),
    ]);
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Monitoramento
            </div>
            <h1 className="mt-1 font-display text-3xl">Central operacional</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Acompanhe tentativas, atividade dos provedores e entrega dos resultados. A tela se
              atualiza automaticamente sem omitir estados ou falhas.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              aria-pressed={autoRefresh}
              onClick={() => setAutoRefresh((current) => !current)}
              className={cn(
                "inline-flex min-h-10 items-center gap-2 rounded-md border px-3 py-2 text-sm font-medium",
                autoRefresh
                  ? "border-success/30 bg-success/10 text-success"
                  : "border-border bg-card text-muted-foreground hover:bg-accent",
              )}
            >
              {autoRefresh ? <Wifi className="h-4 w-4" /> : <WifiOff className="h-4 w-4" />}
              Atualização automática
            </button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => void refreshAll()}
              disabled={isRefreshing}
            >
              <RefreshCw className={cn("mr-2 h-4 w-4", isRefreshing && "animate-spin")} />
              Atualizar agora
            </Button>
          </div>
        </header>

        <div className="text-xs text-muted-foreground" role="status" aria-live="polite">
          {isRefreshing
            ? "Atualizando dados operacionais..."
            : lastUpdatedAt > 0
              ? `Última atualização: ${formatDateTime(new Date(lastUpdatedAt).toISOString())}`
              : "Aguardando a primeira atualização."}
        </div>

        <section className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6" aria-label="Resumo operacional">
          <Metric icon={<Send className="h-4 w-4" />} label="Total encontrado" value={attemptsPage?.totalElements ?? 0} />
          <Metric icon={<Activity className="h-4 w-4" />} label="Ativas nesta página" value={activeAttempts} />
          <Metric icon={<Clock3 className="h-4 w-4" />} label="Sem sinal nesta página" value={staleAttempts} warning={staleAttempts > 0} />
          <Metric icon={<PlugZap className="h-4 w-4" />} label="Integrações conectadas" value={connectedIntegrations} />
          <Metric icon={<AlertTriangle className="h-4 w-4" />} label="Integrações com atenção" value={integrationAlerts} warning={integrationAlerts > 0} />
          <Metric icon={<XCircle className="h-4 w-4" />} label="Entregas em DLQ" value={failedDeliveries} warning={failedDeliveries > 0} />
        </section>

        <IntegrationHealthPanel
          integrations={integrations}
          loading={integrationsQuery.isLoading}
          error={integrationsQuery.error}
          retryingDeliveries={retryingDeliveries}
        />

        <section className="rounded-xl border border-border bg-card p-4" aria-labelledby="monitoring-filters-title">
          <h2 id="monitoring-filters-title" className="sr-only">Filtros das tentativas</h2>
          <div className="grid gap-3 md:grid-cols-3">
            <label className="space-y-1 text-xs font-medium text-muted-foreground">
              Estado
              <select
                value={status}
                onChange={(event) => resetAnd(() => setStatus(event.target.value as StatusFilter))}
                className="input h-11 w-full"
              >
                {statusOptions.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
            <label className="space-y-1 text-xs font-medium text-muted-foreground">
              Avaliação
              <select
                value={simulationId}
                onChange={(event) => resetAnd(() => setSimulationId(event.target.value))}
                className="input h-11 w-full"
              >
                <option value="">Todas as avaliações</option>
                {simulations.map((simulation) => (
                  <option key={simulation.id} value={simulation.id}>{simulation.name}</option>
                ))}
              </select>
            </label>
            <label className="space-y-1 text-xs font-medium text-muted-foreground">
              Nome ou e-mail
              <div className="relative">
                <UserRoundSearch className="pointer-events-none absolute left-3 top-3.5 h-4 w-4 text-muted-foreground" />
                <input
                  value={candidate}
                  onChange={(event) => resetAnd(() => setCandidate(event.target.value))}
                  className="input h-11 w-full pl-9"
                  placeholder="Buscar participante"
                  inputMode="search"
                />
              </div>
            </label>
          </div>
        </section>

        <section className="overflow-hidden rounded-xl border border-border bg-card" aria-labelledby="attempts-title">
          <div className="flex items-center justify-between border-b border-border px-4 py-3">
            <div>
              <h2 id="attempts-title" className="text-sm font-semibold">Tentativas</h2>
              <p className="mt-1 text-xs text-muted-foreground">
                Página {(attemptsPage?.page ?? 0) + 1} de {Math.max(1, attemptsPage?.totalPages ?? 1)} · {attemptsPage?.totalElements ?? 0} registro(s)
              </p>
            </div>
          </div>

          {attemptsQuery.isLoading ? (
            <div className="px-4 py-12 text-center text-sm text-muted-foreground">
              Carregando tentativas...
            </div>
          ) : attemptsQuery.isError ? (
            <div className="px-4 py-12 text-center text-sm text-danger" role="alert">
              {attemptsQuery.error instanceof Error
                ? attemptsQuery.error.message
                : "Não foi possível carregar o monitoramento."}
            </div>
          ) : (
            <>
              <div className="grid gap-3 p-3 md:hidden">
                {attempts.map((attempt) => <AttemptCard key={attempt.attemptId} attempt={attempt} />)}
                {attempts.length === 0 && (
                  <p className="py-8 text-center text-sm text-muted-foreground">
                    Nenhuma tentativa encontrada para os filtros atuais.
                  </p>
                )}
              </div>
              <div className="hidden overflow-x-auto md:block">
                <table className="w-full min-w-[920px] text-left text-sm">
                  <thead className="border-b border-border bg-muted/40 text-[11px] uppercase text-muted-foreground">
                    <tr>
                      <th className="px-4 py-3 font-medium">Participante</th>
                      <th className="px-4 py-3 font-medium">Avaliação</th>
                      <th className="px-4 py-3 font-medium">Estado</th>
                      <th className="px-4 py-3 font-medium">Progresso</th>
                      <th className="px-4 py-3 font-medium">Tempo</th>
                      <th className="px-4 py-3 font-medium">Último sinal</th>
                      <th className="px-4 py-3 text-right font-medium">Ação</th>
                    </tr>
                  </thead>
                  <tbody>
                    {attempts.map((attempt) => <AttemptRow key={attempt.attemptId} attempt={attempt} />)}
                    {attempts.length === 0 && (
                      <tr>
                        <td colSpan={7} className="px-4 py-10 text-center text-muted-foreground">
                          Nenhuma tentativa encontrada para os filtros atuais.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </>
          )}

          <div className="flex items-center justify-between gap-2 border-t border-border px-3 py-3 sm:px-4">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={page <= 0 || attemptsQuery.isFetching}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
            >
              Anterior
            </Button>
            <span className="text-center text-xs text-muted-foreground">
              {attemptsPage?.size ?? 25} por página
            </span>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={!attemptsPage || page + 1 >= attemptsPage.totalPages || attemptsQuery.isFetching}
              onClick={() => setPage((current) => current + 1)}
            >
              Próxima
            </Button>
          </div>
        </section>
      </main>
    </AppShell>
  );
}

function IntegrationHealthPanel({
  integrations,
  loading,
  error,
  retryingDeliveries,
}: {
  integrations: IntegrationCenterItem[];
  loading: boolean;
  error: unknown;
  retryingDeliveries: number;
}) {
  return (
    <section className="rounded-xl border border-border bg-card" aria-labelledby="integration-health-title">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border p-4">
        <div>
          <h2 id="integration-health-title" className="text-sm font-semibold">Saúde das integrações</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            Estado configurado, última atividade autenticada e mensagens de erro dos provedores.
          </p>
        </div>
        <div className="flex items-center gap-3 text-xs text-muted-foreground">
          <span>{retryingDeliveries} entrega(s) aguardando tentativa</span>
          <Link to="/integrations" className="font-medium text-primary hover:underline">Configurar</Link>
        </div>
      </div>
      {loading ? (
        <div className="p-6 text-sm text-muted-foreground">Carregando integrações...</div>
      ) : error ? (
        <div className="p-6 text-sm text-danger" role="alert">
          {error instanceof Error ? error.message : "Não foi possível consultar as integrações."}
        </div>
      ) : (
        <div className="grid gap-3 p-4 lg:grid-cols-3">
          {integrations.map((integration) => (
            <IntegrationHealthCard key={integration.provider} integration={integration} />
          ))}
        </div>
      )}
    </section>
  );
}

function IntegrationHealthCard({ integration }: { integration: IntegrationCenterItem }) {
  const slug = { GUPY: "gupy", RECRUTEI: "recrutei", CUSTOM_API: "custom-api" }[integration.provider];
  const attention = integration.status === "ERRO" || integration.status === "PENDENTE";
  const Icon = integration.status === "CONECTADA" ? CheckCircle2 : attention ? AlertTriangle : PlugZap;
  return (
    <article className={cn("rounded-lg border p-4", attention ? "border-warning/40 bg-warning/5" : "border-border bg-background")}>
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2">
          <Icon className={cn("h-4 w-4 shrink-0", integration.status === "CONECTADA" ? "text-success" : attention ? "text-warning-foreground" : "text-muted-foreground")} />
          <h3 className="truncate text-sm font-semibold">{integration.name}</h3>
        </div>
        <IntegrationStatusBadge status={integration.status} />
      </div>
      <dl className="mt-4 grid gap-2 text-xs">
        <div>
          <dt className="text-muted-foreground">Última atividade</dt>
          <dd className="mt-0.5 font-medium">{formatDateTime(integration.lastSyncAt)}</dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Configurada em</dt>
          <dd className="mt-0.5 font-medium">{formatDateTime(integration.configuredAt)}</dd>
        </div>
      </dl>
      {integration.errorMessage && (
        <p className="mt-3 line-clamp-3 rounded-md bg-danger/10 p-2 text-xs text-danger" role="alert">
          {integration.errorMessage}
        </p>
      )}
      <Link
        to="/integrations/$provider"
        params={{ provider: slug }}
        className="mt-4 inline-flex min-h-10 items-center text-xs font-medium text-primary hover:underline"
      >
        Ver configuração e diagnóstico
      </Link>
    </article>
  );
}

function IntegrationStatusBadge({ status }: { status: IntegrationCenterStatus }) {
  const label: Record<IntegrationCenterStatus, string> = {
    CONECTADA: "Conectada",
    PENDENTE: "Pendente",
    ERRO: "Erro",
    DESATIVADA: "Desativada",
    NAO_CONFIGURADA: "Não configurada",
  };
  return (
    <span className={cn(
      "rounded-full border px-2 py-1 text-[10px] font-semibold uppercase tracking-wide",
      status === "CONECTADA" && "border-success/30 bg-success/10 text-success",
      status === "PENDENTE" && "border-warning/40 bg-warning/10 text-warning-foreground",
      status === "ERRO" && "border-danger/30 bg-danger/10 text-danger",
      (status === "DESATIVADA" || status === "NAO_CONFIGURADA") && "border-border bg-muted/40 text-muted-foreground",
    )}>
      {label[status]}
    </span>
  );
}

function AttemptCard({ attempt }: { attempt: MonitoringAttempt }) {
  return (
    <article className="rounded-lg border border-border bg-background p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="truncate font-medium">{attempt.candidateName}</h3>
          <p className="truncate text-xs text-muted-foreground">{attempt.candidateEmail}</p>
        </div>
        <AttemptStatusBadge attempt={attempt} />
      </div>
      <p className="mt-3 text-sm text-muted-foreground">
        {attempt.simulationName} · v{attempt.versionNumber}
      </p>
      <div className="mt-3" role="progressbar" aria-label="Progresso da tentativa" aria-valuemin={0} aria-valuemax={100} aria-valuenow={attempt.progressPercent}>
        <div className="flex justify-between text-xs text-muted-foreground">
          <span>{attempt.currentTurn}/{attempt.estimatedTurns} etapas</span>
          <span>{attempt.progressPercent}%</span>
        </div>
        <div className="mt-1 h-2 overflow-hidden rounded-full bg-muted">
          <div className="h-full rounded-full bg-primary" style={{ width: `${attempt.progressPercent}%` }} />
        </div>
      </div>
      <dl className="mt-3 grid grid-cols-2 gap-3 text-xs">
        <div><dt className="text-muted-foreground">Tempo</dt><dd className="mt-0.5 font-mono">{formatElapsed(attempt.elapsedSeconds)}</dd></div>
        <div><dt className="text-muted-foreground">Último sinal</dt><dd className="mt-0.5">{formatRelative(attempt.lastSignalAt)}</dd></div>
      </dl>
      {attempt.status === "completed" && (
        <Button asChild variant="outline" size="sm" className="mt-4 w-full">
          <Link to="/results/$attemptId" params={{ attemptId: attempt.attemptId }}>Ver resultado</Link>
        </Button>
      )}
    </article>
  );
}

function AttemptRow({ attempt }: { attempt: MonitoringAttempt }) {
  return (
    <tr className="border-b border-border last:border-0">
      <td className="px-4 py-3"><div className="font-medium">{attempt.candidateName}</div><div className="text-xs text-muted-foreground">{attempt.candidateEmail}</div></td>
      <td className="px-4 py-3 text-muted-foreground">{attempt.simulationName} · v{attempt.versionNumber}</td>
      <td className="px-4 py-3"><AttemptStatusBadge attempt={attempt} /></td>
      <td className="px-4 py-3">
        <div className="text-xs text-muted-foreground">{attempt.currentTurn}/{attempt.estimatedTurns} · {attempt.progressPercent}%</div>
        <div className="mt-1 h-1.5 w-28 overflow-hidden rounded-full bg-muted" role="progressbar" aria-label={`Progresso de ${attempt.candidateName}`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={attempt.progressPercent}>
          <div className="h-full rounded-full bg-primary" style={{ width: `${attempt.progressPercent}%` }} />
        </div>
      </td>
      <td className="px-4 py-3 font-mono text-xs text-muted-foreground">{formatElapsed(attempt.elapsedSeconds)}</td>
      <td className="px-4 py-3 text-xs text-muted-foreground">{formatRelative(attempt.lastSignalAt)}</td>
      <td className="px-4 py-3 text-right">
        {attempt.status === "completed" ? (
          <Button asChild variant="outline" size="sm"><Link to="/results/$attemptId" params={{ attemptId: attempt.attemptId }}>Ver resultado</Link></Button>
        ) : <span className="text-xs text-muted-foreground">Em acompanhamento</span>}
      </td>
    </tr>
  );
}

function AttemptStatusBadge({ attempt }: { attempt: MonitoringAttempt }) {
  const labels: Record<MonitoringAttemptStatus, string> = {
    notStarted: "Não iniciada",
    inProgress: attempt.active ? "Em andamento" : "Em andamento · sem sinal",
    completed: "Concluída",
    abandoned: "Abandonada",
    expired: "Expirada",
  };
  const warning = attempt.status === "abandoned" || attempt.status === "expired" || (attempt.status === "inProgress" && !attempt.active);
  const success = attempt.status === "completed" || (attempt.status === "inProgress" && attempt.active);
  return (
    <span className={cn(
      "inline-flex shrink-0 rounded-full border px-2 py-1 text-xs font-medium",
      warning && "border-danger/30 bg-danger/10 text-danger",
      success && "border-success/30 bg-success/10 text-success",
      !warning && !success && "border-border bg-muted/40 text-muted-foreground",
    )}>
      {labels[attempt.status]}
    </span>
  );
}

function Metric({ icon, label, value, warning = false }: { icon: React.ReactNode; label: string; value: number; warning?: boolean }) {
  return (
    <div className={cn("rounded-xl border bg-card p-4", warning ? "border-danger/30" : "border-border")}>
      <div className="flex items-center gap-2 text-xs text-muted-foreground">{icon}<span>{label}</span></div>
      <div className={cn("mt-2 text-2xl font-semibold tabular-nums", warning && "text-danger")}>{value.toLocaleString("pt-BR")}</div>
    </div>
  );
}

function formatElapsed(seconds: number) {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = seconds % 60;
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m ${rest}s`;
}

function formatRelative(value: string | null) {
  if (!value) return "Sem sinal";
  const seconds = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 1000));
  if (seconds < 60) return `${seconds}s atrás`;
  if (seconds < 3600) return `${Math.floor(seconds / 60)}min atrás`;
  return `${Math.floor(seconds / 3600)}h atrás`;
}

function formatDateTime(value: string | null) {
  if (!value) return "Ainda não registrada";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Data indisponível";
  return date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}
