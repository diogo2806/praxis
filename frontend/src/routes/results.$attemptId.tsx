import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  ArrowLeft,
  CheckCircle2,
  ClipboardList,
  FileText,
  Plus,
  Printer,
  Save,
  Trash2,
} from "lucide-react";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { IntegritySharedStatus } from "@/components/integrity-shared-status";
import { StateBanner } from "@/components/praxis-ui";
import {
  getResultDetail,
  registerResultDecision,
  type AttemptStatus,
  type HumanDecision,
  type ResultDetailResponse,
} from "@/lib/api/praxis";
import {
  getResultExecutiveReport,
  saveResultInterviewGuide,
  type ResultExecutiveReportResponse,
  type ResultInterviewQuestion,
} from "@/lib/api/result-executive-report";
import { cn } from "@/lib/utils";
import { Route as ResultsRoute } from "./results";

export const Route = createFileRoute("/results/$attemptId")({
  head: () => ({
    meta: [
      { title: "Resultado do candidato - Práxis" },
      {
        name: "description",
        content: "Relatório executivo, evidências, roteiro estruturado e decisão humana.",
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
  const filters = ResultsRoute.useSearch();
  const resultQuery = useQuery({
    queryKey: ["result-detail", attemptId],
    queryFn: () => getResultDetail(attemptId),
    retry: false,
  });
  const executiveQuery = useQuery({
    queryKey: ["result-executive-report", attemptId],
    queryFn: () => getResultExecutiveReport(attemptId),
    retry: false,
  });

  return (
    <AppShell>
      <div className="space-y-6">
        <Link
          to="/results"
          search={filters}
          className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent print:hidden"
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
          <ResultDetailContent
            result={resultQuery.data}
            executiveReport={executiveQuery.data}
            executiveLoading={executiveQuery.isLoading}
            executiveError={executiveQuery.isError}
          />
        ) : null}
      </div>
    </AppShell>
  );
}

function ResultDetailContent({
  result,
  executiveReport,
  executiveLoading,
  executiveError,
}: {
  result: ResultDetailResponse;
  executiveReport?: ResultExecutiveReportResponse;
  executiveLoading: boolean;
  executiveError: boolean;
}) {
  return (
    <>
      <CandidateResultHeader result={result} />
      <div className="space-y-6">
        <IntegritySharedStatus attemptId={result.attemptId} />
        {executiveLoading ? (
          <section className="rounded-md border border-border bg-card p-5 text-sm text-muted-foreground">
            Preparando relatório executivo e roteiro de entrevista...
          </section>
        ) : executiveError || !executiveReport ? (
          <StateBanner tone="danger" title="O relatório executivo não pôde ser carregado.">
            As competências, respostas e decisão humana continuam disponíveis abaixo.
          </StateBanner>
        ) : (
          <>
            <ExecutiveReportPanel report={executiveReport} />
            <InterviewGuidePanel report={executiveReport} />
            <AuditTrailPanel report={executiveReport} />
          </>
        )}
        <CompetencyBreakdown competencies={result.competencies} />
        <CandidateAnswersPanel answers={result.answers} />
        <HumanDecisionPanel result={result} />
      </div>
    </>
  );
}

function CandidateResultHeader({ result }: { result: ResultDetailResponse }) {
  return (
    <section className="rounded-md border border-border bg-card p-5 print:border-0 print:p-0">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Resultado do candidato</div>
          <h1 className="mt-1 text-3xl font-semibold">{result.candidate.name}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{result.candidate.email}</p>
        </div>
        <div className="flex items-start gap-4">
          <div className="text-right">
            <div className="text-4xl font-semibold tabular-nums">
              {result.overallScore == null ? "Sem resultado final" : `${result.overallScore}%`}
            </div>
            <div className="mt-1 text-xs uppercase text-muted-foreground">Resultado geral</div>
          </div>
          <button
            type="button"
            onClick={() => window.print()}
            className="inline-flex min-h-10 items-center gap-2 rounded-md border border-border bg-background px-3 py-2 text-sm font-medium hover:bg-accent print:hidden"
          >
            <Printer className="h-4 w-4" />
            Imprimir / salvar PDF
          </button>
        </div>
      </div>
      <dl className="mt-5 grid gap-3 text-sm md:grid-cols-2 xl:grid-cols-5">
        <InfoLine label="Avaliação" value={result.simulation.title} />
        <InfoLine
          label="Versão"
          value={result.simulation.versionNumber == null ? "Não informada" : `v${result.simulation.versionNumber}`}
        />
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

function ExecutiveReportPanel({ report }: { report: ResultExecutiveReportResponse }) {
  return (
    <section className="rounded-md border border-border bg-card">
      <header className="flex flex-wrap items-start justify-between gap-3 border-b border-border p-4">
        <div>
          <h2 className="text-lg font-semibold">Visão executiva</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Leitura determinística baseada na versão {report.simulationVersionNumber ?? "não informada"} e nas evidências do percurso.
          </p>
        </div>
        <span className="rounded-md border border-primary/25 bg-primary/10 px-2 py-1 text-xs font-medium text-primary">
          Sem recomendação automática
        </span>
      </header>

      <div className="grid gap-4 p-4 lg:grid-cols-2">
        {report.summary.competencies.map((competency) => (
          <article key={competency.name} className="rounded-md border border-border bg-background p-4">
            <div className="flex items-center justify-between gap-3">
              <h3 className="font-semibold">{competency.name}</h3>
              <span className="font-semibold tabular-nums">{competency.score}%</span>
            </div>
            <div className="mt-3 space-y-3 text-sm">
              <LabeledText
                label="Dado observado"
                value={`${competency.evidenceCount} evidência(s): ${competency.evidenceReferences.join(", ") || "sem referência recuperável"}.`}
              />
              <LabeledText label="Interpretação por regra" value={competency.interpretation} />
            </div>
          </article>
        ))}
      </div>

      <div className="grid gap-4 border-t border-border p-4 lg:grid-cols-2">
        <div>
          <h3 className="font-semibold">Evidências críticas selecionadas</h3>
          {report.summary.criticalEvidence.length === 0 ? (
            <p className="mt-2 text-sm text-muted-foreground">Nenhuma alternativa crítica foi selecionada.</p>
          ) : (
            <div className="mt-3 space-y-3">
              {report.summary.criticalEvidence.map((evidence) => (
                <article key={evidence.reference} className="rounded-md border border-border bg-background p-3 text-sm">
                  <div className="font-medium">{evidence.stepTitle} · {evidence.reference}</div>
                  <LabeledText label="Evidência observada" value={evidence.observedAnswer} className="mt-2" />
                  <LabeledText
                    label="Interpretação configurada pela empresa"
                    value={evidence.configuredInterpretation ?? "Não cadastrada na versão da avaliação."}
                    className="mt-2"
                  />
                </article>
              ))}
            </div>
          )}
        </div>
        <div className="space-y-4">
          <div>
            <h3 className="font-semibold">Pontos para aprofundar</h3>
            <p className="mt-2 text-sm text-muted-foreground">
              {report.summary.deepDiveCompetencies.length === 0
                ? "Nenhuma competência ficou abaixo da faixa alta configurada."
                : report.summary.deepDiveCompetencies.join(", ")}
            </p>
          </div>
          <div>
            <h3 className="font-semibold">Limitações da leitura</h3>
            <ul className="mt-2 space-y-2 text-sm text-muted-foreground">
              {report.summary.limitations.map((limitation) => (
                <li key={limitation}>• {limitation}</li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </section>
  );
}

function InterviewGuidePanel({ report }: { report: ResultExecutiveReportResponse }) {
  const queryClient = useQueryClient();
  const [questions, setQuestions] = useState<ResultInterviewQuestion[]>([]);
  const [interviewerNotes, setInterviewerNotes] = useState("");

  useEffect(() => {
    setQuestions(report.interviewGuide.questions.map((question) => ({ ...question })));
    setInterviewerNotes(report.interviewGuide.interviewerNotes ?? "");
  }, [report]);

  const mutation = useMutation({
    mutationFn: () =>
      saveResultInterviewGuide(report.attemptId, {
        questions,
        interviewerNotes: interviewerNotes.trim() || null,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["result-executive-report", report.attemptId] });
    },
  });

  function updateQuestion(index: number, patch: Partial<ResultInterviewQuestion>) {
    setQuestions((current) =>
      current.map((question, questionIndex) =>
        questionIndex === index ? { ...question, ...patch } : question,
      ),
    );
  }

  function addQuestion() {
    setQuestions((current) => [
      ...current,
      {
        id: `interviewer-${Date.now()}`,
        competency: null,
        question: "",
        sourceType: "INTERVIEWER",
        evidenceReference: null,
      },
    ]);
  }

  return (
    <section className="rounded-md border border-border bg-card">
      <header className="border-b border-border p-4">
        <div className="flex items-center gap-2">
          <ClipboardList className="h-5 w-5 text-primary" />
          <h2 className="text-lg font-semibold">Roteiro de entrevista estruturada</h2>
        </div>
        <p className="mt-1 text-sm text-muted-foreground">
          Perguntas iniciais geradas por regras fixas. Edite, remova ou acrescente perguntas antes de registrar.
        </p>
        {report.interviewGuide.persisted && (
          <p className="mt-2 text-xs text-muted-foreground">
            Último registro em {formatDate(report.interviewGuide.savedAt)}
            {report.interviewGuide.savedBy ? ` por ${report.interviewGuide.savedBy}` : ""}.
          </p>
        )}
      </header>

      <div className="space-y-4 p-4">
        {questions.length === 0 ? (
          <p className="rounded-md border border-dashed border-border p-4 text-sm text-muted-foreground">
            Todas as perguntas foram removidas. Acrescente uma pergunta ou registre o roteiro vazio.
          </p>
        ) : (
          questions.map((question, index) => (
            <article key={question.id} className="rounded-md border border-border bg-background p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="text-xs font-medium uppercase text-muted-foreground">
                  {sourceLabel(question.sourceType)}
                  {question.evidenceReference ? ` · ${question.evidenceReference}` : ""}
                </div>
                <button
                  type="button"
                  onClick={() => setQuestions((current) => current.filter((_, itemIndex) => itemIndex !== index))}
                  className="rounded-md p-2 text-muted-foreground hover:bg-destructive/10 hover:text-destructive print:hidden"
                  aria-label={`Remover pergunta ${index + 1}`}
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
              <label className="mt-3 block text-xs font-medium uppercase text-muted-foreground">
                Competência
                <input
                  value={question.competency ?? ""}
                  onChange={(event) => updateQuestion(index, { competency: event.target.value || null })}
                  maxLength={180}
                  className="mt-1 h-10 w-full rounded-md border border-border bg-card px-3 text-sm text-foreground"
                />
              </label>
              <label className="mt-3 block text-xs font-medium uppercase text-muted-foreground">
                Pergunta
                <textarea
                  value={question.question}
                  onChange={(event) => updateQuestion(index, { question: event.target.value })}
                  maxLength={600}
                  rows={3}
                  className="mt-1 w-full resize-y rounded-md border border-border bg-card px-3 py-2 text-sm text-foreground"
                />
              </label>
            </article>
          ))
        )}

        <button
          type="button"
          onClick={addQuestion}
          className="inline-flex items-center gap-2 rounded-md border border-border bg-background px-3 py-2 text-sm font-medium hover:bg-accent print:hidden"
        >
          <Plus className="h-4 w-4" />
          Acrescentar pergunta
        </button>

        <label className="block text-sm font-medium">
          Anotações do entrevistador
          <textarea
            value={interviewerNotes}
            onChange={(event) => setInterviewerNotes(event.target.value)}
            rows={5}
            maxLength={4000}
            placeholder="Registre observações factuais da entrevista. A decisão final permanece em seção separada."
            className="mt-2 w-full resize-y rounded-md border border-border bg-background px-3 py-2 text-sm"
          />
        </label>

        <button
          type="button"
          disabled={mutation.isPending || questions.some((question) => !question.question.trim())}
          onClick={() => mutation.mutate()}
          className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50 print:hidden"
        >
          <Save className="h-4 w-4" />
          {mutation.isPending ? "Registrando..." : "Registrar roteiro na auditoria"}
        </button>
        {mutation.isError && (
          <p className="text-sm text-destructive">
            {mutation.error instanceof Error ? mutation.error.message : "Não foi possível registrar o roteiro."}
          </p>
        )}
      </div>
    </section>
  );
}

function AuditTrailPanel({ report }: { report: ResultExecutiveReportResponse }) {
  return (
    <section className="rounded-md border border-border bg-card">
      <header className="border-b border-border p-4">
        <h2 className="text-lg font-semibold">Trilha de auditoria</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Eventos da tentativa preservados para identificar versão, data e ações humanas registradas.
        </p>
      </header>
      <div className="divide-y divide-border">
        {report.auditTrail.map((event, index) => (
          <div key={`${event.eventType}-${event.createdAt}-${index}`} className="grid gap-1 p-4 text-sm md:grid-cols-[180px_1fr_190px]">
            <div className="font-medium">{event.eventType}</div>
            <div className="text-muted-foreground">{event.message}</div>
            <div className="text-muted-foreground md:text-right">{formatDate(event.createdAt)}</div>
          </div>
        ))}
      </div>
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
        <div className="p-4 text-sm text-muted-foreground">Nenhuma competência pontuada para este resultado.</div>
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
        <h2 className="text-lg font-semibold">Respostas e percurso</h2>
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
                <AnswerLine label="Evidência observada" value={answer.answer ?? "-"} />
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
      await queryClient.invalidateQueries({ queryKey: ["result-executive-report", result.attemptId] });
      await queryClient.invalidateQueries({ queryKey: ["results"] });
      setDecision("");
      setNote("");
    },
  });

  return (
    <aside className="rounded-md border border-border bg-card p-4">
      <h2 className="text-lg font-semibold">Decisão humana final</h2>
      <p className="mt-1 text-sm text-muted-foreground">
        A pontuação, o relatório e o roteiro são apoio. A decisão deve ser tomada e registrada por uma pessoa responsável.
      </p>
      {result.humanDecision.status && (
        <div className="mt-4 rounded-md border border-border bg-background p-3 text-sm">
          <div className="flex items-center gap-2 font-medium">
            <CheckCircle2 className="h-4 w-4 text-success" />
            {decisionLabel(result.humanDecision.status)}
          </div>
          <div className="mt-2 text-xs text-muted-foreground">
            {result.humanDecision.decidedBy
              ? `Responsável: ${result.humanDecision.decidedBy}`
              : "Responsável não informado"}
          </div>
          <div className="mt-1 text-xs text-muted-foreground">{formatDate(result.humanDecision.decidedAt)}</div>
          {result.humanDecision.note && (
            <p className="mt-2 text-sm text-muted-foreground">{result.humanDecision.note}</p>
          )}
        </div>
      )}
      <div className="mt-4 grid gap-2 print:hidden">
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
      <label className="mt-4 block text-sm font-medium print:hidden">
        Justificativa da decisão
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
        className="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50 print:hidden"
      >
        <Save className="h-4 w-4" />
        {mutation.isPending ? "Salvando..." : "Salvar decisão"}
      </button>
      {mutation.isError && (
        <p className="mt-2 text-sm text-destructive">
          {mutation.error instanceof Error ? mutation.error.message : "Não foi possível registrar a decisão."}
        </p>
      )}
    </aside>
  );
}

function LabeledText({ label, value, className }: { label: string; value: string; className?: string }) {
  return (
    <div className={className}>
      <div className="text-xs font-medium uppercase text-muted-foreground">{label}</div>
      <p className="mt-1 text-sm">{value}</p>
    </div>
  );
}

function CompetencyScoreBadge({ level }: { level: string }) {
  return <span className={cn("rounded-md px-2 py-1 text-xs font-medium", competencyTone(level))}>{levelLabel(level)}</span>;
}

function ResultStatusBadge({ status }: { status: AttemptStatus }) {
  const meta = resultStatusMeta(status);
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-[11px] font-medium ${meta.className}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {meta.label}
    </span>
  );
}

function resultStatusMeta(status: AttemptStatus) {
  return (
    {
      notStarted: { label: "Criado", className: "border-border bg-muted text-foreground" },
      inProgress: { label: "Em andamento", className: "border-primary/25 bg-primary/10 text-foreground" },
      completed: { label: "Concluído", className: "border-success/25 bg-success/10 text-foreground" },
      abandoned: { label: "Abandonado", className: "border-danger/25 bg-danger/10 text-foreground" },
      expired: { label: "Expirado", className: "border-warning/35 bg-warning/15 text-warning-foreground" },
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
  return (
    {
      ALTO: "bg-success/10 text-success",
      MEDIO: "bg-muted text-muted-foreground",
      BAIXO: "bg-warning/10 text-warning",
    }[level] ?? "bg-muted text-muted-foreground"
  );
}

function levelLabel(level: string) {
  return ({ ALTO: "Alto", MEDIO: "Médio", BAIXO: "Baixo" }[level] ?? level);
}

function decisionLabel(value: string) {
  return (
    {
      ADVANCED: "Avançar candidato",
      REJECTED: "Reprovar",
      HIRED: "Contratar",
      ON_HOLD: "Colocar em espera",
      advanced: "Avançar candidato",
      rejected: "Reprovar",
      hired: "Contratar",
      onHold: "Colocar em espera",
    }[value] ?? value
  );
}

function sourceLabel(value: ResultInterviewQuestion["sourceType"]) {
  return {
    RULE: "Pergunta por regra",
    EVIDENCE: "Pergunta vinculada à evidência",
    INTERVIEWER: "Pergunta adicionada pelo entrevistador",
  }[value];
}

function formatDate(value: string | null) {
  return value
    ? new Intl.DateTimeFormat("pt-BR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      }).format(new Date(value))
    : "-";
}
