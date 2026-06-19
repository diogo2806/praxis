import { createFileRoute, Link, Outlet, useChildMatches } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type CSSProperties } from "react";
import {
  ExternalLink,
  Pause,
  Play,
  RotateCcw,
  Share2,
  ShieldCheck,
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
  submitCandidateAnswer,
  type CandidateAttemptResponse,
  type CandidateLinkResponse,
  type CandidateNodeResponse,
  type MediaType,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { getApiBaseUrl } from "@/lib/runtime-config";
import { cn } from "@/lib/utils";
import { useViewMode } from "@/lib/view-mode";

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
        <div className="grid gap-3 lg:grid-cols-[minmax(180px,1fr)_minmax(180px,1fr)_minmax(220px,1fr)_auto]">
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
    }) => submitCandidateAnswer(token, { respostaId: optionId, tempoEsgotado: timedOut }),
    onSuccess: (response) => {
      setLiveAttempt({
        participacaoId: response.participacaoId,
        avaliacaoNome: liveAttempt?.avaliacaoNome ?? attemptQuery.data?.avaliacaoNome ?? "Praxis",
        status: response.status,
        finalizado: response.finalizado,
        acaoSugeridaFrontend: liveAttempt?.acaoSugeridaFrontend,
        progresso: response.progresso,
        etapaAtual: response.etapaAtual,
      });
      setSelected(null);
    },
  });

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
    if (finished || answerMutation.isPending || !currentNode) return;
    const id = window.setInterval(() => {
      setRemaining((value) => Math.max(0, value - 1));
    }, 1000);
    return () => window.clearInterval(id);
  }, [answerMutation.isPending, currentNode, finished]);

  useEffect(() => {
    if (remaining === 0 && !finished && currentNode && !selected && !answerMutation.isPending) {
      setSelected("Sem resposta nesta etapa");
      answerMutation.mutate({ node: currentNode, timedOut: true });
    }
  }, [answerMutation, currentNode, finished, remaining, selected]);

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
          <div className="mb-8" aria-hidden="true">
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
        ) : attemptQuery.isError ? (
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
                        answerMutation.mutate({
                          node: currentNode,
                          optionId: option.id,
                          timedOut: false,
                        });
                      }}
                      disabled={answerMutation.isPending || selected !== null}
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
                  {answerMutation.isPending ? "Registrando resposta..." : "Resposta registrada."}
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
  /*
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
    }) => submitCandidateAnswer(token, { respostaId: optionId, tempoEsgotado: timedOut }),
    onSuccess: (response) => {
      setLiveAttempt({
        participacaoId: response.participacaoId,
        avaliacaoNome:
          liveAttempt?.avaliacaoNome ?? attemptQuery.data?.avaliacaoNome ?? "Praxis",
        status: response.status,
        finalizado: response.finalizado,
        progresso: response.progresso,
        etapaAtual: response.etapaAtual,
      });
      setSelected(null);
      setPaused(false);
    },
  });

  const attempt = liveAttempt ?? attemptQuery.data;
  const currentNode = attempt?.etapaAtual ?? null;
  const progress = attempt?.progresso ?? null;
  const finished = Boolean(attempt && (attempt.finalizado || !currentNode));
  const showTurn = Boolean(currentNode && !finished);
  const timeLimit = currentNode?.tempoLimiteSegundos ?? 30;

  useEffect(() => {
    setRemaining(timeLimit);
    setSelected(null);
  }, [currentNode?.numero, timeLimit]);

  useEffect(() => {
    if (paused || finished || answerMutation.isPending) return;
    const id = window.setInterval(() => {
      setRemaining((value) => Math.max(0, value - 1));
    }, 1000);
    return () => window.clearInterval(id);
  }, [answerMutation.isPending, paused, finished]);

  useEffect(() => {
    if (remaining === 0 && !finished && currentNode && !selected) {
      setSelected("Sem resposta nesta etapa");
      answerMutation.mutate({ node: currentNode, timedOut: true });
    }
  }, [answerMutation, currentNode, finished, remaining, selected]);

  const pct = remaining / timeLimit;
  const timerTone = pct <= 0.1 ? "bg-danger" : pct <= 0.3 ? "bg-warning" : "bg-primary";

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="link expirado ou participação abandonada fora da janela" />
      <div className="mb-5">
        <div className="text-xs uppercase text-primary">Avaliação do candidato</div>
        <h1 className="mt-1 text-3xl font-semibold">
          {attempt?.avaliacaoNome ?? "Avaliação"}
        </h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Uma situação por vez, timer claro, respostas objetivas e retomada após queda.
        </p>
      </div>

      {attemptQuery.isLoading && (
        <StateBanner tone="info" title="Carregando participação">
          Buscando a avaliação e a etapa atual.
        </StateBanner>
      )}

      {attemptQuery.isError && (
        <StateBanner tone="danger" title="Não foi possível carregar a participação">
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
          A resposta salva na última etapa será retomada automaticamente quando a conexão voltar.
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
                  {progress && (
                    <div className="mb-4 rounded-md border border-border bg-card p-3">
                      <div className="mb-2 flex items-center justify-between gap-3 text-xs">
                        <span className="font-medium text-foreground" aria-live="polite">
                          Passo {progress.passoAtual} de aproximadamente{" "}
                          {progress.passosEstimados}
                        </span>
                        <span className="text-muted-foreground">{progress.percentual}%</span>
                      </div>
                      <div
                        className="praxis-progress-track praxis-progress-track-sm"
                        aria-label={`Passo ${progress.passoAtual} de aproximadamente ${progress.passosEstimados}`}
                        aria-valuemin={0}
                        aria-valuemax={100}
                        aria-valuenow={progress.percentual}
                        role="progressbar"
                      >
                        <div
                          className="praxis-progress-fill bg-success transition-all duration-700 ease-out"
                          style={
                            {
                              "--praxis-progress-value": `${progress.percentual}%`,
                            } as CSSProperties
                          }
                        />
                      </div>
                    </div>
                  )}
                  <div className="mb-4">
                    <div className="mb-1 flex items-center justify-between text-xs">
                      <span aria-live="polite">{remaining}s restantes</span>
                      <span>{paused ? "pausado" : `etapa ${currentNode?.numero ?? 1}`}</span>
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
                      <div>{currentNode?.descricao}</div>
                      {currentNode?.midiaUrl && (
                        <CandidateMedia
                          mediaUrl={currentNode.midiaUrl}
                          mediaType={currentNode.tipoMidia ?? null}
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
                      {(currentNode?.alternativas ?? []).map((option) => (
                        <div
                          key={option.id}
                          className="space-y-2 rounded-md border border-border bg-card p-2 hover:border-primary"
                        >
                          <button
                            type="button"
                            onClick={() => {
                              setSelected(option.texto);
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
                            {option.texto}
                          </button>
                          {option.midiaUrl && (
                            <CandidateMedia
                              mediaUrl={option.midiaUrl}
                              mediaType={option.tipoMidia ?? null}
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
                  <h2 className="mt-2 text-2xl font-semibold">Preparando sua avaliação.</h2>
                  <p className="mt-2 text-sm text-muted-foreground">
                    Buscando a etapa atual e as respostas disponíveis.
                  </p>
                </div>
              ) : (
                <div className="flex min-h-[560px] flex-col justify-center">
                  <div className="text-xs uppercase text-success">Participação finalizada</div>
                  <h2 className="mt-2 text-2xl font-semibold">Obrigado por participar.</h2>
                  <p className="mt-2 text-sm text-muted-foreground">
                    O resultado será processado e entregue para a equipe responsável.
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
                  Reiniciar etapa
                </button>
              </div>
            </div>
          )}

          <StateBanner tone="info" title="Tempo esgotado encerra a etapa">
            Quando chega a zero, registra sem resposta e segue o fluxo configurado.
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
  */
}
