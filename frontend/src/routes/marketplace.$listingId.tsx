import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, CheckCircle2, Loader2, MessageSquare, ShieldCheck, Star } from "lucide-react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { getMarketplaceListing, getMarketplaceReviews } from "@/lib/api/praxis";
import { categoryLabel, formatMarketplaceDate, formatMarketplacePrice } from "@/lib/marketplace";

export const Route = createFileRoute("/marketplace/$listingId")({
  head: () => ({
    meta: [{ title: "Detalhe da avaliação - Marketplace Práxis" }],
  }),
  component: MarketplaceListingDetailPage,
});

function MarketplaceListingDetailPage() {
  const { listingId } = Route.useParams();
  const numericListingId = Number(listingId);

  const listing = useQuery({
    queryKey: ["marketplace-listing", listingId],
    queryFn: () => getMarketplaceListing(listingId),
    enabled: Number.isFinite(numericListingId),
  });
  const reviews = useQuery({
    queryKey: ["marketplace-reviews", numericListingId],
    queryFn: () => getMarketplaceReviews(numericListingId),
    enabled: Number.isFinite(numericListingId),
  });

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-6xl px-5 py-6">
        <Button asChild variant="ghost" size="sm" className="mb-4">
          <Link to="/marketplace">
            <ArrowLeft className="h-4 w-4" />
            Marketplace
          </Link>
        </Button>

        {listing.isLoading && (
          <div className="flex items-center gap-2 rounded-md border border-border bg-card p-4 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            Carregando anúncio
          </div>
        )}
        {listing.isError && (
          <StateBanner tone="danger" title="Não foi possível carregar o anúncio">
            {listing.error instanceof Error ? listing.error.message : "Tente novamente."}
          </StateBanner>
        )}
        {listing.data && (
          <div className="grid gap-5 lg:grid-cols-[1fr_340px]">
            <section className="rounded-md border border-border bg-card p-5">
              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-md bg-primary/10 px-2 py-1 text-xs font-medium text-primary">
                  {categoryLabel(listing.data.category)}
                </span>
                <span className="inline-flex items-center gap-1 rounded-md bg-success/10 px-2 py-1 text-xs font-medium text-success">
                  <ShieldCheck className="h-3.5 w-3.5" />
                  Profissional verificado
                </span>
              </div>
              <h1 className="mt-4 text-3xl font-semibold">{listing.data.title}</h1>
              <p className="mt-3 whitespace-pre-line text-sm leading-6 text-muted-foreground">
                {listing.data.description}
              </p>

              <div className="mt-6 grid gap-3 sm:grid-cols-3">
                <Metric label="Preço" value={formatMarketplacePrice(listing.data.priceCents)} />
                <Metric
                  label="Avaliação"
                  value={listing.data.averageRating ? Number(listing.data.averageRating).toFixed(1) : "Sem notas"}
                />
                <Metric label="Reviews" value={String(listing.data.totalReviews)} />
              </div>

              <section className="mt-6 rounded-md border border-border bg-background p-4">
                <h2 className="text-sm font-semibold">Prévia liberada</h2>
                {listing.data.previewNodes.length === 0 ? (
                  <p className="mt-1 text-sm text-muted-foreground">
                    Este anúncio não possui etapas públicas de preview.
                  </p>
                ) : (
                  <div className="mt-3 space-y-3">
                    {listing.data.previewNodes.map((node) => (
                      <article key={node.id} className="rounded-md border border-border bg-card p-3">
                        <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                          <span className="font-medium text-foreground">{node.speaker}</span>
                          <span>Etapa {node.turnIndex}</span>
                        </div>
                        <p className="mt-2 whitespace-pre-line text-sm leading-6">{node.message}</p>
                        {node.options.length > 0 && (
                          <div className="mt-3 grid gap-2">
                            {node.options.map((option) => (
                              <div key={option.id} className="rounded-md border border-border bg-background px-3 py-2 text-sm">
                                {option.text}
                              </div>
                            ))}
                          </div>
                        )}
                      </article>
                    ))}
                  </div>
                )}
              </section>
            </section>

            <aside className="space-y-4">
              <section className="rounded-md border border-border bg-card p-5">
                <div className="text-sm text-muted-foreground">Profissional</div>
                <Link
                  to="/marketplace/professionals/$professionalId"
                  params={{ professionalId: String(listing.data.professional.id) }}
                  className="mt-1 block text-xl font-semibold hover:text-primary"
                >
                  {listing.data.professional.displayName}
                </Link>
                <div className="mt-3 flex items-center gap-2 text-sm text-muted-foreground">
                  <Star className="h-4 w-4 text-warning" />
                  {listing.data.averageRating ? Number(listing.data.averageRating).toFixed(1) : "Sem notas"} ·{" "}
                  {listing.data.totalReviews} review(s)
                </div>
                <Button asChild className="mt-5 w-full">
                  <Link to="/marketplace/checkout/$listingId" params={{ listingId }}>
                    Comprar avaliação
                  </Link>
                </Button>
                <Button asChild variant="outline" className="mt-2 w-full">
                  <Link to="/dashboard">
                    <MessageSquare className="h-4 w-4" />
                    Entrar para conversar
                  </Link>
                </Button>
              </section>
              <section className="rounded-md border border-border bg-card p-5">
                <h2 className="text-sm font-semibold">Entrega</h2>
                <ul className="mt-3 space-y-2 text-sm text-muted-foreground">
                  <li className="flex gap-2">
                    <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-success" />
                    Cópia da simulação no workspace comprador após pagamento aprovado.
                  </li>
                  <li className="flex gap-2">
                    <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-success" />
                    Histórico financeiro e pedido preservados para auditoria.
                  </li>
                </ul>
              </section>
            </aside>
          </div>
        )}

        {reviews.data && reviews.data.length > 0 && (
          <section className="mt-5 rounded-md border border-border bg-card p-5">
            <h2 className="text-lg font-semibold">Reviews</h2>
            <div className="mt-3 divide-y divide-border">
              {reviews.data.map((review) => (
                <div key={review.id} className="py-3">
                  <div className="flex flex-wrap items-center gap-2 text-sm">
                    <span className="font-semibold">{review.rating}/5</span>
                    <span className="text-muted-foreground">{formatMarketplaceDate(review.createdAt)}</span>
                  </div>
                  {review.comment && <p className="mt-1 text-sm text-muted-foreground">{review.comment}</p>}
                </div>
              ))}
            </div>
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

