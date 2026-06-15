import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { WizardStepper } from "@/components/wizard-stepper";

export const Route = createFileRoute("/nova/dialogo")({
  head: () => ({ meta: [{ title: "Editor de Diálogo & Rubricas" }, { name: "description", content: "Todas as alternativas devem ser plausíveis. Pontuação por rubrica padronizada." }] }),
  component: Page,
});

const options = [
  { l: "A", t: "Muito empática, mas promete estorno SEM validar política.", r: { Empatia: 3, Aderência: 0, Resolução: 1 }, critical: true },
  { l: "B", t: "Segue o processo corretamente, mas soa fria.", r: { Empatia: 1, Aderência: 3, Resolução: 2 } },
  { l: "C", t: "Acolhe, coleta dados mínimos e explica o próximo passo.", r: { Empatia: 3, Aderência: 3, Resolução: 3 }, best: true },
  { l: "D", t: "Resolve rápido, mas ignora o registro no sistema.", r: { Empatia: 2, Aderência: 0, Resolução: 2 } },
];
const rubric = [
  { lvl: 0, b: "Ignora a emoção ou culpa terceiros", p: 0 },
  { lvl: 1, b: "Reconhece parcialmente, não acolhe", p: 3 },
  { lvl: 2, b: "Acolhe e assume postura de solução", p: 7 },
  { lvl: 3, b: "Acolhe, prioriza, explica próximos passos", p: 10 },
];

function Page() {
  return (
    <AppShell>
      <WizardStepper current="dialogo" />
      <div className="mb-6">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 3 · Turno 1 de 4</div>
        <h1 className="mt-1 font-display text-3xl">Editor de diálogo</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">Regra de ouro: todas as alternativas devem ser plausíveis. Sem opção obviamente certa.</p>
      </div>
      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div className="space-y-6">
          <section className="rounded-xl border border-border bg-card p-6">
            <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Mensagem do cliente</div>
            <div className="mt-3 rounded-lg bg-muted/50 p-4">
              <span className="text-[11px] uppercase tracking-wider text-muted-foreground">Carlos M. · 14:02</span>
              <p className="mt-1 text-base">"Chegou QUEBRADO! Quero meu dinheiro de volta AGORA!"</p>
            </div>
            <div className="mt-4 grid gap-3 md:grid-cols-[140px_1fr]">
              <label className="block"><span className="mb-1.5 block text-xs font-medium text-muted-foreground">Tempo</span>
                <select className="input"><option>30 s</option><option>45 s</option><option>60 s</option></select>
              </label>
              <label className="block"><span className="mb-1.5 block text-xs font-medium text-muted-foreground">Justifique o tempo</span>
                <input className="input" defaultValue="Atendimento exige resposta rápida sob pressão emocional." />
              </label>
            </div>
          </section>
          <section className="rounded-xl border border-border bg-card p-6">
            <div className="mb-3 flex items-center justify-between">
              <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Opções · 2 a 4 · máx 160 caract.</div>
              <button className="rounded-md border border-border bg-card px-3 py-1.5 text-xs hover:bg-accent">+ opção</button>
            </div>
            <div className="space-y-3">
              {options.map((o) => (
                <div key={o.l} className={`rounded-lg border p-4 ${o.best ? "border-success/40 bg-success/5" : o.critical ? "border-danger/40 bg-danger/5" : "border-border bg-background"}`}>
                  <div className="flex items-start gap-4">
                    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-border bg-card font-medium">{o.l}</div>
                    <div className="flex-1">
                      <div className="text-sm">{o.t}</div>
                      <div className="mt-2 flex flex-wrap gap-1.5">
                        {Object.entries(o.r).map(([k, v]) => (
                          <span key={k} className="rounded-md border border-border bg-card px-2 py-0.5 text-[11px] text-foreground/75">{k} <b>nv {v}</b></span>
                        ))}
                        {o.critical && <span className="rounded-md border border-danger/40 bg-danger/10 px-2 py-0.5 text-[11px] font-medium text-danger">⚠ Erro crítico (revisão humana)</span>}
                        {o.best && <span className="rounded-md border border-success/40 bg-success/10 px-2 py-0.5 text-[11px] font-medium text-success">✓ Comportamento esperado</span>}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
        <aside className="rounded-xl border border-border bg-card p-5">
          <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Rubrica · Empatia</div>
          <p className="mt-1 text-xs text-foreground/70">O ponto vem do <b>nível</b>, não de número inventado. Alinhado a SIOP/APA e ISO 10667.</p>
          <div className="mt-3 divide-y divide-border overflow-hidden rounded-md border border-border">
            {rubric.map((r) => (
              <div key={r.lvl} className="flex items-start gap-3 bg-card p-3 text-xs">
                <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-muted font-medium">{r.lvl}</span>
                <div className="flex-1 text-foreground/80">{r.b}</div>
                <span className="font-medium tabular-nums">{r.p} pt</span>
              </div>
            ))}
          </div>
        </aside>
      </div>
      <div className="mt-8 flex justify-between">
        <Link to="/nova/personagem" className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent">← Personagem</Link>
        <Link to="/nova/validador" className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">Validar qualidade →</Link>
      </div>
    </AppShell>
  );
}