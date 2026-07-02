import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import { Activity, CheckCircle2, Eye, RefreshCw, Send, XCircle } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Button } from "@/components/ui/button";
import {
  listCandidateLinks,
  listLiveAttempts,
  listResultDeliveries,
  listSimulations,
  type AttemptStatus,
  type CandidateLinkResponse,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/monitoramento")({
  head: () => ({
    meta: [
      { title: "Monitoramento - Práxis" },
      {
        name: "description",
        content: "Central operacional para acompanhar convites, tentativas e entregas.",
      },
    ],
  }),
  component: MonitoringPage,
});

type AttemptFilter = "todos" | "ativos" | "finalizadas" | "sem-sinal";

function MonitoringPage() {
  const [revealed, setRevealed] = useState(false);
  const [attemptFilter, setAttemptFilter] = useState<AttemptFilter>("todos");
  const [simulationFilter, setSimulationFilter] = useState<string>("todos");

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
  });
  const deliveriesQuery = useQuery({
    queryKey: ["result-deliveries"],
    queryFn: () => listResultDeliveries(),
  });
  const liveAttemptsQuery = useQuery({
    queryKey: ["live-attempts"],
    queryFn: listLiveAttempts,
  });
  const candidateLinksQuery = useQuery({
    queryKey: ["candidate-links"],
    queryFn: () => listCandidateLinks(),
  });

  const simulations = useMemo(
    () => (simulationsQuery.data ?? []).filter((simulation) => simulation.status === "published"),
    [simulationsQuery.data],
  );
  const deliveries = deliveriesQuery.data ?? [];
  const liveAttempts = liveAttemptsQuery.data ?? [];
  const candidateLinks = candidateLinksQuery.data ?? [];
  const failedDeliveries = deliveries.filter((delivery) => delivery.status === "dlq").length;
  const totals = buildTotals(candidateLinks);
  const filteredAttempts = liveAttempts.filter((attempt) => {
    const openAttempt = attempt.status === "inProgress" || attempt.status === "paused";
    if (attemptFilter === "ativos" && !attempt.active) return false;
    if (attemptFilter === "finalizadas" && attempt.status !== "completed") return false;
    if (attemptFilter === "sem-sinal" && (!openAttempt || attempt.active)) return false;
    if (simulationFilter !== "todos" && attempt.simulationId !== simulationFilter) return false;
    return true;
  });

  return (
    <AppShell>
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-normal text-foreground">
              Monitoramento
            </div>
            <h1 className="mt-1 font-display text-3xl leading-tight text-foreground">
              Central operacional
            </h1>
            <p className="mt-3 text-sm leading-6 text-muted-foreground">
              Convites, tentativas em andamento, finalizadas e falhas operacionais que a automacao
              nao conseguiu resolver sozinha.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-8 min-h-8 gap-1.5 bg-card text-xs"
              onClick={() => setRevealed((current) => !current)}
            >
              <Eye className="h-3.5 w-3.5" />
              {revealed ? "Ocultar nomes" : "Revelar nomes"}
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-8 min-h-8 gap-1.5 bg-card text-xs"
              onClick={() => {
                void simulationsQuery.refetch();
                void deliveriesQuery.refetch();
                void liveAttemptsQuery.refetch();
                void candidateLinksQuery.refetch();
              }}
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Atualizar
            </Button>
          </div>
        </section>

        <section className="grid grid-cols-2 gap-3 md:grid-cols-4">
          <MetricCard
            icon={<Send className="h-4 w-4" />}
            label="Convites enviados"
            value={totals.invites}
            detail="Total historico"
            tone="ok"
          />
          <MetricCard
            icon={<Activity className="h-4 w-4" />}
            label="Tentativas iniciadas"
            value={totals.started}
            detail={`${totals.adherence}% de adesao`}
            tone="ok"
          />
          <MetricCard
            icon={<CheckCircle2 className="h-4 w-4" />}
            label="Conclusoes"
            value={totals.completed}
            detail={`${totals.completion}% de finalizacao`}
            tone="ok"
          />
          <MetricCard
            icon={<XCircle className="h-4 w-4" />}
            label="Falhas de entrega"
            value={failedDeliveries}
            detail="DLQ pendente"
            tone={failedDeliveries > 0 ? "danger" : "ok"}
          />
        </section>

        <div className="space-y-5">
          <section className="overflow-hidden rounded-xl border border-border bg-card">
            <div className="flex flex-col gap-3 border-b border-border px-4 py-4 md:flex-row md:items-center md:justify-between">
              <div>
                <div className="flex items-center gap-2 text-sm font-semibold">
                  <span className="h-2 w-2 rounded-full bg-success" />
                  Tentativas monitoradas
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  {filteredAttempts.length} de {liveAttempts.length} participacoes acompanhadas
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <select
                  value={simulationFilter}
                  onChange={(event) => setSimulationFilter(event.target.value)}
                  className="h-8 min-h-8 rounded-md border border-border bg-background px-3 text-xs"
                >
                  <option value="todos">Todas as avaliações</option>
                  {simulations.map((simulation) => (
                    <option key={simulation.id} value={simulation.id}>
                      {simulation.name}
                    </option>
                  ))}
                </select>
                <AttemptToggle value={attemptFilter} onChange={setAttemptFilter} />
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[760px] text-left text-sm">
                <thead className="border-b border-border bg-muted/50 text-[11px] uppercase text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3 font-medium">Participante</th>
                    <th className="px-4 py-3 font-medium">Avaliacao</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium">Etapa</th>
                    <th className="px-4 py-3 font-medium">Tempo</th>
                    <th className="px-4 py-3 font-medium">Ultimo sinal</th>
                    <th className="px-4 py-3 text-right font-medium">Acao</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAttempts.map((attempt) => (
                    <tr key={attempt.attemptId} className="border-b border-border last:border-0">
                      <td className="px-4 py-3 font-medium">
                        {revealed ? attempt.candidateName : maskName(attempt.candidateName)}
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">
                        {attempt.simulationName} - v{attempt.versionNumber}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={attempt.status} active={attempt.active} />
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">
                        {attempt.currentTurn}/{attempt.estimatedTurns}
                      </td>
                      <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                        {formatElapsed(attempt.elapsedSeconds)}
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">
                        {formatRelativeTime(attempt.lastSignalAt)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <Button
                          asChild
                          variant="outline"
                          size="sm"
                          className="h-8 min-h-8 bg-background text-xs"
                        >
                          <Link
                            to="/compliance"
                            search={{
                              simulationId: attempt.simulationId,
                              versionNumber: attempt.versionNumber,
                            }}
                          >
                            Acompanhar
                          </Link>
                        </Button>
                      </td>
                    </tr>
                  ))}
                  {filteredAttempts.length === 0 && (
                    <tr>
                      <td
                        colSpan={7}
                        className="px-4 py-8 text-center text-sm text-muted-foreground"
                      >
                        Nenhuma tentativa encontrada para os filtros atuais.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <section className="overflow-hidden rounded-xl border border-border bg-card">
            <div className="border-b border-border px-4 py-4">
              <h2 className="text-sm font-semibold">Avaliações no ar</h2>
              <p className="mt-1 text-xs text-muted-foreground">
                Versoes publicadas com dados de tentativas
              </p>
            </div>
            <div>
              {simulations.slice(0, 4).map((simulation) => (
                <ProductionSimulation key={simulation.id} simulation={simulation} />
              ))}
              {simulations.length === 0 && (
                <div className="px-4 py-8 text-sm text-muted-foreground">
                  Nenhuma avaliação publicada encontrada.
                </div>
              )}
            </div>
          </section>
        </div>
      </div>
    </AppShell>
  );
}

function AttemptToggle({
  value,
  onChange,
}: {
  value: AttemptFilter;
  onChange: (value: AttemptFilter) => void;
}) {
  const items: Array<[AttemptFilter, string]> = [
    ["todos", "Todos"],
    ["ativos", "Ativos"],
    ["finalizadas", "Finalizadas"],
    ["sem-sinal", "Sem sinal"],
  ];
  return (
    <div className="inline-flex h-8 min-h-8 rounded-md border border-border bg-card p-0.5 text-xs">
      {items.map(([id, label]) => (
        <button
          key={id}
          type="button"
          onClick={() => onChange(id)}
          className={cn(
            "min-h-0 rounded px-2 py-0.5 transition",
            value === id
              ? "bg-primary text-primary-foreground"
              : "text-muted-foreground hover:bg-accent",
          )}
        >
          {label}
        </button>
      ))}
    </div>
  );
}

function StatusBadge({ status, active }: { status: AttemptStatus; active: boolean }) {
  const label = attemptStatusLabel(status, active);
  const tone =
    status === "completed"
      ? "border-success/30 bg-success/10 text-success"
      : active
        ? "border-primary/30 bg-primary/10 text-primary"
        : "border-warning/30 bg-warning/10 text-warning";

  return (
    <span className={cn("inline-flex rounded-full border px-2 py-0.5 text-[11px]", tone)}>
      {label}
    </span>
  );
}

function attemptStatusLabel(status: AttemptStatus, active: boolean) {
  if (status === "completed") return "Finalizada";
  if (status === "paused") return "Pausada";
  if (status === "inProgress" && active) return "Ativa";
  if (status === "inProgress") return "Sem sinal";
  if (status === "abandoned") return "Abandonada";
  if (status === "expired") return "Expirada";
  if (status === "failed") return "Falhou";
  return "Nao iniciada";
}

function MetricCard({
  icon,
  label,
  value,
  detail,
  tone,
}: {
  icon: ReactNode;
  label: string;
  value: number;
  detail?: string;
  tone: "ok" | "danger";
}) {
  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <div className="flex items-center gap-2 text-[11px] font-medium uppercase text-muted-foreground">
        {icon}
        {label}
      </div>
      <div className="mt-4 flex items-end gap-2">
        <div className="font-display text-3xl leading-none tabular-nums">{value}</div>
      </div>
      {detail && (
        <div
          className={cn(
            "mt-2 text-xs",
            tone === "danger" ? "text-danger" : "text-muted-foreground",
          )}
        >
          {detail}
        </div>
      )}
    </div>
  );
}

function ProductionSimulation({ simulation }: { simulation: SimulationSummaryResponse }) {
  const completion = Math.round(simulation.completionRatePercent || 0);
  const volume = simulation.attemptsCreated || 0;
  const pending = Math.max(0, 100 - completion);

  return (
    <article className="border-b border-border px-4 py-4 last:border-0">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="truncate text-sm font-semibold">{simulation.name}</h3>
          <div className="mt-1 text-[11px] text-muted-foreground">v{simulation.versionNumber}</div>
        </div>
        <Link
          to="/compliance"
          search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
          className="shrink-0 text-[11px] text-primary hover:underline"
        >
          Compliance -&gt;
        </Link>
      </div>
      <div className="mt-3 grid grid-cols-3 gap-3 text-[11px] uppercase text-muted-foreground">
        <div>
          Volume
          <strong className="mt-1 block text-sm text-foreground">{volume}</strong>
        </div>
        <div>
          Conclusao
          <strong className="mt-1 block text-sm text-foreground">{completion}%</strong>
        </div>
        <div>
          Concluidas
          <strong className="mt-1 block text-sm text-foreground">
            {simulation.attemptsCompleted}
          </strong>
        </div>
      </div>
      <div className="mt-3 flex h-1.5 overflow-hidden rounded-full bg-muted">
        <span className="bg-success" style={{ width: `${completion}%` }} />
        <span className="bg-muted-foreground/20" style={{ width: `${pending}%` }} />
      </div>
      <div className="mt-2 grid grid-cols-2 text-[10px] text-muted-foreground">
        <span>Finalizacao {completion}%</span>
        <span className="text-right">Em aberto {pending}%</span>
      </div>
    </article>
  );
}

function buildTotals(candidateLinks: CandidateLinkResponse[]) {
  // "Convites enviados" = links de candidato efetivamente gerados (fonte da verdade).
  const invites = candidateLinks.length;
  // Tentativas iniciadas = candidatos que sairam do estado "notStarted".
  const started = candidateLinks.filter((link) => link.status !== "notStarted").length;
  const completed = candidateLinks.filter((link) => link.status === "completed").length;
  const completion = Math.round((completed / Math.max(started, 1)) * 100);
  return {
    invites,
    started,
    completed,
    adherence: Math.round((started / Math.max(invites, 1)) * 100),
    completion,
  };
}

function maskName(name: string) {
  const [first = "", second = ""] = name.split(" ");
  return `${first.charAt(0)}${"*".repeat(Math.max(4, first.length - 1))} ${second.charAt(0)}.`;
}

function formatElapsed(totalSeconds: number) {
  const safeSeconds = Math.max(0, totalSeconds);
  const minutes = Math.floor(safeSeconds / 60);
  const seconds = safeSeconds % 60;
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  if (hours > 0) {
    return `${String(hours).padStart(2, "0")}:${String(remainingMinutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
  }
  return `${String(remainingMinutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function formatRelativeTime(value: string) {
  const timestamp = new Date(value).getTime();
  if (Number.isNaN(timestamp)) {
    return "-";
  }
  const seconds = Math.max(0, Math.round((Date.now() - timestamp) / 1000));
  if (seconds < 60) {
    return `ha ${seconds}s`;
  }
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) {
    return `ha ${minutes} min`;
  }
  const hours = Math.round(minutes / 60);
  return `ha ${hours} h`;
}
