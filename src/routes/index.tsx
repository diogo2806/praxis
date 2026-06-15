import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { simulations, statusMeta } from "@/lib/mock";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Painel · Motor de Avaliação Situacional" },
      {
        name: "description",
        content:
          "Painel da Renata: simulações ativas, qualidade, maturidade, vínculo com vagas Gupy.",
      },
    ],
  }),
  component: Dashboard,
});

const toneClass = {
  ok: "bg-success/10 text-success border-success/20",
  warn: "bg-warning/15 text-warning-foreground border-warning/30",
  info: "bg-primary/10 text-primary border-primary/20",
  danger: "bg-danger/10 text-danger border-danger/20",
  muted: "bg-muted text-muted-foreground border-border",
} as const;

function Dashboard() {
  const totals = {
    publicadas: simulations.filter((s) => s.status === "publicada").length,
    piloto: simulations.filter((s) => s.status === "piloto").length,
    rascunhos: simulations.filter((s) => s.status === "rascunho").length,
    tentativas: simulations.reduce((a, s) => a + s.attempts, 0),
  };

  return (
    <AppShell>
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="text-xs uppercase tracking-[0.2em] text-muted-foreground">
            Painel
          </div>
          <h1 className="font-display text-4xl text-foreground">
            Boa tarde, Renata.
          </h1>
          <p className="mt-1 max-w-xl text-sm text-muted-foreground">
            Avaliação situacional estruturada para recrutamento, sem IA julgando o candidato.
            Score por rubrica, trilha auditável, integrada à Gupy.
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            to="/monitoramento"
            className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
          >
            Ver monitoramento
          </Link>
          <Link
            to="/nova/blueprint"
            className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-sm transition hover:bg-primary/90"
          >
            + Nova simulação
          </Link>
        </div>
      </div>

      <div className="mb-8 grid grid-cols-2 gap-3 md:grid-cols-4">
        <Stat label="Publicadas" value={totals.publicadas} hint="Em vagas ativas" />
        <Stat label="Em piloto" value={totals.piloto} hint="Ranqueiam · não eliminam" />
        <Stat label="Rascunhos" value={totals.rascunhos} hint="Esperando blueprint" />
        <Stat
          label="Tentativas (30d)"
          value={totals.tentativas}
          hint="91% taxa de conclusão"
        />
      </div>

      <div className="mb-3 flex items-end justify-between">
        <div>
          <h2 className="font-display text-xl">Simulações</h2>
          <p className="text-xs text-muted-foreground">
            Toda candidatura responde a uma versão imutável.
          </p>
        </div>
        <div className="flex gap-1 text-xs text-muted-foreground">
          <button className="rounded border border-border bg-card px-2 py-1">Todas</button>
          <button className="rounded border border-transparent px-2 py-1 hover:bg-accent">
            Publicadas
          </button>
          <button className="rounded border border-transparent px-2 py-1 hover:bg-accent">
            Piloto
          </button>
          <button className="rounded border border-transparent px-2 py-1 hover:bg-accent">
            Rascunhos
          </button>
        </div>
      </div>

      <div className="overflow-hidden rounded-xl border border-border bg-card">
        <table className="w-full text-sm">
          <thead className="border-b border-border bg-muted/40 text-xs uppercase tracking-wider text-muted-foreground">
            <tr>
              <th className="px-4 py-3 text-left font-medium">Simulação</th>
              <th className="px-4 py-3 text-left font-medium">Status</th>
              <th className="px-4 py-3 text-left font-medium">Maturidade</th>
              <th className="px-4 py-3 text-left font-medium">Qualidade</th>
              <th className="px-4 py-3 text-left font-medium">Versão</th>
              <th className="px-4 py-3 text-left font-medium">Tentativas</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {simulations.map((s) => {
              const m = statusMeta[s.status];
              return (
                <tr key={s.id} className="border-b border-border last:border-0 hover:bg-accent/30">
                  <td className="px-4 py-3">
                    <div className="font-medium text-foreground">{s.name}</div>
                    <div className="text-xs text-muted-foreground">
                      {s.role} · {s.seniority} · atualizada {s.updated}
                    </div>
                    <div className="mt-1 flex flex-wrap gap-1">
                      {s.competencies.map((c) => (
                        <span
                          key={c}
                          className="rounded-full border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground"
                        >
                          {c}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={cn(
                        "inline-flex items-center gap-1.5 rounded-full border px-2 py-0.5 text-[11px] font-medium",
                        toneClass[m.tone],
                      )}
                    >
                      <span className="h-1.5 w-1.5 rounded-full bg-current opacity-70" />
                      {m.label}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-foreground/80">{s.maturity}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <div className="h-1.5 w-24 overflow-hidden rounded-full bg-muted">
                        <div
                          className={cn(
                            "h-full rounded-full",
                            s.quality >= 80
                              ? "bg-success"
                              : s.quality >= 60
                                ? "bg-warning"
                                : "bg-danger",
                          )}
                          style={{ width: `${s.quality}%` }}
                        />
                      </div>
                      <span className="text-xs font-medium tabular-nums">
                        {s.quality}/100
                      </span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-xs tabular-nums text-muted-foreground">
                    {s.version}
                  </td>
                  <td className="px-4 py-3 text-xs tabular-nums">
                    {s.attempts.toLocaleString("pt-BR")}
                    <div className="text-[10px] text-muted-foreground">
                      {Math.round(s.completion * 100)}% concluem
                    </div>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Link
                      to="/nova/validador"
                      className="text-xs font-medium text-primary hover:underline"
                    >
                      Abrir →
                    </Link>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <div className="mt-10 grid gap-4 md:grid-cols-3">
        <PromiseCard
          title="Não promete"
          tone="danger"
          items={[
            "Não usa IA para julgar candidato",
            "Não avalia texto livre no MVP",
            "Não recomenda eliminação automática",
            "Não substitui entrevista humana",
          ]}
        />
        <PromiseCard
          title="Promete (defensável)"
          tone="ok"
          items={[
            "Avaliação situacional estruturada",
            "Score por rubrica padronizada",
            "Trilha de pontuação auditável",
            "Versionamento imutável por candidatura",
          ]}
        />
        <PromiseCard
          title="Próximas calibrações"
          tone="info"
          items={[
            "‘Recuperar cliente’ → piloto com 3 referência",
            "‘O Dia do Caos’ v1.3 → ajustar tempo T2",
            "Revisar família de variações de ‘Negociar’",
            "Recalibrar pesos após 1.000 tentativas",
          ]}
        />
      </div>
    </AppShell>
  );
}

function Stat({ label, value, hint }: { label: string; value: number; hint: string }) {
  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <div className="text-xs uppercase tracking-wider text-muted-foreground">{label}</div>
      <div className="mt-1 font-display text-3xl tabular-nums text-foreground">
        {value.toLocaleString("pt-BR")}
      </div>
      <div className="mt-1 text-[11px] text-muted-foreground">{hint}</div>
    </div>
  );
}

function PromiseCard({
  title,
  items,
  tone,
}: {
  title: string;
  items: string[];
  tone: "ok" | "danger" | "info";
}) {
  const accent =
    tone === "ok" ? "bg-success" : tone === "danger" ? "bg-danger" : "bg-primary";
  return (
    <div className="relative overflow-hidden rounded-xl border border-border bg-card p-5">
      <span className={cn("absolute left-0 top-0 h-full w-1", accent)} />
      <div className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
        {title}
      </div>
      <ul className="space-y-1.5 text-sm">
        {items.map((i) => (
          <li key={i} className="flex gap-2 text-foreground/85">
            <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-muted-foreground/50" />
            {i}
          </li>
        ))}
      </ul>
    </div>
  );
}
