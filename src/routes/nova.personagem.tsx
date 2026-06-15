import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { WizardStepper } from "@/components/wizard-stepper";

export const Route = createFileRoute("/nova/personagem")({
  head: () => ({ meta: [{ title: "Personagem fictício · Nova simulação" }, { name: "description", content: "Personagem do cliente fictício com checklist de linguagem para evitar viés." }] }),
  component: Page,
});

const checklist = [
  "Evita regionalismo desnecessário",
  "Não usa estereótipo de classe",
  "Sem marcador de gênero sem necessidade",
  "Sem referência a idade, sotaque, origem ou crença",
  "Linguagem compatível com o cargo avaliado",
];

function Page() {
  return (
    <AppShell>
      <WizardStepper current="personagem" />
      <div className="mb-6">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 2</div>
        <h1 className="mt-1 font-display text-3xl">Personagem do cliente fictício</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">A linguagem é manualmente auditada para reduzir viés. Nada é classificado por modelo.</p>
      </div>
      <div className="rounded-xl border border-border bg-card p-6">
        <div className="grid gap-4 md:grid-cols-2">
          <label className="block"><span className="mb-1.5 block text-xs font-medium text-muted-foreground">Nome</span><input className="input" defaultValue="Carlos M." /></label>
          <label className="block"><span className="mb-1.5 block text-xs font-medium text-muted-foreground">Perfil emocional inicial</span>
            <div className="grid grid-cols-3 gap-2">
              {["Tranquilo", "Frustrado", "Furioso"].map((p, i) => (
                <label key={p} className={`cursor-pointer rounded-md border px-3 py-2 text-center text-sm ${i === 2 ? "border-primary bg-primary/5 text-primary" : "border-border bg-card"}`}>
                  <input type="radio" name="emo" defaultChecked={i === 2} className="sr-only" />{p}
                </label>
              ))}
            </div>
          </label>
        </div>
        <label className="mt-4 block">
          <span className="mb-1.5 block text-xs font-medium text-muted-foreground">Contexto (RH/gestor)</span>
          <textarea className="input min-h-24" defaultValue="Produto com defeito, presente do filho. Cliente espera resposta rápida." />
        </label>
        <div className="mt-6 rounded-lg border border-warning/30 bg-warning/10 p-4">
          <div className="text-sm font-semibold text-warning-foreground">⚠️ Checklist de linguagem · manual, sem IA</div>
          <ul className="mt-3 space-y-2">
            {checklist.map((c, i) => (
              <li key={c} className="flex items-start gap-3 text-sm">
                <input type="checkbox" defaultChecked={i < 3} className="mt-0.5 h-4 w-4 accent-primary" />
                <span>{c}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
      <div className="mt-8 flex justify-between">
        <Link to="/nova/objetivo" className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent">← Objetivo</Link>
        <Link to="/nova/dialogo" className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">Montar diálogo →</Link>
      </div>
    </AppShell>
  );
}