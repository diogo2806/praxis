import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, CheckCircle2, ExternalLink, XCircle } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import {
  EmptyState,
  NextStepContract,
  ScreenStateStrip,
  StateBanner,
  StatusBadge,
} from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  getSimulationValidation,
  listSimulations,
  type SimulationSummaryResponse,
  type SimulationValidationResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/nova/validador")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
        ? Number(search.versionNumber)
        : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Validador de Qualidade - Praxis" },
      {
        name: "description",
        content: "Diagnostico deterministico com bloqueios de publicacao.",
      },
    ],
  }),
  component: ValidatorPage,
});

type CheckTone = "ok" | "warn" | "danger";

interface ValidationCheck {
  id: string;
  tone: CheckTone;
  text: string;
  target: string;
}

function ValidatorPage() {
  const search = Route.useSearch();
  const hasValidationParams = Boolean(search.simulationId && search.versionNumber);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasValidationParams,
  });
  const validationQuery = useQuery({
    queryKey: ["simulation-validation", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationValidation(search.simulationId!, search.versionNumber!),
    enabled: hasValidationParams,
  });

  const activeChecks = validationQuery.data ? mapValidationIssues(validationQuery.data) : [];
  const blockers = validationQuery.data?.blockerCount ?? 0;
  const warnings = validationQuery.data?.warningCount ?? 0;
  const qualityScore = validationQuery.data?.qualityScore ?? 0;

  return (
    <AppShell>
      <WizardStepper current="validador" />
      <ScreenStateStrip blockedReason="qualquer blocker remove a acao de publicar" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 3.5</div>
          <h1 className="mt-1 text-3xl font-semibold">Validador de Qualidade</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Regras deterministicas, trilha auditavel e bloqueios sem override manual.
          </p>
        </div>
      </div>

      {hasValidationParams && validationQuery.isLoading && (
        <StateBanner tone="info" title="Validador conectado">
          Buscando diagnostico da simulacao {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      )}

      {hasValidationParams && validationQuery.isError && (
        <StateBanner tone="danger" title="Nao foi possivel carregar a validacao">
          {validationQuery.error instanceof Error
            ? validationQuery.error.message
            : "Verifique se o backend esta rodando e se a simulacao existe."}
        </StateBanner>
      )}

      {!hasValidationParams ? (
        <EmptyState
          title="Selecione uma versao para validar"
          description="O validador usa apenas o diagnostico retornado pelo backend."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : (
        <>
          {blockers > 0 ? (
            <StateBanner tone="danger" title={`Publicacao bloqueada - ${blockers} item critico`}>
              O botao Publicar nao aparece. Resolva o blocker ou salve como rascunho.
            </StateBanner>
          ) : warnings > 0 ? (
            <StateBanner tone="warn" title={`Pode publicar - ${warnings} alertas registrados`}>
              A confirmacao registra os alertas no log de auditoria antes de publicar.
            </StateBanner>
          ) : (
            <StateBanner tone="ok" title="Pronta para publicar">
              Sem blocker ou warning ativo. A publicacao usa a versao imutavel atual.
            </StateBanner>
          )}

          <div className="mt-5">
            <NextStepContract
              primary={
                blockers > 0
                  ? "Voltar ao editor. Piloto e publicacao ficam travados."
                  : warnings > 0
                    ? "Confirmar publicacao com alertas gravados no AuditLog."
                    : "Publicar versao imutavel e seguir para piloto."
              }
              secondary="Salvar rascunho nunca publica; volta ao editor mantendo diagnostico clicavel."
              versionRule="Depois de publicar, editar cria nova versao e preserva a publicada."
              lockedAfter="Nao existe override manual para blocker critico."
            />
          </div>

          <div className="mt-5 grid gap-5 lg:grid-cols-[360px_minmax(0,1fr)]">
            <section className="rounded-md border border-border bg-card p-5">
              <div className="text-xs uppercase text-muted-foreground">Score de qualidade</div>
              <div className="mt-2 flex items-end gap-2">
                <div className="text-6xl font-semibold tabular-nums">{qualityScore}</div>
                <div className="mb-2 text-sm text-muted-foreground">/100</div>
              </div>
              <p className="mt-1 text-xs text-muted-foreground">
                Retornado pelo backend junto com os blockers e warnings.
              </p>
              <div className="mt-4 rounded-md border border-border bg-background p-3 text-sm">
                {blockers} blockers e {warnings} warnings.
              </div>
            </section>

            <section className="rounded-md border border-border bg-card p-5">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-sm font-semibold">Diagnostico</h2>
                <span className="rounded-md border border-border bg-background px-2 py-1 text-[11px] text-muted-foreground">
                  dados do backend
                </span>
              </div>
              <ul className="divide-y divide-border">
                {activeChecks.map((check) => (
                  <li key={check.id} className="flex items-start gap-3 py-3 text-sm">
                    <CheckIcon tone={check.tone} />
                    <span className="min-w-0 flex-1">
                      <span className="block text-foreground/90">{check.text}</span>
                      <span className="mt-1 inline-flex items-center gap-1 text-xs text-primary">
                        <ExternalLink className="h-3 w-3" />
                        {check.target}
                      </span>
                    </span>
                  </li>
                ))}
              </ul>

              <div className="mt-5 flex flex-wrap justify-between gap-3">
                <Link
                  to="/nova/dialogo"
                  className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
                >
                  Voltar ao editor
                </Link>
                {blockers > 0 ? (
                  <button className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent">
                    Salvar rascunho
                  </button>
                ) : (
                  <Link
                    to="/nova/governanca"
                    search={{
                      simulationId: search.simulationId!,
                      versionNumber: search.versionNumber!,
                    }}
                    className={cn(
                      "rounded-md px-5 py-2 text-sm font-medium",
                      warnings > 0
                        ? "bg-warning text-warning-foreground hover:opacity-90"
                        : "bg-success text-success-foreground hover:opacity-90",
                    )}
                  >
                    Seguir para governanca
                  </Link>
                )}
              </div>
            </section>
          </div>
        </>
      )}
    </AppShell>
  );
}

function mapValidationIssues(validation: SimulationValidationResponse): ValidationCheck[] {
  if (validation.issues.length === 0) {
    return [
      {
        id: "publishable",
        tone: "ok",
        text: "Nenhum blocker ou alerta encontrado nesta versao",
        target: `Simulacao ${validation.simulationId} v${validation.versionNumber}`,
      },
    ];
  }

  return validation.issues.map((issue, index) => ({
    id: `${issue.severity}-${issue.nodeId ?? "global"}-${index}`,
    tone: issue.severity === "blocker" ? "danger" : "warn",
    text: issue.message,
    target: issue.nodeId ? `Editor: ${issue.nodeId}` : "Simulacao",
  }));
}

function SimulationLinks({
  simulations,
  loading,
}: {
  simulations: SimulationSummaryResponse[];
  loading: boolean;
}) {
  if (loading) {
    return <div className="rounded-md border border-border bg-card px-4 py-3 text-sm">Carregando simulacoes...</div>;
  }

  if (simulations.length === 0) {
    return (
      <Link
        to="/nova/blueprint"
        className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
      >
        Criar simulacao
      </Link>
    );
  }

  return (
    <>
      {simulations.slice(0, 3).map((simulation) => (
        <Link
          key={simulation.id}
          to="/nova/validador"
          search={{
            simulationId: simulation.id,
            versionNumber: simulation.versionNumber,
          }}
          className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
        >
          <span className="block font-medium">{simulation.name}</span>
          <span className="mt-1 block">
            <StatusBadge status={simulation.status} maturity={maturityForStatus(simulation.status)} />
          </span>
        </Link>
      ))}
    </>
  );
}

function CheckIcon({ tone }: { tone: "ok" | "warn" | "danger" }) {
  const cls =
    tone === "ok"
      ? "bg-success/15 text-success"
      : tone === "warn"
        ? "bg-warning/20 text-warning-foreground"
        : "bg-danger/15 text-danger";
  const Icon = tone === "ok" ? CheckCircle2 : tone === "warn" ? AlertTriangle : XCircle;
  return (
    <span
      className={cn("mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full", cls)}
    >
      <Icon className="h-4 w-4" />
    </span>
  );
}
