import { createFileRoute, Link } from "@tanstack/react-router";
import { AlertTriangle, BarChart3, CheckCircle2 } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { simulations } from "@/lib/mock";

export const Route = createFileRoute("/monitoramento")({
  head: () => ({
    meta: [
      { title: "Monitoramento — Práxis" },
      { name: "description", content: "Acompanhamento pós-publicação e sinais de calibração." },
    ],
  }),
  component: MonitoringPage,
});

const cohorts = [
  { label: "Concluídas", value: 842, pct: 91 },
  { label: "Revisão humana", value: 38, pct: 4 },
  { label: "Falha de envio", value: 3, pct: 0.3 },
  { label: "Abandonadas", value: 44, pct: 5 },
];

function MonitoringPage() {
  const hasData = true;

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="sem dados suficientes para calibração" />
      <div className="mb-5 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Monitoramento</div>
          <h1 className="mt-1 text-3xl font-semibold">Pós-publicação</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Acompanhe calibração, vazamento de prova, maturidade e envio de resultados.
          </p>
        </div>
        <Link
          to="/"
          className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar ao painel
        </Link>
      </div>

      {!hasData ? (
        <EmptyState
          title="Sem dados suficientes ainda"
          description="A simulação precisa de respostas antes de exibir distribuição, vazamento ou calibração."
          actions={
            <Link
              to="/"
              className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
            >
              Voltar ao painel
            </Link>
          }
        />
      ) : (
        <div className="space-y-5">
          <StateBanner tone="warn" title="Sinal de vazamento em observacao">
            Tempo medio caiu 18% em uma semana e escolhas de alto score subiram no mesmo caminho.
          </StateBanner>

          <div className="grid gap-3 md:grid-cols-4">
            {cohorts.map((item) => (
              <div key={item.label} className="rounded-md border border-border bg-card p-4">
                <div className="text-xs uppercase text-muted-foreground">{item.label}</div>
                <div className="mt-1 text-2xl font-semibold tabular-nums">{item.value}</div>
                <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-muted">
                  <div
                    className="h-full rounded-full bg-primary"
                    style={{ width: `${item.pct}%` }}
                  />
                </div>
                <div className="mt-1 text-[11px] text-muted-foreground">{item.pct}%</div>
              </div>
            ))}
          </div>

          <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
            <section className="rounded-md border border-border bg-card p-5">
              <div className="mb-4 flex items-center gap-2 text-sm font-semibold">
                <BarChart3 className="h-4 w-4" />
                Simulacoes monitoradas
              </div>
              <div className="space-y-3">
                {simulations.slice(0, 4).map((simulation) => (
                  <div
                    key={simulation.id}
                    className="grid gap-3 rounded-md border border-border bg-background p-3 md:grid-cols-[1fr_220px_120px]"
                  >
                    <div>
                      <div className="font-medium">{simulation.name}</div>
                      <div className="text-xs text-muted-foreground">{simulation.role}</div>
                    </div>
                    <StatusBadge status={simulation.status} maturity={simulation.maturity} />
                    <Link
                      to="/relatorio/cand-thiago"
                      className="self-center text-xs font-medium text-primary hover:underline"
                    >
                      Ver resultado
                    </Link>
                  </div>
                ))}
              </div>
            </section>

            <aside className="space-y-3">
              <StateBanner tone="ok" title="Calibração ativa">
                Referências internas continuam separadas dos candidatos reais.
              </StateBanner>
              <StateBanner tone="danger" title="Regra de alerta">
                Erro crítico gera revisão humana obrigatória; não decide o processo sozinho.
              </StateBanner>
              <div className="rounded-md border border-border bg-card p-4">
                <div className="mb-3 text-sm font-semibold">Checklist operacional</div>
                <ul className="space-y-2 text-sm">
                  <li className="flex gap-2">
                    <CheckCircle2 className="mt-0.5 h-4 w-4 text-success" />
                    Retry exponencial ativo
                  </li>
                  <li className="flex gap-2">
                    <CheckCircle2 className="mt-0.5 h-4 w-4 text-success" />
                    AuditLog imutável
                  </li>
                  <li className="flex gap-2">
                    <AlertTriangle className="mt-0.5 h-4 w-4 text-warning-foreground" />
                    Revisar caminho T2b após 100 novas respostas
                  </li>
                </ul>
              </div>
            </aside>
          </div>
        </div>
      )}
    </AppShell>
  );
}
