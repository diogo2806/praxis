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

const competencyPalette: Record<string, string> = {
  Empatia: "#2F855A",
  Resolução: "#2B6CB0",
  "Aderência à política": "#B7791F",
};

function scoreBarColor(label: string) {
  return competencyPalette[label] ?? "#1B6C8C";
}

function scoreSummaryLabel(tone: HeroOption["tone"]) {
  if (tone === "good") return "Sinal favorável para avançar";
  if (tone === "warn") return "Exige leitura contextual";
  return "Pede revisão do caminho";
}

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Práxis — Avaliações situacionais com critérios rastreáveis" },
      {
        name: "description",
        content:
          "Crie avaliações com cenários do trabalho, critérios configuráveis, indicadores por competência e histórico das respostas para apoiar processos seletivos mais contextualizados.",
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
    icon: Scale,
    title: "Pontuação baseada em regras",
    body:
      "Critérios, pesos e competências são configurados antes da publicação e aplicados às respostas registradas.",
  },
  {
    icon: GitBranch,
    title: "Cenários ramificados",
    body:
      "Cada alternativa pode conduzir a uma nova etapa, representando diferentes caminhos e consequências.",
  },
  {
    icon: ScrollText,
    title: "Resultado rastreável",
    body:
      "Usuários autorizados podem consultar respostas, versões, critérios e eventos relacionados à avaliação.",
  },
  {
    icon: UserCheck,
    title: "Apoio à análise humana",
    body:
      "Os indicadores complementam as demais etapas do processo e não determinam isoladamente a contratação.",
  },
  {
    icon: Library,
    title: "Competências configuráveis",
    body:
      "A equipe define as competências relevantes para o cargo e como elas participam da pontuação.",
  },
  {
    icon: Plug,
    title: "Operação independente",
    body:
      "A avaliação pode ser enviada diretamente por link. Integrações são utilizadas quando necessárias.",
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
    q: "Como a pontuação é calculada?",
    a: "A pontuação utiliza regras, critérios, pesos e competências configurados pela equipe responsável antes da publicação da avaliação.",
  },
  {
    q: "Posso utilizar a Práxis sem integrar com outro sistema?",
    a: "Sim. A empresa pode gerar um link de participação e compartilhá-lo diretamente com o candidato. Integrações são opcionais.",
  },
  {
    q: "O candidato vê pesos ou critérios internos?",
    a: "Pesos, critérios de pontuação e marcadores internos não são apresentados na experiência destinada ao candidato.",
  },
  {
    q: "A Práxis decide quem deve ser contratado?",
    a: "Não. A plataforma organiza indicadores sobre respostas a cenários simulados. A decisão permanece sob responsabilidade da empresa.",
  },
  {
    q: "A avaliação pode ser adaptada?",
    a: "A plataforma oferece recursos configuráveis, como tempo por etapa e conteúdos acessíveis. A adequação final depende também do conteúdo e das medidas adotadas pela empresa.",
  },
  {
    q: "É possível consultar como o resultado foi formado?",
    a: "Usuários autorizados podem consultar registros, respostas, versões e critérios relacionados à avaliação, conforme suas permissões.",
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
              Produto
            </a>
            <a className="hover:text-foreground" href="#como">
              Como funciona
            </a>
            <a className="hover:text-foreground" href="#recursos">
              Recursos
            </a>
            <a className="hover:text-foreground" href="#governanca">
              Governança
            </a>
            <a className="hover:text-foreground" href="#faq">
              Perguntas frequentes
            </a>
          </nav>
          <button
            type="button"
            onClick={openDemoEmail}
            className="inline-flex items-center gap-1.5 rounded-md bg-foreground px-4 py-2 text-sm font-semibold text-background hover:opacity-90"
          >
            Solicitar demonstração
          </button>
        </div>
      </header>

      <main>
        <section id="topo" className="relative overflow-hidden">
          <div className="pointer-events-none absolute inset-x-0 top-0 h-80 bg-[radial-gradient(circle_at_50%_0%,var(--color-primary)/0.18,transparent_58%)]" />
          <div className="relative mx-auto grid max-w-6xl items-center gap-12 px-5 py-16 lg:grid-cols-[1.05fr_1fr] lg:py-20">
            <div>
              <span className="inline-flex w-fit items-center gap-2 rounded-full bg-accent px-3 py-1.5 text-xs font-semibold text-primary">
                <span className="h-2 w-2 rounded-full bg-success" />
                Avaliações situacionais com pontuação baseada em regras
              </span>
              <h1 className="mt-5 max-w-3xl font-display text-4xl font-extrabold leading-[1.05] tracking-tight text-foreground md:text-6xl">
                Veja como candidatos respondem a situações do cargo antes da entrevista.
              </h1>
              <p className="mt-5 max-w-xl text-base leading-relaxed text-muted-foreground md:text-lg">
                Crie cenários alinhados à realidade da função, defina competências e critérios de
                pontuação e organize os resultados com mais contexto para a análise da sua equipe.
              </p>
              <div className="mt-7 flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={openDemoEmail}
                  className="inline-flex items-center gap-2 rounded-md bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/20 hover:bg-primary/90"
                >
                  Solicitar demonstração
                  <ArrowRight className="h-4 w-4" aria-hidden />
                </button>
                <a
                  href="#demo"
                  className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-6 py-3 text-sm font-semibold hover:bg-accent"
                >
                  Experimentar um cenário
                </a>
              </div>
              <p className="mt-5 flex items-center gap-2 text-xs text-muted-foreground">
                <CheckCircle2 className="h-3.5 w-3.5 text-success" aria-hidden />
                A Práxis organiza indicadores para apoiar a análise. A decisão do processo permanece
                com a empresa responsável.
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
                      <div className="mt-4 rounded-md border border-border bg-background/80 p-4">
                        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border pb-3">
                          <div>
                            <div className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground">
                              Cartão de evidência
                            </div>
                            <div className="mt-1 text-sm font-semibold text-foreground">
                              Indicadores por competência
                            </div>
                          </div>
                          <span
                            className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${
                              chosen.tone === "good"
                                ? "bg-success/10 text-success"
                                : chosen.tone === "warn"
                                  ? "bg-warning/10 text-warning"
                                  : "bg-destructive/10 text-destructive"
                            }`}
                          >
                            {scoreSummaryLabel(chosen.tone)}
                          </span>
                        </div>
                        <div className="mt-4 space-y-3">
                        {Object.entries(chosen.scores).map(([label, value]) => (
                          <div
                            key={label}
                            className="grid grid-cols-[minmax(8rem,1fr)_3rem] gap-2 text-xs"
                          >
                            <span className="text-muted-foreground">{label}</span>
                            <span className="text-right font-bold tabular-nums">{value}</span>
                            <span className="col-span-2 h-2 overflow-hidden rounded-full bg-accent">
                              <span
                                className="block h-full rounded-full bg-primary transition-all duration-700"
                                style={{
                                  width: `${value}%`,
                                  backgroundColor: scoreBarColor(label),
                                }}
                              />
                            </span>
                          </div>
                        ))}
                        </div>
                        <p className="mt-4 text-[11px] leading-relaxed text-muted-foreground">
                          A leitura final considera o caminho percorrido no cenário. Os valores acima
                          representam competências observadas nesta resposta, não uma nota única
                          universal.
                        </p>
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

        <section id="problema" className="py-20">
          <div className="mx-auto max-w-6xl px-5">
            <SectionHeading
              eyebrow="O problema"
              title="Conhecimento declarado não mostra toda a forma de decidir."
              body="Currículo, perguntas teóricas e entrevistas continuam importantes, mas podem oferecer pouco contexto sobre como uma pessoa prioriza informações, se comunica e toma decisões diante de situações do trabalho."
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
              title="Estruture, publique e analise com mais contexto."
              body="Sua equipe define o cargo, o contexto, as alternativas possíveis, as competências observadas e os critérios utilizados na pontuação."
            />
            <div className="mt-12 grid gap-4 md:grid-cols-3">
              {[
                {
                  n: "01",
                  t: "Estruture a situação",
                  b: "Defina o cargo, o contexto, as alternativas possíveis, as competências observadas e os critérios utilizados na pontuação.",
                },
                {
                  n: "02",
                  t: "Publique e compartilhe",
                  b: "Disponibilize a avaliação e envie o link diretamente ao candidato. Integrações podem ser utilizadas quando fizerem parte da operação da empresa.",
                },
                {
                  n: "03",
                  t: "Analise com mais contexto",
                  b: "Consulte a pontuação, os indicadores por competência e o caminho de respostas para complementar as demais etapas do processo.",
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
          id="recursos"
          eyebrow="Recursos"
          title="Feito para avaliações mais contextualizadas e documentadas."
          items={features}
          shaded
        />

        <section id="fluxo" className="py-20">
          <div className="mx-auto max-w-6xl px-5">
            <div className="overflow-hidden rounded-lg bg-foreground p-8 text-background md:p-12">
              <div className="grid items-center gap-10 lg:grid-cols-[1.1fr_1fr]">
                <div>
                  <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-background/70">
                    Do convite ao resultado
                  </div>
                  <h2 className="mt-3 font-display text-3xl font-extrabold leading-tight md:text-4xl">
                    A Práxis funciona de forma independente.
                  </h2>
                  <p className="mt-4 max-w-xl text-base leading-relaxed text-background/80">
                    Sua equipe estrutura e publica a avaliação, gera o acesso do candidato,
                    acompanha a participação e consulta os indicadores dentro da plataforma.
                  </p>
                  <p className="mt-4 inline-flex items-center gap-2 rounded-md border border-white/15 bg-white/5 px-3 py-2 text-xs text-background/75">
                    <Plug className="h-3.5 w-3.5" aria-hidden />
                    Integrações são opcionais e dependem da configuração de cada serviço conectado.
                  </p>
                </div>
                <ol className="space-y-2">
                  {[
                    "Sua equipe estrutura e publica a avaliação",
                    "A Práxis gera um link para o candidato",
                    "O candidato responde aos cenários",
                    "A equipe acompanha e analisa os indicadores",
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
                Leve mais contexto para a análise dos seus candidatos.
              </h2>
              <p className="mx-auto mt-5 max-w-[48ch] text-base text-background/80">
                Conheça como a Práxis pode ajudar sua equipe a estruturar situações do cargo,
                aplicar critérios consistentes e consultar resultados com mais rastreabilidade.
              </p>
              <button
                type="button"
                onClick={openDemoEmail}
                className="mt-8 inline-flex items-center gap-2 rounded-md bg-white px-7 py-3.5 text-sm font-bold text-slate-950 shadow-lg hover:bg-slate-100 hover:shadow-white/30"
              >
                Solicitar demonstração por e-mail
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
