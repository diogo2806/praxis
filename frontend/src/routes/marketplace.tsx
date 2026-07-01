import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Filter, Loader2, Search } from "lucide-react";
import { useMemo, useState } from "react";

import { ListingCard } from "@/components/marketplace/listing-card";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { searchMarketplaceListings, type ListingCategory } from "@/lib/api/praxis";
import { marketplaceCategories } from "@/lib/marketplace";

export const Route = createFileRoute("/marketplace")({
  head: () => ({
    meta: [{ title: "Marketplace de psicometria - Práxis" }],
  }),
  component: MarketplacePage,
});

function MarketplacePage() {
  const navigate = useNavigate();
  const [text, setText] = useState("");
  const [category, setCategory] = useState<ListingCategory | "ALL">("ALL");

  const filters = useMemo(
    () => ({ text: text.trim() || undefined, category, size: 24 }),
    [category, text],
  );
  const listings = useQuery({
    queryKey: ["marketplace-listings", filters],
    queryFn: () => searchMarketplaceListings(filters),
  });

  return (
    <main className="min-h-screen bg-background text-foreground">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3 px-5 py-4">
          <Link to="/" className="font-display text-xl font-semibold">
            Práxis
          </Link>
          <div className="flex flex-wrap gap-2">
            <Button asChild variant="outline" size="sm">
              <Link to="/marketplace/orders">Minhas compras</Link>
            </Button>
            <Button asChild variant="outline" size="sm">
              <Link to="/profissional/cadastro">Vender avaliações</Link>
            </Button>
            <Button asChild size="sm">
              <Link to="/dashboard">Entrar</Link>
            </Button>
          </div>
        </div>
      </header>

      <div className="mx-auto max-w-7xl px-5 py-6">
        <div className="mb-5 flex flex-wrap items-end justify-between gap-4">
          <div>
            <div className="text-xs uppercase text-primary">Marketplace</div>
            <h1 className="mt-1 text-3xl font-semibold">Avaliações prontas para contratação</h1>
            <p className="mt-2 max-w-3xl text-sm text-muted-foreground">
              Compre instrumentos publicados por profissionais verificados e receba uma cópia editável no seu workspace.
            </p>
          </div>
          <Button variant="outline" onClick={() => void navigate({ to: "/profissional" })}>
            Área do profissional
          </Button>
        </div>

        <section className="mb-5 rounded-md border border-border bg-card p-4">
          <div className="grid gap-3 md:grid-cols-[1fr_220px_auto]">
            <label className="grid gap-1 text-sm">
              <span className="font-medium">Busca</span>
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  value={text}
                  onChange={(event) => setText(event.target.value)}
                  placeholder="Título, especialidade ou profissional"
                  className="pl-9"
                />
              </div>
            </label>
            <label className="grid gap-1 text-sm">
              <span className="font-medium">Categoria</span>
              <select
                value={category}
                onChange={(event) => setCategory(event.target.value as ListingCategory | "ALL")}
                className="h-9 rounded-md border border-input bg-background px-3 text-sm"
              >
                <option value="ALL">Todas</option>
                {marketplaceCategories.map((item) => (
                  <option key={item.value} value={item.value}>
                    {item.label}
                  </option>
                ))}
              </select>
            </label>
            <div className="flex items-end">
              <Button variant="outline" className="w-full md:w-auto" onClick={() => listings.refetch()}>
                <Filter className="h-4 w-4" />
                Filtrar
              </Button>
            </div>
          </div>
        </section>

        {listings.isLoading && (
          <div className="flex items-center gap-2 rounded-md border border-border bg-card p-4 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            Carregando avaliações
          </div>
        )}
        {listings.isError && (
          <StateBanner tone="danger" title="Não foi possível carregar o marketplace">
            {listings.error instanceof Error ? listings.error.message : "Tente novamente."}
          </StateBanner>
        )}
        {listings.data && listings.data.content.length === 0 && (
          <StateBanner tone="muted" title="Nenhum anúncio encontrado">
            Ajuste os filtros para ver outros instrumentos publicados.
          </StateBanner>
        )}
        {listings.data && listings.data.content.length > 0 && (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {listings.data.content.map((listing) => <ListingCard key={listing.id} listing={listing} />)}
          </div>
        )}
      </div>
    </main>
  );
}

