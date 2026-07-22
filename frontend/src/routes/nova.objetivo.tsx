import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";

import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  getSimulationVersion,
  listSimulations,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/nova/objetivo")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Objetivo & Modelo - Práxis" },
      {
        name: "description",
        content: "Resumo somente leitura do modelo base antes da autoria do personagem.",
      },
    ],
  }),
  component: Page,
});

function Page() {
  const search = Route.useSearch();
  const hasVersionContext = Boolean(search.simulationId && search.versionNumber);

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasVersionContext,
  });

  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasVersionContext,
  });

  const competencies = versionQuery.data?.blueprint.competencies ?? [];
  const hasNoCompetencies = competencies.length === 0;

  return (
    <AppShell>
      <WizardStepper current="avaliacao" unlockedThrough="cenario" />

      <div className="mb-8">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 1</div>
        <h1 className="mt-1 font-display text-3xl">Objetivo do modelo base</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Revise o resumo da versão antes de seguir para o personagem. Esta tela não altera mais
          competências nem estrutura da avaliação.
        </p>
      </div>

      {!hasVersionContext ? (
        <EmptyState
          title="Escolha uma avaliação real"
          description="Sem avaliação e versão, esta tela não cria dados locais nem carrega exemplos."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : versionQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando modelo base">
          Buscando avaliação {search.simulationId} v{search.versionNumber}.
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
                <p className="mt-1 max-w-3xl whitespace-pre-line text-sm text-muted-foreground">
                  {versionQuery.data.description || "Sem descrição cadastrada."}
                </p>
              </div>
              <StatusBadge status={versionQuery.data.status} />
            </div>

            <div className="mt-5 grid gap-3 md:grid-cols-3">
              <Info label="Primeira etapa" value={versionQuery.data.blueprint.rootNodeId} />
              <Info label="Versão" value={`v${versionQuery.data.versionNumber}`} />
              <Info
                label="Situação crítica"
                value={versionQuery.data.criticalSituation || "Não informada"}
              />
            </div>

            <div className="mt-6">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h3 className="text-sm font-semibold">Competências selecionadas</h3>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Resumo somente leitura. A seleção da versão é feita em Nova avaliação e o
                    catálogo global é mantido em Competências.
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Link
                    to="/nova/avaliacao"
                    search={{
                      simulationId: versionQuery.data.simulationId,
                      versionNumber: versionQuery.data.versionNumber,
                    }}
                    className="rounded-md border border-border bg-background px-3 py-2 text-xs font-medium text-primary hover:bg-accent"
                  >
                    Alterar seleção
                  </Link>
                  <Link
                    to="/competencias"
                    className="rounded-md border border-border bg-background px-3 py-2 text-xs font-medium text-primary hover:bg-accent"
                  >
                    Abrir catálogo
                  </Link>
                </div>
              </div>

              {hasNoCompetencies ? (
                <div className="mt-4">
                  <StateBanner tone="warn" title="Nenhuma competência selecionada">
                    Retorne à Nova avaliação e selecione ao menos uma competência ativa do catálogo
                    antes de continuar.
                  </StateBanner>
                </div>
              ) : (
                <div className="mt-4 grid gap-2 md:grid-cols-2 lg:grid-cols-3">
                  {competencies.map((competency) => (
                    <article
                      key={competency.name}
                      className="rounded-md border border-border bg-background p-3"
                    >
                      <div className="text-sm font-medium text-foreground">{competency.name}</div>
                      <dl className="mt-2 space-y-1 text-xs text-muted-foreground">
                        <div className="flex items-center justify-between gap-3">
                          <dt>Peso</dt>
                          <dd className="font-medium text-foreground">
                            {formatPercent(competency.weight)}
                          </dd>
                        </div>
                        <div className="flex items-center justify-between gap-3">
                          <dt>Importância</dt>
                          <dd className="font-medium text-foreground">
                            {competency.tier === "minor" ? "Secundária" : "Principal"}
                          </dd>
                        </div>
                        <div className="flex items-center justify-between gap-3">
                          <dt>Meta</dt>
                          <dd className="font-medium text-foreground">
                            {competency.targetScore == null
                              ? "Não definida"
                              : competency.targetScore.toLocaleString("pt-BR")}
                          </dd>
                        </div>
                      </dl>
                    </article>
                  ))}
                </div>
              )}
            </div>
          </section>

          <div className="mt-8 flex flex-wrap items-center justify-between gap-3">
            <Link
              to="/nova/avaliacao"
              search={{
                simulationId: versionQuery.data.simulationId,
                versionNumber: versionQuery.data.versionNumber,
              }}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar: Nova avaliação
            </Link>

            {hasNoCompetencies ? (
              <span
                aria-disabled="true"
                className="cursor-not-allowed rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground opacity-50"
              >
                Personagem →
              </span>
            ) : (
              <Link
                to="/nova/personagem"
                search={{
                  simulationId: versionQuery.data.simulationId,
                  versionNumber: versionQuery.data.versionNumber,
                }}
                className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                Personagem →
              </Link>
            )}
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
      <div className="mt-1 whitespace-pre-line text-sm font-medium">{value}</div>
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
        to="/nova/avaliacao"
        className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
        search={{ simulationId: undefined, versionNumber: undefined }}
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
          to="/nova/objetivo"
          search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
          className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          {simulation.name} v{simulation.versionNumber}
        </Link>
      ))}
    </div>
  );
}

function formatPercent(weight: number) {
  return `${(weight * 100).toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%`;
}
