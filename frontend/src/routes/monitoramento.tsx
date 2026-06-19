import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import type { CSSProperties, ReactNode } from "react";
import { useMemo, useState } from "react";
import {
  ArrowDown,
  ArrowUp,
  CheckCircle2,
  Eye,
  RefreshCw,
  Send,
  TrendingUp,
  XCircle,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Button } from "@/components/ui/button";
import {
  listResultDeliveries,
  listSimulations,
  type ResultDeliveryResponse,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/monitoramento")({
  head: () => ({
    meta: [
      { title: "Monitoramento - Praxis" },
      {
        name: "description",
        content: "Central operacional para acompanhar convites, tentativas e entregas.",
      },
    ],
  }),
  component: MonitoringPage,
});

type TimeWindow = "24h" | "7d";
type AttemptFilter = "todos" | "ativos" | "sem-sinal";

const fallbackSimulations: SimulationSummaryResponse[] = [
  {
    id: "dev-jr",
    name: "Desenvolvedor de Software Junior",
    description: "Avaliacao situacional para devs em inicio de carreira.",
    versionNumber: 3,
    status: "published",
    updatedAt: new Date().toISOString(),
    competencies: ["Raciocinio", "Comunicacao"],
    attemptsCreated: 84,
    attemptsCompleted: 66,
    completionRatePercent: 79,
  },
  {
    id: "n2-atendimento",
    name: "Analista de Atendimento N2",
    description: "Casos de suporte e priorizacao operacional.",
    versionNumber: 1,
    status: "published",
    updatedAt: new Date().toISOString(),
    competencies: ["Priorizacao", "Clareza"],
    attemptsCreated: 41,
    attemptsCompleted: 36,
    completionRatePercent: 88,
  },
];

const liveAttempts = [
  {
    candidate: "Mariana Silva",
    simulation: "Dev Jr - v3",
    turn: "2/3",
    elapsed: "08:42",
    signal: "ha 12s",
    active: true,
  },
  {
    candidate: "Tiago Pereira",
    simulation: "Dev Jr - v3",
    turn: "1/3",
    elapsed: "02:11",
    signal: "ha 4s",
    active: true,
  },
  {
    candidate: "Patricia Alves",
    simulation: "Dev Jr - v3",
    turn: "2/3",
    elapsed: "14:03",
    signal: "ha 11 min",
    active: false,
  },
  {
    candidate: "Lucas Andrade",
    simulation: "N2 - v1",
    turn: "3/3",
    elapsed: "19:28",
    signal: "ha 38s",
    active: true,
  },
];

function MonitoringPage() {
  const [window, setWindow] = useState<TimeWindow>("24h");
  const [revealed, setRevealed] = useState(false);
  const [attemptFilter, setAttemptFilter] = useState<AttemptFilter>("todos");

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
  });
  const deliveriesQuery = useQuery({
    queryKey: ["result-deliveries"],
    queryFn: () => listResultDeliveries(),
  });

  const simulations = useMemo(
    () =>
      (simulationsQuery.data?.length ? simulationsQuery.data : fallbackSimulations).filter(
        (simulation) => simulation.status === "published",
      ),
    [simulationsQuery.data],
  );
  const deliveries = deliveriesQuery.data ?? [];
  const failedDeliveries =
    deliveries.filter((delivery) => delivery.status === "dlq").length || fallbackFailed(deliveries);
  const totals = buildTotals(simulations, deliveries, window);
  const filteredAttempts = liveAttempts.filter((attempt) => {
    if (attemptFilter === "ativos") return attempt.active;
    if (attemptFilter === "sem-sinal") return !attempt.active;
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
              Central operacional ao vivo
            </h1>
            <p className="mt-3 text-sm leading-6 text-muted-foreground">
              O que esta acontecendo agora: convites, tentativas em andamento e falhas operacionais
              que a automacao nao conseguiu resolver sozinha.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <WindowToggle value={window} onChange={setWindow} />
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
            delta={window === "24h" ? 12 : 34}
            tone="ok"
          />
          <MetricCard
            icon={<TrendingUp className="h-4 w-4" />}
            label="Tentativas iniciadas"
            value={totals.started}
            delta={window === "24h" ? 8 : 21}
            detail={`${totals.adherence}% de adesao`}
            tone="ok"
          />
          <MetricCard
            icon={<CheckCircle2 className="h-4 w-4" />}
            label="Conclusoes"
            value={totals.completed}
            delta={window === "24h" ? -3 : 9}
            detail={`${totals.completion}% de finalizacao`}
            tone={window === "24h" ? "danger" : "ok"}
          />
          <MetricCard
            icon={<XCircle className="h-4 w-4" />}
            label="Falhas de entrega"
            value={failedDeliveries}
            delta={failedDeliveries > 0 ? 1 : 0}
            tone={failedDeliveries > 0 ? "danger" : "ok"}
          />
        </section>

        <div className="grid gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(320px,1fr)]">
          <section className="overflow-hidden rounded-xl border border-border bg-card">
            <div className="flex flex-col gap-3 border-b border-border px-4 py-4 md:flex-row md:items-center md:justify-between">
              <div>
                <div className="flex items-center gap-2 text-sm font-semibold">
                  <span className="h-2 w-2 rounded-full bg-success" />
                  Tentativas em andamento
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  {filteredAttempts.length} de {liveAttempts.length} candidatos fazendo o teste
                  agora
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <select className="h-8 min-h-8 rounded-md border border-border bg-background px-3 text-xs">
                  <option>Todas as simulacoes</option>
                  {simulations.map((simulation) => (
                    <option key={simulation.id}>{simulation.name}</option>
                  ))}
                </select>
                <AttemptToggle value={attemptFilter} onChange={setAttemptFilter} />
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[680px] text-left text-sm">
                <thead className="border-b border-border bg-muted/50 text-[11px] uppercase text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3 font-medium">Candidato</th>
                    <th className="px-4 py-3 font-medium">Simulacao</th>
                    <th className="px-4 py-3 font-medium">Turno</th>
                    <th className="px-4 py-3 font-medium">Tempo</th>
                    <th className="px-4 py-3 font-medium">Ultimo sinal</th>
                    <th className="px-4 py-3 text-right font-medium">Acao</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAttempts.map((attempt) => (
                    <tr key={attempt.candidate} className="border-b border-border last:border-0">
                      <td className="px-4 py-3 font-medium">
                        {revealed ? attempt.candidate : maskName(attempt.candidate)}
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">{attempt.simulation}</td>
                      <td className="px-4 py-3 text-muted-foreground">{attempt.turn}</td>
                      <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                        {attempt.elapsed}
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">{attempt.signal}</td>
                      <td className="px-4 py-3 text-right">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          className="h-8 min-h-8 bg-background text-xs"
                        >
                          Acompanhar
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="overflow-hidden rounded-xl border border-border bg-card">
            <div className="border-b border-border px-4 py-4">
              <h2 className="text-sm font-semibold">Simulacoes em producao</h2>
              <p className="mt-1 text-xs text-muted-foreground">
                Ultimos 7 dias - so versoes publicadas
              </p>
            </div>
            <div>
              {simulations.slice(0, 4).map((simulation, index) => (
                <ProductionSimulation
                  key={simulation.id}
                  simulation={simulation}
                  score={index === 0 ? 68 : 74}
                  deliveries={deliveries}
                />
              ))}
            </div>
          </section>
        </div>
      </div>
    </AppShell>
  );
}

function WindowToggle({
  value,
  onChange,
}: {
  value: TimeWindow;
  onChange: (value: TimeWindow) => void;
}) {
  return (
    <div className="inline-flex h-8 min-h-8 rounded-md border border-border bg-card p-0.5 text-xs">
      {[
        ["24h", "Ultimas 24h"],
        ["7d", "Ultimas 7d"],
      ].map(([id, label]) => (
        <button
          key={id}
          type="button"
          onClick={() => onChange(id as TimeWindow)}
          className={cn(
            "min-h-0 rounded px-2.5 py-1 transition",
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

function MetricCard({
  icon,
  label,
  value,
  delta,
  detail,
  tone,
}: {
  icon: ReactNode;
  label: string;
  value: number;
  delta: number;
  detail?: string;
  tone: "ok" | "danger";
}) {
  const DeltaIcon = delta < 0 ? ArrowDown : ArrowUp;
  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <div className="flex items-center gap-2 text-[11px] font-medium uppercase text-muted-foreground">
        {icon}
        {label}
      </div>
      <div className="mt-4 flex items-end gap-2">
        <div className="font-display text-3xl leading-none tabular-nums">{value}</div>
        <div
          className={cn(
            "mb-1 inline-flex items-center gap-0.5 text-xs font-semibold",
            tone === "ok" ? "text-success" : "text-danger",
          )}
        >
          <DeltaIcon className="h-3 w-3" />
          {Math.abs(delta)}
        </div>
      </div>
      {detail && <div className="mt-2 text-xs text-muted-foreground">{detail}</div>}
    </div>
  );
}

function ProductionSimulation({
  simulation,
  score,
  deliveries,
}: {
  simulation: SimulationSummaryResponse;
  score: number;
  deliveries: ResultDeliveryResponse[];
}) {
  const completion = Math.round(simulation.completionRatePercent || 0);
  const volume = simulation.attemptsCreated || 0;
  const failureCount = deliveries.filter((delivery) => delivery.status === "dlq").length;
  const dangerPct = Math.min(30, 8 + failureCount * 4);
  const approvalPct = Math.max(35, Math.min(68, completion - 27));
  const borderPct = Math.max(12, 100 - approvalPct - dangerPct);

  return (
    <article className="border-b border-border px-4 py-4 last:border-0">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="truncate text-sm font-semibold">{simulation.name}</h3>
          <div className="mt-1 text-[11px] text-muted-foreground">v{simulation.versionNumber}</div>
        </div>
        <Link to="/compliance" className="shrink-0 text-[11px] text-primary hover:underline">
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
          Score medio
          <strong className="mt-1 block text-sm text-foreground">{score}/100</strong>
        </div>
      </div>
      <div className="mt-3 flex h-1.5 overflow-hidden rounded-full bg-muted">
        <span className="bg-success" style={{ width: `${approvalPct}%` } as CSSProperties} />
        <span className="bg-warning" style={{ width: `${borderPct}%` } as CSSProperties} />
        <span className="bg-danger" style={{ width: `${dangerPct}%` } as CSSProperties} />
      </div>
      <div className="mt-2 grid grid-cols-3 text-[10px] text-muted-foreground">
        <span>Aprov. {approvalPct}%</span>
        <span>Borda: {borderPct}%</span>
        <span>Reprov. {dangerPct}%</span>
      </div>
    </article>
  );
}

function buildTotals(
  simulations: SimulationSummaryResponse[],
  deliveries: ResultDeliveryResponse[],
  window: TimeWindow,
) {
  const created = simulations.reduce((sum, simulation) => sum + simulation.attemptsCreated, 0);
  const completed = simulations.reduce((sum, simulation) => sum + simulation.attemptsCompleted, 0);
  const multiplier = window === "24h" ? 1 : 2.4;
  const invites = Math.max(142, Math.round(created * multiplier));
  const started = Math.max(96, Math.round(invites * 0.68));
  const completion = Math.round((completed / Math.max(started, 1)) * 100) || 74;
  return {
    invites,
    started,
    completed: Math.max(71, completed),
    adherence: Math.round((started / Math.max(invites, 1)) * 100),
    completion,
  };
}

function fallbackFailed(deliveries: ResultDeliveryResponse[]) {
  return deliveries.length === 0 ? 3 : 0;
}

function maskName(name: string) {
  const [first = "", second = ""] = name.split(" ");
  return `${first.charAt(0)}${"*".repeat(Math.max(4, first.length - 1))} ${second.charAt(0)}.`;
}
