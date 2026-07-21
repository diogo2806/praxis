import { createFileRoute, Link } from "@tanstack/react-router";
import {
  BookOpenCheck,
  ChevronRight,
  CircleUserRound,
  FilePlus2,
  HelpCircle,
  ListChecks,
  LockKeyhole,
  ShieldCheck,
} from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { useSession } from "@/lib/session";

export const Route = createFileRoute("/avaliacoes/especialista")({
  head: () => ({
    meta: [
      { title: "Área do especialista - Práxis" },
      {
        name: "description",
        content: "Área de trabalho do especialista parceiro para criar, editar e revisar avaliações.",
      },
    ],
  }),
  component: PartnerSpecialistPage,
});

const quickActions = [
  {
    to: "/avaliacoes",
    title: "Minhas avaliações",
    description: "Consulte os rascunhos e continue a edição das avaliações disponíveis.",
    icon: ListChecks,
  },
  {
    to: "/nova/avaliacao",
    title: "Criar avaliação",
    description: "Inicie um novo rascunho e avance pelo fluxo de construção e revisão.",
    icon: FilePlus2,
  },
  {
    to: "/competencias",
    title: "Consultar competências",
    description: "Veja o catálogo definido pela empresa para utilizar nas avaliações.",
    icon: BookOpenCheck,
  },
  {
    to: "/configuracoes/conta",
    title: "Minha conta",
    description: "Atualize seus dados pessoais e as configurações do seu acesso.",
    icon: CircleUserRound,
  },
  {
    to: "/manual",
    title: "Central de manuais",
    description: "Consulte o processo completo e as orientações de cada tela.",
    icon: HelpCircle,
  },
] as const;

function PartnerSpecialistPage() {
  const session = useSession();
  const firstName = session.userName.trim().split(/\s+/)[0] || "Especialista";

  return (
    <AppShell>
      <div className="mx-auto max-w-6xl space-y-8">
        <header className="rounded-2xl border border-primary/25 bg-primary/5 p-6 sm:p-8">
          <div className="flex flex-wrap items-start justify-between gap-5">
            <div className="max-w-3xl">
              <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-primary/25 bg-background px-3 py-1 text-xs font-semibold text-primary">
                <ShieldCheck className="h-4 w-4" />
                Área do especialista parceiro
              </div>
              <h1 className="text-3xl font-semibold tracking-tight text-foreground">
                Olá, {firstName}
              </h1>
              <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-foreground sm:text-base">
                Use esta área para criar, editar e revisar rascunhos de avaliações. As ações de
                publicação e administração permanecem sob responsabilidade da empresa parceira.
              </p>
            </div>
            <Link
              to="/nova/avaliacao"
              className="inline-flex min-h-11 items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90"
            >
              <FilePlus2 className="h-4 w-4" />
              Criar avaliação
            </Link>
          </div>
        </header>

        <section aria-labelledby="specialist-actions-title">
          <div className="mb-4">
            <h2 id="specialist-actions-title" className="text-xl font-semibold text-foreground">
              O que você pode fazer
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Acesse somente as funções liberadas para o perfil de especialista.
            </p>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {quickActions.map((action) => (
              <Link
                key={action.to}
                to={action.to}
                className="group flex min-h-40 flex-col rounded-xl border border-border bg-card p-5 transition hover:border-primary/40 hover:bg-accent/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <action.icon className="h-5 w-5" />
                  </div>
                  <ChevronRight className="h-5 w-5 text-muted-foreground transition group-hover:translate-x-1 group-hover:text-primary" />
                </div>
                <h3 className="mt-4 font-semibold text-foreground">{action.title}</h3>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">{action.description}</p>
              </Link>
            ))}
          </div>
        </section>

        <section className="grid gap-4 lg:grid-cols-2" aria-label="Responsabilidades do perfil">
          <article className="rounded-xl border border-border bg-card p-5">
            <h2 className="flex items-center gap-2 text-lg font-semibold text-foreground">
              <ShieldCheck className="h-5 w-5 text-primary" />
              Permitido ao especialista
            </h2>
            <ul className="mt-4 space-y-3 text-sm leading-6 text-muted-foreground">
              <li>Criar novos rascunhos de avaliações.</li>
              <li>Editar contexto, personagem, diálogo, pontuação e mapa do fluxo.</li>
              <li>Executar a validação estrutural e corrigir bloqueios.</li>
              <li>Consultar as competências disponibilizadas pela empresa.</li>
            </ul>
          </article>

          <article className="rounded-xl border border-border bg-card p-5">
            <h2 className="flex items-center gap-2 text-lg font-semibold text-foreground">
              <LockKeyhole className="h-5 w-5 text-muted-foreground" />
              Responsabilidade da empresa
            </h2>
            <ul className="mt-4 space-y-3 text-sm leading-6 text-muted-foreground">
              <li>Publicar, arquivar ou duplicar avaliações.</li>
              <li>Criar rascunhos a partir de versões já publicadas.</li>
              <li>Administrar clientes, equipe, integrações, plano e cobrança.</li>
              <li>Acessar participações, resultados, monitoramento e calibração.</li>
            </ul>
          </article>
        </section>
      </div>
    </AppShell>
  );
}
