import { createFileRoute, Link } from "@tanstack/react-router";
import { FileText, ShieldCheck } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";

export const Route = createFileRoute("/relatorio/cand-thiago")({
  head: () => ({
    meta: [
      { title: "Relatorio do gestor" },
      { name: "description", content: "Resultado sintetico consumido pelo gestor." },
    ],
  }),
  component: ManagerReport,
});

const scores = [
  { label: "Empatia", value: 86 },
  { label: "Resolucao de conflito", value: 78 },
  { label: "Aderencia a politica", value: 92 },
];

function ManagerReport() {
  return (
    <AppShell>
      <ScreenStateStrip blockedReason="resultado indisponivel ou attempt ainda nao consolidado" />
      <div className="mb-5">
        <div className="text-xs uppercase text-primary">Relatorio do gestor</div>
        <h1 className="mt-1 text-3xl font-semibold">Thiago R. - O Dia do Caos</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Gestor ve evidencia comportamental e recomendacao de entrevista. Nao ve fila Gupy nem
          detalhes de infraestrutura.
        </p>
      </div>

      <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
        <section className="rounded-md border border-border bg-card p-5">
          <div className="mb-4 flex items-center gap-2 text-sm font-semibold">
            <FileText className="h-4 w-4" />
            Resultado
          </div>
          <div className="grid gap-3 md:grid-cols-3">
            {scores.map((score) => (
              <div key={score.label} className="rounded-md border border-border bg-background p-4">
                <div className="text-xs text-muted-foreground">{score.label}</div>
                <div className="mt-1 text-3xl font-semibold tabular-nums">{score.value}</div>
                <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-muted">
                  <div
                    className="h-full rounded-full bg-primary"
                    style={{ width: `${score.value}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
          <div className="mt-5 rounded-md border border-border bg-background p-4">
            <h2 className="text-sm font-semibold">Evidencias para entrevista</h2>
            <ul className="mt-3 space-y-2 text-sm text-foreground/85">
              <li>Equilibrou acolhimento com limite de alcada no primeiro turno.</li>
              <li>Registrou dados minimos antes de oferecer alternativa.</li>
              <li>Nao acionou erro critico de promessa indevida.</li>
            </ul>
          </div>
        </section>

        <aside className="space-y-3">
          <StateBanner tone="ok" title="Enviado a Gupy">
            Resultado entregue uma vez por attemptId. Reenvio duplicado e no-op.
          </StateBanner>
          <StateBanner tone="info" title="Sem nota exata para o candidato">
            O candidato recebeu feedback leve; o gestor recebe a evidencia completa.
          </StateBanner>
          <div className="rounded-md border border-border bg-card p-4">
            <div className="mb-2 flex items-center gap-2 text-sm font-semibold">
              <ShieldCheck className="h-4 w-4" />
              Auditoria
            </div>
            <p className="text-sm text-muted-foreground">
              Score calculado pela versao v1.2. Tentativas em andamento nunca migram de versao.
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
