import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import {
  EmptyState,
  NextStepContract,
  ScreenStateStrip,
  StateBanner,
} from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  getSimulationMonitoring,
  listSimulations,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/nova/piloto")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string"
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Piloto & Calibracao - Praxis" },
      { name: "description", content: "Indicadores reais de execucao antes da publicacao." },
    ],
  }),
  component: Page,
});

function Page() {
  const search = Route.useSearch();
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasContext,
  });
  const monitoringQuery = useQuery({
    queryKey: ["simulation-monitoring", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationMonitoring(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });
  const monitoring = monitoringQuery.data;

  return (
    <AppShell>
      <WizardStepper current="publicacao" />
      <ScreenStateStrip blockedReason="acompanhe tentativas reais antes de avancar" />
      <div className="mb-6">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 4</div>
        <h1 className="mt-1 font-display text-3xl">Piloto & Calibracao</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Esta tela mostra somente indicadores retornados pelo backend.
        </p>
      </div>

      {!hasContext ? (
        <EmptyState
          title="Selecione uma versao para acompanhar"
          description="Nao ha participantes ou checklist local nesta etapa."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : monitoringQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando piloto">
          Buscando monitoramento da simulacao {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      ) : monitoringQuery.isError ? (
        <StateBanner tone="danger" title="Nao foi possivel carregar o piloto">
          {monitoringQuery.error instanceof Error
            ? monitoringQuery.error.message
            : "Verifique a API."}
        </StateBanner>
      ) : monitoring ? (
        <>
          <NextStepContract
            primary="Usar indicadores reais para decidir se a versao segue para mapa e governanca."
            secondary="Voltar ao validador ou blueprint antes da publicacao final continua permitido."
            versionRule="Ajustes posteriores criam nova versao quando a versao atual estiver publicada."
            lockedAfter="Tentativas existentes permanecem na versao em que foram criadas."
          />
          <div className="mt-5 grid gap-4 md:grid-cols-4">
            <Metric label="Criadas" value={monitoring.attemptsCreated} />
            <Metric
              label="Em andamento"
              value={monitoring.attemptsInProgress + monitoring.attemptsPaused}
            />
            <Metric label="Concluidas" value={monitoring.attemptsCompleted} />
            <Metric label="Conclusao" value={`${monitoring.completionRatePercent.toFixed(1)}%`} />
          </div>
          <div className="mt-5 grid gap-4 md:grid-cols-4">
            <Metric label="Abandonadas" value={monitoring.attemptsAbandoned} />
            <Metric label="Expiradas" value={monitoring.attemptsExpired} />
            <Metric label="Falhas" value={monitoring.attemptsFailed} />
            <Metric label="Drop-off" value={`${monitoring.dropOffRatePercent.toFixed(1)}%`} />
          </div>
          <div className="mt-8 flex justify-between">
            <Link
              to="/nova/validador"
              search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar: Validador
            </Link>
            <Link
              to="/nova/mapa"
              search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Ver mapa & score
            </Link>
          </div>
        </>
      ) : null}
    </AppShell>
  );
}

function Metric({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded-md border border-border bg-card p-4">
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="mt-2 text-3xl font-semibold tabular-nums">{value}</div>
    </div>
  );
}

function SimulationLinks({
  loading,
  simulations,
}: {
  loading: boolean;
  simulations: SimulationSummaryResponse[];
}) {
  if (loading) return <span className="text-sm text-muted-foreground">Carregando...</span>;
  return (
    <div className="flex flex-wrap gap-2">
      {simulations.map((simulation) => (
        <Link
          key={`${simulation.id}-${simulation.versionNumber}`}
          to="/nova/piloto"
          search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
          className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          {simulation.name} v{simulation.versionNumber}
        </Link>
      ))}
    </div>
  );
}
