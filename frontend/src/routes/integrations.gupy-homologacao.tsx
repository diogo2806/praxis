import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  AlertTriangle,
  CheckCircle2,
  CircleDashed,
  ClipboardCopy,
  RefreshCw,
  Save,
  ShieldCheck,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import {
  getGupyHomologationStatus,
  updateGupyHomologationEvidence,
  type GupyHomologationCheckStatus,
  type GupyHomologationEvidenceRequest,
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
              Acompanhe o que o Práxis já consegue comprovar e registre somente as evidências
              externas confirmadas pela Gupy e pelo cliente.
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
  const queryClient = useQueryClient();
  const [evidence, setEvidence] = useState<GupyHomologationEvidenceRequest>(() =>
    evidenceRequestFrom(data),
  );

  useEffect(() => {
    setEvidence(evidenceRequestFrom(data));
  }, [data]);

  const evidenceMutation = useMutation({
    mutationFn: updateGupyHomologationEvidence,
    onSuccess: (response) => {
      queryClient.setQueryData(["integrations", "gupy", "homologation"], response);
    },
  });

  function toggleEvidence(field: keyof Omit<GupyHomologationEvidenceRequest, "notes">) {
    setEvidence((current) => ({ ...current, [field]: !current[field] }));
  }

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

      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Metric label="Avaliações publicadas" value={data.metrics.publishedTests} />
        <Metric label="Tentativas originadas pela Gupy" value={data.metrics.gupyAttempts} />
        <Metric label="Tentativas concluídas" value={data.metrics.completedGupyAttempts} />
        <Metric label="Tentativas com webhook" value={data.metrics.attemptsWithResultWebhook} />
        <Metric label="Webhooks entregues" value={data.metrics.sentResultWebhooks} />
        <Metric label="Consultas de resultado" value={data.metrics.resultEndpointQueries} />
        <Metric label="Resultados percentuais válidos" value={data.metrics.validPercentageResults} />
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

      <section className="rounded-xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold">Registro das evidências externas</h2>
        <p className="mt-1 max-w-3xl text-sm leading-6 text-muted-foreground">
          Marque somente itens confirmados em uma vaga real ou não listada. Cada alteração fica
          vinculada ao usuário autenticado e registrada na auditoria da integração.
        </p>
        <div className="mt-5 grid gap-3 md:grid-cols-2">
          <EvidenceCheckbox
            checked={evidence.callbackConfirmed}
            label="A Gupy confirmou o GET no callback_url"
            onChange={() => toggleEvidence("callbackConfirmed")}
          />
          <EvidenceCheckbox
            checked={evidence.resultPagesConfirmed}
            label="Empresa e candidato visualizaram páginas separadas"
            onChange={() => toggleEvidence("resultPagesConfirmed")}
          />
          <EvidenceCheckbox
            checked={evidence.gupyApproved}
            label="Aprovação formal da Gupy recebida"
            onChange={() => toggleEvidence("gupyApproved")}
          />
          <EvidenceCheckbox
            checked={evidence.clientApproved}
            label="Aprovação formal do cliente recebida"
            onChange={() => toggleEvidence("clientApproved")}
          />
        </div>
        <label className="mt-4 block text-sm font-medium">
          Evidências, protocolo ou observações
          <textarea
            value={evidence.notes}
            maxLength={2000}
            rows={4}
            onChange={(event) => setEvidence((current) => ({ ...current, notes: event.target.value }))}
            className="mt-2 w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
            placeholder="Ex.: vaga utilizada, data, responsável da Gupy, protocolo e links dos comprovantes."
          />
        </label>
        <div className="mt-4 flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={() => evidenceMutation.mutate(evidence)}
            disabled={evidenceMutation.isPending}
            className="inline-flex min-h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
          >
            <Save className="h-4 w-4" />
            {evidenceMutation.isPending ? "Salvando..." : "Salvar evidências"}
          </button>
          {evidenceMutation.isSuccess && (
            <span className="text-sm text-success">Evidências registradas.</span>
          )}
          {evidenceMutation.isError && (
            <span role="alert" className="text-sm text-danger">
              {evidenceMutation.error instanceof Error
                ? evidenceMutation.error.message
                : "Não foi possível salvar as evidências."}
            </span>
          )}
        </div>
      </section>

      <section className="rounded-xl border border-border bg-card">
        <div className="border-b border-border p-6">
          <h2 className="text-lg font-semibold">Checklist de homologação</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Itens externos só são concluídos quando houver evidência gerada ou registrada pelo fluxo real.
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

      {data.status === "HOMOLOGATED" ? (
        <StateBanner tone="success" title="Homologação formal registrada">
          As evidências técnicas estão completas e as aprovações da Gupy e do cliente foram
          registradas. A documentação comercial pode ser atualizada para refletir esse estado.
        </StateBanner>
      ) : (
        <StateBanner tone="info" title="Aprovação externa obrigatória">
          O estado “evidências prontas” não significa homologação aprovada. A conclusão formal
          depende do registro das aprovações reais da Gupy e do cliente.
        </StateBanner>
      )}

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

function EvidenceCheckbox({
  checked,
  label,
  onChange,
}: {
  checked: boolean;
  label: string;
  onChange: () => void;
}) {
  return (
    <label className="flex cursor-pointer items-start gap-3 rounded-md border border-border bg-background p-4 text-sm">
      <input
        type="checkbox"
        checked={checked}
        onChange={onChange}
        className="mt-0.5 h-4 w-4"
      />
      <span>{label}</span>
    </label>
  );
}

function evidenceRequestFrom(data: GupyHomologationResponse): GupyHomologationEvidenceRequest {
  return {
    callbackConfirmed: data.externalEvidence.callbackConfirmed,
    resultPagesConfirmed: data.externalEvidence.resultPagesConfirmed,
    gupyApproved: data.externalEvidence.gupyApproved,
    clientApproved: data.externalEvidence.clientApproved,
    notes: data.externalEvidence.notes ?? "",
  };
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
  if (status === "HOMOLOGATED" || status === "EVIDENCE_READY") {
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
    BLOCKED: "Existem bloqueios técnicos",
    READY_FOR_EXTERNAL_VALIDATION: "Pronto para validação em vaga real",
    EVIDENCE_READY: "Evidências técnicas prontas para aprovação",
    HOMOLOGATED: "Homologação formal concluída",
  }[status];
}

function overallStatusDescription(status: GupyHomologationResponse["status"]) {
  return {
    BLOCKED: "Corrija os requisitos ou falhas operacionais antes de avançar com a homologação.",
    READY_FOR_EXTERNAL_VALIDATION:
      "A configuração interna está pronta. O próximo passo é executar e registrar o fluxo com token e vaga reais.",
    EVIDENCE_READY:
      "O fluxo técnico e as validações externas produziram evidências. Faltam as aprovações formais.",
    HOMOLOGATED:
      "A Gupy e o cliente aprovaram formalmente o fluxo após a validação ponta a ponta.",
  }[status];
}
