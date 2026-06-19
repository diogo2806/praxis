import { createFileRoute, Link, Outlet, useChildMatches } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type CSSProperties } from "react";
import {
  ExternalLink,
  Share2,
  Wifi,
  WifiOff,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  createCandidateLink,
  getCandidateAttempt,
  listCandidateLinks,
  listSimulations,
  type CandidateAttemptResponse,
  type CandidateLinkResponse,
  type CandidateNodeResponse,
  type MediaType,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import {
  buildOptimisticAttempt,
  enqueueCandidateAnswer,
  flushCandidateAnswers,
  getPendingCandidateAnswerCount,
  loadCandidateOfflineState,
  saveCandidateAttemptSnapshot,
} from "@/lib/candidate-offline";
import { getApiBaseUrl } from "@/lib/runtime-config";
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
  return <CandidateLinksPage />;
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

function CandidateLinksPage() {
  const queryClient = useQueryClient();
  const [candidateName, setCandidateName] = useState("");
  const [candidateEmail, setCandidateEmail] = useState("");
  const [selectedSimulationId, setSelectedSimulationId] = useState("");
  const [timeMultiplier, setTimeMultiplier] = useState("1");

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });
  const candidateLinksQuery = useQuery({
    queryKey: ["candidate-links"],
    queryFn: listCandidateLinks,
    retry: false,
  });

  const publishedSimulations = (simulationsQuery.data ?? []).filter(
    (simulation) => simulation.status === "published",
  );
  const selectedSimulation =
    publishedSimulations.find((simulation) => simulation.id === selectedSimulationId) ??
    publishedSimulations[0] ??
    null;
  const canGenerate =
    Boolean(selectedSimulation) && candidateName.trim().length > 0 && candidateEmail.includes("@");

  useEffect(() => {
    if (!selectedSimulationId && publishedSimulations.length > 0) {
      setSelectedSimulationId(publishedSimulations[0].id);
    }
  }, [publishedSimulations, selectedSimulationId]);

  const linkMutation = useMutation({
    mutationFn: createCandidateLink,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["candidate-links"] });
      setCandidateName("");
      setCandidateEmail("");
    },
  });

  function handleSubmit() {
    if (!canGenerate || !selectedSimulation) return;
    linkMutation.mutate({
      simulationId: selectedSimulation.id,
      candidateName: candidateName.trim(),
      candidateEmail: candidateEmail.trim(),
      accommodationTimeMultiplier: Number(timeMultiplier) || 1,
    });
  }

  async function handleShare(row: CandidateLinkResponse) {
    const candidateUrl = normalizeCandidatePageUrl(row.candidateUrl);
    const text = `Olá ${row.candidateName}, você foi convidado(a) para uma avaliação. Acesse: ${candidateUrl}`;
    if (navigator.share) {
      await navigator.share({
        title: row.simulationName,
        text,
        url: candidateUrl,
      });
      return;
    }
    await navigator.clipboard.writeText(text);
  }

  const rows = candidateLinksQuery.data ?? [];

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="sem avaliações no ar para gerar link" />

      <div className="mb-6">
        <div className="text-xs uppercase text-primary">Candidatos</div>
        <h1 className="mt-1 text-3xl font-semibold">Links de candidatos</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Gere o acesso e acompanhe em uma tabela com nome, email, link clicavel e compartilhamento.
        </p>
      </div>

      {simulationsQuery.isError && (
        <StateBanner tone="danger" title="Não foi possível carregar avaliações">
          {simulationsQuery.error instanceof Error
            ? simulationsQuery.error.message
            : "Verifique se o servidor está disponível."}
        </StateBanner>
      )}

      {candidateLinksQuery.isError && (
        <StateBanner tone="danger" title="Nao foi possivel carregar links de candidatos">
          {candidateLinksQuery.error instanceof Error
            ? candidateLinksQuery.error.message
            : "Verifique se o servidor está disponível."}
        </StateBanner>
      )}

      {linkMutation.isError && (
        <StateBanner tone="danger" title="Nao foi possivel gerar o link">
          {linkMutation.error instanceof Error
            ? linkMutation.error.message
            : "Verifique os dados do candidato e tente novamente."}
        </StateBanner>
      )}

      <section className="mb-6 rounded-md border border-border bg-card p-5">
        <div className="grid gap-3 lg:grid-cols-[minmax(180px,1fr)_minmax(180px,1fr)_minmax(220px,1fr)_140px_auto]">
          <label className="space-y-1 text-sm">
            <span className="font-medium">Nome</span>
            <input
              className="input"
              placeholder="Nome do candidato"
              value={candidateName}
              onChange={(event) => setCandidateName(event.target.value)}
            />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-medium">Email</span>
            <input
              className="input"
              placeholder="email@empresa.com"
              type="email"
              value={candidateEmail}
              onChange={(event) => setCandidateEmail(event.target.value)}
            />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-medium">Avaliação</span>
            <select
              className="input"
              value={selectedSimulation?.id ?? ""}
              onChange={(event) => setSelectedSimulationId(event.target.value)}
              disabled={simulationsQuery.isLoading || publishedSimulations.length === 0}
            >
              {publishedSimulations.map((simulation: SimulationSummaryResponse) => (
                <option key={simulation.id} value={simulation.id}>
                  {simulation.name}
                </option>
              ))}
            </select>
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-medium">Tempo</span>
            <select
              className="input"
              value={timeMultiplier}
              onChange={(event) => setTimeMultiplier(event.target.value)}
            >
              <option value="1">Padrao</option>
              <option value="1.5">1,5x</option>
              <option value="2">2x</option>
            </select>
          </label>
          <div className="flex items-end">
            <button
              type="button"
              onClick={handleSubmit}
              disabled={!canGenerate || linkMutation.isPending}
              className="inline-flex h-11 w-full items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50 lg:w-auto"
            >
              {linkMutation.isPending ? "Gerando..." : "Gerar link"}
            </button>
          </div>
        </div>
      </section>

      <section className="rounded-md border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nome</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Link</TableHead>
              <TableHead className="text-right">Acao</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {candidateLinksQuery.isLoading ? (
              <TableRow>
                <TableCell colSpan={4} className="py-8 text-center text-sm text-muted-foreground">
                  Carregando links de candidatos...
                </TableCell>
              </TableRow>
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={4} className="py-8 text-center text-sm text-muted-foreground">
                  Nenhum link gerado ainda.
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row) => (
                <TableRow key={row.attemptId}>
                  <TableCell className="font-medium">{row.candidateName}</TableCell>
                  <TableCell>{row.candidateEmail}</TableCell>
                  <TableCell className="max-w-[420px]">
                    <a
                      href={normalizeCandidatePageUrl(row.candidateUrl)}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex max-w-full items-center gap-2 text-primary hover:underline"
                    >
                      <span className="truncate">{normalizeCandidatePageUrl(row.candidateUrl)}</span>
                      <ExternalLink className="h-4 w-4 shrink-0" />
                    </a>
                  </TableCell>
                  <TableCell className="text-right">
                    <button
                      type="button"
                      onClick={() => void handleShare(row)}
                      className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-accent"
                    >
                      <Share2 className="h-4 w-4" />
                      Compartilhar
                    </button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </section>
    </AppShell>
  );
}

function normalizeCandidatePageUrl(candidateUrl: string) {
  if (typeof window === "undefined") {
    return candidateUrl;
  }

  try {
    const url = new URL(candidateUrl);
    const apiBaseUrl = getApiBaseUrl();
    const apiOrigin = apiBaseUrl ? new URL(apiBaseUrl, window.location.origin).origin : null;
    const apiPrefixedCurrentHost = `api-${window.location.hostname}`;
    const pointsToConfiguredApi = apiOrigin !== null && url.origin === apiOrigin;
    const pointsToApiSibling = url.hostname === apiPrefixedCurrentHost;

    if ((pointsToConfiguredApi || pointsToApiSibling) && url.pathname.startsWith("/candidato/")) {
      return `${window.location.origin}${url.pathname}${url.search}${url.hash}`;
    }
  } catch {
    return candidateUrl;
  }

  return candidateUrl;
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
  const [pendingAnswers, setPendingAnswers] = useState(0);
  const [syncing, setSyncing] = useState(false);
  const [submittingAnswer, setSubmittingAnswer] = useState(false);
  const [online, setOnline] = useState(() => (typeof navigator === "undefined" ? true : navigator.onLine));

  const attemptQuery = useQuery({
    queryKey: ["candidate-attempt", token],
    queryFn: () => getCandidateAttempt(token),
  });

  useEffect(() => {
    const cached = loadCandidateOfflineState(token);
    if (cached.attempt) {
      setLiveAttempt(cached.attempt);
    }
    setPendingAnswers(cached.pendingAnswers.length);
  }, [token]);

  useEffect(() => {
    if (attemptQuery.data) {
      setLiveAttempt(attemptQuery.data);
      saveCandidateAttemptSnapshot(token, attemptQuery.data);
    }
  }, [attemptQuery.data, token]);

  useEffect(() => {
    function handleOnline() {
      setOnline(true);
      void syncPendingAnswers();
    }

    function handleOffline() {
      setOnline(false);
    }

    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    navigator.serviceWorker?.addEventListener("message", handleServiceWorkerMessage);
    return () => {
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
      navigator.serviceWorker?.removeEventListener("message", handleServiceWorkerMessage);
    };

    function handleServiceWorkerMessage(event: MessageEvent) {
      if (event.data?.type === "PRAXIS_SYNC_CANDIDATE_ANSWERS") {
        void syncPendingAnswers();
      }
    }
  }, [token]);

  useEffect(() => {
    if (online && pendingAnswers > 0) {
      void syncPendingAnswers();
    }
  }, [online, pendingAnswers, token]);

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

  async function syncPendingAnswers() {
    if (syncing) return;
    setSyncing(true);
    try {
      const result = await flushCandidateAnswers(token);
      if (result.attempt) {
        setLiveAttempt(result.attempt);
      }
      setPendingAnswers(result.pending);
    } catch {
      setPendingAnswers(getPendingCandidateAnswerCount(token));
    } finally {
      setSyncing(false);
    }
  }

  async function submitAnswer(node: CandidateNodeResponse, optionId: string | null, timedOut: boolean) {
    if (!attempt) return;
    setSubmittingAnswer(true);
    enqueueCandidateAnswer(token, {
      etapaId: node.id,
      etapaNumero: node.numero,
      respostaId: optionId,
      respondidaEm: new Date().toISOString(),
      tempoEsgotado: timedOut,
    });
    setPendingAnswers(getPendingCandidateAnswerCount(token));

    const optimisticAttempt = buildOptimisticAttempt(attempt, node, optionId, timedOut);
    setLiveAttempt(optimisticAttempt);
    saveCandidateAttemptSnapshot(token, optimisticAttempt);
    setSelected(null);

    if (online) {
      await syncPendingAnswers();
    }
    setSubmittingAnswer(false);
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
          <button type="button" className={controlClass} onClick={() => setHighContrast((value) => !value)}>
            Contraste
          </button>
          <button type="button" className={controlClass} onClick={() => setLargeText((value) => !value)}>
            A{largeText ? "-" : "+"}
          </button>
          <button type="button" className={controlClass} onClick={() => setDyslexiaFont((value) => !value)}>
            Leitura
          </button>
        </div>

        {currentNode && !finished && (
          <div className="mb-8 space-y-3">
            <div className="flex items-center justify-between gap-3 text-sm" aria-live="polite">
              <span className={cn("inline-flex items-center gap-2 font-medium", online ? "text-emerald-700" : "text-amber-700")}>
                {online ? <Wifi className="h-4 w-4" /> : <WifiOff className="h-4 w-4" />}
                {online ? "Online" : "Modo offline"}
              </span>
              {pendingAnswers > 0 && (
                <span className="font-medium text-amber-700">
                  {syncing ? "Sincronizando..." : `${pendingAnswers} resposta(s) pendente(s)`}
                </span>
              )}
            </div>
            <div className={cn("h-2 w-full overflow-hidden rounded-full", highContrast ? "bg-zinc-800" : "bg-slate-200")}>
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
            <div className="text-sm font-medium uppercase tracking-wide text-red-600">Acesso indisponível</div>
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
                  <audio controls src={currentNode.audioDescricaoUrl} className="mx-auto w-full max-w-md">
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
