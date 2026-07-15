import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  AlertTriangle,
  BarChart3,
  CalendarClock,
  CheckCircle2,
  ClipboardCheck,
  CreditCard,
  FilePlus2,
  Link2,
  ListChecks,
  PlugZap,
  RefreshCw,
  Route as RouteIcon,
  Users,
  type LucideIcon,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { DashboardCompatibilityError, getDashboard } from "@/lib/api/dashboard-strict";
import { type DashboardActionSeverity, type DashboardResponse } from "@/lib/api/praxis";
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
  { label: "Criar avaliação", to: "/simulations/new", icon: FilePlus2, primary: true },
  { label: "Ver avaliações", to: "/avaliacoes", icon: ListChecks, primary: false },
  { label: "Ver resultados", to: "/results", icon: ClipboardCheck, primary: false },
  { label: "Gerar link", to: "/candidate-links/new", icon: Link2, primary: false },
  { label: "Configurar integrações", to: "/integrations", icon: PlugZap, primary: false },
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
        <ErrorState error={dashboardQuery.error} onReload={() => dashboardQuery.refetch()} />
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
          hint="Uso recente informado pelo backend"
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
          Dados consolidados pelo endpoint oficial do dashboard.
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
          Acesse os principais processos sem alterar o estado exibido no dashboard.
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
  icon: LucideIcon;
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
      {actions.length === 0 ? (
        <EmptyPanel message="Nenhuma recomendação foi informada pelo backend." />
      ) : (
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
      )}
    </section>
  );
}

function LatestResultsTable({ dashboard }: { dashboard: DashboardResponse }) {
  return (
    <section className="rounded-md border border-border bg-card">
      <SectionHeader title="Últimos resultados" href="/results" actionLabel="Ver resultados" />
      {dashboard.latestResults.length === 0 ? (
        <EmptyPanel message="Nenhum resultado foi informado pelo backend." />
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
                  <td className="px-4 py-3">{attemptStatusLabel(result.status)}</td>
                  <td className="px-4 py-3 text-muted-foreground">{formatDate(result.date)}</td>
                  <td className="px-4 py-3 tabular-nums">
                    {result.result == null ? "Não informado" : `${result.result}%`}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <a href={result.actionRoute} className="font-medium text-primary hover:underline">
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
      <SectionHeader
        title="Jornadas de avaliação"
        href="/assessment-journeys"
        actionLabel="Ver jornadas"
      />
      {dashboard.journeys.length === 0 ? (
        <EmptyPanel message="Nenhuma jornada foi informada pelo backend." />
      ) : (
        <div className="divide-y divide-border">
          {dashboard.journeys.map((journey) => (
            <div key={journey.id} className="flex flex-wrap items-center justify-between gap-3 p-4">
              <div>
                <div className="font-medium">{journey.name}</div>
                <div className="mt-1 text-xs text-muted-foreground">
                  {journeyStatusLabel(journey.status)} · {journey.candidatesInProgress} candidatos em andamento
                </div>
              </div>
              <a href={journey.actionRoute} className="text-sm font-medium text-primary hover:underline">
                {journey.actionLabel}
              </a>
            </div>
          ))}
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
      {dashboard.integrations.length === 0 ? (
        <EmptyPanel message="Nenhum status de integração foi informado pelo backend." />
      ) : (
        <div className="mt-3 space-y-3">
          {dashboard.integrations.map((integration) => (
            <div
              key={integration.provider}
              className="rounded-md border border-border bg-background p-3"
            >
              <div className="flex items-center justify-between gap-3">
                <div className="font-medium">{integration.name}</div>
                <span className={cn("rounded-md px-2 py-1 text-xs font-medium", integrationTone(integration.status))}>
                  {integrationLabel(integration.status)}
                </span>
              </div>
              <div className="mt-1 text-xs text-muted-foreground">
                {integration.lastSyncAt
                  ? `Última sincronização: ${formatDate(integration.lastSyncAt)}`
                  : "Sem sincronização comprovada"}
              </div>
            </div>
          ))}
        </div>
      )}
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
        <BillingLine
          label="Uso no período"
          value={`${billing.usedInPeriod.toLocaleString("pt-BR")} avaliações concluídas`}
        />
        {billing.plan === "AVULSO" && (
          <BillingLine
            label="Saldo de créditos"
            value={billing.creditBalance.toLocaleString("pt-BR")}
          />
        )}
        {billing.plan === "PROFISSIONAL" && (
          <>
            <BillingLine
              label="Assinatura"
              value={billing.subscriptionStatus ?? "Não informada"}
            />
            <BillingLine
              label="Próxima renovação"
              value={billing.nextRenewalAt ? formatDate(billing.nextRenewalAt) : "Não informada"}
            />
          </>
        )}
        {billing.plan === "ENTERPRISE" && (
          <BillingLine
            label="Condição comercial"
            value={billing.commercialCondition ?? "Não informada"}
          />
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

function SectionHeader({
  title,
  href,
  actionLabel,
}: {
  title: string;
  href: string;
  actionLabel: string;
}) {
  return (
    <div className="flex items-center justify-between gap-3 border-b border-border p-4">
      <h2 className="text-lg font-semibold">{title}</h2>
      <a href={href} className="text-sm font-medium text-primary hover:underline">
        {actionLabel}
      </a>
    </div>
  );
}

function EmptyPanel({ message }: { message: string }) {
  return <div className="p-4 text-sm text-muted-foreground">{message}</div>;
}

function BillingLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="text-right font-medium">{value}</dd>
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

function ErrorState({ error, onReload }: { error: unknown; onReload: () => void }) {
  const incompatible = error instanceof DashboardCompatibilityError;
  const message =
    error instanceof Error ? error.message : "O dashboard não pôde ser carregado nesta tentativa.";

  return (
    <StateBanner
      tone={incompatible ? "warn" : "danger"}
      title={
        incompatible
          ? "Dashboard indisponível por incompatibilidade de versão."
          : "Não foi possível carregar o dashboard."
      }
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
      {incompatible
        ? `${message} Nenhum plano, status operacional ou conexão de integração foi inferido a partir de tokens ou de respostas auxiliares.`
        : message}
    </StateBanner>
  );
}

function ActionIcon({ severity }: { severity: DashboardActionSeverity }) {
  if (severity === "success") {
    return <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-success" />;
  }

  return (
    <AlertTriangle
      className={cn(
        "mt-0.5 h-5 w-5 shrink-0",
        severity === "danger"
          ? "text-danger"
          : severity === "warning"
            ? "text-warning"
            : "text-primary",
      )}
    />
  );
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

function attemptStatusLabel(status: string) {
  return (
    {
      notStarted: "Não iniciada",
      inProgress: "Em andamento",
      completed: "Concluída",
      abandoned: "Abandonada",
      expired: "Expirada",
    }[status] ?? status
  );
}

function journeyStatusLabel(status: string) {
  return (
    {
      draft: "Rascunho",
      published: "Publicada",
      archived: "Arquivada",
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

function formatDate(value: string) {
  const date = new Date(value);
  if (!Number.isFinite(date.getTime())) {
    return "Data não informada";
  }

  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
