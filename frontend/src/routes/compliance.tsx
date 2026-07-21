import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  AlertTriangle,
  CheckCircle2,
  FileCheck2,
  LockKeyhole,
  ShieldCheck,
} from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { StateBanner, StatusBadge } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  getSimulationValidation,
  listSimulations,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/compliance")({
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
    meta: [{ title: "Conformidade contextual - Práxis" }],
  }),
  component: CompliancePage,
});

function CompliancePage() {
  const search = Route.useSearch();
  const hasContext = Boolean(search.simulationId && search.versionNumber);

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
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

  return (
    <AppShell>
      <main className="mx-auto max-w-5xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Conformidade contextual
            </div>
            <h1 className="mt-1 font-display text-3xl">Responsabilidades da versão</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Esta tela não mantém uma segunda lista de avaliações. Ela organiza os atalhos de
              validação, publicação, auditoria e privacidade da versão selecionada.
            </p>
          </div>
          <Button asChild variant="outline">
            <Link to="/avaliacoes">Voltar para Avaliações</Link>
          </Button>
        </header>

        {!hasContext ? (
          <StateBanner tone="warning" title="Abra a conformidade a partir de uma avaliação">
            Selecione uma avaliação e uma versão em Avaliações ou no Validador. A conformidade não
            possui mais uma listagem global concorrente.
            <div className="mt-4">
              <Button asChild>
                <Link to="/avaliacoes">Selecionar avaliação</Link>
              </Button>
            </div>
          </StateBanner>
        ) : validationQuery.isError || simulationsQuery.isError ? (
          <StateBanner tone="danger" title="Não foi possível carregar a conformidade">
            {validationQuery.error instanceof Error
              ? validationQuery.error.message
              : simulationsQuery.error instanceof Error
                ? simulationsQuery.error.message
                : "Tente novamente."}
          </StateBanner>
        ) : validationQuery.isLoading || simulationsQuery.isLoading || !validation ? (
          <section className="rounded-xl border border-border bg-card px-5 py-12 text-center text-sm text-muted-foreground">
            Carregando contexto da versão...
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
                    Cada responsabilidade abaixo abre a tela que realmente mantém a informação.
                  </p>
                </div>
                {simulation && (
                  <StatusBadge
                    status={simulation.status}
                    maturity={maturityForStatus(simulation.status)}
                  />
                )}
              </div>

              <div className="mt-5 grid gap-3 sm:grid-cols-3">
                <MetricCard
                  label="Prontidão estrutural"
                  value={validation.publishable ? "Pronta" : "Bloqueada"}
                  tone={validation.publishable ? "ok" : "danger"}
                />
                <MetricCard
                  label="Bloqueios"
                  value={String(validation.blockerCount)}
                  tone={validation.blockerCount === 0 ? "ok" : "danger"}
                />
                <MetricCard
                  label="Qualidade"
                  value={`${validation.qualityScore}%`}
                  tone={validation.qualityScore >= 80 ? "ok" : "warning"}
                />
              </div>
            </section>

            <section className="grid gap-4 md:grid-cols-2">
              <ResponsibilityCard
                icon={AlertTriangle}
                title="Validação estrutural"
                description="Bloqueios, avisos, qualidade e prontidão são mantidos pelo Validador."
                action="Abrir Validador"
                to="/nova/validador"
                search={{
                  simulationId: validation.simulationId,
                  versionNumber: validation.versionNumber,
                }}
              />
              <ResponsibilityCard
                icon={FileCheck2}
                title="Publicação e auditoria"
                description="Termos, aceite, publicação e trilha de alterações pertencem à Governança."
                action="Abrir Governança"
                to="/nova/governanca"
                search={{
                  simulationId: validation.simulationId,
                  versionNumber: validation.versionNumber,
                }}
              />
              <ResponsibilityCard
                icon={LockKeyhole}
                title="Privacidade corporativa"
                description="Solicitações de titulares, retenção e controles LGPD ficam na área de Privacidade."
                action="Abrir Privacidade"
                to="/privacidade"
              />
              <ResponsibilityCard
                icon={ShieldCheck}
                title="Cadastro e ciclo da avaliação"
                description="Nome, descrição, versão e situação continuam centralizados em Avaliações."
                action="Abrir Avaliações"
                to="/avaliacoes"
              />
            </section>

            {validation.publishable ? (
              <StateBanner tone="ok" title="Validação estrutural aprovada">
                A publicação ainda depende dos termos e confirmações apresentados em Governança.
              </StateBanner>
            ) : (
              <StateBanner tone="warning" title="Existem bloqueios estruturais">
                Corrija os {validation.blockerCount} bloqueio(s) no fluxo indicado pelo Validador antes
                de tentar publicar.
              </StateBanner>
            )}
          </>
        )}
      </main>
    </AppShell>
  );
}

function ResponsibilityCard({
  icon: Icon,
  title,
  description,
  action,
  to,
  search,
}: {
  icon: typeof ShieldCheck;
  title: string;
  description: string;
  action: string;
  to: string;
  search?: { simulationId: string; versionNumber: number };
}) {
  return (
    <article className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-start gap-3">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
          <Icon className="h-5 w-5" />
        </span>
        <div>
          <h2 className="font-semibold">{title}</h2>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">{description}</p>
        </div>
      </div>
      <div className="mt-4">
        <Button asChild variant="outline">
          <Link to={to} search={search}>
            {action}
          </Link>
        </Button>
      </div>
    </article>
  );
}

function MetricCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone: "ok" | "danger" | "warning";
}) {
  const classes = {
    ok: "border-success/30 bg-success/10 text-success",
    danger: "border-danger/30 bg-danger/10 text-danger",
    warning: "border-warning/40 bg-warning/10 text-warning-foreground",
  }[tone];

  return (
    <article className={cn("rounded-lg border p-4", classes)}>
      <div className="text-xs font-semibold uppercase tracking-wide opacity-80">{label}</div>
      <div className="mt-2 flex items-center gap-2 text-2xl font-semibold">
        {tone === "ok" && <CheckCircle2 className="h-5 w-5" />}
        {value}
      </div>
    </article>
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
