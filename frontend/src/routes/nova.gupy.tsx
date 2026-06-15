import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { CheckCircle2, RefreshCw, Server, ShieldAlert, Webhook, XCircle } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";

export const Route = createFileRoute("/nova/gupy")({
  head: () => ({
    meta: [
      { title: "Gupy - Ativacao & Conferencia" },
      {
        name: "description",
        content:
          "Conferencia dos endpoints REST expostos pelo Praxis para a integracao de testes externos da Gupy.",
      },
    ],
  }),
  component: GupyActivation,
});

const endpoints = [
  {
    method: "GET",
    path: "/test",
    description: "lista nossas simulacoes PUBLICADAS como Test[]",
    status: "no ar",
    ok: true,
  },
  {
    method: "POST",
    path: "/test/candidate",
    description: "registra candidato; devolve test_url + test_result_id",
    status: "no ar",
    ok: true,
  },
  {
    method: "GET",
    path: "/test/result/{resultId}",
    description: "devolve TestResult com score e competencias",
    status: "no ar",
    ok: true,
  },
];

const activationChecklist = [
  {
    label: "Os 3 endpoints implementados e no ar",
    ok: true,
    hint: "GET /test, POST /test/candidate e GET /test/result/{resultId}",
  },
  {
    label: "Token Bearer por empresa configurado",
    ok: true,
    hint: "um token por company_id habilitada pela Gupy",
  },
  {
    label: "GET /test devolve simulacoes PUBLICADAS",
    ok: true,
    hint: "rascunhos, expiradas e arquivadas nao aparecem",
  },
  {
    label: "POST /test/candidate cria attempt e devolve test_url + test_result_id",
    ok: true,
    hint: "chamada duplicada e idempotente por company_id, document_id e test_id",
  },
  {
    label: "callback_url tratada",
    ok: true,
    hint: "GET ao finalizar redireciona candidato de volta a Gupy",
  },
  {
    label: "result_webhook_url tratada",
    ok: true,
    hint: "POST assincrono do TestResult com retry, DLQ e reenvio admin",
  },
  {
    label: "TestResult no formato aceito",
    ok: true,
    hint: "score inteiro 0-100, tier major/minor",
  },
  {
    label: "Validado COM o cliente em vaga nao-listada",
    ok: true,
    hint: "nao ha sandbox; validacao ocorre em vaga real nao-listada",
  },
];

const resultFields = [
  {
    key: "results[]",
    value: "nota por competencia (TestResultItem, score 0-100)",
  },
  {
    key: 'tier "major"',
    value: "candidato e empresa veem",
  },
  {
    key: 'tier "minor"',
    value: "so a empresa ve",
  },
  {
    key: "company_result_string",
    value: "Markdown so para empresa, com trilha de pontuacao",
  },
];

const edgeCases = [
  "Duplicidade em /test/candidate devolve o mesmo test_result_id/test_url.",
  'Candidato nao terminou: TestResult fica como "paused" ou "notStarted".',
  "callback falhou: garantir entrega pelo POST no result_webhook_url.",
  "result_webhook_url fora do ar: retry 1s, 4s, 16s, 64s, 256s + DLQ.",
  'previous_result = "fail": recandidatura segue politica da vaga.',
  "candidate_type internal/external pode mudar regra do fluxo.",
];

const attempts = [
  {
    id: "ATT-1832",
    candidate: "Thiago R.",
    status: "Enviado a Gupy via result_webhook_url",
    tone: "ok",
  },
  { id: "ATT-1833", candidate: "Marina F.", status: "Reenviando", tone: "warn" },
  { id: "ATT-1834", candidate: "Joao P.", status: "Falha - na fila DLQ", tone: "danger" },
];

function GupyActivation() {
  const [adminMode, setAdminMode] = useState(true);
  const [failed, setFailed] = useState(false);
  const items = failed
    ? activationChecklist.map((item, index) =>
        index === 1
          ? { ...item, ok: false, hint: "token Bearer da empresa ainda nao foi configurado" }
          : item,
      )
    : activationChecklist;
  const hasFailure = items.some((item) => !item.ok);

  return (
    <AppShell>
      <WizardStepper current="gupy" />
      <ScreenStateStrip blockedReason="checklist de ativacao incompleto bloqueia integracao ativa" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 8</div>
          <h1 className="mt-1 text-3xl font-semibold">Gupy - Ativacao & Conferencia</h1>
          <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
            Expomos 3 endpoints REST. A Gupy nos chama. A vinculacao da simulacao a vaga e feita
            pelo cliente, dentro da Gupy. Nao ha ambiente de sandbox: validacao e feita com o
            cliente em vaga real nao-listada.
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
        <StateBanner tone="danger" title="Ativacao pendente - endpoints ainda nao aprovados">
          Corrija o item em vermelho antes de marcar a integracao como ativa.
        </StateBanner>
      ) : (
        <StateBanner tone="ok" title="Endpoints prontos para consumo pela Gupy">
          A proxima acao apenas registra que a integracao foi conferida e validada com a Gupy.
        </StateBanner>
      )}

      <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
        <main className="space-y-5">
          <section className="rounded-md border border-border bg-card p-5">
            <div className="mb-4 flex items-center gap-2 text-sm font-semibold">
              <Server className="h-4 w-4" />
              Endpoints que expomos (a Gupy consome)
            </div>
            <div className="grid gap-3">
              {endpoints.map((endpoint) => (
                <div
                  key={`${endpoint.method}-${endpoint.path}`}
                  className="grid gap-3 rounded-md border border-border bg-background p-3 md:grid-cols-[72px_220px_1fr_74px]"
                >
                  <span className="rounded-md border border-border bg-card px-2 py-1 text-xs font-semibold">
                    {endpoint.method}
                  </span>
                  <code className="text-sm text-foreground">{endpoint.path}</code>
                  <span className="text-sm text-muted-foreground">{endpoint.description}</span>
                  <span
                    className={
                      endpoint.ok
                        ? "rounded-md border border-success/20 bg-success/10 px-2 py-1 text-center text-[11px] font-medium text-success"
                        : "rounded-md border border-border bg-muted px-2 py-1 text-center text-[11px] font-medium text-muted-foreground"
                    }
                  >
                    {endpoint.status}
                  </span>
                </div>
              ))}
            </div>
          </section>

          <section className="rounded-md border border-border bg-card p-5">
            <h2 className="text-sm font-semibold">Checklist de ativacao</h2>
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
                Marcar integracao como ativa
              </button>
            </div>
          </section>

          <StateBanner tone="info" title="Ativacao nao e self-service">
            Para ativar, enviar a Gupy a URL da nossa API e um token por empresa. A Gupy habilita o
            parceiro. Nao ha sandbox: a primeira validacao e feita junto ao cliente, numa vaga
            nao-listada real.
          </StateBanner>

          <section className="rounded-md border border-border bg-card p-5">
            <div className="mb-4 flex items-center gap-2 text-sm font-semibold">
              <Webhook className="h-4 w-4" />
              Como a nota volta
            </div>
            <div className="divide-y divide-border rounded-md border border-border">
              {resultFields.map((field) => (
                <div key={field.key} className="grid gap-2 p-3 text-sm md:grid-cols-[180px_1fr]">
                  <code className="text-foreground">{field.key}</code>
                  <span className="text-muted-foreground">{field.value}</span>
                </div>
              ))}
            </div>
            <p className="mt-3 text-xs text-muted-foreground">
              Tudo aparece dentro da Gupy. O gestor nao usa tela externa.
            </p>
          </section>

          <section className="rounded-md border border-border bg-card p-5">
            <h2 className="text-sm font-semibold">Casos de borda cobertos</h2>
            <ul className="mt-3 grid gap-2 text-sm text-muted-foreground md:grid-cols-2">
              {edgeCases.map((edgeCase) => (
                <li key={edgeCase} className="rounded-md border border-border bg-background p-3">
                  {edgeCase}
                </li>
              ))}
            </ul>
          </section>
        </main>

        <aside className="rounded-md border border-border bg-card p-5">
          <h2 className="text-sm font-semibold">Status de envio do resultado</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            Reflete o POST assincrono no result_webhook_url, com retry, DLQ e reenvio manual.
          </p>
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
