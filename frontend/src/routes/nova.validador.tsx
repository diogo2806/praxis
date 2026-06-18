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
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Validador de Qualidade - Praxis" },
      {
        name: "description",
        content: "Diagnóstico determinístico com bloqueios de publicação.",
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
  const canPublish = Boolean(validationQuery.data) && blockers === 0;

  return (
    <AppShell>
      <WizardStepper current="revisao" unlockedThrough={canPublish ? "publicacao" : "revisao"} />
      <ScreenStateStrip blockedReason="qualquer blocker remove a acao de publicar" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 3</div>
          <h1 className="mt-1 text-3xl font-semibold">Validador de Qualidade</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Regras determinísticas, histórico completo de alterações e bloqueios sem override manual.
          </p>
        </div>
      </div>

      {hasValidationParams && validationQuery.isLoading && (
        <StateBanner tone="info" title="Validador conectado">
          Buscando diagnóstico da simulação {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      )}

      {hasValidationParams && validationQuery.isError && (
        <StateBanner tone="danger" title="Não foi possível carregar a validação">
          {validationQuery.error instanceof Error
            ? validationQuery.error.message
            : "Verifique se o backend está rodando e se a simulação existe."}
        </StateBanner>
      )}

      {!hasValidationParams ? (
        <EmptyState
          title="Selecione uma versão para validar"
          description="O validador usa apenas o diagnóstico retornado pelo backend."
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
            <StateBanner
              tone="danger"
              title={`Publicação bloqueada — ${blockers} ${blockers === 1 ? "item crítico" : "itens críticos"}`}
            >
              O botão Publicar não aparece. Resolva o blocker ou salve como rascunho.
            </StateBanner>
          ) : warnings > 0 ? (
            <StateBanner
              tone="warn"
              title={`Pode publicar — ${warnings} ${warnings === 1 ? "alerta registrado" : "alertas registrados"}`}
            >
              A confirmação registra os alertas no log de auditoria antes de publicar.
            </StateBanner>
          ) : (
            <StateBanner tone="ok" title="Pronta para publicar">
              Sem blocker ou warning ativo. A publicação usa a versão imutável atual.
            </StateBanner>
          )}

          <div className="mt-5">
            <NextStepContract
              primary={
                blockers > 0
                  ? "Voltar ao editor. Piloto e publicação ficam travados."
                  : warnings > 0
                    ? "Confirmar publicação com alertas gravados no AuditLog."
                    : "Publicar versão imutável e seguir para piloto."
              }
              secondary="Salvar rascunho nunca publica; volta ao editor mantendo diagnostico clicavel."
              versionRule="Depois de publicar, editar cria nova versão e preserva a publicada."
              lockedAfter="Não existe override manual para blocker crítico."
            />
          </div>

          <div className="mt-5 grid gap-5 lg:grid-cols-[360px_minmax(0,1fr)]">
            <section className="rounded-md border border-border bg-card p-5">
              <div className="text-xs uppercase text-muted-foreground">Pontuação de qualidade</div>
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
                <h2 className="text-sm font-semibold">Diagnóstico</h2>
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
                    to="/nova/publicacao"
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
                    Seguir para governança
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
        text: "Nenhum blocker ou alerta encontrado nesta versão",
        target: `Simulação ${validation.simulationId} v${validation.versionNumber}`,
      },
    ];
  }

  return validation.issues.map((issue, index) => ({
    id: `${issue.severity}-${issue.nodeId ?? "global"}-${index}`,
    tone: issue.severity === "blocker" ? "danger" : "warn",
    text: issue.message,
    target: issue.nodeId ? `Editor: ${issue.nodeId}` : "Simulação",
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
    return (
      <div className="rounded-md border border-border bg-card px-4 py-3 text-sm">
        Carregando simulações...
      </div>
    );
  }

  if (simulations.length === 0) {
    return (
      <Link
        to="/nova/avaliacao"
        className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
      >
        Criar simulação
      </Link>
    );
  }

  return (
    <>
      {simulations.slice(0, 3).map((simulation) => (
        <Link
          key={simulation.id}
          to="/nova/revisao"
          search={{
            simulationId: simulation.id,
            versionNumber: simulation.versionNumber,
          }}
          className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
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
