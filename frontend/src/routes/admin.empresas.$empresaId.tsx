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
  blockAdminEmpresaUser,
  cancelAdminEmpresa,
  createCreditCheckout,
  createEmpresaSubscription,
  getAdminEmpresa,
  getAdminEmpresaAudit,
  getAdminEmpresaUsage,
  getEmpresaBilling,
  inviteAdminEmpresaUser,
  listAdminEmpresaUsers,
  listBillingPlans,
  reactivateAdminEmpresa,
  resendAdminEmpresaUserInvite,
  suspendAdminEmpresa,
  syncEmpresaBilling,
  unblockAdminEmpresaUser,
  type EmpresaAdminDetail,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/admin/empresas/$empresaId")({
  head: () => ({ meta: [{ title: "Cliente · Admin Praxis" }] }),
  component: AdminEmpresaDetailPage,
});

type Tab =
  | "geral"
  | "uso"
  | "acessos"
  | "assinatura"
  | "pagamentos"
  | "historico"
  | "auditoria";

const TABS: { id: Tab; label: string }[] = [
  { id: "geral", label: "Geral" },
  { id: "uso", label: "Uso" },
  { id: "acessos", label: "Acessos" },
  { id: "assinatura", label: "Assinatura" },
  { id: "pagamentos", label: "Pagamentos" },
  { id: "historico", label: "Histórico" },
  { id: "auditoria", label: "Auditoria" },
];

function AdminEmpresaDetailPage() {
  const { empresaId } = Route.useParams();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<Tab>("geral");
  const [reasonAction, setReasonAction] = useState<"suspend" | "cancel" | "reactivate" | null>(null);

  const empresaQuery = useQuery({
    queryKey: ["admin-empresa", empresaId],
    queryFn: () => getAdminEmpresa(empresaId),
  });

  function refresh() {
    queryClient.invalidateQueries({ queryKey: ["admin-empresa", empresaId] });
  }

  if (empresaQuery.isLoading) {
    return (
      <AdminShell>
        <p className="text-slate-500">Carregando cliente…</p>
      </AdminShell>
    );
  }
  if (empresaQuery.isError || !empresaQuery.data) {
    return (
      <AdminShell>
        <p className="text-rose-600">Cliente não encontrado.</p>
        <Link to="/admin/empresas" className="text-sm text-primary hover:underline">
          Voltar para clientes
        </Link>
      </AdminShell>
    );
  }

  const empresa = empresaQuery.data;
  const blocked = empresa.status === "SUSPENSO" || empresa.status === "CANCELADO";

  return (
    <AdminShell>
      <Link
        to="/admin/empresas"
        className="mb-4 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700"
      >
        <ArrowLeft className="size-4" /> Clientes
      </Link>

      <div className="flex items-start justify-between rounded-xl border border-slate-200 bg-white p-5">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold">{empresa.name}</h1>
            <StatusBadge status={empresa.status} />
          </div>
          <p className="mt-1 text-sm text-slate-500">
            {planLabel(empresa.commercialPlanType)} · criado em{" "}
            {new Date(empresa.createdAt).toLocaleDateString("pt-BR")}
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
          {empresa.status !== "CANCELADO" && (
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
        {tab === "geral" && <GeralTab empresa={empresa} />}
        {tab === "uso" && <UsoTab empresaId={empresaId} />}
        {tab === "acessos" && <AcessosTab empresaId={empresaId} />}
        {tab === "assinatura" && <AssinaturaTab empresaId={empresaId} />}
        {tab === "pagamentos" && <PagamentosTab empresaId={empresaId} />}
        {tab === "historico" && <AuditTab empresaId={empresaId} mode="historico" />}
        {tab === "auditoria" && <AuditTab empresaId={empresaId} mode="auditoria" />}
      </div>

      <ReasonDialog
        empresaId={empresaId}
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

function GeralTab({ empresa }: { empresa: EmpresaAdminDetail }) {
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="mb-3 font-semibold">Dados cadastrais</h2>
        <Row label="Nome da empresa" value={empresa.name} />
        <Row label="Nome fantasia" value={empresa.tradeName} />
        <Row label="Razão social" value={empresa.legalName} />
        <Row label="CNPJ" value={empresa.taxId} />
        <Row label="E-mail corporativo" value={empresa.corporateEmail} />
        <Row label="Telefone" value={empresa.phone} />
        <Row label="Website" value={empresa.website} />
        <Row label="Vertical de saúde" value={empresa.healthVertical ? "Sim" : "Não"} />
      </section>
      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="mb-3 font-semibold">Situação comercial</h2>
        <Row label="Plano comercial" value={planLabel(empresa.commercialPlanType)} />
        <Row label="Condição comercial" value={empresa.commercialCondition} />
        <Row label="Status atual" value={empresa.status} />
        <Row label="Uso no período" value={String(empresa.completedAttemptsInPeriod)} />
        <Row
          label="Criado em"
          value={new Date(empresa.createdAt).toLocaleString("pt-BR")}
        />
        <Row
          label="Atualizado em"
          value={new Date(empresa.updatedAt).toLocaleString("pt-BR")}
        />
      </section>
    </div>
  );
}

function UsoTab({ empresaId }: { empresaId: string }) {
  const usageQuery = useQuery({
    queryKey: ["admin-empresa-usage", empresaId],
    queryFn: () => getAdminEmpresaUsage(empresaId),
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

function AcessosTab({ empresaId }: { empresaId: string }) {
  const queryClient = useQueryClient();
  const [inviteOpen, setInviteOpen] = useState(false);
  const usersQuery = useQuery({
    queryKey: ["admin-empresa-users", empresaId],
    queryFn: () => listAdminEmpresaUsers(empresaId),
  });

  function refresh() {
    queryClient.invalidateQueries({ queryKey: ["admin-empresa-users", empresaId] });
  }

  const blockMutation = useMutation({
    mutationFn: (userId: number) => blockAdminEmpresaUser(empresaId, userId),
    onSuccess: refresh,
  });
  const unblockMutation = useMutation({
    mutationFn: (userId: number) => unblockAdminEmpresaUser(empresaId, userId),
    onSuccess: refresh,
  });
  const resendMutation = useMutation({
    mutationFn: (userId: number) => resendAdminEmpresaUserInvite(empresaId, userId),
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
        empresaId={empresaId}
        open={inviteOpen}
        onOpenChange={setInviteOpen}
        onDone={refresh}
      />
    </div>
  );
}

function InviteDialog({
  empresaId,
  open,
  onOpenChange,
  onDone,
}: {
  empresaId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDone: () => void;
}) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [inviteUrl, setInviteUrl] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => inviteAdminEmpresaUser(empresaId, { name, email }),
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

function AuditTab({ empresaId, mode }: { empresaId: string; mode: "historico" | "auditoria" }) {
  const auditQuery = useQuery({
    queryKey: ["admin-empresa-audit", empresaId],
    queryFn: () => getAdminEmpresaAudit(empresaId),
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

function formatBRL(cents: number): string {
  return (cents / 100).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function AssinaturaTab({ empresaId }: { empresaId: string }) {
  const queryClient = useQueryClient();
  const billingQuery = useQuery({
    queryKey: ["admin-empresa-billing", empresaId],
    queryFn: () => getEmpresaBilling(empresaId),
  });
  const plansQuery = useQuery({ queryKey: ["billing-plans"], queryFn: listBillingPlans });
  const [planId, setPlanId] = useState<number | "">("");
  const [link, setLink] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: async () => {
      if (planId === "") return null;
      const plan = (plansQuery.data ?? []).find((p) => p.id === planId);
      if (!plan) return null;
      return plan.planType === "AVULSO"
        ? createCreditCheckout(empresaId, plan.id)
        : createEmpresaSubscription(empresaId, plan.id);
    },
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["admin-empresa-billing", empresaId] });
      setLink(result?.initPoint ?? null);
    },
  });

  if (billingQuery.isLoading) return <p className="text-slate-500">Carregando cobrança…</p>;
  const billing = billingQuery.data;

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="mb-3 font-semibold">Situação financeira</h2>
        <Row label="Plano comercial" value={billing?.commercialPlanType} />
        <Row label="Status" value={billing?.status} />
        <Row label="Saldo de créditos (AVULSO)" value={String(billing?.creditBalance ?? 0)} />
        {billing?.subscription ? (
          <>
            <Row label="Assinatura" value={billing.subscription.status} />
            <Row
              label="Próxima cobrança"
              value={
                billing.subscription.currentPeriodEnd
                  ? new Date(billing.subscription.currentPeriodEnd).toLocaleDateString("pt-BR")
                  : "—"
              }
            />
            {billing.subscription.initPoint && (
              <a
                className="mt-2 inline-block text-sm text-primary hover:underline"
                href={billing.subscription.initPoint}
                target="_blank"
                rel="noreferrer"
              >
                Link de pagamento da assinatura
              </a>
            )}
          </>
        ) : (
          <p className="mt-2 text-sm text-slate-500">Sem assinatura ativa.</p>
        )}
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="mb-3 font-semibold">Gerar cobrança (Mercado Pago)</h2>
        <p className="mb-3 text-xs text-slate-500">
          Para AVULSO gera compra de créditos; para PROFISSIONAL cria a assinatura recorrente.
          O pagamento só é confirmado por consulta ao Mercado Pago.
        </p>
        <Label className="text-xs text-slate-500">Plano</Label>
        <select
          value={planId}
          onChange={(e) => setPlanId(e.target.value ? Number(e.target.value) : "")}
          className="mb-3 block h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
        >
          <option value="">Selecione…</option>
          {(plansQuery.data ?? []).map((plan) => (
            <option key={plan.id} value={plan.id}>
              {plan.name} — {formatBRL(plan.priceCents)}
              {plan.creditAmount ? ` (${plan.creditAmount} créditos)` : ""}
            </option>
          ))}
        </select>
        <Button disabled={planId === "" || mutation.isPending} onClick={() => mutation.mutate()}>
          Gerar cobrança
        </Button>
        {mutation.isError && (
          <p className="mt-2 text-sm text-rose-600">
            Falha ao gerar a cobrança. Verifique se a integração Mercado Pago está habilitada.
          </p>
        )}
        {link && (
          <div className="mt-3">
            <Label>Link de pagamento</Label>
            <Input readOnly value={link} onFocus={(e) => e.target.select()} />
          </div>
        )}
      </section>
    </div>
  );
}

function PagamentosTab({ empresaId }: { empresaId: string }) {
  const queryClient = useQueryClient();
  const billingQuery = useQuery({
    queryKey: ["admin-empresa-billing", empresaId],
    queryFn: () => getEmpresaBilling(empresaId),
  });
  const [resourceType, setResourceType] = useState("payment");
  const [resourceId, setResourceId] = useState("");

  const syncMutation = useMutation({
    mutationFn: () => syncEmpresaBilling(empresaId, resourceType, resourceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-empresa-billing", empresaId] });
      setResourceId("");
    },
  });

  const events = billingQuery.data?.events ?? [];

  return (
    <div className="space-y-6">
      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="mb-1 font-semibold">Sincronização manual</h2>
        <p className="mb-3 text-xs text-slate-500">
          Consulta o Mercado Pago e aplica o resultado. Nunca marca pagamento como aprovado sem consultar.
        </p>
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <Label className="text-xs text-slate-500">Tipo</Label>
            <select
              value={resourceType}
              onChange={(e) => setResourceType(e.target.value)}
              className="block h-9 rounded-md border border-input bg-background px-3 text-sm"
            >
              <option value="payment">Pagamento</option>
              <option value="preapproval">Assinatura</option>
            </select>
          </div>
          <div className="flex-1 min-w-[200px]">
            <Label className="text-xs text-slate-500">ID do recurso no Mercado Pago</Label>
            <Input value={resourceId} onChange={(e) => setResourceId(e.target.value)} />
          </div>
          <Button disabled={!resourceId || syncMutation.isPending} onClick={() => syncMutation.mutate()}>
            Sincronizar
          </Button>
        </div>
        {syncMutation.isError && (
          <p className="mt-2 text-sm text-rose-600">Falha ao sincronizar com o Mercado Pago.</p>
        )}
      </section>

      <section className="overflow-hidden rounded-xl border border-slate-200 bg-white">
        <div className="border-b border-slate-100 px-5 py-3">
          <h2 className="font-semibold">Eventos financeiros</h2>
          <p className="text-xs text-slate-500">Trilha append-only (somente leitura).</p>
        </div>
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">Evento</th>
              <th className="px-4 py-3">Recurso MP</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Valor</th>
              <th className="px-4 py-3">Data</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {events.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-slate-500">
                  Nenhum evento financeiro.
                </td>
              </tr>
            ) : (
              events.map((event) => (
                <tr key={event.id}>
                  <td className="px-4 py-3 font-medium">{event.eventType}</td>
                  <td className="px-4 py-3 text-slate-600">
                    {event.mpResourceType}
                    {event.mpResourceId ? ` · ${event.mpResourceId}` : ""}
                  </td>
                  <td className="px-4 py-3 text-slate-600">{event.mpStatus ?? "—"}</td>
                  <td className="px-4 py-3 text-slate-600">
                    {event.amountCents != null ? formatBRL(event.amountCents) : "—"}
                  </td>
                  <td className="px-4 py-3 text-slate-600">
                    {new Date(event.createdAt).toLocaleString("pt-BR")}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>
    </div>
  );
}

function ReasonDialog({
  empresaId,
  action,
  onClose,
  onDone,
}: {
  empresaId: string;
  action: "suspend" | "cancel" | "reactivate" | null;
  onClose: () => void;
  onDone: () => void;
}) {
  const [reason, setReason] = useState("");
  const mutation = useMutation({
    mutationFn: async () => {
      if (action === "suspend") return suspendAdminEmpresa(empresaId, reason);
      if (action === "cancel") return cancelAdminEmpresa(empresaId, reason);
      if (action === "reactivate") return reactivateAdminEmpresa(empresaId, reason);
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
