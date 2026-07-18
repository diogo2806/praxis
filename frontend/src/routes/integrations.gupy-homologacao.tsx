import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  AlertTriangle,
  CheckCircle2,
  CircleDashed,
  ClipboardCopy,
  RefreshCw,
  ShieldCheck,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import {
  getGupyHomologationStatus,
  type GupyHomologationCheckStatus,
  type GupyHomologationResponse,
} from "@/lib/api/gupy-homologation";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/integrations/gupy-homologacao")({
  head: () => ({
    meta: [
      { title: "Homologação Gupy - Práxis" },
      {
        name: "description",
        content: "Prontidão técnica e evidências do fluxo de provedor externo da Gupy.",
      },
    ],
  }),
  component: GupyHomologationPage,
});

function GupyHomologationPage() {
  const [copied, setCopied] = useState(false);
  const homologationQuery = useQuery({
    queryKey: ["integrations", "gupy", "homologation"],
    queryFn: getGupyHomologationStatus,
    retry: false,
  });

  async function copyEvidence(data: GupyHomologationResponse) {
    await navigator.clipboard.writeText(JSON.stringify(data, null, 2));
    setCopied(true);
    window.setTimeout(() => setCopied(false), 2000);
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-6xl space-y-6">
        <header className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Integração Gupy
            </div>
            <h1 className="mt-1 text-3xl font-semibold">Centro de homologação técnica</h1>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
              Acompanhe o que o Práxis já consegue comprovar e o que ainda depende de token, vaga,
              callback, webhook e aprovação no ambiente real da Gupy.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void homologationQuery.refetch()}
              disabled={homologationQuery.isFetching}
              className="inline-flex min-h-10 items-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-accent disabled:opacity-60"
            >
              <RefreshCw
                className={cn("h-4 w-4", homologationQuery.isFetching && "animate-spin")}
              />
              Atualizar
            </button>
            {homologationQuery.data && (
              <button
                type="button"
                onClick={() => void copyEvidence(homologationQuery.data!)}
                className="inline-flex min-h-10 items-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-accent"
              >
                <ClipboardCopy className="h-4 w-4" />
                {copied ? "Evidências copiadas" : "Copiar evidências"}
              </button>
            )}
          </div>
        </header>

        {homologationQuery.isLoading && (
          <section className="rounded-md border border-border bg-card p-6 text-sm text-muted-foreground">
            Calculando a prontidão da integração...
          </section>
        )}

        {homologationQuery.isError && (
          <StateBanner
            tone="danger"
            title="Não foi possível carregar a homologação"
            action={
              <button
                type="button"
                onClick={() => void homologationQuery.refetch()}
                className="rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium"
              >
                Tentar novamente
              </button>
            }
          >
            {homologationQuery.error instanceof Error
              ? homologationQuery.error.message
              : "Tente novamente."}
          </StateBanner>
        )}

        {homologationQuery.data && <HomologationContent data={homologationQuery.data} />}
      </main>
    </AppShell>
  );
}

function HomologationContent({ data }: { data: GupyHomologationResponse }) {
  return (
    <>
      <section className="rounded-xl border border-border bg-card p-6">
        <div className="flex flex-wrap items-start justify-between gap-5">
          <div className="flex items-start gap-3">
            <OverallIcon status={data.status} />
            <div>
              <div className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Estado atual
              </div>
              <h2 className="mt-1 text-xl font-semibold">{overallStatusLabel(data.status)}</h2>
              <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
                {overallStatusDescription(data.status)}
              </p>
            </div>
          </div>
          <div className="text-right">
            <div className="text-3xl font-semibold tabular-nums text-primary">
              {data.readinessPercent}%
            </div>
            <div className="text-xs text-muted-foreground">evidências técnicas concluídas</div>
          </div>
        </div>
        <div
          className="mt-5 h-2 overflow-hidden rounded-full bg-muted"
          role="progressbar"
          aria-label="Prontidão da homologação Gupy"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={data.readinessPercent}
        >
          <div
            className="h-full rounded-full bg-primary"
            style={{ width: `${data.readinessPercent}%` }}
          />
        </div>
      </section>

      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <Metric label="Avaliações publicadas" value={data.metrics.publishedTests} />
        <Metric label="Tentativas originadas pela Gupy" value={data.metrics.gupyAttempts} />
        <Metric label="Tentativas concluídas" value={data.metrics.completedGupyAttempts} />
        <Metric label="Tentativas com webhook" value={data.metrics.attemptsWithResultWebhook} />
        <Metric label="Webhooks entregues" value={data.metrics.sentResultWebhooks} />
        <Metric
          label="Entregas em DLQ"
          value={data.metrics.resultWebhooksInDlq}
          warning={data.metrics.resultWebhooksInDlq > 0}
        />
      </section>

      <section className="rounded-xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold">Endpoints informados à Gupy</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Todos exigem o token gerado na Central de Integrações no cabeçalho Authorization.
        </p>
        <div className="mt-4 grid gap-3">
          {data.endpoints.map((endpoint) => (
            <div
              key={`${endpoint.method}-${endpoint.url}`}
              className="grid gap-2 rounded-md border border-border bg-background p-4 md:grid-cols-[70px_1fr_240px] md:items-center"
            >
              <span className="w-fit rounded-md bg-muted px-2 py-1 font-mono text-xs font-semibold">
                {endpoint.method}
              </span>
              <code className="break-all text-xs text-foreground">{endpoint.url}</code>
              <span className="text-xs text-muted-foreground">{endpoint.purpose}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="rounded-xl border border-border bg-card">
        <div className="border-b border-border p-6">
          <h2 className="text-lg font-semibold">Checklist de homologação</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Itens externos só são concluídos quando houver evidência gerada pelo fluxo real.
          </p>
        </div>
        <div className="divide-y divide-border">
          {data.checks.map((check) => (
            <article key={check.code} className="flex gap-3 p-5">
              <CheckIcon status={check.status} />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="font-medium">{check.title}</h3>
                  {check.external && (
                    <span className="rounded-md border border-border bg-muted/40 px-2 py-0.5 text-[10px] uppercase tracking-wide text-muted-foreground">
                      Ambiente real
                    </span>
                  )}
                  <span className={checkStatusClass(check.status)}>
                    {checkStatusLabel(check.status)}
                  </span>
                </div>
                <p className="mt-1 text-sm leading-6 text-muted-foreground">{check.detail}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <StateBanner tone="info" title="Aprovação externa obrigatória">
        O estado “evidências prontas” não significa homologação aprovada. A conclusão formal depende
        da Gupy e do cliente após o teste em vaga real.
      </StateBanner>

      <div className="flex flex-wrap gap-3">
        <Link
          to="/integrations"
          className="rounded-md border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-accent"
        >
          Voltar para integrações
        </Link>
        <Link
          to="/monitoramento"
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          Abrir entregas e DLQ
        </Link>
      </div>
    </>
  );
}

function Metric({ label, value, warning = false }: { label: string; value: number; warning?: boolean }) {
  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <div className="text-xs uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className={cn("mt-2 text-3xl font-semibold tabular-nums", warning && "text-danger")}>
        {value.toLocaleString("pt-BR")}
      </div>
    </div>
  );
}

function OverallIcon({ status }: { status: GupyHomologationResponse["status"] }) {
  if (status === "EVIDENCE_READY") {
    return <ShieldCheck className="mt-0.5 h-7 w-7 shrink-0 text-success" />;
  }
  if (status === "BLOCKED") {
    return <AlertTriangle className="mt-0.5 h-7 w-7 shrink-0 text-danger" />;
  }
  return <CircleDashed className="mt-0.5 h-7 w-7 shrink-0 text-primary" />;
}

function CheckIcon({ status }: { status: GupyHomologationCheckStatus }) {
  if (status === "OK") return <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-success" />;
  if (status === "BLOCKER") return <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-danger" />;
  return <CircleDashed className="mt-0.5 h-5 w-5 shrink-0 text-muted-foreground" />;
}

function checkStatusClass(status: GupyHomologationCheckStatus) {
  return cn(
    "rounded-md border px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide",
    status === "OK" && "border-success/30 bg-success/10 text-success",
    status === "PENDING" && "border-border bg-muted/40 text-muted-foreground",
    status === "BLOCKER" && "border-danger/30 bg-danger/10 text-danger",
  );
}

function checkStatusLabel(status: GupyHomologationCheckStatus) {
  return { OK: "Concluído", PENDING: "Pendente", BLOCKER: "Bloqueio" }[status];
}

function overallStatusLabel(status: GupyHomologationResponse["status"]) {
  return {
    BLOCKED: "Existem bloqueios internos",
    READY_FOR_EXTERNAL_VALIDATION: "Pronto para validação em vaga real",
    EVIDENCE_READY: "Evidências técnicas prontas para aprovação",
  }[status];
}

function overallStatusDescription(status: GupyHomologationResponse["status"]) {
  return {
    BLOCKED: "Corrija os requisitos internos antes de envolver a Gupy ou um cliente piloto.",
    READY_FOR_EXTERNAL_VALIDATION:
      "A configuração interna está pronta. O próximo passo é executar o fluxo com token e vaga reais.",
    EVIDENCE_READY:
      "O fluxo técnico gerou evidências de tentativa, conclusão e webhook. Falta a confirmação formal da Gupy.",
  }[status];
}
