import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { AlertTriangle, CheckCircle2, ExternalLink, ShieldCheck, XCircle } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { NextStepContract, ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/nova/validador")({
  head: () => ({
    meta: [
      { title: "Validador de Qualidade" },
      {
        name: "description",
        content: "Diagnostico deterministico, sem IA, com score 0-100 e bloqueios de publicacao.",
      },
    ],
  }),
  component: ValidatorPage,
});

const breakdown = [
  { label: "Estrutura do grafo", max: 20, got: 20 },
  { label: "Cobertura de competencias", max: 20, got: 18 },
  { label: "Equilibrio de score", max: 20, got: 12 },
  { label: "Rubricas completas", max: 15, got: 15 },
  { label: "Governanca", max: 10, got: 8 },
  { label: "Fluxo do candidato", max: 10, got: 7 },
  { label: "Preflight Gupy", max: 5, got: 3 },
];

const checks = [
  {
    id: "paths",
    tone: "ok",
    text: "Todos os caminhos tem desfecho",
    target: "Mapa: T2a -> FIM",
  },
  {
    id: "evidence",
    tone: "ok",
    text: "Cada competencia tem pelo menos 3 evidencias",
    target: "Rubricas: Empatia",
  },
  {
    id: "obvious",
    tone: "warn",
    text: "Opcao C pode estar obvia demais; confirmar no piloto",
    target: "Editor: T1 opcao C",
  },
  {
    id: "mobile-time",
    tone: "warn",
    text: "Tempo de 20s parece agressivo para mobile",
    target: "Editor: T3 cronometro",
  },
  {
    id: "score-balance",
    tone: "danger",
    text: "Caminho 3C permite score maximo 28% maior que o 2A",
    target: "Mapa: caminho 3C",
  },
  {
    id: "positioning",
    tone: "danger",
    text: 'Texto ainda sugere "transcricao real"; deve ser julgamento situacional',
    target: "Blueprint: promessa comercial",
  },
] as const;

function ValidatorPage() {
  const [publicationState, setPublicationState] = useState<"blocker" | "warning" | "clear">(
    "blocker",
  );
  const [highlight, setHighlight] = useState("score-balance");
  const [modalOpen, setModalOpen] = useState(false);
  const total = breakdown.reduce((sum, item) => sum + item.got, 0);
  const activeChecks = checks.filter((check) =>
    publicationState === "clear"
      ? check.tone === "ok"
      : publicationState === "warning"
        ? check.tone !== "danger"
        : true,
  );
  const blockers = activeChecks.filter((check) => check.tone === "danger").length;
  const warnings = activeChecks.filter((check) => check.tone === "warn").length;

  return (
    <AppShell>
      <WizardStepper current="validador" />
      <ScreenStateStrip blockedReason="qualquer blocker remove a acao de publicar" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 3.5</div>
          <h1 className="mt-1 text-3xl font-semibold">Validador de Qualidade</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Regras deterministicas. Sem IA, sem interpretacao de texto livre, sem override de
            blocker.
          </p>
        </div>
        <div className="inline-flex rounded-md border border-border bg-card p-1 text-xs">
          {(["blocker", "warning", "clear"] as const).map((state) => (
            <button
              key={state}
              type="button"
              onClick={() => setPublicationState(state)}
              className={cn(
                "rounded px-3 py-1.5",
                publicationState === state && "bg-primary text-primary-foreground",
              )}
            >
              {state === "blocker" ? "Com blocker" : state === "warning" ? "So warning" : "Pronta"}
            </button>
          ))}
        </div>
      </div>

      {blockers > 0 ? (
        <StateBanner tone="danger" title={`Publicacao bloqueada - ${blockers} item critico`}>
          O botao Publicar nao aparece. Resolva o blocker ou salve como rascunho.
        </StateBanner>
      ) : warnings > 0 ? (
        <StateBanner tone="warn" title={`Pode publicar - ${warnings} alertas registrados`}>
          A confirmacao registra os alertas no log de auditoria antes de publicar.
        </StateBanner>
      ) : (
        <StateBanner tone="ok" title="Pronta para publicar">
          Sem blocker ou warning ativo. A publicacao usa a versao imutavel atual.
        </StateBanner>
      )}

      <div className="mt-5">
        <NextStepContract
          primary={
            blockers > 0
              ? "Voltar ao editor. Piloto e publicacao ficam travados."
              : warnings > 0
                ? "Confirmar publicacao com alertas gravados no AuditLog."
                : "Publicar versao imutavel e seguir para piloto."
          }
          secondary="Salvar rascunho nunca publica; volta ao editor mantendo diagnostico clicavel."
          versionRule="Depois de publicar, editar cria nova versao e preserva a publicada."
          lockedAfter="Nao existe override manual para blocker critico."
        />
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-[360px_minmax(0,1fr)]">
        <section className="rounded-md border border-border bg-card p-5">
          <div className="text-xs uppercase text-muted-foreground">Score de qualidade</div>
          <div className="mt-2 flex items-end gap-2">
            <div className="text-6xl font-semibold tabular-nums">{total}</div>
            <div className="mb-2 text-sm text-muted-foreground">/100</div>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            A primeira coisa que o RH ve. Barras seguem os pesos da formula.
          </p>
          <div className="mt-4 space-y-3">
            {breakdown.map((item) => {
              const pct = item.got / item.max;
              return (
                <div key={item.label}>
                  <div className="flex justify-between text-[11px]">
                    <span>{item.label}</span>
                    <span className="tabular-nums text-muted-foreground">
                      {item.got}/{item.max}
                    </span>
                  </div>
                  <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-muted">
                    <div
                      className={cn(
                        "h-full rounded-full",
                        pct >= 0.8 ? "bg-success" : pct >= 0.6 ? "bg-warning" : "bg-danger",
                      )}
                      style={{ width: `${pct * 100}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        <section className="rounded-md border border-border bg-card p-5">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-sm font-semibold">Diagnostico clicavel</h2>
            <span className="rounded-md border border-border bg-background px-2 py-1 text-[11px] text-muted-foreground">
              clique para destacar no editor
            </span>
          </div>
          <ul className="divide-y divide-border">
            {activeChecks.map((check) => (
              <li key={check.id}>
                <button
                  type="button"
                  onClick={() => setHighlight(check.id)}
                  className={cn(
                    "flex w-full items-start gap-3 py-3 text-left text-sm hover:bg-accent/40",
                    highlight === check.id && "bg-accent/50",
                  )}
                >
                  <CheckIcon tone={check.tone} />
                  <span className="min-w-0 flex-1">
                    <span className="block text-foreground/90">{check.text}</span>
                    <span className="mt-1 inline-flex items-center gap-1 text-xs text-primary">
                      <ExternalLink className="h-3 w-3" />
                      Ir para {check.target}
                    </span>
                  </span>
                </button>
              </li>
            ))}
          </ul>

          <div className="mt-5 rounded-md border border-border bg-background p-4">
            <div className="text-xs uppercase text-muted-foreground">Highlight atual</div>
            <div className="mt-1 text-sm font-medium">
              {activeChecks.find((check) => check.id === highlight)?.target ?? "Editor: T1"}
            </div>
            <p className="mt-1 text-xs text-muted-foreground">
              No produto real, este clique abre o turno/opcao e aplica destaque temporario.
            </p>
          </div>

          <div className="mt-5 flex flex-wrap justify-between gap-3">
            <Link
              to="/nova/dialogo"
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar ao editor
            </Link>
            {blockers > 0 ? (
              <button className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent">
                Salvar rascunho
              </button>
            ) : warnings > 0 ? (
              <button
                type="button"
                onClick={() => setModalOpen(true)}
                className="rounded-md bg-warning px-5 py-2 text-sm font-medium text-warning-foreground hover:opacity-90"
              >
                Publicar com alerta registrado
              </button>
            ) : (
              <Link
                to="/nova/piloto"
                className="rounded-md bg-success px-5 py-2 text-sm font-medium text-success-foreground hover:opacity-90"
              >
                Publicar
              </Link>
            )}
          </div>
        </section>
      </div>

      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-foreground/30 p-4">
          <div className="w-full max-w-md rounded-md border border-border bg-card p-5 shadow-xl">
            <div className="flex items-center gap-2 text-sm font-semibold">
              <ShieldCheck className="h-4 w-4 text-warning-foreground" />
              Registrar alertas no AuditLog
            </div>
            <p className="mt-2 text-sm text-muted-foreground">
              Estes alertas ficarao anexados a versao publicada e nao expiram. A auditoria
              conseguira ver quem publicou e quando.
            </p>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
              >
                Cancelar
              </button>
              <Link
                to="/nova/piloto"
                className="rounded-md bg-warning px-4 py-2 text-sm font-medium text-warning-foreground"
              >
                Confirmar e publicar
              </Link>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
}

function CheckIcon({ tone }: { tone: "ok" | "warn" | "danger" }) {
  const cls =
    tone === "ok"
      ? "bg-success/15 text-success"
      : tone === "warn"
        ? "bg-warning/20 text-warning-foreground"
        : "bg-danger/15 text-danger";
  const Icon = tone === "ok" ? CheckCircle2 : tone === "warn" ? AlertTriangle : XCircle;
  return (
    <span
      className={cn("mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full", cls)}
    >
      <Icon className="h-4 w-4" />
    </span>
  );
}
