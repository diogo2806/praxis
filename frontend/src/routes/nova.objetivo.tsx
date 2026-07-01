import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  getSimulationVersion,
  listSimulations,
  updateSimulationBlueprint,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";

function normalizeCompetency(value: string) {
  return value
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .trim()
    .toLowerCase();
}

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

  const isDraft = versionQuery.data?.status === "draft";
  const [editedCompetencies, setEditedCompetencies] = useState<string[]>([]);
  const [newCompetency, setNewCompetency] = useState("");
  const [hydratedVersionKey, setHydratedVersionKey] = useState<string | null>(null);

  // Reidrata o rascunho local apenas quando muda a versão carregada do servidor,
  // preservando edições não salvas durante refetches da mesma versão.
  useEffect(() => {
    if (!versionQuery.data) {
      return;
    }
    const versionKey = `${versionQuery.data.simulationId}:${versionQuery.data.versionNumber}`;
    if (hydratedVersionKey === versionKey) {
      return;
    }
    setEditedCompetencies(versionQuery.data.blueprint.competencies.map((item) => item.name));
    setNewCompetency("");
    setHydratedVersionKey(versionKey);
  }, [hydratedVersionKey, versionQuery.data]);

  const displayCompetencies = isDraft
    ? editedCompetencies
    : (versionQuery.data?.blueprint.competencies.map((item) => item.name) ?? []);
  const trimmedNewCompetency = newCompetency.trim();
  const canAddCompetency =
    trimmedNewCompetency.length > 0 &&
    trimmedNewCompetency.length <= 140 &&
    !displayCompetencies.some(
      (name) => normalizeCompetency(name) === normalizeCompetency(trimmedNewCompetency),
    );

  function addCompetency() {
    if (!canAddCompetency) {
      return;
    }
    setEditedCompetencies((current) => [...current, trimmedNewCompetency]);
    setNewCompetency("");
  }

  function removeCompetency(name: string) {
    setEditedCompetencies((current) => current.filter((item) => item !== name));
  }

  function buildBlueprintBody() {
    const version = versionQuery.data!;
    // Em rascunho, redistribui pesos iguais (somando 1.0) para o conjunto editado;
    // fora de rascunho, reenvia as competências como estão.
    const competencies = isDraft
      ? editedCompetencies.map((name) => {
          const existing = version.blueprint.competencies.find((item) => item.name === name);
          return {
            name,
            weight: editedCompetencies.length > 0 ? 1 / editedCompetencies.length : 1,
            targetScore: existing?.targetScore ?? null,
            tier: existing?.tier ?? "major",
          };
        })
      : version.blueprint.competencies;
    return {
      rootNodeId: version.blueprint.rootNodeId,
      competencies,
      criticalSituation: version.criticalSituation ?? null,
      resultUse: version.resultUse ?? null,
    };
  }

  const saveMutation = useMutation({
    mutationFn: (_options: { advance: boolean }) =>
      updateSimulationBlueprint(search.simulationId!, search.versionNumber!, buildBlueprintBody()),
    onSuccess: async (simulation, options) => {
      await queryClient.invalidateQueries({
        queryKey: ["simulation-version", search.simulationId, search.versionNumber],
      });
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
      if (options.advance) {
        void navigate({
          to: "/nova/personagem",
          search: { simulationId: simulation.id, versionNumber: simulation.versionNumber },
        });
      }
    },
  });

  const hasNoCompetencies = displayCompetencies.length === 0;

  return (
    <AppShell>
      <WizardStepper current="avaliacao" unlockedThrough="cenario" />
      <ScreenStateStrip blockedReason="selecione uma versão real para continuar" />
      <div className="mb-8">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 1</div>
        <h1 className="mt-1 font-display text-3xl">Objetivo do modelo base</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Esta etapa usa o modelo base salvo no sistema. Ajustes estruturais continuam em Nova
          avaliação.
        </p>
      </div>

      {!hasDraftContext ? (
        <EmptyState
          title="Escolha uma avaliação real"
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
                <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
                  {versionQuery.data.description}
                </p>
              </div>
              <StatusBadge status={versionQuery.data.status} />
            </div>
            <div className="mt-5 grid gap-3 md:grid-cols-2">
              <Info label="Primeira etapa" value={versionQuery.data.blueprint.rootNodeId} />
              <Info label="Versão" value={`v${versionQuery.data.versionNumber}`} />
            </div>
            <div className="mt-5">
              <div className="flex items-center justify-between gap-3">
                <h3 className="text-sm font-semibold">Competências</h3>
                {isDraft && (
                  <span className="text-xs text-muted-foreground">
                    Rascunho · edite as competências abaixo
                  </span>
                )}
              </div>
              <div className="mt-3 grid gap-2 md:grid-cols-3">
                {displayCompetencies.map((competency) => (
                  <div
                    key={competency}
                    className="flex items-center justify-between gap-2 rounded-md border border-border bg-background p-3"
                  >
                    <span className="text-sm font-medium">{competency}</span>
                    {isDraft && (
                      <button
                        type="button"
                        onClick={() => removeCompetency(competency)}
                        aria-label={`Remover ${competency}`}
                        className="rounded-md px-2 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                      >
                        ×
                      </button>
                    )}
                  </div>
                ))}
              </div>
              {isDraft && (
                <>
                  <div className="mt-3 grid gap-2 md:grid-cols-[1fr_auto]">
                    <input
                      className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                      placeholder="Adicionar competência"
                      maxLength={140}
                      value={newCompetency}
                      onChange={(event) => setNewCompetency(event.target.value)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter") {
                          event.preventDefault();
                          addCompetency();
                        }
                      }}
                    />
                    <button
                      type="button"
                      onClick={addCompetency}
                      disabled={!canAddCompetency}
                      className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      Adicionar
                    </button>
                  </div>
                  {hasNoCompetencies && (
                    <p className="mt-2 text-xs text-danger">
                      Mantenha ao menos uma competência para salvar.
                    </p>
                  )}
                </>
              )}
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
          {isDraft && saveMutation.isSuccess && !saveMutation.isPending && (
            <div className="mt-5">
              <StateBanner tone="info" title="Alterações salvas">
                As competências do rascunho foram atualizadas.
              </StateBanner>
            </div>
          )}
          <div className="mt-8 flex flex-row-reverse items-center justify-between gap-3">
            <div className="flex flex-row-reverse items-center gap-2">
              <button
                type="button"
                onClick={() => saveMutation.mutate({ advance: true })}
                disabled={saveMutation.isPending || hasNoCompetencies}
                className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {saveMutation.isPending ? "Salvando..." : "Personagem →"}
              </button>
              {isDraft && (
                <button
                  type="button"
                  onClick={() => saveMutation.mutate({ advance: false })}
                  disabled={saveMutation.isPending || hasNoCompetencies}
                  className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Salvar alterações
                </button>
              )}
            </div>
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
