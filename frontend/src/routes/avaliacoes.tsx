import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState, type CSSProperties } from "react";
import {
  Archive,
  CircleHelp,
  ExternalLink,
  FilePlus2,
  Filter,
  PlayCircle,
  Search,
  Target,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Termo } from "@/components/glossario";
import { EmptyState, SkeletonRows, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import {
  listSimulations,
  type SimulationSummaryResponse,
  type SimulationVersionStatus,
} from "@/lib/api/praxis";
import { archiveSimulation } from "@/lib/api/archive-simulation";
import { maturityForStatus } from "@/lib/simulation-meta";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/avaliacoes")({
  head: () => ({
    meta: [
      { title: "Avaliações - Práxis" },
      {
        name: "description",
        content:
          "Veja e edite as avaliações da sua empresa: status, competências, prontidão para publicação e tentativas registradas.",
      },
    ],
  }),
  component: AvaliacoesPage,
});

const filters: Array<"todas" | SimulationVersionStatus> = [
  "todas",
  "published",
  "draft",
  "archived",
];

const filterLabels: Record<(typeof filters)[number], string> = {
  todas: "todas",
  published: "no ar",
  draft: "rascunhos",
  archived: "arquivadas",
};

function AvaliacoesPage() {
  const [filter, setFilter] = useState<(typeof filters)[number]>("todas");
  const [query, setQuery] = useState("");
  const queryClient = useQueryClient();
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });
  const archiveMutation = useMutation({
    mutationFn: archiveSimulation,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
    },
  });

  const simulations = useMemo(() => simulationsQuery.data ?? [], [simulationsQuery.data]);
  const totals = {
    noAr: simulations.filter(isLive).length,
    rascunhos: simulations.filter((simulation) => simulation.status === "draft").length,
    arquivadas: simulations.filter((simulation) => simulation.status === "archived").length,
    tentativas: simulations.reduce((total, simulation) => total + simulation.attemptsCreated, 0),
  };

  const filtered = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return simulations.filter((simulation) => {
      const byStatus =
        filter === "todas" ||
        simulation.status === filter ||
        (filter === "published" && isLive(simulation));
      const byQuery =
        normalizedQuery.length === 0 ||
        simulation.name.toLowerCase().includes(normalizedQuery) ||
        simulation.description.toLowerCase().includes(normalizedQuery);
      return byStatus && byQuery;
    });
  }, [filter, query, simulations]);

  return (
    <AppShell>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold text-foreground">Avaliações</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Consulte e edite as avaliações da sua empresa, com{" "}
            <Termo id="pontuacao-criterios">pontuação por critérios definidos</Termo> e{" "}
            <Termo id="trilha-auditavel">histórico completo de alterações</Termo>.
          </p>
        </div>
        <Link
          to="/comecar"
          className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <FilePlus2 className="h-4 w-4" />
          Nova avaliação
        </Link>
      </div>

      {simulationsQuery.isLoading ? (
        <section className="rounded-md border border-border bg-card p-4">
          <SkeletonRows rows={5} />
        </section>
      ) : simulationsQuery.isError ? (
        <StateBanner tone="danger" title="Não foi possível carregar as avaliações">
          {simulationsQuery.error instanceof Error
            ? simulationsQuery.error.message
            : "Verifique se o sistema está disponível e tente novamente."}
        </StateBanner>
      ) : simulations.length === 0 ? (
        <EmptyState
          title="Nenhuma avaliação cadastrada"
          description="Crie uma avaliação para que ela apareça aqui e possa ser acompanhada pelo Dashboard."
          actions={
            <Link
              to="/comecar"
              className="inline-flex items-center justify-between rounded-md border border-primary bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Começar agora
              <FilePlus2 className="h-4 w-4" />
            </Link>
          }
        />
      ) : (
        <div className="space-y-6">
          {archiveMutation.isError && (
            <StateBanner tone="danger" title="Não foi possível arquivar a avaliação">
              {archiveMutation.error instanceof Error ? archiveMutation.error.message : "Tente novamente."}
            </StateBanner>
          )}

          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Stat label="No ar" value={totals.noAr} hint="Em aplicações ativas" onClick={() => setFilter("published")} />
            <Stat label="Arquivadas" value={totals.arquivadas} hint="Fora de uso, com histórico preservado" onClick={() => setFilter("archived")} />
            <Stat label="Rascunhos" value={totals.rascunhos} hint="Em construção" onClick={() => setFilter("draft")} />
            <Stat label="Tentativas" value={totals.tentativas} hint="Total registrado" onClick={() => setFilter("todas")} />
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-xl font-semibold">Avaliações</h2>
              <p className="text-xs text-muted-foreground">
                O arquivamento substitui a exclusão definitiva: o teste sai de uso, mas evidências, versões e auditoria permanecem preservadas.
              </p>
            </div>
            <div className="flex min-w-0 flex-wrap gap-2">
              <label className="relative">
                <Search className="pointer-events-none absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <input
                  type="search"
                  name="simulation-search"
                  autoComplete="off"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  className="input w-64 pl-8"
                  placeholder="Buscar avaliação"
                />
              </label>
              <div className="inline-flex flex-wrap gap-1 rounded-md border border-border bg-card p-1">
                <Filter className="m-1.5 h-4 w-4 text-muted-foreground" />
                {filters.map((item) => (
                  <button
                    key={item}
                    type="button"
                    onClick={() => setFilter(item)}
                    className={cn(
                      "rounded px-2 py-1 text-xs capitalize hover:bg-accent",
                      filter === item && "bg-primary text-primary-foreground hover:bg-primary",
                    )}
                  >
                    {filterLabels[item]}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {filtered.length === 0 ? (
            <EmptyState
              title="Nenhuma avaliação neste filtro"
              description="Ajuste busca, limpe o filtro ou crie um novo rascunho."
              actions={
                <button
                  type="button"
                  onClick={() => {
                    setFilter("todas");
                    setQuery("");
                  }}
                  className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
                >
                  Limpar filtros
                </button>
              }
            />
          ) : (
            <div className="overflow-x-auto">
              <TooltipProvider delayDuration={150}>
                <table className="w-full text-sm">
                  <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
                    <tr>
                      <th className="px-4 py-3 text-left font-medium">Avaliação</th>
                      <th className="px-4 py-3 text-left font-medium">Competências</th>
                      <th className="px-4 py-3 text-left font-medium">Status</th>
                      <th className="px-4 py-3 text-left font-medium">Prontidão</th>
                      <th className="px-4 py-3 text-left font-medium">Conclusão</th>
                      <th className="px-4 py-3 text-left font-medium">Tentativas</th>
                      <th className="px-4 py-3 text-right font-medium">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.map((simulation) => (
                      <tr key={simulation.id} className="border-b border-border last:border-0 hover:bg-accent/35">
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <div className="font-medium text-foreground">{simulation.name}</div>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <button
                                  type="button"
                                  aria-label={`Descrição de ${simulation.name}`}
                                  className="inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-md border border-border bg-background text-muted-foreground hover:bg-accent hover:text-foreground"
                                >
                                  <CircleHelp className="h-3.5 w-3.5" />
                                </button>
                              </TooltipTrigger>
                              <TooltipContent className="max-w-sm text-left leading-snug">
                                {simulation.description}
                              </TooltipContent>
                            </Tooltip>
                          </div>
                          <div className="mt-1 text-xs text-muted-foreground">
                            Atualizada {formatDateTime(simulation.updatedAt)}
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex max-w-[220px] flex-wrap gap-1">
                            {simulation.competencies.map((competency) => (
                              <span
                                key={competency}
                                className="rounded-md border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground"
                              >
                                {competency}
                              </span>
                            ))}
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <StatusBadge status={simulation.status} variant="status" />
                          {hasLiveVersionBehind(simulation) && (
                            <span className="mt-1 inline-flex items-center gap-1 rounded-md border border-success/40 bg-success/10 px-1.5 py-0.5 text-[10px] font-medium text-success">
                              <PlayCircle className="h-3 w-3" />v{simulation.livePublishedVersionNumber} no ar
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <StatusBadge
                            status={simulation.status}
                            maturity={maturityForStatus(simulation.status)}
                            variant="maturity"
                          />
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <div className="praxis-progress-track praxis-progress-track-sm w-24">
                              <div
                                className={cn(
                                  "praxis-progress-fill",
                                  simulation.completionRatePercent >= 80
                                    ? "bg-success"
                                    : simulation.completionRatePercent >= 60
                                      ? "bg-warning"
                                      : "bg-danger",
                                )}
                                style={
                                  {
                                    "--praxis-progress-value": `${simulation.completionRatePercent}%`,
                                  } as CSSProperties
                                }
                              />
                            </div>
                            <span className="text-xs font-medium tabular-nums">
                              {formatPercent(simulation.completionRatePercent)}
                            </span>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-xs tabular-nums">
                          {simulation.attemptsCreated.toLocaleString("pt-BR")}
                          <div className="text-[10px] text-muted-foreground">
                            {simulation.attemptsCompleted.toLocaleString("pt-BR")} concluídas
                          </div>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <div className="inline-flex gap-1">
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <button
                                  type="button"
                                  aria-label={`Arquivar ${simulation.name}`}
                                  onClick={() => {
                                    if (window.confirm(`Arquivar "${simulation.name}"? O histórico será preservado.`)) {
                                      archiveMutation.mutate(simulation.id);
                                    }
                                  }}
                                  disabled={archiveMutation.isPending || simulation.status === "archived"}
                                  className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-border bg-background text-warning hover:bg-warning/10 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                  <Archive className="h-3.5 w-3.5" />
                                </button>
                              </TooltipTrigger>
                              <TooltipContent>Arquivar sem apagar histórico</TooltipContent>
                            </Tooltip>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Link
                                  to="/nova/validador"
                                  search={simulationSearch(simulation)}
                                  aria-label={`Abrir ${simulation.name}`}
                                  className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-border bg-background text-primary hover:bg-primary/10"
                                >
                                  <ExternalLink className="h-3.5 w-3.5" />
                                </Link>
                              </TooltipTrigger>
                              <TooltipContent>Abrir</TooltipContent>
                            </Tooltip>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Link
                                  to="/talent-match"
                                  search={simulationSearch(simulation)}
                                  aria-label={`Comparar participações de ${simulation.name}`}
                                  className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-border bg-background text-primary hover:bg-primary/10"
                                >
                                  <Target className="h-3.5 w-3.5" />
                                </Link>
                              </TooltipTrigger>
                              <TooltipContent>Comparar participações</TooltipContent>
                            </Tooltip>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </TooltipProvider>
            </div>
          )}
        </div>
      )}
    </AppShell>
  );
}

function isLive(simulation: SimulationSummaryResponse) {
  return simulation.status === "published" || simulation.livePublishedVersionNumber != null;
}

function hasLiveVersionBehind(simulation: SimulationSummaryResponse) {
  return simulation.status !== "published" && simulation.livePublishedVersionNumber != null;
}

function simulationSearch(simulation: SimulationSummaryResponse) {
  return {
    simulationId: simulation.id,
    versionNumber: simulation.versionNumber,
  };
}

function formatPercent(value: number) {
  return `${Number.isFinite(value) ? value.toFixed(value % 1 === 0 ? 0 : 1) : "0"}%`;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function Stat({
  label,
  value,
  hint,
  onClick,
}: {
  label: string;
  value: number;
  hint: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="rounded-md border border-border bg-card p-4 text-left hover:bg-accent"
    >
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="mt-1 text-3xl font-semibold tabular-nums text-foreground">
        {value.toLocaleString("pt-BR")}
      </div>
      <div className="mt-1 text-[11px] text-muted-foreground">{hint}</div>
    </button>
  );
}
