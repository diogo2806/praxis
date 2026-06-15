import { createFileRoute, Link } from "@tanstack/react-router";
import { CheckCircle2, FileText, ShieldCheck } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";

export const Route = createFileRoute("/relatorio/cand-thiago")({
  head: () => ({
    meta: [
      { title: "Detalhe do Resultado — Práxis" },
      {
        name: "description",
        content: "Trilha opcional de pontuação, auditoria e explicabilidade do resultado.",
      },
    ],
  }),
  component: ResultDetail,
});

const scores = [
  { label: "Empatia", value: 86 },
  { label: "Resolução de conflito", value: 78 },
  { label: "Aderência à política", value: 92 },
];

function ResultDetail() {
  return (
    <AppShell>
      <ScreenStateStrip blockedReason="resultado indisponível ou tentativa ainda não consolidada" />
      <div className="mb-5">
        <div className="text-xs uppercase text-primary">Detalhe do resultado</div>
        <h1 className="mt-1 text-3xl font-semibold">Thiago R. - O Dia do Caos</h1>
        <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
          Esta tela é opcional e serve para trilha de pontuação, auditoria e explicabilidade. A
          decisão do gestor acontece dentro da Gupy.
        </p>
      </div>

      <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
        <section className="rounded-md border border-border bg-card p-5">
          <div className="mb-4 flex items-center gap-2 text-sm font-semibold">
            <FileText className="h-4 w-4" />
            Evidências estruturadas
          </div>
          <div className="grid gap-3 md:grid-cols-3">
            {scores.map((score) => (
              <div key={score.label} className="rounded-md border border-border bg-background p-4">
                <div className="text-xs text-muted-foreground">{score.label}</div>
                <div className="mt-1 text-3xl font-semibold tabular-nums">{score.value}</div>
                <div className="mt-2 flex items-center gap-2">
                  <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-muted">
                    <div
                      className="h-full rounded-full bg-primary"
                      style={{ width: `${score.value}%` }}
                    />
                  </div>
                  <span className="text-[11px] tabular-nums text-muted-foreground">
                    {score.value}/100
                  </span>
                </div>
              </div>
            ))}
          </div>
          <div className="mt-5 rounded-md border border-border bg-background p-4">
            <h2 className="text-sm font-semibold">Evidências para entrevista</h2>
            <ul className="mt-3 space-y-2 text-sm text-foreground/85">
              <li>Equilibrou acolhimento com limite de alçada no primeiro turno.</li>
              <li>Registrou dados mínimos antes de oferecer alternativa.</li>
              <li>Não acionou erro crítico de promessa indevida.</li>
            </ul>
          </div>
        </section>

        <aside className="space-y-3">
          <div className="rounded-md border border-border bg-card p-4">
            <div className="mb-3 flex items-center gap-2 text-sm font-semibold">
              <CheckCircle2 className="h-4 w-4 text-success" />
              Práxis — Resultado na Gupy
            </div>
            <div className="space-y-2 text-sm">
              <Row label="Score geral" value="78/100" />
              <Row label="Empatia" value="86" />
              <Row label="Resolução de conflito" value="78" />
              <Row label="Aderência à política" value="92" />
              <Row label="Status" value="Recomenda entrevista" />
            </div>
          </div>
          <StateBanner tone="ok" title="Resumo principal enviado à Gupy">
            Este detalhe é opcional. O resumo principal retorna para a candidatura na Gupy.
          </StateBanner>
          <StateBanner tone="info" title="Sem decisão operacional externa">
            Esta página é read-only; a decisão operacional do processo acontece na Gupy.
          </StateBanner>
          <div className="rounded-md border border-border bg-card p-4">
            <div className="mb-2 flex items-center gap-2 text-sm font-semibold">
              <ShieldCheck className="h-4 w-4" />
              Auditoria
            </div>
            <p className="text-sm text-muted-foreground">
              Score calculado pela versão v1.2. Tentativas em andamento nunca migram de versão.
            </p>
          </div>
          <Link
            to="/monitoramento"
            className="inline-flex w-full justify-center rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
          >
            Voltar ao monitoramento
          </Link>
        </aside>
      </div>
    </AppShell>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-md border border-border bg-background px-3 py-2">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}
