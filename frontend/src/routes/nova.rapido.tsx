import { useMutation, useQuery } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { ArrowRight, Sparkles } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import {
  createFromQuickStart,
  getQuickStartTemplates,
  type QuickStartCategory,
  type QuickStartTemplateSummaryResponse,
} from "@/lib/api/praxis";

const QUICK_START_DESTINATION = "/nova/validador" as const;

export const Route = createFileRoute("/nova/rapido")({
  head: () => ({
    meta: [
      { title: "Começar rápido - Práxis" },
      {
        name: "description",
        content: "Crie uma avaliação a partir de um modelo pronto e siga direto para a revisão.",
      },
    ],
  }),
  component: QuickStartPage,
});

function QuickStartPage() {
  const navigate = useNavigate();
  const templatesQuery = useQuery({
    queryKey: ["quick-start-templates"],
    queryFn: getQuickStartTemplates,
  });
  const createMutation = useMutation({
    mutationFn: (category: QuickStartCategory) => createFromQuickStart(category),
    onSuccess: (created) => {
      void navigate({
        to: QUICK_START_DESTINATION,
        search: {
          simulationId: created.simulationId,
          versionNumber: created.versionNumber,
        },
      });
    },
  });

  return (
    <AppShell>
      <div className="mb-6">
        <div className="inline-flex items-center gap-1.5 text-xs uppercase tracking-[0.2em] text-primary">
          <Sparkles className="h-3.5 w-3.5" />
          Começar rápido
        </div>
        <h1 className="mt-1 font-display text-3xl">Escolha um modelo para revisar</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Cada modelo já vem com cenário, alternativas, competências e pesos preenchidos. Depois da
          criação, você segue direto para a revisão estrutural antes de publicar.
        </p>
      </div>

      {createMutation.isError && (
        <StateBanner tone="danger" title="Não foi possível criar a partir do modelo">
          {createMutation.error instanceof Error ? createMutation.error.message : "Tente novamente."}
        </StateBanner>
      )}

      {templatesQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando modelos">
          Buscando os modelos prontos disponíveis.
        </StateBanner>
      ) : templatesQuery.isError ? (
        <StateBanner tone="danger" title="Não foi possível carregar os modelos">
          {templatesQuery.error instanceof Error
            ? templatesQuery.error.message
            : "Tente novamente."}
        </StateBanner>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {(templatesQuery.data ?? []).map((template) => (
            <TemplateCard
              key={template.category}
              template={template}
              pending={createMutation.isPending && createMutation.variables === template.category}
              disabled={createMutation.isPending}
              onUse={() => createMutation.mutate(template.category)}
            />
          ))}
        </div>
      )}

      <div className="mt-8 text-sm text-muted-foreground">
        Prefere montar do zero?{" "}
        <Link to="/nova/avaliacao" className="font-medium text-primary hover:underline">
          Criar do início →
        </Link>
      </div>
    </AppShell>
  );
}

function TemplateCard({
  template,
  pending,
  disabled,
  onUse,
}: {
  template: QuickStartTemplateSummaryResponse;
  pending: boolean;
  disabled: boolean;
  onUse: () => void;
}) {
  return (
    <article className="flex flex-col rounded-md border border-border bg-card p-5">
      <h2 className="text-base font-semibold">{template.title}</h2>
      <p className="mt-1 flex-1 text-sm text-muted-foreground">{template.description}</p>
      <div className="mt-3 text-xs text-muted-foreground">
        {template.nodeCount} cenário{template.nodeCount === 1 ? "" : "s"}
      </div>
      <button
        type="button"
        onClick={onUse}
        disabled={disabled}
        className="mt-4 inline-flex items-center justify-center gap-1.5 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {pending ? "Criando..." : "Usar este"}
        {!pending && <ArrowRight className="h-4 w-4" />}
      </button>
    </article>
  );
}
