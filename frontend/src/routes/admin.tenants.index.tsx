import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Download, Plus } from "lucide-react";
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
import {
  cancelAdminTenant,
  createAdminTenant,
  listAdminTenants,
  reactivateAdminTenant,
  suspendAdminTenant,
  type CommercialPlanType,
  type CreateTenantAdminRequest,
  type TenantAdminSummary,
  type TenantStatus,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/admin/tenants/")({
  head: () => ({ meta: [{ title: "Clientes · Admin Praxis" }] }),
  component: AdminTenantsPage,
});

const PLAN_OPTIONS: CommercialPlanType[] = ["AVULSO", "PROFISSIONAL", "ENTERPRISE"];
const STATUS_OPTIONS: TenantStatus[] = ["ATIVO", "EM_TESTE", "SUSPENSO", "CANCELADO"];

function AdminTenantsPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<TenantStatus | "">("");
  const [plan, setPlan] = useState<CommercialPlanType | "">("");
  const [createOpen, setCreateOpen] = useState(false);
  const [reasonAction, setReasonAction] = useState<
    | { kind: "suspend" | "cancel" | "reactivate"; tenant: TenantAdminSummary }
    | null
  >(null);

  const tenantsQuery = useQuery({
    queryKey: ["admin-tenants", search, status, plan],
    queryFn: () =>
      listAdminTenants({
        search: search || undefined,
        status: status || undefined,
        plan: plan || undefined,
      }),
  });

  const tenants = tenantsQuery.data ?? [];

  function exportCsv() {
    const header = ["Nome", "CNPJ", "E-mail", "Plano", "Status", "Uso no período", "Criado em"];
    const rows = tenants.map((t) => [
      t.name,
      t.taxId ?? "",
      t.corporateEmail ?? "",
      t.commercialPlanType,
      t.status,
      String(t.completedAttemptsInPeriod),
      t.createdAt,
    ]);
    const csv = [header, ...rows]
      .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(","))
      .join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "clientes.csv";
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <AdminShell>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Clientes</h1>
          <p className="mt-1 text-sm text-slate-500">
            Cada linha é um cliente da plataforma (tenant).
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={exportCsv}>
            <Download className="size-4" /> Exportar CSV
          </Button>
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="size-4" /> Novo cliente
          </Button>
        </div>
      </div>

      <div className="mt-6 flex flex-wrap items-end gap-3 rounded-xl border border-slate-200 bg-white p-4">
        <div className="flex-1 min-w-[220px]">
          <Label className="text-xs text-slate-500">Busca livre</Label>
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Nome, CNPJ ou e-mail"
          />
        </div>
        <div>
          <Label className="text-xs text-slate-500">Status</Label>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value as TenantStatus | "")}
            className="block h-9 rounded-md border border-input bg-background px-3 text-sm"
          >
            <option value="">Todos</option>
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <div>
          <Label className="text-xs text-slate-500">Plano comercial</Label>
          <select
            value={plan}
            onChange={(e) => setPlan(e.target.value as CommercialPlanType | "")}
            className="block h-9 rounded-md border border-input bg-background px-3 text-sm"
          >
            <option value="">Todos</option>
            {PLAN_OPTIONS.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="mt-4 overflow-hidden rounded-xl border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">Nome</th>
              <th className="px-4 py-3">CNPJ</th>
              <th className="px-4 py-3">E-mail</th>
              <th className="px-4 py-3">Plano</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Uso</th>
              <th className="px-4 py-3">Criado em</th>
              <th className="px-4 py-3 text-right">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {tenantsQuery.isLoading ? (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-slate-500">
                  Carregando…
                </td>
              </tr>
            ) : tenants.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-slate-500">
                  Nenhum cliente encontrado.
                </td>
              </tr>
            ) : (
              tenants.map((tenant) => (
                <tr key={tenant.tenantId} className="hover:bg-slate-50">
                  <td className="px-4 py-3 font-medium">
                    <Link
                      to="/admin/tenants/$tenantId"
                      params={{ tenantId: tenant.tenantId }}
                      className="text-primary hover:underline"
                    >
                      {tenant.name}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-slate-600">{tenant.taxId ?? "—"}</td>
                  <td className="px-4 py-3 text-slate-600">{tenant.corporateEmail ?? "—"}</td>
                  <td className="px-4 py-3 text-slate-600">{planLabel(tenant.commercialPlanType)}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={tenant.status} />
                  </td>
                  <td className="px-4 py-3 text-slate-600">{tenant.completedAttemptsInPeriod}</td>
                  <td className="px-4 py-3 text-slate-600">
                    {new Date(tenant.createdAt).toLocaleDateString("pt-BR")}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-1.5">
                      <Link
                        to="/admin/tenants/$tenantId"
                        params={{ tenantId: tenant.tenantId }}
                      >
                        <Button variant="ghost" size="sm">
                          Ver
                        </Button>
                      </Link>
                      {tenant.status === "SUSPENSO" || tenant.status === "CANCELADO" ? (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setReasonAction({ kind: "reactivate", tenant })}
                        >
                          Reativar
                        </Button>
                      ) : (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setReasonAction({ kind: "suspend", tenant })}
                        >
                          Suspender
                        </Button>
                      )}
                      {tenant.status !== "CANCELADO" && (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-rose-600"
                          onClick={() => setReasonAction({ kind: "cancel", tenant })}
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

      <CreateTenantDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        onCreated={() => queryClient.invalidateQueries({ queryKey: ["admin-tenants"] })}
      />

      <ReasonDialog
        action={reasonAction}
        onClose={() => setReasonAction(null)}
        onDone={() => queryClient.invalidateQueries({ queryKey: ["admin-tenants"] })}
      />
    </AdminShell>
  );
}

function ReasonDialog({
  action,
  onClose,
  onDone,
}: {
  action: { kind: "suspend" | "cancel" | "reactivate"; tenant: TenantAdminSummary } | null;
  onClose: () => void;
  onDone: () => void;
}) {
  const [reason, setReason] = useState("");

  const mutation = useMutation({
    mutationFn: async () => {
      if (!action) return;
      if (action.kind === "suspend") return suspendAdminTenant(action.tenant.tenantId, reason);
      if (action.kind === "cancel") return cancelAdminTenant(action.tenant.tenantId, reason);
      return reactivateAdminTenant(action.tenant.tenantId, reason);
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
            {action?.tenant.name} — informe o motivo (obrigatório, registrado em auditoria).
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          <Label>Motivo</Label>
          <Input value={reason} onChange={(e) => setReason(e.target.value)} autoFocus />
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

function CreateTenantDialog({
  open,
  onOpenChange,
  onCreated,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated: () => void;
}) {
  const empty: CreateTenantAdminRequest = {
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
  const [form, setForm] = useState<CreateTenantAdminRequest>(empty);
  const [inviteUrl, setInviteUrl] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => createAdminTenant(form),
    onSuccess: (response) => {
      onCreated();
      setInviteUrl(response.inviteUrl);
      setForm(empty);
    },
  });

  function field(key: keyof CreateTenantAdminRequest, value: string | boolean) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Novo cliente</DialogTitle>
          <DialogDescription>
            O usuário responsável é criado com papel EMPRESA (nunca ADMIN).
          </DialogDescription>
        </DialogHeader>

        {inviteUrl !== null ? (
          <div className="space-y-3">
            <p className="text-sm text-emerald-700">Cliente cadastrado com sucesso.</p>
            {inviteUrl && (
              <div>
                <Label>Link de convite do responsável</Label>
                <Input readOnly value={inviteUrl} onFocus={(e) => e.target.select()} />
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
            <Text label="Nome da empresa *" value={form.name} onChange={(v) => field("name", v)} />
            <Text label="Nome fantasia" value={form.tradeName ?? ""} onChange={(v) => field("tradeName", v)} />
            <Text label="Razão social" value={form.legalName ?? ""} onChange={(v) => field("legalName", v)} />
            <Text label="CNPJ" value={form.taxId ?? ""} onChange={(v) => field("taxId", v)} />
            <Text label="E-mail corporativo" value={form.corporateEmail ?? ""} onChange={(v) => field("corporateEmail", v)} />
            <Text label="Telefone" value={form.phone ?? ""} onChange={(v) => field("phone", v)} />
            <Text label="Website" value={form.website ?? ""} onChange={(v) => field("website", v)} />
            <div>
              <Label className="text-xs text-slate-500">Plano comercial</Label>
              <select
                value={form.commercialPlanType}
                onChange={(e) => field("commercialPlanType", e.target.value)}
                className="block h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
              >
                {PLAN_OPTIONS.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <Label className="text-xs text-slate-500">Status inicial</Label>
              <select
                value={form.initialStatus ?? "EM_TESTE"}
                onChange={(e) => field("initialStatus", e.target.value)}
                className="block h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
              >
                {(["EM_TESTE", "ATIVO"] as TenantStatus[]).map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </div>
            <div className="sm:col-span-2">
              <Text
                label="Condição comercial"
                value={form.commercialCondition ?? ""}
                onChange={(v) => field("commercialCondition", v)}
              />
            </div>
            <Text
              label="Nome do responsável *"
              value={form.responsibleName}
              onChange={(v) => field("responsibleName", v)}
            />
            <Text
              label="E-mail do responsável *"
              value={form.responsibleEmail}
              onChange={(v) => field("responsibleEmail", v)}
            />
            <label className="flex items-center gap-2 text-sm sm:col-span-2">
              <input
                type="checkbox"
                checked={form.healthVertical}
                onChange={(e) => field("healthVertical", e.target.checked)}
              />
              Vertical de saúde (LGPD — dado sensível)
            </label>
            <label className="flex items-center gap-2 text-sm sm:col-span-2">
              <input
                type="checkbox"
                checked={form.sendInvite}
                onChange={(e) => field("sendInvite", e.target.checked)}
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
      <Input value={value} onChange={(e) => onChange(e.target.value)} />
    </div>
  );
}
