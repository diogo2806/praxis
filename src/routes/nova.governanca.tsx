import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { WizardStepper } from "@/components/wizard-stepper";

export const Route = createFileRoute("/nova/governanca")({
  head: () => ({ meta: [{ title: "Governança & Aprovações" }, { name: "description", content: "Estados, papéis e versionamento imutável." }] }),
  component: Page,
});

const states = ["Rascunho", "Em revisão (gestor)", "Em revisão (compliance)", "Aprovada", "Publicada"];
const audit = [
  { who: "Renata Silveira", role: "RH · Criadora", w: "10/06 14:22", t: "criou v0.1" },
  { who: "Marcos Lima", role: "Gestor", w: "11/06 09:15", t: "aprovou diálogo" },
  { who: "Validador", role: "Sistema", w: "11/06 09:16", t: "qualidade 82/100 · 2 warnings" },
  { who: "Carla Souza", role: "Compliance", w: "12/06 16:48", t: "aprovou texto e LGPD" },
  { who: "Renata Silveira", role: "RH", w: "13/06 10:02", t: "publicou v1.0 (com warnings)" },
];

function Page() {
  return (
    <AppShell>
      <WizardStepper current="governanca" />
      <div className="mb-6"><div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 7</div><h1 className="mt-1 font-display text-3xl">Governança de publicação</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">RH não publica direto em vaga crítica. Estados, papéis, versionamento imutável.</p></div>
      <div className="rounded-xl border border-border bg-card p-5"><h3 className="text-sm font-semibold">Estado atual</h3>
        <ol className="mt-4 flex flex-wrap gap-2 text-xs">{states.map((s,i)=>(<li key={s} className={`rounded-full border px-3 py-1.5 ${i<3?"border-success/30 bg-success/10 text-success":i===3?"border-primary bg-primary/10 text-primary":"border-border bg-card text-muted-foreground"}`}>{i<3?"✓ ":i===3?"● ":""}{s}</li>))}</ol></div>
      <div className="mt-6 rounded-xl border border-border bg-card p-5">
        <h3 className="text-sm font-semibold">Log de auditoria imutável</h3>
        <ul className="mt-4 divide-y divide-border">{audit.map(a=>(
          <li key={a.w} className="flex items-start gap-3 py-3 text-sm">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-muted text-[11px] font-medium">{a.who[0]}</div>
            <div className="flex-1"><div><span className="font-medium">{a.who}</span> <span className="text-xs text-muted-foreground">· {a.role}</span></div><div className="text-xs text-foreground/75">{a.t}</div></div>
            <div className="text-xs tabular-nums text-muted-foreground">{a.w}</div>
          </li>))}</ul>
      </div>
      <div className="mt-8 flex justify-between"><Link to="/nova/mapa" className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent">← Mapa</Link><Link to="/nova/gupy" className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">Gupy Preflight →</Link></div>
    </AppShell>
  );
}