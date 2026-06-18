import { createFileRoute } from "@tanstack/react-router";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Check, Edit2, Eye, EyeOff, Plus, X } from "lucide-react";

import { AppShell } from "@/components/app-shell";
import {
  EmptyState,
  SkeletonRows,
  StateBanner,
} from "@/components/praxis-ui";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  getAllConfigOptions,
  updateConfigOption,
  updateTenantConfig,
  type TenantConfigOption,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/nova/competencias")({
  head: () => ({
    meta: [
      { title: "Gerenciar Competências - Práxis" },
      {
        name: "description",
        content: "Crie, edite e gerencie as competências utilizadas nas suas simulações.",
      },
    ],
  }),
  component: CompetenciasManagement,
});

type EditingOption = TenantConfigOption & { original: TenantConfigOption };

function CompetenciasManagement() {
  const queryClient = useQueryClient();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [newCompetencia, setNewCompetencia] = useState("");
  const [editingOption, setEditingOption] = useState<EditingOption | null>(null);

  const competenciasQuery = useQuery({
    queryKey: ["configOptions", "COMPETENCY", "all"],
    queryFn: () => getAllConfigOptions("COMPETENCY"),
  });

  const updateOptionMutation = useMutation({
    mutationFn: ({ optionValue, update }: { optionValue: string; update: Partial<TenantConfigOption> }) =>
      updateConfigOption("COMPETENCY", optionValue, {
        label: update.label || "",
        locked: update.locked || false,
        selectedByDefault: update.selectedByDefault || false,
        active: update.active !== undefined ? update.active : true,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["configOptions", "COMPETENCY", "all"] });
      await queryClient.invalidateQueries({ queryKey: ["tenant-config"] });
      setEditingId(null);
      setEditingOption(null);
      setIsDialogOpen(false);
    },
  });

  const addCompetenciaMutation = useMutation({
    mutationFn: async (label: string) => {
      const current = competenciasQuery.data || [];
      const newOption = {
        value: label,
        label: label,
        locked: false,
        selectedByDefault: false,
        active: true,
      };
      return updateTenantConfig("COMPETENCY", [...current, newOption]);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["configOptions", "COMPETENCY", "all"] });
      await queryClient.invalidateQueries({ queryKey: ["tenant-config"] });
      setNewCompetencia("");
      setIsDialogOpen(false);
    },
  });

  const competencias = (competenciasQuery.data || []).sort((a, b) => {
    if (a.active !== b.active) return a.active ? -1 : 1;
    return a.label.localeCompare(b.label);
  });

  const activeCount = competencias.filter((c) => c.active).length;
  const inactiveCount = competencias.filter((c) => !c.active).length;

  const handleOpenEditDialog = (option: TenantConfigOption) => {
    setEditingOption({
      ...option,
      original: option,
    });
    setEditingId(option.value);
    setIsDialogOpen(true);
  };

  const handleToggleActive = () => {
    if (!editingOption) return;
    const updatedOption = {
      ...editingOption,
      active: !editingOption.active,
    };
    setEditingOption(updatedOption);
  };

  const handleSaveEdit = () => {
    if (!editingOption) return;
    updateOptionMutation.mutate({
      optionValue: editingOption.value,
      update: {
        label: editingOption.label,
        locked: editingOption.locked,
        selectedByDefault: editingOption.selectedByDefault,
        active: editingOption.active,
      },
    });
  };

  const handleDeleteClick = (value: string) => {
    setDeletingId(value);
    setIsDeleteDialogOpen(true);
  };

  const handleConfirmDelete = () => {
    if (!deletingId) return;
    updateOptionMutation.mutate({
      optionValue: deletingId,
      update: { active: false },
    });
    setIsDeleteDialogOpen(false);
  };

  const handleAddNew = () => {
    if (!newCompetencia.trim()) return;
    addCompetenciaMutation.mutate(newCompetencia.trim());
  };

  if (competenciasQuery.isLoading) {
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

  if (competenciasQuery.isError) {
    return (
      <AppShell>
        <div className="mb-6">
          <h1 className="text-3xl font-semibold">Gerenciar Competências</h1>
        </div>
        <StateBanner tone="danger" title="Erro ao carregar competências">
          {competenciasQuery.error instanceof Error
            ? competenciasQuery.error.message
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
            Cadastre, edite e controle quais competências aparecem nas suas simulações.
          </p>
        </div>
        <button
          onClick={() => {
            setEditingId(null);
            setEditingOption(null);
            setNewCompetencia("");
            setIsDialogOpen(true);
          }}
          className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="h-4 w-4" />
          Nova competência
        </button>
      </div>

      {competenciasQuery.isError && (
        <StateBanner tone="danger" title="Erro ao carregar">
          {competenciasQuery.error instanceof Error
            ? competenciasQuery.error.message
            : "Tente novamente."}
        </StateBanner>
      )}

      {addCompetenciaMutation.isError && (
        <StateBanner tone="danger" title="Erro ao adicionar competência">
          {addCompetenciaMutation.error instanceof Error
            ? addCompetenciaMutation.error.message
            : "Tente novamente."}
        </StateBanner>
      )}

      {updateOptionMutation.isError && (
        <StateBanner tone="danger" title="Erro ao atualizar competência">
          {updateOptionMutation.error instanceof Error
            ? updateOptionMutation.error.message
            : "Tente novamente."}
        </StateBanner>
      )}

      {competencias.length === 0 ? (
        <EmptyState
          title="Nenhuma competência cadastrada"
          description="Crie sua primeira competência para começar a usar nas simulações."
          actions={
            <button
              onClick={() => {
                setEditingId(null);
                setEditingOption(null);
                setNewCompetencia("");
                setIsDialogOpen(true);
              }}
              className="inline-flex items-center justify-between rounded-md border border-primary bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Criar primeira competência
              <Plus className="h-4 w-4" />
            </button>
          }
        />
      ) : (
        <div className="space-y-6">
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Stat
              label="Ativas"
              value={activeCount}
              hint="Aparecem nas simulações"
            />
            <Stat
              label="Desativadas"
              value={inactiveCount}
              hint="Não aparecem mais"
            />
          </div>

          <div className="rounded-md border border-border bg-card">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium">Competência</th>
                    <th className="px-4 py-3 text-left font-medium">Status</th>
                    <th className="px-4 py-3 text-right font-medium">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {competencias.map((competencia) => (
                    <tr
                      key={competencia.value}
                      className={cn(
                        "border-b border-border last:border-0 transition",
                        !competencia.active && "bg-muted/30 opacity-60"
                      )}
                    >
                      <td className="px-4 py-3">
                        <div className="font-medium text-foreground">{competencia.label}</div>
                        <div className="mt-0.5 text-xs text-muted-foreground">{competencia.value}</div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          {competencia.active ? (
                            <>
                              <div className="inline-flex items-center gap-1.5 rounded-md bg-success/10 px-2 py-1 text-xs font-medium text-success">
                                <div className="h-1.5 w-1.5 rounded-full bg-success" />
                                Ativa
                              </div>
                            </>
                          ) : (
                            <>
                              <div className="inline-flex items-center gap-1.5 rounded-md bg-muted px-2 py-1 text-xs font-medium text-muted-foreground">
                                <div className="h-1.5 w-1.5 rounded-full bg-muted-foreground" />
                                Desativada
                              </div>
                            </>
                          )}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex justify-end gap-2">
                          <button
                            onClick={() => handleOpenEditDialog(competencia)}
                            disabled={updateOptionMutation.isPending}
                            className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-border bg-background text-foreground hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                            title="Editar"
                          >
                            <Edit2 className="h-3.5 w-3.5" />
                          </button>
                          {competencia.active ? (
                            <button
                              onClick={() => handleDeleteClick(competencia.value)}
                              disabled={updateOptionMutation.isPending}
                              className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-border bg-background text-muted-foreground hover:bg-danger/10 hover:text-danger disabled:cursor-not-allowed disabled:opacity-50"
                              title="Desativar"
                            >
                              <EyeOff className="h-3.5 w-3.5" />
                            </button>
                          ) : (
                            <button
                              onClick={() => {
                                updateOptionMutation.mutate({
                                  optionValue: competencia.value,
                                  update: { active: true },
                                });
                              }}
                              disabled={updateOptionMutation.isPending}
                              className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-border bg-background text-muted-foreground hover:bg-success/10 hover:text-success disabled:cursor-not-allowed disabled:opacity-50"
                              title="Ativar"
                            >
                              <Eye className="h-3.5 w-3.5" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {editingId ? "Editar Competência" : "Nova Competência"}
            </DialogTitle>
            <DialogDescription>
              {editingId
                ? "Edite os detalhes da competência."
                : "Preencha os dados para criar uma nova competência."}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            {editingId ? (
              <>
                <div>
                  <Label htmlFor="edit-label">Nome da Competência</Label>
                  <Input
                    id="edit-label"
                    value={editingOption?.label || ""}
                    onChange={(e) => {
                      if (editingOption) {
                        setEditingOption({
                          ...editingOption,
                          label: e.target.value,
                        });
                      }
                    }}
                    disabled={updateOptionMutation.isPending}
                  />
                </div>

                <div className="flex items-center justify-between rounded-md border border-border bg-muted/30 p-3">
                  <div>
                    <Label className="font-medium">
                      {editingOption?.active ? "Ativar" : "Desativar"}
                    </Label>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {editingOption?.active
                        ? "Aparecerá nas simulações"
                        : "Não aparecerá nas simulações"}
                    </p>
                  </div>
                  <Switch
                    checked={editingOption?.active || false}
                    onCheckedChange={handleToggleActive}
                    disabled={updateOptionMutation.isPending}
                  />
                </div>

                <div className="flex gap-3 pt-4">
                  <button
                    onClick={() => {
                      setIsDialogOpen(false);
                      setEditingId(null);
                      setEditingOption(null);
                    }}
                    className="flex-1 rounded-md border border-border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                    disabled={updateOptionMutation.isPending}
                  >
                    Cancelar
                  </button>
                  <button
                    onClick={handleSaveEdit}
                    disabled={!editingOption?.label || updateOptionMutation.isPending}
                    className="flex-1 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {updateOptionMutation.isPending && (
                      <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                    )}
                    Salvar
                  </button>
                </div>
              </>
            ) : (
              <>
                <div>
                  <Label htmlFor="new-competencia">Nome da Competência</Label>
                  <Input
                    id="new-competencia"
                    placeholder="Ex: Pensamento Crítico"
                    value={newCompetencia}
                    onChange={(e) => setNewCompetencia(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        handleAddNew();
                      }
                    }}
                    disabled={addCompetenciaMutation.isPending}
                  />
                </div>

                <div className="flex gap-3 pt-4">
                  <button
                    onClick={() => setIsDialogOpen(false)}
                    className="flex-1 rounded-md border border-border bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                    disabled={addCompetenciaMutation.isPending}
                  >
                    Cancelar
                  </button>
                  <button
                    onClick={handleAddNew}
                    disabled={!newCompetencia.trim() || addCompetenciaMutation.isPending}
                    className="flex-1 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {addCompetenciaMutation.isPending && (
                      <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                    )}
                    Adicionar
                  </button>
                </div>
              </>
            )}
          </div>
        </DialogContent>
      </Dialog>

      <AlertDialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Desativar competência?</AlertDialogTitle>
            <AlertDialogDescription>
              Esta competência será desativada e não aparecerá mais nas simulações. Você pode reativá-la a qualquer
              momento.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="flex gap-3">
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleConfirmDelete}
              disabled={updateOptionMutation.isPending}
              className="bg-danger hover:bg-danger/90"
            >
              {updateOptionMutation.isPending ? "Desativando..." : "Desativar"}
            </AlertDialogAction>
          </div>
        </AlertDialogContent>
      </AlertDialog>
    </AppShell>
  );
}

function Stat({
  label,
  value,
  hint,
}: {
  label: string;
  value: number;
  hint: string;
}) {
  return (
    <div className="rounded-md border border-border bg-card p-4">
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="mt-1 text-3xl font-semibold tabular-nums text-foreground">
        {value}
      </div>
      <div className="mt-1 text-[11px] text-muted-foreground">{hint}</div>
    </div>
  );
}
