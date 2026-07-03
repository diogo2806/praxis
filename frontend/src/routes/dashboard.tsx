import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  AlertTriangle,
  BarChart3,
  CalendarClock,
  CheckCircle2,
  ClipboardCheck,
  ClipboardList,
  CreditCard,
  FilePlus2,
  Link2,
  ListChecks,
  PlugZap,
  RefreshCw,
  Route as RouteIcon,
  Users,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner } from "@/components/praxis-ui";
import {
  getDashboard,
  type AttemptStatus,
  type AssessmentJourneyStatus,
  type DashboardActionSeverity,
  type DashboardResponse,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/dashboard")({
  head: () => ({
    meta: [
      { title: "Dashboard - Práxis" },
      {
        name: "description",
        content: "Visão consolidada do cliente, operação, integrações, jornadas e uso do plano.",
      },
    ],
  }),
  component: DashboardPage,
});

const quickActions = [
  {
    label: "Criar avaliação",
    to: "/simulations/new",
    icon: FilePlus2,
    primary: true,
  },
  {
    label: "Ver avaliações",
    to: "/avaliacoes",
    icon: ListChecks,
    primary: false,
  },
  {
    label: "Ver resultados",
    to: "/results",
    icon: ClipboardList,
    primary: false,
  },
  {
    label: "Gerar link",
    to: "/candidate-links/new",
    icon: Link2,
    primary: false,
  },
  {
    label: "Configurar integrações",
    to: "/integrations",
    icon: PlugZap,
    primary: false,
  },
] as const;

function DashboardPage() {
  const dashboardQuery = useQuery({
    queryKey: ["dashboard"],
    queryFn: getDashboard,
    retry: false,
  });

  return (
    <AppShell>
      {dashboardQuery.isLoading ? (
        <LoadingState />
      ) : dashboardQuery.isError ? (
        <ErrorState onReload={() => dashboardQuery.refetch()} />
      ) : dashboardQuery.data ? (
        <DashboardContent
          dashboard={dashboardQuery.data}
          onReload={() => dashboardQuery.refetch()}
        />
      ) : null}
    </AppShell>
  );
}

function DashboardContent({
  dashboard,
  onReload,
}: {
  dashboard: DashboardResponse;
  onReload: () => void;
}) {
  const isEmpty =
    dashboard.activeSimulations === 0 &&
    dashboard.assessmentJourneys.total === 0 &&
    dashboard.latestResults.length === 0;

  if (isEmpty) {
    return (
      <EmptyState
        title="Comece pelo Dashboard."
        description="Este é o centro da operação no Práxis. Crie uma avaliação para iniciar o fluxo e depois acompanhe links, candidatos, resultados e integrações por aqui."
        actions={
          <>
            <Link
              to="/simulations/new"
              className="inline-flex items-center justify-between gap-2 rounded-md border border-primary bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Criar primeira avaliação
              <FilePlus2 className="h-4 w-4" />
            </Link>
            <Link
              to="/integrations"
              className="inline-flex items-center justify-between gap-2 rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
            >
              Configurar integrações
              <PlugZap className="h-4 w-4" />
            </Link>
          </>
        }
      />
    );
  }

  return (
    <div className="space-y-6">
      <DashboardHeader dashboard={dashboard} onReload={onReload} />
      <DashboardQuickActions />

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <DashboardMetricCard
          title="Avaliações ativas"
          value={dashboard.activeSimulations}
          hint="Publicadas e disponíveis"
          icon={ClipboardCheck}
        />
        <DashboardMetricCard
          title="Jornadas de avaliação"
          value={dashboard.assessmentJourneys.total}
          hint={`${dashboard.assessmentJourneys.published} publicadas · ${dashboard.assessmentJourneys.draft} rascunhos`}
          icon={RouteIcon}
        />
        <DashboardMetricCard
          title="Candidatos em andamento"
          value={dashboard.candidatesInProgress}
          hint="Avaliações ou jornadas abertas"
          icon={Users}
        />
        <DashboardMetricCard
          title="Concluídas nos últimos 30 dias"
          value={dashboard.completedAttemptsLast30Days}
          hint="Uso recente da operação"
          icon={BarChart3}
        />
      </div>

      <div className="space-y-6">
        <RecommendedActionsPanel actions={dashboard.recommendedActions} />
        <LatestResultsTable dashboard={dashboard} />
        <AssessmentJourneySummary dashboard={dashboard} />
        <IntegrationsStatusPanel dashboard={dashboard} />
        <BillingUsageCard dashboard={dashboard} />
      </div>
    </div>
  );
}

function DashboardHeader({
  dashboard,
  onReload,
}: {
  dashboard: DashboardResponse;
  onReload: () => void;
}) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div>
        <div className="text-xs uppercase text-muted-foreground">Centro da operação</div>
        <h1 className="mt-1 text-3xl font-semibold text-foreground">
          Dashboard da {dashboard.empresaName}
        </h1>
        <p className="mt-1 flex items-center gap-2 text-sm text-muted-foreground">
          <CalendarClock className="h-4 w-4" />
          Acompanhe avaliações, candidatos, resultados, integrações e uso do plano em um só lugar.
        </p>
      </div>
      <button
        type="button"
        onClick={onReload}
        className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
      >
        <RefreshCw className="h-4 w-4" />
        Recarregar
      </button>
    </div>
  );
}

function DashboardQuickActions() {
  return (
    <section className="rounded-md border border-border bg-card p-4">
      <div className="mb-3">
        <h2 className="text-lg font-semibold">Ações principais</h2>
        <p className="text-sm text-muted-foreground">
          Comece ou acompanhe os principais processos da operação.
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        {quickActions.map((action) => (
          <Link
            key={action.to}
            to={action.to}
            className={cn(
              "inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm font-medium",
              action.primary
                ? "border-primary bg-primary text-primary-foreground hover:bg-primary/90"
                : "border-border bg-background text-foreground hover:bg-accent",
            )}
          >
            <action.icon className="h-4 w-4" />
            {action.label}
          </Link>
        ))}
      </div>
    </section>
  );
}

function DashboardMetricCard({
  title,
  value,
  hint,
  icon: Icon,
}: {
  title: string;
  value: number;
  hint: string;
  icon: typeof ClipboardCheck;
}) {
  return (
    <section className="rounded-md border border-border bg-card p-4">
      <div className="flex items-center justify-between gap-3">
        <div className="text-xs uppercase text-muted-foreground">{title}</div>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </div>
      <div className="mt-2 text-3xl font-semibold tabular-nums text-foreground">
        {value.toLocaleString("pt-BR")}
      </div>
      <div className="mt-1 text-xs text-muted-foreground">{hint}</div>
    </section>
  );
}

function RecommendedActionsPanel({
  actions,
}: {
  actions: DashboardResponse["recommendedActions"];
}) {
  return (
    <section className="rounded-md border border-border bg-card p-4">
      <h2 className="text-lg font-semibold">Próximos passos</h2>
      <div className="mt-3 space-y-3">
        {actions.map((action) => (
          <div
            key={`${action.type}-${action.route}`}
            className="flex flex-wrap items-start justify-between gap-3 rounded-md border border-border bg-background p-3"
          >
            <div className="flex min-w-0 gap-3">
              <ActionIcon severity={action.severity} />
              <div>
                <div className="font-medium text-foreground">{action.title}</div>
                <p className="mt-0.5 text-sm text-muted-foreground">{action.description}</p>
              </div>
            </div>
            <a
              href={action.route}
              className="inline-flex shrink-0 items-center rounded-md border border-border bg-card px-3 py-1.5 text-xs font-medium hover:bg-accent"
            >
              {action.buttonLabel}
            </a>
          </div>
        ))}
      </div>
    </section>
  );
}

function LatestResultsTable({ dashboard }: { dashboard: DashboardResponse }) {
  return (
    <section className="rounded-md border border-border bg-card">
      <div className="flex items-center justify-between gap-3 border-b border-border p-4">
        <h2 className="text-lg font-semibold">Últimos resultados</h2>
        <a href="/results" className="text-sm font-medium text-primary hover:underline">
          Ver resultados
        </a>
      </div>
      {dashboard.latestResults.length === 0 ? (
        <EmptyStateCard
          title="Nenhum resultado ainda"
          description="Os resultados aparecerão aqui após as primeiras conclusões."
        />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
              <tr>
                <th className="px-4 py-3 text-left font-medium">Candidato</th>
                <th className="px-4 py-3 text-left font-medium">Avaliação/Jornada</th>
                <th className="px-4 py-3 text-left font-medium">Status</th>
                <th className="px-4 py-3 text-left font-medium">Data</th>
                <th className="px-4 py-3 text-left font-medium">Resultado</th>
                <th className="px-4 py-3 text-right font-medium">Ação</th>
              </tr>
            </thead>
            <tbody>
              {dashboard.latestResults.map((result) => (
                <tr key={result.attemptId} className="border-b border-border last:border-0">
                  <td className="px-4 py-3 font-medium">{result.candidateName}</td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {result.simulationOrJourneyName}
                  </td>
                  <td className="px-4 py-3">
                    <AttemptStatusBadge status={result.status} />
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">{formatDate(result.date)}</td>
                  <td className="px-4 py-3 tabular-nums">
                    {result.result == null ? "-" : `${result.result}%`}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <a
                      href={result.actionRoute}
                      className="text-sm font-medium text-primary hover:underline"
                    >
                      {result.actionLabel}
                    </a>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function AssessmentJourneySummary({ dashboard }: { dashboard: DashboardResponse }) {
  return (
    <section className="rounded-md border border-border bg-card">
      <div className="flex items-center justify-between gap-3 border-b border-border p-4">
        <h2 className="text-lg font-semibold">Jornadas de avaliação</h2>
        <a href="/assessment-journeys" className="text-sm font-medium text-primary hover:underline">
          Ver todas as jornadas
        </a>
      </div>
      {dashboard.journeys.length === 0 ? (
        <EmptyStateCard
          title="Nenhuma jornada criada"
          description="Crie uma jornada para organizar processos com múltiplas avaliações."
        />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
              <tr>
                <th className="px-4 py-3 text-left font-medium">Nome</th>
                <th className="px-4 py-3 text-left font-medium">Status</th>
                <th className="px-4 py-3 text-left font-medium">Candidatos em andamento</th>
                <th className="px-4 py-3 text-right font-medium">Ação</th>
              </tr>
            </thead>
            <tbody>
              {dashboard.journeys.map((journey) => (
                <tr key={journey.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3 font-medium">{journey.name}</td>
                  <td className="px-4 py-3">
                    <JourneyStatusBadge status={journey.status} />
                  </td>
                  <td className="px-4 py-3 tabular-nums">
                    {journey.candidatesInProgress.toLocaleString("pt-BR")}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <a
                      href={journey.actionRoute}
                      className="text-sm font-medium text-primary hover:underline"
                    >
                      {journey.actionLabel}
                    </a>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function IntegrationsStatusPanel({ dashboard }: { dashboard: DashboardResponse }) {
  return (
    <section className="rounded-md border border-border bg-card p-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">Integrações</h2>
        <a href="/integrations" className="text-sm font-medium text-primary hover:underline">
          Configurar
        </a>
      </div>
      <div className="mt-3 space-y-3">
        {dashboard.integrations.map((integration) => (
          <div
            key={integration.provider}
            className="rounded-md border border-border bg-background p-3"
          >
            <div className="flex items-center justify-between gap-3">
              <div className="font-medium">{integration.name}</div>
              <span
                className={cn(
                  "rounded-md px-2 py-1 text-xs font-medium",
                  integrationTone(integration.status),
                )}
              >
                {integrationLabel(integration.status)}
              </span>
            </div>
            <div className="mt-1 text-xs text-muted-foreground">
              {integration.lastSyncAt
                ? `Última sync: ${formatDate(integration.lastSyncAt)}`
                : "Sem sincronização registrada"}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

function BillingUsageCard({ dashboard }: { dashboard: DashboardResponse }) {
  const billing = dashboard.billing;
  return (
    <section className="rounded-md border border-border bg-card p-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">Plano e uso</h2>
        <CreditCard className="h-4 w-4 text-muted-foreground" />
      </div>
      <dl className="mt-4 space-y-3 text-sm">
        <BillingLine label="Plano" value={planLabel(billing.plan)} />
        <BillingLine label="Status" value={billing.status} />
        {billing.plan === "AVULSO" && (
          <>
            <BillingLine
              label="Saldo de créditos"
              value={billing.creditBalance.toLocaleString("pt-BR")}
            />
            <BillingLine
              label="Créditos usados no período"
              value={billing.usedInPeriod.toLocaleString("pt-BR")}
            />
          </>
        )}
        {billing.plan === "PROFISSIONAL" && (
          <>
            <BillingLine
              label="Assinatura"
              value={billing.subscriptionStatus ?? "Sem assinatura"}
            />
            <BillingLine
              label="Uso no período"
              value={`${billing.usedInPeriod.toLocaleString("pt-BR")} avaliações concluídas`}
            />
            <BillingLine
              label="Próxima renovação"
              value={billing.nextRenewalAt ? formatDate(billing.nextRenewalAt) : "Não informada"}
            />
          </>
        )}
        {billing.plan === "ENTERPRISE" && (
          <>
            <BillingLine
              label="Condição comercial"
              value={billing.commercialCondition ?? "Sob contrato"}
            />
            <BillingLine
              label="Uso no período"
              value={`${billing.usedInPeriod.toLocaleString("pt-BR")} avaliações concluídas`}
            />
          </>
        )}
      </dl>
      <a
        href="/billing"
        className="mt-4 inline-flex w-full items-center justify-center rounded-md border border-border bg-background px-3 py-2 text-sm font-medium hover:bg-accent"
      >
        Ver detalhes do plano
      </a>
    </section>
  );
}

function BillingLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}

function EmptyStateCard({ title, description }: { title: string; description: string }) {
  return (
    <div className="p-4">
      <div className="rounded-md border border-dashed border-border bg-background p-4">
        <div className="font-medium">{title}</div>
        <p className="mt-1 text-sm text-muted-foreground">{description}</p>
      </div>
    </div>
  );
}

function LoadingState() {
  return (
    <section className="rounded-md border border-border bg-card p-6">
      <div className="text-sm font-medium">Carregando seu dashboard...</div>
      <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="h-28 animate-pulse rounded-md bg-muted" />
        ))}
      </div>
    </section>
  );
}

function ErrorState({ onReload }: { onReload: () => void }) {
  return (
    <StateBanner
      tone="danger"
      title="Não foi possível carregar o dashboard."
      action={
        <button
          type="button"
          onClick={onReload}
          className="rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium"
        >
          Recarregar
        </button>
      }
    >
      Tente novamente.
    </StateBanner>
  );
}

function ActionIcon({ severity }: { severity: DashboardActionSeverity }) {
  if (severity === "success") {
    return <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-success" />;
  }
  return <AlertTriangle className={cn("mt-0.5 h-5 w-5 shrink-0", severityIcon(severity))} />;
}

function severityIcon(severity: DashboardActionSeverity) {
  return {
    info: "text-primary",
    warning: "text-warning",
    success: "text-success",
    danger: "text-danger",
  }[severity];
}

function integrationTone(status: string) {
  return (
    {
      CONECTADA: "bg-success/10 text-success",
      PENDENTE: "bg-warning/10 text-warning",
      ERRO: "bg-danger/10 text-danger",
      DESATIVADA: "bg-muted text-muted-foreground",
      NAO_CONFIGURADA: "bg-muted text-muted-foreground",
    }[status] ?? "bg-muted text-muted-foreground"
  );
}

function integrationLabel(status: string) {
  return (
    {
      CONECTADA: "Conectada",
      PENDENTE: "Pendente",
      ERRO: "Erro",
      DESATIVADA: "Desativada",
      NAO_CONFIGURADA: "Não configurada",
    }[status] ?? status
  );
}

function planLabel(plan: string) {
  return (
    {
      AVULSO: "Avulso",
      PROFISSIONAL: "Profissional",
      ENTERPRISE: "Enterprise",
    }[plan] ?? plan
  );
}

const attemptStatusMeta: Record<AttemptStatus, { label: string; cls: string }> = {
  notStarted: { label: "Não iniciada", cls: "border-border bg-muted text-foreground" },
  inProgress: { label: "Em andamento", cls: "border-primary/25 bg-primary/10 text-foreground" },
  completed: { label: "Concluída", cls: "border-success/25 bg-success/10 text-foreground" },
  abandoned: { label: "Abandonada", cls: "border-border bg-muted text-foreground" },
  expired: { label: "Expirada", cls: "border-border bg-muted text-foreground" },
};

function AttemptStatusBadge({ status }: { status: AttemptStatus }) {
  const meta = attemptStatusMeta[status] ?? {
    label: status,
    cls: "border-border bg-muted text-foreground",
  };
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-[11px] font-medium",
        meta.cls,
      )}
    >
      {meta.label}
    </span>
  );
}

const journeyStatusMeta: Record<AssessmentJourneyStatus, { label: string; cls: string }> = {
  draft: { label: "Rascunho", cls: "border-border bg-muted text-foreground" },
  published: { label: "Publicada", cls: "border-success/25 bg-success/10 text-foreground" },
  archived: { label: "Arquivada", cls: "border-border bg-muted text-foreground" },
};

function JourneyStatusBadge({ status }: { status: AssessmentJourneyStatus }) {
  const meta = journeyStatusMeta[status] ?? {
    label: status,
    cls: "border-border bg-muted text-foreground",
  };
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-[11px] font-medium",
        meta.cls,
      )}
    >
      {meta.label}
    </span>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
