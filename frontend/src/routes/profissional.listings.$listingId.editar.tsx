import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ArrowLeft, Loader2, Save } from "lucide-react";
import { useEffect, useState } from "react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  getMarketplaceListing,
  updateMarketplaceListing,
  type ListingCategory,
  type UpdateMarketplaceListingRequest,
} from "@/lib/api/praxis";
import { marketplaceCategories, splitList } from "@/lib/marketplace";

export const Route = createFileRoute("/profissional/listings/$listingId/editar")({
  head: () => ({
    meta: [{ title: "Editar anúncio - Marketplace Práxis" }],
  }),
  component: EditProfessionalListingPage,
});

function EditProfessionalListingPage() {
  const { listingId } = Route.useParams();
  const numericListingId = Number(listingId);
  const listing = useQuery({
    queryKey: ["marketplace-listing", listingId],
    queryFn: () => getMarketplaceListing(listingId),
    enabled: Number.isFinite(numericListingId),
  });
  const [form, setForm] = useState({
    title: "",
    description: "",
    category: "" as ListingCategory | "",
    price: "",
    previewNodeIds: "",
  });

  // Pré-carrega o formulário com os dados atuais do anúncio.
  useEffect(() => {
    if (!listing.data) return;
    setForm({
      title: listing.data.title,
      description: listing.data.description,
      category: listing.data.category,
      price: (listing.data.priceCents / 100).toFixed(2).replace(".", ","),
      previewNodeIds: listing.data.previewNodeIds.join(", "),
    });
  }, [listing.data]);

  const buildRequest = (): UpdateMarketplaceListingRequest => {
    const request: UpdateMarketplaceListingRequest = {};
    if (form.title.trim()) request.title = form.title.trim();
    if (form.description.trim()) request.description = form.description.trim();
    if (form.category) request.category = form.category;
    if (form.price.trim())
      request.priceCents = Math.round(Number(form.price.replace(",", ".")) * 100);
    if (form.previewNodeIds.trim())
      request.previewNodeIds = splitList(form.previewNodeIds).map(Number).filter(Number.isFinite);
    return request;
  };

  const update = useMutation({
    mutationFn: () => updateMarketplaceListing(numericListingId, buildRequest()),
  });

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-4xl px-5 py-6">
        <Button asChild variant="ghost" size="sm" className="mb-4">
          <Link to="/profissional/listings">
            <ArrowLeft className="h-4 w-4" />
            Minhas avaliações
          </Link>
        </Button>

        <section className="rounded-md border border-border bg-card p-5">
          <div className="text-xs uppercase text-primary">Anúncios</div>
          <h1 className="mt-1 text-2xl font-semibold">
            {listing.data ? `Editar "${listing.data.title}"` : "Editar anúncio"}
          </h1>

          {listing.isLoading && (
            <div className="mt-5 flex items-center gap-2 text-sm text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              Carregando anúncio
            </div>
          )}
          {listing.isError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Não foi possível carregar o anúncio">
                {listing.error instanceof Error ? listing.error.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}
          {update.isSuccess && (
            <div className="mt-5">
              <StateBanner tone="ok" title="Anúncio atualizado" />
            </div>
          )}
          {update.isError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Não foi possível atualizar o anúncio">
                {update.error instanceof Error ? update.error.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}

          {listing.data && (
            <form
              className="mt-5 grid gap-4"
              onSubmit={(event) => {
                event.preventDefault();
                update.mutate();
              }}
            >
              <Field
                label="Título"
                value={form.title}
                onChange={(value) => setForm({ ...form, title: value })}
              />
              <label className="grid gap-1 text-sm">
                <span className="font-medium">Categoria</span>
                <select
                  value={form.category}
                  onChange={(event) =>
                    setForm({ ...form, category: event.target.value as ListingCategory })
                  }
                  className="h-9 rounded-md border border-input bg-background px-3 text-sm"
                >
                  {marketplaceCategories.map((item) => (
                    <option key={item.value} value={item.value}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </label>
              <Field
                label="Preço em reais"
                value={form.price}
                onChange={(value) => setForm({ ...form, price: value })}
              />
              <Field
                label="Etapas liberadas para preview (opcional)"
                value={form.previewNodeIds}
                onChange={(value) => setForm({ ...form, previewNodeIds: value })}
                placeholder="101, 102"
              />
              <label className="grid gap-1 text-sm">
                <span className="font-medium">Descrição</span>
                <textarea
                  value={form.description}
                  onChange={(event) => setForm({ ...form, description: event.target.value })}
                  rows={7}
                  className="rounded-md border border-input bg-background px-3 py-2 text-sm"
                />
              </label>
              <div className="flex justify-end">
                <Button type="submit" disabled={update.isPending}>
                  {update.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Save className="h-4 w-4" />
                  )}
                  Salvar alterações
                </Button>
              </div>
            </form>
          )}
        </section>
      </div>
    </main>
  );
}

function Field({
  label,
  value,
  onChange,
  placeholder,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}) {
  return (
    <label className="grid gap-1 text-sm">
      <span className="font-medium">{label}</span>
      <Input
        value={value}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}
