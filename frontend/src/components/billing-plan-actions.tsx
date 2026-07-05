import { useState } from "react";
import { ArrowRightLeft, Building2, CreditCard, Loader2, Send, XCircle } from "lucide-react";
import type { CommercialPlanType, PlanChangeRequest, PlanChangeRequestType, PlanManagementResponse, SubscriptionPlan } from "@/lib/api/client-billing";

export function BillingPlanActions({ management, plans, busy, onBuyCredits, onChangePlan, onEnterpriseRequest }: {
  management: PlanManagementResponse;
  plans: SubscriptionPlan[];
  busy: boolean;
  onBuyCredits: (id: number) => void;
  onChangePlan: (id: number) => void;
  onEnterpriseRequest: (type: PlanChangeRequestType, plan?: CommercialPlanType, note?: string) => void;
}) {
  return management.currentPlan === "ENTERPRISE"
    ? <EnterpriseActions requests={management.enterpriseRequests} busy={busy} onRequest={onEnterpriseRequest} />
    : <SelfServiceActions currentPlan={management.currentPlan} plans={plans} busy={busy} onBuyCredits={onBuyCredits} onChange={onChangePlan} />;
}

function SelfServiceActions({ currentPlan, plans, busy, onBuyCredits, onChange }: { currentPlan: Exclude<CommercialPlanType, "ENTERPRISE">; plans: SubscriptionPlan[]; busy: boolean; onBuyCredits: (id: number) => void; onChange: (id: number) => void; }) {
  const [selected, setSelected] = useState<SubscriptionPlan | null>(null);
  const credits = plans.filter((plan) => plan.planType === "AVULSO");
  const subscriptions = plans.filter((plan) => plan.planType === "PROFISSIONAL");
  const changingToCredits = currentPlan === "PROFISSIONAL";
  const changingToSubscription = currentPlan === "AVULSO";

  return <section className="space-y-4 rounded-md border border-border bg-card p-6">
    <Title icon={<ArrowRightLeft className="h-4 w-4" />} text="Gerenciar plano" />
    {currentPlan === "AVULSO" ? <p className="text-sm text-muted-foreground">Seu modelo atual não possui cobrança recorrente. Você pode comprar novos créditos quando precisar ou migrar para uma assinatura mensal.</p> : <p className="text-sm text-muted-foreground">Você pode trocar sua assinatura mensal ou migrar para créditos avulsos. Ao confirmar uma mudança, a cobrança recorrente atual será cancelada antes do checkout do novo plano.</p>}
    {currentPlan === "AVULSO" && <PlanList title="Comprar créditos" plans={credits} busy={busy} action="Comprar pacote" onSelect={onBuyCredits} />}
    {changingToSubscription && <PlanList title="Migrar para assinatura mensal" plans={subscriptions} busy={busy} action="Escolher assinatura" onSelect={(id) => setSelected(plans.find((plan) => plan.id === id) ?? null)} />}
    {currentPlan === "PROFISSIONAL" && <>
      <PlanList title="Trocar assinatura mensal" plans={subscriptions} busy={busy} action="Mudar assinatura" onSelect={(id) => setSelected(plans.find((plan) => plan.id === id) ?? null)} />
      <PlanList title="Migrar para créditos avulsos" plans={credits} busy={busy} action="Mudar para créditos" onSelect={(id) => setSelected(plans.find((plan) => plan.id === id) ?? null)} />
    </>}
    {selected && <div className="space-y-3 rounded-md border border-amber-300 bg-amber-50/50 p-4">
      <p className="text-sm font-semibold">Confirmar alteração para {selected.name}?</p>
      <p className="text-sm text-muted-foreground">{changingToCredits ? "A assinatura mensal atual será cancelada e o checkout do pacote de créditos será aberto." : "O checkout do Mercado Pago será aberto para autorizar a nova cobrança recorrente."}</p>
      <div className="flex flex-wrap gap-2"><button type="button" disabled={busy} onClick={() => { onChange(selected.id); setSelected(null); }} className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-60">{busy && <Loader2 className="h-4 w-4 animate-spin" />}Confirmar mudança</button><button type="button" disabled={busy} onClick={() => setSelected(null)} className="rounded-md border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-accent">Voltar</button></div>
    </div>}
  </section>;
}

function EnterpriseActions({ requests, busy, onRequest }: { requests: PlanChangeRequest[]; busy: boolean; onRequest: (type: PlanChangeRequestType, plan?: CommercialPlanType, note?: string) => void; }) {
  const [mode, setMode] = useState<"change" | "cancel" | null>(null);
  const [target, setTarget] = useState<CommercialPlanType>("PROFISSIONAL");
  const [note, setNote] = useState("");
  const pending = requests.find((request) => request.status === "PENDING");
  return <section className="space-y-4 rounded-md border border-border bg-card p-6">
    <Title icon={<Building2 className="h-4 w-4" />} text="Gerenciar contrato Enterprise" />
    <p className="text-sm text-muted-foreground">Alterações de um contrato Enterprise dependem de revisão comercial. Registre a solicitação aqui; ela fica visível nesta página até ser tratada.</p>
    {pending ? <RequestStatus request={pending} /> : mode === null ? <div className="flex flex-wrap gap-2"><button type="button" onClick={() => setMode("change")} className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"><ArrowRightLeft className="h-4 w-4" />Solicitar mudança de plano</button><button type="button" onClick={() => setMode("cancel")} className="inline-flex items-center gap-2 rounded-md border border-destructive/40 bg-card px-4 py-2 text-sm font-medium text-destructive hover:bg-destructive/5"><XCircle className="h-4 w-4" />Solicitar cancelamento</button></div> : <div className="space-y-3 rounded-md border border-border bg-muted/20 p-4">
      <p className="text-sm font-semibold">{mode === "change" ? "Qual plano deseja solicitar?" : "Confirmar solicitação de cancelamento?"}</p>
      {mode === "change" && <div className="flex flex-wrap gap-2"><Choice active={target === "PROFISSIONAL"} onClick={() => setTarget("PROFISSIONAL")} text="Assinatura mensal" /><Choice active={target === "AVULSO"} onClick={() => setTarget("AVULSO")} text="Créditos avulsos" /></div>}
      <textarea value={note} onChange={(event) => setNote(event.target.value)} maxLength={1000} placeholder="Explique o que sua empresa precisa (opcional)" className="min-h-20 w-full rounded-md border border-input bg-background px-3 py-2 text-sm" />
      <div className="flex flex-wrap gap-2"><button type="button" disabled={busy} onClick={() => { onRequest(mode === "change" ? "CHANGE_PLAN" : "CANCEL_CONTRACT", mode === "change" ? target : undefined, note); setMode(null); setNote(""); }} className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:opacity-60">{busy && <Loader2 className="h-4 w-4 animate-spin" />}<Send className="h-4 w-4" />Enviar solicitação</button><button type="button" disabled={busy} onClick={() => setMode(null)} className="rounded-md border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-accent">Voltar</button></div>
    </div>}
    {requests.filter((request) => request.status !== "PENDING").slice(0, 3).map((request) => <RequestStatus key={request.id} request={request} />)}
  </section>;
}

function PlanList({ title, plans, busy, action, onSelect }: { title: string; plans: SubscriptionPlan[]; busy: boolean; action: string; onSelect: (id: number) => void; }) {
  return <div className="space-y-2"><h3 className="text-sm font-semibold">{title}</h3>{plans.length === 0 ? <p className="text-sm text-muted-foreground">Nenhuma opção disponível agora.</p> : <div className="grid gap-3 lg:grid-cols-2">{plans.map((plan) => <div key={plan.id} className="rounded-md border border-border p-4"><div className="flex items-start justify-between gap-3"><div><p className="font-semibold">{plan.name}</p><p className="mt-1 text-sm text-muted-foreground">{plan.planType === "AVULSO" ? `${plan.creditAmount} créditos` : "Cobrança mensal recorrente"}</p></div><p className="font-semibold">{money(plan.priceCents, plan.currency)}</p></div><button type="button" disabled={busy} onClick={() => onSelect(plan.id)} className="mt-4 inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-accent disabled:opacity-60"><CreditCard className="h-4 w-4" />{action}</button></div>)}</div>}</div>;
}

function RequestStatus({ request }: { request: PlanChangeRequest }) { return <div className="rounded-md border border-border bg-muted/20 p-3 text-sm"><p className="font-medium">{request.requestType === "CANCEL_CONTRACT" ? "Solicitação de cancelamento" : `Solicitação de mudança para ${label(request.requestedPlan)}`}</p><p className="mt-1 text-muted-foreground">Status: {status(request.status)} · enviada em {date(request.createdAt)}</p>{request.note && <p className="mt-1 text-muted-foreground">{request.note}</p>}</div>; }
function Choice({ active, text, onClick }: { active: boolean; text: string; onClick: () => void }) { return <button type="button" onClick={onClick} className={active ? "rounded-md border border-primary bg-primary/10 px-3 py-2 text-sm font-medium" : "rounded-md border border-border bg-card px-3 py-2 text-sm"}>{text}</button>; }
function Title({ icon, text }: { icon: React.ReactNode; text: string }) { return <div className="flex items-center gap-2"><span className="text-muted-foreground">{icon}</span><h2 className="text-base font-semibold">{text}</h2></div>; }
const money = (cents: number, currency: string) => new Intl.NumberFormat("pt-BR", { style: "currency", currency }).format(cents / 100);
const date = (value: string) => new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(value));
const label = (plan: CommercialPlanType | null) => plan === "AVULSO" ? "créditos avulsos" : plan === "PROFISSIONAL" ? "assinatura mensal" : "plano";
const status = (value: string) => ({ PENDING: "Aguardando análise", IN_REVIEW: "Em análise", APPROVED: "Aprovada", REJECTED: "Recusada", CANCELLED: "Cancelada" }[value] ?? value);
