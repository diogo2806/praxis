import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import type { CSSProperties } from "react";
import { AlertTriangle, BarChart3, CheckCircle2, Clock, RefreshCw, Send } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  getSimulationMonitoring,
  listSimulations,
  listResultDeliveries,
  type ResultDeliveryResponse,
  type ResultDeliveryStatus,
  type SimulationSummaryResponse,
  type SimulationMonitoringResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/monitoramento")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
        ? Number(search.versionNumber)
        : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Monitoramento - Praxis" },
      {
        name: "description",
        content: "Acompanhamento de uso, entregas e alertas após a publicação.",
      },
    ],
  }),
  component: MonitoringPage,
});

const deliveryMeta: Record<ResultDeliveryStatus, { label: string; className: string }> = {
  pending: { label: "Pendente", className: "border-border bg-muted text-muted-foreground" },
  retrying: {
    label: "Reenviando",
    className: "border-warning/40 bg-warning/15 text-warning-foreground",
  },
  sent: { label: "Enviado", className: "border-success/30 bg-success/10 text-success" },
  dlq: { label: "Falha no envio", className: "border-danger/30 bg-danger/10 text-danger" },
};

function buildLiveCohorts(monitoring?: SimulationMonitoringResponse) {
  if (!monitoring) return [];

  const total = Math.max(monitoring.attemptsCreated, 1);
  return [
    {
      label: "Concluídas",
      value: monitoring.attemptsCompleted,
      pct: monitoring.completionRatePercent,
    },
    {
      label: "Em andamento",
      value: monitoring.attemptsInProgress + monitoring.attemptsPaused,
      pct: ((monitoring.attemptsInProgress + monitoring.attemptsPaused) / total) * 100,
    },
    {
      label: "Nao iniciadas",
      value: monitoring.attemptsNotStarted,
      pct: (monitoring.attemptsNotStarted / total) * 100,
    },
    {
      label: "Abandonadas",
      value: monitoring.attemptsAbandoned + monitoring.attemptsExpired + monitoring.attemptsFailed,
      pct: monitoring.dropOffRatePercent,
    },
  ];
}

function MonitoringPage() {
  const search = Route.useSearch();
  const hasMonitoringParams = Boolean(search.simulationId && search.versionNumber);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasMonitoringParams,
  });
  const monitoringQuery = useQuery({
    queryKey: ["simulation-monitoring", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationMonitoring(search.simulationId!, search.versionNumber!),
    enabled: hasMonitoringParams,
  });
  const deliveriesQuery = useQuery({
    queryKey: ["result-deliveries", search.simulationId, search.versionNumber],
    queryFn: () =>
      listResultDeliveries({
        simulationId: search.simulationId,
        versionNumber: search.versionNumber,
      }),
    enabled: hasMonitoringParams,
  });

  const monitoring = monitoringQuery.data;
  const deliveries = deliveriesQuery.data ?? [];
  const cohorts = buildLiveCohorts(monitoring);
  const hasData = monitoringQuery.isLoading || Boolean(monitoring);
  const riskyDeliveries = deliveries.filter(
    (delivery) => delivery.status === "retrying" || delivery.status === "dlq",
  );
  const retryingDeliveries = deliveries.filter((delivery) => delivery.status === "retrying").length;
  const sentDeliveries = deliveries.filter((delivery) => delivery.status === "sent").length;
  const failedDeliveries = deliveries.filter((delivery) => delivery.status === "dlq").length;

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="sem dados suficientes para análise" />
      <div className="mb-5 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Monitoramento</div>
          <h1 className="mt-1 text-3xl font-semibold">Acompanhamento</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Veja tentativas, conclusões, falhas de envio e sinais que precisam de revisão.
          </p>
        </div>
        <Link
          to="/"
          className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar ao painel
        </Link>
      </div>
      {hasMonitoringParams && monitoringQuery.isLoading && (
        <StateBanner tone="info" title="Monitoramento conectado">
          Buscando indicadores da simulação {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      )}

      {hasMonitoringParams && monitoringQuery.isError && (
        <StateBanner tone="danger" title="Não foi possível carregar o monitoramento">
          {monitoringQuery.error instanceof Error
            ? monitoringQuery.error.message
            : "Verifique se o servidor está rodando e se a versão existe."}
        </StateBanner>
      )}

      {!hasMonitoringParams ? (
        <EmptyState
          title="Selecione uma simulação para monitorar"
          description="Você verá os indicadores (tentativas, conclusões, erros e entregas) depois que uma avaliação estiver no ar na Gupy. Escolha uma simulação abaixo ou crie uma nova."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : !hasData ? (
        <EmptyState
          title="Não foi possível exibir monitoramento"
          description="A versão solicitada não retornou indicadores. Verifique o erro acima ou escolha outra simulação."
          actions={
            <Link
              to="/"
              className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
            >
              Voltar ao painel
            </Link>
          }
        />
      ) : (
        <div className="mt-5 space-y-5">
          <StateBanner
            tone={failedDeliveries > 0 ? "danger" : riskyDeliveries.length > 0 ? "warn" : "ok"}
            title={
              failedDeliveries > 0
                ? "Envios com falha precisam de atenção"
                : riskyDeliveries.length > 0
                  ? "Resultados sendo reenviados"
                  : "Fila de resultados saudável"
            }
          >
            {`${sentDeliveries} resultados enviados, ${retryingDeliveries} sendo reenviados e ${failedDeliveries} com falha.`}
          </StateBanner>

          <div className="grid gap-3 md:grid-cols-4">
            {cohorts.map((item) => (
              <div key={item.label} className="rounded-md border border-border bg-card p-4">
                <div className="text-xs uppercase text-muted-foreground">{item.label}</div>
                <div className="mt-1 text-2xl font-semibold tabular-nums">{item.value}</div>
                <div className="praxis-progress-track praxis-progress-track-sm mt-2">
                  <div
                    className="praxis-progress-fill bg-primary"
                    style={
                      {
                        "--praxis-progress-value": `${Math.min(100, Math.max(0, item.pct))}%`,
                      } as CSSProperties
                    }
                  />
                </div>
                <div className="mt-1 text-[11px] text-muted-foreground">
                  {formatPercent(item.pct)}
                </div>
              </div>
            ))}
          </div>

          <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
            <section className="rounded-md border border-border bg-card p-5">
              <div className="mb-4 flex items-center gap-2 text-sm font-semibold">
                <BarChart3 className="h-4 w-4" />
                Fila de entregas Gupy da versão
              </div>
              <DeliveryList
                deliveries={deliveries}
                loading={deliveriesQuery.isLoading}
                error={deliveriesQuery.isError}
              />
            </section>

            <aside className="space-y-3">
              <StateBanner tone="ok" title="Acompanhamento ativo">
                Referências internas continuam separadas dos candidatos reais.
              </StateBanner>
              <StateBanner tone={failedDeliveries > 0 ? "danger" : "warn"} title="Regra de alerta">
                Erro crítico gera revisão humana obrigatória; não decide o processo sozinho.
              </StateBanner>
              <div className="rounded-md border border-border bg-card p-4">
                <div className="mb-3 text-sm font-semibold">Checklist operacional</div>
                <ul className="space-y-2 text-sm">
                  <li className="flex gap-2">
                    <CheckCircle2 className="mt-0.5 h-4 w-4 text-success" />
                    Reenvio automático ativo
                  </li>
                  <li className="flex gap-2">
                    <CheckCircle2 className="mt-0.5 h-4 w-4 text-success" />
                    <span>Registro de auditoria imutável</span>
                  </li>
                  <li className="flex gap-2">
                    <AlertTriangle className="mt-0.5 h-4 w-4 text-warning-foreground" />
                    Revisar caminhos com abandono acima de 20%
                  </li>
                </ul>
              </div>
            </aside>
          </div>
        </div>
      )}
    </AppShell>
  );
}

function DeliveryList({
  deliveries,
  loading,
  error,
}: {
  deliveries: ResultDeliveryResponse[];
  loading: boolean;
  error: boolean;
}) {
  if (loading) {
    return (
      <div className="rounded-md border border-border bg-background p-4 text-sm">
        Carregando entregas...
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-md border border-danger/30 bg-danger/10 p-4 text-sm text-danger">
        Não foi possível carregar a fila de entregas.
      </div>
    );
  }

  if (deliveries.length === 0) {
    return (
      <div className="rounded-md border border-border bg-background p-4 text-sm text-muted-foreground">
        Nenhuma entrega registrada ainda.
      </div>
    );
  }

  return (
    <div className="rounded-md border border-border bg-background">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Resultado</TableHead>
            <TableHead>Tentativa</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="text-right">Envio</TableHead>
            <TableHead>Erro</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {deliveries.slice(0, 6).map((delivery) => {
            const meta = deliveryMeta[delivery.status];
            return (
              <TableRow key={delivery.id}>
                <TableCell className="min-w-[180px] font-medium">{delivery.resultId}</TableCell>
                <TableCell className="min-w-[180px] text-xs text-muted-foreground">
                  {delivery.attemptId}
                </TableCell>
                <TableCell>
                  <span
                    className={cn(
                      "inline-flex h-7 items-center justify-center rounded-md border px-2 text-xs font-medium",
                      meta.className,
                    )}
                  >
                    {meta.label}
                  </span>
                </TableCell>
                <TableCell className="min-w-[140px] text-right text-xs tabular-nums text-muted-foreground">
                  <span className="inline-flex items-center justify-end gap-2">
                    {delivery.status === "sent" ? (
                      <Send className="h-3.5 w-3.5" />
                    ) : delivery.status === "retrying" ? (
                      <RefreshCw className="h-3.5 w-3.5" />
                    ) : (
                      <Clock className="h-3.5 w-3.5" />
                    )}
                    {delivery.status === "sent"
                      ? formatDateTime(delivery.sentAt)
                      : formatDateTime(delivery.nextAttemptAt ?? delivery.createdAt)}
                  </span>
                </TableCell>
                <TableCell className="min-w-[220px] max-w-[360px] text-xs text-danger">
                  <span className="line-clamp-2">{delivery.lastError ?? "-"}</span>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}

function SimulationLinks({
  simulations,
  loading,
}: {
  simulations: SimulationSummaryResponse[];
  loading: boolean;
}) {
  if (loading) {
    return (
      <div className="rounded-md border border-border bg-card px-4 py-3 text-sm">
        Carregando simulações...
      </div>
    );
  }

  if (simulations.length === 0) {
    return (
      <Link
        to="/nova/blueprint"
        className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
      >
        Criar simulação
      </Link>
    );
  }

  return (
    <>
      {simulations.slice(0, 3).map((simulation) => (
        <Link
          key={simulation.id}
          to="/monitoramento"
          search={{
            simulationId: simulation.id,
            versionNumber: simulation.versionNumber,
          }}
          className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
        >
          <span className="block font-medium">{simulation.name}</span>
          <span className="mt-1 block">
            <StatusBadge
              status={simulation.status}
              maturity={maturityForStatus(simulation.status)}
            />
          </span>
        </Link>
      ))}
    </>
  );
}

function formatPercent(value: number) {
  return `${Number.isFinite(value) ? value.toFixed(value % 1 === 0 ? 0 : 1) : "0"}%`;
}

function formatDateTime(value: string | null) {
  if (!value) return "sem data";
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
