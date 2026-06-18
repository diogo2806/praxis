import { createFileRoute, Link, Outlet, useChildMatches } from "@tanstack/react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect, useState, type CSSProperties } from "react";
import { Pause, Play, RotateCcw, ShieldCheck, Wifi, WifiOff } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import {
  getCandidateAttempt,
  submitCandidateAnswer,
  type CandidateAttemptResponse,
  type CandidateNodeResponse,
  type MediaType,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";
import { useViewMode } from "@/lib/view-mode";

export const Route = createFileRoute("/candidato")({
  head: () => ({
    meta: [
      { title: "Visão do Candidato - Praxis" },
      {
        name: "description",
        content: "Experiência mobile-first com timer, chat, retomada e acessibilidade.",
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
        description="Para visualizar o teste do candidato, use o código de acesso enviado pelo convite. Cole aqui ou abra o link do e-mail."
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
              Abrir tentativa
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

export function CandidateExperience({ token }: { token: string }) {
  const [liveAttempt, setLiveAttempt] = useState<CandidateAttemptResponse | null>(null);
  const [remaining, setRemaining] = useState(30);
  const [paused, setPaused] = useState(false);
  const [offline, setOffline] = useState(false);
  const [selected, setSelected] = useState<string | null>(null);
  const technical = useViewMode() === "technical";

  const attemptQuery = useQuery({
    queryKey: ["candidate-attempt", token],
    queryFn: () => getCandidateAttempt(token),
  });

  useEffect(() => {
    if (attemptQuery.data) {
      setLiveAttempt(attemptQuery.data);
    }
  }, [attemptQuery.data]);

  const answerMutation = useMutation({
    mutationFn: ({
      node,
      optionId,
      timedOut,
    }: {
      node: CandidateNodeResponse;
      optionId?: string | null;
      timedOut: boolean;
    }) => submitCandidateAnswer(token, { nodeId: node.id, optionId, timedOut }),
    onSuccess: (response) => {
      setLiveAttempt({
        attemptId: response.attemptId,
        simulationName:
          liveAttempt?.simulationName ?? attemptQuery.data?.simulationName ?? "Praxis",
        status: response.status,
        completed: response.completed,
        currentNode: response.currentNode,
      });
      setSelected(null);
      setPaused(false);
    },
  });

  const attempt = liveAttempt ?? attemptQuery.data;
  const currentNode = attempt?.currentNode ?? null;
  const finished = Boolean(attempt && (attempt.completed || !currentNode));
  const showTurn = Boolean(currentNode && !finished);
  const timeLimit = currentNode?.timeLimitSeconds ?? 30;

  useEffect(() => {
    setRemaining(timeLimit);
    setSelected(null);
  }, [currentNode?.id, timeLimit]);

  useEffect(() => {
    if (paused || finished || answerMutation.isPending) return;
    const id = window.setInterval(() => {
      setRemaining((value) => Math.max(0, value - 1));
    }, 1000);
    return () => window.clearInterval(id);
  }, [answerMutation.isPending, paused, finished]);

  useEffect(() => {
    if (remaining === 0 && !finished && currentNode && !selected) {
      setSelected("Sem resposta neste turno");
      answerMutation.mutate({ node: currentNode, timedOut: true });
    }
  }, [answerMutation, currentNode, finished, remaining, selected]);

  const pct = remaining / timeLimit;
  const timerTone = pct <= 0.1 ? "bg-danger" : pct <= 0.3 ? "bg-warning" : "bg-primary";

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="link expirado ou tentativa abandonada fora da janela" />
      <div className="mb-5">
        <div className="text-xs uppercase text-primary">Visão do candidato</div>
        <h1 className="mt-1 text-3xl font-semibold">
          {attempt?.simulationName ?? "Simulação situacional"}
        </h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Chat estruturado, timer legível, navegação por teclado e retomada após queda.
        </p>
      </div>

      {attemptQuery.isLoading && (
        <StateBanner tone="info" title="Carregando tentativa">
          Buscando a simulação e o turno atual no sistema.
        </StateBanner>
      )}

      {attemptQuery.isError && (
        <StateBanner tone="danger" title="Não foi possível carregar a tentativa">
          {attemptQuery.error instanceof Error
            ? attemptQuery.error.message
            : "Verifique se o servidor está rodando e se o código de acesso é válido."}
        </StateBanner>
      )}

      {technical && (
        <StateBanner tone="info" title="Código de acesso carregado">
          Código visualizado apenas no modo técnico: <code>{token}</code>.
        </StateBanner>
      )}

      {offline && (
        <StateBanner tone="warn" title="Conexão perdida - reconectando">
          A resposta salva no último turno será retomada automaticamente quando a conexão voltar.
        </StateBanner>
      )}

      {answerMutation.isError && (
        <StateBanner tone="danger" title="Resposta não enviada">
          {answerMutation.error instanceof Error
            ? answerMutation.error.message
            : "Tente enviar a resposta novamente."}
        </StateBanner>
      )}

      <div className="mt-5 grid gap-5 lg:grid-cols-[360px_minmax(0,1fr)]">
        <section className="rounded-md border border-border bg-card p-5">
          <div className="mx-auto max-w-[320px] rounded-[30px] border border-border bg-foreground p-2">
            <div className="min-h-[620px] rounded-[24px] bg-background p-4">
              {showTurn ? (
                <>
                  <div className="mb-4">
                    <div className="mb-1 flex items-center justify-between text-xs">
                      <span aria-live="polite">{remaining}s restantes</span>
                      <span>{paused ? "pausado" : `turno ${currentNode?.turnIndex ?? 1}`}</span>
                    </div>
                    <div
                      className="praxis-progress-track praxis-progress-track-md"
                      aria-label={`${remaining} segundos restantes`}
                      role="timer"
                    >
                      <div
                        className={cn("praxis-progress-fill transition-all", timerTone)}
                        style={
                          {
                            "--praxis-progress-value": `${pct * 100}%`,
                          } as CSSProperties
                        }
                      />
                    </div>
                  </div>
                  <div className="space-y-3" aria-live="polite">
                    <div className="mr-8 space-y-2 rounded-md bg-muted px-3 py-2 text-sm">
                      <div>{currentNode?.message}</div>
                      {currentNode?.mediaUrl && (
                        <CandidateMedia
                          mediaUrl={currentNode.mediaUrl}
                          mediaType={currentNode.mediaType ?? null}
                        />
                      )}
                    </div>
                    {selected && (
                      <div className="ml-8 rounded-md bg-primary px-3 py-2 text-sm text-primary-foreground">
                        {selected}
                      </div>
                    )}
                    {selected && (
                      <div className="mr-16 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">
                        {answerMutation.isPending ? "enviando..." : "registrado"}
                      </div>
                    )}
                  </div>
                  {!selected && (
                    <div className="mt-5 space-y-2">
                      {(currentNode?.options ?? []).map((option) => (
                        <div
                          key={option.id}
                          className="space-y-2 rounded-md border border-border bg-card p-2 hover:border-primary"
                        >
                          <button
                            type="button"
                            onClick={() => {
                              setSelected(option.text);
                              if (currentNode) {
                                answerMutation.mutate({
                                  node: currentNode,
                                  optionId: option.id,
                                  timedOut: false,
                                });
                              }
                            }}
                            disabled={answerMutation.isPending}
                            className="w-full rounded-md p-1 text-left text-sm hover:bg-primary/5 disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            {option.text}
                          </button>
                          {option.mediaUrl && (
                            <CandidateMedia
                              mediaUrl={option.mediaUrl}
                              mediaType={option.mediaType ?? null}
                            />
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </>
              ) : attemptQuery.isLoading ? (
                <div className="flex min-h-[560px] flex-col justify-center">
                  <div className="text-xs uppercase text-primary">Carregando</div>
                  <h2 className="mt-2 text-2xl font-semibold">Preparando sua simulação.</h2>
                  <p className="mt-2 text-sm text-muted-foreground">
                    Buscando o turno atual e as alternativas disponíveis.
                  </p>
                </div>
              ) : (
                <div className="flex min-h-[560px] flex-col justify-center">
                  <div className="text-xs uppercase text-success">Tentativa finalizada</div>
                  <h2 className="mt-2 text-2xl font-semibold">Obrigado por participar.</h2>
                  <p className="mt-2 text-sm text-muted-foreground">
                    O resultado será processado pelo sistema e entregue para o fluxo configurado.
                  </p>
                </div>
              )}
            </div>
          </div>
        </section>

        <aside className="space-y-4">
          <div className="rounded-md border border-border bg-card p-5">
            <div className="mb-3 flex items-center gap-2 text-sm font-semibold">
              <ShieldCheck className="h-4 w-4 text-primary" />
              Recursos de acessibilidade
            </div>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>Compatível com leitor de tela</li>
              <li>Alto contraste</li>
              <li>Tempo estendido quando configurado</li>
              <li>Navegação por teclado</li>
            </ul>
          </div>

          {technical && (
            <div className="rounded-md border border-border bg-card p-5">
              <h2 className="text-sm font-semibold">Controles de estado</h2>
              <div className="mt-4 flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => setPaused((value) => !value)}
                  className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
                >
                  {paused ? <Play className="h-4 w-4" /> : <Pause className="h-4 w-4" />}
                  {paused ? "Retomar" : "Pausar"}
                </button>
                <button
                  type="button"
                  onClick={() => setOffline((value) => !value)}
                  className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
                >
                  {offline ? <Wifi className="h-4 w-4" /> : <WifiOff className="h-4 w-4" />}
                  {offline ? "Reconectar" : "Simular queda"}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setRemaining(timeLimit);
                    setSelected(null);
                    setPaused(false);
                  }}
                  className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
                >
                  <RotateCcw className="h-4 w-4" />
                  Reiniciar turno
                </button>
              </div>
            </div>
          )}

          <StateBanner tone="info" title="Tempo esgotado encerra só o turno">
            Quando chega a zero, registra sem resposta e avança. A simulação inteira não fecha.
          </StateBanner>
        </aside>
      </div>
      <div className="mt-6">
        <Link
          to="/"
          className="inline-flex rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar ao painel
        </Link>
      </div>
    </AppShell>
  );
}
