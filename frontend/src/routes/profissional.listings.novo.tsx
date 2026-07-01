import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation } from "@tanstack/react-query";
import { ArrowLeft, Loader2, Send } from "lucide-react";
import { useState } from "react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  createMarketplaceListing,
  submitMarketplaceListing,
  type ListingCategory,
} from "@/lib/api/praxis";
import { marketplaceCategories, splitList } from "@/lib/marketplace";

export const Route = createFileRoute("/profissional/listings/novo")({
  head: () => ({
    meta: [{ title: "Novo anúncio - Marketplace Práxis" }],
  }),
  component: NewProfessionalListingPage,
});

function NewProfessionalListingPage() {
  const [form, setForm] = useState({
    sourceSimulationId: "",
    sourceVersionNumber: "1",
    title: "",
    description: "",
    category: "SELECAO" as ListingCategory,
    price: "",
    previewNodeIds: "",
  });

  const createAndSubmit = useMutation({
    mutationFn: async () => {
      const created = await createMarketplaceListing({
        sourceSimulationId: form.sourceSimulationId,
        sourceVersionNumber: Number(form.sourceVersionNumber),
        title: form.title,
        description: form.description,
        category: form.category,
        priceCents: Math.round(Number(form.price.replace(",", ".")) * 100),
        previewNodeIds: splitList(form.previewNodeIds).map(Number).filter(Number.isFinite),
      });
      return submitMarketplaceListing(created.id);
    },
  });

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-4xl px-5 py-6">
        <Button asChild variant="ghost" size="sm" className="mb-4">
          <Link to="/profissional">
            <ArrowLeft className="h-4 w-4" />
            Área do profissional
          </Link>
        </Button>

        <section className="rounded-md border border-border bg-card p-5">
          <div className="text-xs uppercase text-primary">Anúncios</div>
          <h1 className="mt-1 text-2xl font-semibold">Novo anúncio</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            A simulação precisa estar publicada para ser clonada ao comprador.
          </p>

          {createAndSubmit.isSuccess && (
            <div className="mt-5">
              <StateBanner tone="ok" title="Anúncio enviado para revisão">
                A moderação precisa aprovar o anúncio antes de ele aparecer na vitrine.
              </StateBanner>
            </div>
          )}
          {createAndSubmit.isError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Não foi possível enviar o anúncio">
                {createAndSubmit.error instanceof Error ? createAndSubmit.error.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}

          <form
            className="mt-5 grid gap-4"
            onSubmit={(event) => {
              event.preventDefault();
              createAndSubmit.mutate();
            }}
          >
            <div className="grid gap-4 md:grid-cols-2">
              <Field
                label="ID da simulação"
                value={form.sourceSimulationId}
                onChange={(value) => setForm({ ...form, sourceSimulationId: value })}
                required
              />
              <Field
                label="Número da versão"
                type="number"
                value={form.sourceVersionNumber}
                onChange={(value) => setForm({ ...form, sourceVersionNumber: value })}
                required
              />
            </div>
            <Field label="Título" value={form.title} onChange={(value) => setForm({ ...form, title: value })} required />
            <label className="grid gap-1 text-sm">
              <span className="font-medium">Categoria</span>
              <select
                value={form.category}
                onChange={(event) => setForm({ ...form, category: event.target.value as ListingCategory })}
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
              type="number"
              value={form.price}
              onChange={(value) => setForm({ ...form, price: value })}
              required
            />
            <Field
              label="Nós liberados para preview"
              value={form.previewNodeIds}
              onChange={(value) => setForm({ ...form, previewNodeIds: value })}
              placeholder="101, 102"
            />
            <label className="grid gap-1 text-sm">
              <span className="font-medium">Descrição</span>
              <textarea
                value={form.description}
                required
                onChange={(event) => setForm({ ...form, description: event.target.value })}
                rows={7}
                className="rounded-md border border-input bg-background px-3 py-2 text-sm"
              />
            </label>
            <div className="flex justify-end">
              <Button type="submit" disabled={createAndSubmit.isPending}>
                {createAndSubmit.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                Enviar para revisão
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
  required,
  placeholder,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  required?: boolean;
  placeholder?: string;
}) {
  return (
    <label className="grid gap-1 text-sm">
      <span className="font-medium">{label}</span>
      <Input
        type={type}
        value={value}
        required={required}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}

