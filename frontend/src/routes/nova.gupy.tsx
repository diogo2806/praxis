import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { CheckCircle2, RefreshCw, ShieldAlert, XCircle } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";

export const Route = createFileRoute("/nova/gupy")({
  head: () => ({
    meta: [
      { title: "Gupy Preflight" },
      { name: "description", content: "Checklist final antes de vincular simulacao a vaga Gupy." },
    ],
  }),
  component: GupyPreflight,
});

const preflight = [
  { label: "Token Gupy valido", ok: true, hint: "expira em 22 dias" },
  { label: "Plano com etapa customizada", ok: true, hint: "recurso ativo" },
  { label: "Vaga Atendimento N2 encontrada", ok: true, hint: "ID GUPY-8421" },
  { label: "Webhook de resultado configurado", ok: true, hint: "idempotencyKey = attemptId" },
  { label: "Versao sem blocker", ok: true, hint: "warnings registrados" },
];

const attempts = [
  { id: "ATT-1832", candidate: "Thiago R.", status: "Enviado a Gupy", tone: "ok" },
  { id: "ATT-1833", candidate: "Marina F.", status: "Reenviando", tone: "warn" },
  { id: "ATT-1834", candidate: "Joao P.", status: "Falha - na fila DLQ", tone: "danger" },
];

function GupyPreflight() {
  const [adminMode, setAdminMode] = useState(true);
  const [failed, setFailed] = useState(false);
  const items = failed
    ? preflight.map((item, index) =>
        index === 0 ? { ...item, ok: false, hint: "token expirado; reconecte a Gupy" } : item,
      )
    : preflight;
  const hasFailure = items.some((item) => !item.ok);

  return (
    <AppShell>
      <WizardStepper current="gupy" />
      <ScreenStateStrip blockedReason="preflight falho bloqueia vinculo com a vaga" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 8</div>
          <h1 className="mt-1 text-3xl font-semibold">Gupy Preflight Check</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Falha bloqueia publicacao antes do vinculo. Sucesso muda a simulacao para publicada.
          </p>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setFailed((value) => !value)}
            className="rounded-md border border-border bg-card px-3 py-2 text-xs hover:bg-accent"
          >
            Alternar erro
          </button>
          <label className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-xs">
            <input
              type="checkbox"
              checked={adminMode}
              onChange={(event) => setAdminMode(event.target.checked)}
            />
            Admin integracao
          </label>
        </div>
      </div>

      {hasFailure ? (
        <StateBanner tone="danger" title="Preflight falhou - simulacao nao publicada">
          Corrija o item em vermelho. Erro de contrato 4xx nao retenta; vai para inspeção.
        </StateBanner>
      ) : (
        <StateBanner tone="ok" title="Simulacao vinculada a vaga Atendimento N2">
          Status atualizado para PUBLISHED. O candidato recebe link da etapa Gupy.
        </StateBanner>
      )}

      <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
        <section className="rounded-md border border-border bg-card p-5">
          <h2 className="text-sm font-semibold">Checklist por item</h2>
          <div className="mt-4 space-y-3">
            {items.map((item) => (
              <div
                key={item.label}
                className={`flex items-start gap-3 rounded-md border p-3 ${
                  item.ok ? "border-success/20 bg-success/5" : "border-danger/30 bg-danger/5"
                }`}
              >
                {item.ok ? (
                  <CheckCircle2 className="mt-0.5 h-4 w-4 text-success" />
                ) : (
                  <XCircle className="mt-0.5 h-4 w-4 text-danger" />
                )}
                <div>
                  <div className="text-sm font-medium">{item.label}</div>
                  <div className="text-xs text-muted-foreground">{item.hint}</div>
                </div>
              </div>
            ))}
          </div>
          <div className="mt-5 flex justify-between">
            <Link
              to="/nova/governanca"
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar
            </Link>
            <button
              disabled={hasFailure}
              className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Publicar e vincular
            </button>
          </div>
        </section>

        <aside className="rounded-md border border-border bg-card p-5">
          <h2 className="text-sm font-semibold">Status de envio do resultado</h2>
          <div className="mt-4 space-y-3">
            {attempts.map((attempt) => (
              <div key={attempt.id} className="rounded-md border border-border bg-background p-3">
                <div className="flex items-center justify-between gap-2">
                  <div>
                    <div className="text-sm font-medium">{attempt.candidate}</div>
                    <div className="text-xs text-muted-foreground">{attempt.id}</div>
                  </div>
                  <span
                    className={`rounded-md px-2 py-1 text-[11px] ${
                      attempt.tone === "ok"
                        ? "bg-success/10 text-success"
                        : attempt.tone === "warn"
                          ? "bg-warning/15 text-warning-foreground"
                          : "bg-danger/10 text-danger"
                    }`}
                  >
                    {attempt.status}
                  </span>
                </div>
                {attempt.tone === "danger" && adminMode && (
                  <button className="mt-3 inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-1.5 text-xs hover:bg-accent">
                    <RefreshCw className="h-3.5 w-3.5" />
                    Reprocessar
                  </button>
                )}
              </div>
            ))}
          </div>
          {!adminMode && (
            <div className="mt-4 flex items-start gap-2 rounded-md border border-border bg-muted/40 p-3 text-xs text-muted-foreground">
              <ShieldAlert className="mt-0.5 h-3.5 w-3.5" />
              RH comum nao ve fila DLQ nem reprocessamento manual.
            </div>
          )}
        </aside>
      </div>
    </AppShell>
  );
}
