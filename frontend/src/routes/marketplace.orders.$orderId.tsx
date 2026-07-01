import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, CheckCircle2, Loader2 } from "lucide-react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { getMarketplaceOrder } from "@/lib/api/praxis";
import { formatMarketplaceDate, formatMarketplacePrice, orderStatusLabels } from "@/lib/marketplace";

export const Route = createFileRoute("/marketplace/orders/$orderId")({
  head: () => ({
    meta: [{ title: "Detalhe da compra - Marketplace Praxis" }],
  }),
  component: MarketplaceOrderDetailPage,
});

function MarketplaceOrderDetailPage() {
  const { orderId } = Route.useParams();
  const order = useQuery({
    queryKey: ["marketplace-order", orderId],
    queryFn: () => getMarketplaceOrder(orderId),
    enabled: Number.isFinite(Number(orderId)),
    retry: false,
  });

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-4xl px-5 py-6">
        <Button asChild variant="ghost" size="sm" className="mb-4">
          <Link to="/marketplace/orders">
            <ArrowLeft className="h-4 w-4" />
            Minhas compras
          </Link>
        </Button>

        {order.isLoading && (
          <div className="flex items-center gap-2 rounded-md border border-border bg-card p-4 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            Carregando pedido
          </div>
        )}
        {order.isError && (
          <StateBanner tone="danger" title="Nao foi possivel carregar o pedido">
            {order.error instanceof Error ? order.error.message : "Tente novamente."}
          </StateBanner>
        )}
        {order.data && (
          <section className="rounded-md border border-border bg-card p-5">
            <div className="text-xs uppercase text-primary">Pedido #{order.data.id}</div>
            <h1 className="mt-1 text-2xl font-semibold">{order.data.listingTitle}</h1>

            <div className="mt-5 grid gap-3 sm:grid-cols-3">
              <Metric label="Status" value={orderStatusLabels[order.data.status]} />
              <Metric label="Valor" value={formatMarketplacePrice(order.data.priceCents)} />
              <Metric label="Pagamento" value={formatMarketplaceDate(order.data.paidAt)} />
            </div>

            <section className="mt-5 rounded-md border border-border bg-background p-4">
              <h2 className="flex items-center gap-2 text-sm font-semibold">
                <CheckCircle2 className="h-4 w-4 text-success" />
                Clone da simulacao
              </h2>
              <p className="mt-2 text-sm text-muted-foreground">
                {order.data.clonedSimulationId
                  ? `Simulacao clonada #${order.data.clonedSimulationId}`
                  : "A simulacao sera clonada no workspace comprador apos a confirmacao do pagamento."}
              </p>
            </section>
          </section>
        )}
      </div>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-border bg-background p-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-1 text-lg font-semibold">{value}</div>
    </div>
  );
}
