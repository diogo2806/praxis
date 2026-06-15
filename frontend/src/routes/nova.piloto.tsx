import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { NextStepContract, ScreenStateStrip } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";

export const Route = createFileRoute("/nova/piloto")({
  head: () => ({
    meta: [
      { title: "Piloto & Calibração — Práxis" },
      {
        name: "description",
        content: "Teste com colaboradores referência antes de ir para vaga real.",
      },
    ],
  }),
  component: Page,
});

const piloto = [
  { n: "Mariana (ref.)", s: 92, t: "4m12s", p: "C→C→C", c: 0 },
  { n: "Pedro (ref.)", s: 88, t: "3m47s", p: "C→B→C", c: 0 },
  { n: "Júlia (ref.)", s: 90, t: "4m30s", p: "C→C→B", c: 0 },
  { n: "Lucas (fora)", s: 64, t: "5m02s", p: "A→… (rev.)", c: 1 },
  { n: "Bruna (fora)", s: 71, t: "4m55s", p: "B→C→C", c: 0 },
  { n: "Tiago (fora)", s: 58, t: "6m18s", p: "D→C→A", c: 1 },
];
const check = [
  "Testar com 3 ref.",
  "Testar com 3 fora",
  "Comparar distribuição",
  "Ajustar óbvias",
  "Calibrar tempo",
  "Calibrar pesos",
  "Aprovar v1.0",
];

function Page() {
  return (
    <AppShell>
      <WizardStepper current="piloto" />
      <ScreenStateStrip blockedReason="faltam referencias ou distribuicao minima de piloto" />
      <div className="mb-6">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 4</div>
        <h1 className="mt-1 font-display text-3xl">Piloto & Calibração</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Se os 3 referência não tirarem nota alta, a simulação está medindo a coisa errada.
        </p>
      </div>
      <div className="mb-5">
        <NextStepContract
          primary="Concluir checklist de referencia e seguir para mapa & score."
          secondary="Voltar ao validador ou blueprint antes da publicacao final continua permitido."
          versionRule="Aprovar v1.0 congela resultados do piloto; ajustes posteriores viram v1.1."
          lockedAfter="Candidato de piloto nao muda de versao no meio da tentativa."
        />
      </div>
      <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
        <div className="overflow-hidden rounded-xl border border-border bg-card">
          <table className="w-full text-sm">
            <thead className="border-b border-border bg-muted/40 text-xs uppercase tracking-wider text-muted-foreground">
              <tr>
                <th className="px-4 py-2 text-left font-medium">Participante</th>
                <th className="px-4 py-2 text-left font-medium">Score</th>
                <th className="px-4 py-2 text-left font-medium">Tempo</th>
                <th className="px-4 py-2 text-left font-medium">Caminho</th>
                <th className="px-4 py-2 text-left font-medium">Crítico</th>
              </tr>
            </thead>
            <tbody>
              {piloto.map((p, i) => (
                <tr
                  key={p.n}
                  className={`border-b border-border last:border-0 ${i < 3 ? "bg-success/[0.03]" : ""}`}
                >
                  <td className="px-4 py-3">
                    {p.n}
                    {i < 3 && (
                      <div className="text-[10px] uppercase tracking-wider text-success">
                        Referência
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium tabular-nums">
                      {p.s}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs tabular-nums text-muted-foreground">{p.t}</td>
                  <td className="px-4 py-3 font-mono text-xs text-foreground/75">{p.p}</td>
                  <td className="px-4 py-3">
                    {p.c ? (
                      <span className="rounded-full border border-danger/30 bg-danger/10 px-2 py-0.5 text-[11px] text-danger">
                        ⚠ revisão
                      </span>
                    ) : (
                      <span className="text-xs text-muted-foreground">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <aside className="rounded-xl border border-border bg-card p-5">
          <h3 className="text-sm font-semibold">Checklist</h3>
          <ul className="mt-3 space-y-2">
            {check.map((c, i) => (
              <li key={c} className="flex items-start gap-2 text-sm">
                <input
                  type="checkbox"
                  defaultChecked={i < 5}
                  className="mt-0.5 h-4 w-4 accent-primary"
                />
                <span className={i < 5 ? "line-through opacity-60" : ""}>{c}</span>
              </li>
            ))}
          </ul>
        </aside>
      </div>
      <div className="mt-8 flex justify-between">
        <Link
          to="/nova/validador"
          className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar: Validador
        </Link>
        <Link
          to="/nova/mapa"
          className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          Ver mapa & score →
        </Link>
      </div>
    </AppShell>
  );
}
