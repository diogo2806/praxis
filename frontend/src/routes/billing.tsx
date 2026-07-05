import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertCircle,
  AlertTriangle,
  BadgeCheck,
  Building2,
  CalendarClock,
  CheckCircle2,
  CreditCard,
  ExternalLink,
  History,
  Info,
  Loader2,
  RefreshCw,
  ShoppingCart,
  XCircle,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import {
  type BillingEvent,
  type ClientBillingResponse,
  type CommercialPlanType,
  type CreditMovement,
  type FinancialStatus,
  type SubscriptionPlan,
  cancelClientSubscription,
  createClientCreditCheckout,
  createClientSubscriptionCheckout,
  getClientBilling,
  listClientBillingPlans,
  syncClientSubscription,
} from "@/lib/api/client-billing";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/billing")({
  head: () => ({
    meta: [{ title: "Planos, pagamentos e créditos - Práxis" }],
  }),
  component: BillingPage,
});

function BillingPage() {
  const queryClient = useQueryClient();
  const billingQuery = useQuery({
    queryKey: ["billing"],
    queryFn: getClientBilling,
    retry: false,
  });
  const plansQuery = useQuery({
    queryKey: ["billing", "plans"],
    queryFn: listClientBillingPlans,
    retry: false,
  });
  const refreshBilling = () => queryClient.invalidateQueries({ queryKey: ["billing"] });

  const creditCheckout = useMutation({
    mutationFn: createClientCreditCheckout,
    onSuccess: (checkout) => {
      openCheckout(checkout.initPoint);
      void refreshBilling();
    },
  });
  const subscriptionCheckout = useMutation({
    mutationFn: createClientSubscriptionCheckout,
    onSuccess: (checkout) => {
      openCheckout(checkout.initPoint);
      void refreshBilling();
    },
  });
  const syncSubscription = useMutation({
    mutationFn: syncClientSubscription,
    onSuccess: () => void refreshBilling(),
  });
  const cancelSubscription = useMutation({
    mutationFn: cancelClientSubscription,
    onSuccess: () => void refreshBilling(),
  });

  const error = messageFromError(
    creditCheckout.error ?? subscriptionCheckout.error ?? syncSubscription.error ?? cancelSubscription.error,
  );
  const busy = creditCheckout.isPending || subscriptionCheckout.isPending || syncSubscription.isPending || cancelSubscription.isPending;

  return (
    <AppShell>
      <div className="mb-6 space-y-2">
        <h1 className="text-2xl font-semibold">Planos, pagamentos e créditos</h1>
        <p className="text-sm text-muted-foreground">
          Contrate um plano, acompanhe sua assinatura e consulte cada pagamento e movimentação de créditos.
        </p>
      </div>

      {billingQuery.isLoading ? (
        <LoadingState />
      ) : billingQuery.isError ? (
        <ErrorState onRetry={() => billingQuery.refetch()} />
      ) : billingQuery.data ? (
        <BillingContent
          data={billingQuery.data}
          plans={plansQuery.data ?? []}
          plansLoading={plansQuery.isLoading}
          busy={busy}
          error={error}
          onCreditCheckout={(planId) => creditCheckout.mutate(planId)}
          onSubscriptionCheckout={(planId) => subscriptionCheckout.mutate(planId)}
          onSyncSubscription={() => syncSubscription.mutate()}
          onCancelSubscription={() => cancelSubscription.mutate()}
        />
      ) : null}
    </AppShell>
  );
}

function BillingContent({
  data,
  plans,
  plansLoading,
  busy,
  error,
  onCreditCheckout,
  onSubscriptionCheckout,
  onSyncSubscription,
  onCancelSubscription,
}: {
  data: ClientBillingResponse;
  plans: SubscriptionPlan[];
  plansLoading: boolean;
  busy: boolean;
  error: string | null;
  onCreditCheckout: (planId: number) => void;
  onSubscriptionCheckout: (planId: number) => void;
  onSyncSubscription: () => void;
  onCancelSubscription: () => void;
}) {
  return (
    <div className="space-y-4">
      {error && (
        <div className="rounded-md border border-destructive/30 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      )}
      <CurrentPlanCard data={data} />
      {!data.plan && <FirstContractNotice />}
      {data.plan === "AVULSO" && <CreditBalanceCard data={data} />}
      {data.plan === "PROFISSIONAL" && data.subscription && (
        <SubscriptionManagementCard
          data={data}
          busy={busy}
          onSync={onSyncSubscription}
          onCancel={onCancelSubscription}
        />
      )}
      {data.plan === "ENTERPRISE" && <EnterpriseCard data={data} />}
      <CheckoutCard
        data={data}
        plans={plans}
        loading={plansLoading}
        busy={busy}
        onCreditCheckout={onCreditCheckout}
        onSubscriptionCheckout={onSubscriptionCheckout}
      />
      <UsageSummaryCard data={data} />
      <PaymentHistoryCard events={data.events} />
      <CreditHistoryCard movements={data.creditMovements} />
    </div>
  );
}

function CurrentPlanCard({ data }: { data: ClientBillingResponse }) {
  return (
    <Card>
      <CardHeader icon={<BadgeCheck className="h-4 w-4" />} title="Situação atual" />
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <InfoRow label="Plano" value={data.plan ? planLabel(data.plan) : "Ainda não contratado"} />
        <InfoRow label="Situação" value={<EmpresaStatusBadge status={data.empresaStatus} />} />
        <InfoRow label="Situação financeira" value={<FinancialStatusBadge status={data.financialStatus} />} />
        {data.plan === "AVULSO" && (
          <InfoRow label="Créditos disponíveis" value={`${data.creditBalance} crédito${data.creditBalance !== 1 ? "s" : ""}`} />
        )}
        {data.plan === "PROFISSIONAL" && data.subscription?.currentPeriodEnd && (
          <InfoRow label="Próxima cobrança" value={formatDate(data.subscription.currentPeriodEnd)} />
        )}
      </div>
    </Card>
  );
}

function FirstContractNotice() {
  return (
    <section className="rounded-md border border-primary/30 bg-primary/5 p-5">
      <div className="flex gap-3">
        <Info className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
        <div className="space-y-1">
          <h2 className="text-sm font-semibold">Escolha como deseja contratar</h2>
          <p className="text-sm text-muted-foreground">
            Você pode comprar créditos para usar quando precisar ou contratar uma assinatura mensal. Após selecionar uma opção, o pagamento é concluído no Mercado Pago.
          </p>
        </div>
      </div>
    </section>
  );
}

function CreditBalanceCard({ data }: { data: ClientBillingResponse }) {
  const empty = data.creditBalance === 0;
  return (
    <Card className={empty ? "border-destructive/50 bg-destructive/5" : undefined}>
      <CardHeader icon={<CreditCard className="h-4 w-4" />} title="Saldo de créditos" />
      <div className="flex items-center gap-3">
        {empty ? (
          <AlertCircle className="h-5 w-5 shrink-0 text-destructive" />
        ) : (
          <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />
        )}
        <div>
          <p className="text-2xl font-bold">{data.creditBalance}</p>
          <p className="text-sm text-muted-foreground">
            {empty ? "Não há créditos disponíveis. Escolha um pacote abaixo para continuar usando a plataforma." : "créditos disponíveis para novas avaliações"}
          </p>
        </div>
      </div>
    </Card>
  );
}

function SubscriptionManagementCard({
  data,
  busy,
  onSync,
  onCancel,
}: {
  data: ClientBillingResponse;
  busy: boolean;
  onSync: () => void;
  onCancel: () => void;
}) {
  const [confirmingCancellation, setConfirmingCancellation] = useState(false);
  const subscription = data.subscription!;
  const canCancel = subscription.status !== "CANCELLED";

  return (
    <Card>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <CardHeader icon={<CalendarClock className="h-4 w-4" />} title="Assinatura mensal" />
        <button
          type="button"
          onClick={onSync}
          disabled={busy}
          className="inline-flex items-center justify-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent disabled:opacity-60"
        >
          {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
          Atualizar situação
        </button>
      </div>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <InfoRow label="Status da assinatura" value={subscriptionStatusLabel(subscription.status)} />
        {subscription.lastPaymentAt && <InfoRow label="Último pagamento" value={formatDate(subscription.lastPaymentAt)} />}
        {subscription.currentPeriodEnd && <InfoRow label="Vigência informada" value={formatDate(subscription.currentPeriodEnd)} />}
        {subscription.graceUntil && <InfoRow label="Carência até" value={<span className="font-medium text-amber-600">{formatDate(subscription.graceUntil)}</span>} />}
      </div>
      <div className="flex flex-wrap gap-2">
        {subscription.initPoint && (
          <a
            href={subscription.initPoint}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-accent"
          >
            <ExternalLink className="h-4 w-4" />
            Abrir no Mercado Pago
          </a>
        )}
        {canCancel && !confirmingCancellation && (
          <button
            type="button"
            onClick={() => setConfirmingCancellation(true)}
            disabled={busy}
            className="inline-flex items-center gap-2 rounded-md border border-destructive/40 bg-card px-4 py-2 text-sm font-medium text-destructive hover:bg-destructive/5 disabled:opacity-60"
          >
            <XCircle className="h-4 w-4" />
            Cancelar assinatura
          </button>
        )}
      </div>
      {confirmingCancellation && (
        <div className="space-y-3 rounded-md border border-destructive/30 bg-destructive/5 p-4">
          <p className="text-sm font-medium">Confirmar cancelamento da cobrança recorrente?</p>
          <p className="text-sm text-muted-foreground">
            A solicitação é enviada ao Mercado Pago e a situação da assinatura será atualizada na plataforma. Seus históricos de pagamentos e créditos continuam disponíveis.
          </p>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => {
                onCancel();
                setConfirmingCancellation(false);
              }}
              disabled={busy}
              className="inline-flex items-center gap-2 rounded-md bg-destructive px-4 py-2 text-sm font-medium text-destructive-foreground hover:bg-destructive/90 disabled:opacity-60"
            >
              {busy && <Loader2 className="h-4 w-4 animate-spin" />}
              Confirmar cancelamento
            </button>
            <button
              type="button"
              onClick={() => setConfirmingCancellation(false)}
              disabled={busy}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-accent disabled:opacity-60"
            >
              Manter assinatura
            </button>
          </div>
        </div>
      )}
    </Card>
  );
}

function EnterpriseCard({ data }: { data: ClientBillingResponse }) {
  return (
    <Card>
      <CardHeader icon={<Building2 className="h-4 w-4" />} title="Contrato Enterprise" />
      <p className="text-sm text-muted-foreground">
        Seu plano é acompanhado pela equipe comercial. Para alterações de contrato ou faturamento, fale com o suporte responsável pela sua empresa.
      </p>
      <InfoRow label="Situação financeira" value={<FinancialStatusBadge status={data.financialStatus} />} />
    </Card>
  );
}

function CheckoutCard({
  data,
  plans,
  loading,
  busy,
  onCreditCheckout,
  onSubscriptionCheckout,
}: {
  data: ClientBillingResponse;
  plans: SubscriptionPlan[];
  loading: boolean;
  busy: boolean;
  onCreditCheckout: (planId: number) => void;
  onSubscriptionCheckout: (planId: number) => void;
}) {
  if (data.plan === "ENTERPRISE") return null;

  const creditPlans = plans.filter((plan) => plan.planType === "AVULSO");
  const subscriptionPlans = plans.filter((plan) => plan.planType === "PROFISSIONAL");
  const hasActiveSubscription = data.subscription && data.subscription.status !== "CANCELLED";

  return (
    <Card id="contratacao">
      <CardHeader icon={<ShoppingCart className="h-4 w-4" />} title={data.plan ? "Comprar ou renovar" : "Contratar agora"} />
      {loading ? (
        <div className="flex items-center gap-2 py-3 text-sm text-muted-foreground">
          <Loader2 className="h-4 w-4 animate-spin" />
          Carregando planos disponíveis...
        </div>
      ) : !data.plan ? (
        <div className="space-y-6">
          <PlanOptions
            title="Créditos avulsos"
            description="Pague apenas pelos créditos que deseja usar. O saldo fica disponível após a confirmação do pagamento."
            plans={creditPlans}
            busy={busy}
            actionLabel="Comprar créditos"
            onSelect={onCreditCheckout}
          />
          <PlanOptions
            title="Assinatura mensal"
            description="Autorize uma cobrança recorrente no Mercado Pago para manter seu plano mensal ativo."
            plans={subscriptionPlans}
            busy={busy}
            actionLabel="Assinar plano"
            onSelect={onSubscriptionCheckout}
          />
        </div>
      ) : data.plan === "AVULSO" ? (
        <PlanOptions
          title="Adicionar créditos"
          description="Selecione o pacote de créditos que deseja comprar. O saldo só será liberado depois da confirmação financeira."
          plans={creditPlans}
          busy={busy}
          actionLabel="Comprar pacote"
          onSelect={onCreditCheckout}
        />
      ) : hasActiveSubscription ? (
        <p className="text-sm text-muted-foreground">
          Você já possui uma assinatura em andamento. Use os controles acima para atualizar a situação, abrir o Mercado Pago ou cancelar a renovação recorrente.
        </p>
      ) : (
        <PlanOptions
          title="Contratar nova assinatura"
          description="Sua assinatura anterior está cancelada ou não foi concluída. Escolha um plano para criar uma nova autorização de cobrança."
          plans={subscriptionPlans}
          busy={busy}
          actionLabel="Assinar plano"
          onSelect={onSubscriptionCheckout}
        />
      )}
    </Card>
  );
}

function PlanOptions({
  title,
  description,
  plans,
  busy,
  actionLabel,
  onSelect,
}: {
  title: string;
  description: string;
  plans: SubscriptionPlan[];
  busy: boolean;
  actionLabel: string;
  onSelect: (planId: number) => void;
}) {
  return (
    <section className="space-y-3">
      <div>
        <h3 className="text-sm font-semibold">{title}</h3>
        <p className="mt-1 text-sm text-muted-foreground">{description}</p>
      </div>
      {plans.length === 0 ? (
        <p className="rounded-md border border-border bg-muted/20 px-3 py-2 text-sm text-muted-foreground">Nenhuma opção está disponível no momento.</p>
      ) : (
        <div className="grid gap-3 lg:grid-cols-2">
          {plans.map((plan) => (
            <div key={plan.id} className="rounded-md border border-border bg-card p-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="font-semibold">{plan.name}</p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {plan.planType === "AVULSO" ? `${plan.creditAmount} crédito${plan.creditAmount !== 1 ? "s" : ""}` : "Cobrança mensal recorrente"}
                  </p>
                </div>
                <p className="text-lg font-bold">{formatCurrency(plan.priceCents, plan.currency)}</p>
              </div>
              <button
                type="button"
                onClick={() => onSelect(plan.id)}
                disabled={busy}
                className="mt-4 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
              >
                {busy && <Loader2 className="h-4 w-4 animate-spin" />}
                {actionLabel}
              </button>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

function UsageSummaryCard({ data }: { data: ClientBillingResponse }) {
  return (
    <Card>
      <CardHeader icon={<History className="h-4 w-4" />} title="Uso da plataforma" />
      <div className="grid gap-3 sm:grid-cols-3">
        <StatBox label="Avaliações nos últimos 7 dias" value={data.usage.completedLast7Days} />
        <StatBox label="Avaliações nos últimos 30 dias" value={data.usage.completedLast30Days} />
        <StatBox label="Avaliações concluídas no total" value={data.usage.completedAllTime} />
      </div>
    </Card>
  );
}

function PaymentHistoryCard({ events }: { events: BillingEvent[] }) {
  return (
    <Card id="pagamentos">
      <CardHeader icon={<History className="h-4 w-4" />} title="Histórico de pagamentos" />
      {events.length === 0 ? (
        <EmptyHistory text="Nenhuma cobrança ou pagamento foi registrado ainda." />
      ) : (
        <div className="-mx-6 overflow-x-auto px-6">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border text-left text-muted-foreground">
                <th className="pb-2 pr-4 font-medium">Data</th>
                <th className="pb-2 pr-4 font-medium">Movimentação</th>
                <th className="pb-2 pr-4 font-medium">Status</th>
                <th className="pb-2 text-right font-medium">Valor</th>
              </tr>
            </thead>
            <tbody>
              {events.map((event) => (
                <tr key={event.id} className="border-b border-border/50 last:border-0">
                  <td className="whitespace-nowrap py-3 pr-4 text-muted-foreground">{formatDateTime(event.createdAt)}</td>
                  <td className="py-3 pr-4">{eventTypeLabel(event.eventType)}</td>
                  <td className="py-3 pr-4"><PaymentStatus status={event.mpStatus} /></td>
                  <td className="py-3 text-right">{event.amountCents == null ? "—" : formatCurrency(event.amountCents, event.currency ?? "BRL")}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

function CreditHistoryCard({ movements }: { movements: CreditMovement[] }) {
  return (
    <Card id="creditos">
      <CardHeader icon={<CreditCard className="h-4 w-4" />} title="Extrato de créditos" />
      {movements.length === 0 ? (
        <EmptyHistory text="Nenhuma movimentação de crédito foi registrada ainda." />
      ) : (
        <div className="-mx-6 overflow-x-auto px-6">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border text-left text-muted-foreground">
                <th className="pb-2 pr-4 font-medium">Data</th>
                <th className="pb-2 pr-4 font-medium">Movimentação</th>
                <th className="pb-2 pr-4 text-right font-medium">Variação</th>
                <th className="pb-2 text-right font-medium">Saldo após</th>
              </tr>
            </thead>
            <tbody>
              {movements.map((movement) => (
                <tr key={movement.id} className="border-b border-border/50 last:border-0">
                  <td className="whitespace-nowrap py-3 pr-4 text-muted-foreground">{formatDateTime(movement.createdAt)}</td>
                  <td className="py-3 pr-4">
                    <p>{creditReasonLabel(movement.reason)}</p>
                    {movement.note && <p className="mt-0.5 text-xs text-muted-foreground">{movement.note}</p>}
                  </td>
                  <td className={cn("py-3 pr-4 text-right font-medium", movement.delta > 0 ? "text-emerald-700" : "text-destructive")}>
                    {movement.delta > 0 ? "+" : ""}{movement.delta}
                  </td>
                  <td className="py-3 text-right">{movement.balanceAfter}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

function PaymentStatus({ status }: { status: string | null }) {
  if (!status) return <span className="text-muted-foreground">—</span>;
  const labels: Record<string, string> = {
    approved: "Aprovado",
    authorized: "Autorizada",
    pending: "Pendente",
    rejected: "Recusado",
    cancelled: "Cancelado",
    refunded: "Estornado",
    charged_back: "Contestado",
    created: "Checkout criado",
    paused: "Pausada",
  };
  return <span className="capitalize text-muted-foreground">{labels[status] ?? status}</span>;
}

function EmptyHistory({ text }: { text: string }) {
  return <p className="rounded-md border border-dashed border-border px-4 py-5 text-sm text-muted-foreground">{text}</p>;
}

function EmpresaStatusBadge({ status }: { status: string }) {
  const map: Record<string, { label: string; className: string }> = {
    ATIVO: { label: "Ativo", className: "bg-emerald-100 text-emerald-700" },
    EM_TESTE: { label: "Em teste", className: "bg-blue-100 text-blue-700" },
    PENDENTE_PAGAMENTO: { label: "Pagamento pendente", className: "bg-amber-100 text-amber-700" },
    INADIMPLENTE: { label: "Inadimplente", className: "bg-red-100 text-red-700" },
    SEM_CREDITO: { label: "Sem créditos", className: "bg-orange-100 text-orange-700" },
    SUSPENSO: { label: "Suspenso", className: "bg-gray-100 text-gray-600" },
    CANCELADO: { label: "Cancelado", className: "bg-gray-100 text-gray-600" },
  };
  const config = map[status] ?? { label: status, className: "bg-gray-100 text-gray-600" };
  return <span className={cn("inline-flex rounded-full px-2 py-0.5 text-xs font-medium", config.className)}>{config.label}</span>;
}

function FinancialStatusBadge({ status }: { status: FinancialStatus }) {
  const map: Record<FinancialStatus, { label: string; className: string; icon: React.ReactNode }> = {
    REGULAR: { label: "Regular", className: "text-emerald-700", icon: <CheckCircle2 className="h-4 w-4 text-emerald-600" /> },
    PENDENTE_PAGAMENTO: { label: "Pagamento pendente", className: "text-amber-700", icon: <AlertTriangle className="h-4 w-4 text-amber-500" /> },
    INADIMPLENTE: { label: "Pagamento em atraso", className: "text-red-700", icon: <AlertCircle className="h-4 w-4 text-red-600" /> },
    SEM_CREDITO: { label: "Sem créditos", className: "text-orange-700", icon: <AlertCircle className="h-4 w-4 text-orange-500" /> },
    CANCELADO: { label: "Cancelado", className: "text-gray-500", icon: <XCircle className="h-4 w-4 text-gray-400" /> },
  };
  const config = map[status] ?? { label: status, className: "text-gray-600", icon: null };
  return <span className={cn("inline-flex items-center gap-1 text-sm font-medium", config.className)}>{config.icon}{config.label}</span>;
}

function Card({ children, className, id }: { children: React.ReactNode; className?: string; id?: string }) {
  return <section id={id} className={cn("space-y-4 rounded-md border border-border bg-card p-6", className)}>{children}</section>;
}

function CardHeader({ icon, title }: { icon: React.ReactNode; title: string }) {
  return <div className="flex items-center gap-2"><span className="text-muted-foreground">{icon}</span><h2 className="text-base font-semibold">{title}</h2></div>;
}

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return <div className="space-y-0.5"><p className="text-xs text-muted-foreground">{label}</p><div className="text-sm font-medium">{value}</div></div>;
}

function StatBox({ label, value }: { label: string; value: number }) {
  return <div className="rounded-md border border-border/60 bg-muted/30 p-4 text-center"><p className="text-2xl font-bold">{value}</p><p className="mt-1 text-xs text-muted-foreground">{label}</p></div>;
}

function LoadingState() {
  return <div className="flex justify-center gap-2 py-12 text-muted-foreground"><Loader2 className="h-5 w-5 animate-spin" /><span className="text-sm">Carregando suas informações financeiras...</span></div>;
}

function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <section className="space-y-3 rounded-md border border-destructive/40 bg-destructive/5 p-6 text-center">
      <AlertCircle className="mx-auto h-6 w-6 text-destructive" />
      <p className="text-sm font-medium">Não foi possível carregar as informações de cobrança.</p>
      <button type="button" onClick={onRetry} className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent">
        <RefreshCw className="h-4 w-4" />
        Tentar novamente
      </button>
    </section>
  );
}

function planLabel(plan: CommercialPlanType): string {
  if (plan === "AVULSO") return "Créditos avulsos";
  if (plan === "PROFISSIONAL") return "Assinatura mensal";
  return "Enterprise";
}

function subscriptionStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: "Aguardando autorização",
    AUTHORIZED: "Ativa",
    DELINQUENT: "Pagamento em atraso",
    PAUSED: "Pausada",
    CANCELLED: "Cancelada",
  };
  return map[status] ?? status;
}

function eventTypeLabel(type: string): string {
  const map: Record<string, string> = {
    CREDIT_CHECKOUT_CREATED: "Checkout de créditos iniciado",
    CREDIT_PURCHASE_APPROVED: "Compra de créditos aprovada",
    CREDIT_AUTO_RECHARGE_FAILED: "Recarga automática recusada",
    SUBSCRIPTION_CREATED: "Assinatura criada",
    SUBSCRIPTION_AUTHORIZED: "Assinatura autorizada",
    SUBSCRIPTION_PAYMENT_APPROVED: "Pagamento da assinatura aprovado",
    SUBSCRIPTION_PAYMENT_REJECTED: "Pagamento da assinatura recusado",
    SUBSCRIPTION_CANCELLED: "Assinatura cancelada",
    PAYMENT_PENDING: "Pagamento pendente",
    PAYMENT_REFUNDED: "Pagamento estornado",
    PAYMENT_CHARGEBACK: "Contestação de pagamento",
  };
  return map[type] ?? type;
}

function creditReasonLabel(reason: string): string {
  const map: Record<string, string> = {
    PURCHASE: "Créditos adquiridos",
    CONSUMPTION: "Crédito utilizado em avaliação",
    ADJUSTMENT: "Ajuste de créditos",
  };
  return map[reason] ?? reason;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(value));
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatCurrency(cents: number, currency: string): string {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency }).format(cents / 100);
}

function openCheckout(initPoint: string | null) {
  if (initPoint) {
    window.open(initPoint, "_blank", "noopener,noreferrer");
  }
}

function messageFromError(error: unknown): string | null {
  if (!error) return null;
  if (error instanceof Error) return error.message;
  return "Não foi possível concluir esta ação de cobrança.";
}
