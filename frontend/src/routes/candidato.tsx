import { createFileRoute, Link, Outlet, useChildMatches } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useState, type CSSProperties } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState } from "@/components/praxis-ui";
import {
  getCandidateAttempt,
  submitCandidateAnswer,
  type CandidateAttemptResponse,
  type CandidateNodeResponse,
  type MediaType,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/candidato")({
  head: () => ({
    meta: [
      { title: "Avaliação do Candidato - Praxis" },
      {
        name: "description",
        content: "Experiência mobile-first com timer, respostas claras, retomada e acessibilidade.",
      },
    ],
  }),
  component: CandidateRouteLayout,
});

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
        description="Para abrir a avaliação, use o código de acesso enviado pelo convite. Cole aqui ou abra o link do e-mail."
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
              Abrir avaliação
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
  const [selected, setSelected] = useState<string | null>(null);
  const [highContrast, setHighContrast] = useState(false);
  const [largeText, setLargeText] = useState(false);
  const [dyslexiaFont, setDyslexiaFont] = useState(false);
  const [submittingAnswer, setSubmittingAnswer] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

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
  const timeLimit = Math.max(
    1,
    currentNode?.tempoLimiteSegundosAcomodado ?? currentNode?.tempoLimiteSegundos ?? 30,
  );
  const timePercentage = finished ? 0 : Math.max(0, Math.min(100, (remaining / timeLimit) * 100));

  useEffect(() => {
    setRemaining(timeLimit);
    setSelected(null);
  }, [currentNode?.numero, timeLimit]);

  useEffect(() => {
    if (finished || submittingAnswer || !currentNode) return;
    const id = window.setInterval(() => {
      setRemaining((value) => Math.max(0, value - 1));
    }, 1000);
    return () => window.clearInterval(id);
  }, [submittingAnswer, currentNode, finished]);

  useEffect(() => {
    if (remaining === 0 && !finished && currentNode && !selected && !submittingAnswer) {
      setSelected("Sem resposta nesta etapa");
      void submitAnswer(currentNode, null, true);
    }
  }, [currentNode, finished, remaining, selected, submittingAnswer]);

  async function submitAnswer(
    node: CandidateNodeResponse,
    optionId: string | null,
    timedOut: boolean,
  ) {
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
        acaoSugeridaFrontend: response.finalizado ? "VER_RESULTADOS" : attempt.acaoSugeridaFrontend,
        progresso: response.progresso,
        etapaAtual: response.etapaAtual,
      });
      setSelected(null);
      void attemptQuery.refetch();
    } catch (error) {
      setSubmitError(
        error instanceof Error ? error.message : "Nao foi possivel registrar a resposta.",
      );
      setSelected(null);
    } finally {
      setSubmittingAnswer(false);
    }
  }

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

  return (
    <main className={pageClass} style={fontStyle}>
      <section className={panelClass}>
        <div className="mb-5 flex flex-wrap items-center justify-end gap-2">
          <button
            type="button"
            className={controlClass}
            onClick={() => setHighContrast((value) => !value)}
          >
            Contraste
          </button>
          <button
            type="button"
            className={controlClass}
            onClick={() => setLargeText((value) => !value)}
          >
            A{largeText ? "-" : "+"}
          </button>
          <button
            type="button"
            className={controlClass}
            onClick={() => setDyslexiaFont((value) => !value)}
          >
            Leitura
          </button>
        </div>

        {currentNode && !finished && (
          <div className="mb-8 space-y-3">
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
            <h1 className="mt-3 text-3xl font-semibold">Preparando sua avaliação.</h1>
          </div>
        ) : attemptQuery.isError && !attempt ? (
          <div className="flex flex-1 flex-col justify-center">
            <div className="text-sm font-medium uppercase tracking-wide text-red-600">
              Acesso indisponível
            </div>
            <h1 className="mt-3 text-3xl font-semibold">Não foi possível carregar a avaliação.</h1>
            <p className="mt-3 max-w-xl opacity-80">
              {attemptQuery.error instanceof Error
                ? attemptQuery.error.message
                : "Verifique o link recebido e tente novamente."}
            </p>
          </div>
        ) : currentNode && !finished ? (
          <div className="flex flex-1 flex-col justify-center">
            <div className="mx-auto w-full max-w-2xl">
              <div className="mb-6 text-center">
                <div className="text-sm font-medium uppercase tracking-wide opacity-70">
                  {attempt?.avaliacaoNome ?? "Avaliação"}
                </div>
                <h1 className="mt-3 text-2xl font-semibold sm:text-3xl">{currentNode.pessoa}</h1>
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
                        setSelected(option.texto);
                        void submitAnswer(currentNode, option.id, false);
                      }}
                      disabled={submittingAnswer || selected !== null}
                      className={optionClass}
                      aria-label={option.descricaoAcessivel || option.texto}
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

              {selected && (
                <div className="mt-5 text-center text-sm font-medium opacity-75" aria-live="polite">
                  {submittingAnswer ? "Registrando resposta..." : "Resposta registrada."}
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
          </div>
        )}
      </section>
    </main>
  );
}

export function CandidateExperience({ token }: { token: string }) {
  return <FocusedCandidateExperience token={token} />;
}
