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
          "Teste situacional integrado ao processo de recrutamento, com pontuação baseada em regras previamente definidas, indicadores por competência e trilha de auditoria.",
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
    title: "Comparação entre caminhos",
    body: "A pontuação considera o caminho percorrido e as competências avaliadas em cada cenário, conforme a configuração publicada.",
  },
  {
    icon: ScrollText,
    title: "Resultado rastreável",
    body: "A equipe autorizada pode consultar etapas, escolhas, rubricas e eventos relacionados ao cálculo.",
  },
  {
    icon: UserCheck,
    title: "Apoio à revisão humana",
    body: "O Práxis não emite reprovação automática. Casos configurados como críticos são sinalizados para análise da empresa.",
  },
  {
    icon: Library,
    title: "Biblioteca de cenários",
    body: "Modelos prontos por área e senioridade. O RH edita, testa com um piloto e publica quando estiver pronto.",
  },
  {
    icon: GitBranch,
    title: "Integração com recrutamento",
    body: "Quando habilitada, a integração envia indicadores e resultados para o ambiente de recrutamento configurado.",
  },
];

const governance = [
  {
    icon: ShieldCheck,
    title: "Trilha cronológica de auditoria",
    body: "Eventos relevantes, como criação, resposta, expiração e conclusão, são registrados para consulta por usuários autorizados.",
  },
  {
    icon: Lock,
    title: "Apoio à governança de privacidade",
    body: "Recursos ajudam a informar finalidades, prazos de retenção e canais de atendimento. A conformidade depende também das práticas de cada controlador.",
  },
  {
    icon: Scale,
    title: "Defensabilidade",
    body: "Pesos, rubricas e versões permitem identificar quais regras participaram do cálculo apresentado.",
  },
];

const accessibility = [
  {
    icon: Eye,
    title: "Contraste e legibilidade",
    body: "A interface usa contraste, hierarquia visual e textos legíveis como parte das práticas de acessibilidade.",
  },
  {
    icon: Keyboard,
    title: "Operável por teclado",
    body: "A interface incorpora navegação por teclado e foco visível nos fluxos principais.",
  },
  {
    icon: Accessibility,
    title: "Leitores de tela",
    body: "Enunciados, alternativas e informações dinâmicas usam marcação semântica e idioma pt-BR.",
  },
  {
    icon: Timer,
    title: "Tempo ajustável",
    body: "Limites por etapa podem ser ajustados conforme as necessidades do candidato e as medidas adotadas pela empresa.",
  },
];

const faq = [
  {
    q: "O Práxis usa IA generativa para avaliar o candidato?",
    a: "Não. A pontuação apresentada atualmente é calculada por regras, pesos e critérios previamente configurados pela equipe responsável pelo teste.",
  },
  {
    q: "Como funciona a integração com a Gupy?",
    a: "Quando devidamente contratada, habilitada e configurada, a integração pode receber convites e enviar resultados por meio da API disponível. O funcionamento está sujeito às credenciais, regras e disponibilidade do serviço de terceiros.",
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
    a: "A interface foi desenvolvida com práticas de acessibilidade, incluindo navegação por teclado, foco visível, marcação semântica e recursos configuráveis de acomodação.",
  },
  {
    q: "O Práxis reprova alguém sozinho?",
    a: "Não. O resultado é um indicador complementar. Casos configurados como críticos são sinalizados para análise, e a decisão final permanece sob responsabilidade da empresa.",
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
                Teste situacional com pontuação baseada em regras
              </span>
              <h1 className="mt-5 max-w-3xl font-display text-4xl font-extrabold leading-[1.05] tracking-tight text-foreground md:text-6xl">
                Observe como cada candidato responde a situações representativas do trabalho.
              </h1>
              <p className="mt-5 max-w-xl text-base leading-relaxed text-muted-foreground md:text-lg">
                Antes de avançar para a entrevista, apresente cenários simulados do cargo e obtenha
                indicadores por competência, acompanhados do caminho de respostas e dos critérios
                utilizados no cálculo.
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
                Integração técnica com a API de testes da Gupy, sujeita à habilitação, às
                credenciais e à disponibilidade do serviço.
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
              Integração técnica sujeita à habilitação no serviço de terceiros
            </p>
            <div className="mt-3 inline-flex flex-wrap items-center justify-center gap-2 text-sm font-semibold text-foreground/80">
              <span className="rounded-md bg-foreground px-2 py-1 text-xs font-bold text-background">
                Gupy
              </span>
              Gupy é marca de seu respectivo titular. A referência não implica endosso ou parceria,
              salvo quando formalmente informada.
            </div>
          </div>
        </section>

        <section id="problema" className="py-20">
          <div className="mx-auto max-w-6xl px-5">
            <SectionHeading
              eyebrow="O problema"
              title="Avaliações teóricas nem sempre mostram como a pessoa analisa uma situação prática."
              body="Provas tradicionais continuam sendo úteis, mas podem oferecer um sinal limitado sobre priorização, comunicação, tomada de decisão e aderência a procedimentos diante de cenários do dia a dia."
            />
            <div className="mt-12 grid gap-4 md:grid-cols-2">
              <CompareCard
                tone="bad"
                title="Avaliação exclusivamente teórica"
                items={[
                  "Concentra-se principalmente no conhecimento declarado",
                  "Pode oferecer pouco contexto sobre o raciocínio aplicado",
                  "Nem sempre permite reconstruir por que uma nota foi atribuída",
                  "Pode exigir mais etapas para identificar candidatos alinhados ao cenário da vaga",
                ]}
              />
              <CompareCard
                tone="good"
                title="Práxis"
                items={[
                  "Apresenta cenários simulados inspirados no contexto do cargo",
                  "Permite configurar alternativas plausíveis e critérios de pontuação",
                  "Organiza indicadores por competência",
                  "Registra o caminho de respostas para apoiar a análise do RH",
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
              body="Sua equipe define o caso, as alternativas possíveis, as competências observadas e os critérios de pontuação. A plataforma calcula conforme as regras publicadas."
            />
            <div className="mt-12 grid gap-4 md:grid-cols-3">
              {[
                {
                  n: "01",
                  t: "Sua equipe estrutura o cenário",
                  b: "Defina uma situação relevante do cargo, as alternativas possíveis, as competências observadas e os critérios de pontuação.",
                },
                {
                  n: "02",
                  t: "O candidato decide",
                  b: "A pessoa analisa os cenários e escolhe como agiria em cada etapa, utilizando celular ou computador compatível.",
                },
                {
                  n: "03",
                  t: "O RH recebe indicadores",
                  b: "A plataforma apresenta a pontuação, os indicadores e o caminho de respostas para análise conjunta com as demais etapas.",
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
          title="Feito para decisões mais documentadas."
          items={features}
          shaded
        />

        <section id="gupy" className="py-20">
          <div className="mx-auto max-w-6xl px-5">
            <div className="overflow-hidden rounded-lg bg-foreground p-8 text-background md:p-12">
              <div className="grid items-center gap-10 lg:grid-cols-[1.1fr_1fr]">
                <div>
                  <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-background/70">
                    Integração técnica com a API de testes da Gupy
                  </div>
                  <h2 className="mt-3 font-display text-3xl font-extrabold leading-tight md:text-4xl">
                    A plataforma de recrutamento organiza o funil. A Práxis adiciona indicadores.
                  </h2>
                  <p className="mt-4 max-w-xl text-base leading-relaxed text-background/80">
                    Arquitetura preparada para integrações técnicas de testes externos: rotas{" "}
                    <code className="font-mono text-xs">/test</code>,{" "}
                    <code className="font-mono text-xs">/test/candidate</code>,{" "}
                    <code className="font-mono text-xs">/test/result/{"{id}"}</code>, auth Bearer e
                    envio automático com retentativas.
                  </p>
                  <p className="mt-4 inline-flex items-center gap-2 rounded-md border border-white/15 bg-white/5 px-3 py-2 text-xs text-background/75">
                    <Plug className="h-3.5 w-3.5" aria-hidden />
                    Funcionamento sujeito à contratação, habilitação, credenciais, disponibilidade e
                    regras do serviço de terceiros.
                  </p>
                </div>
                <ol className="space-y-2">
                  {[
                    'Candidato clica em "Iniciar teste" na Gupy',
                    "Faz o teste na Práxis",
                    "Score e competências voltam à Gupy",
                    "Equipe responsável analisa os indicadores no ambiente configurado",
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
          title="Recursos para decisões mais documentadas."
          body="Versionamento, registros operacionais e critérios de pontuação apoiam processos internos de governança, auditoria e revisão."
          items={governance}
        />

        <IconGrid
          id="acessibilidade"
          eyebrow="Acessibilidade"
          title="Desenvolvido com práticas de acessibilidade."
          body="A adequação de cada teste deve considerar o conteúdo criado, as necessidades do candidato e as medidas adotadas pela empresa responsável pelo processo."
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
                Acrescente indicadores situacionais ao seu processo seletivo.
              </h2>
              <p className="mx-auto mt-5 max-w-[46ch] text-base text-background/80">
                Conheça como a Práxis pode ajudar sua equipe a estruturar cenários, organizar
                critérios e analisar respostas com mais contexto e rastreabilidade.
              </p>
              <button
                type="button"
                onClick={openDemoEmail}
                className="mt-8 inline-flex items-center gap-2 rounded-md bg-white px-7 py-3.5 text-sm font-bold text-slate-950 shadow-lg hover:bg-slate-100 hover:shadow-white/30"
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
