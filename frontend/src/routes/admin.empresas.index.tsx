import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ChevronLeft, ChevronRight, Download, Plus } from "lucide-react";
import { AdminShell, StatusBadge, planLabel } from "@/components/admin-shell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { searchAdminEmpresas } from "@/lib/api/admin-empresas";
import {
  cancelAdminEmpresa,
  createAdminEmpresa,
  grantAdminEmpresaCredits,
  reactivateAdminEmpresa,
  suspendAdminEmpresa,
  type CommercialPlanType,
  type CreateEmpresaAdminRequest,
  type EmpresaAdminSummary,
  type EmpresaStatus,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/admin/empresas/")({
  head: () => ({ meta: [{ title: "Clientes · Admin Praxis" }] }),
  component: AdminEmpresasPage,
});

const PLAN_OPTIONS: CommercialPlanType[] = ["AVULSO", "PROFISSIONAL", "ENTERPRISE"];
const STATUS_OPTIONS: EmpresaStatus[] = ["ATIVO", "EM_TESTE", "SUSPENSO", "CANCELADO"];

function AdminEmpresasPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<EmpresaStatus | "">("");
  const [plan, setPlan] = useState<CommercialPlanType | "">("");
  const [createOpen, setCreateOpen] = useState(false);
  const [reasonAction, setReasonAction] = useState<
    | { kind: "suspend" | "cancel" | "reactivate"; empresa: EmpresaAdminSummary }
    | null
  >(null);
  const [creditEmpresa, setCreditEmpresa] = useState<EmpresaAdminSummary | null>(null);

  const empresasQuery = useQuery({
    queryKey: ["admin-empresas", page, search, status, plan],
    queryFn: () =>
      searchAdminEmpresas(page, {
        search: search || undefined,
        status: status || undefined,
        plan: plan || undefined,
      }),
  });

  const response = empresasQuery.data;
  const empresas = response?.items ?? [];
  const totalElements = response?.totalElements ?? 0;
  const totalPages = response?.totalPages ?? 0;
  const pageSize = response?.size ?? 100;
  const firstItem = totalElements === 0 ? 0 : page * pageSize + 1;
  const lastItem = Math.min((page + 1) * pageSize, totalElements);

  function changeSearch(value: string) {
    setSearch(value);
    setPage(0);
  }

  function changeStatus(value: EmpresaStatus | "") {
    setStatus(value);
    setPage(0);
  }

  function changePlan(value: CommercialPlanType | "") {
    setPlan(value);
    setPage(0);
  }

  function exportCsv() {
    const header = ["Nome", "CNPJ", "E-mail", "Plano", "Status", "Uso no período", "Créditos", "Criado em"];
    const rows = empresas.map((empresa) => [
      empresa.name,
      empresa.taxId ?? "",
      empresa.corporateEmail ?? "",
      empresa.commercialPlanType,
      empresa.status,
      String(empresa.completedAttemptsInPeriod),
      String(empresa.creditBalance),
      empresa.createdAt,
    ]);
    const csv = [header, ...rows]
      .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(","))
      .join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `clientes-pagina-${page + 1}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <AdminShell>
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Clientes</h1>
          <p className="mt-1 text-sm text-slate-500">
            Consulta paginada com uso e saldo agregados em lote.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" disabled={empresas.length === 0} onClick={exportCsv}>
            <Download className="size-4" /> Exportar página
          </Button>
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="size-4" /> Novo cliente
          </Button>
        </div>
      </div>

      <div className="mt-6 flex flex-wrap items-end gap-3 rounded-xl border border-slate-200 bg-white p-4">
        <div className="min-w-[220px] flex-1">
          <Label className="text-xs text-slate-500">Busca livre</Label>
          <Input
            value={search}
            onChange={(event) => changeSearch(event.target.value)}
            placeholder="Nome, CNPJ ou e-mail"
          />
        </div>
        <div>
          <Label className="text-xs text-slate-500">Status</Label>
          <select
            value={status}
            onChange={(event) => changeStatus(event.target.value as EmpresaStatus | "")}
            className="block h-9 rounded-md border border-input bg-background px-3 text-sm"
          >
            <option value="">Todos</option>
            {STATUS_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </div>
        <div>
          <Label className="text-xs text-slate-500">Plano comercial</Label>
          <select
            value={plan}
            onChange={(event) => changePlan(event.target.value as CommercialPlanType | "")}
            className="block h-9 rounded-md border border-input bg-background px-3 text-sm"
          >
            <option value="">Todos</option>
            {PLAN_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="mt-4 overflow-hidden rounded-xl border border-slate-200 bg-white">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Nome</th>
                <th className="px-4 py-3">CNPJ</th>
                <th className="px-4 py-3">E-mail</th>
                <th className="px-4 py-3">Plano</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Uso</th>
                <th className="px-4 py-3">Créditos</th>
                <th className="px-4 py-3">Criado em</th>
                <th className="px-4 py-3 text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {empresasQuery.isLoading ? (
                <tr>
                  <td colSpan={9} className="px-4 py-8 text-center text-slate-500">
                    Carregando…
                  </td>
                </tr>
              ) : empresasQuery.isError ? (
                <tr>
                  <td colSpan={9} className="px-4 py-8 text-center text-rose-600">
                    Não foi possível carregar os clientes.
                  </td>
                </tr>
              ) : empresas.length === 0 ? (
                <tr>
                  <td colSpan={9} className="px-4 py-8 text-center text-slate-500">
                    Nenhum cliente encontrado.
                  </td>
                </tr>
              ) : (
                empresas.map((empresa) => (
                  <tr key={empresa.empresaId} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-medium">
                      <Link
                        to="/admin/empresas/$empresaId"
                        params={{ empresaId: empresa.empresaId }}
                        className="text-primary hover:underline"
                      >
                        {empresa.name}
                      </Link>
                    </td>
                    <td className="px-4 py-3 text-slate-600">{empresa.taxId ?? "—"}</td>
                    <td className="px-4 py-3 text-slate-600">{empresa.corporateEmail ?? "—"}</td>
                    <td className="px-4 py-3 text-slate-600">{planLabel(empresa.commercialPlanType)}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={empresa.status} />
                    </td>
                    <td className="px-4 py-3 text-slate-600">{empresa.completedAttemptsInPeriod}</td>
                    <td className="px-4 py-3 font-medium tabular-nums text-slate-700">
                      {empresa.creditBalance}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {new Date(empresa.createdAt).toLocaleDateString("pt-BR")}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-1.5">
                        <Link to="/admin/empresas/$empresaId" params={{ empresaId: empresa.empresaId }}>
                          <Button variant="ghost" size="sm">
                            Ver
                          </Button>
                        </Link>
                        {empresa.status !== "CANCELADO" && (
                          <Button variant="outline" size="sm" onClick={() => setCreditEmpresa(empresa)}>
                            Créditos
                          </Button>
                        )}
                        {empresa.status === "SUSPENSO" || empresa.status === "CANCELADO" ? (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setReasonAction({ kind: "reactivate", empresa })}
                          >
                            Reativar
                          </Button>
                        ) : (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setReasonAction({ kind: "suspend", empresa })}
                          >
                            Suspender
                          </Button>
                        )}
                        {empresa.status !== "CANCELADO" && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-rose-600"
                            onClick={() => setReasonAction({ kind: "cancel", empresa })}
                          >
                            Cancelar
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 px-4 py-3">
          <span className="text-sm text-slate-500">
            {totalElements === 0 ? "Nenhum registro" : `${firstItem}–${lastItem} de ${totalElements}`}
          </span>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0 || empresasQuery.isFetching}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
            >
              <ChevronLeft className="size-4" /> Anterior
            </Button>
            <span className="min-w-28 text-center text-sm text-slate-600">
              Página {totalPages === 0 ? 0 : page + 1} de {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={totalPages === 0 || page + 1 >= totalPages || empresasQuery.isFetching}
              onClick={() => setPage((current) => current + 1)}
            >
              Próxima <ChevronRight className="size-4" />
            </Button>
          </div>
        </div>
      </div>

      <CreateEmpresaDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        onCreated={() => queryClient.invalidateQueries({ queryKey: ["admin-empresas"] })}
      />

      <ReasonDialog
        action={reasonAction}
        onClose={() => setReasonAction(null)}
        onDone={() => queryClient.invalidateQueries({ queryKey: ["admin-empresas"] })}
      />

      <GrantCreditsDialog
        empresa={creditEmpresa}
        onClose={() => setCreditEmpresa(null)}
        onDone={() => queryClient.invalidateQueries({ queryKey: ["admin-empresas"] })}
      />
    </AdminShell>
  );
}

function GrantCreditsDialog({
  empresa,
  onClose,
  onDone,
}: {
  empresa: EmpresaAdminSummary | null;
  onClose: () => void;
  onDone: () => void;
}) {
  const [amount, setAmount] = useState("10");
  const [note, setNote] = useState("");
  const parsedAmount = Number(amount);
  const validAmount = Number.isInteger(parsedAmount) && parsedAmount > 0 && parsedAmount <= 100000;

  const mutation = useMutation({
    mutationFn: async () => {
      if (!empresa) return;
      return grantAdminEmpresaCredits(empresa.empresaId, parsedAmount, note);
    },
    onSuccess: () => {
      onDone();
      setAmount("10");
      setNote("");
      onClose();
    },
  });

  return (
    <Dialog open={empresa !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Dar créditos de cortesia</DialogTitle>
          <DialogDescription>
            {empresa?.name} — os créditos são somados ao saldo atual ({empresa?.creditBalance ?? 0}).
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          <div>
            <Label>Quantidade de créditos</Label>
            <Input
              type="number"
              min={1}
              max={100000}
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              autoFocus
            />
          </div>
          <div>
            <Label>Observação (opcional)</Label>
            <Input
              value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="Ex.: liberação para teste"
            />
          </div>
          {mutation.isError && (
            <p className="text-sm text-rose-600">Falha ao conceder créditos. Tente novamente.</p>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button disabled={!validAmount || mutation.isPending} onClick={() => mutation.mutate()}>
            Conceder
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function ReasonDialog({
  action,
  onClose,
  onDone,
}: {
  action: { kind: "suspend" | "cancel" | "reactivate"; empresa: EmpresaAdminSummary } | null;
  onClose: () => void;
  onDone: () => void;
}) {
  const [reason, setReason] = useState("");

  const mutation = useMutation({
    mutationFn: async () => {
      if (!action) return;
      if (action.kind === "suspend") return suspendAdminEmpresa(action.empresa.empresaId, reason);
      if (action.kind === "cancel") return cancelAdminEmpresa(action.empresa.empresaId, reason);
      return reactivateAdminEmpresa(action.empresa.empresaId, reason);
    },
    onSuccess: () => {
      onDone();
      setReason("");
      onClose();
    },
  });

  const titles = {
    suspend: "Suspender cliente",
    cancel: "Cancelar cliente",
    reactivate: "Reativar cliente",
  } as const;

  return (
    <Dialog open={action !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{action ? titles[action.kind] : ""}</DialogTitle>
          <DialogDescription>
            {action?.empresa.name} — informe o motivo obrigatório para auditoria.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          <Label>Motivo</Label>
          <Input value={reason} onChange={(event) => setReason(event.target.value)} autoFocus />
          {mutation.isError && (
            <p className="text-sm text-rose-600">Falha ao aplicar a ação. Tente novamente.</p>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button
            disabled={reason.trim().length === 0 || mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            Confirmar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function CreateEmpresaDialog({
  open,
  onOpenChange,
  onCreated,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated: () => void;
}) {
  const empty: CreateEmpresaAdminRequest = {
    name: "",
    tradeName: "",
    legalName: "",
    taxId: "",
    corporateEmail: "",
    phone: "",
    website: "",
    healthVertical: false,
    commercialPlanType: "PROFISSIONAL",
    commercialCondition: "",
    initialStatus: "EM_TESTE",
    responsibleName: "",
    responsibleEmail: "",
    sendInvite: true,
  };
  const [form, setForm] = useState<CreateEmpresaAdminRequest>(empty);
  const [inviteUrl, setInviteUrl] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => createAdminEmpresa(form),
    onSuccess: (response) => {
      onCreated();
      setInviteUrl(response.inviteUrl);
      setForm(empty);
    },
  });

  function field(key: keyof CreateEmpresaAdminRequest, value: string | boolean) {
    setForm((previous) => ({ ...previous, [key]: value }));
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Novo cliente</DialogTitle>
          <DialogDescription>
            O usuário responsável é criado com papel EMPRESA.
          </DialogDescription>
        </DialogHeader>

        {inviteUrl !== null ? (
          <div className="space-y-3">
            <p className="text-sm text-emerald-700">Cliente cadastrado com sucesso.</p>
            {inviteUrl && (
              <div>
                <Label>Link de convite do responsável</Label>
                <Input readOnly value={inviteUrl} onFocus={(event) => event.target.select()} />
              </div>
            )}
            <DialogFooter>
              <Button
                onClick={() => {
                  setInviteUrl(null);
                  onOpenChange(false);
                }}
              >
                Concluir
              </Button>
            </DialogFooter>
          </div>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            <Text label="Nome da empresa *" value={form.name} onChange={(value) => field("name", value)} />
            <Text label="Nome fantasia" value={form.tradeName ?? ""} onChange={(value) => field("tradeName", value)} />
            <Text label="Razão social" value={form.legalName ?? ""} onChange={(value) => field("legalName", value)} />
            <Text label="CNPJ" value={form.taxId ?? ""} onChange={(value) => field("taxId", value)} />
            <Text
              label="E-mail corporativo"
              value={form.corporateEmail ?? ""}
              onChange={(value) => field("corporateEmail", value)}
            />
            <Text label="Telefone" value={form.phone ?? ""} onChange={(value) => field("phone", value)} />
            <Text label="Website" value={form.website ?? ""} onChange={(value) => field("website", value)} />
            <div>
              <Label className="text-xs text-slate-500">Plano comercial</Label>
              <select
                value={form.commercialPlanType}
                onChange={(event) => field("commercialPlanType", event.target.value)}
                className="block h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
              >
                {PLAN_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <Label className="text-xs text-slate-500">Status inicial</Label>
              <select
                value={form.initialStatus ?? "EM_TESTE"}
                onChange={(event) => field("initialStatus", event.target.value)}
                className="block h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
              >
                {(["EM_TESTE", "ATIVO"] as EmpresaStatus[]).map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </div>
            <div className="sm:col-span-2">
              <Text
                label="Condição comercial"
                value={form.commercialCondition ?? ""}
                onChange={(value) => field("commercialCondition", value)}
              />
            </div>
            <Text
              label="Nome do responsável *"
              value={form.responsibleName}
              onChange={(value) => field("responsibleName", value)}
            />
            <Text
              label="E-mail do responsável *"
              value={form.responsibleEmail}
              onChange={(value) => field("responsibleEmail", value)}
            />
            <label className="flex items-center gap-2 text-sm sm:col-span-2">
              <input
                type="checkbox"
                checked={form.healthVertical}
                onChange={(event) => field("healthVertical", event.target.checked)}
              />
              Vertical de saúde
            </label>
            <label className="flex items-center gap-2 text-sm sm:col-span-2">
              <input
                type="checkbox"
                checked={form.sendInvite}
                onChange={(event) => field("sendInvite", event.target.checked)}
              />
              Gerar link de convite para o responsável
            </label>

            {mutation.isError && (
              <p className="text-sm text-rose-600 sm:col-span-2">
                Não foi possível cadastrar. Verifique os campos obrigatórios.
              </p>
            )}

            <DialogFooter className="sm:col-span-2">
              <Button variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button
                disabled={
                  mutation.isPending ||
                  !form.name ||
                  !form.responsibleName ||
                  !form.responsibleEmail
                }
                onClick={() => mutation.mutate()}
              >
                Cadastrar cliente
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function Text({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div>
      <Label className="text-xs text-slate-500">{label}</Label>
      <Input value={value} onChange={(event) => onChange(event.target.value)} />
    </div>
  );
}
