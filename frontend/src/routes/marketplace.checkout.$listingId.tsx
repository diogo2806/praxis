import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ArrowLeft, CreditCard, Loader2, ShieldCheck } from "lucide-react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { createMarketplaceCheckout, getMarketplaceListing } from "@/lib/api/praxis";
import { formatMarketplacePrice } from "@/lib/marketplace";

export const Route = createFileRoute("/marketplace/checkout/$listingId")({
  head: () => ({
    meta: [{ title: "Checkout marketplace - Práxis" }],
  }),
  component: MarketplaceCheckoutPage,
});

function MarketplaceCheckoutPage() {
  const { listingId } = Route.useParams();
  const numericListingId = Number(listingId);

  const listing = useQuery({
    queryKey: ["marketplace-listing", listingId],
    queryFn: () => getMarketplaceListing(listingId),
    enabled: Number.isFinite(numericListingId),
  });

  const checkout = useMutation({
    mutationFn: () => createMarketplaceCheckout(numericListingId),
    onSuccess: (data) => {
      window.location.assign(data.checkoutUrl);
    },
  });

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-4xl px-5 py-6">
        <Button asChild variant="ghost" size="sm" className="mb-4">
          <Link to="/marketplace/$listingId" params={{ listingId }}>
            <ArrowLeft className="h-4 w-4" />
            Voltar
          </Link>
        </Button>

        <div className="rounded-md border border-border bg-card p-5">
          <div className="flex items-center gap-2 text-xs uppercase text-primary">
            <CreditCard className="h-4 w-4" />
            Checkout
          </div>
          <h1 className="mt-2 text-2xl font-semibold">Finalizar compra</h1>

          {listing.isLoading && (
            <div className="mt-5 flex items-center gap-2 text-sm text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              Carregando pedido
            </div>
          )}
          {listing.isError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Não foi possível preparar o checkout">
                {listing.error instanceof Error ? listing.error.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}
          {checkout.isError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Não foi possível iniciar o pagamento">
                {checkout.error instanceof Error ? checkout.error.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}
          {listing.data && (
            <div className="mt-5 grid gap-5 md:grid-cols-[1fr_260px]">
              <section className="rounded-md border border-border bg-background p-4">
                <h2 className="text-lg font-semibold">{listing.data.title}</h2>
                <div className="mt-3 flex items-center gap-2 text-sm text-muted-foreground">
                  <ShieldCheck className="h-4 w-4" />
                  {listing.data.professional.displayName}
                </div>
                <p className="mt-3 line-clamp-4 text-sm text-muted-foreground">{listing.data.description}</p>
              </section>
              <aside className="rounded-md border border-border bg-background p-4">
                <div className="text-sm text-muted-foreground">Total</div>
                <div className="mt-1 text-2xl font-semibold">
                  {formatMarketplacePrice(listing.data.priceCents)}
                </div>
                <Button
                  className="mt-5 w-full"
                  disabled={checkout.isPending}
                  onClick={() => checkout.mutate()}
                >
                  {checkout.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <CreditCard className="h-4 w-4" />}
                  Pagar com Mercado Pago
                </Button>
              </aside>
            </div>
          )}
        </div>
      </div>
    </main>
  );
}

