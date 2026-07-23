import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  AlertTriangle,
  CheckCircle2,
  GitBranch,
  PlayCircle,
  RotateCcw,
  ShieldCheck,
  TimerReset,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  getSimulationValidation,
  getSimulationVersion,
  type SimulationVersionNodeResponse,
} from "@/lib/api/praxis";
import {
  analyzePreviewGraph,
  calculatePreviewCoverage,
  previewOptionKey,
  type PreviewFlowProblem,
} from "@/lib/preview-analysis";
import {
  buildValidationDiagnostics,
  type ValidationDiagnostic,
} from "@/lib/validation-diagnostics";
import { cn } from "@/lib/utils";

const MAX_PREVIEW_STEPS = 50;

export const Route = createFileRoute("/nova/previa")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Prévia da jornada - Práxis" },
      {
        name: "description",
        content:
          "Execução interna da jornada e mapa de cobertura sem criar participação ou resultado oficial.",
      },
    ],
  }),
  component: CandidateJourneyPreviewPage,
});

function CandidateJourneyPreviewPage() {
  const search = Route.useSearch();
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const versionQuery = useQuery({
    queryKey: ["simulation-version-preview", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
    retry: false,
  });
  const validationQuery = useQuery({
    queryKey: ["simulation-validation-preview", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationValidation(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
    retry: false,
  });
  const version = versionQuery.data;
  const analysis = useMemo(() => (version ? analyzePreviewGraph(version) : null), [version]);
  const diagnostics = useMemo(
    () => buildValidationDiagnostics(version, validationQuery.data?.issues ?? []),
    [validationQuery.data?.issues, version],
  );
  const nodeById = useMemo(
    () => new Map((version?.nodes ?? []).map((node) => [node.id, node])),
    [version],
  );
  const [currentNodeId, setCurrentNodeId] = useState<string | null>(null);
  const [visitedNodeIds, setVisitedNodeIds] = useState<string[]>([]);
  const [visitedOptionKeys, setVisitedOptionKeys] = useState<string[]>([]);
  const [pathNodeIds, setPathNodeIds] = useState<string[]>([]);
  const [completed, setCompleted] = useState(false);
  const [completionMessage, setCompletionMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!version) return;
    const rootNodeId = version.blueprint.rootNodeId;
    setCurrentNodeId(rootNodeId || null);
    setVisitedNodeIds(rootNodeId ? [rootNodeId] : []);
    setVisitedOptionKeys([]);
    setPathNodeIds(rootNodeId ? [rootNodeId] : []);
    setCompleted(false);
    setCompletionMessage(null);
    recordPreviewTelemetry({
      event: "previewStarted",
      simulationId: version.simulationId,
      versionNumber: version.versionNumber,
      nodeId: rootNodeId || null,
    });
  }, [version]);

  const coverage = useMemo(
    () =>
      version && analysis
        ? calculatePreviewCoverage(version, analysis, visitedNodeIds, visitedOptionKeys)
        : null,
    [analysis, version, visitedNodeIds, visitedOptionKeys],
  );
  const currentNode = currentNodeId ? nodeById.get(currentNodeId) ?? null : null;
  const stepLimitReached = pathNodeIds.length >= MAX_PREVIEW_STEPS;

  function restartPreview() {
    if (!version) return;
    const rootNodeId = version.blueprint.rootNodeId;
    setCurrentNodeId(rootNodeId || null);
    setVisitedNodeIds(rootNodeId ? [rootNodeId] : []);
    setVisitedOptionKeys([]);
    setPathNodeIds(rootNodeId ? [rootNodeId] : []);
    setCompleted(false);
    setCompletionMessage(null);
    recordPreviewTelemetry({
      event: "previewRestarted",
      simulationId: version.simulationId,
      versionNumber: version.versionNumber,
      nodeId: rootNodeId || null,
    });
  }

  function selectOption(optionId: string) {
    if (!version || !currentNode || completed || stepLimitReached) return;
    const option = currentNode.options.find((candidate) => candidate.id === optionId);
    if (!option) return;
    setVisitedOptionKeys((current) => appendUnique(current, previewOptionKey(currentNode.id, option.id)));
    recordPreviewTelemetry({
      event: "optionSelected",
      simulationId: version.simulationId,
      versionNumber: version.versionNumber,
      nodeId: currentNode.id,
      optionId: option.id,
    });
    advancePreview(option.nextNodeId, "A alternativa encerrou a prévia.");
  }

  function simulateTimeout() {
    if (!version || !currentNode || completed || stepLimitReached) return;
    recordPreviewTelemetry({
      event: "timeoutSimulated",
      simulationId: version.simulationId,
      versionNumber: version.versionNumber,
      nodeId: currentNode.id,
    });
    advancePreview(currentNode.timeoutNextNodeId, "O tempo esgotado encerrou a prévia.");
  }

  function advancePreview(nextNodeId: string | null, terminalMessage: string) {
    if (!version || !currentNode) return;
    if (!nextNodeId) {
      setCompleted(true);
      setCompletionMessage(terminalMessage);
      return;
    }
    const nextNode = nodeById.get(nextNodeId);
    if (!nextNode) {
      setCompleted(true);
      setCompletionMessage("O caminho aponta para uma etapa inexistente.");
      return;
    }
    setCurrentNodeId(nextNode.id);
    setVisitedNodeIds((current) => appendUnique(current, nextNode.id));
    setPathNodeIds((current) => [...current, nextNode.id]);
    if (nextNode.isFinal) {
      setCompleted(true);
      setCompletionMessage(nextNode.reportText || "Etapa final alcançada na prévia.");
    }
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <WizardStepper current="revisao" unlockedThrough="revisao" />
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Teste interno da autoria
            </div>
            <h1 className="mt-1 font-display text-3xl">Prévia da jornada do candidato</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Percorra a versão como uma pessoa candidata e acompanhe a cobertura. Esta sessão não
              cria participação, não consome crédito e não gera resultado oficial.
            </p>
          </div>
          {hasContext && (
            <div className="flex flex-wrap gap-2">
              <Button asChild variant="outline">
                <Link
                  to="/nova/validador"
                  search={{
                    simulationId: search.simulationId,
                    versionNumber: search.versionNumber,
                  }}
                >
                  <ShieldCheck className="mr-2 h-4 w-4" />
                  Voltar ao Validador
                </Link>
              </Button>
              <Button variant="outline" onClick={restartPreview} disabled={!version}>
                <RotateCcw className="mr-2 h-4 w-4" />
                Reiniciar prévia
              </Button>
            </div>
          )}
        </header>

        <StateBanner tone="info" title="Ambiente isolado de prévia">
          Somente eventos técnicos da autoria são guardados na sessão do navegador. Nenhuma resposta
          é enviada às APIs de participação, faturamento, resultados ou integração externa.
        </StateBanner>

        {!hasContext ? (
          <StateBanner tone="warning" title="Avaliação e versão não informadas">
            Abra esta prévia pelo botão Testar como candidato dentro da criação ou da revisão da
            avaliação.
          </StateBanner>
        ) : versionQuery.isLoading || validationQuery.isLoading ? (
          <section className="rounded-xl border border-border bg-card px-5 py-14 text-center text-sm text-muted-foreground">
            Preparando a jornada e o mapa de cobertura...
          </section>
        ) : versionQuery.error ? (
          <StateBanner tone="danger" title="Não foi possível carregar a versão">
            {versionQuery.error instanceof Error ? versionQuery.error.message : "Tente novamente."}
          </StateBanner>
        ) : !version || !analysis || !coverage ? null : (
          <div className="grid gap-6 xl:grid-cols-[minmax(0,1.15fr)_minmax(360px,0.85fr)]">
            <section className="space-y-5">
              <PreviewHeader
                versionName={version.name}
                versionNumber={version.versionNumber}
                pathNodeIds={pathNodeIds}
              />

              {stepLimitReached && (
                <StateBanner tone="danger" title="Limite de segurança da prévia atingido">
                  O percurso ultrapassou {MAX_PREVIEW_STEPS} transições. Reinicie a prévia e corrija
                  possíveis ciclos no Mapa.
                </StateBanner>
              )}

              {currentNode ? (
                <CandidateNodeCard
                  node={currentNode}
                  completed={completed}
                  completionMessage={completionMessage}
                  stepLimitReached={stepLimitReached}
                  onSelectOption={selectOption}
                  onTimeout={simulateTimeout}
                />
              ) : (
                <StateBanner tone="danger" title="Etapa inicial indisponível">
                  A raiz configurada não existe nesta versão. Abra o Mapa ou o Validador para corrigir.
                </StateBanner>
              )}

              <ValidationPanel
                diagnostics={diagnostics}
                simulationId={version.simulationId}
                versionNumber={version.versionNumber}
              />
            </section>

            <aside className="space-y-5">
              <CoverageSummary coverage={coverage} />
              <FlowProblems
                problems={analysis.problems}
                simulationId={version.simulationId}
                versionNumber={version.versionNumber}
              />
              <CoverageMap
                nodes={version.nodes}
                visitedNodeIds={visitedNodeIds}
                analysis={analysis}
              />
            </aside>
          </div>
        )}
      </main>
    </AppShell>
  );
}

function PreviewHeader({
  versionName,
  versionNumber,
  pathNodeIds,
}: {
  versionName: string;
  versionNumber: number;
  pathNodeIds: string[];
}) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-start gap-3">
        <PlayCircle className="mt-0.5 h-5 w-5 text-primary" />
        <div>
          <h2 className="font-semibold">
            {versionName} · versão {versionNumber}
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Caminho atual: {pathNodeIds.length > 0 ? pathNodeIds.join(" → ") : "não iniciado"}
          </p>
        </div>
      </div>
    </section>
  );
}

function CandidateNodeCard({
  node,
  completed,
  completionMessage,
  stepLimitReached,
  onSelectOption,
  onTimeout,
}: {
  node: SimulationVersionNodeResponse;
  completed: boolean;
  completionMessage: string | null;
  stepLimitReached: boolean;
  onSelectOption: (optionId: string) => void;
  onTimeout: () => void;
}) {
  return (
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-primary">
            Etapa {node.turnIndex}
          </p>
          <h2 className="mt-1 text-xl font-semibold">{node.speaker || "Situação"}</h2>
        </div>
        {node.isFinal && (
          <span className="rounded-full bg-success/10 px-3 py-1 text-xs font-semibold text-success">
            Final
          </span>
        )}
      </div>

      <p className="mt-5 whitespace-pre-wrap text-sm leading-7 text-foreground">
        {node.clientMessage || "Esta etapa ainda não possui mensagem."}
      </p>

      {completed ? (
        <div className="mt-6 rounded-xl border border-success/30 bg-success/5 p-4">
          <div className="flex items-start gap-3">
            <CheckCircle2 className="mt-0.5 h-5 w-5 text-success" />
            <div>
              <h3 className="font-semibold">Percurso encerrado nesta prévia</h3>
              <p className="mt-1 text-sm leading-6 text-muted-foreground">
                {completionMessage || node.reportText || "Encerramento alcançado."}
              </p>
            </div>
          </div>
        </div>
      ) : (
        <div className="mt-6 space-y-3">
          {node.options.map((option, index) => (
            <button
              key={option.id}
              type="button"
              onClick={() => onSelectOption(option.id)}
              disabled={stepLimitReached}
              className="flex min-h-12 w-full items-start gap-3 rounded-xl border border-border bg-background p-4 text-left text-sm transition hover:border-primary hover:bg-primary/5 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                {index + 1}
              </span>
              <span className="leading-6">{option.text || "Alternativa sem texto"}</span>
            </button>
          ))}
          {node.options.length === 0 && !node.isFinal && (
            <StateBanner tone="danger" title="Etapa sem alternativas">
              O percurso não pode continuar. Corrija a etapa no Editor de diálogo.
            </StateBanner>
          )}
          {node.timeLimitSeconds != null && node.timeLimitSeconds > 0 && (
            <Button variant="outline" onClick={onTimeout} disabled={stepLimitReached}>
              <TimerReset className="mr-2 h-4 w-4" />
              Simular tempo esgotado ({node.timeLimitSeconds}s)
            </Button>
          )}
        </div>
      )}
    </section>
  );
}

function CoverageSummary({
  coverage,
}: {
  coverage: ReturnType<typeof calculatePreviewCoverage>;
}) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-start gap-3">
        <GitBranch className="mt-0.5 h-5 w-5 text-primary" />
        <div>
          <h2 className="font-semibold">Cobertura da sessão</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Atualizada a cada etapa e alternativa percorrida.
          </p>
        </div>
      </div>
      <div className="mt-4 grid gap-3 sm:grid-cols-3 xl:grid-cols-1 2xl:grid-cols-3">
        <CoverageMetric
          label="Etapas"
          value={`${coverage.visitedNodes}/${coverage.totalNodes}`}
          percent={coverage.nodePercent}
        />
        <CoverageMetric
          label="Alternativas"
          value={`${coverage.visitedOptions}/${coverage.totalOptions}`}
          percent={coverage.optionPercent}
        />
        <CoverageMetric
          label="Finais"
          value={`${coverage.visitedFinals}/${coverage.totalFinals}`}
          percent={coverage.finalPercent}
        />
      </div>
    </section>
  );
}

function CoverageMetric({ label, value, percent }: { label: string; value: string; percent: number }) {
  return (
    <div className="rounded-lg border border-border bg-background p-3">
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          {label}
        </span>
        <span className="text-xs font-semibold text-primary">{percent}%</span>
      </div>
      <div className="mt-2 text-lg font-semibold">{value}</div>
      <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-muted">
        <div className="h-full rounded-full bg-primary" style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
}

function ValidationPanel({
  diagnostics,
  simulationId,
  versionNumber,
}: {
  diagnostics: ValidationDiagnostic[];
  simulationId: string;
  versionNumber: number;
}) {
  if (diagnostics.length === 0) {
    return (
      <StateBanner tone="ok" title="Validador sem pendências">
        Nenhum bloqueio ou aviso estrutural foi retornado para esta versão.
      </StateBanner>
    );
  }

  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-start gap-3">
        <AlertTriangle className="mt-0.5 h-5 w-5 text-warning" />
        <div>
          <h2 className="font-semibold">Pendências do Validador</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Cada item abre diretamente o editor responsável pelo campo afetado.
          </p>
        </div>
      </div>
      <div className="mt-4 space-y-3">
        {diagnostics.map((diagnostic, index) => (
          <article
            key={`${diagnostic.issue.nodeId ?? "global"}-${diagnostic.issue.message}-${index}`}
            className="rounded-lg border border-border bg-background p-4"
          >
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  {diagnostic.nodeLabel} · {diagnostic.fieldLabel}
                </div>
                <p className="mt-2 text-sm leading-6">{diagnostic.issue.message}</p>
                <p className="mt-1 text-xs leading-5 text-muted-foreground">
                  {diagnostic.resolution}
                </p>
              </div>
              <DiagnosticEditorLink
                diagnostic={diagnostic}
                simulationId={simulationId}
                versionNumber={versionNumber}
              />
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function DiagnosticEditorLink({
  diagnostic,
  simulationId,
  versionNumber,
}: {
  diagnostic: ValidationDiagnostic;
  simulationId: string;
  versionNumber: number;
}) {
  if (diagnostic.editor === "mapa") {
    return (
      <Button asChild size="sm" variant="outline">
        <Link
          to="/nova/mapa"
          search={{ simulationId, versionNumber, nodeId: diagnostic.nodeId ?? undefined }}
        >
          Abrir no Mapa
        </Link>
      </Button>
    );
  }
  if (diagnostic.editor === "avaliacao") {
    return (
      <Button asChild size="sm" variant="outline">
        <Link to="/nova/avaliacao" search={{ simulationId, versionNumber }}>
          Abrir Avaliação
        </Link>
      </Button>
    );
  }
  return (
    <Button asChild size="sm" variant="outline">
      <Link
        to="/nova/dialogo"
        search={{ simulationId, versionNumber, nodeId: diagnostic.nodeId ?? undefined }}
      >
        Abrir no Diálogo
      </Link>
    </Button>
  );
}

function FlowProblems({
  problems,
  simulationId,
  versionNumber,
}: {
  problems: PreviewFlowProblem[];
  simulationId: string;
  versionNumber: number;
}) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <h2 className="font-semibold">Problemas de fluxo</h2>
      <p className="mt-1 text-sm text-muted-foreground">
        Destinos ausentes, ciclos, caminhos longos e finais inacessíveis.
      </p>
      {problems.length === 0 ? (
        <p className="mt-4 text-sm text-success">Nenhum problema adicional detectado na prévia.</p>
      ) : (
        <div className="mt-4 space-y-3">
          {problems.slice(0, 12).map((problem, index) => (
            <div
              key={`${problem.kind}-${problem.nodeId ?? "global"}-${index}`}
              className="rounded-lg border border-danger/30 bg-danger/5 p-3"
            >
              <p className="text-sm leading-6">{problem.message}</p>
              <div className="mt-2 flex gap-2">
                <Button asChild size="sm" variant="outline">
                  <Link
                    to="/nova/mapa"
                    search={{
                      simulationId,
                      versionNumber,
                      nodeId: problem.nodeId ?? undefined,
                    }}
                  >
                    Corrigir no Mapa
                  </Link>
                </Button>
              </div>
            </div>
          ))}
          {problems.length > 12 && (
            <p className="text-xs text-muted-foreground">
              Mais {problems.length - 12} problema(s) disponível(is) no Validador.
            </p>
          )}
        </div>
      )}
    </section>
  );
}

function CoverageMap({
  nodes,
  visitedNodeIds,
  analysis,
}: {
  nodes: SimulationVersionNodeResponse[];
  visitedNodeIds: string[];
  analysis: ReturnType<typeof analyzePreviewGraph>;
}) {
  const visited = new Set(visitedNodeIds);
  const unreachable = new Set(analysis.unreachableNodeIds);
  const cycles = new Set(analysis.cycleNodeIds);
  const problems = new Set(analysis.problems.map((problem) => problem.nodeId).filter(Boolean));

  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <h2 className="font-semibold">Mapa de cobertura</h2>
      <p className="mt-1 text-sm text-muted-foreground">
        Etapas visitadas, não testadas, inacessíveis, finais e com problema.
      </p>
      <div className="mt-4 space-y-2">
        {[...nodes]
          .sort((left, right) => left.turnIndex - right.turnIndex)
          .map((node) => {
            const statuses = [
              visited.has(node.id) ? "Visitada" : "Não testada",
              unreachable.has(node.id) ? "Inacessível" : null,
              cycles.has(node.id) ? "Ciclo" : null,
              node.isFinal ? "Final" : null,
              problems.has(node.id) ? "Problema" : null,
            ].filter((status): status is string => Boolean(status));
            return (
              <div
                key={node.id}
                className={cn(
                  "rounded-lg border p-3",
                  visited.has(node.id) ? "border-primary/40 bg-primary/5" : "border-border bg-background",
                  problems.has(node.id) && "border-danger/40 bg-danger/5",
                )}
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <div className="text-sm font-semibold">Etapa {node.turnIndex}</div>
                    <div className="mt-0.5 max-w-[34rem] truncate text-xs text-muted-foreground">
                      {node.clientMessage || node.id}
                    </div>
                  </div>
                  <div className="flex flex-wrap justify-end gap-1">
                    {statuses.map((status) => (
                      <span
                        key={status}
                        className="rounded-full border border-border bg-card px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide"
                      >
                        {status}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            );
          })}
      </div>
    </section>
  );
}

function appendUnique(values: string[], value: string): string[] {
  return values.includes(value) ? values : [...values, value];
}

type PreviewTelemetryEvent = {
  event: "previewStarted" | "previewRestarted" | "optionSelected" | "timeoutSimulated";
  simulationId: string;
  versionNumber: number;
  nodeId: string | null;
  optionId?: string;
};

function recordPreviewTelemetry(event: PreviewTelemetryEvent): void {
  if (typeof window === "undefined") return;
  const storageKey = "praxis.authoring.preview.telemetry";
  try {
    const current = JSON.parse(window.sessionStorage.getItem(storageKey) ?? "[]") as unknown[];
    const next = [...current.slice(-199), { ...event, recordedAt: new Date().toISOString() }];
    window.sessionStorage.setItem(storageKey, JSON.stringify(next));
  } catch {
    window.sessionStorage.removeItem(storageKey);
  }
}
