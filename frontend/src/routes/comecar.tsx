import { createFileRoute, Link } from "@tanstack/react-router";
import {
  BarChart3,
  CheckCircle2,
  ClipboardCheck,
  HelpCircle,
  Link2,
  ListChecks,
  Rocket,
  ShieldCheck,
  Sparkles,
  Users,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { useLanguage } from "@/lib/language-context";

export const Route = createFileRoute("/comecar")({
  head: () => ({
    meta: [
      { title: "Comece por aqui - Práxis" },
      {
        name: "description",
        content:
          "Entenda em 1 minuto o que e o Praxis, como funciona e por onde comecar. Linguagem simples, sem jargao.",
      },
    ],
  }),
  component: GettingStartedPage,
});

const steps = [
  {
    icon: ClipboardCheck,
    title: "1. Defina o contexto",
    text: "Informe o objetivo da avaliação, as situações observadas, as competências e os critérios de pontuação.",
  },
  {
    icon: Link2,
    title: "2. A pessoa participa",
    text: "Compartilhe o acesso por link para que a pessoa responda aos cenários configurados.",
  },
  {
    icon: BarChart3,
    title: "3. Analise os registros",
    text: "Consulte indicadores, respostas, percurso e eventos relacionados à participação.",
  },
];

const audiences = [
  "Equipes que estruturam avaliações situacionais",
  "Áreas de desenvolvimento e capacitação",
  "Equipes de atendimento e operações",
  "Áreas responsáveis por procedimentos e conformidade",
];

const startActions = [
  {
    icon: Rocket,
    title: "Criar minha primeira avaliação",
    text: "Monte uma avaliação por cenários, passo a passo.",
    to: "/nova/avaliacao" as const,
    primary: true,
  },
  {
    icon: BarChart3,
    title: "Ver o painel",
    text: "Veja as avaliações e participações que já existem.",
    to: "/dashboard" as const,
    primary: false,
  },
  {
    icon: Link2,
    title: "Compartilhar uma avaliação",
    text: "Convide uma pessoa para responder uma avaliação pronta.",
    to: "/enviar-link" as const,
    primary: false,
  },
];

const faq = [
  {
    q: "O que é o Práxis, em uma frase?",
    a: "É uma plataforma para criar avaliações por cenários, aplicar critérios configurados e acompanhar respostas, indicadores e registros do percurso.",
  },
  {
    q: "Quanto tempo leva para a pessoa concluir a avaliação?",
    a: "Depende da quantidade e da complexidade dos cenários configurados para cada avaliação.",
  },
  {
    q: "Como a pontuação é calculada?",
    a: "A plataforma aplica as regras publicadas de forma padronizada e permite rastrear a origem da pontuação. A equidade também depende da qualidade dos cenários, da validação dos critérios, das acomodações oferecidas e da revisão da empresa.",
  },
  {
    q: "Usa inteligência artificial para julgar a pessoa?",
    a: "Não. A pontuação atual é calculada por regras configuradas previamente. O Práxis não emite reprovação automática e pode sinalizar respostas críticas para análise da equipe responsável.",
  },
  {
    q: "Funciona no celular?",
    a: "A interface foi desenvolvida para navegadores modernos em computadores, tablets e celulares. A compatibilidade pode variar conforme o navegador, a versão do sistema e os recursos de acessibilidade utilizados.",
  },
  {
    q: "Meus dados estão seguros?",
    a: "O Práxis adota controles técnicos e organizacionais destinados à proteção dos dados. Nenhum sistema é completamente imune a riscos, por isso os controles devem ser acompanhados de gestão de acesso, monitoramento e procedimentos de resposta a incidentes.",
  },
  {
    q: "O Práxis garante conformidade com a LGPD?",
    a: "A plataforma oferece recursos que apoiam a governança de privacidade. A adequação completa depende também das finalidades, bases legais, políticas, contratos e procedimentos adotados pela organização responsável.",
  },
  {
    q: "Preciso ter formação técnica para usar?",
    a: "Não. A criação da avaliação é guiada por etapas. Termos técnicos têm uma explicação simples quando você passa o mouse sobre eles.",
  },
  {
    q: "A pessoa participante pediu um código de acesso. O que é isso?",
    a: "É o código do convite. Ele já vai dentro do link enviado; se a pessoa perder o acesso, basta reenviar o link pela tela Enviar link.",
  },
];

function GettingStartedPage() {
  const { t } = useLanguage();

  return (
    <AppShell>
      <div className="mx-auto max-w-3xl">
        <div className="mb-8">
          <div className="inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
            <Sparkles className="h-3.5 w-3.5" />
            {t.common.startHere}
          </div>
          <h1 className="mt-3 text-3xl font-semibold text-foreground sm:text-4xl">
            {t.getStarted.heading}
          </h1>
          <p className="mt-3 text-base leading-relaxed text-muted-foreground">
            {t.getStarted.description}
          </p>
        </div>

        <section aria-labelledby="como-funciona" className="mb-10">
          <h2
            id="como-funciona"
            className="mb-4 flex items-center gap-2 text-xl font-semibold text-foreground"
          >
            <ListChecks className="h-5 w-5 text-primary" />
            {t.getStarted.howItWorks}
          </h2>
          <div className="grid gap-3 sm:grid-cols-3">
            {steps.map(({ icon: Icon, title, text }) => (
              <div key={title} className="rounded-lg border border-border bg-card p-5">
                <Icon className="h-6 w-6 text-primary" />
                <h3 className="mt-3 text-base font-semibold text-foreground">{title}</h3>
                <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">{text}</p>
              </div>
            ))}
          </div>
        </section>

        <section aria-labelledby="para-quem" className="mb-10">
          <h2
            id="para-quem"
            className="mb-4 flex items-center gap-2 text-xl font-semibold text-foreground"
          >
            <Users className="h-5 w-5 text-primary" />
            {t.getStarted.forWhom}
          </h2>
          <ul className="space-y-2">
            {audiences.map((item) => (
              <li
                key={item}
                className="flex items-start gap-3 rounded-lg border border-border bg-card p-4 text-base text-foreground"
              >
                <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-success" />
                {item}
              </li>
            ))}
          </ul>
        </section>

        <section aria-labelledby="por-onde-comecar" className="mb-10">
          <h2
            id="por-onde-comecar"
            className="mb-4 flex items-center gap-2 text-xl font-semibold text-foreground"
          >
            <Rocket className="h-5 w-5 text-primary" />
            {t.getStarted.whereToStart}
          </h2>
          <div className="grid gap-3 sm:grid-cols-3">
            {startActions.map(({ icon: Icon, title, text, to, primary }) => (
              <Link
                key={title}
                to={to}
                className={
                  primary
                    ? "flex flex-col rounded-lg border border-primary bg-primary p-5 text-primary-foreground transition hover:bg-primary/90"
                    : "flex flex-col rounded-lg border border-border bg-card p-5 transition hover:bg-accent"
                }
              >
                <Icon className="h-6 w-6" />
                <span className="mt-3 text-base font-semibold">{title}</span>
                <span
                  className={
                    primary
                      ? "mt-1 text-sm text-primary-foreground/85"
                      : "mt-1 text-sm text-muted-foreground"
                  }
                >
                  {text}
                </span>
              </Link>
            ))}
          </div>
          <p className="mt-3 text-sm text-muted-foreground">
            ou{" "}
            <Link to="/nova/rapido" className="font-medium text-primary hover:underline">
              comece com um modelo pronto →
            </Link>
          </p>
        </section>

        <section aria-labelledby="faq" className="mb-10">
          <h2
            id="faq"
            className="mb-2 flex items-center gap-2 text-xl font-semibold text-foreground"
          >
            <HelpCircle className="h-5 w-5 text-primary" />
            {t.getStarted.faq}
          </h2>
          <div className="rounded-lg border border-border bg-card px-5">
            <Accordion type="single" collapsible>
              {faq.map(({ q, a }, index) => (
                <AccordionItem key={q} value={`item-${index}`} className="last:border-b-0">
                  <AccordionTrigger className="text-base">{q}</AccordionTrigger>
                  <AccordionContent className="text-base leading-relaxed text-muted-foreground">
                    {a}
                  </AccordionContent>
                </AccordionItem>
              ))}
            </Accordion>
          </div>
        </section>

        <section className="rounded-lg border border-border bg-muted/40 p-5">
          <div className="flex items-start gap-3">
            <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-success" />
            <p className="text-sm leading-relaxed text-muted-foreground">
              <strong className="text-foreground">Resultado como indicador complementar.</strong> A
              Práxis organiza cenários, respostas e indicadores definidos pela organização. A
              interpretação e a decisão final permanecem sob responsabilidade da equipe responsável.
            </p>
          </div>
        </section>
      </div>
    </AppShell>
  );
}
