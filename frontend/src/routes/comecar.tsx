import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { CheckCircle2, Circle, RefreshCw } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { getDashboard } from "@/lib/api/dashboard-strict";
import type { DashboardResponse } from "@/lib/api/praxis";
import { useSession } from "@/lib/session";

export const Route = createFileRoute("/comecar")({
  head: () => ({
    meta: [
      { title: "Comece aqui - Práxis" },
      {
        name: "description",
        content: "Acompanhe a configuração inicial e conclua o primeiro processo no Práxis.",
      },
    ],
  }),
  component: StartPage,
});

type OnboardingStepKey =
  | "assessment"
  | "scenario"
  | "publish"
  | "journey"
  | "invite"
  | "result";

const steps = [
  {
    key: "assessment" as const,
    number: "01",
    title: "Crie a avaliação",
    description:
      "Defina o cargo, a situação crítica e as competências que serão observadas.",
    to: "/nova/avaliacao" as const,
    action: "Criar avaliação",
  },
  {
    key: "scenario" as const,
    number: "02",
    title: "Monte o cenário",
    description:
      "Cadastre etapas e alternativas. Cada resposta recebe critérios e pontuações definidos antes da aplicação.",
    to: "/avaliacoes" as const,
    action: "Ver avaliações",
  },
  {
    key: "publish" as const,
    number: "03",
    title: "Revise e publique",
    description:
      "O validador bloqueia falhas estruturais e caminhos que tornariam os resultados incomparáveis.",
    to: "/avaliacoes" as const,
    action: "Continuar uma avaliação",
  },
  {
    key: "journey" as const,
    number: "04",
    title: "Monte a jornada",
    description:
      "Organize uma ou mais avaliações na ordem em que a pessoa candidata deverá realizá-las.",
    to: "/jornadas" as const,
    action: "Criar jornada",
  },
  {
    key: "invite" as const,
    number: "05",
    title: "Convide e acompanhe",
    description:
      "Publique a jornada, gere o convite da pessoa participante e acompanhe o andamento do processo.",
    to: "/jornadas" as const,
    action: "Abrir jornadas",
  },
  {
    key: "result" as const,
    number: "06",
    title: "Revise o primeiro resultado",
    description:
      "Abra as evidências por competência e registre a decisão humana sobre a candidatura.",
    to: "/results" as const,
    action: "Ver resultados",
  },
];

function StartPage() {
  const session = useSession();
  const dashboardQuery = useQuery({
    queryKey: ["dashboard", "onboarding"],
    queryFn: getDashboard,
    retry: false,
    enabled: Boolean(session.token),
  });

  if (!session.token) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-5 py-12 text-foreground">
        <section className="w-full max-w-xl rounded-xl border border-border bg-card p-8 text-center">
          <div className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">Práxis</div>
          <h1 className="mt-3 font-display text-3xl">Acesse seu workspace para começar</h1>
          <p className="mx-auto mt-3 max-w-md text-sm leading-6 text-muted-foreground">
            O guia de criação e as ações operacionais ficam disponíveis após a autenticação.
          </p>
          <div className="mt-6 flex flex-wrap justify-center gap-3">
            <Link
              to="/login"
              className="rounded-md bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Entrar
            </Link>
            <Link
              to="/"
              className="rounded-md border border-border bg-card px-5 py-2.5 text-sm font-medium hover:bg-accent"
            >
              Conhecer o Práxis
            </Link>
          </div>
        </section>
      </main>
    );
  }

  const completion = onboardingCompletion(dashboardQuery.data);
  const completedCount = steps.filter((step) => completion[step.key]).length;
  const progress = Math.round((completedCount / steps.length) * 100);
  const nextStep = steps.find((step) => !completion[step.key]);

  return (
    <AppShell>
      <main className="mx-auto max-w-5xl space-y-8">
        <header className="max-w-3xl">
          <div className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">
            Comece aqui
          </div>
          <h1 className="mt-2 font-display text-4xl">Da avaliação à decisão humana</h1>
          <p className="mt-3 text-sm leading-6 text-muted-foreground">
            Acompanhe a configuração do seu workspace e avance uma etapa por vez até revisar o
            primeiro resultado. O sistema apoia a análise; a decisão continua sendo humana.
          </p>
        </header>

        {dashboardQuery.isError && (
          <StateBanner
            tone="danger"
            title="Não foi possível calcular o progresso"
            action={
              <button
                type="button"
                onClick={() => dashboardQuery.refetch()}
                className="inline-flex items-center gap-1.5 rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium"
              >
                <RefreshCw className="h-3.5 w-3.5" />
                Tentar novamente
              </button>
            }
          >
            O guia continua disponível, mas as etapas concluídas não puderam ser confirmadas agora.
          </StateBanner>
        )}

        <section className="rounded-xl border border-border bg-card p-5" aria-labelledby="onboarding-progress-title">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h2 id="onboarding-progress-title" className="text-lg font-semibold">
                Configuração inicial
              </h2>
              <p className="mt-1 text-sm text-muted-foreground">
                {dashboardQuery.isLoading
                  ? "Calculando o progresso do workspace..."
                  : `${completedCount} de ${steps.length} etapas concluídas.`}
              </p>
            </div>
            <div className="text-2xl font-semibold tabular-nums text-primary" aria-live="polite">
              {dashboardQuery.isLoading ? "—" : `${progress}%`}
            </div>
          </div>
          <div
            className="mt-4 h-2 overflow-hidden rounded-full bg-muted"
            role="progressbar"
            aria-label="Progresso da configuração inicial"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={dashboardQuery.isLoading ? 0 : progress}
          >
            <div
              className="h-full rounded-full bg-primary transition-[width]"
              style={{ width: `${dashboardQuery.isLoading ? 0 : progress}%` }}
            />
          </div>
          {!dashboardQuery.isLoading && nextStep && (
            <div className="mt-5 flex flex-col gap-3 rounded-lg border border-primary/25 bg-primary/5 p-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="text-xs font-semibold uppercase tracking-wide text-primary">
                  Próxima ação
                </div>
                <div className="mt-1 font-medium">{nextStep.title}</div>
                <p className="mt-1 text-sm text-muted-foreground">{nextStep.description}</p>
              </div>
              <Link
                to={nextStep.to}
                className="inline-flex min-h-10 shrink-0 items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                {nextStep.action}
              </Link>
            </div>
          )}
          {!dashboardQuery.isLoading && !nextStep && (
            <div className="mt-5 flex items-start gap-3 rounded-lg border border-success/30 bg-success/10 p-4">
              <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-success" />
              <div>
                <div className="font-medium">Configuração inicial concluída</div>
                <p className="mt-1 text-sm text-muted-foreground">
                  Continue acompanhando jornadas, resultados e integrações pelas telas específicas.
                </p>
              </div>
            </div>
          )}
        </section>

        <section className="grid gap-4 md:grid-cols-2">
          {steps.map((step) => {
            const complete = completion[step.key];
            return (
              <article key={step.number} className="rounded-xl border border-border bg-card p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="font-mono text-xs font-semibold text-primary">{step.number}</div>
                  <span
                    className={
                      complete
                        ? "inline-flex items-center gap-1 text-xs font-medium text-success"
                        : "inline-flex items-center gap-1 text-xs text-muted-foreground"
                    }
                  >
                    {complete ? <CheckCircle2 className="h-4 w-4" /> : <Circle className="h-4 w-4" />}
                    {complete ? "Concluída" : "Pendente"}
                  </span>
                </div>
                <h2 className="mt-2 text-lg font-semibold">{step.title}</h2>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">{step.description}</p>
                <Link
                  to={step.to}
                  className="mt-4 inline-flex rounded-md border border-border bg-background px-4 py-2 text-sm font-medium hover:bg-accent"
                >
                  {complete ? "Abrir novamente" : step.action}
                </Link>
              </article>
            );
          })}
        </section>

        <section className="grid gap-4 rounded-xl border border-border bg-card p-5 md:grid-cols-2 lg:grid-cols-4">
          <QuickLink title="Resultados" description="Revise evidências e registre a decisão humana." to="/results" />
          <QuickLink title="Integrações" description="Configure Gupy, Recrutei ou sua API própria." to="/integrations" />
          <QuickLink title="Homologação Gupy" description="Acompanhe prontidão, endpoints e evidências do fluxo real." to="/integrations/gupy-homologacao" />
          <QuickLink title="Centro operacional" description="Acompanhe tentativas, webhooks, retry e DLQ." to="/monitoramento" />
        </section>
      </main>
    </AppShell>
  );
}

function onboardingCompletion(dashboard?: DashboardResponse): Record<OnboardingStepKey, boolean> {
  if (!dashboard) {
    return {
      assessment: false,
      scenario: false,
      publish: false,
      journey: false,
      invite: false,
      result: false,
    };
  }

  const hasPublishedAssessment = dashboard.activeSimulations > 0;
  const hasJourney = dashboard.assessmentJourneys.total > 0;
  const hasCandidateActivity =
    dashboard.candidatesInProgress > 0 || dashboard.completedAttemptsLast30Days > 0;
  const hasResult =
    dashboard.completedAttemptsLast30Days > 0 || dashboard.latestResults.length > 0;

  return {
    assessment: hasPublishedAssessment,
    scenario: hasPublishedAssessment,
    publish: hasPublishedAssessment,
    journey: hasJourney,
    invite: hasCandidateActivity,
    result: hasResult,
  };
}

function QuickLink({
  title,
  description,
  to,
}: {
  title: string;
  description: string;
  to: "/results" | "/integrations" | "/integrations/gupy-homologacao" | "/monitoramento";
}) {
  return (
    <Link to={to} className="rounded-lg border border-border bg-background p-4 hover:bg-accent">
      <span className="block text-sm font-semibold">{title}</span>
      <span className="mt-1 block text-xs leading-5 text-muted-foreground">{description}</span>
    </Link>
  );
}
