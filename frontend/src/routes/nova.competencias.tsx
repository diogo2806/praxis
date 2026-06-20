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
import { getTenantConfig, updateTenantConfig, type TenantConfigOption } from "@/lib/api/praxis";

export const Route = createFileRoute("/nova/competencias")({
  head: () => ({
    meta: [
      { title: "Gerenciar Competências - Práxis" },
      {
        name: "description",
        content: "Crie e edite as competências utilizadas nas suos testes.",
      },
    ],
  }),
  component: CompetenciasManagement,
});

type EditingOption = TenantConfigOption & { originalValue: string };

function CompetenciasManagement() {
  const queryClient = useQueryClient();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [newCompetencia, setNewCompetencia] = useState("");
  const [editingOption, setEditingOption] = useState<EditingOption | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [pendingDeleteOption, setPendingDeleteOption] = useState<EditingOption | null>(null);

  const tenantConfigQuery = useQuery({
    queryKey: ["tenant-config"],
    queryFn: getTenantConfig,
  });

  const competencias = tenantConfigQuery.data?.competencies || [];

  const saveCatalogMutation = useMutation({
    mutationFn: (options: TenantConfigOption[]) => updateTenantConfig("COMPETENCY", options),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["tenant-config"] });
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

  const handleOpenCreateDialog = () => {
    setEditingOption(null);
    setNewCompetencia("");
    setIsDialogOpen(true);
  };

  const handleOpenEditDialog = (option: TenantConfigOption) => {
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

  const requestDelete = (option: TenantConfigOption) => {
    setPendingDeleteOption({ ...option, originalValue: option.value });
  };

  const confirmDelete = () => {
    if (!pendingDeleteOption) return;
    handleRemove(pendingDeleteOption.value);
    setPendingDeleteOption(null);
  };

  if (tenantConfigQuery.isLoading) {
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

  if (tenantConfigQuery.isError) {
    return (
      <AppShell>
        <div className="mb-6">
          <h1 className="text-3xl font-semibold">Gerenciar Competências</h1>
        </div>
        <StateBanner tone="danger" title="Erro ao carregar competências">
          {tenantConfigQuery.error instanceof Error
            ? tenantConfigQuery.error.message
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
            Cadastre e edite as competências que aparecem nas suos testes.
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
          description="Crie sua primeira competência para começar a usar nos testes."
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
                onChange={(event) => setSearchTerm(event.target.value)}
                disabled={saveCatalogMutation.isPending}
              />
            </div>
            <div className="text-xs text-muted-foreground">
              {visibleCompetencias.length} de {competencias.length} encontradas
            </div>
          </div>

          <div className="rounded-md border border-border bg-card">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium">Competência</th>
                    <th className="px-4 py-3 text-right font-medium">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleCompetencias.length === 0 ? (
                    <tr>
                      <td colSpan={2} className="px-4 py-5 text-center text-muted-foreground">
                        Nenhuma competência encontrada com o filtro atual.
                      </td>
                    </tr>
                  ) : (
                    visibleCompetencias.map((competencia) => (
                      <tr
                        key={competencia.value}
                        className="border-b border-border last:border-0 transition"
                      >
                        <td className="px-4 py-3">
                          <div className="font-medium text-foreground">{competencia.label}</div>
                          <div className="mt-0.5 text-xs text-muted-foreground">
                            {competencia.value}
                          </div>
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
                    ))
                  )}
                </tbody>
              </table>
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
              de teste criados a partir de agora.
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
