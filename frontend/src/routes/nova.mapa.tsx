import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";

export const Route = createFileRoute("/nova/mapa")({
  head: () => ({
    meta: [
      { title: "Mapa & Score Normalizado" },
      { name: "description", content: "Grafo de turnos e normalização por caminho." },
    ],
  }),
  component: Page,
});

function Page() {
  return (
    <AppShell>
      <WizardStepper current="mapa" />
      <ScreenStateStrip blockedReason="grafo invalido ou caminho morto precisa voltar ao editor" />
      <div className="mb-6">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 5–6</div>
        <h1 className="mt-1 font-display text-3xl">Mapa & score normalizado</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Dois candidatos têm o <b>mesmo teto possível</b>, independente do caminho.
        </p>
      </div>
      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div className="rounded-xl border border-border bg-card p-6">
          <h3 className="mb-4 text-sm font-semibold">"O Dia do Caos" — v1.0</h3>
          <svg viewBox="0 0 720 320" className="h-auto w-full">
            <defs>
              <marker id="arr" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto">
                <path d="M0,0 L6,3 L0,6 Z" className="fill-muted-foreground" />
              </marker>
            </defs>
            <g fontFamily="ui-sans-serif" fontSize="12">
              <g transform="translate(40,140)">
                <rect
                  rx="10"
                  width="120"
                  height="60"
                  className="fill-primary/10 stroke-primary"
                  strokeWidth="1.5"
                />
                <text x="60" y="26" textAnchor="middle" className="fill-primary font-semibold">
                  T1
                </text>
                <text x="60" y="44" textAnchor="middle" className="fill-foreground/70">
                  A·B·C·D
                </text>
              </g>
              {[
                { y: 50, l: "C", c: "stroke-success" },
                { y: 120, l: "B/D", c: "stroke-primary" },
                { y: 200, l: "A", c: "stroke-danger" },
              ].map((b, i) => (
                <g key={i}>
                  <path
                    d={`M160 170 C 240 170, 240 ${b.y + 30}, 320 ${b.y + 30}`}
                    fill="none"
                    className={b.c}
                    strokeWidth="1.5"
                    markerEnd="url(#arr)"
                  />
                  <text
                    x="240"
                    y={b.y + 20}
                    textAnchor="middle"
                    className="fill-muted-foreground text-[11px]"
                  >
                    {b.l}
                  </text>
                </g>
              ))}
              {[
                { y: 50, t: "T2a", n: "encerra cedo", c: "fill-success/10 stroke-success" },
                { y: 120, t: "T2c", n: "segue 2 turnos", c: "fill-primary/10 stroke-primary" },
                { y: 200, t: "T2b", n: "revisão humana", c: "fill-danger/10 stroke-danger" },
              ].map((n) => (
                <g key={n.t} transform={`translate(320,${n.y})`}>
                  <rect rx="10" width="140" height="60" className={n.c} strokeWidth="1.5" />
                  <text x="70" y="26" textAnchor="middle" className="fill-foreground font-semibold">
                    {n.t}
                  </text>
                  <text x="70" y="44" textAnchor="middle" className="fill-foreground/70">
                    {n.n}
                  </text>
                </g>
              ))}
              {[50, 120, 200].map((y, i) => (
                <g key={i}>
                  <path
                    d={`M460 ${y + 30} L 580 ${y + 30}`}
                    fill="none"
                    stroke="currentColor"
                    className="text-muted-foreground"
                    strokeWidth="1.5"
                    markerEnd="url(#arr)"
                  />
                  <g transform={`translate(580,${y})`}>
                    <rect
                      rx="10"
                      width="100"
                      height="60"
                      className="fill-muted stroke-border"
                      strokeWidth="1.5"
                    />
                    <text
                      x="50"
                      y="36"
                      textAnchor="middle"
                      className="fill-foreground font-semibold"
                    >
                      FIM
                    </text>
                  </g>
                </g>
              ))}
            </g>
          </svg>
          <div className="mt-4 grid grid-cols-2 gap-3 text-xs md:grid-cols-4">
            {["A", "B", "C", "D"].map((p) => (
              <div key={p} className="rounded-md border border-border bg-background p-3">
                <div className="font-mono text-[11px] text-muted-foreground">Caminho {p}</div>
                <div className="mt-1 font-display text-xl tabular-nums">máx 100</div>
              </div>
            ))}
          </div>
        </div>
        <aside className="space-y-4">
          <div className="rounded-xl border border-border bg-card p-5">
            <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Fórmula
            </div>
            <pre className="mt-2 overflow-x-auto rounded-md bg-muted/50 p-3 font-mono text-[11px] leading-relaxed">{`score_competência =
  pts_obtidos / pts_possíveis_no_caminho

score_final =
    Empatia    × 40%
  + Resolução  × 35%
  + Processo   × 25%`}</pre>
          </div>
          <div className="rounded-xl border border-danger/30 bg-danger/5 p-5 text-sm">
            <div className="text-xs font-semibold uppercase tracking-wider text-danger">
              Erro crítico ≠ reprova
            </div>
            <p className="mt-2 text-foreground/80">
              Dispara revisão humana, bloqueia recomendação automática.
            </p>
          </div>
        </aside>
      </div>
      <div className="mt-8 flex justify-between">
        <Link
          to="/nova/piloto"
          className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar: Piloto
        </Link>
        <Link
          to="/nova/governanca"
          className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          Governança →
        </Link>
      </div>
    </AppShell>
  );
}
