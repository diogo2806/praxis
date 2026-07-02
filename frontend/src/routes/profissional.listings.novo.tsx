import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ArrowLeft, Loader2, Save, Send } from "lucide-react";
import { useState } from "react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  createMarketplaceListing,
  listSimulations,
  submitMarketplaceListing,
  type CreateMarketplaceListingRequest,
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
    sourceVersionNumber: 1,
    title: "",
    description: "",
    category: "SELECAO" as ListingCategory,
    price: "",
    previewNodeIds: "",
  });

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });
  const publishedSimulations = (simulationsQuery.data ?? []).filter(
    (simulation) => simulation.status === "published",
  );

  const buildRequest = (): CreateMarketplaceListingRequest => ({
    sourceSimulationId: form.sourceSimulationId,
    sourceVersionNumber: form.sourceVersionNumber,
    title: form.title,
    description: form.description,
    category: form.category,
    priceCents: Math.round(Number(form.price.replace(",", ".")) * 100),
    previewNodeIds: splitList(form.previewNodeIds).map(Number).filter(Number.isFinite),
  });

  const saveDraft = useMutation({
    mutationFn: async () => createMarketplaceListing(buildRequest()),
  });

  const createAndSubmit = useMutation({
    mutationFn: async () => {
      const created = await createMarketplaceListing(buildRequest());
      return submitMarketplaceListing(created.id);
    },
  });

  const isSaving = saveDraft.isPending || createAndSubmit.isPending;

  const selectSimulation = (simulationId: string) => {
    const simulation = publishedSimulations.find((item) => item.id === simulationId);
    setForm({
      ...form,
      sourceSimulationId: simulationId,
      sourceVersionNumber: simulation?.versionNumber ?? 1,
      title: form.title || simulation?.name || "",
    });
  };

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
            Escolha uma avaliação publicada: o comprador recebe uma cópia editável dela no workspace
            da empresa.
          </p>

          {saveDraft.isSuccess && (
            <div className="mt-5">
              <StateBanner
                tone="ok"
                title="Rascunho salvo"
                action={
                  <Button asChild size="sm" variant="outline">
                    <Link to="/profissional/listings">Ver meus anúncios</Link>
                  </Button>
                }
              >
                O anúncio ficou disponível para edição antes do envio para revisão.
              </StateBanner>
            </div>
          )}
          {createAndSubmit.isSuccess && (
            <div className="mt-5">
              <StateBanner
                tone="ok"
                title="Anúncio enviado para revisão"
                action={
                  <Button asChild size="sm" variant="outline">
                    <Link to="/profissional/listings">Ver meus anúncios</Link>
                  </Button>
                }
              >
                A moderação precisa aprovar o anúncio antes de ele aparecer na vitrine.
              </StateBanner>
            </div>
          )}
          {(saveDraft.isError || createAndSubmit.isError) && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Não foi possível salvar o anúncio">
                {(saveDraft.error instanceof Error && saveDraft.error.message) ||
                  (createAndSubmit.error instanceof Error && createAndSubmit.error.message) ||
                  "Tente novamente."}
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
            <label className="grid gap-1 text-sm">
              <span className="font-medium">Avaliação publicada</span>
              {simulationsQuery.isLoading ? (
                <div className="flex items-center gap-2 rounded-md border border-input bg-background px-3 py-2 text-sm text-muted-foreground">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Carregando avaliações
                </div>
              ) : publishedSimulations.length > 0 ? (
                <select
                  value={form.sourceSimulationId}
                  required
                  onChange={(event) => selectSimulation(event.target.value)}
                  className="h-9 rounded-md border border-input bg-background px-3 text-sm"
                >
                  <option value="">Selecione a avaliação que será vendida</option>
                  {publishedSimulations.map((simulation) => (
                    <option key={simulation.id} value={simulation.id}>
                      {simulation.name} — v{simulation.versionNumber}
                    </option>
                  ))}
                </select>
              ) : (
                <StateBanner tone="warn" title="Nenhuma avaliação publicada">
                  Publique uma avaliação no seu workspace antes de criar o anúncio.{" "}
                  <Link to="/avaliacoes" className="font-medium underline">
                    Ver minhas avaliações
                  </Link>
                </StateBanner>
              )}
            </label>
            <Field
              label="Título do anúncio"
              value={form.title}
              onChange={(value) => setForm({ ...form, title: value })}
              required
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
              type="number"
              value={form.price}
              onChange={(value) => setForm({ ...form, price: value })}
              required
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
                required
                onChange={(event) => setForm({ ...form, description: event.target.value })}
                rows={7}
                className="rounded-md border border-input bg-background px-3 py-2 text-sm"
              />
            </label>
            <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
              <Button
                type="button"
                variant="outline"
                disabled={isSaving || !form.sourceSimulationId}
                onClick={() => saveDraft.mutate()}
              >
                {saveDraft.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Save className="h-4 w-4" />
                )}
                Salvar rascunho
              </Button>
              <Button type="submit" disabled={isSaving || !form.sourceSimulationId}>
                {createAndSubmit.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Send className="h-4 w-4" />
                )}
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
