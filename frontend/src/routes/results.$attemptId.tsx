import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CheckCircle2, FileText, Save } from "lucide-react";
import { useState } from "react";
import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import {
  getResultDetail,
  registerResultDecision,
  type AttemptStatus,
  type HumanDecision,
  type ResultDetailResponse,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/results/$attemptId")({
  head: () => ({
    meta: [
      { title: "Resultado do candidato - Práxis" },
      {
        name: "description",
        content: "Detalhe do resultado por candidato, competências, respostas e decisão humana.",
      },
    ],
  }),
  component: ResultDetailPage,
});

const decisionOptions: Array<{ value: HumanDecision; label: string }> = [
  { value: "advanced", label: "Avançar candidato" },
  { value: "rejected", label: "Reprovar" },
  { value: "hired", label: "Contratar" },
  { value: "onHold", label: "Colocar em espera" },
];

function ResultDetailPage() {
  const { attemptId } = Route.useParams();
  const resultQuery = useQuery({
    queryKey: ["result-detail", attemptId],
    queryFn: () => getResultDetail(attemptId),
    retry: false,
  });

  return (
    <AppShell>
      <div className="space-y-6">
        <Link
          to="/results"
          className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          <ArrowLeft className="h-4 w-4" />
          Voltar para resultados
        </Link>

        {resultQuery.isLoading ? (
          <section className="rounded-md border border-border bg-card p-6 text-sm font-medium">
            Carregando resultado...
          </section>
        ) : resultQuery.isError ? (
          <StateBanner tone="danger" title="Não foi possível carregar o resultado.">
            Tente novamente.
          </StateBanner>
        ) : resultQuery.data ? (
          <ResultDetailContent result={resultQuery.data} />
        ) : null}
      </div>
    </AppShell>
  );
}

function ResultDetailContent({ result }: { result: ResultDetailResponse }) {
  return (
    <>
      <CandidateResultHeader result={result} />
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <div className="space-y-6">
          <CompetencyBreakdown competencies={result.competencies} />
          <CandidateAnswersPanel answers={result.answers} />
        </div>
        <HumanDecisionPanel result={result} />
      </div>
    </>
  );
}

function CandidateResultHeader({ result }: { result: ResultDetailResponse }) {
  return (
    <section className="rounded-md border border-border bg-card p-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Resultado do candidato</div>
          <h1 className="mt-1 text-3xl font-semibold">{result.candidate.name}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{result.candidate.email}</p>
        </div>
        <div className="text-right">
          <div className="text-4xl font-semibold tabular-nums">
            {result.overallScore == null ? "-" : `${result.overallScore}%`}
          </div>
          <div className="mt-1 text-xs uppercase text-muted-foreground">Resultado geral</div>
        </div>
      </div>
      <dl className="mt-5 grid gap-3 text-sm md:grid-cols-2 xl:grid-cols-4">
        <InfoLine label="Avaliação" value={result.simulation.title} />
        <div>
          <dt className="text-xs uppercase text-muted-foreground">Status</dt>
          <dd className="mt-1">
            <ResultStatusBadge status={result.status} />
          </dd>
        </div>
        <InfoLine label="Iniciado em" value={formatDate(result.startedAt)} />
        <InfoLine label="Concluído em" value={formatDate(result.finishedAt)} />
      </dl>
    </section>
  );
}

function CompetencyBreakdown({ competencies }: { competencies: ResultDetailResponse["competencies"] }) {
  return (
    <section className="rounded-md border border-border bg-card">
      <div className="border-b border-border p-4">
        <h2 className="text-lg font-semibold">Competências</h2>
      </div>
      {competencies.length === 0 ? (
        <div className="p-4 text-sm text-muted-foreground">
          Nenhuma competência pontuada para este resultado.
        </div>
      ) : (
        <div className="divide-y divide-border">
          {competencies.map((competency) => (
            <div key={competency.name} className="p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="font-medium">{competency.name}</div>
                <div className="flex items-center gap-2">
                  <CompetencyScoreBadge level={competency.level} />
                  <span className="text-lg font-semibold tabular-nums">{competency.score}%</span>
                </div>
              </div>
              <p className="mt-2 text-sm text-muted-foreground">{competency.summary}</p>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

function CandidateAnswersPanel({ answers }: { answers: ResultDetailResponse["answers"] }) {
  return (
    <section className="rounded-md border border-border bg-card">
      <div className="border-b border-border p-4">
        <h2 className="text-lg font-semibold">Respostas</h2>
      </div>
      {answers.length === 0 ? (
        <div className="p-4 text-sm text-muted-foreground">Nenhuma resposta registrada.</div>
      ) : (
        <div className="divide-y divide-border">
          {answers.map((answer, index) => (
            <article key={`${answer.stepTitle}-${index}`} className="p-4">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <FileText className="h-4 w-4 text-muted-foreground" />
                {answer.stepTitle}
              </div>
              <dl className="mt-3 space-y-2 text-sm">
                <AnswerLine label="Pergunta" value={answer.question} />
                <AnswerLine label="Resposta" value={answer.answer ?? "-"} />
                <AnswerLine label="Pontuação" value={answer.score == null ? "-" : `${answer.score}/10`} />
              </dl>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function HumanDecisionPanel({ result }: { result: ResultDetailResponse }) {
  const queryClient = useQueryClient();
  const [decision, setDecision] = useState<HumanDecision | "">("");
  const [note, setNote] = useState("");
  const mutation = useMutation({
    mutationFn: () =>
      registerResultDecision(result.attemptId, {
        decision: decision as HumanDecision,
        note: note.trim() || undefined,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["result-detail", result.attemptId] });
      await queryClient.invalidateQueries({ queryKey: ["results"] });
      setDecision("");
      setNote("");
    },
  });

  return (
    <aside className="rounded-md border border-border bg-card p-4">
      <h2 className="text-lg font-semibold">Decisão humana</h2>
      <p className="mt-1 text-sm text-muted-foreground">
        A pontuação é apoio. A decisão final deve ser registrada por uma pessoa.
      </p>
      {result.humanDecision.status && (
        <div className="mt-4 rounded-md border border-border bg-background p-3 text-sm">
          <div className="flex items-center gap-2 font-medium">
            <CheckCircle2 className="h-4 w-4 text-success" />
            {decisionLabel(result.humanDecision.status)}
          </div>
          <div className="mt-2 text-xs text-muted-foreground">
            {result.humanDecision.decidedBy ? `Responsável: ${result.humanDecision.decidedBy}` : "Responsável não informado"}
          </div>
          <div className="mt-1 text-xs text-muted-foreground">
            {formatDate(result.humanDecision.decidedAt)}
          </div>
          {result.humanDecision.note && (
            <p className="mt-2 text-sm text-muted-foreground">{result.humanDecision.note}</p>
          )}
        </div>
      )}
      <div className="mt-4 grid gap-2">
        {decisionOptions.map((option) => (
          <button
            key={option.value}
            type="button"
            onClick={() => setDecision(option.value)}
            className={cn(
              "rounded-md border px-3 py-2 text-left text-sm hover:bg-accent",
              decision === option.value
                ? "border-primary bg-primary/10 text-foreground"
                : "border-border bg-background",
            )}
          >
            {option.label}
          </button>
        ))}
      </div>
      <label className="mt-4 block text-sm font-medium">
        Observação
        <textarea
          value={note}
          onChange={(event) => setNote(event.target.value)}
          rows={5}
          maxLength={1000}
          className="mt-2 w-full resize-none rounded-md border border-border bg-background px-3 py-2 text-sm"
        />
      </label>
      <button
        type="button"
        disabled={!decision || mutation.isPending}
        onClick={() => mutation.mutate()}
        className="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
      >
        <Save className="h-4 w-4" />
        {mutation.isPending ? "Salvando..." : "Salvar decisão"}
      </button>
      {mutation.isError && (
        <p className="mt-2 text-sm text-destructive">
          {mutation.error instanceof Error
            ? mutation.error.message
            : "Não foi possível registrar a decisão."}
        </p>
      )}
    </aside>
  );
}

function CompetencyScoreBadge({ level }: { level: string }) {
  return (
    <span className={cn("rounded-md px-2 py-1 text-xs font-medium", competencyTone(level))}>
      {levelLabel(level)}
    </span>
  );
}

function ResultStatusBadge({ status }: { status: AttemptStatus }) {
  const meta = resultStatusMeta(status);
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-[11px] font-medium ${meta.className}`}
    >
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {meta.label}
    </span>
  );
}

function resultStatusMeta(status: AttemptStatus) {
  return (
    {
      notStarted: {
        label: "Criado",
        className: "border-border bg-muted text-foreground",
      },
      inProgress: {
        label: "Em andamento",
        className: "border-primary/25 bg-primary/10 text-foreground",
      },
      paused: {
        label: "Pausado",
        className: "border-warning/35 bg-warning/15 text-warning-foreground",
      },
      completed: {
        label: "Concluído",
        className: "border-success/25 bg-success/10 text-foreground",
      },
      abandoned: {
        label: "Abandonado",
        className: "border-danger/25 bg-danger/10 text-foreground",
      },
      expired: {
        label: "Expirado",
        className: "border-warning/35 bg-warning/15 text-warning-foreground",
      },
      failed: {
        label: "Falhou",
        className: "border-danger/25 bg-danger/10 text-foreground",
      },
    } satisfies Record<AttemptStatus, { label: string; className: string }>
  )[status];
}

function InfoLine({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs uppercase text-muted-foreground">{label}</dt>
      <dd className="mt-1 font-medium">{value}</dd>
    </div>
  );
}

function AnswerLine({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs uppercase text-muted-foreground">{label}</dt>
      <dd className="mt-1">{value}</dd>
    </div>
  );
}

function competencyTone(level: string) {
  return {
    ALTO: "bg-success/10 text-success",
    MEDIO: "bg-muted text-muted-foreground",
    BAIXO: "bg-warning/10 text-warning",
  }[level] ?? "bg-muted text-muted-foreground";
}

function levelLabel(level: string) {
  return {
    ALTO: "Alto",
    MEDIO: "Médio",
    BAIXO: "Baixo",
  }[level] ?? level;
}

function decisionLabel(value: string) {
  return {
    ADVANCED: "Avançar candidato",
    REJECTED: "Reprovar",
    HIRED: "Contratar",
    ON_HOLD: "Colocar em espera",
    advanced: "Avançar candidato",
    rejected: "Reprovar",
    hired: "Contratar",
    onHold: "Colocar em espera",
  }[value] ?? value;
}

function formatDate(value: string | null) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
