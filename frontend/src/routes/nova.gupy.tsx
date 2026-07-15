import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, RefreshCw, Send, XCircle } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  getGupyPreflight,
  listResultDeliveries,
  listSimulations,
  type GupyPreflightCheckResponse,
  type ResultDeliveryResponse,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";

export const Route = createFileRoute("/nova/gupy")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Ativação Gupy - Práxis" },
      {
        name: "description",
        content: "Conferência técnica de uma versão publicada antes da ativação no catálogo Gupy.",
      },
    ],
  }),
  component: GupyActivation,
});

function GupyActivation() {
  const search = Route.useSearch();
  const hasParams = Boolean(search.simulationId && search.versionNumber);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasParams,
  });
  const preflightQuery = useQuery({
    queryKey: ["gupy-preflight", search.simulationId, search.versionNumber],
    queryFn: () => getGupyPreflight(search.simulationId!, search.versionNumber!),
    enabled: hasParams,
  });
  const deliveriesQuery = useQuery({
    queryKey: ["result-deliveries", search.simulationId, search.versionNumber],
    queryFn: () =>
      listResultDeliveries({
        simulationId: search.simulationId,
        versionNumber: search.versionNumber,
      }),
    enabled: hasParams,
  });
  const hasBlocker = preflightQuery.data?.checks.some((item) => item.status === "blocker") ?? false;

  return (
    <AppShell>
      <WizardStepper current="publicacao" />
      <header className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
            Ativação no ATS
          </div>
          <h1 className="mt-1 font-display text-3xl">Gupy — ativação e conferência</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
            Primeiro publique a versão no Práxis. Depois execute esta conferência para validar o
            token real da integração, a URL pública e a estrutura que será exposta à Gupy.
          </p>
        </div>
        <Link
          to="/integrations/$provider"
          params={{ provider: "gupy" }}
          className="rounded-md border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-accent"
        >
          Configurar integração
        </Link>
      </header>

      {!hasParams ? (
        <EmptyState
          title="Selecione uma versão publicada"
          description="Rascunhos precisam ser revisados e publicados antes da ativação na Gupy."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={(simulationsQuery.data ?? []).filter(
                (simulation) => simulation.status === "published",
              )}
            />
          }
        />
      ) : (
        <div className="space-y-5">
          {preflightQuery.isLoading && (
            <StateBanner tone="info" title="Executando conferência">
              Validando {search.simulationId} v{search.versionNumber} com as configurações atuais.
            </StateBanner>
          )}

          {preflightQuery.isError && (
            <StateBanner tone="danger" title="Não foi possível executar a conferência">
              {preflightQuery.error instanceof Error
                ? preflightQuery.error.message
                : "Verifique a configuração e tente novamente."}
            </StateBanner>
          )}

          {preflightQuery.data && (
            <StateBanner
              tone={hasBlocker ? "danger" : "ok"}
              title={hasBlocker ? "Ativação bloqueada" : "Pronta para ativação"}
            >
              {hasBlocker
                ? "Corrija todos os bloqueios antes de vincular esta versão no catálogo da Gupy."
                : "A versão publicada passou pelas verificações técnicas disponíveis no Práxis."}
            </StateBanner>
          )}

          <section className="rounded-xl border border-border bg-card p-5">
            <h2 className="text-sm font-semibold">Lista de verificação</h2>
            <div className="mt-4 space-y-3">
              {(preflightQuery.data?.checks ?? []).map((item) => (
                <PreflightCheck key={item.code} item={item} />
              ))}
              {!preflightQuery.isLoading && !preflightQuery.data && !preflightQuery.isError && (
                <p className="text-sm text-muted-foreground">Nenhuma verificação disponível.</p>
              )}
            </div>
          </section>

          <section className="rounded-xl border border-border bg-card p-5">
            <div className="mb-4 flex items-center gap-2 text-sm font-semibold">
              <Send className="h-4 w-4" />
              Entregas de resultado desta versão
            </div>
            <DeliveryList
              deliveries={deliveriesQuery.data ?? []}
              loading={deliveriesQuery.isLoading}
              error={deliveriesQuery.isError}
            />
          </section>

          <div className="flex flex-wrap justify-between gap-3">
            <Link
              to="/nova/governanca"
              search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar para publicação
            </Link>
            <Link
              to="/integrations/$provider"
              params={{ provider: "gupy" }}
              className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Abrir configuração Gupy
            </Link>
          </div>
        </div>
      )}
    </AppShell>
  );
}

function PreflightCheck({ item }: { item: GupyPreflightCheckResponse }) {
  const ok = item.status === "ok";
  const Icon = ok ? CheckCircle2 : XCircle;
  return (
    <div
      className={`flex items-start gap-3 rounded-md border p-3 ${
        ok ? "border-success/20 bg-success/5" : "border-danger/30 bg-danger/5"
      }`}
    >
      <Icon className={`mt-0.5 h-4 w-4 ${ok ? "text-success" : "text-danger"}`} />
      <div>
        <div className="text-sm font-medium">{formatCheckCode(item.code)}</div>
        <div className="text-xs leading-5 text-muted-foreground">{item.message}</div>
      </div>
    </div>
  );
}

function DeliveryList({
  deliveries,
  loading,
  error,
}: {
  deliveries: ResultDeliveryResponse[];
  loading: boolean;
  error: boolean;
}) {
  if (loading) {
    return <div className="text-sm text-muted-foreground">Carregando entregas...</div>;
  }
  if (error) {
    return <div className="text-sm text-danger">Não foi possível carregar as entregas.</div>;
  }
  if (deliveries.length === 0) {
    return (
      <div className="rounded-md border border-border bg-background p-3 text-sm text-muted-foreground">
        Nenhuma entrega registrada para esta versão.
      </div>
    );
  }
  return (
    <div className="space-y-3">
      {deliveries.slice(0, 10).map((delivery) => (
        <div key={delivery.id} className="rounded-md border border-border bg-background p-3">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <div className="truncate text-sm font-medium">{delivery.resultId}</div>
              <div className="truncate text-xs text-muted-foreground">{delivery.attemptId}</div>
            </div>
            <span className="rounded-md border border-border bg-card px-2 py-1 text-[11px]">
              {delivery.status}
            </span>
          </div>
          {delivery.status === "retrying" && (
            <div className="mt-2 inline-flex items-center gap-2 text-xs text-muted-foreground">
              <RefreshCw className="h-3.5 w-3.5" />
              próxima tentativa {formatDateTime(delivery.nextAttemptAt)}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function SimulationLinks({
  simulations,
  loading,
}: {
  simulations: SimulationSummaryResponse[];
  loading: boolean;
}) {
  if (loading) {
    return <div className="text-sm text-muted-foreground">Carregando avaliações...</div>;
  }
  if (simulations.length === 0) {
    return (
      <Link
        to="/avaliacoes"
        className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
      >
        Revisar e publicar uma avaliação
      </Link>
    );
  }
  return (
    <div className="flex flex-wrap gap-3">
      {simulations.map((simulation) => (
        <Link
          key={`${simulation.id}-${simulation.versionNumber}`}
          to="/nova/gupy"
          search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
          className="rounded-md border border-border bg-card px-4 py-3 text-left text-sm hover:bg-accent"
        >
          <span className="block font-medium">{simulation.name}</span>
          <span className="mt-1 block">
            <StatusBadge status={simulation.status} maturity={maturityForStatus(simulation.status)} />
          </span>
        </Link>
      ))}
    </div>
  );
}

function formatCheckCode(value: string) {
  return value
    .replace(/([A-Z])/g, " $1")
    .trim()
    .replace(/^./, (letter) => letter.toUpperCase());
}

function formatDateTime(value: string | null) {
  if (!value) return "sem data";
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
