import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { useSession } from "@/lib/session";

export const Route = createFileRoute("/comecar")({
  head: () => ({
    meta: [
      { title: "Comece aqui - Práxis" },
      {
        name: "description",
        content: "Entenda o fluxo do Práxis e acesse as principais ações da plataforma.",
      },
    ],
  }),
  component: StartPage,
});

const steps = [
  {
    number: "01",
    title: "Crie a avaliação",
    description:
      "Defina o cargo, a situação crítica e as competências que serão observadas.",
    to: "/nova/avaliacao" as const,
    action: "Criar avaliação",
  },
  {
    number: "02",
    title: "Monte o cenário",
    description:
      "Cadastre etapas e alternativas. Cada resposta recebe critérios e pontuações definidos antes da aplicação.",
    to: "/avaliacoes" as const,
    action: "Ver avaliações",
  },
  {
    number: "03",
    title: "Revise e publique",
    description:
      "O validador bloqueia falhas estruturais e caminhos que tornariam os resultados incomparáveis.",
    to: "/avaliacoes" as const,
    action: "Continuar uma avaliação",
  },
  {
    number: "04",
    title: "Monte a jornada",
    description:
      "Organize uma ou mais avaliações na ordem em que a pessoa candidata deverá realizá-las.",
    to: "/jornadas" as const,
    action: "Criar jornada",
  },
  {
    number: "05",
    title: "Convide e acompanhe",
    description:
      "Publique a jornada, gere o convite da pessoa participante e acompanhe o andamento do processo.",
    to: "/jornadas" as const,
    action: "Abrir jornadas",
  },
];

function StartPage() {
  const session = useSession();

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

  return (
    <AppShell>
      <main className="mx-auto max-w-5xl space-y-8">
        <header className="max-w-3xl">
          <div className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">
            Comece aqui
          </div>
          <h1 className="mt-2 font-display text-4xl">Da avaliação à jornada e à decisão humana</h1>
          <p className="mt-3 text-sm leading-6 text-muted-foreground">
            A avaliação define o conteúdo do teste. A jornada organiza como esse conteúdo será
            aplicado às pessoas participantes. O sistema apoia a análise; a decisão sobre a
            candidatura continua sendo de uma pessoa.
          </p>
        </header>

        <section className="grid gap-4 md:grid-cols-2">
          {steps.map((step) => (
            <article key={step.number} className="rounded-xl border border-border bg-card p-5">
              <div className="font-mono text-xs font-semibold text-primary">{step.number}</div>
              <h2 className="mt-2 text-lg font-semibold">{step.title}</h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">{step.description}</p>
              <Link
                to={step.to}
                className="mt-4 inline-flex rounded-md border border-border bg-background px-4 py-2 text-sm font-medium hover:bg-accent"
              >
                {step.action}
              </Link>
            </article>
          ))}
        </section>

        <section className="grid gap-4 rounded-xl border border-border bg-card p-5 md:grid-cols-3">
          <QuickLink title="Resultados" description="Revise evidências e registre a decisão humana." to="/results" />
          <QuickLink title="Integrações" description="Configure Gupy, Recrutei ou sua API própria." to="/integrations" />
          <QuickLink title="Centro operacional" description="Acompanhe tentativas, webhooks, retry e DLQ." to="/monitoramento" />
        </section>
      </main>
    </AppShell>
  );
}

function QuickLink({
  title,
  description,
  to,
}: {
  title: string;
  description: string;
  to: "/results" | "/integrations" | "/monitoramento";
}) {
  return (
    <Link to={to} className="rounded-lg border border-border bg-background p-4 hover:bg-accent">
      <span className="block text-sm font-semibold">{title}</span>
      <span className="mt-1 block text-xs leading-5 text-muted-foreground">{description}</span>
    </Link>
  );
}
