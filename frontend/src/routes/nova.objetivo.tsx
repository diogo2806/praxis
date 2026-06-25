import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  getSimulationVersion,
  listSimulations,
  updateSimulationBlueprint,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/nova/objetivo")({
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
      { title: "Objetivo & Modelo - Práxis" },
      { name: "description", content: "Revisão do modelo base antes da autoria do personagem." },
    ],
  }),
  component: Page,
});

function Page() {
  const search = Route.useSearch();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const hasDraftContext = Boolean(search.simulationId && search.versionNumber);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasDraftContext,
  });
  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasDraftContext,
  });
  const saveMutation = useMutation({
    mutationFn: () =>
      updateSimulationBlueprint(search.simulationId!, search.versionNumber!, {
        rootNodeId: versionQuery.data!.blueprint.rootNodeId,
        competencies: versionQuery.data!.blueprint.competencies,
      }),
    onSuccess: async (simulation) => {
      await queryClient.invalidateQueries({
        queryKey: ["simulation-version", search.simulationId, search.versionNumber],
      });
      void navigate({
        to: "/nova/personagem",
        search: { simulationId: simulation.id, versionNumber: simulation.versionNumber },
      });
    },
  });

  return (
    <AppShell>
      <WizardStepper current="avaliacao" unlockedThrough="cenario" />
      <ScreenStateStrip blockedReason="selecione uma versão real para continuar" />
      <div className="mb-8">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 1</div>
        <h1 className="mt-1 font-display text-3xl">Objetivo do modelo base</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Esta etapa usa o modelo base salvo no sistema. Ajustes estruturais continuam em Nova
          teste.
        </p>
      </div>

      {!hasDraftContext ? (
        <EmptyState
          title="Escolha um teste real"
          description="Sem contexto de versão, esta tela não cria objetivo local nem carrega exemplos."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : versionQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando modelo base">
          Buscando teste {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      ) : versionQuery.isError ? (
        <StateBanner tone="danger" title="Não foi possível carregar o modelo base">
          {versionQuery.error instanceof Error
            ? versionQuery.error.message
            : "Não foi possível carregar agora. Verifique sua conexão e tente novamente."}
        </StateBanner>
      ) : versionQuery.data ? (
        <>
          <section className="rounded-md border border-border bg-card p-6">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="text-xl font-semibold">{versionQuery.data.name}</h2>
                <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
                  {versionQuery.data.description}
                </p>
              </div>
              <div className="flex flex-col items-end gap-2">
                <StatusBadge status={versionQuery.data.status} />
                {versionQuery.data.status === "draft" && (
                  <Link
                    to="/nova/blueprint"
                    search={{
                      simulationId: search.simulationId,
                      versionNumber: search.versionNumber,
                    }}
                    className="rounded-md border border-border bg-card px-3 py-1.5 text-xs font-medium hover:bg-accent"
                  >
                    Editar modelo base
                  </Link>
                )}
              </div>
            </div>
            <div className="mt-5 grid gap-3 md:grid-cols-2">
              <Info label="Primeira etapa" value={versionQuery.data.blueprint.rootNodeId} />
              <Info label="Versão" value={`v${versionQuery.data.versionNumber}`} />
            </div>
            <div className="mt-5">
              <h3 className="text-sm font-semibold">Competências</h3>
              <div className="mt-3 grid gap-2 md:grid-cols-3">
                {versionQuery.data.blueprint.competencies.map((competency) => (
                  <div
                    key={competency.name}
                    className="rounded-md border border-border bg-background p-3"
                  >
                    <div className="text-sm font-medium">{competency.name}</div>
                  </div>
                ))}
              </div>
            </div>
          </section>
          {saveMutation.isError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Não foi possível confirmar o modelo base">
                {saveMutation.error instanceof Error
                  ? saveMutation.error.message
                  : "Tente novamente."}
              </StateBanner>
            </div>
          )}
          <div className="mt-8 flex flex-row-reverse justify-between">
            <button
              type="button"
              onClick={() => saveMutation.mutate()}
              disabled={saveMutation.isPending}
              className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {saveMutation.isPending ? "Salvando..." : "Personagem →"}
            </button>
            <Link
              to="/nova/blueprint"
              search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar: Modelo base
            </Link>
          </div>
        </>
      ) : null}
    </AppShell>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-border bg-background p-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-1 text-sm font-medium">{value}</div>
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
  if (simulations.length === 0) {
    return (
      <Link
        to="/nova/blueprint"
        className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
      >
        Criar modelo base
      </Link>
    );
  }
  return (
    <div className="flex flex-wrap gap-2">
      {simulations.map((simulation) => (
        <Link
          key={`${simulation.id}-${simulation.versionNumber}`}
          to="/nova/blueprint"
          search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
          className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          {simulation.name} v{simulation.versionNumber}
        </Link>
      ))}
    </div>
  );
}
