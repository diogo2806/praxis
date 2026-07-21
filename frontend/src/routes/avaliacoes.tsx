import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Archive, Copy, ExternalLink, FilePlus2, PlayCircle, Search, Target } from "lucide-react";
import { useMemo, useState, type FormEvent } from "react";

import { AppShell } from "@/components/app-shell";
import { EmptyState, SkeletonRows, StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { isRestrictedPartnerSpecialist } from "@/lib/access-control";
import { archiveSimulation } from "@/lib/api/archive-simulation";
import {
  listSimulations,
  type SimulationSummaryResponse,
  type SimulationVersionStatus,
} from "@/lib/api/praxis";
import { duplicateSimulation } from "@/lib/api/simulation-duplicates";
import { useSession } from "@/lib/session";

export const Route = createFileRoute("/avaliacoes")({
  head: () => ({
    meta: [
      { title: "Avaliações - Práxis" },
      {
        name: "description",
        content: "Crie e mantenha o conteúdo que será usado nas jornadas de avaliação.",
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
  todas: "Todas",
  published: "Publicadas",
  draft: "Rascunhos",
  archived: "Arquivadas",
};

function AvaliacoesPage() {
  const session = useSession();
  const specialistAccess = isRestrictedPartnerSpecialist(session.roles);
  const [filter, setFilter] = useState<(typeof filters)[number]>("todas");
  const [query, setQuery] = useState("");
  const [duplicatedName, setDuplicatedName] = useState<string | null>(null);
  const [duplicateTarget, setDuplicateTarget] = useState<SimulationSummaryResponse | null>(null);
  const [duplicateName, setDuplicateName] = useState("");
  const [archiveTarget, setArchiveTarget] = useState<SimulationSummaryResponse | null>(null);
  const queryClient = useQueryClient();

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });

  const archiveMutation = useMutation({
    mutationFn: archiveSimulation,
    onSuccess: async () => {
      setArchiveTarget(null);
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
    },
  });

  const duplicateMutation = useMutation({
    mutationFn: ({ simulation, name }: { simulation: SimulationSummaryResponse; name: string }) =>
      duplicateSimulation(simulation.id, simulation.versionNumber, name),
    onSuccess: async (_created, variables) => {
      setDuplicatedName(variables.name);
      setDuplicateTarget(null);
      setDuplicateName("");
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
    },
  });

  const simulations = useMemo(() => simulationsQuery.data ?? [], [simulationsQuery.data]);
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

  function requestDuplicate(simulation: SimulationSummaryResponse) {
    if (specialistAccess) return;
    duplicateMutation.reset();
    setDuplicatedName(null);
    setDuplicateName(`${simulation.name} - cópia`);
    setDuplicateTarget(simulation);
  }

  function confirmDuplicate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!duplicateTarget) return;
    const name = duplicateName.trim();
    if (!name) return;
    duplicateMutation.mutate({ simulation: duplicateTarget, name });
  }

  function requestArchive(simulation: SimulationSummaryResponse) {
    if (specialistAccess || simulation.status === "archived") return;
    archiveMutation.reset();
    setArchiveTarget(simulation);
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-7xl space-y-5">
        <header className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-semibold text-foreground">Avaliações</h1>
            <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
              {specialistAccess
                ? "Crie, edite e revise avaliações. Publicação, duplicação, arquivamento e comparação ficam sob responsabilidade da empresa."
                : "Aqui fica o conteúdo dos testes. A aplicação aos participantes acontece em Jornadas."}
            </p>
          </div>
          <Link
            to="/nova/avaliacao"
            className="inline-flex min-h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            <FilePlus2 className="h-4 w-4" />
            Nova avaliação
          </Link>
        </header>

        {!specialistAccess && archiveMutation.isError && !archiveTarget && (
          <StateBanner tone="danger" title="Não foi possível arquivar a avaliação">
            {archiveMutation.error instanceof Error ? archiveMutation.error.message : "Tente novamente."}
          </StateBanner>
        )}

        {!specialistAccess && duplicateMutation.isError && !duplicateTarget && (
          <StateBanner tone="danger" title="Não foi possível duplicar a avaliação">
            {duplicateMutation.error instanceof Error
              ? duplicateMutation.error.message
              : "Tente novamente sem alterar a avaliação de origem."}
          </StateBanner>
        )}

        {!specialistAccess && duplicatedName && (
          <StateBanner tone="ok" title="Avaliação reutilizada com segurança">
            “{duplicatedName}” foi criada como um novo rascunho independente. A avaliação de origem
            permanece inalterada.
          </StateBanner>
        )}

        {simulationsQuery.isLoading ? (
          <section className="rounded-md border border-border bg-card p-4">
            <SkeletonRows rows={5} />
          </section>
        ) : simulationsQuery.isError ? (
          <StateBanner
            tone="danger"
            title="Não foi possível carregar as avaliações"
            action={
              <button
                type="button"
                onClick={() => simulationsQuery.refetch()}
                className="rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium"
              >
                Tentar novamente
              </button>
            }
          >
            {simulationsQuery.error instanceof Error
              ? simulationsQuery.error.message
              : "Verifique se o sistema está disponível e tente novamente."}
          </StateBanner>
        ) : simulations.length === 0 ? (
          <EmptyState
            title="Nenhuma avaliação cadastrada"
            description={
              specialistAccess
                ? "Crie a primeira avaliação e envie para revisão e publicação pela empresa."
                : "Crie o primeiro teste e publique-o antes de adicioná-lo a uma jornada."
            }
            actions={
              <Link
                to="/nova/avaliacao"
                className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                <FilePlus2 className="h-4 w-4" />
                Criar avaliação
              </Link>
            }
          />
        ) : (
          <>
            <section className="flex flex-wrap items-center justify-between gap-3 rounded-md border border-border bg-card p-4">
              <div className="flex min-w-0 flex-1 flex-wrap gap-2">
                <label className="relative min-w-[220px] flex-1 sm:max-w-sm">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <input
                    type="search"
                    name="simulation-search"
                    autoComplete="off"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    className="input w-full pl-9"
                    placeholder="Buscar avaliação"
                  />
                </label>
                <select
                  value={filter}
                  onChange={(event) => setFilter(event.target.value as (typeof filters)[number])}
                  className="input min-w-[170px]"
                  aria-label="Filtrar avaliações por situação"
                >
                  {filters.map((item) => (
                    <option key={item} value={item}>
                      {filterLabels[item]}
                    </option>
                  ))}
                </select>
              </div>
              <div className="text-sm text-muted-foreground" aria-live="polite">
                {filtered.length.toLocaleString("pt-BR")} de {simulations.length.toLocaleString("pt-BR")}
              </div>
            </section>

            {filtered.length === 0 ? (
              <EmptyState
                title="Nenhuma avaliação encontrada"
                description="Altere a busca ou o filtro selecionado."
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
              <section className="overflow-hidden rounded-md border border-border bg-card">
                <div className="overflow-x-auto">
                  <TooltipProvider delayDuration={150}>
                    <table className="w-full min-w-[820px] text-sm">
                      <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
                        <tr>
                          <th className="px-4 py-3 text-left font-medium">Avaliação</th>
                          <th className="px-4 py-3 text-left font-medium">Situação</th>
                          <th className="px-4 py-3 text-left font-medium">Uso</th>
                          <th className="px-4 py-3 text-right font-medium">Ações</th>
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
                              <p className="mt-1 max-w-2xl text-xs text-muted-foreground">
                                {simulation.description || "Sem descrição."}
                              </p>
                              <div className="mt-1 text-[11px] text-muted-foreground">
                                {simulation.competencies.length}{" "}
                                {simulation.competencies.length === 1 ? "competência" : "competências"} ·
                                atualizada {formatDateTime(simulation.updatedAt)}
                              </div>
                            </td>
                            <td className="px-4 py-3">
                              <StatusBadge status={simulation.status} variant="status" />
                              {hasLiveVersionBehind(simulation) && (
                                <span className="mt-1 inline-flex items-center gap-1 rounded-md border border-success/40 bg-success/10 px-1.5 py-0.5 text-[10px] font-medium text-success">
                                  <PlayCircle className="h-3 w-3" />v
                                  {simulation.livePublishedVersionNumber} publicada
                                </span>
                              )}
                              <div className="mt-1 text-xs text-muted-foreground">
                                {simulationStatusDetail(simulation)}
                              </div>
                            </td>
                            <td className="px-4 py-3 text-xs tabular-nums">
                              <div>{simulation.attemptsCreated.toLocaleString("pt-BR")} aplicações</div>
                              <div className="mt-1 text-muted-foreground">
                                {simulation.attemptsCompleted.toLocaleString("pt-BR")} concluídas
                              </div>
                            </td>
                            <td className="px-4 py-3 text-right">
                              <div className="inline-flex gap-1">
                                {!specialistAccess && (
                                  <>
                                    <Tooltip>
                                      <TooltipTrigger asChild>
                                        <button
                                          type="button"
                                          aria-label={`Duplicar ${simulation.name}`}
                                          onClick={() => requestDuplicate(simulation)}
                                          disabled={duplicateMutation.isPending}
                                          className="inline-flex h-9 w-9 items-center justify-center rounded-md border border-border bg-background text-primary hover:bg-primary/10 disabled:cursor-not-allowed disabled:opacity-50"
                                        >
                                          <Copy className="h-4 w-4" />
                                        </button>
                                      </TooltipTrigger>
                                      <TooltipContent>
                                        Duplicar como novo rascunho reutilizável
                                      </TooltipContent>
                                    </Tooltip>
                                    <Tooltip>
                                      <TooltipTrigger asChild>
                                        <button
                                          type="button"
                                          aria-label={`Arquivar ${simulation.name}`}
                                          onClick={() => requestArchive(simulation)}
                                          disabled={
                                            archiveMutation.isPending || simulation.status === "archived"
                                          }
                                          className="inline-flex h-9 w-9 items-center justify-center rounded-md border border-border bg-background text-warning hover:bg-warning/10 disabled:cursor-not-allowed disabled:opacity-50"
                                        >
                                          <Archive className="h-4 w-4" />
                                        </button>
                                      </TooltipTrigger>
                                      <TooltipContent>
                                        Arquivar sem apagar o histórico
                                      </TooltipContent>
                                    </Tooltip>
                                  </>
                                )}
                                <Tooltip>
                                  <TooltipTrigger asChild>
                                    <Link
                                      to="/nova/validador"
                                      search={simulationSearch(simulation)}
                                      aria-label={`Abrir ${simulation.name}`}
                                      className="inline-flex h-9 w-9 items-center justify-center rounded-md border border-border bg-background text-primary hover:bg-primary/10"
                                    >
                                      <ExternalLink className="h-4 w-4" />
                                    </Link>
                                  </TooltipTrigger>
                                  <TooltipContent>Abrir avaliação</TooltipContent>
                                </Tooltip>
                                {!specialistAccess && (
                                  <Tooltip>
                                    <TooltipTrigger asChild>
                                      <Link
                                        to="/talent-match"
                                        search={simulationSearch(simulation)}
                                        aria-label={`Comparar participações de ${simulation.name}`}
                                        className="inline-flex h-9 w-9 items-center justify-center rounded-md border border-border bg-background text-primary hover:bg-primary/10"
                                      >
                                        <Target className="h-4 w-4" />
                                      </Link>
                                    </TooltipTrigger>
                                    <TooltipContent>Comparar participações</TooltipContent>
                                  </Tooltip>
                                )}
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </TooltipProvider>
                </div>
              </section>
            )}
          </>
        )}

        <Dialog
          open={duplicateTarget != null}
          onOpenChange={(open) => {
            if (!open && !duplicateMutation.isPending) {
              setDuplicateTarget(null);
              setDuplicateName("");
              duplicateMutation.reset();
            }
          }}
        >
          <DialogContent>
            <form onSubmit={confirmDuplicate} className="space-y-5">
              <DialogHeader>
                <DialogTitle>Duplicar avaliação</DialogTitle>
                <DialogDescription>
                  Será criado um novo rascunho independente. A avaliação de origem e seu histórico
                  não serão alterados.
                </DialogDescription>
              </DialogHeader>

              <label htmlFor="duplicate-simulation-name" className="block">
                <span className="mb-1.5 block text-sm font-medium text-foreground">
                  Nome da nova avaliação
                </span>
                <input
                  id="duplicate-simulation-name"
                  value={duplicateName}
                  onChange={(event) => setDuplicateName(event.target.value)}
                  autoFocus
                  required
                  maxLength={180}
                  className="input w-full"
                  aria-describedby="duplicate-simulation-source"
                />
              </label>
              <p id="duplicate-simulation-source" className="text-xs text-muted-foreground">
                Origem: {duplicateTarget?.name ?? "-"} · versão{" "}
                {duplicateTarget?.versionNumber ?? "-"}
              </p>

              {duplicateMutation.isError && (
                <p role="alert" className="text-sm text-danger">
                  {duplicateMutation.error instanceof Error
                    ? duplicateMutation.error.message
                    : "Não foi possível criar o novo rascunho."}
                </p>
              )}

              <DialogFooter>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setDuplicateTarget(null);
                    setDuplicateName("");
                    duplicateMutation.reset();
                  }}
                  disabled={duplicateMutation.isPending}
                >
                  Cancelar
                </Button>
                <Button
                  type="submit"
                  disabled={duplicateMutation.isPending || !duplicateName.trim()}
                >
                  {duplicateMutation.isPending ? "Duplicando..." : "Criar rascunho"}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>

        <AlertDialog
          open={archiveTarget != null}
          onOpenChange={(open) => {
            if (!open && !archiveMutation.isPending) {
              setArchiveTarget(null);
              archiveMutation.reset();
            }
          }}
        >
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Arquivar avaliação?</AlertDialogTitle>
              <AlertDialogDescription>
                “{archiveTarget?.name ?? "Esta avaliação"}” ficará fora de uso, mas versões,
                aplicações, resultados e auditoria serão preservados.
              </AlertDialogDescription>
            </AlertDialogHeader>

            {archiveMutation.isError && (
              <p role="alert" className="text-sm text-danger">
                {archiveMutation.error instanceof Error
                  ? archiveMutation.error.message
                  : "Não foi possível arquivar a avaliação."}
              </p>
            )}

            <AlertDialogFooter>
              <AlertDialogCancel disabled={archiveMutation.isPending}>Voltar</AlertDialogCancel>
              <AlertDialogAction
                disabled={archiveMutation.isPending || !archiveTarget}
                onClick={(event) => {
                  event.preventDefault();
                  if (archiveTarget) {
                    archiveMutation.mutate(archiveTarget.id);
                  }
                }}
                className="bg-warning text-warning-foreground hover:bg-warning/90"
              >
                {archiveMutation.isPending ? "Arquivando..." : "Confirmar arquivamento"}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </AppShell>
  );
}

function isLive(simulation: SimulationSummaryResponse) {
  return simulation.status === "published" || simulation.livePublishedVersionNumber != null;
}

function hasLiveVersionBehind(simulation: SimulationSummaryResponse) {
  return simulation.status !== "published" && simulation.livePublishedVersionNumber != null;
}

function simulationStatusDetail(simulation: SimulationSummaryResponse) {
  if (simulation.status === "archived") return "Fora de uso; histórico preservado";
  if (simulation.status === "published") return "Disponível para adicionar a jornadas";
  return `${formatPercent(simulation.completionRatePercent)} configurada`;
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
