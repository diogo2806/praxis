import type { MarketplaceListingDetail } from "@/lib/api/marketplace";
import { formatMarketplacePrice } from "@/lib/marketplace";

export function CheckoutSummary({ listing }: { listing: MarketplaceListingDetail }) {
  return (
    <section className="rounded-md border border-border bg-card p-5">
      <div className="text-sm text-muted-foreground">Resumo</div>
      <h2 className="mt-1 text-xl font-semibold">{listing.title}</h2>
      <div className="mt-3 text-sm text-muted-foreground">{listing.professional.displayName}</div>
      <div className="mt-5 flex items-center justify-between border-t border-border pt-4">
        <span className="text-sm text-muted-foreground">Total</span>
        <span className="text-2xl font-semibold">{formatMarketplacePrice(listing.priceCents)}</span>
      </div>
    </section>
  );
}
