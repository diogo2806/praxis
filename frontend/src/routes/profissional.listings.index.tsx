import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, ExternalLink, Loader2, Plus } from "lucide-react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { getMarketplaceProfessionalDashboard } from "@/lib/api/praxis";
import { listingStatusLabels } from "@/lib/marketplace";

export const Route = createFileRoute("/profissional/listings/")({
  head: () => ({
    meta: [{ title: "Meus testes - Marketplace Praxis" }],
  }),
  component: ProfessionalListingsPage,
});

function ProfessionalListingsPage() {
  const dashboard = useQuery({
    queryKey: ["marketplace-professional-dashboard"],
    queryFn: getMarketplaceProfessionalDashboard,
    retry: false,
  });
  const listings = dashboard.data?.listings ?? [];

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-5xl px-5 py-6">
        <Button asChild variant="ghost" size="sm" className="mb-4">
          <Link to="/profissional">
            <ArrowLeft className="h-4 w-4" />
            Area do profissional
          </Link>
        </Button>

        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="text-xs uppercase text-primary">Anuncios</div>
            <h1 className="mt-1 text-2xl font-semibold">Meus testes</h1>
          </div>
          <Button asChild>
            <Link to="/profissional/listings/novo">
              <Plus className="h-4 w-4" />
              Novo anuncio
            </Link>
          </Button>
        </div>

        {dashboard.isLoading && (
          <div className="flex items-center gap-2 rounded-md border border-border bg-card p-4 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            Carregando anuncios
          </div>
        )}
        {dashboard.isError && (
          <StateBanner tone="danger" title="Nao foi possivel carregar seus anuncios">
            {dashboard.error instanceof Error ? dashboard.error.message : "Tente novamente."}
          </StateBanner>
        )}
        {dashboard.data && listings.length === 0 && (
          <section className="rounded-md border border-border bg-card p-5">
            <p className="text-sm text-muted-foreground">Nenhum teste publicado ainda.</p>
          </section>
        )}
        {listings.length > 0 && (
          <section className="rounded-md border border-border bg-card">
            <div className="grid grid-cols-[1fr_140px_120px] gap-3 border-b border-border px-4 py-3 text-xs font-medium uppercase text-muted-foreground">
              <div>Titulo</div>
              <div>Status</div>
              <div className="text-right">Vendas</div>
            </div>
            <div className="divide-y divide-border">
              {listings.map((listing) => (
                <div key={listing.id} className="grid grid-cols-1 gap-3 px-4 py-4 sm:grid-cols-[1fr_140px_120px] sm:items-center">
                  <div>
                    <div className="font-medium">{listing.title}</div>
                    <div className="mt-1 flex flex-wrap gap-3">
                      <Button asChild variant="link" className="h-auto p-0 text-xs">
                        <Link to="/profissional/listings/$listingId/editar" params={{ listingId: String(listing.id) }}>
                          Editar
                        </Link>
                      </Button>
                      {listing.status === "APPROVED" && (
                        <Button asChild variant="link" className="h-auto p-0 text-xs">
                          <Link to="/marketplace/$listingId" params={{ listingId: String(listing.id) }}>
                            <ExternalLink className="h-3.5 w-3.5" />
                            Ver na vitrine
                          </Link>
                        </Button>
                      )}
                    </div>
                  </div>
                  <div className="text-sm text-muted-foreground">{listingStatusLabels[listing.status]}</div>
                  <div className="text-sm font-semibold sm:text-right">{listing.salesCount} venda(s)</div>
                </div>
              ))}
            </div>
          </section>
        )}
      </div>
    </main>
  );
}
