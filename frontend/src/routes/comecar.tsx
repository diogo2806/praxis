import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { CheckCircle2, Circle, RefreshCw } from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { getDashboard } from "@/lib/api/dashboard-strict";
import { isOnboardingComplete, onboardingCompletion, type OnboardingStepKey } from "@/lib/onboarding";
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

const steps: Array<{
  key: OnboardingStepKey;
  number: string;
  title: string;
  description: string;
  to: "/nova/avaliacao" | "/avaliacoes" | "/jornadas" | "/participacoes/jornada" | "/results";
  action: string;
}> = [
  {
    key: "assessment",
    number: "01",
    title: "Crie a avaliação",
    description: "Defina o cargo, a situação crítica e as competências que serão observadas.",
    to: "/nova/avaliacao",
    action: "Criar avaliação",
  },
  {
    key: "scenario",
    number: "02",
    title: "Monte o cenário",
    description: "Cadastre etapas e alternativas com critérios definidos antes da aplicação.",
    to: "/avaliacoes",
    action: "Ver avaliações",
  },
  {
    key: "publish",
    number: "03",
    title: "Revise e publique",
    description: "Corrija os bloqueios do validador e publique a versão pronta.",
    to: "/avaliacoes",
    action: "Continuar avaliação",
  },
  {
    key: "journey",
    number: "04",
    title: "Monte a jornada",
    description: "Organize uma ou mais avaliações na ordem do processo seletivo.",
    to: "/jornadas",
    action: "Criar jornada",
  },
  {
    key: "invite",
    number: "05",
    title: "Convide e acompanhe",
    description: "Selecione a jornada publicada, gere o convite e acompanhe a participação.",
    to: "/participacoes/jornada",
    action: "Convidar por jornada",
  },
  {
    key: "result",
    number: "06",
    title: "Revise o primeiro resultado",
    description: "Abra as evidências e registre a decisão humana sobre a candidatura.",
    to: "/results",
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
            <Link to="/login" className="rounded-md bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90">
              Entrar
            </Link>
            <Link to="/" className="rounded-md border border-border bg-card px-5 py-2.5 text-sm font-medium hover:bg-accent">
              Conhecer o Práxis
            </Link>
          </div>
        </section>
      </main>
    );
  }

  const completion = onboardingCompletion(dashboardQuery.data);
  const completed = isOnboardingComplete(dashboardQuery.data);
  const completedCount = steps.filter((step) => completion[step.key]).length;
  const progress = Math.round((completedCount / steps.length) * 100);
  const nextStep = steps.find((step) => !completion[step.key]);

  if (!dashboardQuery.isLoading && completed) {
    return (
      <AppShell>
        <main className="mx-auto max-w-3xl">
          <section className="rounded-xl border border-success/30 bg-card p-8 text-center shadow-sm">
            <CheckCircle2 className="mx-auto h-12 w-12 text-success" />
            <div className="mt-4 text-xs font-semibold uppercase tracking-[0.18em] text-success">
              Onboarding concluído
            </div>
            <h1 className="mt-2 font-display text-3xl">Seu primeiro ciclo está completo</h1>
            <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-muted-foreground">
              O guia não permanece na navegação depois da conclusão. Continue pelo Dashboard ou
              consulte o processo completo na Central de manuais quando precisar revisar as etapas.
            </p>
            <div className="mt-6 flex flex-wrap justify-center gap-3">
              <Link to="/dashboard" className="rounded-md bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90">
                Ir para o Dashboard
              </Link>
              <a href="/manual#onboarding-inicial" className="rounded-md border border-border bg-background px-5 py-2.5 text-sm font-medium hover:bg-accent">
                Revisar processo completo
              </a>
            </div>
          </section>
        </main>
      </AppShell>
    );
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-5xl space-y-8">
        <header className="max-w-3xl">
          <div className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">Comece aqui</div>
          <h1 className="mt-2 font-display text-4xl">Da avaliação à decisão humana</h1>
          <p className="mt-3 text-sm leading-6 text-muted-foreground">
            Avance uma etapa por vez até revisar o primeiro resultado. Depois da conclusão, este guia
            deixa a navegação e permanece disponível na Central de manuais.
          </p>
        </header>

        {dashboardQuery.isError && (
          <StateBanner
            tone="danger"
            title="Não foi possível calcular o progresso"
            action={
              <button type="button" onClick={() => dashboardQuery.refetch()} className="inline-flex items-center gap-1.5 rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium">
                <RefreshCw className="h-3.5 w-3.5" />Tentar novamente
              </button>
            }
          >
            O guia continua disponível, mas as etapas concluídas não puderam ser confirmadas agora.
          </StateBanner>
        )}

        <section className="rounded-xl border border-border bg-card p-5" aria-labelledby="onboarding-progress-title">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h2 id="onboarding-progress-title" className="text-lg font-semibold">Configuração inicial</h2>
              <p className="mt-1 text-sm text-muted-foreground">
                {dashboardQuery.isLoading ? "Calculando o progresso do workspace..." : `${completedCount} de ${steps.length} etapas concluídas.`}
              </p>
            </div>
            <div className="text-2xl font-semibold tabular-nums text-primary" aria-live="polite">
              {dashboardQuery.isLoading ? "—" : `${progress}%`}
            </div>
          </div>
          <div className="mt-4 h-2 overflow-hidden rounded-full bg-muted" role="progressbar" aria-label="Progresso da configuração inicial" aria-valuemin={0} aria-valuemax={100} aria-valuenow={dashboardQuery.isLoading ? 0 : progress}>
            <div className="h-full rounded-full bg-primary transition-[width]" style={{ width: `${dashboardQuery.isLoading ? 0 : progress}%` }} />
          </div>
          {!dashboardQuery.isLoading && nextStep && (
            <div className="mt-5 flex flex-col gap-3 rounded-lg border border-primary/25 bg-primary/5 p-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="text-xs font-semibold uppercase tracking-wide text-primary">Próxima ação</div>
                <div className="mt-1 font-medium">{nextStep.title}</div>
                <p className="mt-1 text-sm text-muted-foreground">{nextStep.description}</p>
              </div>
              <Link to={nextStep.to} className="inline-flex min-h-10 shrink-0 items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
                {nextStep.action}
              </Link>
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
                  <span className={complete ? "inline-flex items-center gap-1 text-xs font-medium text-success" : "inline-flex items-center gap-1 text-xs text-muted-foreground"}>
                    {complete ? <CheckCircle2 className="h-4 w-4" /> : <Circle className="h-4 w-4" />}
                    {complete ? "Concluída" : "Pendente"}
                  </span>
                </div>
                <h2 className="mt-2 text-lg font-semibold">{step.title}</h2>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">{step.description}</p>
                <Link to={step.to} className="mt-4 inline-flex rounded-md border border-border bg-background px-4 py-2 text-sm font-medium hover:bg-accent">
                  {complete ? "Abrir novamente" : step.action}
                </Link>
              </article>
            );
          })}
        </section>
      </main>
    </AppShell>
  );
}
