import { createFileRoute, Link, Outlet, useChildMatches } from "@tanstack/react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useCallback, useEffect, useState, type CSSProperties } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState } from "@/components/praxis-ui";
import {
  getCandidateAttempt,
  PraxisApiError,
  recordHealthConsent,
  requestHumanReview,
  submitCandidateAnswer,
  type CandidateAttemptResponse,
  type CandidateNodeResponse,
  type MediaType,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/candidato")({
  head: () => ({
    meta: [
      { title: "Teste do candidato - Praxis" },
      {
        name: "description",
        content: "Experiência mobile-first com timer, respostas claras, retomada e acessibilidade.",
      },
    ],
  }),
  component: CandidateRouteLayout,
});

const SUBMIT_ERROR_MESSAGES: Record<number, string> = {
  400: "Não consegui registrar agora. Toque na resposta de novo, por favor.",
  409: "O tempo desta etapa terminou. Sua resposta foi registrada e seguimos para a próxima.",
};

const LOAD_ERROR_MESSAGES: Record<number, string> = {
  400: "Não consegui abrir este teste. Verifique o link recebido e tente novamente.",
  404: "Não encontrei este teste. Verifique o link recebido e tente novamente.",
  409: "Este teste não está mais disponível.",
};

function friendlyApiErrorMessage(
  error: unknown,
  messagesByStatus: Record<number, string>,
  fallback: string,
) {
  if (error instanceof PraxisApiError) {
    return messagesByStatus[error.status] ?? fallback;
  }
  return fallback;
}

// `candidato.tsx` is the parent route of `candidato.$token.tsx`, so it must
// render an <Outlet /> for the token experience to show. Without it, opening
// `/candidato/:token` (a link that already carries the token) would fall back
// to this entry form and ask for the token again. We only show the entry form
// when no child route (token) is active.
function CandidateRouteLayout() {
  const childMatches = useChildMatches();
  if (childMatches.length > 0) {
    return <Outlet />;
  }
  return <CandidateEntryPage />;
}

function CandidateEntryPage() {
  const [token, setToken] = useState("");
  const normalizedToken = token.trim();

  return (
    <AppShell>
      <EmptyState
        title="Código de acesso obrigatório"
        description="Para abrir o teste, use o código de acesso enviado pelo convite. Cole aqui ou abra o link do e-mail."
        actions={
          <div className="grid gap-2 sm:grid-cols-[minmax(0,1fr)_auto_auto]">
            <input
              className="input"
              placeholder="Código de acesso"
              value={token}
              onChange={(event) => setToken(event.target.value)}
            />
            <Link
              to="/candidato/$token"
              params={{ token: normalizedToken || "_" }}
              className={`rounded-md bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90 ${
                !normalizedToken ? "pointer-events-none opacity-50" : ""
              }`}
            >
              Abrir teste
            </Link>
            <Link
              to="/"
              className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
            >
              Voltar ao painel
            </Link>
          </div>
        }
      />
    </AppShell>
  );
}

function CandidateMedia({
  mediaUrl,
  mediaType,
}: {
  mediaUrl: string;
  mediaType: MediaType | null;
}) {
  if (mediaType === "AUDIO") {
    return (
      <audio
        controls
        src={mediaUrl}
        className="w-full"
        onClick={(event) => event.stopPropagation()}
      >
        Seu navegador não suporta áudio.
      </audio>
    );
  }
  return (
    <img
      src={mediaUrl}
      alt="Mídia do atendimento"
      className="max-h-48 w-auto rounded-md border border-border object-contain"
    />
  );
}

function FocusedCandidateExperience({ token }: { token: string }) {
  const [liveAttempt, setLiveAttempt] = useState<CandidateAttemptResponse | null>(null);
  const [remaining, setRemaining] = useState(30);
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);
  const [highContrast, setHighContrast] = useState(false);
  const [largeText, setLargeText] = useState(false);
  const [dyslexiaFont, setDyslexiaFont] = useState(false);
  const [submittingAnswer, setSubmittingAnswer] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [healthConsentGiven, setHealthConsentGiven] = useState(false);

  const attemptQuery = useQuery({
    queryKey: ["candidate-attempt", token],
    queryFn: () => getCandidateAttempt(token),
  });

  useEffect(() => {
    if (attemptQuery.data) {
      setLiveAttempt(attemptQuery.data);
    }
  }, [attemptQuery.data]);

  const attempt = liveAttempt ?? attemptQuery.data;
  const currentNode = attempt?.etapaAtual ?? null;
  const finished = Boolean(attempt && (attempt.finalizado || !currentNode));
  // Na vertical de saúde, o participante precisa consentir o tratamento de dado sensível
  // antes de iniciar; enquanto isso, o cronômetro fica pausado para não autoenviar respostas.
  const needsHealthConsent = Boolean(attempt?.verticalSaude) && !healthConsentGiven && !finished;
  const timeLimit = Math.max(
    1,
    currentNode?.tempoLimiteSegundosAcomodado ?? currentNode?.tempoLimiteSegundos ?? 30,
  );
  const timePercentage = finished ? 0 : Math.max(0, Math.min(100, (remaining / timeLimit) * 100));
  const currentStep = attempt?.progresso?.passoAtual ?? currentNode?.numero ?? 1;
  const totalSteps = Math.max(currentStep, attempt?.progresso?.passosEstimados ?? currentStep);
  const selectedOption = currentNode?.alternativas.find((option) => option.id === selectedOptionId);

  const submitAnswer = useCallback(
    async (node: CandidateNodeResponse, optionId: string | null, timedOut: boolean) => {
      if (!attempt) return;
      setSubmittingAnswer(true);
      setSubmitError(null);

      try {
        const response = await submitCandidateAnswer(token, {
          etapaId: node.id,
          etapaNumero: node.numero,
          respostaId: optionId,
          respondidaEm: new Date().toISOString(),
          tempoEsgotado: timedOut,
        });
        setLiveAttempt({
          participacaoId: response.participacaoId,
          avaliacaoNome: attempt.avaliacaoNome,
          status: response.status,
          finalizado: response.finalizado,
          acaoSugeridaFrontend: response.finalizado
            ? "VER_RESULTADOS"
            : attempt.acaoSugeridaFrontend,
          progresso: response.progresso,
          etapaAtual: response.etapaAtual,
        });
        setSelectedOptionId(null);
        void attemptQuery.refetch();
      } catch (error) {
        setSubmitError(
          friendlyApiErrorMessage(
            error,
            SUBMIT_ERROR_MESSAGES,
            "Tivemos um problema ao salvar. Tente novamente.",
          ),
        );
        setSelectedOptionId(null);
      } finally {
        setSubmittingAnswer(false);
      }
    },
    [attempt, attemptQuery, token],
  );

  useEffect(() => {
    setRemaining(timeLimit);
    setSelectedOptionId(null);
  }, [currentNode?.numero, timeLimit]);

  useEffect(() => {
    if (finished || submittingAnswer || !currentNode || needsHealthConsent) return;
    const id = window.setInterval(() => {
      setRemaining((value) => Math.max(0, value - 1));
    }, 1000);
    return () => window.clearInterval(id);
  }, [submittingAnswer, currentNode, finished, needsHealthConsent]);

  useEffect(() => {
    if (
      remaining === 0 &&
      !finished &&
      currentNode &&
      !selectedOptionId &&
      !submittingAnswer &&
      !needsHealthConsent
    ) {
      void submitAnswer(currentNode, null, true);
    }
  }, [
    currentNode,
    finished,
    remaining,
    selectedOptionId,
    submitAnswer,
    submittingAnswer,
    needsHealthConsent,
  ]);

  const pageClass = cn(
    "min-h-screen px-4 py-5 transition-colors sm:px-6",
    highContrast ? "bg-black text-white" : "bg-slate-50 text-slate-950",
    largeText ? "text-lg" : "text-base",
  );
  const fontStyle = {
    fontFamily: dyslexiaFont
      ? "'OpenDyslexic', 'Atkinson Hyperlegible', Verdana, Arial, sans-serif"
      : undefined,
  } satisfies CSSProperties;
  const panelClass = cn(
    "mx-auto flex min-h-[calc(100vh-2.5rem)] w-full max-w-3xl flex-col",
    "rounded-md border p-4 shadow-sm sm:p-6",
    highContrast ? "border-white bg-black" : "border-slate-200 bg-white",
  );
  const controlClass = cn(
    "inline-flex h-10 items-center gap-2 rounded-md border px-3 text-sm font-medium transition-colors",
    highContrast
      ? "border-white bg-black text-white hover:bg-white hover:text-black"
      : "border-slate-300 bg-white text-slate-900 hover:bg-slate-100",
  );
  const optionClass = cn(
    "w-full rounded-md border-2 p-4 text-left leading-relaxed transition-colors disabled:cursor-not-allowed disabled:opacity-60",
    highContrast
      ? "border-white bg-black text-white hover:bg-white hover:text-black"
      : "border-slate-200 bg-white text-slate-950 hover:border-blue-600 hover:bg-blue-50",
  );
  const selectedOptionClass = highContrast
    ? "border-yellow-300 bg-white text-black"
    : "border-blue-700 bg-blue-50 ring-2 ring-blue-700/20";
  const primaryButtonClass = cn(
    "inline-flex w-full items-center justify-center rounded-md px-5 py-3 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto",
    highContrast
      ? "border border-white bg-white text-black hover:bg-zinc-200"
      : "bg-blue-700 text-white hover:bg-blue-800",
  );

  return (
    <main className={pageClass} style={fontStyle}>
      <section className={panelClass}>
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div className="text-sm font-semibold uppercase tracking-wide opacity-70">
            Acessibilidade
          </div>
          <div className="flex flex-wrap justify-end gap-2">
            <button
              type="button"
              className={controlClass}
              onClick={() => setHighContrast((value) => !value)}
              aria-pressed={highContrast}
              title="Alterna para alto contraste"
            >
              Alto contraste
            </button>
            <button
              type="button"
              className={controlClass}
              onClick={() => setLargeText((value) => !value)}
              aria-pressed={largeText}
              title="Aumenta o tamanho do texto"
            >
              Texto {largeText ? "normal" : "maior"}
            </button>
            <button
              type="button"
              className={controlClass}
              onClick={() => setDyslexiaFont((value) => !value)}
              aria-pressed={dyslexiaFont}
              title="Troca para uma fonte mais fácil de ler"
            >
              Fonte para dislexia
            </button>
          </div>
        </div>

        {currentNode && !finished && !needsHealthConsent && (
          <div className="mb-8 space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2 text-sm font-medium">
              <span>
                Etapa {currentStep} de {totalSteps}
              </span>
              <span className="tabular-nums">{remaining}s restantes</span>
            </div>
            <div
              className={cn(
                "h-2 w-full overflow-hidden rounded-full",
                highContrast ? "bg-zinc-800" : "bg-slate-200",
              )}
            >
              <div
                className={cn(
                  "h-full rounded-full transition-all duration-1000 ease-linear",
                  timePercentage > 35 ? "bg-blue-600" : "bg-amber-500",
                )}
                style={{ width: `${timePercentage}%` }}
              />
            </div>
          </div>
        )}

        {attemptQuery.isLoading ? (
          <div className="flex flex-1 flex-col justify-center">
            <div className="text-sm font-medium uppercase tracking-wide opacity-70">Carregando</div>
            <h1 className="mt-3 text-3xl font-semibold">Preparando seu teste.</h1>
          </div>
        ) : attemptQuery.isError && !attempt ? (
          <div className="flex flex-1 flex-col justify-center">
            <div className="text-sm font-medium uppercase tracking-wide text-red-600">
              Acesso indisponível
            </div>
            <h1 className="mt-3 text-3xl font-semibold">Não foi possível carregar o teste.</h1>
            <p className="mt-3 max-w-xl opacity-80">
              {friendlyApiErrorMessage(
                attemptQuery.error,
                LOAD_ERROR_MESSAGES,
                "Verifique o link recebido e tente novamente.",
              )}
            </p>
          </div>
        ) : needsHealthConsent ? (
          <HealthConsentGate
            token={token}
            highContrast={highContrast}
            onConsented={() => setHealthConsentGiven(true)}
          />
        ) : currentNode && !finished ? (
          <div className="flex flex-1 flex-col justify-center">
            <div className="mx-auto w-full max-w-2xl">
              <div className="mb-6 text-center">
                <div className="text-sm font-medium uppercase tracking-wide opacity-70">
                  {attempt?.avaliacaoNome ?? "Teste"}
                </div>
                <h1 className="mt-3 text-2xl font-semibold sm:text-3xl">{currentNode.pessoa}</h1>
                <p className="mt-3 text-sm font-medium opacity-75">
                  Escolha uma alternativa. Você pode trocar antes de confirmar; depois de confirmar,
                  a resposta é final.
                </p>
              </div>

              <div className="space-y-4 text-center">
                <p
                  className="text-pretty text-xl leading-relaxed sm:text-2xl"
                  aria-label={currentNode.descricaoAcessivel || currentNode.descricao}
                >
                  {currentNode.descricao}
                </p>
                {currentNode.audioDescricaoUrl && (
                  <audio
                    controls
                    src={currentNode.audioDescricaoUrl}
                    className="mx-auto w-full max-w-md"
                  >
                    Seu navegador não suporta áudio.
                  </audio>
                )}
                {currentNode.midiaUrl && (
                  <div className="flex justify-center">
                    <CandidateMedia
                      mediaUrl={currentNode.midiaUrl}
                      mediaType={currentNode.tipoMidia ?? null}
                    />
                  </div>
                )}
              </div>

              <div className="mt-8 grid gap-3">
                {currentNode.alternativas.map((option) => (
                  <div key={option.id} className="space-y-2">
                    <button
                      type="button"
                      onClick={() => {
                        setSubmitError(null);
                        setSelectedOptionId(option.id);
                      }}
                      disabled={submittingAnswer}
                      className={cn(
                        optionClass,
                        selectedOptionId === option.id && selectedOptionClass,
                      )}
                      aria-label={option.descricaoAcessivel || option.texto}
                      aria-pressed={selectedOptionId === option.id}
                    >
                      {option.texto}
                    </button>
                    {option.audioDescricaoUrl && (
                      <audio controls src={option.audioDescricaoUrl} className="w-full">
                        Seu navegador não suporta áudio.
                      </audio>
                    )}
                    {option.midiaUrl && (
                      <CandidateMedia
                        mediaUrl={option.midiaUrl}
                        mediaType={option.tipoMidia ?? null}
                      />
                    )}
                  </div>
                ))}
              </div>

              {selectedOption && (
                <div className="mt-5 space-y-3 text-center" aria-live="polite">
                  <div className="text-sm font-medium opacity-75">
                    Alternativa selecionada. Confirme para enviar sua resposta final.
                  </div>
                  <button
                    type="button"
                    className={primaryButtonClass}
                    onClick={() => submitAnswer(currentNode, selectedOption.id, false)}
                    disabled={submittingAnswer}
                  >
                    {submittingAnswer ? "Registrando resposta..." : "Confirmar resposta final"}
                  </button>
                </div>
              )}
              {submitError && (
                <div
                  className="mt-5 text-center text-sm font-medium text-red-600"
                  aria-live="assertive"
                >
                  {submitError}
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className="flex flex-1 flex-col justify-center text-center">
            <div className="text-sm font-medium uppercase tracking-wide text-emerald-700">
              Participação finalizada
            </div>
            <h1 className="mt-3 text-3xl font-semibold">Obrigado por participar.</h1>
            <p className="mx-auto mt-3 max-w-xl opacity-80">
              O resultado será processado e entregue para a equipe responsável.
            </p>
            <p className="mx-auto mt-3 max-w-xl text-sm opacity-70">
              Esta avaliação mede como você age em uma situação de trabalho, por competência. Ela é
              apoio à decisão: quem decide sobre a sua candidatura é uma pessoa, não um sistema
              automático. Você pode pedir que uma pessoa revise o resultado.
            </p>
            <HumanReviewRequest attemptId={token} highContrast={highContrast} />
          </div>
        )}

        <p className="mt-6 border-t border-current pt-4 text-center text-xs leading-relaxed opacity-50">
          {attempt?.verticalSaude
            ? "Esta é uma atividade educativa. Seus dados são tratados para esta finalidade, conforme a LGPD e a política de privacidade da empresa responsável. Não é diagnóstico nem substitui avaliação profissional. A decisão é de uma pessoa, não de um sistema automático, e você pode pedir revisão humana."
            : "Seus dados são tratados para fins desta avaliação, conforme a LGPD e a política de privacidade da empresa responsável. A pontuação segue critérios definidos antes do teste, sem IA julgando você. A decisão sobre a sua candidatura é tomada por uma pessoa, não por um sistema automático, e você pode solicitar revisão humana do resultado."}
        </p>
      </section>
    </main>
  );
}

function HumanReviewRequest({
  attemptId,
  highContrast,
}: {
  attemptId: string;
  highContrast: boolean;
}) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const mutation = useMutation({
    mutationFn: () => requestHumanReview(attemptId, reason),
  });

  const fieldClass = cn(
    "w-full resize-none rounded-md border p-2.5 text-sm",
    highContrast ? "border-zinc-600 bg-zinc-900 text-zinc-100" : "border-slate-300 bg-white",
  );
  const buttonClass = cn(
    "rounded-md px-4 py-2 text-sm font-medium",
    highContrast ? "bg-zinc-100 text-zinc-900" : "bg-blue-600 text-white",
    "disabled:cursor-not-allowed disabled:opacity-50",
  );

  if (mutation.isSuccess) {
    return (
      <p className="mx-auto mt-5 max-w-xl text-sm font-medium text-emerald-700" aria-live="polite">
        Pedido de revisão registrado. Uma pessoa da equipe responsável vai analisar.
      </p>
    );
  }

  return (
    <div className="mx-auto mt-5 w-full max-w-md">
      {!open ? (
        <button
          type="button"
          className="text-sm font-medium underline"
          onClick={() => setOpen(true)}
        >
          Solicitar revisão humana
        </button>
      ) : (
        <div className="space-y-2 text-left">
          <label className="block text-sm font-medium">Solicitar revisão humana</label>
          <textarea
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            rows={3}
            maxLength={1000}
            placeholder="Se quiser, conte por que (opcional)."
            className={fieldClass}
          />
          <button
            type="button"
            className={buttonClass}
            disabled={mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? "Enviando..." : "Enviar pedido"}
          </button>
          {mutation.isError && (
            <p className="text-sm font-medium text-red-600" aria-live="assertive">
              Não consegui registrar agora. Tente novamente em instantes.
            </p>
          )}
        </div>
      )}
    </div>
  );
}

function HealthConsentGate({
  token,
  highContrast,
  onConsented,
}: {
  token: string;
  highContrast: boolean;
  onConsented: () => void;
}) {
  const [agreed, setAgreed] = useState(false);
  const [onBehalfOfMinor, setOnBehalfOfMinor] = useState(false);
  const mutation = useMutation({
    mutationFn: () => recordHealthConsent(token, onBehalfOfMinor),
    onSuccess: onConsented,
  });

  const cardClass = cn(
    "mx-auto w-full max-w-2xl rounded-md border p-5 text-left sm:p-6",
    highContrast ? "border-white bg-black" : "border-slate-200 bg-white",
  );
  const checkboxRowClass = cn(
    "flex items-start gap-3 rounded-md border p-3",
    highContrast ? "border-zinc-600" : "border-slate-200",
  );
  const buttonClass = cn(
    "inline-flex w-full items-center justify-center rounded-md px-5 py-3 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto",
    highContrast ? "bg-white text-black hover:bg-zinc-200" : "bg-blue-700 text-white hover:bg-blue-800",
  );

  return (
    <div className="flex flex-1 flex-col justify-center">
      <div className={cardClass}>
        <div className="text-sm font-medium uppercase tracking-wide opacity-70">Antes de começar</div>
        <h1 className="mt-2 text-2xl font-semibold">Uso dos seus dados nesta atividade</h1>
        <div className="mt-4 space-y-3 text-sm leading-relaxed opacity-90">
          <p>
            Esta atividade é um <strong>exercício educativo de tomada de decisão</strong>. Ela
            apresenta situações do dia a dia para você praticar escolhas.{" "}
            <strong>Não é uma consulta, não é diagnóstico e não substitui a orientação de um
            profissional de saúde.</strong>
          </p>
          <p>
            Para realizar a atividade, a empresa responsável vai tratar respostas suas que podem
            revelar informações relacionadas à sua saúde ou aos seus hábitos. Esses dados serão
            usados <strong>somente</strong> para gerar o resultado educativo desta atividade e para
            as finalidades descritas na política de privacidade da empresa responsável.
          </p>
          <ul className="list-disc space-y-1 pl-5">
            <li>A pontuação segue critérios definidos antes da atividade. Não há IA julgando você.</li>
            <li>
              Seus dados não serão usados para decidir, sozinhos e de forma automatizada, sobre
              tratamento, atendimento ou acesso a serviços.
            </li>
            <li>Você pode pedir que uma pessoa revise o resultado.</li>
            <li>
              Você pode acessar, corrigir ou excluir seus dados e revogar este consentimento a
              qualquer momento, pelo canal indicado pela empresa responsável. A revogação não afeta
              atividades já realizadas.
            </li>
          </ul>
        </div>

        <div className="mt-5 space-y-2">
          <label className={checkboxRowClass}>
            <input
              type="checkbox"
              className="mt-0.5 h-4 w-4"
              checked={agreed}
              onChange={(event) => setAgreed(event.target.checked)}
            />
            <span className="text-sm">
              Li e concordo que a empresa responsável trate os dados sensíveis de saúde informados
              por mim nesta atividade, exclusivamente para as finalidades educativas acima.
            </span>
          </label>
          <label className={checkboxRowClass}>
            <input
              type="checkbox"
              className="mt-0.5 h-4 w-4"
              checked={onBehalfOfMinor}
              onChange={(event) => setOnBehalfOfMinor(event.target.checked)}
            />
            <span className="text-sm">
              Estou concordando como responsável legal pela pessoa sob minha responsabilidade.
            </span>
          </label>
        </div>

        <div className="mt-5 flex flex-col items-start gap-2">
          <button
            type="button"
            className={buttonClass}
            disabled={!agreed || mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? "Registrando consentimento..." : "Concordar e continuar"}
          </button>
          {mutation.isError && (
            <p className="text-sm font-medium text-red-600" aria-live="assertive">
              Não consegui registrar o consentimento agora. Tente novamente em instantes.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

export function CandidateExperience({ token }: { token: string }) {
  return <FocusedCandidateExperience token={token} />;
}
