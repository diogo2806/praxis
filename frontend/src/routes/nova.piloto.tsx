import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import {
  EmptyState,
  StateBanner,
} from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import { CalibrationReport } from "@/components/simulation/calibration-report";
import {
  getCalibrationReport,
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
      { title: "Piloto e indicadores - Práxis" },
      { name: "description", content: "Indicadores reais de execução antes da publicação." },
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
  const calibrationQuery = useQuery({
    queryKey: ["simulation-calibration", search.simulationId, search.versionNumber],
    queryFn: () => getCalibrationReport(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });
  const monitoring = monitoringQuery.data;

  return (
    <AppShell>
      <WizardStepper current="publicacao" />
      <div className="mb-6">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 4</div>
        <h1 className="mt-1 font-display text-3xl">Piloto e indicadores</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Esta tela mostra os indicadores reais de quem já respondeu, calculados pelo sistema.
        </p>
      </div>
      {!hasContext ? (
        <EmptyState
          title="Selecione uma versão para acompanhar"
          description="Escolha uma versão abaixo para ver os indicadores de quem já respondeu."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : monitoringQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando piloto">
          Buscando monitoramento da avaliação {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      ) : monitoringQuery.isError ? (
        <StateBanner tone="danger" title="Não foi possível carregar o piloto">
          {monitoringQuery.error instanceof Error
            ? monitoringQuery.error.message
            : "Verifique sua conexão e tente novamente."}
        </StateBanner>
      ) : monitoring ? (
        <>
          <div className="mt-5 grid gap-4 md:grid-cols-4">
            <Metric label="Criadas" value={monitoring.attemptsCreated} />
            <Metric
              label="Em andamento"
              value={monitoring.attemptsInProgress + monitoring.attemptsPaused}
            />
            <Metric label="Concluídas" value={monitoring.attemptsCompleted} />
            <Metric label="Conclusão" value={`${monitoring.completionRatePercent.toFixed(1)}%`} />
          </div>
          <div className="mt-5 grid gap-4 md:grid-cols-4">
            <Metric label="Abandonadas" value={monitoring.attemptsAbandoned} />
            <Metric label="Expiradas" value={monitoring.attemptsExpired} />
            <Metric label="Falhas" value={monitoring.attemptsFailed} />
            <Metric label="Desistência" value={`${monitoring.dropOffRatePercent.toFixed(1)}%`} />
          </div>
          <section className="mt-8">
            {calibrationQuery.isLoading ? (
              <StateBanner tone="info" title="Calculando calibração">
                Reunindo as tentativas concluídas para calibrar critérios e pesos.
              </StateBanner>
            ) : calibrationQuery.isError ? (
              <StateBanner tone="danger" title="Não foi possível calcular a calibração">
                {calibrationQuery.error instanceof Error
                  ? calibrationQuery.error.message
                  : "Tente novamente."}
              </StateBanner>
            ) : calibrationQuery.data ? (
              <CalibrationReport report={calibrationQuery.data} />
            ) : null}
          </section>
          <div className="mt-8 flex justify-between">
            <Link
              to="/nova/validador"
              search={{
                simulationId: search.simulationId,
                versionNumber: search.versionNumber,
              }}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar: Validador
            </Link>
            <Link
              to="/nova/mapa"
              search={{
                simulationId: search.simulationId,
                versionNumber: search.versionNumber,
              }}
              className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Ver mapa e pontuação
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
      {simulations.map((simulation) => {
        const versionNumber = simulation.livePublishedVersionNumber ?? simulation.versionNumber;
        return (
          <Link
            key={`${simulation.id}-${versionNumber}`}
            to="/nova/piloto"
            search={{ simulationId: simulation.id, versionNumber }}
            className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
          >
            {simulation.name} v{versionNumber}
          </Link>
        );
      })}
    </div>
  );
}
