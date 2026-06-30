import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, ClipboardCheck, ExternalLink, Loader2, Play, RefreshCw } from "lucide-react";
import {
  completePublicAssessmentJourneyAttemptStep,
  getPublicAssessmentJourneyAttempt,
  getPublicAssessmentJourneyResult,
  startPublicAssessmentJourneyAttempt,
  startPublicAssessmentJourneyAttemptStep,
  type AssessmentJourneyAttemptResponse,
  type JourneyAttemptStepResponse,
} from "@/lib/api/praxis";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/jornada/$attemptId")({
  head: () => ({
    meta: [
      { title: "Jornada de Avaliacao - Praxis" },
      {
        name: "description",
        content: "Acesso do candidato a uma Jornada de Avaliacao.",
      },
    ],
  }),
  component: CandidateJourneyPage,
});

function CandidateJourneyPage() {
  const { attemptId } = Route.useParams();
  const queryClient = useQueryClient();
  const attemptQuery = useQuery({
    queryKey: ["public-assessment-journey-attempt", attemptId],
    queryFn: () => getPublicAssessmentJourneyAttempt(attemptId),
    retry: false,
  });
  const resultQuery = useQuery({
    queryKey: ["public-assessment-journey-result", attemptId],
    queryFn: () => getPublicAssessmentJourneyResult(attemptId),
    enabled: attemptQuery.data?.status === "COMPLETED",
    retry: false,
  });
  const startJourneyMutation = useMutation({
    mutationFn: () => startPublicAssessmentJourneyAttempt(attemptId),
    onSuccess: async () => invalidateAttempt(queryClient, attemptId),
  });
  const startStepMutation = useMutation({
    mutationFn: (stepId: number) => startPublicAssessmentJourneyAttemptStep(attemptId, stepId),
    onSuccess: async (attempt) => {
      await invalidateAttempt(queryClient, attemptId);
      const startedStep = attempt.steps.find(
        (step) => step.status === "IN_PROGRESS" && step.candidateUrl,
      );
      if (startedStep?.candidateUrl) {
        window.location.href = toParticipantPageUrl(startedStep.candidateUrl);
      }
    },
  });
  const completeStepMutation = useMutation({
    mutationFn: (stepId: number) => completePublicAssessmentJourneyAttemptStep(attemptId, stepId),
    onSuccess: async () => invalidateAttempt(queryClient, attemptId),
  });

  const attempt = attemptQuery.data;
  const mutationError =
    startJourneyMutation.error || startStepMutation.error || completeStepMutation.error;

  return (
    <main className="min-h-screen bg-background px-4 py-8 text-foreground sm:px-6 lg:px-8">
      <div className="mx-auto max-w-4xl space-y-6">
        <header className="rounded-md border border-border bg-card p-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <div className="text-xs font-semibold uppercase text-primary">
                Jornada de Avaliacao
              </div>
              <h1 className="mt-1 text-3xl font-semibold">
                {attempt?.journeyName ?? "Carregando jornada"}
              </h1>
              {attempt && (
                <p className="mt-2 text-sm text-muted-foreground">
                  Participante:{" "}
                  <span className="font-medium text-foreground">{attempt.candidateName}</span>
                </p>
              )}
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-9 gap-2 bg-card"
              onClick={() => void attemptQuery.refetch()}
            >
              <RefreshCw className="h-4 w-4" />
              Atualizar
            </Button>
          </div>
        </header>

        {attemptQuery.isLoading ? (
          <section className="rounded-md border border-border bg-card p-8 text-center text-sm text-muted-foreground">
            <Loader2 className="mx-auto mb-3 h-5 w-5 animate-spin" />
            Carregando sua jornada...
          </section>
        ) : attemptQuery.isError || !attempt ? (
          <StateBanner tone="danger" title="Nao foi possivel abrir a jornada">
            {attemptQuery.error instanceof Error
              ? attemptQuery.error.message
              : "Confira o link recebido."}
          </StateBanner>
        ) : (
          <>
            {mutationError && (
              <StateBanner tone="danger" title="Nao foi possivel atualizar a jornada">
                {mutationError instanceof Error
                  ? mutationError.message
                  : "Atualize a pagina e tente novamente."}
              </StateBanner>
            )}

            <ProgressSummary attempt={attempt} />

            {attempt.status === "CREATED" && (
              <section className="rounded-md border border-border bg-card p-5">
                <Button
                  type="button"
                  className="h-10 gap-2"
                  disabled={startJourneyMutation.isPending}
                  onClick={() => startJourneyMutation.mutate()}
                >
                  <Play className="h-4 w-4" />
                  Iniciar jornada
                </Button>
              </section>
            )}

            <section className="rounded-md border border-border bg-card">
              <div className="border-b border-border p-5">
                <h2 className="text-xl font-semibold">Testes da jornada</h2>
                <p className="mt-1 text-sm text-muted-foreground">
                  Siga a ordem abaixo. Ao concluir um teste, volte para esta pagina e confirme a
                  etapa.
                </p>
              </div>
              <div className="divide-y divide-border">
                {attempt.steps.map((step, index) => (
                  <CandidateJourneyStep
                    key={step.id}
                    attempt={attempt}
                    step={step}
                    index={index}
                    canStart={canStartStep(attempt, step)}
                    onStart={() => startStepMutation.mutate(step.id)}
                    startPending={startStepMutation.isPending}
                    onComplete={() => completeStepMutation.mutate(step.id)}
                    completePending={completeStepMutation.isPending}
                  />
                ))}
              </div>
            </section>

            {attempt.status === "COMPLETED" && (
              <section className="rounded-md border border-border bg-card p-5">
                <div className="flex items-center gap-2 text-success">
                  <CheckCircle2 className="h-5 w-5" />
                  <h2 className="text-xl font-semibold">Jornada concluida</h2>
                </div>
                {resultQuery.data && (
                  <div className="mt-4 grid gap-3 sm:grid-cols-2">
                    {resultQuery.data.tests.map((test) => (
                      <div
                        key={test.simulationId}
                        className="rounded-md border border-border bg-background p-4"
                      >
                        <div className="text-sm font-medium">{test.simulationName}</div>
                        <div className="mt-1 text-xs text-muted-foreground">
                          Pontuacao: {test.score ?? "pendente"}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </section>
            )}
          </>
        )}
      </div>
    </main>
  );
}

function ProgressSummary({ attempt }: { attempt: AssessmentJourneyAttemptResponse }) {
  const completed = attempt.steps.filter((step) => step.status === "COMPLETED").length;
  const total = attempt.steps.length;
  const percent = Math.round((completed / Math.max(total, 1)) * 100);

  return (
    <section className="rounded-md border border-border bg-card p-5">
      <div className="flex items-center justify-between gap-4">
        <div>
          <div className="text-sm font-medium">Progresso geral</div>
          <div className="mt-1 text-xs text-muted-foreground">
            {completed} de {total} testes concluidos
          </div>
        </div>
        <div className="text-2xl font-semibold tabular-nums">{percent}%</div>
      </div>
      <div className="mt-4 h-2 overflow-hidden rounded-full bg-muted">
        <div className="h-full bg-primary" style={{ width: `${percent}%` }} />
      </div>
    </section>
  );
}

function CandidateJourneyStep({
  attempt,
  step,
  index,
  canStart,
  onStart,
  startPending,
  onComplete,
  completePending,
}: {
  attempt: AssessmentJourneyAttemptResponse;
  step: JourneyAttemptStepResponse;
  index: number;
  canStart: boolean;
  onStart: () => void;
  startPending: boolean;
  onComplete: () => void;
  completePending: boolean;
}) {
  const completed = step.status === "COMPLETED";
  const inProgress = step.status === "IN_PROGRESS";

  return (
    <article className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex min-w-0 gap-4">
        <div
          className={cn(
            "flex h-10 w-10 shrink-0 items-center justify-center rounded-full border text-sm font-semibold",
            completed
              ? "border-success bg-success/10 text-success"
              : inProgress
                ? "border-primary bg-primary/10 text-primary"
                : "border-border bg-background text-muted-foreground",
          )}
        >
          {completed ? <CheckCircle2 className="h-5 w-5" /> : index + 1}
        </div>
        <div className="min-w-0">
          <h3 className="truncate text-sm font-semibold">{step.simulationName}</h3>
          <p className="mt-1 text-xs text-muted-foreground">
            v{step.simulationVersionNumber} - {step.required ? "obrigatorio" : "opcional"} -{" "}
            {stepStatusLabel(step.status)}
          </p>
        </div>
      </div>

      <div className="flex flex-wrap gap-2 sm:justify-end">
        {step.candidateUrl && !completed && (
          <Button asChild variant="outline" size="sm" className="h-9 gap-2 bg-background">
            <a href={toParticipantPageUrl(step.candidateUrl)}>
              <ExternalLink className="h-4 w-4" />
              Abrir teste
            </a>
          </Button>
        )}
        {!step.candidateAttemptId && (
          <Button
            type="button"
            size="sm"
            className="h-9 gap-2"
            disabled={!canStart || attempt.status === "COMPLETED" || startPending}
            onClick={onStart}
          >
            <ClipboardCheck className="h-4 w-4" />
            Iniciar teste
          </Button>
        )}
        {step.candidateAttemptId && !completed && (
          <Button
            type="button"
            size="sm"
            className="h-9 gap-2"
            disabled={completePending}
            onClick={onComplete}
          >
            <CheckCircle2 className="h-4 w-4" />
            Confirmar conclusao
          </Button>
        )}
      </div>
    </article>
  );
}

function canStartStep(
  attempt: AssessmentJourneyAttemptResponse,
  target: JourneyAttemptStepResponse,
) {
  if (attempt.status === "COMPLETED") return false;
  return attempt.steps
    .filter((step) => step.orderIndex < target.orderIndex && step.required)
    .every((step) => step.status === "COMPLETED");
}

function stepStatusLabel(status: JourneyAttemptStepResponse["status"]) {
  const labels: Record<JourneyAttemptStepResponse["status"], string> = {
    PENDING: "pendente",
    IN_PROGRESS: "em andamento",
    COMPLETED: "concluido",
    SKIPPED: "ignorado",
  };
  return labels[status];
}

function toParticipantPageUrl(candidateUrl: string) {
  if (typeof window === "undefined") {
    return candidateUrl;
  }
  try {
    const token = new URL(candidateUrl).pathname.split("/candidato/")[1];
    if (token) {
      return `${window.location.origin}/candidato/${token}`;
    }
  } catch {
    // Mantem a URL da API quando ela nao puder ser analisada.
  }
  return candidateUrl;
}

async function invalidateAttempt(
  queryClient: ReturnType<typeof useQueryClient>,
  attemptId: string,
) {
  await queryClient.invalidateQueries({
    queryKey: ["public-assessment-journey-attempt", attemptId],
  });
  await queryClient.invalidateQueries({
    queryKey: ["public-assessment-journey-result", attemptId],
  });
}
