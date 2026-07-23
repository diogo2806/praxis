import { useId, useState } from "react";

type PricingTier = {
  annualAssessments: number;
  unitPrice: string;
  annualTotal: string;
};

type FaqItem = {
  question: string;
  answer: string;
};

export const PROFESSIONAL_ANNUAL_TIERS = [
  { annualAssessments: 100, unitPrice: "54,90", annualTotal: "5.490,00" },
  { annualAssessments: 300, unitPrice: "49,90", annualTotal: "14.970,00" },
  { annualAssessments: 1_000, unitPrice: "44,90", annualTotal: "44.900,00" },
  { annualAssessments: 3_000, unitPrice: "39,90", annualTotal: "119.700,00" },
] as const satisfies readonly PricingTier[];

export const ANNUAL_CYCLE_DESCRIPTION =
  "A assinatura Profissional é anual. O pacote completo entra no saldo após a confirmação do pagamento e pode ser usado durante os 12 meses.";

export const CONTRACTING_FAQ_ANSWER =
  "Há compra avulsa para demandas pontuais e assinatura anual Profissional com pacotes de 100, 300, 1.000 ou 3.000 avaliações. O pagamento anual libera o pacote completo no saldo para uso durante os 12 meses. Operações Enterprise, integrações, suporte específico e condições contratuais ficam sob consulta conforme volume e escopo.";

const FAQ_ITEMS = [
  {
    question: "A Práxis usa IA generativa para avaliar pessoas?",
    answer:
      "Não. A pontuação é calculada a partir de critérios, pesos e regras configurados previamente pela equipe responsável.",
  },
  {
    question: "Preciso integrar a Práxis a outro sistema?",
    answer:
      "Não. A operação pode ser realizada por links diretos e acompanhada no painel. Integrações são opcionais e dependem da existência e configuração de um conector compatível.",
  },
  {
    question: "Como funciona a contratação?",
    answer: CONTRACTING_FAQ_ANSWER,
  },
  {
    question: "Quanto tempo leva para colocar no ar?",
    answer:
      "Com cenários já estruturados, a equipe pode ajustar o contexto, os critérios e os pesos, testar em modo piloto e publicar quando estiver pronto.",
  },
  {
    question: "O participante vê pesos, gabarito ou marcadores críticos?",
    answer:
      "Não. A visão do participante é limpa. Pesos, critérios e marcadores ficam restritos ao painel administrativo e à trilha de auditoria.",
  },
  {
    question: "A Práxis toma a decisão final?",
    answer:
      "Não. A plataforma organiza critérios, pontuações e registros para apoiar a análise. A interpretação e a decisão final permanecem com a equipe responsável.",
  },
  {
    question: "Para quais contextos serve?",
    answer:
      "A Práxis pode ser configurada para diferentes contextos em que uma equipe precisa apresentar situações, registrar escolhas e analisar critérios previamente definidos.",
  },
] as const satisfies readonly FaqItem[];

function formatAssessmentQuantity(quantity: number) {
  return new Intl.NumberFormat("pt-BR").format(quantity);
}

export function LandingPricingSection() {
  return (
    <section id="contratacao" className="bg-white px-5 py-20 sm:px-8">
      <div className="mx-auto max-w-6xl">
        <div className="mx-auto max-w-3xl text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-700">Planos</p>
          <h2 className="mt-3 font-serif text-3xl font-semibold tracking-tight text-slate-900 sm:text-5xl">
            Comece por avaliação. Cresça quando fizer sentido.
          </h2>
          <p className="mt-5 text-lg leading-8 text-slate-600">
            Você paga pelo volume de avaliações, não por assento. Todos os planos mantêm os recursos de
            criação, aplicação e rastreabilidade.
          </p>
          <p className="mt-4 text-sm font-medium text-slate-700">{ANNUAL_CYCLE_DESCRIPTION}</p>
        </div>

        <div className="mt-12 grid gap-6 lg:grid-cols-[0.8fr_1.4fr_0.8fr]">
          <article className="flex flex-col rounded-2xl border border-slate-200 bg-slate-50 p-7 shadow-sm">
            <p className="text-sm font-semibold uppercase tracking-wider text-slate-500">Avulso</p>
            <h3 className="mt-3 text-2xl font-semibold text-slate-900">R$ 69,90</h3>
            <p className="text-sm text-slate-500">por avaliação</p>
            <p className="mt-5 flex-1 leading-7 text-slate-600">
              Para pilotos e demandas pontuais, sem mensalidade ou compromisso recorrente.
            </p>
            <a
              className="mt-7 inline-flex min-h-11 items-center justify-center rounded-full border border-slate-300 px-5 font-semibold text-slate-800 transition hover:border-sky-700 hover:text-sky-700"
              href="/billing"
            >
              Comprar créditos
            </a>
          </article>

          <article className="rounded-2xl border-2 border-sky-700 bg-white p-7 shadow-xl shadow-sky-950/10">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-sm font-semibold uppercase tracking-wider text-sky-700">Profissional</p>
                <h3 className="mt-2 text-2xl font-semibold text-slate-900">Pacotes anuais por volume</h3>
              </div>
              <span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-semibold text-sky-800">
                Mais escolhido
              </span>
            </div>
            <p className="mt-4 leading-7 text-slate-600">
              Para quem avalia com volume recorrente. Quanto maior o pacote anual, menor o preço de cada
              avaliação.
            </p>

            <div className="mt-6 overflow-x-auto">
              <table className="w-full min-w-[480px] border-collapse text-left" aria-label="Pacotes anuais por volume">
                <thead>
                  <tr className="border-b border-slate-200 text-sm text-slate-500">
                    <th className="py-3 pr-4 font-semibold">Avaliações/ano</th>
                    <th className="px-4 py-3 text-right font-semibold">Cada</th>
                    <th className="py-3 pl-4 text-right font-semibold">Total/ano</th>
                  </tr>
                </thead>
                <tbody>
                  {PROFESSIONAL_ANNUAL_TIERS.map((tier) => (
                    <tr key={tier.annualAssessments} className="border-b border-slate-100 last:border-0">
                      <td className="py-4 pr-4 font-semibold text-slate-900">
                        {formatAssessmentQuantity(tier.annualAssessments)}
                      </td>
                      <td className="px-4 py-4 text-right text-slate-700">R$ {tier.unitPrice}</td>
                      <td className="py-4 pl-4 text-right font-semibold text-slate-900">
                        R$ {tier.annualTotal}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <p className="mt-5 rounded-xl bg-sky-50 p-4 text-sm leading-6 text-sky-950">
              Cobrança anual: o pacote completo entra no seu saldo após o pagamento, e o que não for usado
              permanece disponível durante a vigência de 12 meses.
            </p>
            <a
              className="mt-6 inline-flex min-h-11 w-full items-center justify-center rounded-full bg-sky-700 px-5 font-semibold text-white transition hover:bg-sky-800"
              href="/billing"
            >
              Contratar Profissional
            </a>
          </article>

          <article className="flex flex-col rounded-2xl border border-slate-200 bg-slate-900 p-7 text-white shadow-sm">
            <p className="text-sm font-semibold uppercase tracking-wider text-slate-300">Enterprise</p>
            <h3 className="mt-3 text-2xl font-semibold">Sob consulta</h3>
            <p className="mt-5 flex-1 leading-7 text-slate-300">
              Para grandes volumes, integrações, suporte específico e condições contratuais personalizadas.
            </p>
            <a
              className="mt-7 inline-flex min-h-11 items-center justify-center rounded-full bg-white px-5 font-semibold text-slate-900 transition hover:bg-slate-100"
              href="mailto:contato@iforce.com.br?subject=Plano%20Enterprise%20da%20Pr%C3%A1xis"
            >
              Falar com vendas
            </a>
          </article>
        </div>
      </div>
    </section>
  );
}

export function LandingFaqSection() {
  return (
    <section id="faq" className="bg-slate-50 px-5 py-20 sm:px-8">
      <div className="mx-auto max-w-4xl">
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-700">FAQ</p>
          <h2 className="mt-3 font-serif text-3xl font-semibold tracking-tight text-slate-900 sm:text-5xl">
            Perguntas frequentes
          </h2>
        </div>
        <div className="mt-10 divide-y divide-slate-200 overflow-hidden rounded-2xl border border-slate-200 bg-white">
          {FAQ_ITEMS.map((item) => (
            <FaqDisclosure key={item.question} item={item} />
          ))}
        </div>
      </div>
    </section>
  );
}

function FaqDisclosure({ item }: { item: FaqItem }) {
  const [open, setOpen] = useState(false);
  const answerId = useId();

  return (
    <article>
      <h3>
        <button
          type="button"
          className="flex w-full items-center justify-between gap-5 px-6 py-5 text-left font-semibold text-slate-900 transition hover:bg-slate-50"
          aria-expanded={open}
          aria-controls={answerId}
          onClick={() => setOpen((current) => !current)}
        >
          <span>{item.question}</span>
          <span aria-hidden="true" className="text-2xl font-normal text-sky-700">
            {open ? "−" : "+"}
          </span>
        </button>
      </h3>
      {open ? (
        <div id={answerId} className="px-6 pb-6 leading-7 text-slate-600">
          {item.answer}
        </div>
      ) : null}
    </article>
  );
}
