import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  Eye,
  History,
  Loader2,
  ShieldCheck,
} from "lucide-react";
import { useState } from "react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  decideIntegrityReview,
  getIntegrityReview,
  listIntegrityReviews,
  type IntegrityReviewDecision,
  type IntegrityReviewDetail,
} from "@/lib/api/integrity-reviews";
import { cn } from "@/lib/utils";

const decisionOptions: Array<{ value: IntegrityReviewDecision; label: string }> = [
  { value: "NO_IMPACT", label: "Sem impacto" },
  { value: "TECHNICAL_ISSUE_CONFIRMED", label: "Problema técnico confirmado" },
  { value: "REAPPLICATION_RECOMMENDED", label: "Reaplicação recomendada" },
  { value: "PRIVACY_COMPLIANCE_REVIEW", label: "Análise de privacidade/compliance" },
];

export function IntegrityReviewQueuePanel() {
  const queryClient = useQueryClient();
  const [selectedAttemptId, setSelectedAttemptId] = useState<string | null>(null);
  const [decision, setDecision] = useState<IntegrityReviewDecision | "">("");
  const [justification, setJustification] = useState("");
  const [shareWithCompany, setShareWithCompany] = useState(false);

  const queueQuery = useQuery({
    queryKey: ["integrity-reviews", 0],
    queryFn: () => listIntegrityReviews(0, 25),
    retry: false,
    refetchInterval: 60_000,
  });
  const detailQuery = useQuery({
    queryKey: ["integrity-review", selectedAttemptId],
    queryFn: () => getIntegrityReview(selectedAttemptId as string),
    enabled: selectedAttemptId !== null,
    retry: false,
    staleTime: Number.POSITIVE_INFINITY,
    refetchOnWindowFocus: false,
  });
  const decisionMutation = useMutation({
    mutationFn: () =>
      decideIntegrityReview(selectedAttemptId as string, {
        decision: decision as IntegrityReviewDecision,
        justification: justification.trim(),
        shareWithCompany,
      }),
    onSuccess: async (detail) => {
      queryClient.setQueryData(["integrity-review", selectedAttemptId], detail);
      await queryClient.invalidateQueries({ queryKey: ["integrity-reviews"] });
      setDecision("");
      setJustification("");
    },
  });

  const items = queueQuery.data?.items ?? [];
  const detail = detailQuery.data;

  function openEvidence(attemptId: string) {
    setSelectedAttemptId(attemptId);
    setDecision("");
    setJustification("");
    setShareWithCompany(false);
  }

  return (
    <section className="mx-auto mt-8 max-w-7xl space-y-5" aria-labelledby="integrity-review-title">
      <header className="rounded-xl border border-border bg-card p-5">
        <div className="flex items-start gap-3">
          <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
          <div>
            <h2 id="integrity-review-title" className="text-xl font-semibold">
              Revisão técnica de integridade
            </h2>
            <p className="mt-2 max-w-4xl text-sm leading-6 text-muted-foreground">
              Esta fila reúne somente ocorrências técnicas explicáveis. Os alertas não classificam
              intenção, não aplicam penalidade e não alteram pontuação, competências, resultado da
              integração ou decisão de contratação.
            </p>
          </div>
        </div>
      </header>

      {queueQuery.isLoading ? (
        <div className="rounded-xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">
          Carregando revisões técnicas...
        </div>
      ) : queueQuery.isError ? (
        <StateBanner tone="danger" title="Não foi possível carregar a fila técnica">
          {queueQuery.error instanceof Error ? queueQuery.error.message : "Tente novamente."}
        </StateBanner>
      ) : items.length === 0 ? (
        <StateBanner tone="ok" title="Nenhuma revisão técnica pendente">
          Nenhuma tentativa atingiu as regras determinísticas de encaminhamento.
        </StateBanner>
      ) : (
        <div className="grid gap-5 xl:grid-cols-[22rem_minmax(0,1fr)]">
          <div className="space-y-3" aria-label="Fila de revisões técnicas">
            {items.map((item) => (
              <article
                key={item.attemptId}
                className={cn(
                  "rounded-xl border bg-card p-4",
                  selectedAttemptId === item.attemptId
                    ? "border-primary ring-2 ring-primary/15"
                    : "border-border",
                )}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <h3 className="truncate font-semibold">{item.candidateName}</h3>
                    <p className="mt-1 truncate text-xs text-muted-foreground">
                      {item.candidateEmail}
                    </p>
                  </div>
                  <span
                    className={cn(
                      "rounded-full border px-2 py-1 text-[10px] font-semibold uppercase",
                      item.reviewStatus === "DECIDED"
                        ? "border-success/30 bg-success/10 text-success"
                        : "border-warning/40 bg-warning/10 text-warning-foreground",
                    )}
                  >
                    {item.reviewStatus === "DECIDED" ? "Decidida" : "Pendente"}
                  </span>
                </div>
                <div className="mt-3 flex items-center gap-2 text-xs text-muted-foreground">
                  <AlertTriangle className="h-3.5 w-3.5" />
                  {item.alertCount} alerta{item.alertCount === 1 ? "" : "s"} explicável
                </div>
                {item.evidenceDiscardedAt && (
                  <p className="mt-2 text-xs text-muted-foreground">
                    Evidências descartadas em {formatDate(item.evidenceDiscardedAt)}.
                  </p>
                )}
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="mt-4 w-full gap-2 bg-background"
                  onClick={() => openEvidence(item.attemptId)}
                >
                  <Eye className="h-4 w-4" />
                  Abrir evidências
                </Button>
              </article>
            ))}
          </div>

          <div>
            {selectedAttemptId === null ? (
              <div className="rounded-xl border border-dashed border-border bg-card p-10 text-center text-sm text-muted-foreground">
                Selecione uma revisão para abrir as evidências. Cada acesso fica registrado.
              </div>
            ) : detailQuery.isLoading ? (
              <div className="rounded-xl border border-border bg-card p-10 text-center text-sm text-muted-foreground">
                <Loader2 className="mx-auto mb-3 h-5 w-5 animate-spin" />
                Registrando acesso e carregando evidências...
              </div>
            ) : detailQuery.isError ? (
              <StateBanner tone="danger" title="Não foi possível abrir as evidências">
                {detailQuery.error instanceof Error ? detailQuery.error.message : "Tente novamente."}
              </StateBanner>
            ) : detail ? (
              <IntegrityReviewDetailPanel
                detail={detail}
                decision={decision}
                justification={justification}
                shareWithCompany={shareWithCompany}
                saving={decisionMutation.isPending}
                error={decisionMutation.error}
                onDecisionChange={setDecision}
                onJustificationChange={setJustification}
                onShareChange={setShareWithCompany}
                onSave={() => decisionMutation.mutate()}
              />
            ) : null}
          </div>
        </div>
      )}
    </section>
  );
}

function IntegrityReviewDetailPanel({
  detail,
  decision,
  justification,
  shareWithCompany,
  saving,
  error,
  onDecisionChange,
  onJustificationChange,
  onShareChange,
  onSave,
}: {
  detail: IntegrityReviewDetail;
  decision: IntegrityReviewDecision | "";
  justification: string;
  shareWithCompany: boolean;
  saving: boolean;
  error: unknown;
  onDecisionChange: (decision: IntegrityReviewDecision) => void;
  onJustificationChange: (value: string) => void;
  onShareChange: (value: boolean) => void;
  onSave: () => void;
}) {
  return (
    <div className="space-y-5">
      <section className="rounded-xl border border-border bg-card p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div className="text-xs font-semibold uppercase tracking-wide text-primary">
              Evidência técnica restrita
            </div>
            <h3 className="mt-1 text-xl font-semibold">{detail.candidateName}</h3>
            <p className="mt-1 text-sm text-muted-foreground">{detail.candidateEmail}</p>
          </div>
          <a
            href={`/results/${encodeURIComponent(detail.attemptId)}`}
            className="text-sm font-medium text-primary hover:underline"
          >
            Abrir resultado separado
          </a>
        </div>
        <div className="mt-4 grid gap-3 md:grid-cols-2">
          {detail.alerts.map((alert) => (
            <article key={alert.code} className="rounded-lg border border-warning/35 bg-warning/5 p-4">
              <div className="flex items-start gap-2">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-warning" />
                <div>
                  <h4 className="text-sm font-semibold">{alert.title}</h4>
                  <p className="mt-1 text-xs leading-5 text-muted-foreground">
                    {alert.explanation}
                  </p>
                  <div className="mt-2 text-xs font-medium">Ocorrências: {alert.occurrences}</div>
                </div>
              </div>
            </article>
          ))}
        </div>
        {detail.evidenceDiscardedAt && (
          <StateBanner tone="info" title="Evidências descartadas pela política de retenção">
            O parecer e a trilha de auditoria foram preservados. Sessões e eventos técnicos não estão
            mais disponíveis.
          </StateBanner>
        )}
      </section>

      <section className="rounded-xl border border-border bg-card p-5">
        <div className="flex items-center gap-2">
          <Clock3 className="h-4 w-4 text-primary" />
          <h3 className="text-lg font-semibold">Sessões e linha do tempo</h3>
        </div>
        <div className="mt-4 space-y-3">
          {detail.sessions.length === 0 ? (
            <p className="text-sm text-muted-foreground">Nenhuma sessão técnica disponível.</p>
          ) : (
            detail.sessions.map((session) => (
              <article key={session.id} className="rounded-lg border border-border bg-background p-3 text-xs">
                <div className="flex flex-wrap justify-between gap-2 font-medium">
                  <span>{session.status}</span>
                  <span>{session.userAgentCategory}</span>
                </div>
                <div className="mt-2 text-muted-foreground">
                  Início: {formatDate(session.startedAt)} · Último heartbeat: {formatDate(session.lastHeartbeatAt)}
                </div>
              </article>
            ))
          )}
        </div>
        <div className="mt-4 max-h-80 overflow-auto rounded-lg border border-border">
          {detail.events.length === 0 ? (
            <div className="p-4 text-sm text-muted-foreground">Nenhum evento técnico disponível.</div>
          ) : (
            <ol className="divide-y divide-border">
              {detail.events.map((event) => (
                <li key={event.id} className="p-3 text-xs">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="font-semibold">{eventLabel(event.eventType)}</span>
                    <span className="text-muted-foreground">{formatDate(event.occurredAt)}</span>
                  </div>
                  {(event.visibilityState || event.inputMode || event.detail) && (
                    <p className="mt-1 text-muted-foreground">
                      {[event.visibilityState, event.inputMode, event.detail].filter(Boolean).join(" · ")}
                    </p>
                  )}
                </li>
              ))}
            </ol>
          )}
        </div>
      </section>

      <section className="rounded-xl border border-border bg-card p-5">
        <h3 className="text-lg font-semibold">Parecer humano</h3>
        <p className="mt-1 text-sm text-muted-foreground">
          Escolha um status neutro e descreva os fatos verificados. O parecer não altera o resultado
          da avaliação.
        </p>
        {detail.reviewStatus === "DECIDED" && detail.decision && (
          <div className="mt-4 rounded-lg border border-success/30 bg-success/10 p-4 text-sm">
            <div className="flex items-center gap-2 font-semibold">
              <CheckCircle2 className="h-4 w-4 text-success" />
              {decisionLabel(detail.decision)}
            </div>
            <p className="mt-2 text-muted-foreground">{detail.justification}</p>
            <div className="mt-2 text-xs text-muted-foreground">
              {detail.reviewedBy} · {formatDate(detail.decidedAt)}
            </div>
          </div>
        )}
        <div className="mt-4 grid gap-2 md:grid-cols-2">
          {decisionOptions.map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => onDecisionChange(option.value)}
              className={cn(
                "rounded-md border px-3 py-3 text-left text-sm hover:bg-accent",
                decision === option.value
                  ? "border-primary bg-primary/10"
                  : "border-border bg-background",
              )}
            >
              {option.label}
            </button>
          ))}
        </div>
        <label className="mt-4 block text-sm font-medium">
          Justificativa obrigatória
          <textarea
            value={justification}
            onChange={(event) => onJustificationChange(event.target.value)}
            rows={5}
            maxLength={2000}
            className="mt-2 w-full resize-y rounded-md border border-border bg-background px-3 py-2 text-sm"
            placeholder="Registre os fatos técnicos observados e o motivo do parecer."
          />
        </label>
        <label className="mt-3 flex items-start gap-3 rounded-md border border-border bg-background p-3 text-sm">
          <input
            type="checkbox"
            checked={shareWithCompany}
            onChange={(event) => onShareChange(event.target.checked)}
            className="mt-1"
          />
          <span>
            Exibir somente este status neutro e a data no relatório empresarial. Evidências,
            justificativa e responsável continuam restritos.
          </span>
        </label>
        {error != null && (
          <p className="mt-3 text-sm text-destructive">
            {error instanceof Error ? error.message : "Não foi possível salvar o parecer."}
          </p>
        )}
        <Button
          type="button"
          className="mt-4 w-full"
          disabled={!decision || justification.trim().length === 0 || saving}
          onClick={onSave}
        >
          {saving ? "Salvando parecer..." : "Salvar parecer auditável"}
        </Button>
      </section>

      <section className="rounded-xl border border-border bg-card p-5">
        <div className="flex items-center gap-2">
          <History className="h-4 w-4 text-primary" />
          <h3 className="text-lg font-semibold">Trilha de auditoria</h3>
        </div>
        <ol className="mt-4 space-y-2">
          {detail.auditTrail.map((entry) => (
            <li key={entry.id} className="rounded-md border border-border bg-background p-3 text-xs">
              <div className="flex flex-wrap justify-between gap-2 font-medium">
                <span>{auditLabel(entry.action)}</span>
                <span>{formatDate(entry.createdAt)}</span>
              </div>
              <div className="mt-1 text-muted-foreground">Responsável: {entry.actorUserId}</div>
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
}

function decisionLabel(decision: IntegrityReviewDecision) {
  return decisionOptions.find((option) => option.value === decision)?.label ?? decision;
}

function auditLabel(action: IntegrityReviewDetail["auditTrail"][number]["action"]) {
  return {
    QUEUE_CREATED: "Encaminhamento criado",
    EVIDENCE_ACCESSED: "Evidências acessadas",
    DECISION_RECORDED: "Parecer registrado ou alterado",
    EVIDENCE_DISCARDED: "Evidências descartadas",
  }[action];
}

function eventLabel(eventType: string) {
  const labels: Record<string, string> = {
    SESSION_STARTED: "Sessão iniciada",
    SESSION_RESUMED: "Sessão retomada",
    SESSION_EXPIRED: "Sessão expirada",
    HEARTBEAT: "Heartbeat recebido",
    TAB_HIDDEN: "Aba não visível",
    TAB_VISIBLE: "Aba visível",
    INPUT_MODE_CHANGED: "Modo de entrada alterado",
    NODE_PRESENTED: "Etapa apresentada",
    ASSET_LOADED: "Mídia carregada",
    STIMULUS_STARTED: "Estímulo iniciado",
    RESPONSE_SELECTED: "Resposta selecionada",
    RESPONSE_CONFIRMED: "Resposta confirmada",
    SESSION_CLOSED: "Sessão encerrada",
  };
  return labels[eventType] ?? eventType;
}

function formatDate(value: string | null) {
  if (!value) return "Sem data";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Sem data";
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(date);
}
