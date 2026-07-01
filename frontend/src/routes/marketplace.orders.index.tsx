import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Loader2, ShoppingBag } from "lucide-react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { listMarketplaceOrders } from "@/lib/api/praxis";
import { formatMarketplaceDate, formatMarketplacePrice, orderStatusLabels } from "@/lib/marketplace";

export const Route = createFileRoute("/marketplace/orders/")({
  head: () => ({
    meta: [{ title: "Minhas compras - Marketplace Praxis" }],
  }),
  component: MarketplaceOrdersPage,
});

function MarketplaceOrdersPage() {
  const orders = useQuery({
    queryKey: ["marketplace-orders"],
    queryFn: listMarketplaceOrders,
    retry: false,
  });

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-5xl px-5 py-6">
        <Button asChild variant="ghost" size="sm" className="mb-4">
          <Link to="/marketplace">
            <ArrowLeft className="h-4 w-4" />
            Marketplace
          </Link>
        </Button>

        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="text-xs uppercase text-primary">Compras</div>
            <h1 className="mt-1 text-2xl font-semibold">Minhas compras</h1>
          </div>
          <Button asChild variant="outline">
            <Link to="/marketplace">
              <ShoppingBag className="h-4 w-4" />
              Ver vitrine
            </Link>
          </Button>
        </div>

        {orders.isLoading && (
          <div className="flex items-center gap-2 rounded-md border border-border bg-card p-4 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            Carregando compras
          </div>
        )}
        {orders.isError && (
          <StateBanner tone="danger" title="Nao foi possivel carregar suas compras">
            {orders.error instanceof Error ? orders.error.message : "Tente novamente."}
          </StateBanner>
        )}
        {orders.data?.length === 0 && (
          <section className="rounded-md border border-border bg-card p-5">
            <p className="text-sm text-muted-foreground">Nenhuma compra registrada ainda.</p>
          </section>
        )}
        {(orders.data ?? []).length > 0 && (
          <section className="rounded-md border border-border bg-card">
            <div className="grid grid-cols-[1fr_150px_130px] gap-3 border-b border-border px-4 py-3 text-xs font-medium uppercase text-muted-foreground">
              <div>Teste</div>
              <div>Status</div>
              <div className="text-right">Valor</div>
            </div>
            <div className="divide-y divide-border">
              {orders.data!.map((order) => (
                <article key={order.id} className="grid grid-cols-1 gap-3 px-4 py-4 sm:grid-cols-[1fr_150px_130px] sm:items-center">
                  <div>
                    <Link
                      to="/marketplace/orders/$orderId"
                      params={{ orderId: String(order.id) }}
                      className="font-medium hover:text-primary"
                    >
                      {order.listingTitle}
                    </Link>
                    <div className="mt-1 text-xs text-muted-foreground">
                      Pedido #{order.id} · {formatMarketplaceDate(order.paidAt)}
                    </div>
                    {order.clonedSimulationId && (
                      <div className="mt-1 text-xs text-muted-foreground">
                        Simulacao clonada #{order.clonedSimulationId}
                      </div>
                    )}
                  </div>
                  <div className="text-sm text-muted-foreground">{orderStatusLabels[order.status]}</div>
                  <div className="text-sm font-semibold sm:text-right">{formatMarketplacePrice(order.priceCents)}</div>
                </article>
              ))}
            </div>
          </section>
        )}
      </div>
    </main>
  );
}
