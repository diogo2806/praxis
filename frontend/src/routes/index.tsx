import { useState, type ComponentType } from "react";
import { createFileRoute } from "@tanstack/react-router";
import {
  Accessibility,
  ArrowRight,
  CheckCircle2,
  Eye,
  GitBranch,
  Keyboard,
  Library,
  Lock,
  Plug,
  Scale,
  ScrollText,
  ShieldCheck,
  Sparkles,
  Timer,
  UserCheck,
  X,
} from "lucide-react";

type HeroOption = {
  k: string;
  text: string;
  tone: "good" | "warn" | "bad";
  react: string;
  scores: Record<string, number>;
};

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Práxis - Teste situacional para Gupy" },
      {
        name: "description",
        content:
          "Landing pública da Práxis: teste situacional determinística, integrada à Gupy, com governança, acessibilidade e trilha auditável.",
      },
    ],
  }),
  component: LandingPage,
});

const heroOptions: HeroOption[] = [
  {
    k: "A",
    text: "Pedir desculpas e prometer retorno em 30min, sem confirmar com o time.",
    tone: "warn",
    react:
      '"Tá, mas e se não resolver de novo?" Você prometeu prazo sem validar com o time. Isso liga um alerta de aderência.',
    scores: { Empatia: 72, Resolução: 48, "Aderência à política": 30 },
  },
  {
    k: "B",
    text: "Reconhecer a frustração, confirmar o número do chamado e dar prazo realista alinhado ao time.",
    tone: "good",
    react:
      '"Obrigado por pelo menos me dar um prazo de verdade." Você acolheu, confirmou o caso e alinhou expectativa.',
    scores: { Empatia: 90, Resolução: 86, "Aderência à política": 80 },
  },
  {
    k: "C",
    text: "Explicar a política interna de SLA e pedir paciência até o próximo ciclo.",
    tone: "warn",
    react:
      '"Política? Eu só quero meu problema resolvido!" Correto no processo, mas frio para o momento do cliente.',
    scores: { Empatia: 28, Resolução: 60, "Aderência à política": 82 },
  },
  {
    k: "D",
    text: "Encaminhar direto para o supervisor sem tentar resolver.",
    tone: "bad",
    react:
      '"De novo sou empurrado para outra pessoa?" Você pulou a tentativa de resolução e a leitura do caso.',
    scores: { Empatia: 40, Resolução: 32, "Aderência à política": 45 },
  },
];

const features = [
  {
    icon: Sparkles,
    title: "Sem IA julgando candidato",
    body: "A pontuação vem de critérios, pesos e cálculo. Zero caixa-preta, zero custo de IA, totalmente explicável.",
  },
  {
    icon: Scale,
    title: "Score justo entre caminhos",
    body: "A nota é normalizada pelo caminho percorrido: quem decide rápido não é penalizado por um cenário mais curto.",
  },
  {
    icon: ScrollText,
    title: "Trilha auditável",
    body: "Cada ponto tem origem: qual etapa, qual escolha, qual rubrica. Pronto para o gestor e para o jurídico.",
  },
  {
    icon: UserCheck,
    title: "Decide, não reprova",
    body: "Erro crítico aciona revisão humana. A decisão final é sempre de uma pessoa.",
  },
  {
    icon: Library,
    title: "Biblioteca de cenários",
    body: "Modelos prontos por área e senioridade. O RH edita, testa com um piloto e publica quando estiver pronto.",
  },
  {
    icon: GitBranch,
    title: "Dentro da Gupy",
    body: "O gestor não troca de ferramenta. Nota e competências chegam direto na candidatura.",
  },
];

const governance = [
  {
    icon: ShieldCheck,
    title: "Trilha imutável",
    body: "Cada evento da tentativa fica registrado: criação, resposta, timeout e finalização.",
  },
  {
    icon: Lock,
    title: "LGPD por desenho",
    body: "Bases legais expostas, retenção configurável e anonimização programada após o ciclo da seleção.",
  },
  {
    icon: Scale,
    title: "Defensabilidade",
    body: "Pesos versionados e critérios visíveis permitem reconstruir por que cada candidato recebeu cada ponto.",
  },
];

const accessibility = [
  {
    icon: Eye,
    title: "Contraste e legibilidade",
    body: "Tipografia e cores seguem WCAG 2.1 AA: contraste mínimo em texto e interface.",
  },
  {
    icon: Keyboard,
    title: "Operável por teclado",
    body: "O teste é navegável com Tab, Shift+Tab e Enter, sem armadilha de foco.",
  },
  {
    icon: Accessibility,
    title: "Leitores de tela",
    body: "Enunciados, alternativas e cronômetro expostos com ARIA correto e idioma pt-BR.",
  },
  {
    icon: Timer,
    title: "Tempo ajustável",
    body: "Limite por etapa pode ser estendido conforme a LBI, sem afetar o cálculo da pontuação.",
  },
];

const faq = [
  {
    q: "O Práxis usa IA generativa para avaliar o candidato?",
    a: "Não. O teste é determinística: a pontuação sai de critérios e pesos versionados definidos pela sua equipe.",
  },
  {
    q: "Como funciona a integração com a Gupy?",
    a: "Sim. A Práxis se conecta à Gupy para listar testes, abrir convites de candidatos e devolver resultados automaticamente com retentativas quando há falha temporária.",
  },
  {
    q: "Funciona com outros sistemas além da Gupy?",
    a: "O foco operacional hoje é a Gupy. Outras integrações só entram quando houver contrato, cliente real e validação fim a fim.",
  },
  {
    q: "O candidato vê pesos, gabarito ou marcadores críticos?",
    a: "Nunca. A visão do candidato é limpa. Pesos, critérios e marcadores ficam restritos ao painel admin e à trilha de auditoria.",
  },
  {
    q: "O teste é acessível?",
    a: "Sim. Há navegação por teclado, foco visível, contraste AA, marcação semântica e suporte a leitor de tela.",
  },
  {
    q: "O Práxis reprova alguém sozinho?",
    a: "Não. O Práxis recomenda, nunca elimina automaticamente. Erro crítico aciona revisão humana.",
  },
];

const demoEmailUser = "contato";
const demoEmailDomain = "iforce.com.br";
const demoEmailSubject = "Quero uma demonstração Práxis";

function openDemoEmail() {
  const params = new URLSearchParams({ subject: demoEmailSubject });
  window.location.href = `mailto:${demoEmailUser}@${demoEmailDomain}?${params.toString()}`;
}

function Brand() {
  return (
    <a href="#topo" className="flex min-h-0 items-center gap-2.5">
      <span className="inline-flex h-9 w-9 items-center justify-center rounded-md bg-primary text-primary-foreground">
        <Sparkles className="h-4 w-4" aria-hidden />
      </span>
      <span className="font-display text-lg leading-none">
        Práxis
        <span className="ml-1.5 text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
          by iForce
        </span>
      </span>
    </a>
  );
}

function LandingPage() {
  const [picked, setPicked] = useState<string | null>(null);
  const chosen = heroOptions.find((option) => option.k === picked) ?? null;

  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="sticky top-0 z-30 border-b border-border bg-background/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-5 px-5">
          <Brand />
          <nav className="hidden items-center gap-6 text-sm text-muted-foreground md:flex">
            <a className="hover:text-foreground" href="#problema">
              Por que
            </a>
            <a className="hover:text-foreground" href="#como">
              Como funciona
            </a>
            <a className="hover:text-foreground" href="#gupy">
              Integração Gupy
            </a>
            <a className="hover:text-foreground" href="#governanca">
              Governança
            </a>
          </nav>
          <a
            href="#cta"
            className="inline-flex items-center gap-1.5 rounded-md bg-foreground px-4 py-2 text-sm font-semibold text-background hover:opacity-90"
          >
            Falar com a gente
          </a>
        </div>
      </header>

      <main>
        <section id="topo" className="relative overflow-hidden">
          <div className="pointer-events-none absolute inset-x-0 top-0 h-80 bg-[radial-gradient(circle_at_50%_0%,var(--color-primary)/0.18,transparent_58%)]" />
          <div className="relative mx-auto grid max-w-6xl items-center gap-12 px-5 py-16 lg:grid-cols-[1.05fr_1fr] lg:py-20">
            <div>
              <span className="inline-flex w-fit items-center gap-2 rounded-full bg-accent px-3 py-1.5 text-xs font-semibold text-primary">
                <span className="h-2 w-2 rounded-full bg-success" />
                Teste situacional, sem IA julgando candidato
              </span>
              <h1 className="mt-5 max-w-3xl font-display text-4xl font-extrabold leading-[1.05] tracking-tight text-foreground md:text-6xl">
                Veja como o candidato decide no cenário real do trabalho.
              </h1>
              <p className="mt-5 max-w-xl text-base leading-relaxed text-muted-foreground md:text-lg">
                Antes de gastar a agenda do gestor com entrevista, a Práxis mostra a decisão da
                pessoa numa situação real do cargo, com pontuação por competência e trilha
                auditável.
              </p>
              <div className="mt-7 flex flex-wrap gap-3">
                <a
                  href="#cta"
                  className="inline-flex items-center gap-2 rounded-md bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/20 hover:bg-primary/90"
                >
                  Agendar demonstração
                  <ArrowRight className="h-4 w-4" aria-hidden />
                </a>
                <a
                  href="#demo"
                  className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-6 py-3 text-sm font-semibold hover:bg-accent"
                >
                  Ver como funciona
                </a>
              </div>
              <p className="mt-5 flex items-center gap-2 text-xs text-muted-foreground">
                <CheckCircle2 className="h-3.5 w-3.5 text-success" aria-hidden />
                Compatível com a API de testes da Gupy. O resultado volta para dentro da plataforma.
              </p>
            </div>

            <section id="demo" aria-label="Demonstração interativa">
              <div className="overflow-hidden rounded-lg border border-border bg-card shadow-2xl shadow-sidebar/15">
                <div className="flex items-center justify-between bg-foreground px-4 py-3 text-background">
                  <div className="flex items-center gap-3">
                    <span className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-warning text-xs font-bold text-warning-foreground">
                      CM
                    </span>
                    <div className="leading-tight">
                      <div className="text-sm font-semibold">Carlos M.</div>
                      <div className="text-[10px] uppercase tracking-wider opacity-70">
                        Etapa 1/3 · Abertura
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-1.5 text-xs tabular-nums opacity-90">
                    <span className="h-2 w-2 rounded-full bg-warning" />
                    00:24
                  </div>
                </div>
                <div className="space-y-5 p-5">
                  <div>
                    <div className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                      Cliente, furioso
                    </div>
                    <p className="mt-1.5 max-w-[92%] rounded-md bg-accent px-4 py-3 text-sm leading-relaxed">
                      "Já é a terceira vez que abro chamado e ninguém resolve. Preciso disso hoje,
                      ou vou escalar para cima."
                    </p>
                  </div>
                  <div
                    className="flex flex-col gap-2"
                    role="radiogroup"
                    aria-label="Escolha uma resposta"
                  >
                    {heroOptions.map((option) => {
                      const isPicked = picked === option.k;
                      return (
                        <button
                          key={option.k}
                          type="button"
                          role="radio"
                          aria-checked={isPicked}
                          disabled={picked !== null}
                          onClick={() => setPicked(option.k)}
                          className={`flex items-start gap-3 rounded-md border px-3.5 py-3 text-left text-sm transition disabled:cursor-default ${
                            isPicked
                              ? "border-primary bg-primary/10"
                              : "border-border bg-card hover:border-primary/60 hover:bg-primary/5"
                          } ${picked !== null && !isPicked ? "opacity-50" : ""}`}
                        >
                          <span className="mt-0.5 inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-accent text-xs font-bold text-primary">
                            {option.k}
                          </span>
                          <span className="leading-snug">{option.text}</span>
                        </button>
                      );
                    })}
                  </div>

                  {chosen && (
                    <div className="animate-in fade-in slide-in-from-bottom-2">
                      <div
                        className={`rounded-md border-l-[3px] px-3.5 py-3 text-sm ${
                          chosen.tone === "good"
                            ? "border-success bg-success/10"
                            : chosen.tone === "warn"
                              ? "border-warning bg-warning/10"
                              : "border-destructive bg-destructive/10"
                        }`}
                        aria-live="polite"
                      >
                        <div className="mb-1 text-[10px] font-bold uppercase tracking-wider text-muted-foreground">
                          Cliente reage
                        </div>
                        {chosen.react}
                      </div>
                      <div className="mt-4 space-y-2">
                        {Object.entries(chosen.scores).map(([label, value]) => (
                          <div
                            key={label}
                            className="grid grid-cols-[minmax(6.5rem,8rem)_1fr_2.5rem] items-center gap-3 text-xs"
                          >
                            <span className="text-muted-foreground">{label}</span>
                            <span className="h-1.5 overflow-hidden rounded-full bg-accent">
                              <span
                                className="block h-full rounded-full bg-primary transition-all duration-700"
                                style={{ width: `${value}%` }}
                              />
                            </span>
                            <span className="text-right font-bold tabular-nums">{value}</span>
                          </div>
                        ))}
                      </div>
                      <button
                        type="button"
                        onClick={() => setPicked(null)}
                        className="mt-3 text-xs font-semibold text-primary hover:underline"
                      >
                        Testar outra resposta
                      </button>
                    </div>
                  )}
                </div>
                <div className="border-t border-border bg-accent/30 px-5 py-3 text-center text-[11px] text-muted-foreground">
                  Demonstração interativa. Escolha uma resposta acima.
                </div>
              </div>
            </section>
          </div>
        </section>

        <section className="border-y border-border bg-card/60">
          <div className="mx-auto max-w-6xl px-5 py-8 text-center">
            <p className="text-[11px] font-semibold uppercase tracking-[0.12em] text-muted-foreground">
              Pensado para empresas que recrutam pela
            </p>
            <div className="mt-3 inline-flex flex-wrap items-center justify-center gap-2 text-sm font-semibold text-foreground/80">
              <span className="rounded-md bg-foreground px-2 py-1 text-xs font-bold text-background">
                Gupy
              </span>
              Hub de Integrações · categoria Testes
            </div>
          </div>
        </section>

        <section id="problema" className="py-20">
          <div className="mx-auto max-w-6xl px-5">
            <SectionHeading
              eyebrow="O problema"
              title="O teste tradicional virou alvo fácil."
              body="Provas de múltipla escolha e redação perderam confiabilidade: o candidato consulta a IA, decora a resposta certa e passa. Você entrevista quem é bom de prova, não quem sabe lidar com a situação."
            />
            <div className="mt-12 grid gap-4 md:grid-cols-2">
              <CompareCard
                tone="bad"
                title="Teste comum"
                items={[
                  "Mede conhecimento decorável, não comportamento",
                  'Resposta "certa" é óbvia e fácil de colar com IA',
                  "Nota sem contexto: o gestor não sabe o porquê",
                  "Entrevistas desperdiçadas com quem não tem o perfil",
                ]}
              />
              <CompareCard
                tone="good"
                title="Práxis"
                items={[
                  "Coloca a pessoa para decidir num cenário real do cargo",
                  "Todas as opções são plausíveis: mede julgamento",
                  "Score por competência com a trilha exata de cada ponto",
                  "O gestor entrevista quem já provou saber lidar",
                ]}
              />
            </div>
          </div>
        </section>

        <section id="como" className="py-20">
          <div className="mx-auto max-w-6xl px-5">
            <SectionHeading
              eyebrow="Como funciona"
              title="Um cenário ramificado, montado pelo seu RH."
              body="Sem programar, sem IA decidindo nada. O RH escreve o caso, as respostas e quanto cada uma vale por competência. A pontuação é determinística: regra e cálculo."
            />
            <div className="mt-12 grid gap-4 md:grid-cols-3">
              {[
                {
                  n: "01",
                  t: "O RH monta o caso",
                  b: "Define a situação crítica do cargo, as respostas possíveis e o peso de cada competência.",
                },
                {
                  n: "02",
                  t: "O candidato decide",
                  b: "Na Gupy, ele entra no teste, conversa com um cliente fictício e escolhe como agir.",
                },
                {
                  n: "03",
                  t: "O gestor recebe a evidência",
                  b: "Score por competência e trilha de decisão voltam para dentro da Gupy.",
                },
              ].map((step) => (
                <article key={step.n} className="rounded-lg border border-border bg-card p-6">
                  <div className="inline-flex h-9 w-9 items-center justify-center rounded-md border-2 border-primary text-sm font-bold tabular-nums text-primary">
                    {step.n}
                  </div>
                  <h3 className="mt-4 font-display text-lg font-bold leading-snug">{step.t}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{step.b}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <IconGrid
          eyebrow="Por dentro"
          title="Feito para decisão de contratação defensável."
          items={features}
          shaded
        />

        <section id="gupy" className="py-20">
          <div className="mx-auto max-w-6xl px-5">
            <div className="overflow-hidden rounded-lg bg-foreground p-8 text-background md:p-12">
              <div className="grid items-center gap-10 lg:grid-cols-[1.1fr_1fr]">
                <div>
                  <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-background/70">
                    Compatível com a API de testes da Gupy
                  </div>
                  <h2 className="mt-3 font-display text-3xl font-extrabold leading-tight md:text-4xl">
                    A Gupy organiza o funil. A Práxis adiciona a evidência.
                  </h2>
                  <p className="mt-4 max-w-xl text-base leading-relaxed text-background/80">
                    Arquitetura aderente ao contrato de provedores externos da Gupy: rotas{" "}
                    <code className="font-mono text-xs">/test</code>,{" "}
                    <code className="font-mono text-xs">/test/candidate</code>,{" "}
                    <code className="font-mono text-xs">/test/result/{"{id}"}</code>, auth Bearer e
                    envio automático com retentativas.
                  </p>
                  <p className="mt-4 inline-flex items-center gap-2 rounded-md border border-white/15 bg-white/5 px-3 py-2 text-xs text-background/75">
                    <Plug className="h-3.5 w-3.5" aria-hidden />
                    Integração Gupy explícita, sem camada genérica criada antes de existir demanda
                    real.
                  </p>
                </div>
                <ol className="space-y-2">
                  {[
                    'Candidato clica em "Iniciar teste" na Gupy',
                    "Faz o teste na Práxis",
                    "Score e competências voltam à Gupy",
                    "Gestor decide sem sair da plataforma",
                  ].map((item, index) => (
                    <li
                      key={item}
                      className="flex items-center gap-3 rounded-md border border-white/15 bg-white/10 px-4 py-3 text-sm"
                    >
                      <span className="inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-white/20 text-xs font-bold">
                        {index + 1}
                      </span>
                      {item}
                    </li>
                  ))}
                </ol>
              </div>
            </div>
          </div>
        </section>

        <IconGrid
          id="governanca"
          eyebrow="Governança & Auditoria"
          title="Pronto para o jurídico e para o compliance."
          body="As mesmas garantias que a sua equipe de governança exigiria de um sistema crítico, sem precisar pedir."
          items={governance}
        />

        <IconGrid
          id="acessibilidade"
          eyebrow="Acessibilidade"
          title="Teste justa também é teste acessível."
          body="O Práxis nasce alinhado às WCAG 2.1 AA e à LBI: o candidato com deficiência percorre o mesmo teste, com os mesmos critérios."
          items={accessibility}
          shaded
          columns="four"
        />

        <section id="faq" className="py-20">
          <div className="mx-auto max-w-3xl px-5">
            <div className="mx-auto max-w-2xl text-center">
              <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-primary">
                FAQ
              </div>
              <h2 className="mt-3 font-display text-3xl font-extrabold leading-tight tracking-tight md:text-4xl">
                Perguntas frequentes
              </h2>
            </div>
            <div className="mt-10 divide-y divide-border overflow-hidden rounded-lg border border-border bg-card">
              {faq.map((item) => (
                <details key={item.q} className="group p-5 open:bg-accent/30">
                  <summary className="flex cursor-pointer list-none items-start justify-between gap-4 text-sm font-semibold">
                    {item.q}
                    <span className="mt-0.5 text-muted-foreground transition group-open:rotate-45">
                      +
                    </span>
                  </summary>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{item.a}</p>
                </details>
              ))}
            </div>
          </div>
        </section>

        <section id="cta" className="py-20">
          <div className="mx-auto max-w-6xl px-5">
            <div className="rounded-lg bg-foreground p-10 text-center text-background md:p-16">
              <h2 className="mx-auto max-w-[22ch] font-display text-3xl font-extrabold leading-tight md:text-5xl">
                Menos entrevista por currículo. Mais decisão por evidência.
              </h2>
              <p className="mx-auto mt-5 max-w-[46ch] text-base text-background/80">
                Mostre como seus candidatos decidem antes de chamar para a conversa. Agende uma
                demonstração da Práxis.
              </p>
              <button
                type="button"
                onClick={openDemoEmail}
                className="mt-8 inline-flex items-center gap-2 rounded-md bg-background px-7 py-3.5 text-sm font-bold text-foreground shadow-lg hover:shadow-white/30"
              >
                Agendar demonstração
                <ArrowRight className="h-4 w-4" aria-hidden />
              </button>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-border bg-background py-12">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-5 text-sm text-muted-foreground sm:flex-row">
          <Brand />
          <div>© 2026 iForce · praxis.iforce.com.br</div>
        </div>
      </footer>
    </div>
  );
}

function SectionHeading({
  eyebrow,
  title,
  body,
}: {
  eyebrow: string;
  title: string;
  body: string;
}) {
  return (
    <div className="max-w-2xl">
      <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-primary">
        {eyebrow}
      </div>
      <h2 className="mt-3 font-display text-3xl font-extrabold leading-tight tracking-tight md:text-4xl">
        {title}
      </h2>
      {body && <p className="mt-4 text-base leading-relaxed text-muted-foreground">{body}</p>}
    </div>
  );
}

function CompareCard({
  tone,
  title,
  items,
}: {
  tone: "good" | "bad";
  title: string;
  items: string[];
}) {
  const Icon = tone === "good" ? CheckCircle2 : X;

  return (
    <article
      className={
        tone === "good"
          ? "rounded-lg border border-sidebar-border bg-sidebar p-6 text-sidebar-foreground"
          : "rounded-lg border border-border bg-card p-6"
      }
    >
      <div className="flex items-center gap-3">
        <span
          className={
            tone === "good"
              ? "inline-flex h-9 w-9 items-center justify-center rounded-md bg-white/15 text-white"
              : "inline-flex h-9 w-9 items-center justify-center rounded-md bg-destructive/10 text-destructive"
          }
        >
          <Icon className="h-4 w-4" aria-hidden />
        </span>
        <h3 className="font-display text-lg font-bold">{title}</h3>
      </div>
      <ul className="mt-5 space-y-3 text-sm">
        {items.map((item) => (
          <li key={item} className="flex items-start gap-2.5">
            <Icon
              className={`mt-0.5 h-4 w-4 shrink-0 ${
                tone === "good" ? "text-success" : "text-destructive"
              }`}
              aria-hidden
            />
            <span className={tone === "good" ? "text-sidebar-foreground/90" : "text-foreground/80"}>
              {item}
            </span>
          </li>
        ))}
      </ul>
    </article>
  );
}

function IconGrid({
  id,
  eyebrow,
  title,
  body,
  items,
  shaded = false,
  columns = "three",
}: {
  id?: string;
  eyebrow: string;
  title: string;
  body?: string;
  items: Array<{
    icon: ComponentType<{ className?: string }>;
    title: string;
    body: string;
  }>;
  shaded?: boolean;
  columns?: "three" | "four";
}) {
  return (
    <section id={id} className={shaded ? "bg-accent/40 py-20" : "py-20"}>
      <div className="mx-auto max-w-6xl px-5">
        <SectionHeading eyebrow={eyebrow} title={title} body={body ?? ""} />
        <div
          className={`mt-12 grid gap-4 md:grid-cols-2 ${
            columns === "four" ? "lg:grid-cols-4" : "lg:grid-cols-3"
          }`}
        >
          {items.map((item) => (
            <article key={item.title} className="rounded-lg border border-border bg-card p-6">
              <span className="inline-flex h-10 w-10 items-center justify-center rounded-md bg-accent text-primary">
                <item.icon className="h-5 w-5" />
              </span>
              <h3 className="mt-4 font-display text-base font-bold leading-snug">{item.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{item.body}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
