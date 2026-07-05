import { CreditCard, History } from "lucide-react";
import type { BillingEvent, CreditMovement } from "@/lib/api/client-billing";
import { cn } from "@/lib/utils";

export function BillingHistory({ events, movements }: { events: BillingEvent[]; movements: CreditMovement[] }) {
  return (
    <div className="space-y-4">
      <section className="space-y-4 rounded-md border border-border bg-card p-6">
        <Title icon={<History className="h-4 w-4" />} text="Histórico de pagamentos" />
        {events.length === 0 ? <Empty text="Nenhuma cobrança ou pagamento foi registrado ainda." /> : (
          <div className="-mx-6 overflow-x-auto px-6"><table className="w-full text-sm"><thead><tr className="border-b border-border text-left text-muted-foreground"><th className="pb-2 pr-4 font-medium">Data</th><th className="pb-2 pr-4 font-medium">Movimentação</th><th className="pb-2 pr-4 font-medium">Status</th><th className="pb-2 text-right font-medium">Valor</th></tr></thead><tbody>{events.map((event) => <tr key={event.id} className="border-b border-border/50 last:border-0"><td className="whitespace-nowrap py-3 pr-4 text-muted-foreground">{dateTime(event.createdAt)}</td><td className="py-3 pr-4">{eventLabel(event.eventType)}</td><td className="py-3 pr-4 text-muted-foreground">{paymentStatus(event.mpStatus)}</td><td className="py-3 text-right">{event.amountCents == null ? "—" : money(event.amountCents, event.currency ?? "BRL")}</td></tr>)}</tbody></table></div>
        )}
      </section>
      <section className="space-y-4 rounded-md border border-border bg-card p-6">
        <Title icon={<CreditCard className="h-4 w-4" />} text="Extrato de créditos" />
        {movements.length === 0 ? <Empty text="Nenhuma movimentação de crédito foi registrada ainda." /> : (
          <div className="-mx-6 overflow-x-auto px-6"><table className="w-full text-sm"><thead><tr className="border-b border-border text-left text-muted-foreground"><th className="pb-2 pr-4 font-medium">Data</th><th className="pb-2 pr-4 font-medium">Movimentação</th><th className="pb-2 pr-4 text-right font-medium">Variação</th><th className="pb-2 text-right font-medium">Saldo após</th></tr></thead><tbody>{movements.map((item) => <tr key={item.id} className="border-b border-border/50 last:border-0"><td className="whitespace-nowrap py-3 pr-4 text-muted-foreground">{dateTime(item.createdAt)}</td><td className="py-3 pr-4"><p>{creditLabel(item.reason)}</p>{item.note && <p className="mt-0.5 text-xs text-muted-foreground">{item.note}</p>}</td><td className={cn("py-3 pr-4 text-right font-medium", item.delta > 0 ? "text-emerald-700" : "text-destructive")}>{item.delta > 0 ? "+" : ""}{item.delta}</td><td className="py-3 text-right">{item.balanceAfter}</td></tr>)}</tbody></table></div>
        )}
      </section>
    </div>
  );
}

function Title({ icon, text }: { icon: React.ReactNode; text: string }) { return <div className="flex items-center gap-2"><span className="text-muted-foreground">{icon}</span><h2 className="text-base font-semibold">{text}</h2></div>; }
function Empty({ text }: { text: string }) { return <p className="rounded-md border border-dashed border-border px-4 py-5 text-sm text-muted-foreground">{text}</p>; }
const dateTime = (value: string) => new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
const money = (cents: number, currency: string) => new Intl.NumberFormat("pt-BR", { style: "currency", currency }).format(cents / 100);
const paymentStatus = (value: string | null) => ({ approved: "Aprovado", authorized: "Autorizada", pending: "Pendente", rejected: "Recusado", cancelled: "Cancelado", refunded: "Estornado", charged_back: "Contestado", created: "Checkout criado", paused: "Pausada" }[value ?? ""] ?? value ?? "—");
const eventLabel = (value: string) => ({ CREDIT_CHECKOUT_CREATED: "Checkout de créditos iniciado", CREDIT_PURCHASE_APPROVED: "Compra de créditos aprovada", CREDIT_AUTO_RECHARGE_FAILED: "Recarga automática recusada", SUBSCRIPTION_CREATED: "Assinatura criada", SUBSCRIPTION_AUTHORIZED: "Assinatura autorizada", SUBSCRIPTION_PAYMENT_APPROVED: "Pagamento da assinatura aprovado", SUBSCRIPTION_PAYMENT_REJECTED: "Pagamento da assinatura recusado", SUBSCRIPTION_CANCELLED: "Assinatura cancelada", PAYMENT_PENDING: "Pagamento pendente", PAYMENT_REFUNDED: "Pagamento estornado", PAYMENT_CHARGEBACK: "Contestação de pagamento" }[value] ?? value);
const creditLabel = (value: string) => ({ PURCHASE: "Créditos adquiridos", CONSUMPTION: "Crédito utilizado em avaliação", ADJUSTMENT: "Ajuste de créditos" }[value] ?? value);
