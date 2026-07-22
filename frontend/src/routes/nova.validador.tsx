import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  AlertTriangle,
  CheckCircle2,
  GitBranch,
  ListChecks,
  RefreshCw,
  ShieldCheck,
  Target,
  XCircle,
} from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  getSimulationValidation,
  getSimulationVersion,
  listSimulations,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";
import { cn } from "@/lib/utils";
import {
  buildValidationDiagnostics,
  type ValidationDiagnostic,
  type ValidationEditor,
} from "@/lib/validation-diagnostics";

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
        content: "Diagnóstico estrutural com localização exata, orientação e atalhos de correção.",
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
  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
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
  const diagnostics = buildValidationDiagnostics(versionQuery.data, validation?.issues ?? []);
  const blockers = diagnostics.filter((diagnostic) => diagnostic.issue.severity === "blocker");
  const warnings = diagnostics.filter((diagnostic) => diagnostic.issue.severity === "warning");
  const loading = validationQuery.isLoading || versionQuery.isLoading;
  const pageError = validationQuery.error ?? versionQuery.error;

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
              Cada diagnóstico informa a etapa, a resposta ou campo afetado, o que precisa ser
              alterado e abre diretamente o local correto para a correção.
            </p>
          </div>

          {hasContext && (
            <div className="flex flex-wrap gap-2">
              <Button asChild variant="outline">
                <Link
                  to="/nova/dialogo"
                  search={{
                    simulationId: search.simulationId,
                    nodeId: undefined,
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
                    nodeId: undefined,
                    versionNumber: search.versionNumber,
                  }}
                >
                  <GitBranch className="mr-2 h-4 w-4" />
                  Abrir Mapa
                </Link>
              </Button>
              <Button
                variant="outline"
                onClick={() => {
                  void Promise.all([validationQuery.refetch(), versionQuery.refetch()]);
                }}
                disabled={validationQuery.isFetching || versionQuery.isFetching}
              >
                <RefreshCw
                  className={cn(
                    "mr-2 h-4 w-4",
                    (validationQuery.isFetching || versionQuery.isFetching) && "animate-spin",
                  )}
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
        ) : pageError ? (
          <StateBanner tone="danger" title="Não foi possível validar esta versão">
            {pageError instanceof Error ? pageError.message : "Tente novamente."}
          </StateBanner>
        ) : loading || !validation ? (
          <section className="rounded-xl border border-border bg-card px-5 py-12 text-center text-sm text-muted-foreground">
            Executando diagnóstico estrutural...
          </section>
        ) : (
          <>
            <section className="rounded-xl border border-border bg-card p-5">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="text-lg font-semibold">
                    {simulation?.name ?? validation.simulationId} · versão{" "}
                    {validation.versionNumber}
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
                <MetricCard
                  label="Bloqueios"
                  value={String(validation.blockerCount)}
                  tone="danger"
                />
                <MetricCard label="Avisos" value={String(validation.warningCount)} tone="warning" />
                <MetricCard
                  label="Qualidade"
                  value={`${validation.qualityScore}%`}
                  tone="neutral"
                />
              </div>
            </section>

            {diagnostics.length === 0 ? (
              <StateBanner tone="ok" title="Nenhum bloqueio ou aviso encontrado">
                Esta versão está estruturalmente pronta. Confirme termos e publicação em Governança.
              </StateBanner>
            ) : (
              <section className="space-y-4">
                <DiagnosticGroup
                  title="Bloqueios de publicação"
                  description="Resolva estes itens antes de publicar. Os campos afetados estão destacados abaixo."
                  diagnostics={blockers}
                  tone="danger"
                  simulationId={validation.simulationId}
                  versionNumber={validation.versionNumber}
                />
                <DiagnosticGroup
                  title="Avisos"
                  description="Não impedem necessariamente a publicação, mas exigem revisão."
                  diagnostics={warnings}
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
                    O Validador diagnostica a estrutura. Termos, auditoria, privacidade e publicação
                    permanecem nas telas responsáveis por essas etapas.
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
  diagnostics,
  tone,
  simulationId,
  versionNumber,
}: {
  title: string;
  description: string;
  diagnostics: ValidationDiagnostic[];
  tone: "danger" | "warning";
  simulationId: string;
  versionNumber: number;
}) {
  if (diagnostics.length === 0) return null;

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
        {diagnostics.map((diagnostic, index) => (
          <DiagnosticCard
            key={`${diagnostic.issue.severity}-${diagnostic.nodeId ?? "global"}-${diagnostic.optionId ?? index}-${index}`}
            diagnostic={diagnostic}
            tone={tone}
            simulationId={simulationId}
            versionNumber={versionNumber}
          />
        ))}
      </div>
    </section>
  );
}

function DiagnosticCard({
  diagnostic,
  tone,
  simulationId,
  versionNumber,
}: {
  diagnostic: ValidationDiagnostic;
  tone: "danger" | "warning";
  simulationId: string;
  versionNumber: number;
}) {
  const location = [diagnostic.nodeLabel, diagnostic.optionLabel, diagnostic.fieldLabel]
    .filter(Boolean)
    .join(" · ");
  const primaryEditor = diagnostic.editor;

  return (
    <article
      className={cn(
        "rounded-lg border-2 p-4 shadow-sm",
        tone === "danger" ? "border-danger/50 bg-danger/5" : "border-warning/50 bg-warning/5",
      )}
    >
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span
              className={cn(
                "inline-flex rounded-md px-2 py-1 text-[11px] font-semibold uppercase tracking-wide",
                tone === "danger"
                  ? "bg-danger text-danger-foreground"
                  : "bg-warning text-warning-foreground",
              )}
            >
              {tone === "danger" ? "Bloqueio" : "Aviso"}
            </span>
            <span className="inline-flex items-center gap-1 rounded-md border border-border bg-background px-2 py-1 text-xs font-semibold">
              <Target className="h-3.5 w-3.5 text-primary" />
              {location}
            </span>
          </div>

          {diagnostic.optionText && (
            <div className="mt-3 rounded-md border border-border bg-background p-3">
              <div className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
                Resposta afetada
              </div>
              <p className="mt-1 text-sm font-medium">{diagnostic.optionText}</p>
            </div>
          )}

          <div className="mt-3 grid gap-3 md:grid-cols-2">
            <div>
              <div className="text-xs font-semibold text-foreground">Problema identificado</div>
              <p className="mt-1 text-sm leading-6 text-muted-foreground">
                {diagnostic.issue.message}
              </p>
            </div>
            <div className="rounded-md border border-primary/25 bg-primary/5 p-3">
              <div className="text-xs font-semibold text-foreground">Como resolver</div>
              <p className="mt-1 text-sm leading-6 text-muted-foreground">
                {diagnostic.resolution}
              </p>
            </div>
          </div>
        </div>

        <div className="flex shrink-0 flex-wrap gap-2 lg:w-48 lg:flex-col">
          <EditorLink
            editor={primaryEditor}
            simulationId={simulationId}
            versionNumber={versionNumber}
            nodeId={diagnostic.nodeId}
            primary
          />
          {primaryEditor !== "dialogo" && (
            <EditorLink
              editor="dialogo"
              simulationId={simulationId}
              versionNumber={versionNumber}
              nodeId={diagnostic.nodeId}
            />
          )}
          {primaryEditor !== "mapa" && diagnostic.nodeId && (
            <EditorLink
              editor="mapa"
              simulationId={simulationId}
              versionNumber={versionNumber}
              nodeId={diagnostic.nodeId}
            />
          )}
        </div>
      </div>
    </article>
  );
}

function EditorLink({
  editor,
  simulationId,
  versionNumber,
  nodeId,
  primary = false,
}: {
  editor: ValidationEditor;
  simulationId: string;
  versionNumber: number;
  nodeId: string | null;
  primary?: boolean;
}) {
  const route =
    editor === "avaliacao" ? "/nova/avaliacao" : editor === "mapa" ? "/nova/mapa" : "/nova/dialogo";
  const label =
    editor === "avaliacao"
      ? "Corrigir na Avaliação"
      : editor === "mapa"
        ? "Corrigir no Mapa"
        : "Corrigir no Diálogo";
  const Icon = editor === "mapa" ? GitBranch : ListChecks;

  return (
    <Button asChild size="sm" variant={primary ? "default" : "outline"} className="justify-start">
      <Link
        to={route}
        search={{
          simulationId,
          versionNumber,
          nodeId: nodeId ?? undefined,
        }}
      >
        <Icon className="mr-2 h-4 w-4" />
        {label}
      </Link>
    </Button>
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
            <Link
              to="/nova/avaliacao"
              search={{ simulationId: undefined, versionNumber: undefined }}
            >
              Criar avaliação
            </Link>
          </Button>
        }
      />
    );
  }

  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-center gap-2">
        <CheckCircle2 className="h-5 w-5 text-primary" />
        <div>
          <h2 className="font-semibold">Escolha a avaliação para validar</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            O diagnóstico será executado sobre a versão indicada em cada item.
          </p>
        </div>
      </div>
      <div className="mt-4 grid gap-3 md:grid-cols-2">
        {simulations.map((simulation) => (
          <Link
            key={`${simulation.id}-${simulation.versionNumber}`}
            to="/nova/validador"
            search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
            className="rounded-lg border border-border bg-background p-4 transition hover:border-primary hover:bg-primary/5"
          >
            <div className="font-semibold">{simulation.name}</div>
            <div className="mt-1 text-xs text-muted-foreground">
              Versão {simulation.versionNumber} · {simulation.status}
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}

function findSimulation(
  simulations: SimulationSummaryResponse[],
  simulationId: string | undefined,
  versionNumber: number | undefined,
) {
  return simulations.find(
    (simulation) =>
      simulation.id === simulationId &&
      (versionNumber == null || simulation.versionNumber === versionNumber),
  );
}
