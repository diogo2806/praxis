import { createFileRoute } from "@tanstack/react-router";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Edit2, Plus, Trash2 } from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { EmptyState, SkeletonRows, StateBanner } from "@/components/praxis-ui";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getEmpresaConfig, updateEmpresaConfig, type EmpresaConfigOption } from "@/lib/api/praxis";

export const Route = createFileRoute("/competencias")({
  head: () => ({
    meta: [
      { title: "Gerenciar Competências - Práxis" },
      {
        name: "description",
        content: "Crie e edite as competências utilizadas nas suas avaliações.",
      },
    ],
  }),
  component: CompetenciasManagement,
});

type EditingOption = EmpresaConfigOption & { originalValue: string };

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [10, 25, 50];

function CompetenciasManagement() {
  const queryClient = useQueryClient();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [newCompetencia, setNewCompetencia] = useState("");
  const [editingOption, setEditingOption] = useState<EditingOption | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [pendingDeleteOption, setPendingDeleteOption] = useState<EditingOption | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

  const empresaConfigQuery = useQuery({
    queryKey: ["empresa-config"],
    queryFn: getEmpresaConfig,
  });

  const competencias = empresaConfigQuery.data?.competencies || [];

  const saveCatalogMutation = useMutation({
    mutationFn: (options: EmpresaConfigOption[]) => updateEmpresaConfig("COMPETENCY", options),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["empresa-config"] });
      setNewCompetencia("");
      setEditingOption(null);
      setIsDialogOpen(false);
    },
  });

  const visibleCompetencias = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLowerCase();
    return [...competencias]
      .filter((competencia) => competencia.label.toLowerCase().includes(normalizedSearch))
      .sort((a, b) => a.label.localeCompare(b.label, "pt-BR", { sensitivity: "base" }));
  }, [competencias, searchTerm]);

  const totalPages = Math.max(1, Math.ceil(visibleCompetencias.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const pageStartIndex = (safeCurrentPage - 1) * pageSize;
  const pageEndIndex = Math.min(pageStartIndex + pageSize, visibleCompetencias.length);
  const paginatedCompetencias = visibleCompetencias.slice(pageStartIndex, pageEndIndex);
  const pageRangeStart = visibleCompetencias.length === 0 ? 0 : pageStartIndex + 1;

  const handleOpenCreateDialog = () => {
    setEditingOption(null);
    setNewCompetencia("");
    setIsDialogOpen(true);
  };

  const handleOpenEditDialog = (option: EmpresaConfigOption) => {
    setEditingOption({
      ...option,
      originalValue: option.value,
    });
    setIsDialogOpen(true);
  };

  const handleAddNew = () => {
    const label = newCompetencia.trim();
    if (!label) return;

    saveCatalogMutation.mutate([
      ...competencias,
      {
        value: label,
        label,
        locked: false,
        selectedByDefault: false,
      },
    ]);
  };

  const handleSaveEdit = () => {
    if (!editingOption) return;
    const label = editingOption.label.trim();
    if (!label) return;

    saveCatalogMutation.mutate(
      competencias.map((competencia) =>
        competencia.value === editingOption.originalValue
          ? {
              ...competencia,
              label,
            }
          : competencia,
      ),
    );
  };

  const handleRemove = (value: string) => {
    saveCatalogMutation.mutate(competencias.filter((competencia) => competencia.value !== value));
  };

  const requestDelete = (option: EmpresaConfigOption) => {
    setPendingDeleteOption({ ...option, originalValue: option.value });
  };

  const confirmDelete = () => {
    if (!pendingDeleteOption) return;
    handleRemove(pendingDeleteOption.value);
    setPendingDeleteOption(null);
  };

  if (empresaConfigQuery.isLoading) {
    return (
      <AppShell>
        <div className="mb-6">
          <h1 className="text-3xl font-semibold">Gerenciar Competências</h1>
        </div>
        <section className="rounded-md border border-border bg-card p-4">
          <SkeletonRows rows={5} />
        </section>
      </AppShell>
    );
  }

  if (empresaConfigQuery.isError) {
    return (
      <AppShell>
        <div className="mb-6">
          <h1 className="text-3xl font-semibold">Gerenciar Competências</h1>
        </div>
        <StateBanner tone="danger" title="Erro ao carregar competências">
          {empresaConfigQuery.error instanceof Error
            ? empresaConfigQuery.error.message
            : "Não foi possível carregar as competências. Tente novamente."}
        </StateBanner>
      </AppShell>
    );
  }

  return (
    <AppShell>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold">Gerenciar Competências</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Cadastre e edite as competências que aparecem nas suas avaliações.
          </p>
        </div>
        <button
          onClick={handleOpenCreateDialog}
          className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="h-4 w-4" />
          Nova competência
        </button>
      </div>

      {saveCatalogMutation.isError && (
        <StateBanner tone="danger" title="Erro ao salvar competências">
          {saveCatalogMutation.error instanceof Error
            ? saveCatalogMutation.error.message
            : "Tente novamente."}
        </StateBanner>
      )}

      {competencias.length === 0 ? (
        <EmptyState
          title="Nenhuma competência cadastrada"
          description="Crie sua primeira competência para começar a usar nas avaliações."
          actions={
            <button
              onClick={handleOpenCreateDialog}
              className="inline-flex items-center justify-between rounded-md border border-primary bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Criar primeira competência
              <Plus className="h-4 w-4" />
            </button>
          }
        />
      ) : (
        <div className="space-y-6">
          <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
            <Stat label="Competências" value={competencias.length} hint="Persistidas no catálogo" />
          </div>

          <div className="rounded-md border border-border bg-card p-4">
            <div className="mb-3">
              <Label
                htmlFor="filter-competencias"
                className="mb-1.5 block text-xs text-muted-foreground"
              >
                Buscar por nome
              </Label>
              <Input
                id="filter-competencias"
                placeholder="Digite parte do nome da competência"
                value={searchTerm}
                onChange={(event) => {
                  setSearchTerm(event.target.value);
                  setCurrentPage(1);
                }}
                disabled={saveCatalogMutation.isPending}
              />
            </div>
            <div className="text-xs text-muted-foreground">
              {visibleCompetencias.length} de {competencias.length} encontradas
            </div>
          </div>

          <div className="rounded-md border border-border bg-card">
            <div className="overflow-x-auto">
              <table className="w-full text-sm" data-no-pagination>
                <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium">Competência</th>
                    <th className="px-4 py-3 text-left font-medium">Status</th>
                    <th className="px-4 py-3 text-right font-medium">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleCompetencias.length === 0 ? (
                    <tr>
                      <td colSpan={3} className="px-4 py-5 text-center text-muted-foreground">
                        Nenhuma competência encontrada com o filtro atual.
                      </td>
                    </tr>
                  ) : (
                    paginatedCompetencias.map((competencia) => {
                      const showIdentifier =
                        competencia.value.trim().toLowerCase() !==
                        competencia.label.trim().toLowerCase();

                      return (
                        <tr
                          key={competencia.value}
                          className="border-b border-border last:border-0 transition"
                        >
                          <td className="px-4 py-3">
                            <div className="font-medium text-foreground">{competencia.label}</div>
                            {showIdentifier && (
                              <div className="mt-0.5 text-xs text-muted-foreground">
                                ID: {competencia.value}
                              </div>
                            )}
                          </td>
                          <td className="px-4 py-3">
                            <span className="inline-flex rounded-md border border-success/30 bg-success/10 px-2 py-1 text-xs font-medium text-success">
                              Ativa
                            </span>
                          </td>
                          <td className="px-4 py-3 text-right">
                            <div className="flex justify-end gap-2">
                              <button
                                onClick={() => handleOpenEditDialog(competencia)}
                                disabled={saveCatalogMutation.isPending}
                                className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-border bg-background text-foreground hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                                title="Editar"
                              >
                                <Edit2 className="h-3.5 w-3.5" />
                              </button>
                              <button
                                onClick={() => requestDelete(competencia)}
                                disabled={saveCatalogMutation.isPending}
                                className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-border bg-background text-muted-foreground hover:bg-danger/10 hover:text-danger disabled:cursor-not-allowed disabled:opacity-50"
                                title="Remover"
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>

            <div
              className="flex w-full flex-wrap items-center justify-between gap-3 border-t border-border px-4 py-3 text-sm text-muted-foreground"
              role="navigation"
              aria-label="Paginação da tabela de competências"
            >
              <span className="tabular-nums" aria-live="polite">
                {visibleCompetencias.length === 0
                  ? "0 registros"
                  : `${pageRangeStart}–${pageEndIndex} de ${visibleCompetencias.length}`}
              </span>
              <div className="flex flex-wrap items-center gap-2">
                <label htmlFor="competencias-page-size" className="flex items-center gap-2">
                  <span className="sr-only sm:not-sr-only">Linhas por página</span>
                  <select
                    id="competencias-page-size"
                    aria-label="Linhas por página"
                    value={pageSize}
                    onChange={(event) => {
                      setPageSize(Number(event.target.value));
                      setCurrentPage(1);
                    }}
                    className="h-9 rounded-md border border-border bg-background px-2 text-sm text-foreground outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    {PAGE_SIZE_OPTIONS.map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                </label>
                <button
                  type="button"
                  onClick={() => setCurrentPage(Math.max(1, safeCurrentPage - 1))}
                  disabled={safeCurrentPage <= 1}
                  aria-label="Ir para a página anterior"
                  className="inline-flex h-9 items-center justify-center rounded-md border border-border bg-background px-3 text-sm font-medium text-foreground transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Anterior
                </button>
                <span className="min-w-24 text-center tabular-nums text-foreground" aria-live="polite">
                  Página {safeCurrentPage} de {totalPages}
                </span>
                <button
                  type="button"
                  onClick={() => setCurrentPage(Math.min(totalPages, safeCurrentPage + 1))}
                  disabled={safeCurrentPage >= totalPages}
                  aria-label="Ir para a próxima página"
                  className="inline-flex h-9 items-center justify-center rounded-md border border-border bg-background px-3 text-sm font-medium text-foreground transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Próxima
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editingOption ? "Editar Competência" : "Nova Competência"}</DialogTitle>
            <DialogDescription>
              {editingOption
                ? "Edite o nome da competência."
                : "Preencha o nome para criar uma nova competência."}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            {editingOption ? (
              <div>
                <Label htmlFor="edit-label">Nome da Competência</Label>
                <Input
                  id="edit-label"
                  value={editingOption.label}
                  onChange={(event) =>
                    setEditingOption({
                      ...editingOption,
                      label: event.target.value,
                    })
                  }
                  disabled={saveCatalogMutation.isPending}
                />
              </div>
            ) : (
              <div>
                <Label htmlFor="new-competencia">Nome da Competência</Label>
                <Input
                  id="new-competencia"
                  placeholder="Ex: Pensamento Crítico"
                  value={newCompetencia}
                  onChange={(event) => setNewCompetencia(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      handleAddNew();
                    }
                  }}
                  disabled={saveCatalogMutation.isPending}
                />
              </div>
            )}

            <div className="flex gap-3 pt-4">
              <button
                onClick={() => {
                  setIsDialogOpen(false);
                  setEditingOption(null);
                }}
                className="flex-1 rounded-md border border-border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                disabled={saveCatalogMutation.isPending}
              >
                Cancelar
              </button>
              <button
                onClick={editingOption ? handleSaveEdit : handleAddNew}
                disabled={
                  saveCatalogMutation.isPending ||
                  (editingOption ? !editingOption.label.trim() : !newCompetencia.trim())
                }
                className="flex-1 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {saveCatalogMutation.isPending && (
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                )}
                {editingOption ? "Salvar" : "Adicionar"}
              </button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog
        open={Boolean(pendingDeleteOption)}
        onOpenChange={(open) => !open && setPendingDeleteOption(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Remover competência</DialogTitle>
            <DialogDescription>
              Confirme para remover esta competência do catálogo. Essa alteração afeta novos planos
              de avaliação criados a partir de agora.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              Essa competência:
              <span className="ml-1 font-medium">{pendingDeleteOption?.label}</span> será removida.
            </p>
            <div className="flex gap-3 pt-2">
              <button
                onClick={() => setPendingDeleteOption(null)}
                className="flex-1 rounded-md border border-border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                disabled={saveCatalogMutation.isPending}
              >
                Cancelar
              </button>
              <button
                onClick={confirmDelete}
                disabled={saveCatalogMutation.isPending}
                className="flex-1 rounded-md bg-danger px-4 py-2 text-sm font-medium text-danger-foreground hover:bg-danger/90 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {saveCatalogMutation.isPending ? (
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                ) : (
                  "Confirmar remoção"
                )}
              </button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </AppShell>
  );
}

function Stat({ label, value, hint }: { label: string; value: number; hint: string }) {
  return (
    <div className="rounded-md border border-border bg-card p-4">
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="mt-1 text-3xl font-semibold tabular-nums text-foreground">{value}</div>
      <div className="mt-1 text-[11px] text-muted-foreground">{hint}</div>
    </div>
  );
}
