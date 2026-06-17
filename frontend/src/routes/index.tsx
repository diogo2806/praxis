import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Archive, BarChart3, FilePlus2, Filter, PlayCircle, Search, Table2 } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import {
  EmptyState,
  ScreenStateStrip,
  SkeletonRows,
  StateBanner,
  StatusBadge,
} from "@/components/praxis-ui";
import {
  archiveSimulation,
  listSimulations,
  type SimulationSummaryResponse,
  type SimulationVersionStatus,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";
import { useSession } from "@/lib/session";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Painel - Praxis" },
      {
        name: "description",
        content: "Painel: simulacoes ativas, qualidade, maturidade e vinculo com vagas Gupy.",
      },
    ],
  }),
  component: Dashboard,
});

const filters: Array<"todas" | SimulationVersionStatus> = [
  "todas",
  "published",
  "approved",
  "inReview",
  "draft",
  "rejected",
  "archived",
];

const filterLabels: Record<(typeof filters)[number], string> = {
  todas: "todas",
  published: "publicadas",
  approved: "aprovadas",
  inReview: "em revisao",
  draft: "rascunhos",
  rejected: "reprovadas",
  archived: "arquivadas",
};

function Dashboard() {
  const [filter, setFilter] = useState<(typeof filters)[number]>("todas");
  const [query, setQuery] = useState("");
  const queryClient = useQueryClient();
  const session = useSession();
  const firstName = session.userName.trim().split(/\s+/)[0] || "bem-vindo";
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });
  const simulations = useMemo(() => simulationsQuery.data ?? [], [simulationsQuery.data]);
  const archiveMutation = useMutation({
    mutationFn: archiveSimulation,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
    },
  });

  const totals = {
    publicadas: simulations.filter((s) => s.status === "published").length,
    aprovadas: simulations.filter((s) => s.status === "approved").length,
    rascunhos: simulations.filter((s) => s.status === "draft").length,
    tentativas: simulations.reduce((a, s) => a + s.attemptsCreated, 0),
  };

  const filtered = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return simulations.filter((simulation) => {
      const byStatus = filter === "todas" || simulation.status === filter;
      const byQuery =
        normalizedQuery.length === 0 ||
        simulation.name.toLowerCase().includes(normalizedQuery) ||
        simulation.description.toLowerCase().includes(normalizedQuery);
      return byStatus && byQuery;
    });
  }, [filter, query, simulations]);

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="workspace sem permissao ou Gupy desconectada" />
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-muted-foreground">Painel</div>
          <h1 className="mt-1 text-3xl font-semibold text-foreground">Boa tarde, {firstName}.</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Avaliacao situacional estruturada para recrutamento, com score por rubrica, decisao em
            contexto e trilha auditavel.
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            to="/monitoramento"
            className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
          >
            <BarChart3 className="h-4 w-4" />
            Monitoramento
          </Link>
          <Link
            to="/nova/avaliacao"
            className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
          >
            <PlayCircle className="h-4 w-4" />
            Fluxo guiado
          </Link>
          <Link
            to="/nova/avaliacao"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            <FilePlus2 className="h-4 w-4" />
            Nova simulacao
          </Link>
        </div>
      </div>

      {simulationsQuery.isLoading ? (
        <section className="rounded-md border border-border bg-card p-4">
          <SkeletonRows rows={5} />
        </section>
      ) : simulationsQuery.isError ? (
        <StateBanner tone="danger" title="Nao foi possivel carregar as simulacoes">
          {simulationsQuery.error instanceof Error
            ? simulationsQuery.error.message
            : "Verifique se o backend esta rodando."}
        </StateBanner>
      ) : simulations.length === 0 ? (
        <EmptyState
          title="Nenhuma simulacao cadastrada"
          description="O painel depende do backend. Crie ou importe uma simulacao para que ela apareca aqui."
          actions={
            <>
              <Link
                to="/nova/avaliacao"
                className="inline-flex items-center justify-between rounded-md border border-primary bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                Criar primeira simulacao
                <FilePlus2 className="h-4 w-4" />
              </Link>
              <Link
                to="/nova/avaliacao"
                className="inline-flex items-center justify-between rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
              >
                Abrir fluxo de cadastro
                <Table2 className="h-4 w-4" />
              </Link>
            </>
          }
        />
      ) : (
        <div className="space-y-6">
          {archiveMutation.isError && (
            <StateBanner tone="danger" title="Nao foi possivel arquivar a simulacao">
              {archiveMutation.error instanceof Error
                ? archiveMutation.error.message
                : "Tente novamente."}
            </StateBanner>
          )}
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Stat
              label="Publicadas"
              value={totals.publicadas}
              hint="Em vagas ativas"
              onClick={() => setFilter("published")}
            />
            <Stat
              label="Aprovadas"
              value={totals.aprovadas}
              hint="Prontas para publicar"
              onClick={() => setFilter("approved")}
            />
            <Stat
              label="Rascunhos"
              value={totals.rascunhos}
              hint="Em construcao"
              onClick={() => setFilter("draft")}
            />
            <Stat
              label="Tentativas"
              value={totals.tentativas}
              hint="Total registrado"
              onClick={() => setFilter("todas")}
            />
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-xl font-semibold">Simulacoes</h2>
              <p className="text-xs text-muted-foreground">
                Status tecnico e maturidade aparecem juntos em todas as linhas.
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
                  placeholder="Buscar simulacao"
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
              title="Nenhuma simulacao neste filtro"
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
            <div className="overflow-hidden rounded-md border border-border bg-card">
              <table className="w-full text-sm">
                <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium">Simulacao</th>
                    <th className="px-4 py-3 text-left font-medium">Estado</th>
                    <th className="px-4 py-3 text-left font-medium">Conclusao</th>
                    <th className="px-4 py-3 text-left font-medium">Versao</th>
                    <th className="px-4 py-3 text-left font-medium">Tentativas</th>
                    <th className="px-4 py-3" />
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((simulation) => (
                    <tr
                      key={simulation.id}
                      className="border-b border-border last:border-0 hover:bg-accent/35"
                    >
                      <td className="px-4 py-3">
                        <div className="font-medium text-foreground">{simulation.name}</div>
                        <div className="text-xs text-muted-foreground">
                          {simulation.description} - atualizada{" "}
                          {formatDateTime(simulation.updatedAt)}
                        </div>
                        <div className="mt-1 flex flex-wrap gap-1">
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
                        <StatusBadge
                          status={simulation.status}
                          maturity={maturityForStatus(simulation.status)}
                        />
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <div className="h-1.5 w-24 overflow-hidden rounded-full bg-muted">
                            <div
                              className={cn(
                                "h-full rounded-full",
                                simulation.completionRatePercent >= 80
                                  ? "bg-success"
                                  : simulation.completionRatePercent >= 60
                                    ? "bg-warning"
                                    : "bg-danger",
                              )}
                              style={{ width: `${simulation.completionRatePercent}%` }}
                            />
                          </div>
                          <span className="text-xs font-medium tabular-nums">
                            {formatPercent(simulation.completionRatePercent)}
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-xs tabular-nums text-muted-foreground">
                        v{simulation.versionNumber}
                      </td>
                      <td className="px-4 py-3 text-xs tabular-nums">
                        {simulation.attemptsCreated.toLocaleString("pt-BR")}
                        <div className="text-[10px] text-muted-foreground">
                          {simulation.attemptsCompleted.toLocaleString("pt-BR")} concluidas
                        </div>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex justify-end gap-3">
                          <button
                            type="button"
                            onClick={() => {
                              if (
                                window.confirm(
                                  `Arquivar "${simulation.name}"? Ela sairá do painel ativo.`,
                                )
                              ) {
                                archiveMutation.mutate(simulation.id);
                              }
                            }}
                            disabled={archiveMutation.isPending}
                            className="inline-flex items-center gap-1 text-xs font-medium text-danger hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            <Archive className="h-3.5 w-3.5" />
                            Arquivar
                          </button>
                          <Link
                            to="/nova/revisao"
                            search={simulationSearch(simulation)}
                            className="text-xs font-medium text-primary hover:underline"
                          >
                            Abrir
                          </Link>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </AppShell>
  );
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
