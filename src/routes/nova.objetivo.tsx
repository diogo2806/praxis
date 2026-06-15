import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { WizardStepper } from "@/components/wizard-stepper";

export const Route = createFileRoute("/nova/objetivo")({
  head: () => ({
    meta: [
      { title: "Objetivo & Modelo · Nova simulação" },
      { name: "description", content: "Escolha por objetivo, não só por área. Dificuldade é calculada a partir de 5 dimensões." },
    ],
  }),
  component: Page,
});

const objetivos = [
  { t: "Lidar com conflito", d: "Cliente furioso, colega irritado, deadline em risco" },
  { t: "Priorizar sob pressão", d: "Múltiplas demandas competindo pela mesma hora" },
  { t: "Negociar sem dar desconto", d: "Preservar margem mantendo relação" },
  { t: "Comunicar uma negativa", d: "Dizer não com clareza, sem perder o vínculo" },
  { t: "Recuperar cliente em risco", d: "Reverter churn iminente em conta grande" },
  { t: "Escalar problema corretamente", d: "Pedir ajuda sem terceirizar a decisão" },
];

const dimensions = [
  { label: "Intensidade emocional", value: 70 },
  { label: "Ambiguidade da informação", value: 35 },
  { label: "Risco de negócio", value: 80 },
  { label: "Conflito empatia × política", value: 65 },
  { label: "Autonomia exigida", value: 55 },
];

function Page() {
  return (
    <AppShell>
      <WizardStepper current="objetivo" />
      <div className="mb-8">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 1</div>
        <h1 className="mt-1 font-display text-3xl">O que você quer avaliar?</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Em vez de só "Atendimento / Vendas / Liderança", escolha o objetivo
          comportamental — aproxima a simulação do problema real do cargo.
        </p>
      </div>
      <div className="grid gap-3 md:grid-cols-3">
        {objetivos.map((o, i) => (
          <button key={o.t} className={`text-left rounded-xl border p-5 transition ${i === 0 ? "border-primary bg-primary/5 shadow-sm" : "border-border bg-card hover:border-primary/40 hover:bg-accent/50"}`}>
            <div className="flex items-start justify-between">
              <div className="font-medium">{o.t}</div>
              <span className={`h-5 w-5 rounded-full border-2 ${i === 0 ? "border-primary bg-primary" : "border-border"}`} />
            </div>
            <p className="mt-1 text-xs text-muted-foreground">{o.d}</p>
          </button>
        ))}
      </div>
      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <div className="rounded-xl border border-border bg-card p-6">
          <h2 className="font-display text-xl">Começar de…</h2>
          <p className="mt-1 text-sm text-muted-foreground">Modelos são pontos de partida — toda simulação passa pelo Blueprint, Validador e Piloto antes de ir para vaga crítica.</p>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <div className="rounded-lg border border-primary/40 bg-primary/5 p-4">
              <div className="text-xs font-semibold uppercase tracking-wider text-primary">Modelo pronto</div>
              <div className="mt-1 font-medium">"O Dia do Caos"</div>
              <p className="text-xs text-muted-foreground">Atendimento · Pleno · 4 turnos · calibrado em 5 empresas</p>
            </div>
            <div className="rounded-lg border border-border bg-card p-4 hover:bg-accent/50">
              <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Do zero</div>
              <div className="mt-1 font-medium">Começar em branco</div>
              <p className="text-xs text-muted-foreground">Sem turnos pré-escritos. Exige mais calibração.</p>
            </div>
          </div>
        </div>
        <div className="rounded-xl border border-border bg-card p-6">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-xl">Dificuldade calculada</h2>
            <span className="rounded-full border border-warning/30 bg-warning/15 px-2 py-0.5 text-[11px] font-medium text-warning-foreground">INTENSO</span>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">"Leve / Moderado / Intenso" é vago. Aqui sai de 5 dimensões.</p>
          <div className="mt-4 space-y-3">
            {dimensions.map((d) => (
              <div key={d.label}>
                <div className="mb-1 flex justify-between text-xs">
                  <span className="text-foreground/80">{d.label}</span>
                  <span className="tabular-nums text-muted-foreground">{d.value}</span>
                </div>
                <div className="h-1.5 overflow-hidden rounded-full bg-muted">
                  <div className="h-full rounded-full bg-primary" style={{ width: `${d.value}%` }} />
                </div>
              </div>
            ))}
          </div>
          <p className="mt-4 rounded-md bg-muted/60 p-3 text-xs text-foreground/80">Há conflito real entre empatia e política — não só cliente gritando. Marcado como <b>Intenso</b>.</p>
        </div>
      </div>
      <div className="mt-8 flex justify-between">
        <Link to="/nova/blueprint" className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent">← Blueprint</Link>
        <Link to="/nova/personagem" className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">Personagem →</Link>
      </div>
    </AppShell>
  );
}