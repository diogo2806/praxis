import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { WizardStepper } from "@/components/wizard-stepper";

export const Route = createFileRoute("/nova/validador")({
  head: () => ({ meta: [{ title: "Validador de Qualidade" }, { name: "description", content: "Diagnóstico determinístico, sem IA, com fórmula 100 pts." }] }),
  component: Page,
});

const breakdown = [
  { l: "Estrutura do grafo (sem caminho morto)", max: 20, got: 20 },
  { l: "Cobertura de competências", max: 20, got: 18 },
  { l: "Equilíbrio de score por caminho", max: 20, got: 12 },
  { l: "Rubricas completas", max: 15, got: 15 },
  { l: "Governança / versionamento", max: 10, got: 8 },
  { l: "Fluxo do candidato", max: 10, got: 6 },
  { l: "Integração / preflight", max: 5, got: 3 },
];
const checks = [
  { i: "✓", tone: "ok", t: "Todos os caminhos têm desfecho" },
  { i: "✓", tone: "ok", t: "Cada competência tem ≥3 evidências" },
  { i: "✓", tone: "ok", t: "Pontuação normalizada por caminho" },
  { i: "!", tone: "warn", t: "Opção C óbvia? (confirmar no piloto)" },
  { i: "!", tone: "warn", t: "Tempo de 20s parece agressivo p/ mobile" },
  { i: "✕", tone: "danger", t: "Caminho 3C permite score máx. 28% maior que o 2A" },
  { i: "✕", tone: "danger", t: 'Risco: não vender como "transcrição real"' },
];

function Page() {
  const total = breakdown.reduce((a, b) => a + b.got, 0);
  return (
    <AppShell>
      <WizardStepper current="validador" />
      <div className="mb-6">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 3.5</div>
        <h1 className="mt-1 font-display text-3xl">Validador de Qualidade</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">Regras determinísticas — <b>sem IA</b>. Sistema especialista, não editor.</p>
      </div>
      <div className="grid gap-6 lg:grid-cols-[340px_1fr]">
        <div className="rounded-2xl border border-border bg-card p-6">
          <div className="text-xs uppercase tracking-wider text-muted-foreground">Qualidade</div>
          <div className="mt-1 flex items-end gap-2">
            <div className="font-display text-6xl tabular-nums">{total}</div>
            <div className="mb-2 text-sm text-muted-foreground">/100</div>
          </div>
          <div className="mt-2 text-xs text-warning-foreground">Publicar como <b>PILOTO</b>, não eliminatória.</div>
          <div className="mt-4 space-y-2">
            {breakdown.map((r) => (
              <div key={r.l}>
                <div className="flex justify-between text-[11px]"><span>{r.l}</span><span className="tabular-nums text-muted-foreground">{r.got}/{r.max}</span></div>
                <div className="mt-0.5 h-1 overflow-hidden rounded-full bg-muted">
                  <div className={`h-full ${r.got/r.max>=0.8?"bg-success":r.got/r.max>=0.6?"bg-warning":"bg-danger"}`} style={{ width: `${(r.got/r.max)*100}%` }} />
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="space-y-6">
          <section className="rounded-xl border border-border bg-card p-5">
            <h3 className="mb-3 text-sm font-semibold">Diagnóstico</h3>
            <ul className="divide-y divide-border">
              {checks.map((c) => (
                <li key={c.t} className="flex items-start gap-3 py-2.5 text-sm">
                  <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[11px] font-bold ${c.tone==="ok"?"bg-success/15 text-success":c.tone==="warn"?"bg-warning/20 text-warning-foreground":"bg-danger/15 text-danger"}`}>{c.i}</span>
                  <span className="text-foreground/85">{c.t}</span>
                </li>
              ))}
            </ul>
          </section>
          <div className="flex flex-wrap justify-between gap-3">
            <Link to="/nova/dialogo" className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent">← Editor</Link>
            <Link to="/nova/piloto" className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">Corrigir & ir ao piloto →</Link>
          </div>
        </div>
      </div>
    </AppShell>
  );
}