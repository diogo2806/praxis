import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ArrowLeft } from "lucide-react";
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
import { cn } from "@/lib/utils";
import {
  blockAdminTenantUser,
  cancelAdminTenant,
  getAdminTenant,
  getAdminTenantAudit,
  getAdminTenantUsage,
  inviteAdminTenantUser,
  listAdminTenantUsers,
  reactivateAdminTenant,
  resendAdminTenantUserInvite,
  suspendAdminTenant,
  unblockAdminTenantUser,
  type TenantAdminDetail,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/admin/tenants/$tenantId")({
  head: () => ({ meta: [{ title: "Cliente · Admin Praxis" }] }),
  component: AdminTenantDetailPage,
});

type Tab = "geral" | "uso" | "acessos" | "historico" | "auditoria";

const TABS: { id: Tab; label: string }[] = [
  { id: "geral", label: "Geral" },
  { id: "uso", label: "Uso" },
  { id: "acessos", label: "Acessos" },
  { id: "historico", label: "Histórico" },
  { id: "auditoria", label: "Auditoria" },
];

function AdminTenantDetailPage() {
  const { tenantId } = Route.useParams();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<Tab>("geral");
  const [reasonAction, setReasonAction] = useState<"suspend" | "cancel" | "reactivate" | null>(null);

  const tenantQuery = useQuery({
    queryKey: ["admin-tenant", tenantId],
    queryFn: () => getAdminTenant(tenantId),
  });

  function refresh() {
    queryClient.invalidateQueries({ queryKey: ["admin-tenant", tenantId] });
  }

  if (tenantQuery.isLoading) {
    return (
      <AdminShell>
        <p className="text-slate-500">Carregando cliente…</p>
      </AdminShell>
    );
  }
  if (tenantQuery.isError || !tenantQuery.data) {
    return (
      <AdminShell>
        <p className="text-rose-600">Cliente não encontrado.</p>
        <Link to="/admin/tenants" className="text-sm text-primary hover:underline">
          Voltar para clientes
        </Link>
      </AdminShell>
    );
  }

  const tenant = tenantQuery.data;
  const blocked = tenant.status === "SUSPENSO" || tenant.status === "CANCELADO";

  return (
    <AdminShell>
      <Link
        to="/admin/tenants"
        className="mb-4 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700"
      >
        <ArrowLeft className="size-4" /> Clientes
      </Link>

      <div className="flex items-start justify-between rounded-xl border border-slate-200 bg-white p-5">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold">{tenant.name}</h1>
            <StatusBadge status={tenant.status} />
          </div>
          <p className="mt-1 text-sm text-slate-500">
            {planLabel(tenant.commercialPlanType)} · criado em{" "}
            {new Date(tenant.createdAt).toLocaleDateString("pt-BR")}
          </p>
        </div>
        <div className="flex gap-2">
          {blocked ? (
            <Button variant="outline" onClick={() => setReasonAction("reactivate")}>
              Reativar
            </Button>
          ) : (
            <Button variant="outline" onClick={() => setReasonAction("suspend")}>
              Suspender
            </Button>
          )}
          {tenant.status !== "CANCELADO" && (
            <Button variant="ghost" className="text-rose-600" onClick={() => setReasonAction("cancel")}>
              Cancelar
            </Button>
          )}
        </div>
      </div>

      <div className="mt-6 flex gap-1 border-b border-slate-200">
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={cn(
              "border-b-2 px-4 py-2 text-sm font-medium transition-colors",
              tab === t.id
                ? "border-primary text-primary"
                : "border-transparent text-slate-500 hover:text-slate-700",
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {tab === "geral" && <GeralTab tenant={tenant} />}
        {tab === "uso" && <UsoTab tenantId={tenantId} />}
        {tab === "acessos" && <AcessosTab tenantId={tenantId} />}
        {tab === "historico" && <AuditTab tenantId={tenantId} mode="historico" />}
        {tab === "auditoria" && <AuditTab tenantId={tenantId} mode="auditoria" />}
      </div>

      <ReasonDialog
        tenantId={tenantId}
        action={reasonAction}
        onClose={() => setReasonAction(null)}
        onDone={refresh}
      />
    </AdminShell>
  );
}

function Row({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div className="flex justify-between gap-4 border-b border-slate-100 py-2 text-sm">
      <span className="text-slate-500">{label}</span>
      <span className="text-right font-medium text-slate-800">{value || "—"}</span>
    </div>
  );
}

function GeralTab({ tenant }: { tenant: TenantAdminDetail }) {
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="mb-3 font-semibold">Dados cadastrais</h2>
        <Row label="Nome da empresa" value={tenant.name} />
        <Row label="Nome fantasia" value={tenant.tradeName} />
        <Row label="Razão social" value={tenant.legalName} />
        <Row label="CNPJ" value={tenant.taxId} />
        <Row label="E-mail corporativo" value={tenant.corporateEmail} />
        <Row label="Telefone" value={tenant.phone} />
        <Row label="Website" value={tenant.website} />
        <Row label="Vertical de saúde" value={tenant.healthVertical ? "Sim" : "Não"} />
      </section>
      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="mb-3 font-semibold">Situação comercial</h2>
        <Row label="Plano comercial" value={planLabel(tenant.commercialPlanType)} />
        <Row label="Condição comercial" value={tenant.commercialCondition} />
        <Row label="Status atual" value={tenant.status} />
        <Row label="Uso no período" value={String(tenant.completedAttemptsInPeriod)} />
        <Row
          label="Criado em"
          value={new Date(tenant.createdAt).toLocaleString("pt-BR")}
        />
        <Row
          label="Atualizado em"
          value={new Date(tenant.updatedAt).toLocaleString("pt-BR")}
        />
      </section>
    </div>
  );
}

function UsoTab({ tenantId }: { tenantId: string }) {
  const usageQuery = useQuery({
    queryKey: ["admin-tenant-usage", tenantId],
    queryFn: () => getAdminTenantUsage(tenantId),
  });

  if (usageQuery.isLoading) return <p className="text-slate-500">Carregando uso…</p>;
  if (!usageQuery.data) return <p className="text-rose-600">Falha ao carregar uso.</p>;
  const usage = usageQuery.data;

  return (
    <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
      <Metric label="Concluídas no período" value={usage.completedAttempts} />
      <Metric label="Últimos 7 dias" value={usage.completedAttemptsLast7Days} />
      <Metric label="Últimos 30 dias" value={usage.completedAttemptsLast30Days} />
      <Metric label="Total histórico" value={usage.completedAttemptsAllTime} />
      <div className="col-span-2 rounded-xl border border-slate-200 bg-white p-5 md:col-span-4">
        <p className="text-sm text-slate-500">Última avaliação concluída</p>
        <p className="mt-1 font-medium">
          {usage.lastCompletedAttemptAt
            ? new Date(usage.lastCompletedAttemptAt).toLocaleString("pt-BR")
            : "Nenhuma avaliação concluída"}
        </p>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-semibold">{value}</p>
    </div>
  );
}

function AcessosTab({ tenantId }: { tenantId: string }) {
  const queryClient = useQueryClient();
  const [inviteOpen, setInviteOpen] = useState(false);
  const usersQuery = useQuery({
    queryKey: ["admin-tenant-users", tenantId],
    queryFn: () => listAdminTenantUsers(tenantId),
  });

  function refresh() {
    queryClient.invalidateQueries({ queryKey: ["admin-tenant-users", tenantId] });
  }

  const blockMutation = useMutation({
    mutationFn: (userId: number) => blockAdminTenantUser(tenantId, userId),
    onSuccess: refresh,
  });
  const unblockMutation = useMutation({
    mutationFn: (userId: number) => unblockAdminTenantUser(tenantId, userId),
    onSuccess: refresh,
  });
  const resendMutation = useMutation({
    mutationFn: (userId: number) => resendAdminTenantUserInvite(tenantId, userId),
    onSuccess: refresh,
  });

  const users = usersQuery.data ?? [];

  return (
    <div>
      <div className="mb-3 flex justify-end">
        <Button size="sm" onClick={() => setInviteOpen(true)}>
          Convidar usuário
        </Button>
      </div>
      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">Nome</th>
              <th className="px-4 py-3">E-mail</th>
              <th className="px-4 py-3">Perfil</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Último acesso</th>
              <th className="px-4 py-3 text-right">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {users.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-6 text-center text-slate-500">
                  Sem usuários.
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id}>
                  <td className="px-4 py-3 font-medium">{user.name}</td>
                  <td className="px-4 py-3 text-slate-600">{user.email}</td>
                  <td className="px-4 py-3 text-slate-600">{user.roles.join(", ")}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={user.status} />
                  </td>
                  <td className="px-4 py-3 text-slate-600">
                    {user.lastLoginAt
                      ? new Date(user.lastLoginAt).toLocaleString("pt-BR")
                      : "—"}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-1.5">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => resendMutation.mutate(user.id)}
                      >
                        Reenviar convite
                      </Button>
                      {user.status === "BLOQUEADO" ? (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => unblockMutation.mutate(user.id)}
                        >
                          Desbloquear
                        </Button>
                      ) : (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => blockMutation.mutate(user.id)}
                        >
                          Bloquear
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

      <InviteDialog
        tenantId={tenantId}
        open={inviteOpen}
        onOpenChange={setInviteOpen}
        onDone={refresh}
      />
    </div>
  );
}

function InviteDialog({
  tenantId,
  open,
  onOpenChange,
  onDone,
}: {
  tenantId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDone: () => void;
}) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [inviteUrl, setInviteUrl] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => inviteAdminTenantUser(tenantId, { name, email }),
    onSuccess: (response) => {
      onDone();
      setInviteUrl(response.inviteUrl);
      setName("");
      setEmail("");
    },
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Convidar usuário</DialogTitle>
          <DialogDescription>O usuário recebe o papel EMPRESA.</DialogDescription>
        </DialogHeader>
        {inviteUrl !== null ? (
          <div className="space-y-3">
            {inviteUrl && (
              <div>
                <Label>Link de convite</Label>
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
          <div className="space-y-3">
            <div>
              <Label>Nome</Label>
              <Input value={name} onChange={(e) => setName(e.target.value)} />
            </div>
            <div>
              <Label>E-mail</Label>
              <Input value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            {mutation.isError && (
              <p className="text-sm text-rose-600">Falha ao convidar. Verifique o e-mail.</p>
            )}
            <DialogFooter>
              <Button variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button disabled={!name || !email || mutation.isPending} onClick={() => mutation.mutate()}>
                Convidar
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function AuditTab({ tenantId, mode }: { tenantId: string; mode: "historico" | "auditoria" }) {
  const auditQuery = useQuery({
    queryKey: ["admin-tenant-audit", tenantId],
    queryFn: () => getAdminTenantAudit(tenantId),
  });

  if (auditQuery.isLoading) return <p className="text-slate-500">Carregando…</p>;
  const events = auditQuery.data ?? [];

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5">
      {mode === "auditoria" && (
        <p className="mb-3 text-xs text-slate-500">
          Trilha append-only (somente leitura): eventos não podem ser editados nem excluídos.
        </p>
      )}
      {events.length === 0 ? (
        <p className="text-sm text-slate-500">Nenhum evento registrado.</p>
      ) : (
        <ol className="space-y-3">
          {events.map((event) => (
            <li key={event.id} className="border-l-2 border-slate-200 pl-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">{event.message}</span>
                <span className="text-xs text-slate-400">
                  {new Date(event.createdAt).toLocaleString("pt-BR")}
                </span>
              </div>
              <p className="text-xs text-slate-500">
                {event.eventType}
                {event.actorUserId ? ` · operador ${event.actorUserId}` : ""}
              </p>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

function ReasonDialog({
  tenantId,
  action,
  onClose,
  onDone,
}: {
  tenantId: string;
  action: "suspend" | "cancel" | "reactivate" | null;
  onClose: () => void;
  onDone: () => void;
}) {
  const [reason, setReason] = useState("");
  const mutation = useMutation({
    mutationFn: async () => {
      if (action === "suspend") return suspendAdminTenant(tenantId, reason);
      if (action === "cancel") return cancelAdminTenant(tenantId, reason);
      if (action === "reactivate") return reactivateAdminTenant(tenantId, reason);
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
          <DialogTitle>{action ? titles[action] : ""}</DialogTitle>
          <DialogDescription>Motivo obrigatório, registrado em auditoria.</DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          <Label>Motivo</Label>
          <Input value={reason} onChange={(e) => setReason(e.target.value)} autoFocus />
          {mutation.isError && <p className="text-sm text-rose-600">Falha ao aplicar a ação.</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button disabled={reason.trim().length === 0 || mutation.isPending} onClick={() => mutation.mutate()}>
            Confirmar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
