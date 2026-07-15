import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import {
  Activity,
  Bell,
  CheckCircle2,
  Clock3,
  Eye,
  RefreshCw,
  Send,
  UserRoundSearch,
  XCircle,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Button } from "@/components/ui/button";
import { listResultDeliveries, listSimulations } from "@/lib/api/praxis";
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
        content: "Central operacional paginada para acompanhar todas as tentativas e entregas.",
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
  const [revealed, setRevealed] = useState(false);

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
  });
  const simulationsQuery = useQuery({ queryKey: ["simulations"], queryFn: listSimulations });
  const deliveriesQuery = useQuery({
    queryKey: ["result-deliveries"],
    queryFn: () => listResultDeliveries(),
  });
  const notificationsQuery = useQuery({
    queryKey: ["notifications"],
    queryFn: listNotifications,
  });

  const attemptsPage = attemptsQuery.data;
  const attempts = attemptsPage?.items ?? [];
  const simulations = (simulationsQuery.data ?? []).filter(
    (simulation) => simulation.status === "published",
  );
  const failedDeliveries = (deliveriesQuery.data ?? []).filter(
    (delivery) => delivery.status === "dlq",
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

  function resetAnd(action: () => void) {
    setPage(0);
    action();
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
              Consulte todas as tentativas com paginação e filtros. Nenhum estado é omitido e o
              total exibido vem diretamente do banco de dados.
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setRevealed((current) => !current)}
            >
              <Eye className="mr-2 h-4 w-4" />
              {revealed ? "Ocultar nomes" : "Revelar nomes"}
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => attemptsQuery.refetch()}
              disabled={attemptsQuery.isFetching}
            >
              <RefreshCw className={cn("mr-2 h-4 w-4", attemptsQuery.isFetching && "animate-spin")} />
              Atualizar
            </Button>
          </div>
        </header>

        <section className="grid grid-cols-2 gap-3 lg:grid-cols-5">
          <Metric icon={<Send className="h-4 w-4" />} label="Total encontrado" value={attemptsPage?.totalElements ?? 0} />
          <Metric icon={<Activity className="h-4 w-4" />} label="Ativas nesta página" value={activeAttempts} />
          <Metric icon={<Clock3 className="h-4 w-4" />} label="Sem sinal nesta página" value={staleAttempts} warning={staleAttempts > 0} />
          <Metric icon={<Bell className="h-4 w-4" />} label="Notificações não lidas" value={unreadNotifications} warning={unreadNotifications > 0} />
          <Metric icon={<XCircle className="h-4 w-4" />} label="Entregas em DLQ" value={failedDeliveries} warning={failedDeliveries > 0} />
        </section>

        <section className="rounded-xl border border-border bg-card p-4">
          <div className="grid gap-3 md:grid-cols-3">
            <label className="space-y-1 text-xs font-medium text-muted-foreground">
              Estado
              <select
                value={status}
                onChange={(event) => resetAnd(() => setStatus(event.target.value as StatusFilter))}
                className="input h-10 w-full"
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
                className="input h-10 w-full"
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
                <UserRoundSearch className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                <input
                  value={candidate}
                  onChange={(event) => resetAnd(() => setCandidate(event.target.value))}
                  className="input h-10 w-full pl-9"
                  placeholder="Buscar participante"
                />
              </div>
            </label>
          </div>
        </section>

        <section className="overflow-hidden rounded-xl border border-border bg-card">
          <div className="flex items-center justify-between border-b border-border px-4 py-3">
            <div>
              <h2 className="text-sm font-semibold">Tentativas</h2>
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
            <div className="px-4 py-12 text-center text-sm text-danger">
              {attemptsQuery.error instanceof Error
                ? attemptsQuery.error.message
                : "Não foi possível carregar o monitoramento."}
            </div>
          ) : (
            <div className="overflow-x-auto">
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
                  {attempts.map((attempt) => (
                    <AttemptRow key={attempt.attemptId} attempt={attempt} revealed={revealed} />
                  ))}
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
          )}

          <div className="flex items-center justify-between border-t border-border px-4 py-3">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={page <= 0 || attemptsQuery.isFetching}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
            >
              Anterior
            </Button>
            <span className="text-xs text-muted-foreground">
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

function AttemptRow({ attempt, revealed }: { attempt: MonitoringAttempt; revealed: boolean }) {
  return (
    <tr className="border-b border-border last:border-0">
      <td className="px-4 py-3">
        <div className="font-medium">{revealed ? attempt.candidateName : maskName(attempt.candidateName)}</div>
        <div className="text-xs text-muted-foreground">{revealed ? attempt.candidateEmail : "dados ocultos"}</div>
      </td>
      <td className="px-4 py-3 text-muted-foreground">
        {attempt.simulationName} · v{attempt.versionNumber}
      </td>
      <td className="px-4 py-3"><AttemptStatusBadge attempt={attempt} /></td>
      <td className="px-4 py-3">
        <div className="text-xs text-muted-foreground">
          {attempt.currentTurn}/{attempt.estimatedTurns} · {attempt.progressPercent}%
        </div>
        <div className="mt-1 h-1.5 w-28 overflow-hidden rounded-full bg-muted">
          <div className="h-full rounded-full bg-primary" style={{ width: `${attempt.progressPercent}%` }} />
        </div>
      </td>
      <td className="px-4 py-3 font-mono text-xs text-muted-foreground">{formatElapsed(attempt.elapsedSeconds)}</td>
      <td className="px-4 py-3 text-xs text-muted-foreground">{formatRelative(attempt.lastSignalAt)}</td>
      <td className="px-4 py-3 text-right">
        {attempt.status === "completed" ? (
          <Button asChild variant="outline" size="sm">
            <Link to="/results/$attemptId" params={{ attemptId: attempt.attemptId }}>Ver resultado</Link>
          </Button>
        ) : (
          <span className="text-xs text-muted-foreground">Em acompanhamento</span>
        )}
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
      "inline-flex rounded-full border px-2 py-1 text-xs font-medium",
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
      <div className={cn("flex items-center gap-2 text-xs", warning ? "text-danger" : "text-muted-foreground")}>{icon}{label}</div>
      <div className="mt-2 text-2xl font-semibold">{value}</div>
    </div>
  );
}

function maskName(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  return parts.map((part) => `${part.charAt(0)}${"•".repeat(Math.max(2, Math.min(6, part.length - 1)))}`).join(" ");
}

function formatElapsed(seconds: number) {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (hours > 0) return `${hours}h ${minutes}min`;
  return `${minutes}min`;
}

function formatRelative(value: string) {
  const seconds = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 1000));
  if (seconds < 60) return "agora";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `há ${minutes} min`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `há ${hours} h`;
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(new Date(value));
}
