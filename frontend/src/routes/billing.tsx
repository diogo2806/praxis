import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, AlertTriangle, BadgeCheck, CalendarClock, CheckCircle2, CreditCard, Loader2, RefreshCw, TrendingUp, XCircle } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { BillingHistory } from "@/components/billing-history";
import { BillingPlanActions } from "@/components/billing-plan-actions";
import {
  type AutoRechargeConfigResponse,
  type ClientBillingResponse,
  type CommercialPlanType,
  type FinancialStatus,
  cancelClientSubscription,
  changeClientPlan,
  createClientCreditCheckout,
  createEnterprisePlanRequest,
  getAutoRechargeConfig,
  getClientBilling,
  getClientPlanManagement,
  listClientBillingPlans,
  syncClientSubscription,
} from "@/lib/api/client-billing";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/billing")({
  head: () => ({ meta: [{ title: "Planos, pagamentos e créditos - Práxis" }] }),
  component: BillingPage,
});

function BillingPage() {
  const client = useQueryClient();
  const billing = useQuery({ queryKey: ["billing"], queryFn: getClientBilling, retry: false });
  const plans = useQuery({ queryKey: ["billing", "plans"], queryFn: listClientBillingPlans, retry: false });
  const management = useQuery({ queryKey: ["billing", "management"], queryFn: getClientPlanManagement, retry: false });
  const autoRecharge = useQuery({
    queryKey: ["billing", "auto-recharge"],
    queryFn: getAutoRechargeConfig,
    enabled: billing.data?.plan === "AVULSO",
    retry: false,
  });
  const refresh = () => {
    void client.invalidateQueries({ queryKey: ["billing"] });
  };
  const checkout = (result: { initPoint: string | null }) => { if (result.initPoint) window.open(result.initPoint, "_blank", "noopener,noreferrer"); refresh(); };
  const credit = useMutation({ mutationFn: createClientCreditCheckout, onSuccess: checkout });
  const change = useMutation({ mutationFn: changeClientPlan, onSuccess: checkout });
  const sync = useMutation({ mutationFn: syncClientSubscription, onSuccess: refresh });
  const cancel = useMutation({ mutationFn: cancelClientSubscription, onSuccess: refresh });
  const enterprise = useMutation({ mutationFn: ({ type, plan, note }: { type: "CHANGE_PLAN" | "CANCEL_CONTRACT"; plan?: CommercialPlanType; note?: string }) => createEnterprisePlanRequest(type, plan, note), onSuccess: refresh });
  const busy = credit.isPending || change.isPending || sync.isPending || cancel.isPending || enterprise.isPending;
  const error = errorMessage(credit.error ?? change.error ?? sync.error ?? cancel.error ?? enterprise.error);

  return <AppShell><main className="space-y-4">
    <header className="mb-6 space-y-2"><h1 className="text-2xl font-semibold">Planos, pagamentos e créditos</h1><p className="text-sm text-muted-foreground">Contrate, altere ou cancele sua cobrança e acompanhe pagamentos e créditos em um só lugar.</p></header>
    {billing.isLoading ? <Loading /> : billing.isError || !billing.data ? <Failure retry={() => billing.refetch()} /> : <BillingView data={billing.data} plans={plans.data ?? []} management={management.data} autoRecharge={autoRecharge.data} autoRechargeLoading={autoRecharge.isLoading} autoRechargeError={autoRecharge.isError} plansLoading={plans.isLoading || management.isLoading} busy={busy} error={error} onCredit={(id) => credit.mutate(id)} onChange={(id) => change.mutate(id)} onSync={() => sync.mutate()} onCancel={() => cancel.mutate()} onEnterpriseRequest={(type, plan, note) => enterprise.mutate({ type, plan, note })} />}
  </main></AppShell>;
}

function BillingView({ data, plans, management, autoRecharge, autoRechargeLoading, autoRechargeError, plansLoading, busy, error, onCredit, onChange, onSync, onCancel, onEnterpriseRequest }: {
  data: ClientBillingResponse;
  plans: ReturnType<typeof listClientBillingPlans> extends Promise<infer T> ? T : never;
  management: ReturnType<typeof getClientPlanManagement> extends Promise<infer T> ? T | undefined : never;
  autoRecharge: AutoRechargeConfigResponse | undefined;
  autoRechargeLoading: boolean;
  autoRechargeError: boolean;
  plansLoading: boolean;
  busy: boolean;
  error: string | null;
  onCredit: (id: number) => void;
  onChange: (id: number) => void;
  onSync: () => void;
  onCancel: () => void;
  onEnterpriseRequest: (type: "CHANGE_PLAN" | "CANCEL_CONTRACT", plan?: CommercialPlanType, note?: string) => void;
}) {
  const recoveryPlanId = autoRecharge?.planId ?? plans.find((plan) => plan.planType === "AVULSO")?.id ?? null;
  const recoverNow = recoveryPlanId == null ? undefined : () => onCredit(recoveryPlanId);

  return <div className="space-y-4">
    {error && <div className="rounded-md border border-destructive/30 bg-destructive/5 px-4 py-3 text-sm text-destructive">{error}</div>}
    {autoRecharge && autoRechargeNeedsAttention(autoRecharge) && <AutoRechargeFailureAlert config={autoRecharge} busy={busy} onRecover={recoverNow} />}
    <StatusCard data={data} />
    {data.plan === "AVULSO" && <CreditBalance data={data} />}
    {data.plan === "AVULSO" && <AutoRechargeCard config={autoRecharge} loading={autoRechargeLoading} failed={autoRechargeError} plans={plans} busy={busy} onRecover={recoverNow} />}
    {data.plan === "PROFISSIONAL" && data.subscription && <SubscriptionCard data={data} busy={busy} onSync={onSync} onCancel={onCancel} />}
    {plansLoading || !management ? <section className="rounded-md border border-border bg-card p-6 text-sm text-muted-foreground"><Loader2 className="mr-2 inline h-4 w-4 animate-spin" />Carregando opções de plano...</section> : <BillingPlanActions management={management} plans={plans} busy={busy} onBuyCredits={onCredit} onChangePlan={onChange} onEnterpriseRequest={onEnterpriseRequest} />}
    <Usage data={data} />
    <BillingHistory events={data.events} movements={data.creditMovements} />
  </div>;
}

function AutoRechargeFailureAlert({ config, busy, onRecover }: { config: AutoRechargeConfigResponse; busy: boolean; onRecover?: () => void }) {
  return <section className="sticky top-4 z-20 rounded-md border border-destructive/40 bg-background p-4 shadow-lg" role="alert">
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex gap-3"><AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-destructive" /><div><p className="font-semibold text-destructive">A recarga automática precisa de atenção</p><p className="text-sm text-muted-foreground">{config.lastOutcome ?? "A última tentativa não recompôs o saldo."} Faça uma recarga manual para retomar as operações imediatamente.</p></div></div>
      <div className="flex shrink-0 gap-2"><button type="button" onClick={() => document.getElementById("auto-recharge")?.scrollIntoView({ behavior: "smooth", block: "center" })} className="rounded-md border border-border bg-card px-4 py-2 text-sm font-medium">Ver detalhes</button>{onRecover && <button type="button" disabled={busy} onClick={onRecover} className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-60">{busy ? "Abrindo..." : "Recarregar agora"}</button>}</div>
    </div>
  </section>;
}

function AutoRechargeCard({ config, loading, failed, plans, busy, onRecover }: { config: AutoRechargeConfigResponse | undefined; loading: boolean; failed: boolean; plans: ReturnType<typeof listClientBillingPlans> extends Promise<infer T> ? T : never; busy: boolean; onRecover?: () => void }) {
  if (loading) return <section id="auto-recharge" className="rounded-md border border-border bg-card p-6 text-sm text-muted-foreground"><Loader2 className="mr-2 inline h-4 w-4 animate-spin" />Carregando recarga automática...</section>;
  if (failed || !config) return <section id="auto-recharge" className="rounded-md border border-destructive/30 bg-destructive/5 p-6 text-sm text-destructive">Não foi possível consultar a recarga automática.</section>;
  const selectedPlan = plans.find((plan) => plan.id === config.planId);
  const attention = autoRechargeNeedsAttention(config);
  return <Card id="auto-recharge" title="Recarga automática" icon={<RefreshCw className="h-4 w-4" />}>
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4"><Info label="Situação" value={config.enabled ? config.status === "PENDING" ? "Cobrança em andamento" : attention ? "Ação necessária" : "Ativa" : "Desativada"} /><Info label="Saldo de disparo" value={`${config.thresholdCredits} crédito${config.thresholdCredits !== 1 ? "s" : ""}`} /><Info label="Pacote" value={selectedPlan?.name ?? (config.planId ? `Plano ${config.planId}` : "Não configurado")} /><Info label="Forma de pagamento" value={config.cardConfigured ? "Cartão configurado" : "Não configurada"} /></div>
    {config.lastOutcome && <div className={cn("rounded-md border px-4 py-3 text-sm", attention ? "border-destructive/30 bg-destructive/5 text-destructive" : "border-border bg-muted/30 text-muted-foreground")}><p className="font-medium">Último resultado</p><p>{config.lastOutcome}</p>{config.lastTriggeredAt && <p className="mt-1 text-xs">Tentativa em {dateTime(config.lastTriggeredAt)}</p>}</div>}
    {attention && onRecover && <div className="flex flex-wrap items-center justify-between gap-3 rounded-md border border-border bg-muted/20 p-4"><p className="text-sm text-muted-foreground">Use outro meio de pagamento no checkout para recompor o saldo sem aguardar suporte.</p><button type="button" disabled={busy} onClick={onRecover} className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-60">{busy ? "Abrindo checkout..." : "Recarregar agora"}</button></div>}
  </Card>;
}

function StatusCard({ data }: { data: ClientBillingResponse }) { return <Card title="Situação atual" icon={<BadgeCheck className="h-4 w-4" />}><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4"><Info label="Plano" value={planName(data.plan)} /><Info label="Situação" value={<Badge text={empresaStatus(data.empresaStatus)} tone={data.empresaStatus === "ATIVO" ? "success" : "neutral"} />} /><Info label="Situação financeira" value={<Financial status={data.financialStatus} />} />{data.plan === "AVULSO" && <Info label="Créditos disponíveis" value={`${data.creditBalance} crédito${data.creditBalance !== 1 ? "s" : ""}`} />}</div></Card>; }
function CreditBalance({ data }: { data: ClientBillingResponse }) { const empty = data.creditBalance === 0; return <Card title="Saldo de créditos" icon={<CreditCard className="h-4 w-4" />}><div className="flex items-center gap-3">{empty ? <AlertCircle className="h-5 w-5 text-destructive" /> : <CheckCircle2 className="h-5 w-5 text-emerald-600" />}<div><p className="text-2xl font-bold">{data.creditBalance}</p><p className="text-sm text-muted-foreground">{empty ? "Não há créditos disponíveis. Escolha um pacote em Gerenciar plano." : "créditos disponíveis para novas avaliações"}</p></div></div></Card>; }

function SubscriptionCard({ data, busy, onSync, onCancel }: { data: ClientBillingResponse; busy: boolean; onSync: () => void; onCancel: () => void; }) {
  const [confirm, setConfirm] = useState(false); const sub = data.subscription!;
  return <Card title="Assinatura mensal" icon={<CalendarClock className="h-4 w-4" />}><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3"><Info label="Status" value={subscriptionStatus(sub.status)} />{sub.lastPaymentAt && <Info label="Último pagamento" value={date(sub.lastPaymentAt)} />}{sub.currentPeriodEnd && <Info label="Próxima cobrança informada" value={date(sub.currentPeriodEnd)} />}</div><div className="flex flex-wrap gap-2"><button type="button" disabled={busy} onClick={onSync} className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-accent disabled:opacity-60"><RefreshCw className="h-4 w-4" />Atualizar situação</button>{sub.status !== "CANCELLED" && <button type="button" disabled={busy} onClick={() => setConfirm(true)} className="inline-flex items-center gap-2 rounded-md border border-destructive/40 bg-card px-4 py-2 text-sm font-medium text-destructive hover:bg-destructive/5 disabled:opacity-60"><XCircle className="h-4 w-4" />Cancelar cobrança recorrente</button>}</div>{confirm && <div className="space-y-3 rounded-md border border-destructive/30 bg-destructive/5 p-4"><p className="text-sm font-semibold">Confirmar cancelamento da cobrança recorrente?</p><p className="text-sm text-muted-foreground">A assinatura será cancelada no Mercado Pago. Seus dados, pagamentos e históricos permanecem preservados.</p><div className="flex gap-2"><button type="button" disabled={busy} onClick={() => { onCancel(); setConfirm(false); }} className="rounded-md bg-destructive px-4 py-2 text-sm font-medium text-destructive-foreground disabled:opacity-60">{busy ? "Cancelando..." : "Confirmar cancelamento"}</button><button type="button" disabled={busy} onClick={() => setConfirm(false)} className="rounded-md border border-border bg-card px-4 py-2 text-sm font-medium">Voltar</button></div></div>}</Card>;
}

function Usage({ data }: { data: ClientBillingResponse }) {
  const variation = data.usage.variationPercent;
  return <Card title="Adoção da plataforma" icon={<TrendingUp className="h-4 w-4" />}>
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4"><Metric value={data.usage.completedLast7Days} label="Avaliações nos últimos 7 dias" /><Metric value={data.usage.completedLast30Days} label="Avaliações no ciclo atual" /><Metric value={data.usage.completedPrevious30Days} label="Avaliações no ciclo anterior" /><Metric value={data.usage.completedAllTime} label="Avaliações concluídas no total" /></div>
    <div className="rounded-md border border-border bg-muted/20 p-4"><p className="text-xs text-muted-foreground">Evolução de uso</p><p className="mt-1 text-base font-semibold">{adoptionLabel(data.usage.adoptionLevel)}</p><p className="mt-1 text-sm text-muted-foreground">{variation == null ? "Ainda não há volume suficiente no ciclo anterior para uma comparação confiável." : `${variation > 0 ? "+" : ""}${Math.round(variation)}% em relação aos 30 dias anteriores.`}</p><p className="mt-2 text-xs text-muted-foreground">Este indicador mede utilização da plataforma. Ele não representa, isoladamente, retorno financeiro ou ROI.</p></div>
  </Card>;
}
function Card({ id, title, icon, children }: { id?: string; title: string; icon: React.ReactNode; children: React.ReactNode }) { return <section id={id} className="space-y-4 rounded-md border border-border bg-card p-6"><div className="flex items-center gap-2"><span className="text-muted-foreground">{icon}</span><h2 className="text-base font-semibold">{title}</h2></div>{children}</section>; }
function Info({ label, value }: { label: string; value: React.ReactNode }) { return <div><p className="text-xs text-muted-foreground">{label}</p><div className="mt-0.5 text-sm font-medium">{value}</div></div>; }
function Metric({ value, label }: { value: number; label: string }) { return <div className="rounded-md border border-border/60 bg-muted/30 p-4 text-center"><p className="text-2xl font-bold">{value}</p><p className="mt-1 text-xs text-muted-foreground">{label}</p></div>; }
function Badge({ text, tone }: { text: string; tone: "success" | "neutral" }) { return <span className={cn("inline-flex rounded-full px-2 py-0.5 text-xs font-medium", tone === "success" ? "bg-emerald-100 text-emerald-700" : "bg-gray-100 text-gray-600")}>{text}</span>; }
function Financial({ status }: { status: FinancialStatus }) { const warning = status === "PENDENTE_PAGAMENTO" || status === "INADIMPLENTE"; return <span className={cn("inline-flex items-center gap-1", warning ? "text-amber-700" : "text-emerald-700")}>{warning ? <AlertTriangle className="h-4 w-4" /> : <CheckCircle2 className="h-4 w-4" />}{({ REGULAR: "Regular", PENDENTE_PAGAMENTO: "Pagamento pendente", INADIMPLENTE: "Pagamento em atraso", SEM_CREDITO: "Sem créditos", CANCELADO: "Cancelado" }[status])}</span>; }
function Loading() { return <div className="flex justify-center gap-2 py-12 text-muted-foreground"><Loader2 className="h-5 w-5 animate-spin" />Carregando informações financeiras...</div>; }
function Failure({ retry }: { retry: () => void }) { return <section className="space-y-3 rounded-md border border-destructive/40 bg-destructive/5 p-6 text-center"><AlertCircle className="mx-auto h-6 w-6 text-destructive" /><p className="text-sm font-medium">Não foi possível carregar as informações de cobrança.</p><button type="button" onClick={retry} className="rounded-md border border-border bg-card px-4 py-2 text-sm">Tentar novamente</button></section>; }
const autoRechargeNeedsAttention = (config: AutoRechargeConfigResponse) => config.enabled && config.status === "IDLE" && Boolean(config.lastOutcome && /(recusad|falha|ausente|inválid|não retornou)/i.test(config.lastOutcome));
const adoptionLabel = (level: ClientBillingResponse["usage"]["adoptionLevel"]) => ({ SEM_BASE: "Dados insuficientes para comparação", ATENCAO: "Uso em queda", CRESCIMENTO: "Uso em crescimento", ESTAVEL: "Uso estável" }[level]);
const date = (value: string) => new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(value));
const dateTime = (value: string) => new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
const planName = (plan: CommercialPlanType | null) => plan === "AVULSO" ? "Créditos avulsos" : plan === "PROFISSIONAL" ? "Assinatura mensal" : plan === "ENTERPRISE" ? "Enterprise" : "Não informado";
const empresaStatus = (status: string) => ({ ATIVO: "Ativo", EM_TESTE: "Em teste", PENDENTE_PAGAMENTO: "Pagamento pendente", INADIMPLENTE: "Inadimplente", SEM_CREDITO: "Sem créditos", SUSPENSO: "Suspenso", CANCELADO: "Cancelado" }[status] ?? status);
const subscriptionStatus = (status: string) => ({ PENDING: "Aguardando autorização", AUTHORIZED: "Ativa", DELINQUENT: "Pagamento em atraso", PAUSED: "Pausada", CANCELLED: "Cancelada" }[status] ?? status);
const errorMessage = (error: unknown) => error instanceof Error ? error.message : error ? "Não foi possível concluir esta ação." : null;
