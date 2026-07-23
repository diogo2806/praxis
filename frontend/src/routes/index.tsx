import { createFileRoute } from "@tanstack/react-router";

import { LandingFaqSection, LandingPricingSection } from "../components/landing-commercial";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Práxis: Avaliações por cenários estruturadas e rastreáveis" },
      {
        name: "description",
        content:
          "Crie avaliações por cenários, configure critérios e pesos, compartilhe por link e acompanhe respostas, indicadores e registros do percurso.",
      },
    ],
    links: [
      { rel: "preconnect", href: "https://fonts.googleapis.com" },
      { rel: "preconnect", href: "https://fonts.gstatic.com", crossOrigin: "anonymous" },
      {
        rel: "stylesheet",
        href: "https://fonts.googleapis.com/css2?family=Source+Serif+4:ital,opsz,wght@0,8..60,400;0,8..60,500;0,8..60,600;0,8..60,700;1,8..60,400&family=IBM+Plex+Sans:wght@400;500;600;700&display=swap",
      },
    ],
  }),
  component: LandingPage,
});

const applications = [
  {
    title: "Seleção e mobilidade interna",
    description:
      "Avalie respostas a situações relacionadas a uma função, atividade ou contexto profissional.",
  },
  {
    title: "Capacitação e desenvolvimento",
    description:
      "Observe como participantes aplicam conhecimentos e critérios em situações simuladas.",
  },
  {
    title: "Atendimento e operações",
    description:
      "Represente ocorrências, decisões, procedimentos e consequências presentes na rotina das equipes.",
  },
  {
    title: "Procedimentos e conformidade",
    description:
      "Avalie a compreensão e a aplicação de orientações internas por meio de situações contextualizadas.",
  },
] as const;

const governanceItems = [
  {
    title: "Trilha de decisão",
    description:
      "Eventos da tentativa, respostas, tempos e finalização ficam disponíveis para análise e auditoria.",
  },
  {
    title: "Critérios versionados",
    description:
      "Pesos, marcadores e regras permanecem vinculados à versão usada em cada aplicação.",
  },
  {
    title: "Revisão humana",
    description:
      "A plataforma organiza evidências para apoiar a equipe responsável, sem substituir a decisão final.",
  },
] as const;

function LandingPage() {
  return (
    <div className="min-h-screen bg-[#fbfaf6] text-slate-900 [font-family:'IBM_Plex_Sans',sans-serif]">
      <header className="sticky top-0 z-30 border-b border-slate-200/80 bg-[#fbfaf6]/95 backdrop-blur">
        <div className="mx-auto flex min-h-16 max-w-6xl flex-wrap items-center justify-between gap-4 px-5 py-3 sm:px-8">
          <a href="#topo" className="font-serif text-2xl font-semibold tracking-tight">
            Práxis <span className="text-amber-500">•</span>
          </a>
          <nav aria-label="Navegação principal" className="flex flex-wrap items-center justify-end gap-x-5 gap-y-2 text-sm font-medium text-slate-600">
            <a className="transition hover:text-sky-700" href="#como-funciona">
              Como funciona
            </a>
            <a className="transition hover:text-sky-700" href="#aplicacoes">
              Aplicações
            </a>
            <a className="transition hover:text-sky-700" href="#governanca">
              Governança
            </a>
            <a className="transition hover:text-sky-700" href="#contratacao">
              Planos
            </a>
            <a
              className="rounded-full bg-slate-900 px-4 py-2 font-semibold text-white transition hover:bg-sky-800"
              href="/login"
            >
              Entrar
            </a>
          </nav>
        </div>
      </header>

      <main id="topo">
        <section className="relative overflow-hidden px-5 py-20 sm:px-8 sm:py-28">
          <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_top_right,rgba(14,165,233,0.14),transparent_34%),radial-gradient(circle_at_top_left,rgba(245,158,11,0.12),transparent_30%)]" />
          <div className="mx-auto grid max-w-6xl items-center gap-12 lg:grid-cols-[1.1fr_0.9fr]">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-sky-700">
                Avaliações situacionais estruturadas
              </p>
              <h1 className="mt-5 max-w-4xl font-serif text-5xl font-medium leading-[1.05] tracking-tight text-slate-950 sm:text-7xl">
                Observe decisões em cenários próximos da realidade.
              </h1>
              <p className="mt-7 max-w-2xl text-lg leading-8 text-slate-600 sm:text-xl">
                Crie cenários interativos, configure critérios e pesos, compartilhe por link e acompanhe
                indicadores e registros do percurso.
              </p>
              <div className="mt-9 flex flex-wrap gap-3">
                <a
                  className="inline-flex min-h-12 items-center justify-center rounded-full bg-sky-700 px-6 font-semibold text-white shadow-lg shadow-sky-950/15 transition hover:bg-sky-800"
                  href="mailto:contato@iforce.com.br?subject=Demonstra%C3%A7%C3%A3o%20da%20Pr%C3%A1xis"
                >
                  Solicitar demonstração
                </a>
                <a
                  className="inline-flex min-h-12 items-center justify-center rounded-full border border-slate-300 bg-white px-6 font-semibold text-slate-800 transition hover:border-sky-700 hover:text-sky-700"
                  href="#contratacao"
                >
                  Ver planos
                </a>
              </div>
            </div>

            <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl shadow-slate-950/10 sm:p-8">
              <div className="flex items-center justify-between gap-4 border-b border-slate-100 pb-5">
                <div>
                  <p className="text-sm font-semibold text-slate-900">Cenário de atendimento</p>
                  <p className="text-xs text-slate-500">Turno 1 de 3</p>
                </div>
                <span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-800">
                  Situação crítica
                </span>
              </div>
              <p className="mt-6 font-serif text-2xl leading-9 text-slate-900">
                Um cliente relata atraso e exige uma solução imediata que depende de outra equipe. Como você
                responde?
              </p>
              <div className="mt-6 space-y-3">
                {[
                  "Prometo resolver ainda hoje, mesmo sem confirmação.",
                  "Acolho a reclamação, assumo o acompanhamento e alinho o prazo com a equipe responsável.",
                  "Informo apenas o prazo padrão previsto na política.",
                ].map((answer, index) => (
                  <div
                    key={answer}
                    className={
                      index === 1
                        ? "flex gap-3 rounded-xl border-2 border-sky-700 bg-sky-50 p-4 text-sm leading-6 text-slate-800"
                        : "flex gap-3 rounded-xl border border-slate-200 p-4 text-sm leading-6 text-slate-600"
                    }
                  >
                    <span className="font-semibold text-sky-800">{String.fromCharCode(65 + index)}</span>
                    <span>{answer}</span>
                  </div>
                ))}
              </div>
              <div className="mt-6 grid grid-cols-3 gap-3 text-center text-xs">
                <Indicator label="Comunicação" value="+3" />
                <Indicator label="Conflitos" value="+2" />
                <Indicator label="Aderência" value="+1" />
              </div>
            </div>
          </div>
        </section>

        <section id="como-funciona" className="bg-white px-5 py-20 sm:px-8">
          <div className="mx-auto max-w-6xl">
            <div className="mx-auto max-w-3xl text-center">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-700">Como funciona</p>
              <h2 className="mt-3 font-serif text-3xl font-semibold tracking-tight sm:text-5xl">
                Da criação do cenário à análise das evidências.
              </h2>
            </div>
            <div className="mt-12 grid gap-6 md:grid-cols-3">
              <ProcessCard number="01" title="Configure" description="Defina cenários, alternativas, critérios, pesos, tempos e marcadores que exigem revisão." />
              <ProcessCard number="02" title="Compartilhe" description="Publique a avaliação e envie o acesso por link, sem depender de integração com outro sistema." />
              <ProcessCard number="03" title="Analise" description="Acompanhe indicadores, respostas e a trilha cronológica usada para compor o resultado." />
            </div>
          </div>
        </section>

        <section id="aplicacoes" className="px-5 py-20 sm:px-8">
          <div className="mx-auto max-w-6xl">
            <div className="max-w-3xl">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-700">Onde se aplica</p>
              <h2 className="mt-3 font-serif text-3xl font-semibold tracking-tight sm:text-5xl">
                Uma estrutura adaptável a diferentes contextos.
              </h2>
              <p className="mt-5 text-lg leading-8 text-slate-600">
                A aplicação depende dos cenários, critérios e conteúdos definidos pela organização responsável.
              </p>
            </div>
            <div className="mt-10 grid gap-5 sm:grid-cols-2">
              {applications.map((application) => (
                <article key={application.title} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
                  <h3 className="text-lg font-semibold">{application.title}</h3>
                  <p className="mt-3 leading-7 text-slate-600">{application.description}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section id="governanca" className="bg-slate-950 px-5 py-20 text-white sm:px-8">
          <div className="mx-auto max-w-6xl">
            <div className="max-w-3xl">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-300">Governança</p>
              <h2 className="mt-3 font-serif text-3xl font-semibold tracking-tight sm:text-5xl">
                Recursos para apoiar privacidade, revisão e rastreabilidade.
              </h2>
            </div>
            <div className="mt-12 grid gap-6 md:grid-cols-3">
              {governanceItems.map((item) => (
                <article key={item.title} className="rounded-2xl border border-slate-700 bg-slate-900 p-6">
                  <h3 className="text-lg font-semibold">{item.title}</h3>
                  <p className="mt-3 leading-7 text-slate-300">{item.description}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <LandingPricingSection />
        <LandingFaqSection />

        <section className="px-5 py-20 text-center sm:px-8">
          <div className="mx-auto max-w-4xl rounded-3xl bg-sky-800 px-6 py-14 text-white shadow-2xl shadow-sky-950/20 sm:px-12">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-200">Vamos conversar</p>
            <h2 className="mt-4 font-serif text-3xl font-semibold tracking-tight sm:text-5xl">
              Estruture cenários, critérios e evidências em um só fluxo.
            </h2>
            <p className="mx-auto mt-5 max-w-2xl text-lg leading-8 text-sky-100">
              Conheça a criação de avaliações, a participação por link e a análise dos resultados em uma
              demonstração da Práxis.
            </p>
            <a
              className="mt-8 inline-flex min-h-12 items-center justify-center rounded-full bg-white px-6 font-semibold text-sky-900 transition hover:bg-sky-50"
              href="mailto:contato@iforce.com.br?subject=Demonstra%C3%A7%C3%A3o%20da%20Pr%C3%A1xis"
            >
              Solicitar demonstração
            </a>
          </div>
        </section>
      </main>

      <footer className="border-t border-slate-200 bg-white px-5 py-8 sm:px-8">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 text-sm text-slate-500">
          <a href="#topo" className="font-serif text-xl font-semibold text-slate-900">
            Práxis <span className="text-amber-500">•</span>
          </a>
          <span>© 2026 iForce · praxis.iforce.com.br</span>
        </div>
      </footer>
    </div>
  );
}

function Indicator({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-slate-50 p-3">
      <div className="font-semibold text-sky-700">{value}</div>
      <div className="mt-1 text-slate-500">{label}</div>
    </div>
  );
}

function ProcessCard({ number, title, description }: { number: string; title: string; description: string }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-slate-50 p-6">
      <span className="text-sm font-semibold text-sky-700">{number}</span>
      <h3 className="mt-4 text-xl font-semibold">{title}</h3>
      <p className="mt-3 leading-7 text-slate-600">{description}</p>
    </article>
  );
}
