import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  ArrowLeft,
  Download,
  Eye,
  Filter,
  FolderOpen,
  Layers3,
  Plus,
  RefreshCw,
  Save,
  Tag,
} from "lucide-react";
import { useMemo, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  createParticipationBulkJob,
  createParticipationTag,
  createSavedView,
  downloadParticipationBulkReport,
  listParticipationBulkJobs,
  listParticipationTags,
  listSavedViews,
  previewParticipationBulk,
  type BulkAction,
  type BulkFilter,
  type BulkPreview,
  type BulkPreviewRequest,
  type ParticipationRef,
  type SavedView,
  type SelectionMode,
} from "@/lib/api/participation-operations";
import {
  searchParticipations,
  type ParticipationMonitoringItem,
} from "@/lib/api/participations";
import { listSimulations } from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/participacoes/operacoes-em-lote")({
  head: () => ({
    meta: [
      { title: "Operações em lote - Práxis" },
      {
        name: "description",
        content: "Salve visões, aplique tags e processe participações em lote com prévia e auditoria.",
      },
    ],
  }),
  component: ParticipationBulkOperationsPage,
});

type ProcessFilter = "all" | "waiting" | "active" | "completed" | "attention";
type LinkFilter = "all" | "active" | "expiringSoon" | "expired" | "canceled";

type FiltersState = {
  simulationId: string;
  candidate: string;
  processStatus: ProcessFilter;
  linkStatus: LinkFilter;
  attention: boolean;
};

const DEFAULT_FILTERS: FiltersState = {
  simulationId: "",
  candidate: "",
  processStatus: "all",
  linkStatus: "all",
  attention: false,
};

const ACTIONS: Array<{ value: BulkAction; label: string }> = [
  { value: "RESEND", label: "Reenviar convite" },
  { value: "EXTEND", label: "Estender validade" },
  { value: "CANCEL", label: "Cancelar jornadas" },
  { value: "ADD_TAG", label: "Adicionar tag" },
  { value: "REMOVE_TAG", label: "Remover tag" },
  { value: "EXPORT", label: "Exportar relatório" },
];

function ParticipationBulkOperationsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<FiltersState>(DEFAULT_FILTERS);
  const [selectionMode, setSelectionMode] = useState<SelectionMode>("EXPLICIT");
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [action, setAction] = useState<BulkAction>("RESEND");
  const [additionalDays, setAdditionalDays] = useState(7);
  const [tagId, setTagId] = useState("");
  const [justification, setJustification] = useState("");
  const [preview, setPreview] = useState<BulkPreview | null>(null);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [tagDialogOpen, setTagDialogOpen] = useState(false);
  const [selectedViewId, setSelectedViewId] = useState("");
  const [message, setMessage] = useState<string | null>(null);

  const participationQuery = useQuery({
    queryKey: ["participation-bulk-source", page, filters.simulationId, filters.candidate],
    queryFn: () =>
      searchParticipations({
        page,
        size: 25,
        simulationId: filters.simulationId || undefined,
        candidate: filters.candidate.trim() || undefined,
      }),
    retry: false,
  });

  const viewsQuery = useQuery({
    queryKey: ["participation-saved-views"],
    queryFn: listSavedViews,
    retry: false,
  });
  const tagsQuery = useQuery({
    queryKey: ["participation-tags"],
    queryFn: listParticipationTags,
    retry: false,
  });
  const jobsQuery = useQuery({
    queryKey: ["participation-bulk-jobs"],
    queryFn: listParticipationBulkJobs,
    retry: false,
    refetchInterval: (query) =>
      query.state.data?.some((job) => job.status === "PENDING" || job.status === "RUNNING")
        ? 2_000
        : false,
  });
  const simulationsQuery = useQuery({ queryKey: ["simulations"], queryFn: listSimulations, retry: false });

  const visibleParticipations = useMemo(
    () => (participationQuery.data?.items ?? []).filter((item) => matchesFilters(item, filters)),
    [participationQuery.data?.items, filters],
  );
  const currentPageKeys = visibleParticipations.map(participationKey);
  const allCurrentPageSelected =
    currentPageKeys.length > 0 && currentPageKeys.every((key) => selected.has(key));

  const previewMutation = useMutation({
    mutationFn: previewParticipationBulk,
    onSuccess: setPreview,
  });
  const createJobMutation = useMutation({
    mutationFn: createParticipationBulkJob,
    onSuccess: async (job) => {
      setPreview(null);
      setSelected(new Set());
      setSelectionMode("EXPLICIT");
      setMessage(`Lote criado com ${job.totalItems} item(ns).`);
      await queryClient.invalidateQueries({ queryKey: ["participation-bulk-jobs"] });
    },
  });

  const currentRequest = buildRequest({
    action,
    selectionMode,
    selected,
    visibleParticipations,
    filters,
    additionalDays,
    tagId,
    justification,
  });

  function toggleCurrentPage() {
    setSelectionMode("EXPLICIT");
    setSelected((current) => {
      const next = new Set(current);
      if (allCurrentPageSelected) {
        currentPageKeys.forEach((key) => next.delete(key));
      } else {
        currentPageKeys.forEach((key) => next.add(key));
      }
      return next;
    });
  }

  function toggleParticipation(item: ParticipationMonitoringItem) {
    setSelectionMode("EXPLICIT");
    const key = participationKey(item);
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  }

  function applyView(view: SavedView) {
    const viewFilters = view.filters as Partial<FiltersState>;
    setFilters({
      simulationId: stringValue(viewFilters.simulationId),
      candidate: stringValue(viewFilters.candidate),
      processStatus: processFilterValue(viewFilters.processStatus),
      linkStatus: linkFilterValue(viewFilters.linkStatus),
      attention: Boolean(viewFilters.attention),
    });
    setSelectedViewId(view.id);
    setPage(0);
    setSelected(new Set());
    setSelectionMode("EXPLICIT");
  }

  const error =
    participationQuery.error ??
    previewMutation.error ??
    createJobMutation.error ??
    viewsQuery.error ??
    tagsQuery.error ??
    jobsQuery.error;

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <Button asChild variant="ghost" size="sm" className="-ml-2 mb-2">
              <Link to="/participacoes">
                <ArrowLeft className="h-4 w-4" /> Voltar para Participações
              </Link>
            </Button>
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Operação em escala
            </div>
            <h1 className="mt-1 font-display text-3xl">Visões, tags e ações em lote</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Reutilize filtros, selecione a página ou todos os resultados filtrados, revise o impacto
              e acompanhe cada item do lote sem executar decisões de contratação em massa.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={() => setViewDialogOpen(true)}>
              <Save className="h-4 w-4" /> Salvar visão
            </Button>
            <Button variant="outline" onClick={() => setTagDialogOpen(true)}>
              <Plus className="h-4 w-4" /> Nova tag
            </Button>
            <Button
              variant="outline"
              onClick={() => void Promise.all([participationQuery.refetch(), jobsQuery.refetch()])}
            >
              <RefreshCw className={cn("h-4 w-4", participationQuery.isFetching && "animate-spin")} />
              Atualizar
            </Button>
          </div>
        </header>

        {message && <StateBanner tone="ok" title="Operação registrada">{message}</StateBanner>}
        {error && (
          <StateBanner tone="danger" title="Não foi possível concluir a operação">
            {error instanceof Error ? error.message : "Tente novamente."}
          </StateBanner>
        )}

        <section className="rounded-xl border border-border bg-card p-4" aria-label="Visões e filtros">
          <div className="flex flex-wrap items-end gap-3">
            <label className="min-w-[240px] flex-1 space-y-1 text-xs font-medium text-muted-foreground">
              <span className="inline-flex items-center gap-1"><FolderOpen className="h-3.5 w-3.5" /> Visão salva</span>
              <select
                className="input h-11 w-full"
                value={selectedViewId}
                onChange={(event) => {
                  const view = viewsQuery.data?.find((item) => item.id === event.target.value);
                  if (view) applyView(view);
                  else {
                    setSelectedViewId("");
                    setFilters(DEFAULT_FILTERS);
                  }
                }}
              >
                <option value="">Filtros atuais</option>
                {(viewsQuery.data ?? []).map((view) => (
                  <option key={view.id} value={view.id}>
                    {view.name}{view.shared ? " · compartilhada" : ""}
                  </option>
                ))}
              </select>
            </label>
            <label className="min-w-[220px] flex-1 space-y-1 text-xs font-medium text-muted-foreground">
              Avaliação
              <select
                className="input h-11 w-full"
                value={filters.simulationId}
                onChange={(event) => updateFilter("simulationId", event.target.value)}
              >
                <option value="">Todas</option>
                {(simulationsQuery.data ?? []).map((simulation) => (
                  <option key={simulation.id} value={simulation.id}>{simulation.name}</option>
                ))}
              </select>
            </label>
            <label className="min-w-[220px] flex-1 space-y-1 text-xs font-medium text-muted-foreground">
              Nome ou e-mail
              <Input
                value={filters.candidate}
                onChange={(event) => updateFilter("candidate", event.target.value)}
                placeholder="Buscar participante"
              />
            </label>
          </div>
          <div className="mt-3 grid gap-3 sm:grid-cols-3">
            <label className="space-y-1 text-xs font-medium text-muted-foreground">
              Situação do processo
              <select
                className="input h-11 w-full"
                value={filters.processStatus}
                onChange={(event) => updateFilter("processStatus", event.target.value as ProcessFilter)}
              >
                <option value="all">Todas</option>
                <option value="waiting">Aguardando início</option>
                <option value="active">Em andamento</option>
                <option value="completed">Concluídas</option>
                <option value="attention">Com problema</option>
              </select>
            </label>
            <label className="space-y-1 text-xs font-medium text-muted-foreground">
              Situação do link
              <select
                className="input h-11 w-full"
                value={filters.linkStatus}
                onChange={(event) => updateFilter("linkStatus", event.target.value as LinkFilter)}
              >
                <option value="all">Todos</option>
                <option value="active">Ativo</option>
                <option value="expiringSoon">Expirando</option>
                <option value="expired">Expirado</option>
                <option value="canceled">Cancelado</option>
              </select>
            </label>
            <label className="flex min-h-11 items-center gap-2 self-end rounded-md border border-input px-3 text-sm">
              <input
                type="checkbox"
                checked={filters.attention}
                onChange={(event) => updateFilter("attention", event.target.checked)}
              />
              Somente itens que exigem atenção
            </label>
          </div>
        </section>

        <section className="rounded-xl border border-border bg-card">
          <div className="flex flex-col gap-3 border-b border-border p-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex flex-wrap items-center gap-2">
              <Button variant={selectionMode === "EXPLICIT" ? "default" : "outline"} size="sm" onClick={toggleCurrentPage}>
                <Layers3 className="h-4 w-4" />
                {allCurrentPageSelected ? "Limpar página" : "Selecionar página"}
              </Button>
              <Button
                variant={selectionMode === "FILTER" ? "default" : "outline"}
                size="sm"
                onClick={() => {
                  setSelectionMode("FILTER");
                  setSelected(new Set());
                }}
              >
                <Filter className="h-4 w-4" /> Todos do filtro
              </Button>
              <span className="text-xs text-muted-foreground">
                {selectionMode === "FILTER"
                  ? `${participationQuery.data?.totalElements ?? 0} resultado(s) do filtro`
                  : `${selected.size} selecionado(s)`}
              </span>
            </div>
            <div className="text-xs text-muted-foreground">
              A seleção confirmada é congelada antes do processamento.
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[880px] text-sm">
              <thead className="bg-muted/40 text-left text-xs uppercase text-muted-foreground">
                <tr>
                  <th className="w-12 px-4 py-3">Sel.</th>
                  <th className="px-4 py-3">Participante</th>
                  <th className="px-4 py-3">Processo</th>
                  <th className="px-4 py-3">Situação</th>
                  <th className="px-4 py-3">Link</th>
                  <th className="px-4 py-3">Ações permitidas</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {participationQuery.isLoading ? (
                  <TableMessage message="Carregando participações…" />
                ) : visibleParticipations.length === 0 ? (
                  <TableMessage message="Nenhuma participação encontrada para os filtros." />
                ) : (
                  visibleParticipations.map((item) => {
                    const key = participationKey(item);
                    return (
                      <tr key={key} className="align-top hover:bg-muted/30">
                        <td className="px-4 py-3">
                          <input
                            type="checkbox"
                            aria-label={`Selecionar ${item.candidateName}`}
                            checked={selectionMode === "EXPLICIT" && selected.has(key)}
                            disabled={selectionMode === "FILTER"}
                            onChange={() => toggleParticipation(item)}
                          />
                        </td>
                        <td className="px-4 py-3">
                          <div className="font-medium">{item.candidateName}</div>
                          <div className="text-xs text-muted-foreground">{item.candidateEmail}</div>
                        </td>
                        <td className="px-4 py-3">
                          <div>{item.participationType === "journey" ? item.journeyName : item.simulationName}</div>
                          <div className="text-xs text-muted-foreground">
                            {item.participationType === "journey" ? "Jornada" : `Individual · v${item.versionNumber ?? "-"}`}
                          </div>
                        </td>
                        <td className="px-4 py-3"><StatusBadge value={item.status} /></td>
                        <td className="px-4 py-3"><StatusBadge value={item.linkStatus} /></td>
                        <td className="px-4 py-3 text-xs text-muted-foreground">
                          {[item.canResend && "reenviar", item.canExtend && "estender", item.canCancel && "cancelar"]
                            .filter(Boolean)
                            .join(" · ") || "somente tags/exportação"}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-between border-t border-border p-4">
            <Button variant="outline" disabled={page <= 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>Anterior</Button>
            <span className="text-xs text-muted-foreground">
              Página {(participationQuery.data?.page ?? 0) + 1} de {Math.max(1, participationQuery.data?.totalPages ?? 1)}
            </span>
            <Button
              variant="outline"
              disabled={!participationQuery.data || page + 1 >= participationQuery.data.totalPages}
              onClick={() => setPage((value) => value + 1)}
            >Próxima</Button>
          </div>
        </section>

        <section className="rounded-xl border border-border bg-card p-4">
          <h2 className="font-display text-xl">Configurar ação</h2>
          <div className="mt-4 grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <label className="space-y-1 text-xs font-medium text-muted-foreground">
              Ação
              <select className="input h-11 w-full" value={action} onChange={(event) => setAction(event.target.value as BulkAction)}>
                {ACTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
              </select>
            </label>
            {action === "EXTEND" && (
              <label className="space-y-1 text-xs font-medium text-muted-foreground">
                Dias adicionais
                <Input type="number" min={1} max={365} value={additionalDays} onChange={(event) => setAdditionalDays(Number(event.target.value))} />
              </label>
            )}
            {(action === "ADD_TAG" || action === "REMOVE_TAG") && (
              <label className="space-y-1 text-xs font-medium text-muted-foreground">
                Tag
                <select className="input h-11 w-full" value={tagId} onChange={(event) => setTagId(event.target.value)}>
                  <option value="">Selecione</option>
                  {(tagsQuery.data ?? []).map((tagItem) => <option key={tagItem.id} value={tagItem.id}>{tagItem.name}</option>)}
                </select>
              </label>
            )}
            <label className="space-y-1 text-xs font-medium text-muted-foreground md:col-span-2">
              Justificativa {action === "CANCEL" ? "(obrigatória)" : "(opcional)"}
              <Input value={justification} onChange={(event) => setJustification(event.target.value)} placeholder="Contexto da operação para auditoria" />
            </label>
          </div>
          <div className="mt-4 flex justify-end">
            <Button
              disabled={
                previewMutation.isPending ||
                (selectionMode === "EXPLICIT" && selected.size === 0) ||
                ((action === "ADD_TAG" || action === "REMOVE_TAG") && !tagId) ||
                (action === "CANCEL" && !justification.trim())
              }
              onClick={() => previewMutation.mutate(currentRequest)}
            >
              <Eye className="h-4 w-4" /> {previewMutation.isPending ? "Calculando…" : "Revisar impacto"}
            </Button>
          </div>
        </section>

        <BulkJobsSection jobs={jobsQuery.data ?? []} />

        <BulkPreviewDialog
          preview={preview}
          request={currentRequest}
          pending={createJobMutation.isPending}
          onClose={() => setPreview(null)}
          onConfirm={() =>
            createJobMutation.mutate({
              ...currentRequest,
              idempotencyKey: crypto.randomUUID(),
            })
          }
        />
        <SavedViewDialog
          open={viewDialogOpen}
          filters={filters}
          onClose={() => setViewDialogOpen(false)}
          onSaved={async () => {
            setViewDialogOpen(false);
            await queryClient.invalidateQueries({ queryKey: ["participation-saved-views"] });
          }}
        />
        <TagDialog
          open={tagDialogOpen}
          onClose={() => setTagDialogOpen(false)}
          onSaved={async () => {
            setTagDialogOpen(false);
            await queryClient.invalidateQueries({ queryKey: ["participation-tags"] });
          }}
        />
      </main>
    </AppShell>
  );

  function updateFilter<K extends keyof FiltersState>(key: K, value: FiltersState[K]) {
    setPage(0);
    setSelectedViewId("");
    setSelected(new Set());
    setSelectionMode("EXPLICIT");
    setFilters((current) => ({ ...current, [key]: value }));
  }
}

function BulkJobsSection({ jobs }: { jobs: Awaited<ReturnType<typeof listParticipationBulkJobs>> }) {
  return (
    <section className="rounded-xl border border-border bg-card p-4">
      <h2 className="font-display text-xl">Lotes recentes</h2>
      {jobs.length === 0 ? (
        <p className="mt-3 text-sm text-muted-foreground">Nenhum lote criado.</p>
      ) : (
        <div className="mt-4 space-y-3">
          {jobs.map((job) => (
            <article key={job.id} className="rounded-lg border border-border p-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-semibold">{actionLabel(job.action)}</span>
                    <StatusBadge value={job.status} />
                  </div>
                  <div className="mt-1 text-xs text-muted-foreground">
                    {new Date(job.createdAt).toLocaleString("pt-BR")} · {job.selectionMode === "FILTER" ? "todos do filtro" : "seleção explícita"}
                  </div>
                </div>
                <Button variant="outline" size="sm" disabled={job.status === "PENDING" || job.status === "RUNNING"} onClick={() => void downloadParticipationBulkReport(job.id)}>
                  <Download className="h-4 w-4" /> Relatório CSV
                </Button>
              </div>
              <div className="mt-3 h-2 overflow-hidden rounded-full bg-muted">
                <div className="h-full bg-primary transition-all" style={{ width: `${job.progressPercent}%` }} />
              </div>
              <div className="mt-2 flex flex-wrap gap-4 text-xs text-muted-foreground">
                <span>{job.processedItems}/{job.totalItems} processados</span>
                <span>{job.succeededItems} sucessos</span>
                <span>{job.skippedItems} ignorados</span>
                <span>{job.failedItems} falhas</span>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function BulkPreviewDialog({
  preview,
  request,
  pending,
  onClose,
  onConfirm,
}: {
  preview: BulkPreview | null;
  request: BulkPreviewRequest;
  pending: boolean;
  onClose: () => void;
  onConfirm: () => void;
}) {
  return (
    <Dialog open={Boolean(preview)} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Confirmar operação em lote</DialogTitle>
          <DialogDescription>{preview?.impact}</DialogDescription>
        </DialogHeader>
        {preview && (
          <div className="space-y-4">
            <div className="grid grid-cols-3 gap-3">
              <Metric label="Selecionados" value={preview.selectedCount} />
              <Metric label="Elegíveis" value={preview.eligibleCount} />
              <Metric label="Excluídos" value={preview.excludedCount} />
            </div>
            {request.justification && (
              <div className="rounded-md bg-muted p-3 text-sm">
                <strong>Justificativa:</strong> {request.justification}
              </div>
            )}
            {preview.excluded.length > 0 && (
              <div>
                <h3 className="text-sm font-semibold">Exceções</h3>
                <div className="mt-2 max-h-48 overflow-y-auto rounded-md border border-border">
                  {preview.excluded.slice(0, 100).map((item) => (
                    <div key={`${item.participationType}:${item.participationId}`} className="border-b border-border p-3 text-xs last:border-0">
                      <div className="font-mono">{item.participationType}:{item.participationId}</div>
                      <div className="mt-1 text-muted-foreground">{item.reason}</div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancelar</Button>
          <Button disabled={pending || !preview || preview.eligibleCount === 0} onClick={onConfirm}>
            {pending ? "Criando lote…" : "Confirmar e processar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function SavedViewDialog({
  open,
  filters,
  onClose,
  onSaved,
}: {
  open: boolean;
  filters: FiltersState;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [name, setName] = useState("");
  const [shared, setShared] = useState(false);
  const mutation = useMutation({
    mutationFn: () => createSavedView({ name, shared, filters, sort: { createdAt: "desc" }, columns: ["candidate", "process", "status", "link", "actions"] }),
    onSuccess: onSaved,
  });
  return (
    <Dialog open={open} onOpenChange={(value) => !value && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Salvar visão atual</DialogTitle>
          <DialogDescription>Os filtros atuais poderão ser reutilizados por você ou pela empresa.</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div><Label>Nome</Label><Input className="mt-1" value={name} onChange={(event) => setName(event.target.value)} placeholder="Convites expirados do piloto" /></div>
          <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={shared} onChange={(event) => setShared(event.target.checked)} /> Compartilhar com a empresa</label>
          {mutation.isError && <p className="text-sm text-danger">{mutation.error.message}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancelar</Button>
          <Button disabled={!name.trim() || mutation.isPending} onClick={() => mutation.mutate()}>{mutation.isPending ? "Salvando…" : "Salvar visão"}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function TagDialog({ open, onClose, onSaved }: { open: boolean; onClose: () => void; onSaved: () => void }) {
  const [name, setName] = useState("");
  const [color, setColor] = useState("#2563EB");
  const [description, setDescription] = useState("");
  const mutation = useMutation({ mutationFn: () => createParticipationTag({ name, color, description }), onSuccess: onSaved });
  return (
    <Dialog open={open} onOpenChange={(value) => !value && onClose()}>
      <DialogContent>
        <DialogHeader><DialogTitle>Nova tag</DialogTitle><DialogDescription>Tags são internas e isoladas por empresa.</DialogDescription></DialogHeader>
        <div className="space-y-4">
          <div><Label>Nome</Label><Input className="mt-1" value={name} onChange={(event) => setName(event.target.value)} /></div>
          <div><Label>Cor</Label><Input className="mt-1 h-11" type="color" value={color} onChange={(event) => setColor(event.target.value)} /></div>
          <div><Label>Descrição</Label><Input className="mt-1" value={description} onChange={(event) => setDescription(event.target.value)} /></div>
          {mutation.isError && <p className="text-sm text-danger">{mutation.error.message}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancelar</Button>
          <Button disabled={!name.trim() || mutation.isPending} onClick={() => mutation.mutate()}>{mutation.isPending ? "Criando…" : "Criar tag"}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return <div className="rounded-md border border-border p-3 text-center"><div className="text-2xl font-semibold tabular-nums">{value}</div><div className="text-xs text-muted-foreground">{label}</div></div>;
}

function TableMessage({ message }: { message: string }) {
  return <tr><td colSpan={6} className="px-4 py-10 text-center text-muted-foreground">{message}</td></tr>;
}

function StatusBadge({ value }: { value: string }) {
  const normalized = value.toLowerCase();
  const tone = normalized.includes("completed") || normalized === "active"
    ? "bg-emerald-100 text-emerald-700"
    : normalized.includes("failed") || normalized === "expired" || normalized === "canceled" || normalized === "abandoned"
      ? "bg-rose-100 text-rose-700"
      : normalized.includes("running") || normalized === "inprogress" || normalized === "expiringsoon"
        ? "bg-amber-100 text-amber-700"
        : "bg-slate-100 text-slate-600";
  return <span className={cn("inline-flex rounded-full px-2.5 py-1 text-xs font-medium", tone)}>{statusLabel(value)}</span>;
}

function buildRequest({
  action,
  selectionMode,
  selected,
  visibleParticipations,
  filters,
  additionalDays,
  tagId,
  justification,
}: {
  action: BulkAction;
  selectionMode: SelectionMode;
  selected: Set<string>;
  visibleParticipations: ParticipationMonitoringItem[];
  filters: FiltersState;
  additionalDays: number;
  tagId: string;
  justification: string;
}): BulkPreviewRequest {
  const byKey = new Map(visibleParticipations.map((item) => [participationKey(item), item]));
  const refs: ParticipationRef[] = Array.from(selected).map((key) => {
    const item = byKey.get(key);
    const [type, ...idParts] = key.split(":");
    return { type: item?.participationType ?? (type as ParticipationRef["type"]), id: item?.participationId ?? idParts.join(":") };
  });
  const filter: BulkFilter = {
    simulationId: filters.simulationId || undefined,
    candidate: filters.candidate.trim() || undefined,
    processStatus: filters.processStatus === "all" ? undefined : filters.processStatus,
    linkStatus: filters.linkStatus === "all" ? undefined : filters.linkStatus,
    attention: filters.attention || undefined,
  };
  return {
    action,
    selectionMode,
    selected: refs,
    filter,
    additionalDays: action === "EXTEND" ? additionalDays : undefined,
    tagId: action === "ADD_TAG" || action === "REMOVE_TAG" ? tagId : undefined,
    justification: justification.trim() || undefined,
  };
}

function participationKey(item: ParticipationMonitoringItem) {
  return `${item.participationType}:${item.participationId}`;
}

function matchesFilters(item: ParticipationMonitoringItem, filters: FiltersState) {
  if (filters.processStatus !== "all") {
    const matches = filters.processStatus === "waiting"
      ? item.status === "notStarted"
      : filters.processStatus === "active"
        ? item.status === "inProgress"
        : filters.processStatus === "completed"
          ? item.status === "completed"
          : needsAttention(item);
    if (!matches) return false;
  }
  if (filters.linkStatus !== "all" && item.linkStatus !== filters.linkStatus) return false;
  if (filters.attention && !needsAttention(item)) return false;
  return true;
}

function needsAttention(item: ParticipationMonitoringItem) {
  return item.status === "abandoned" || item.status === "expired" || item.linkStatus === "expired" || item.linkStatus === "canceled" || (item.status === "inProgress" && !item.active);
}

function actionLabel(action: BulkAction) {
  return ACTIONS.find((item) => item.value === action)?.label ?? action;
}

function statusLabel(value: string) {
  const labels: Record<string, string> = {
    notStarted: "Aguardando início",
    inProgress: "Em andamento",
    completed: "Concluída",
    abandoned: "Cancelada/abandonada",
    expired: "Expirada",
    active: "Ativo",
    expiringSoon: "Expirando",
    canceled: "Cancelado",
    PENDING: "Pendente",
    RUNNING: "Processando",
    COMPLETED: "Concluído",
    COMPLETED_WITH_ERRORS: "Concluído com falhas",
    FAILED: "Falhou",
  };
  return labels[value] ?? value;
}

function stringValue(value: unknown) {
  return typeof value === "string" ? value : "";
}

function processFilterValue(value: unknown): ProcessFilter {
  return ["all", "waiting", "active", "completed", "attention"].includes(String(value))
    ? (value as ProcessFilter)
    : "all";
}

function linkFilterValue(value: unknown): LinkFilter {
  return ["all", "active", "expiringSoon", "expired", "canceled"].includes(String(value))
    ? (value as LinkFilter)
    : "all";
}
