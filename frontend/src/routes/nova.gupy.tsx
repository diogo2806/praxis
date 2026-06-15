import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import {
  CheckCircle2,
  ExternalLink,
  RefreshCw,
  Server,
  ShieldAlert,
  UserCheck,
  Webhook,
  XCircle,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { NextStepContract, ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import { gupyConnectionLabels, useGupyConnectionState, useViewMode } from "@/lib/view-mode";
import { WizardStepper } from "@/components/wizard-stepper";

export const Route = createFileRoute("/nova/gupy")({
  head: () => ({
    meta: [
      { title: "Gupy — Ativação & Conferência — Práxis" },
      {
        name: "description",
        content:
          "Resumo comercial e diagnóstico técnico da integração do Práxis com testes externos da Gupy.",
      },
    ],
  }),
  component: GupyActivation,
});

const endpoints = [
  {
    method: "GET",
    path: "/test",
    description: "lista nossas simulações PUBLICADAS como Test[]",
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
    description: "devolve TestResult com score e competências",
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
    label: "GET /test devolve simulações PUBLICADAS",
    ok: true,
    hint: "rascunhos, expiradas e arquivadas não aparecem",
  },
  {
    label: "POST /test/candidate cria tentativa e devolve test_url + test_result_id",
    ok: true,
    hint: "chamada duplicada é idempotente por company_id, document_id e test_id",
  },
  {
    label: "callback_url tratada",
    ok: true,
    hint: "GET ao finalizar redireciona candidato de volta à Gupy",
  },
  {
    label: "result_webhook_url tratada",
    ok: true,
    hint: "POST assíncrono do TestResult com retry, DLQ e reenvio admin",
  },
  {
    label: "TestResult no formato aceito",
    ok: true,
    hint: "score inteiro 0-100, tier major/minor",
  },
  {
    label: "Validado com o cliente em vaga não-listada",
    ok: true,
    hint: "não há sandbox; validação ocorre em vaga real não-listada",
  },
];

const resultFields = [
  { key: "results[]", value: "nota por competência (TestResultItem, score 0-100)" },
  { key: 'tier "major"', value: "candidato e empresa veem" },
  { key: 'tier "minor"', value: "só a empresa vê" },
  { key: "company_result_string", value: "Markdown só para empresa, com trilha de pontuação" },
];

const edgeCases = [
  "Duplicidade em /test/candidate devolve o mesmo test_result_id/test_url.",
  'Candidato não terminou: TestResult fica como "paused" ou "notStarted".',
  "callback falhou: garantir entrega pelo POST no result_webhook_url.",
  "result_webhook_url fora do ar: retry 1s, 4s, 16s, 64s, 256s + DLQ.",
  'previous_result = "fail": recandidatura segue política da vaga.',
  "candidate_type internal/external pode mudar regra do fluxo.",
];

const attempts = [
  { id: "ATT-1832", candidate: "Thiago R.", status: "Enviado à Gupy", tone: "ok" },
  { id: "ATT-1833", candidate: "Marina F.", status: "Reenviando", tone: "warn" },
  { id: "ATT-1834", candidate: "João P.", status: "Falha - na fila DLQ", tone: "danger" },
];

const businessSteps = [
  "A empresa seleciona o teste Práxis dentro da Gupy",
  "O candidato é redirecionado para a simulação",
  "Ao finalizar, o resultado retorna para a candidatura na Gupy",
  "RH e gestor continuam decidindo dentro da Gupy",
];

const businessCards = [
  { title: "Teste disponível na Gupy", Icon: CheckCircle2 },
  { title: "Candidato redirecionado", Icon: ExternalLink },
  { title: "Resultado enviado", Icon: Webhook },
  { title: "Gestor permanece na Gupy", Icon: UserCheck },
];

function GupyActivation() {
  const technical = useViewMode() === "technical";
  const connectionState = useGupyConnectionState("/nova/gupy");
  const [tab, setTab] = useState<"summary" | "diagnostic">("summary");
  const [adminMode, setAdminMode] = useState(technical);
  const failed = connectionState === "error";
  const items = failed
    ? activationChecklist.map((item, index) =>
        index === 1
          ? { ...item, ok: false, hint: "token Bearer da empresa ainda não foi configurado" }
          : item,
      )
    : activationChecklist;
  const hasFailure = items.some((item) => !item.ok);

  return (
    <AppShell>
      <WizardStepper current="gupy" />
      <ScreenStateStrip blockedReason="checklist de ativação incompleto bloqueia integração ativa" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 8</div>
          <h1 className="mt-1 text-3xl font-semibold">Gupy — Ativação & Conferência</h1>
          <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
            O Práxis fica disponível como teste externo. A Gupy chama nossos endpoints, o candidato
            conclui a simulação e o resultado volta para a candidatura na Gupy.
          </p>
        </div>
        {technical && (
          <div className="flex gap-2">
            <a
              href={failed ? "/nova/gupy?mode=technical" : "/nova/gupy?mode=technical&gupy=error"}
              className="rounded-md border border-border bg-card px-3 py-2 text-xs hover:bg-accent"
            >
              {failed ? "Limpar erro" : "Simular erro Gupy"}
            </a>
            <label className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-xs">
              <input
                type="checkbox"
                checked={adminMode}
                onChange={(event) => setAdminMode(event.target.checked)}
              />
              Admin integração
            </label>
          </div>
        )}
      </div>

      <div className="mb-5 inline-flex rounded-md border border-border bg-card p-1 text-sm">
        <button
          type="button"
          onClick={() => setTab("summary")}
          className={`rounded px-3 py-1.5 ${tab === "summary" ? "bg-primary text-primary-foreground" : "hover:bg-accent"}`}
        >
          Resumo da integração
        </button>
        <button
          type="button"
          onClick={() => setTab("diagnostic")}
          className={`rounded px-3 py-1.5 ${tab === "diagnostic" ? "bg-primary text-primary-foreground" : "hover:bg-accent"}`}
        >
          Diagnóstico técnico
        </button>
      </div>

      {hasFailure ? (
        <StateBanner tone="danger" title={gupyConnectionLabels.error}>
          Não foi possível confirmar a ativação. Abra o diagnóstico técnico e corrija o item em
          vermelho.
        </StateBanner>
      ) : (
        <StateBanner tone="ok" title={gupyConnectionLabels[connectionState]}>
          A próxima ação registra que a integração foi conferida e validada com a Gupy.
        </StateBanner>
      )}

      <div className="mt-5">
        <NextStepContract
          primary={
            hasFailure
              ? "Corrigir checklist de ativação antes de marcar integração ativa."
              : "Registrar conferência e aguardar ativação/vínculo dentro da Gupy."
          }
          secondary="Cliente vincula a simulação na vaga dentro da Gupy; gestor não usa tela externa."
          versionRule="A Gupy lista apenas testes publicados e versões imutáveis."
          lockedAfter="Integração ativa não publica rascunho nem altera tentativa já iniciada."
        />
      </div>

      {tab === "summary" ? (
        <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_340px]">
          <section className="rounded-md border border-border bg-card p-5">
            <h2 className="text-sm font-semibold">Como funciona</h2>
            <ol className="mt-4 grid gap-3">
              {businessSteps.map((step, index) => (
                <li
                  key={step}
                  className="flex gap-3 rounded-md border border-border bg-background p-3"
                >
                  <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-primary text-xs font-semibold text-primary-foreground">
                    {index + 1}
                  </span>
                  <span className="text-sm">{step}</span>
                </li>
              ))}
            </ol>
          </section>
          <aside className="grid gap-3">
            {businessCards.map(({ title, Icon }) => (
              <div key={title} className="rounded-md border border-border bg-card p-4">
                <div className="flex items-center gap-2 text-sm font-semibold">
                  <Icon className="h-4 w-4 text-primary" />
                  {title}
                </div>
              </div>
            ))}
            <button
              disabled={hasFailure}
              className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Marcar integração como ativa
            </button>
          </aside>
        </div>
      ) : (
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
              <h2 className="text-sm font-semibold">Checklist de ativação</h2>
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
                  Marcar integração como ativa
                </button>
              </div>
            </section>

            <StateBanner tone="info" title="Ativação não é self-service">
              Para ativar, enviar à Gupy a URL da nossa API e um token por empresa. A Gupy habilita
              o parceiro. Não há sandbox: a primeira validação é feita junto ao cliente, numa vaga
              não-listada real.
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
                Tudo aparece dentro da Gupy. O gestor não usa tela externa.
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
              Reflete o POST assíncrono no result_webhook_url, com retry, DLQ e reenvio manual.
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
                RH comum não vê fila DLQ nem reprocessamento manual.
              </div>
            )}
          </aside>
        </div>
      )}
    </AppShell>
  );
}
