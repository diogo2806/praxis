import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation } from "@tanstack/react-query";
import { ArrowLeft, Loader2, Save } from "lucide-react";
import { useState } from "react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  updateMarketplaceListing,
  type ListingCategory,
  type UpdateMarketplaceListingRequest,
} from "@/lib/api/praxis";
import { marketplaceCategories, splitList } from "@/lib/marketplace";

export const Route = createFileRoute("/profissional/listings/$listingId/editar")({
  head: () => ({
    meta: [{ title: "Editar anuncio - Marketplace Praxis" }],
  }),
  component: EditProfessionalListingPage,
});

function EditProfessionalListingPage() {
  const { listingId } = Route.useParams();
  const numericListingId = Number(listingId);
  const [form, setForm] = useState({
    title: "",
    description: "",
    category: "" as ListingCategory | "",
    price: "",
    previewNodeIds: "",
  });

  const buildRequest = (): UpdateMarketplaceListingRequest => {
    const request: UpdateMarketplaceListingRequest = {};
    if (form.title.trim()) request.title = form.title.trim();
    if (form.description.trim()) request.description = form.description.trim();
    if (form.category) request.category = form.category;
    if (form.price.trim()) request.priceCents = Math.round(Number(form.price.replace(",", ".")) * 100);
    if (form.previewNodeIds.trim()) request.previewNodeIds = splitList(form.previewNodeIds).map(Number).filter(Number.isFinite);
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
            Meus testes
          </Link>
        </Button>

        <section className="rounded-md border border-border bg-card p-5">
          <div className="text-xs uppercase text-primary">Anuncios</div>
          <h1 className="mt-1 text-2xl font-semibold">Editar anuncio #{listingId}</h1>

          {update.isSuccess && (
            <div className="mt-5">
              <StateBanner tone="ok" title="Anuncio atualizado" />
            </div>
          )}
          {update.isError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Nao foi possivel atualizar o anuncio">
                {update.error instanceof Error ? update.error.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}

          <form
            className="mt-5 grid gap-4"
            onSubmit={(event) => {
              event.preventDefault();
              update.mutate();
            }}
          >
            <Field label="Titulo" value={form.title} onChange={(value) => setForm({ ...form, title: value })} />
            <label className="grid gap-1 text-sm">
              <span className="font-medium">Categoria</span>
              <select
                value={form.category}
                onChange={(event) => setForm({ ...form, category: event.target.value as ListingCategory | "" })}
                className="h-9 rounded-md border border-input bg-background px-3 text-sm"
              >
                <option value="">Manter atual</option>
                {marketplaceCategories.map((item) => (
                  <option key={item.value} value={item.value}>
                    {item.label}
                  </option>
                ))}
              </select>
            </label>
            <Field label="Preco em reais" type="number" value={form.price} onChange={(value) => setForm({ ...form, price: value })} />
            <Field
              label="Nos liberados para preview"
              value={form.previewNodeIds}
              onChange={(value) => setForm({ ...form, previewNodeIds: value })}
              placeholder="101, 102"
            />
            <label className="grid gap-1 text-sm">
              <span className="font-medium">Descricao</span>
              <textarea
                value={form.description}
                onChange={(event) => setForm({ ...form, description: event.target.value })}
                rows={7}
                className="rounded-md border border-input bg-background px-3 py-2 text-sm"
              />
            </label>
            <div className="flex justify-end">
              <Button type="submit" disabled={update.isPending || !Number.isFinite(numericListingId)}>
                {update.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                Salvar alteracoes
              </Button>
            </div>
          </form>
        </section>
      </div>
    </main>
  );
}

function Field({
  label,
  value,
  onChange,
  type = "text",
  placeholder,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  placeholder?: string;
}) {
  return (
    <label className="grid gap-1 text-sm">
      <span className="font-medium">{label}</span>
      <Input type={type} value={value} placeholder={placeholder} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}
