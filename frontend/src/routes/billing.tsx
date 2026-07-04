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
  HeadphonesIcon,
  History,
  Info,
  Loader2,
  RefreshCw,
  ShoppingCart,
  XCircle,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import {
  type BillingAction,
  type BillingEvent,
  type ClientBillingResponse,
  type CommercialPlanType,
  type FinancialStatus,
  type SubscriptionPlan,
  createClientCreditCheckout,
  createClientSubscriptionCheckout,
  getClientBilling,
  listClientBillingPlans,
  syncClientSubscription,
} from "@/lib/api/client-billing";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/billing")({
  head: () => ({
    meta: [{ title: "Plano e uso - Práxis" }],
  }),
  component: BillingPage,
});

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

function BillingPage() {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: ["billing"],
    queryFn: getClientBilling,
    retry: false,
  });
  const plansQuery = useQuery({
    queryKey: ["billing", "plans"],
    queryFn: listClientBillingPlans,
    retry: false,
  });
  const creditCheckout = useMutation({
    mutationFn: createClientCreditCheckout,
    onSuccess: (checkout) => {
      openCheckout(checkout.initPoint);
      void queryClient.invalidateQueries({ queryKey: ["billing"] });
    },
  });
  const subscriptionCheckout = useMutation({
    mutationFn: createClientSubscriptionCheckout,
    onSuccess: (checkout) => {
      openCheckout(checkout.initPoint);
      void queryClient.invalidateQueries({ queryKey: ["billing"] });
    },
  });
  const syncSubscription = useMutation({
    mutationFn: syncClientSubscription,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["billing"] });
    },
  });

  return (
    <AppShell>
      <div className="space-y-2 mb-6">
        <h1 className="text-2xl font-semibold">Plano e uso</h1>
        <p className="text-sm text-muted-foreground">
          Acompanhe o plano da sua empresa e resolva créditos ou assinatura sem acionar o suporte.
        </p>
      </div>

      {query.isLoading ? (
        <LoadingState />
      ) : query.isError ? (
        <ErrorState onRetry={() => query.refetch()} />
      ) : query.data ? (
        <BillingContent
          data={query.data}
          plans={plansQuery.data ?? []}
          plansLoading={plansQuery.isLoading}
          checkoutPending={creditCheckout.isPending || subscriptionCheckout.isPending}
          syncPending={syncSubscription.isPending}
          checkoutError={messageFromError(creditCheckout.error ?? subscriptionCheckout.error ?? syncSubscription.error)}
          onCreditCheckout={(planId) => creditCheckout.mutate(planId)}
          onSubscriptionCheckout={(planId) => subscriptionCheckout.mutate(planId)}
          onSyncSubscription={() => syncSubscription.mutate()}
        />
      ) : null}
    </AppShell>
  );
}

// ---------------------------------------------------------------------------
// Content
// ---------------------------------------------------------------------------

function BillingContent({
  data,
  plans,
  plansLoading,
  checkoutPending,
  syncPending,
  checkoutError,
  onCreditCheckout,
  onSubscriptionCheckout,
  onSyncSubscription,
}: {
  data: ClientBillingResponse;
  plans: SubscriptionPlan[];
  plansLoading: boolean;
  checkoutPending: boolean;
  syncPending: boolean;
  checkoutError: string | null;
  onCreditCheckout: (planId: number) => void;
  onSubscriptionCheckout: (planId: number) => void;
  onSyncSubscription: () => void;
}) {
  const hasNoSetup =
    data.plan == null &&
    data.subscription == null &&
    data.creditBalance === 0 &&
    data.events.length === 0;

  if (hasNoSetup) {
    return <EmptySetupState />;
  }

  return (
    <div className="space-y-4">
      <CurrentPlanCard data={data} />
      <UsageSummaryCard data={data} />
      {data.plan === "AVULSO" && <CreditBalanceCard data={data} />}
      {data.plan === "PROFISSIONAL" && data.subscription && (
        <SubscriptionCard data={data} onSync={onSyncSubscription} syncPending={syncPending} />
      )}
      {data.plan === "ENTERPRISE" && <EnterpriseCard data={data} />}
      <CheckoutSelfServiceCard
        data={data}
        plans={plans}
        loading={plansLoading}
        pending={checkoutPending}
        error={checkoutError}
        onCreditCheckout={onCreditCheckout}
        onSubscriptionCheckout={onSubscriptionCheckout}
      />
      <BillingActionPanel actions={data.availableActions} subscriptionUrl={data.subscription?.initPoint ?? null} />
      {data.events.length > 0 && <BillingEventsTable events={data.events} />}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Cards
// ---------------------------------------------------------------------------

function CurrentPlanCard({ data }: { data: ClientBillingResponse }) {
  return (
    <Card>
      <CardHeader icon={<BadgeCheck className="h-4 w-4" />} title="Plano atual" />
      <div className="grid gap-3 sm:grid-cols-2">
        <InfoRow label="Plano" value={planLabel(data.plan)} />
        <InfoRow label="Status" value={<EmpresaStatusBadge status={data.empresaStatus} />} />
        {data.plan === "PROFISSIONAL" && data.subscription?.currentPeriodEnd && (
          <InfoRow label="Próxima renovação" value={formatDate(data.subscription.currentPeriodEnd)} />
        )}
        {data.plan === "AVULSO" && (
          <InfoRow label="Saldo de créditos" value={`${data.creditBalance} crédito${data.creditBalance !== 1 ? "s" : ""}`} />
        )}
      </div>
    </Card>
  );
}

function UsageSummaryCard({ data }: { data: ClientBillingResponse }) {
  return (
    <Card>
      <CardHeader icon={<History className="h-4 w-4" />} title="Uso" />
      <div className="grid gap-3 sm:grid-cols-3">
        <StatBox label="Últimos 7 dias" value={data.usage.completedLast7Days} />
        <StatBox label="Últimos 30 dias" value={data.usage.completedLast30Days} />
        <StatBox label="Total histórico" value={data.usage.completedAllTime} />
      </div>
    </Card>
  );
}

function CreditBalanceCard({ data }: { data: ClientBillingResponse }) {
  const low = data.creditBalance === 0;
  return (
    <Card className={low ? "border-destructive/50 bg-destructive/5" : undefined}>
      <CardHeader icon={<CreditCard className="h-4 w-4" />} title="Créditos" />
      <div className="flex items-center gap-3">
        {low ? (
          <AlertCircle className="h-5 w-5 text-destructive shrink-0" />
        ) : (
          <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
        )}
        <div>
          <p className="text-2xl font-bold">{data.creditBalance}</p>
          <p className="text-sm text-muted-foreground">
            {low
              ? "Sem créditos disponíveis. Compre mais para continuar avaliando."
              : `crédito${data.creditBalance !== 1 ? "s" : ""} disponíve${data.creditBalance !== 1 ? "is" : "l"}`}
          </p>
        </div>
      </div>
    </Card>
  );
}

function SubscriptionCard({
  data,
  onSync,
  syncPending,
}: {
  data: ClientBillingResponse;
  onSync: () => void;
  syncPending: boolean;
}) {
  const sub = data.subscription!;
  return (
    <Card>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <CardHeader icon={<CalendarClock className="h-4 w-4" />} title="Cobrança" />
        <button
          type="button"
          onClick={onSync}
          disabled={syncPending}
          className="inline-flex items-center justify-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent disabled:opacity-60"
        >
          {syncPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
          Sincronizar assinatura
        </button>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <InfoRow label="Status financeiro" value={<FinancialStatusBadge status={data.financialStatus} />} />
        <InfoRow label="Status da assinatura" value={subscriptionStatusLabel(sub.status)} />
        {sub.lastPaymentAt && <InfoRow label="Último pagamento" value={formatDate(sub.lastPaymentAt)} />}
        {sub.currentPeriodEnd && <InfoRow label="Próxima cobrança" value={formatDate(sub.currentPeriodEnd)} />}
        {sub.graceUntil && (
          <InfoRow label="Carência até" value={<span className="text-amber-600 font-medium">{formatDate(sub.graceUntil)}</span>} />
        )}
      </div>
    </Card>
  );
}

function EnterpriseCard({ data }: { data: ClientBillingResponse }) {
  return (
    <Card>
      <CardHeader icon={<Building2 className="h-4 w-4" />} title="Contrato Enterprise" />
      <p className="text-sm text-muted-foreground">
        Seu plano é gerenciado comercialmente pela equipe Práxis. Entre em contato com o suporte para dúvidas sobre condições ou faturamento.
      </p>
      <InfoRow label="Status" value={<FinancialStatusBadge status={data.financialStatus} />} />
    </Card>
  );
}

function CheckoutSelfServiceCard({
  data,
  plans,
  loading,
  pending,
  error,
  onCreditCheckout,
  onSubscriptionCheckout,
}: {
  data: ClientBillingResponse;
  plans: SubscriptionPlan[];
  loading: boolean;
  pending: boolean;
  error: string | null;
  onCreditCheckout: (planId: number) => void;
  onSubscriptionCheckout: (planId: number) => void;
}) {
  const creditPlans = plans.filter((plan) => plan.planType === "AVULSO");
  const subscriptionPlans = plans.filter((plan) => plan.planType === "PROFISSIONAL");

  if (data.plan === "ENTERPRISE") return null;

  return (
    <Card id="checkout">
      <CardHeader icon={<ShoppingCart className="h-4 w-4" />} title="Self-service de cobrança" />
      <p className="text-sm text-muted-foreground">
        Gere o checkout no Mercado Pago pela própria conta. Créditos e regularização só entram após confirmação financeira.
      </p>
      {error && (
        <div className="rounded-md border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive">
          {error}
        </div>
      )}
      {loading ? (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="h-4 w-4 animate-spin" />
          Carregando opções de cobrança...
        </div>
      ) : data.plan === "AVULSO" ? (
        <PlanButtons plans={creditPlans} pending={pending} emptyText="Nenhum pacote de créditos ativo." onSelect={onCreditCheckout} />
      ) : (
        <div className="space-y-3">
          {data.subscription?.initPoint && (
            <a
              href={data.subscription.initPoint}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-2 rounded-md border border-primary bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              <ExternalLink className="h-4 w-4" />
              Abrir autorização atual
            </a>
          )}
          <PlanButtons plans={subscriptionPlans} pending={pending} emptyText="Nenhum plano profissional ativo." onSelect={onSubscriptionCheckout} />
        </div>
      )}
    </Card>
  );
}

function PlanButtons({
  plans,
  pending,
  emptyText,
  onSelect,
}: {
  plans: SubscriptionPlan[];
  pending: boolean;
  emptyText: string;
  onSelect: (planId: number) => void;
}) {
  if (plans.length === 0) {
    return <p className="text-sm text-muted-foreground">{emptyText}</p>;
  }

  return (
    <div className="grid gap-2 sm:grid-cols-2">
      {plans.map((plan) => (
        <button
          key={plan.id}
          type="button"
          onClick={() => onSelect(plan.id)}
          disabled={pending}
          className="rounded-md border border-border bg-card p-4 text-left hover:bg-accent disabled:opacity-60"
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm font-semibold">{plan.name}</p>
              {plan.creditAmount != null && <p className="text-xs text-muted-foreground">{plan.creditAmount} créditos</p>}
            </div>
            <span className="text-sm font-semibold whitespace-nowrap">{formatCurrency(plan.priceCents, plan.currency)}</span>
          </div>
        </button>
      ))}
    </div>
  );
}

function BillingActionPanel({ actions, subscriptionUrl }: { actions: BillingAction[]; subscriptionUrl: string | null }) {
  if (actions.length === 0) return null;

  return (
    <Card>
      <CardHeader icon={<Info className="h-4 w-4" />} title="Ações disponíveis" />
      <div className="flex flex-wrap gap-2">
        {actions.map((action) => (
          <ActionButton key={action} action={action} subscriptionUrl={subscriptionUrl} />
        ))}
      </div>
    </Card>
  );
}

function ActionButton({ action, subscriptionUrl }: { action: BillingAction; subscriptionUrl: string | null }) {
  const config: Record<BillingAction, { label: string; icon: React.ReactNode; href?: string; variant?: "primary" | "default" }> = {
    BUY_CREDITS: { label: "Comprar créditos", icon: <ShoppingCart className="h-4 w-4" />, href: "#checkout", variant: "primary" },
    MANAGE_AUTO_RECHARGE: { label: "Recarga automática", icon: <RefreshCw className="h-4 w-4" />, href: "#checkout" },
    VIEW_HISTORY: { label: "Ver histórico", icon: <History className="h-4 w-4" />, href: "#historico" },
    VIEW_SUBSCRIPTION: { label: "Ver assinatura", icon: <ExternalLink className="h-4 w-4" />, href: subscriptionUrl ?? "#checkout" },
    SYNC_SUBSCRIPTION: { label: "Sincronizar assinatura", icon: <RefreshCw className="h-4 w-4" />, href: "#checkout" },
    UPDATE_PAYMENT: { label: "Atualizar pagamento", icon: <CreditCard className="h-4 w-4" />, href: subscriptionUrl ?? "#checkout", variant: "primary" },
    CONTACT_SUPPORT: { label: "Falar com suporte", icon: <HeadphonesIcon className="h-4 w-4" />, href: "mailto:suporte@praxis.com.br" },
  };

  const { label, icon, href, variant } = config[action];
  const base = "inline-flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium transition-colors";
  const cls = variant === "primary"
    ? cn(base, "border-primary bg-primary text-primary-foreground hover:bg-primary/90")
    : cn(base, "border-border bg-card hover:bg-accent");

  return (
    <a href={href} target={href?.startsWith("http") ? "_blank" : undefined} rel={href?.startsWith("http") ? "noreferrer" : undefined} className={cls}>
      {icon}
      {label}
    </a>
  );
}

function BillingEventsTable({ events }: { events: BillingEvent[] }) {
  return (
    <Card id="historico">
      <CardHeader icon={<History className="h-4 w-4" />} title="Histórico financeiro" />
      <div className="overflow-x-auto -mx-6 px-6">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border text-left text-muted-foreground">
              <th className="pb-2 pr-4 font-medium">Data</th>
              <th className="pb-2 pr-4 font-medium">Evento</th>
              <th className="pb-2 pr-4 font-medium">Status</th>
              <th className="pb-2 font-medium text-right">Valor</th>
            </tr>
          </thead>
          <tbody>
            {events.map((event) => (
              <tr key={event.id} className="border-b border-border/50 last:border-0">
                <td className="py-2 pr-4 text-muted-foreground whitespace-nowrap">{formatDate(event.createdAt)}</td>
                <td className="py-2 pr-4">{eventTypeLabel(event.eventType)}</td>
                <td className="py-2 pr-4">
                  {event.mpStatus ? <span className="text-muted-foreground capitalize">{event.mpStatus}</span> : <span className="text-muted-foreground">—</span>}
                </td>
                <td className="py-2 text-right">
                  {event.amountCents != null ? formatCurrency(event.amountCents, event.currency ?? "BRL") : <span className="text-muted-foreground">—</span>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

// ---------------------------------------------------------------------------
// Badges and primitives
// ---------------------------------------------------------------------------

function EmpresaStatusBadge({ status }: { status: string }) {
  const map: Record<string, { label: string; cls: string }> = {
    ATIVO: { label: "Ativo", cls: "bg-emerald-100 text-emerald-700" },
    EM_TESTE: { label: "Em teste", cls: "bg-blue-100 text-blue-700" },
    PENDENTE_PAGAMENTO: { label: "Pagamento pendente", cls: "bg-amber-100 text-amber-700" },
    INADIMPLENTE: { label: "Inadimplente", cls: "bg-red-100 text-red-700" },
    SEM_CREDITO: { label: "Sem créditos", cls: "bg-orange-100 text-orange-700" },
    SUSPENSO: { label: "Suspenso", cls: "bg-gray-100 text-gray-600" },
    CANCELADO: { label: "Cancelado", cls: "bg-gray-100 text-gray-600" },
  };
  const cfg = map[status] ?? { label: status, cls: "bg-gray-100 text-gray-600" };
  return <span className={cn("inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium", cfg.cls)}>{cfg.label}</span>;
}

function FinancialStatusBadge({ status }: { status: FinancialStatus }) {
  const map: Record<FinancialStatus, { label: string; cls: string; icon: React.ReactNode }> = {
    REGULAR: { label: "Regular", cls: "text-emerald-700", icon: <CheckCircle2 className="h-4 w-4 text-emerald-600" /> },
    PENDENTE_PAGAMENTO: { label: "Pagamento pendente", cls: "text-amber-700", icon: <AlertTriangle className="h-4 w-4 text-amber-500" /> },
    INADIMPLENTE: { label: "Pagamento em atraso", cls: "text-red-700", icon: <AlertCircle className="h-4 w-4 text-red-600" /> },
    SEM_CREDITO: { label: "Sem créditos", cls: "text-orange-700", icon: <AlertCircle className="h-4 w-4 text-orange-500" /> },
    CANCELADO: { label: "Cancelado", cls: "text-gray-500", icon: <XCircle className="h-4 w-4 text-gray-400" /> },
  };
  const cfg = map[status] ?? { label: status, cls: "text-gray-600", icon: null };
  return <span className={cn("inline-flex items-center gap-1 text-sm font-medium", cfg.cls)}>{cfg.icon}{cfg.label}</span>;
}

function Card({ children, className, id }: { children: React.ReactNode; className?: string; id?: string }) {
  return <section id={id} className={cn("rounded-md border border-border bg-card p-6 space-y-4", className)}>{children}</section>;
}

function CardHeader({ icon, title }: { icon: React.ReactNode; title: string }) {
  return <div className="flex items-center gap-2"><span className="text-muted-foreground">{icon}</span><h2 className="text-base font-semibold">{title}</h2></div>;
}

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return <div className="space-y-0.5"><p className="text-xs text-muted-foreground">{label}</p><div className="text-sm font-medium">{value}</div></div>;
}

function StatBox({ label, value }: { label: string; value: number }) {
  return <div className="rounded-md border border-border/60 bg-muted/30 p-4 text-center"><p className="text-2xl font-bold">{value}</p><p className="text-xs text-muted-foreground mt-1">{label}</p></div>;
}

// ---------------------------------------------------------------------------
// States and helpers
// ---------------------------------------------------------------------------

function LoadingState() {
  return <div className="flex items-center gap-2 text-muted-foreground py-12 justify-center"><Loader2 className="h-5 w-5 animate-spin" /><span className="text-sm">Carregando plano e uso...</span></div>;
}

function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <section className="rounded-md border border-destructive/40 bg-destructive/5 p-6 space-y-3 text-center">
      <AlertCircle className="h-6 w-6 text-destructive mx-auto" />
      <p className="text-sm font-medium">Não foi possível carregar as informações do plano.</p>
      <button type="button" onClick={onRetry} className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent">
        <RefreshCw className="h-4 w-4" />
        Tentar novamente
      </button>
    </section>
  );
}

function EmptySetupState() {
  return (
    <section className="rounded-md border border-border bg-card p-8 text-center space-y-2">
      <CreditCard className="h-8 w-8 text-muted-foreground mx-auto" />
      <p className="text-sm font-medium">Seu plano ainda não possui cobrança configurada.</p>
      <p className="text-sm text-muted-foreground">Entre em contato com o suporte para ativar seu plano.</p>
    </section>
  );
}

function planLabel(plan: CommercialPlanType | null): string {
  if (plan === "AVULSO") return "Avulso";
  if (plan === "PROFISSIONAL") return "Profissional";
  if (plan === "ENTERPRISE") return "Enterprise";
  return "Não informado";
}

function subscriptionStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: "Pendente",
    AUTHORIZED: "Autorizada",
    DELINQUENT: "Inadimplente",
    PAUSED: "Pausada",
    CANCELLED: "Cancelada",
  };
  return map[status] ?? status;
}

function eventTypeLabel(type: string): string {
  const map: Record<string, string> = {
    CREDIT_CHECKOUT_CREATED: "Compra de créditos criada",
    CREDIT_PURCHASE_APPROVED: "Compra aprovada",
    CREDIT_AUTO_RECHARGE_FAILED: "Recarga automática recusada",
    SUBSCRIPTION_CREATED: "Assinatura criada",
    SUBSCRIPTION_AUTHORIZED: "Assinatura autorizada",
    SUBSCRIPTION_PAYMENT_APPROVED: "Pagamento aprovado",
    SUBSCRIPTION_PAYMENT_REJECTED: "Pagamento recusado",
    SUBSCRIPTION_CANCELLED: "Assinatura cancelada",
    PAYMENT_PENDING: "Pagamento pendente",
    PAYMENT_REFUNDED: "Pagamento estornado",
    PAYMENT_CHARGEBACK: "Contestação de pagamento",
  };
  return map[type] ?? type;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(value));
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
  return "Não foi possível criar ou sincronizar a cobrança.";
}
