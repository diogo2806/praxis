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
      { title: "Comece por aqui - Praxis" },
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
    title: "1. Você monta o teste",
    text: "Escolhe o cargo e descreve uma situação real do trabalho. O Práxis transforma isso em um teste com perguntas de decisão.",
  },
  {
    icon: Link2,
    title: "2. O candidato responde",
    text: "Você envia um link por e-mail ou WhatsApp. A pessoa responde do celular ou do computador, em 20 a 30 minutos.",
  },
  {
    icon: BarChart3,
    title: "3. Você vê o resultado",
    text: "A nota sai de regras claras e iguais para todos. Você consegue ver o porquê de cada ponto, sem caixa-preta.",
  },
];

const audiences = [
  "Empresas que fazem recrutamento e seleção",
  "Equipes de Recursos Humanos (RH)",
  "Gestores que precisam avaliar candidatos de forma consistente",
];

const startActions = [
  {
    icon: Rocket,
    title: "Criar meu primeiro teste",
    text: "Monte um teste do zero, passo a passo.",
    to: "/nova/blueprint" as const,
    primary: true,
  },
  {
    icon: BarChart3,
    title: "Ver o painel",
    text: "Veja os testes e resultados que já existem.",
    to: "/" as const,
    primary: false,
  },
  {
    icon: Link2,
    title: "Enviar link a um candidato",
    text: "Convide alguém para responder um teste pronto.",
    to: "/enviar-link" as const,
    primary: false,
  },
];

const faq = [
  {
    q: "O que é o Práxis, em uma frase?",
    a: "É uma plataforma para avaliar candidatos em processos seletivos usando situações reais do trabalho, em vez de provas teóricas.",
  },
  {
    q: "Quanto tempo leva para o candidato fazer o teste?",
    a: "Na maioria dos casos, entre 20 e 30 minutos.",
  },
  {
    q: "Os resultados são consistentes?",
    a: "A nota vem de regras claras, aplicadas da mesma forma a cada pessoa, e não de uma inteligência artificial decidindo sozinha. É possível ver de onde veio cada ponto. A adequação dos critérios é de responsabilidade de quem cria o teste.",
  },
  {
    q: "Usa inteligência artificial para julgar a pessoa?",
    a: "Não. Nenhuma IA decide a aprovação. O cálculo segue critérios definidos antes do teste, e um erro grave leva a uma revisão feita por uma pessoa.",
  },
  {
    q: "Funciona no celular?",
    a: "Sim. O candidato pode responder por celular, tablet ou computador.",
  },
  {
    q: "Meus dados estão seguros?",
    a: "O Práxis foi desenhado com controles alinhados à LGPD (a lei brasileira de proteção de dados): registra um histórico do tratamento e oferece configurações de retenção e revisão. A empresa que aplica o teste é a responsável pelo uso dos dados e pela adequação à legislação aplicável.",
  },
  {
    q: "Preciso ter formação técnica para usar?",
    a: "Não. A criação do teste é guiada por etapas. Termos técnicos têm uma explicação simples quando você passa o mouse sobre eles.",
  },
  {
    q: "O candidato me pediu um código de acesso. O que é isso?",
    a: "É o código do convite. Ele já vai dentro do link enviado ao candidato; se a pessoa perder o acesso, basta reenviar o link pela tela Enviar link.",
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
              <strong className="text-foreground">Sem IA julgando o candidato.</strong> Os
              resultados vêm de regras claras, definidas antes do teste, e não de decisões
              automáticas. É possível ver o porquê de cada nota.
            </p>
          </div>
        </section>
      </div>
    </AppShell>
  );
}
