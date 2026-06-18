import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  getSimulationVersion,
  listSimulations,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/nova/mapa")({
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
      { title: "Mapa & Score - Praxis" },
      { name: "description", content: "Fluxo da conversa e pesos reais da versão." },
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
  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });
  const version = versionQuery.data;

  return (
    <AppShell>
      <WizardStepper current="revisao" unlockedThrough="publicacao" />
      <ScreenStateStrip blockedReason="fluxo da conversa inválido ou com caminho sem saída precisa voltar ao editor" />
      <div className="mb-6">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 3</div>
        <h1 className="mt-1 font-display text-3xl">Mapa & pontuação</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Visão geral das etapas, das alternativas e dos pesos de cada competência.
        </p>
      </div>

      {!hasContext ? (
        <EmptyState
          title="Selecione uma versão para ver o mapa"
          description="Escolha uma versão abaixo para ver o mapa completo da conversa."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : versionQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando mapa">
          Buscando o fluxo da conversa da simulação {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      ) : versionQuery.isError ? (
        <StateBanner tone="danger" title="Não foi possível carregar o mapa">
          {versionQuery.error instanceof Error ? versionQuery.error.message : "Verifique sua conexão e tente novamente."}
        </StateBanner>
      ) : version ? (
        <>
          <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
            <div className="rounded-md border border-border bg-card p-5">
              <h2 className="text-lg font-semibold">
                {version.name} - v{version.versionNumber}
              </h2>
              <div className="mt-4 space-y-4">
                {version.nodes.map((node) => (
                  <div key={node.id} className="rounded-md border border-border bg-background p-4">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="font-semibold">{node.id}</div>
                      <div className="text-xs text-muted-foreground">turno {node.turnIndex}</div>
                    </div>
                    <p className="mt-2 text-sm text-foreground/80">{node.clientMessage}</p>
                    <div className="mt-3 space-y-2">
                      {node.options.map((option) => (
                        <div
                          key={option.id}
                          className="rounded border border-border bg-card p-3 text-sm"
                        >
                          <div>{option.text}</div>
                          <div className="mt-2 flex flex-wrap gap-1.5 text-[11px] text-muted-foreground">
                            <span className="rounded border border-border px-2 py-1">
                              próximo: {option.nextNodeId ?? "fim"}
                            </span>
                            {Object.entries(option.competencyLevels).map(([name, value]) => (
                              <span key={name} className="rounded border border-border px-2 py-1">
                                {name}: {value}
                              </span>
                            ))}
                            {option.isCritical && (
                              <span className="rounded border border-danger/30 px-2 py-1 text-danger">
                                crítica
                              </span>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <aside className="rounded-md border border-border bg-card p-5">
              <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Pesos do score
              </div>
              <div className="mt-3 space-y-2">
                {version.blueprint.competencies.map((competency) => (
                  <div
                    key={competency.name}
                    className="flex justify-between rounded-md border border-border bg-background p-3 text-sm"
                  >
                    <span>{competency.name}</span>
                    <span className="tabular-nums text-muted-foreground">
                      {(competency.weight * 100).toFixed(0)}%
                    </span>
                  </div>
                ))}
              </div>
            </aside>
          </div>
          <div className="mt-8 flex justify-between">
            <Link
              to="/nova/piloto"
              search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar: Piloto
            </Link>
            <Link
              to="/nova/governanca"
              search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Governança
            </Link>
          </div>
        </>
      ) : null}
    </AppShell>
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
          to="/nova/mapa"
          search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
          className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          {simulation.name} v{simulation.versionNumber}
        </Link>
      ))}
    </div>
  );
}
