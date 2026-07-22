import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { CheckCircle2, XCircle } from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  getGupyPreflight,
  listSimulations,
  type GupyPreflightResponse,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";

export const Route = createFileRoute("/nova/gupy")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number" && Number.isFinite(search.versionNumber)
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
        <Button asChild variant="outline" className="bg-card">
          <Link to="/integrations/$provider" params={{ provider: "gupy" }}>
            Configurar integração
          </Link>
        </Button>
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
            <h2 className="text-sm font-semibold">Falhas e entregas operacionais</h2>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
              Esta tela valida somente a prontidão técnica da versão. Entregas com falha,
              retentativas e itens em DLQ são tratados exclusivamente na Central operacional.
            </p>
            <Button asChild variant="outline" className="mt-4 bg-card">
              <Link to="/monitoramento">Abrir Central operacional</Link>
            </Button>
          </section>

          <div className="flex flex-wrap justify-between gap-3">
            <Button asChild variant="outline" className="bg-card">
              <Link
                to="/nova/governanca"
                search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              >
                Voltar para publicação
              </Link>
            </Button>
            <Button asChild>
              <Link to="/integrations/$provider" params={{ provider: "gupy" }}>
                Abrir configuração Gupy
              </Link>
            </Button>
          </div>
        </div>
      )}
    </AppShell>
  );
}

function PreflightCheck({ item }: { item: GupyPreflightResponse["checks"][number] }) {
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
      <Button asChild variant="outline" className="bg-card">
        <Link to="/avaliacoes">Revisar e publicar uma avaliação</Link>
      </Button>
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
            <StatusBadge
              status={simulation.status}
              maturity={maturityForStatus(simulation.status)}
            />
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
