import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  Link2,
  RefreshCw,
  Search,
  Send,
  UserRoundSearch,
} from "lucide-react";
import { useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { listSimulations } from "@/lib/api/praxis";
import {
  searchMonitoringAttempts,
  type MonitoringAttempt,
  type MonitoringAttemptStatus,
} from "@/lib/api/monitoring";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/participacoes")({
  head: () => ({
    meta: [
      { title: "Participações - Práxis" },
      {
        name: "description",
        content: "Convide participantes e acompanhe o andamento das avaliações em um único lugar.",
      },
    ],
  }),
  component: ParticipacoesPage,
});

type ProcessFilter = "all" | "waiting" | "active" | "completed" | "attention";

const processFilters: Array<{ value: ProcessFilter; label: string }> = [
  { value: "all", label: "Todas" },
  { value: "waiting", label: "Aguardando início" },
  { value: "active", label: "Em andamento" },
  { value: "completed", label: "Concluídas" },
  { value: "attention", label: "Com problema" },
];

function ParticipacoesPage() {
  const [page, setPage] = useState(0);
  const [processFilter, setProcessFilter] = useState<ProcessFilter>("all");
  const [simulationId, setSimulationId] = useState("");
  const [candidate, setCandidate] = useState("");

  const attemptsQuery = useQuery({
    queryKey: ["participations", page, simulationId, candidate],
    queryFn: () =>
      searchMonitoringAttempts({
        page,
        size: 25,
        status: "all",
        simulationId: simulationId || undefined,
        candidate: candidate.trim() || undefined,
      }),
    retry: false,
    refetchInterval: 30_000,
  });
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });

  const attemptsPage = attemptsQuery.data;
  const attempts = useMemo(
    () => (attemptsPage?.items ?? []).filter((attempt) => matchesProcessFilter(attempt, processFilter)),
    [attemptsPage?.items, processFilter],
  );
  const simulations = (simulationsQuery.data ?? []).filter(
    (simulation) => simulation.status === "published" || simulation.livePublishedVersionNumber != null,
  );

  function resetPage(action: () => void) {
    setPage(0);
    action();
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Participações
            </div>
            <h1 className="mt-1 font-display text-3xl">Convites e acompanhamento</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Este é o ponto único para acompanhar pessoas. Crie convites pelas jornadas e use o
              envio isolado apenas quando a avaliação não fizer parte de um processo.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button asChild className="gap-2">
              <Link to="/jornadas">
                <Send className="h-4 w-4" />
                Convidar por jornada
              </Link>
            </Button>
            <Button asChild variant="outline" className="gap-2 bg-card">
              <Link to="/enviar-link">
                <Link2 className="h-4 w-4" />
                Avaliação isolada
              </Link>
            </Button>
            <Button
              type="button"
              variant="outline"
              className="gap-2 bg-card"
              disabled={attemptsQuery.isFetching}
              onClick={() => void attemptsQuery.refetch()}
            >
              <RefreshCw className={cn("h-4 w-4", attemptsQuery.isFetching && "animate-spin")} />
              Atualizar
            </Button>
          </div>
        </header>

        <section className="rounded-xl border border-border bg-card p-4" aria-label="Filtros das participações">
          <div className="flex flex-wrap gap-2" role="tablist" aria-label="Situação do processo">
            {processFilters.map((filter) => (
              <button
                key={filter.value}
                type="button"
                role="tab"
                aria-selected={processFilter === filter.value}
                onClick={() => setProcessFilter(filter.value)}
                className={cn(
                  "min-h-10 rounded-md border px-3 py-2 text-sm font-medium",
                  processFilter === filter.value
                    ? "border-primary/40 bg-primary/10 text-primary"
                    : "border-border bg-background hover:bg-accent",
                )}
              >
                {filter.label}
              </button>
            ))}
          </div>

          <div className="mt-4 grid gap-3 md:grid-cols-2">
            <label className="space-y-1 text-xs font-medium text-muted-foreground">
              Avaliação
              <select
                value={simulationId}
                onChange={(event) => resetPage(() => setSimulationId(event.target.value))}
                className="input h-11 w-full"
              >
                <option value="">Todas as avaliações</option>
                {simulations.map((simulation) => (
                  <option key={simulation.id} value={simulation.id}>
                    {simulation.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="space-y-1 text-xs font-medium text-muted-foreground">
              Nome ou e-mail
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-3.5 h-4 w-4 text-muted-foreground" />
                <input
                  value={candidate}
                  onChange={(event) => resetPage(() => setCandidate(event.target.value))}
                  className="input h-11 w-full pl-9"
                  placeholder="Buscar participante"
                  inputMode="search"
                />
              </div>
            </label>
          </div>
        </section>

        {attemptsQuery.isError ? (
          <StateBanner
            tone="danger"
            title="Não foi possível carregar as participações"
            action={
              <button
                type="button"
                onClick={() => void attemptsQuery.refetch()}
                className="rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium"
              >
                Tentar novamente
              </button>
            }
          >
            {attemptsQuery.error instanceof Error ? attemptsQuery.error.message : "Tente novamente."}
          </StateBanner>
        ) : attemptsQuery.isLoading ? (
          <section className="rounded-xl border border-border bg-card px-4 py-12 text-center text-sm text-muted-foreground">
            Carregando participações...
          </section>
        ) : attempts.length === 0 ? (
          <EmptyState
            title="Nenhuma participação encontrada"
            description={
              processFilter === "all"
                ? "Crie o primeiro convite por uma jornada publicada."
                : "Não há registros nesta situação para os filtros atuais."
            }
            actions={
              <Button asChild>
                <Link to="/jornadas">Abrir jornadas</Link>
              </Button>
            }
          />
        ) : (
          <ParticipationList attempts={attempts} />
        )}

        <div className="flex items-center justify-between gap-3">
          <Button
            type="button"
            variant="outline"
            disabled={page <= 0 || attemptsQuery.isFetching}
            onClick={() => setPage((current) => Math.max(0, current - 1))}
          >
            Anterior
          </Button>
          <span className="text-xs text-muted-foreground">
            Página {(attemptsPage?.page ?? 0) + 1} de {Math.max(1, attemptsPage?.totalPages ?? 1)}
          </span>
          <Button
            type="button"
            variant="outline"
            disabled={!attemptsPage || page + 1 >= attemptsPage.totalPages || attemptsQuery.isFetching}
            onClick={() => setPage((current) => current + 1)}
          >
            Próxima
          </Button>
        </div>
      </main>
    </AppShell>
  );
}

function ParticipationList({ attempts }: { attempts: MonitoringAttempt[] }) {
  return (
    <section className="overflow-hidden rounded-xl border border-border bg-card">
      <div className="grid gap-3 p-3 md:hidden">
        {attempts.map((attempt) => (
          <ParticipationCard key={attempt.attemptId} attempt={attempt} />
        ))}
      </div>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[900px] text-left text-sm">
          <thead className="border-b border-border bg-muted/40 text-[11px] uppercase text-muted-foreground">
            <tr>
              <th className="px-4 py-3 font-medium">Participante</th>
              <th className="px-4 py-3 font-medium">Avaliação</th>
              <th className="px-4 py-3 font-medium">Situação</th>
              <th className="px-4 py-3 font-medium">Progresso</th>
              <th className="px-4 py-3 font-medium">Última atividade</th>
              <th className="px-4 py-3 text-right font-medium">Ação</th>
            </tr>
          </thead>
          <tbody>
            {attempts.map((attempt) => (
              <tr key={attempt.attemptId} className="border-b border-border last:border-0 hover:bg-accent/30">
                <td className="px-4 py-3">
                  <div className="font-medium">{attempt.candidateName}</div>
                  <div className="text-xs text-muted-foreground">{attempt.candidateEmail}</div>
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {attempt.simulationName} · v{attempt.versionNumber}
                </td>
                <td className="px-4 py-3">
                  <ParticipationStatus attempt={attempt} />
                </td>
                <td className="px-4 py-3">
                  <div className="text-xs text-muted-foreground">
                    {attempt.currentTurn}/{attempt.estimatedTurns} · {attempt.progressPercent}%
                  </div>
                  <div className="mt-1 h-1.5 w-28 overflow-hidden rounded-full bg-muted">
                    <div className="h-full rounded-full bg-primary" style={{ width: `${attempt.progressPercent}%` }} />
                  </div>
                </td>
                <td className="px-4 py-3 text-xs text-muted-foreground">
                  {formatRelative(attempt.lastSignalAt)}
                </td>
                <td className="px-4 py-3 text-right">
                  <ParticipationAction attempt={attempt} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function ParticipationCard({ attempt }: { attempt: MonitoringAttempt }) {
  return (
    <article className="rounded-lg border border-border bg-background p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="truncate font-medium">{attempt.candidateName}</h2>
          <p className="truncate text-xs text-muted-foreground">{attempt.candidateEmail}</p>
        </div>
        <ParticipationStatus attempt={attempt} />
      </div>
      <p className="mt-3 text-sm text-muted-foreground">
        {attempt.simulationName} · v{attempt.versionNumber}
      </p>
      <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
        <span>{attempt.progressPercent}% concluído</span>
        <span>{formatRelative(attempt.lastSignalAt)}</span>
      </div>
      <div className="mt-2 h-2 overflow-hidden rounded-full bg-muted">
        <div className="h-full rounded-full bg-primary" style={{ width: `${attempt.progressPercent}%` }} />
      </div>
      <div className="mt-4">
        <ParticipationAction attempt={attempt} fullWidth />
      </div>
    </article>
  );
}

function ParticipationAction({ attempt, fullWidth = false }: { attempt: MonitoringAttempt; fullWidth?: boolean }) {
  if (attempt.status === "completed") {
    return (
      <Button asChild variant="outline" size="sm" className={cn(fullWidth && "w-full")}>
        <Link to="/results/$attemptId" params={{ attemptId: attempt.attemptId }}>
          Analisar resultado
        </Link>
      </Button>
    );
  }
  return (
    <Button asChild variant="outline" size="sm" className={cn(fullWidth && "w-full")}>
      <Link to="/enviar-link">Gerenciar convite</Link>
    </Button>
  );
}

function ParticipationStatus({ attempt }: { attempt: MonitoringAttempt }) {
  const meta = statusMeta(attempt.status, attempt.active);
  const Icon = meta.icon;
  return (
    <span className={cn("inline-flex items-center gap-1.5 rounded-full border px-2 py-1 text-[11px] font-medium", meta.className)}>
      <Icon className="h-3.5 w-3.5" />
      {meta.label}
    </span>
  );
}

function statusMeta(status: MonitoringAttemptStatus, active: boolean) {
  if (status === "inProgress" && !active) {
    return {
      label: "Sem atividade recente",
      icon: AlertTriangle,
      className: "border-warning/40 bg-warning/10 text-warning-foreground",
    };
  }
  return (
    {
      notStarted: {
        label: "Aguardando início",
        icon: Clock3,
        className: "border-border bg-muted text-foreground",
      },
      inProgress: {
        label: "Em andamento",
        icon: UserRoundSearch,
        className: "border-primary/30 bg-primary/10 text-primary",
      },
      completed: {
        label: "Concluída",
        icon: CheckCircle2,
        className: "border-success/30 bg-success/10 text-success",
      },
      abandoned: {
        label: "Abandonada",
        icon: AlertTriangle,
        className: "border-danger/30 bg-danger/10 text-danger",
      },
      expired: {
        label: "Expirada",
        icon: AlertTriangle,
        className: "border-warning/40 bg-warning/10 text-warning-foreground",
      },
    } satisfies Record<
      MonitoringAttemptStatus,
      { label: string; icon: typeof Clock3; className: string }
    >
  )[status];
}

function matchesProcessFilter(attempt: MonitoringAttempt, filter: ProcessFilter) {
  if (filter === "all") return true;
  if (filter === "waiting") return attempt.status === "notStarted";
  if (filter === "active") return attempt.status === "inProgress" && attempt.active;
  if (filter === "completed") return attempt.status === "completed";
  return attempt.status === "abandoned" || attempt.status === "expired" || (attempt.status === "inProgress" && !attempt.active);
}

function formatRelative(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Sem registro";
  const seconds = Math.max(0, Math.round((Date.now() - date.getTime()) / 1000));
  if (seconds < 60) return "Agora";
  if (seconds < 3600) return `Há ${Math.floor(seconds / 60)} min`;
  if (seconds < 86_400) return `Há ${Math.floor(seconds / 3600)} h`;
  return new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" }).format(date);
}
