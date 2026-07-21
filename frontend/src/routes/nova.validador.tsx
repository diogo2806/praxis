import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  AlertTriangle,
  CheckCircle2,
  GitBranch,
  ListChecks,
  RefreshCw,
  ShieldCheck,
  XCircle,
} from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  getSimulationValidation,
  listSimulations,
  type SimulationSummaryResponse,
  type ValidationIssueResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/nova/validador")({
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
      { title: "Revisão - Práxis" },
      {
        name: "description",
        content: "Diagnóstico estrutural com bloqueios, avisos e atalhos para os editores responsáveis.",
      },
    ],
  }),
  component: ValidatorPage,
});

function ValidatorPage() {
  const search = Route.useSearch();
  const hasContext = Boolean(search.simulationId && search.versionNumber);

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });
  const validationQuery = useQuery({
    queryKey: ["simulation-validation", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationValidation(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
    retry: false,
  });

  const simulation = findSimulation(
    simulationsQuery.data ?? [],
    search.simulationId,
    search.versionNumber,
  );
  const validation = validationQuery.data;
  const blockers = validation?.issues.filter((issue) => issue.severity === "blocker") ?? [];
  const warnings = validation?.issues.filter((issue) => issue.severity === "warning") ?? [];

  return (
    <AppShell>
      <main className="mx-auto max-w-6xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Validação estrutural
            </div>
            <h1 className="mt-1 font-display text-3xl">Revisão e prontidão</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Esta tela somente diagnostica. Conteúdo, alternativas e pontuação são corrigidos no
              Editor de diálogo; posição e conexões são corrigidas no Mapa.
            </p>
          </div>

          {hasContext && (
            <div className="flex flex-wrap gap-2">
              <Button asChild variant="outline">
                <Link
                  to="/nova/dialogo"
                  search={{
                    simulationId: search.simulationId,
                    versionNumber: search.versionNumber,
                  }}
                >
                  <ListChecks className="mr-2 h-4 w-4" />
                  Abrir Diálogo
                </Link>
              </Button>
              <Button asChild variant="outline">
                <Link
                  to="/nova/mapa"
                  search={{
                    simulationId: search.simulationId,
                    versionNumber: search.versionNumber,
                  }}
                >
                  <GitBranch className="mr-2 h-4 w-4" />
                  Abrir Mapa
                </Link>
              </Button>
              <Button
                variant="outline"
                onClick={() => void validationQuery.refetch()}
                disabled={validationQuery.isFetching}
              >
                <RefreshCw
                  className={cn("mr-2 h-4 w-4", validationQuery.isFetching && "animate-spin")}
                />
                Validar novamente
              </Button>
            </div>
          )}
        </header>

        {!hasContext ? (
          <SimulationSelector
            simulations={simulationsQuery.data ?? []}
            loading={simulationsQuery.isLoading}
            error={simulationsQuery.error}
          />
        ) : validationQuery.isError ? (
          <StateBanner tone="danger" title="Não foi possível validar esta versão">
            {validationQuery.error instanceof Error
              ? validationQuery.error.message
              : "Tente novamente."}
          </StateBanner>
        ) : validationQuery.isLoading || !validation ? (
          <section className="rounded-xl border border-border bg-card px-5 py-12 text-center text-sm text-muted-foreground">
            Executando diagnóstico estrutural...
          </section>
        ) : (
          <>
            <section className="rounded-xl border border-border bg-card p-5">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="text-lg font-semibold">
                    {simulation?.name ?? validation.simulationId} · versão {validation.versionNumber}
                  </h2>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Resultado calculado pelas regras atuais de publicação.
                  </p>
                </div>
                {simulation && (
                  <StatusBadge
                    status={simulation.status}
                    maturity={maturityForStatus(simulation.status)}
                  />
                )}
              </div>

              <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <MetricCard
                  label="Prontidão"
                  value={validation.publishable ? "Publicável" : "Bloqueada"}
                  tone={validation.publishable ? "ok" : "danger"}
                />
                <MetricCard label="Bloqueios" value={String(validation.blockerCount)} tone="danger" />
                <MetricCard label="Avisos" value={String(validation.warningCount)} tone="warning" />
                <MetricCard label="Qualidade" value={`${validation.qualityScore}%`} tone="neutral" />
              </div>
            </section>

            {validation.issues.length === 0 ? (
              <StateBanner tone="ok" title="Nenhum bloqueio ou aviso encontrado">
                Esta versão está estruturalmente pronta. Confirme termos e publicação em Governança.
              </StateBanner>
            ) : (
              <section className="space-y-4">
                <DiagnosticGroup
                  title="Bloqueios de publicação"
                  description="Precisam ser resolvidos antes de publicar."
                  issues={blockers}
                  tone="danger"
                  simulationId={validation.simulationId}
                  versionNumber={validation.versionNumber}
                />
                <DiagnosticGroup
                  title="Avisos"
                  description="Não impedem necessariamente a publicação, mas exigem revisão."
                  issues={warnings}
                  tone="warning"
                  simulationId={validation.simulationId}
                  versionNumber={validation.versionNumber}
                />
              </section>
            )}

            <section className="rounded-xl border border-border bg-card p-5">
              <div className="flex items-start gap-3">
                <ShieldCheck className="mt-0.5 h-5 w-5 text-primary" />
                <div className="min-w-0 flex-1">
                  <h2 className="font-semibold">Próximas responsabilidades</h2>
                  <p className="mt-1 text-sm leading-6 text-muted-foreground">
                    O Validador não publica e não mantém auditoria ou privacidade. Essas responsabilidades
                    permanecem nas telas proprietárias.
                  </p>
                  <div className="mt-4 flex flex-wrap gap-2">
                    <Button asChild>
                      <Link
                        to="/nova/governanca"
                        search={{
                          simulationId: validation.simulationId,
                          versionNumber: validation.versionNumber,
                        }}
                      >
                        Governança e publicação
                      </Link>
                    </Button>
                    <Button asChild variant="outline">
                      <Link
                        to="/compliance"
                        search={{
                          simulationId: validation.simulationId,
                          versionNumber: validation.versionNumber,
                        }}
                      >
                        Ver conformidade contextual
                      </Link>
                    </Button>
                  </div>
                </div>
              </div>
            </section>
          </>
        )}
      </main>
    </AppShell>
  );
}

function DiagnosticGroup({
  title,
  description,
  issues,
  tone,
  simulationId,
  versionNumber,
}: {
  title: string;
  description: string;
  issues: ValidationIssueResponse[];
  tone: "danger" | "warning";
  simulationId: string;
  versionNumber: number;
}) {
  if (issues.length === 0) return null;

  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-start gap-3">
        {tone === "danger" ? (
          <XCircle className="mt-0.5 h-5 w-5 text-danger" />
        ) : (
          <AlertTriangle className="mt-0.5 h-5 w-5 text-warning-foreground" />
        )}
        <div>
          <h2 className="font-semibold">{title}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{description}</p>
        </div>
      </div>

      <div className="mt-4 space-y-3">
        {issues.map((issue, index) => (
          <article
            key={`${issue.severity}-${issue.nodeId ?? "global"}-${index}`}
            className="rounded-lg border border-border bg-background p-4"
          >
            <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
              <div>
                <div className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  {issue.nodeId ? `Etapa ${issue.nodeId}` : "Regra global"}
                </div>
                <p className="mt-1 text-sm leading-6">{issue.message}</p>
              </div>
              <div className="flex shrink-0 flex-wrap gap-2">
                <Button asChild size="sm">
                  <Link
                    to="/nova/dialogo"
                    search={{ simulationId, versionNumber }}
                  >
                    Corrigir no Diálogo
                  </Link>
                </Button>
                <Button asChild size="sm" variant="outline">
                  <Link
                    to="/nova/mapa"
                    search={{ simulationId, versionNumber }}
                  >
                    Abrir no Mapa
                  </Link>
                </Button>
              </div>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function MetricCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone: "ok" | "danger" | "warning" | "neutral";
}) {
  const classes = {
    ok: "border-success/30 bg-success/10 text-success",
    danger: "border-danger/30 bg-danger/10 text-danger",
    warning: "border-warning/40 bg-warning/10 text-warning-foreground",
    neutral: "border-border bg-muted/40 text-foreground",
  }[tone];

  return (
    <article className={cn("rounded-lg border p-4", classes)}>
      <div className="text-xs font-semibold uppercase tracking-wide opacity-80">{label}</div>
      <div className="mt-2 text-2xl font-semibold">{value}</div>
    </article>
  );
}

function SimulationSelector({
  simulations,
  loading,
  error,
}: {
  simulations: SimulationSummaryResponse[];
  loading: boolean;
  error: unknown;
}) {
  if (error) {
    return (
      <StateBanner tone="danger" title="Não foi possível carregar as avaliações">
        {error instanceof Error ? error.message : "Tente novamente."}
      </StateBanner>
    );
  }
  if (loading) {
    return (
      <section className="rounded-xl border border-border bg-card px-5 py-12 text-center text-sm text-muted-foreground">
        Carregando avaliações...
      </section>
    );
  }
  if (simulations.length === 0) {
    return (
      <EmptyState
        title="Nenhuma avaliação disponível"
        description="Crie uma avaliação antes de executar o diagnóstico."
        actions={
          <Button asChild>
            <Link to="/nova/avaliacao">Criar avaliação</Link>
          </Button>
        }
      />
    );
  }

  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-center gap-2">
        <CheckCircle2 className="h-5 w-5 text-primary" />
        <h2 className="font-semibold">Escolha uma avaliação para validar</h2>
      </div>
      <div className="mt-4 grid gap-3 md:grid-cols-2">
        {simulations.map((simulation) => (
          <Link
            key={`${simulation.id}-${simulation.versionNumber}`}
            to="/nova/validador"
            search={{
              simulationId: simulation.id,
              versionNumber: simulation.versionNumber,
            }}
            className="rounded-lg border border-border bg-background p-4 transition hover:border-primary/40 hover:bg-accent"
          >
            <div className="font-medium">{simulation.name}</div>
            <div className="mt-1 text-xs text-muted-foreground">
              Versão {simulation.versionNumber}
            </div>
            <div className="mt-3">
              <StatusBadge
                status={simulation.status}
                maturity={maturityForStatus(simulation.status)}
              />
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}

function findSimulation(
  simulations: SimulationSummaryResponse[],
  simulationId?: string,
  versionNumber?: number,
) {
  return simulations.find(
    (simulation) =>
      simulation.id === simulationId &&
      (versionNumber == null || simulation.versionNumber === versionNumber),
  );
}
