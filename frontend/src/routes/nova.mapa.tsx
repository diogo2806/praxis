import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useMemo, useState, type PointerEvent as ReactPointerEvent } from "react";
import { GitBranch, Link2, Move, MousePointer2 } from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  groupIssuesByNode,
  ValidationBadge,
  ValidationSummary,
} from "@/components/simulation/validation-badge";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  createSimulationBranchNode,
  getSimulationValidation,
  getSimulationVersion,
  listSimulations,
  updateSimulationNode,
  updateSimulationOption,
  type SimulationSummaryResponse,
  type SimulationVersionDetailResponse,
  type SimulationVersionNodeResponse,
  type SimulationVersionOptionResponse,
} from "@/lib/api/praxis";
import {
  buildNodeDisplayCodes,
  compareDisplayCodes,
  type NodeDisplayCodes,
} from "@/lib/simulation-node-hierarchy";
import { canEditSimulationVersion } from "@/lib/simulation-meta";
import { cn } from "@/lib/utils";

type CanvasPoint = { x: number; y: number };
type NodePositions = Record<string, CanvasPoint>;
type DraggingNode = { nodeId: string; offsetX: number; offsetY: number };

const CREATE_BRANCH_VALUE = "__CREATE_BRANCH__";
const NODE_WIDTH = 260;
const NODE_HEIGHT = 160;
const CANVAS_WIDTH = 1500;
const CANVAS_HEIGHT = 900;

export const Route = createFileRoute("/nova/mapa")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    nodeId: typeof search.nodeId === "string" ? search.nodeId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string"
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Mapa do fluxo - Práxis" },
      {
        name: "description",
        content: "Organização visual das etapas e conexões da avaliação.",
      },
    ],
  }),
  component: Page,
});

function Page() {
  const search = Route.useSearch();
  const queryClient = useQueryClient();
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const [selectedId, setSelectedId] = useState<string | null>(search.nodeId ?? null);
  const [positions, setPositions] = useState<NodePositions>({});
  const [dragging, setDragging] = useState<DraggingNode | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasContext,
  });
  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });
  const validationQuery = useQuery({
    queryKey: ["simulation-validation", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationValidation(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });

  const version = versionQuery.data;
  const nodes = useMemo(() => version?.nodes ?? [], [version?.nodes]);
  const displayCodes = useMemo(
    () => buildNodeDisplayCodes(nodes, version?.blueprint.rootNodeId),
    [nodes, version?.blueprint.rootNodeId],
  );
  const orderedNodes = useMemo(
    () =>
      [...nodes].sort((left, right) =>
        compareDisplayCodes(
          displayCodes.get(left.id) ?? String(left.turnIndex),
          displayCodes.get(right.id) ?? String(right.turnIndex),
        ),
      ),
    [nodes, displayCodes],
  );
  const selectedNode =
    orderedNodes.find((node) => node.id === selectedId) ?? orderedNodes[0] ?? null;
  const isEditable = version?.status ? canEditSimulationVersion(version.status) : false;
  const issuesByNode = useMemo(
    () => groupIssuesByNode(validationQuery.data?.issues),
    [validationQuery.data?.issues],
  );

  useEffect(() => {
    if (orderedNodes.length === 0) {
      setPositions({});
      return;
    }
    setPositions((current) => createPositions(orderedNodes, current));
    setSelectedId((current) =>
      current && orderedNodes.some((node) => node.id === current) ? current : orderedNodes[0].id,
    );
  }, [orderedNodes]);

  const refreshVersion = async () => {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: ["simulation-version", search.simulationId, search.versionNumber],
      }),
      queryClient.invalidateQueries({
        queryKey: ["simulation-validation", search.simulationId, search.versionNumber],
      }),
    ]);
  };

  const positionMutation = useMutation({
    mutationFn: ({ nodeId, point }: { nodeId: string; point: CanvasPoint }) =>
      updateSimulationNode(search.simulationId!, search.versionNumber!, nodeId, {
        positionX: Math.round(point.x),
        positionY: Math.round(point.y),
      }),
    onSuccess: async () => {
      setFeedback("Posição da etapa salva.");
      await refreshVersion();
    },
  });

  const connectionMutation = useMutation({
    mutationFn: ({
      node,
      option,
      targetNodeId,
    }: {
      node: SimulationVersionNodeResponse;
      option: SimulationVersionOptionResponse;
      targetNodeId: string | null;
    }) =>
      updateSimulationOption(search.simulationId!, search.versionNumber!, node.id, option.id, {
        text: option.text,
        competencyLevels: option.competencyLevels,
        isCritical: option.isCritical,
        nextNodeId: targetNodeId,
        mediaUrl: option.mediaUrl ?? undefined,
        mediaType: option.mediaType,
      }),
    onSuccess: async () => {
      setFeedback("Conexão da alternativa salva.");
      await refreshVersion();
    },
  });

  const branchMutation = useMutation({
    mutationFn: ({ nodeId, optionId }: { nodeId: string; optionId: string }) =>
      createSimulationBranchNode(search.simulationId!, search.versionNumber!, nodeId, optionId),
    onSuccess: async (nodeId) => {
      await refreshVersion();
      setSelectedId(nodeId);
      setFeedback("Nova etapa ramificada criada, vinculada e posicionada no mapa.");
    },
  });

  const mutationError = positionMutation.error ?? connectionMutation.error ?? branchMutation.error;

  function startDragging(event: ReactPointerEvent<HTMLButtonElement>, nodeId: string) {
    if (!isEditable) return;
    const point = positions[nodeId];
    if (!point) return;
    const canvas = event.currentTarget.closest("[data-flow-canvas]") as HTMLElement | null;
    const rect = canvas?.getBoundingClientRect();
    setDragging({
      nodeId,
      offsetX: event.clientX - (rect?.left ?? 0) - point.x,
      offsetY: event.clientY - (rect?.top ?? 0) - point.y,
    });
    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function moveDragging(event: ReactPointerEvent<HTMLDivElement>) {
    if (!dragging) return;
    const rect = event.currentTarget.getBoundingClientRect();
    const x = clamp(
      event.clientX - rect.left - dragging.offsetX,
      24,
      CANVAS_WIDTH - NODE_WIDTH - 24,
    );
    const y = clamp(
      event.clientY - rect.top - dragging.offsetY,
      24,
      CANVAS_HEIGHT - NODE_HEIGHT - 24,
    );
    setPositions((current) => ({ ...current, [dragging.nodeId]: { x, y } }));
  }

  function finishDragging() {
    if (!dragging) return;
    const point = positions[dragging.nodeId];
    const nodeId = dragging.nodeId;
    setDragging(null);
    if (point) positionMutation.mutate({ nodeId, point });
  }

  return (
    <AppShell>
      <WizardStepper current="revisao" unlockedThrough="publicacao" />
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 3</div>
          <h1 className="mt-1 font-display text-3xl">Mapa do fluxo</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Organize a posição das etapas e defina para onde cada alternativa direciona. Conteúdo,
            mídia, criticidade e pontuação são alterados somente no Editor de diálogo.
          </p>
        </div>
        <div className="flex flex-col items-end gap-2">
          {version && <StatusBadge status={version.status} />}
          {version && validationQuery.data && (
            <ValidationSummary
              blockerCount={validationQuery.data.blockerCount}
              warningCount={validationQuery.data.warningCount}
            />
          )}
        </div>
      </div>

      {!hasContext ? (
        <EmptyState
          title="Selecione uma versão para ver o mapa"
          description="Escolha uma avaliação para organizar visualmente suas etapas e conexões."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : versionQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando mapa">
          Buscando a avaliação {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      ) : versionQuery.isError ? (
        <StateBanner tone="danger" title="Não foi possível carregar o mapa">
          {versionQuery.error instanceof Error
            ? versionQuery.error.message
            : "Verifique sua conexão e tente novamente."}
        </StateBanner>
      ) : version ? (
        <>
          {!isEditable && (
            <div className="mb-4">
              <StateBanner tone="warn" title="Versão protegida contra edição">
                A posição e as conexões só podem ser alteradas em uma versão de rascunho.
              </StateBanner>
            </div>
          )}
          {feedback && (
            <div className="mb-4">
              <StateBanner tone="info" title="Alteração salva">
                {feedback}
              </StateBanner>
            </div>
          )}
          {mutationError && (
            <div className="mb-4">
              <StateBanner tone="danger" title="Não foi possível salvar">
                {mutationError instanceof Error ? mutationError.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}

          <section className="rounded-md border border-border bg-card">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border p-4">
              <div>
                <h2 className="text-sm font-semibold">
                  {version.name} · v{version.versionNumber}
                </h2>
                <p className="mt-1 text-xs text-muted-foreground">
                  A numeração 1, 1.1 e 1.1.1 representa visualmente cada ramificação. Arraste as
                  etapas ou crie uma nova ramificação diretamente no destino de uma alternativa.
                </p>
              </div>
              <Link
                to="/nova/dialogo"
                search={{
                  simulationId: search.simulationId,
                  versionNumber: search.versionNumber,
                  nodeId: selectedNode?.id,
                }}
                className="rounded-md border border-border bg-background px-3 py-2 text-xs font-medium hover:bg-accent"
              >
                Editar conteúdo no diálogo
              </Link>
            </div>

            <div className="grid gap-4 p-4 xl:grid-cols-[minmax(0,1fr)_340px]">
              <div className="overflow-auto rounded-md border border-border bg-background">
                <div
                  data-flow-canvas
                  className="relative touch-none bg-[radial-gradient(circle_at_1px_1px,color-mix(in_oklab,var(--color-border)_80%,transparent)_1px,transparent_0)] [background-size:24px_24px]"
                  style={{ width: CANVAS_WIDTH, height: CANVAS_HEIGHT }}
                  onPointerMove={moveDragging}
                  onPointerUp={finishDragging}
                  onPointerCancel={() => setDragging(null)}
                  onPointerLeave={() => {
                    if (dragging) finishDragging();
                  }}
                >
                  <FlowEdges version={version} positions={positions} />
                  {orderedNodes.map((node) => {
                    const position = positions[node.id] ?? { x: 24, y: 24 };
                    const selected = selectedNode?.id === node.id;
                    const displayCode = displayCodes.get(node.id) ?? String(node.turnIndex);
                    return (
                      <article
                        key={node.id}
                        className={cn(
                          "absolute overflow-hidden rounded-md border bg-card shadow-sm",
                          selected ? "border-primary ring-2 ring-primary/20" : "border-border",
                        )}
                        style={{
                          left: position.x,
                          top: position.y,
                          width: NODE_WIDTH,
                          minHeight: NODE_HEIGHT,
                        }}
                      >
                        <button
                          type="button"
                          className="flex w-full cursor-move items-center justify-between border-b border-border px-3 py-2 text-left"
                          onPointerDown={(event) => startDragging(event, node.id)}
                          onClick={() => setSelectedId(node.id)}
                          disabled={!isEditable}
                          aria-label={`Posicionar etapa ${displayCode}`}
                        >
                          <span className="flex items-center gap-2 text-xs font-semibold">
                            <Move className="h-3.5 w-3.5 text-primary" />
                            Etapa {displayCode}
                          </span>
                          <ValidationBadge issues={issuesByNode.get(node.id) ?? []} />
                        </button>
                        <button
                          type="button"
                          className="w-full p-3 text-left"
                          onClick={() => setSelectedId(node.id)}
                        >
                          <div className="line-clamp-3 text-sm font-medium">
                            {node.clientMessage || "Mensagem ainda não preenchida"}
                          </div>
                          <div className="mt-3 flex items-center justify-between text-xs text-muted-foreground">
                            <span>{node.options.length} alternativas</span>
                            <span>{node.id}</span>
                          </div>
                        </button>
                      </article>
                    );
                  })}
                </div>
              </div>

              <ConnectionPanel
                node={selectedNode}
                nodes={orderedNodes}
                displayCodes={displayCodes}
                disabled={!isEditable || connectionMutation.isPending || branchMutation.isPending}
                creatingBranchOptionId={
                  branchMutation.isPending ? (branchMutation.variables?.optionId ?? null) : null
                }
                onChange={(option, targetNodeId) => {
                  if (!selectedNode) return;
                  setFeedback(null);
                  connectionMutation.mutate({ node: selectedNode, option, targetNodeId });
                }}
                onCreateBranch={(option) => {
                  if (!selectedNode) return;
                  setFeedback(null);
                  branchMutation.mutate({ nodeId: selectedNode.id, optionId: option.id });
                }}
                simulationId={search.simulationId!}
                versionNumber={search.versionNumber!}
              />
            </div>
          </section>

          <div className="mt-8 flex justify-between gap-3">
            <Link
              to="/nova/dialogo"
              search={{
                simulationId: search.simulationId,
                nodeId: search.nodeId,
                versionNumber: search.versionNumber,
              }}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar: Editor de diálogo
            </Link>
            <Link
              to="/nova/validador"
              search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Validar fluxo
            </Link>
          </div>
        </>
      ) : null}
    </AppShell>
  );
}

function ConnectionPanel({
  node,
  nodes,
  displayCodes,
  disabled,
  creatingBranchOptionId,
  onChange,
  onCreateBranch,
  simulationId,
  versionNumber,
}: {
  node: SimulationVersionNodeResponse | null;
  nodes: SimulationVersionNodeResponse[];
  displayCodes: NodeDisplayCodes;
  disabled: boolean;
  creatingBranchOptionId: string | null;
  onChange: (option: SimulationVersionOptionResponse, targetNodeId: string | null) => void;
  onCreateBranch: (option: SimulationVersionOptionResponse) => void;
  simulationId: string;
  versionNumber: number;
}) {
  if (!node) {
    return (
      <aside className="rounded-md border border-border bg-card p-4">
        <EmptyState
          title="Nenhuma etapa"
          description="Crie etapas no Editor de diálogo antes de organizar o mapa."
        />
      </aside>
    );
  }

  const nodeDisplayCode = displayCodes.get(node.id) ?? String(node.turnIndex);
  const availableTargets = nodes
    .filter((candidate) => candidate.turnIndex > node.turnIndex)
    .sort((left, right) =>
      compareDisplayCodes(
        displayCodes.get(left.id) ?? String(left.turnIndex),
        displayCodes.get(right.id) ?? String(right.turnIndex),
      ),
    );

  return (
    <aside className="rounded-md border border-border bg-card p-4">
      <div className="flex items-center gap-2 text-sm font-semibold">
        <MousePointer2 className="h-4 w-4 text-primary" />
        Conexões da etapa {nodeDisplayCode}
      </div>
      <p className="mt-1 text-xs leading-5 text-muted-foreground">
        Defina um destino existente, finalize a avaliação ou crie uma nova etapa ramificada já
        vinculada a esta alternativa.
      </p>

      <div className="mt-4 space-y-3">
        {node.options.map((option) => (
          <div key={option.id} className="rounded-md border border-border bg-background p-3">
            <div className="flex gap-2">
              <GitBranch className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
              <div className="min-w-0 flex-1">
                <div className="line-clamp-3 text-sm font-medium">{option.text}</div>
                <label className="mt-3 block">
                  <span className="mb-1 block text-xs text-muted-foreground">Destino</span>
                  <select
                    className="input"
                    value={option.nextNodeId ?? "FIM"}
                    disabled={disabled}
                    aria-label={`Destino da alternativa ${option.id}`}
                    onChange={(event) => {
                      const target = event.target.value;
                      if (target === CREATE_BRANCH_VALUE) {
                        onCreateBranch(option);
                        return;
                      }
                      onChange(option, target === "FIM" ? null : target);
                    }}
                  >
                    {availableTargets.map((target) => (
                      <option key={target.id} value={target.id}>
                        Etapa {displayCodes.get(target.id) ?? target.turnIndex} · {target.id}
                      </option>
                    ))}
                    <option value="FIM">Finalizar avaliação</option>
                    <option disabled>──────────</option>
                    <option value={CREATE_BRANCH_VALUE}>+ Criar nova etapa ramificada</option>
                  </select>
                </label>
                {creatingBranchOptionId === option.id && (
                  <p className="mt-2 text-xs text-primary">
                    Criando e posicionando a ramificação...
                  </p>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {node.options.length === 0 && (
        <p className="mt-4 rounded-md border border-warning/30 bg-warning/10 p-3 text-xs text-warning-foreground">
          Esta etapa ainda não possui alternativas. Cadastre-as no Editor de diálogo.
        </p>
      )}

      <Link
        to="/nova/dialogo"
        search={{ simulationId, versionNumber, nodeId: node.id }}
        className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-md border border-border bg-background px-3 py-2 text-sm font-medium hover:bg-accent"
      >
        <Link2 className="h-4 w-4" />
        Editar conteúdo desta etapa
      </Link>
    </aside>
  );
}

function FlowEdges({
  version,
  positions,
}: {
  version: SimulationVersionDetailResponse;
  positions: NodePositions;
}) {
  const edges = version.nodes.flatMap((node) =>
    node.options.flatMap((option) => {
      if (!option.nextNodeId) return [];
      const target = version.nodes.find((candidate) => candidate.id === option.nextNodeId);
      if (!target) return [];
      const sourcePoint = positions[node.id];
      const targetPoint = positions[target.id];
      if (!sourcePoint || !targetPoint) return [];
      return [{ node, option, target, sourcePoint, targetPoint }];
    }),
  );

  return (
    <svg className="pointer-events-none absolute inset-0 h-full w-full" aria-hidden="true">
      <defs>
        <marker id="flow-arrow" markerWidth="10" markerHeight="10" refX="9" refY="5" orient="auto">
          <path d="M0,0 L10,5 L0,10 Z" fill="var(--color-primary)" />
        </marker>
      </defs>
      {edges.map(({ node, option, target, sourcePoint, targetPoint }) => {
        const x1 = sourcePoint.x + NODE_WIDTH;
        const y1 = sourcePoint.y + NODE_HEIGHT / 2;
        const x2 = targetPoint.x;
        const y2 = targetPoint.y + NODE_HEIGHT / 2;
        const middle = Math.max(60, Math.abs(x2 - x1) / 2);
        return (
          <path
            key={`${node.id}-${option.id}-${target.id}`}
            d={`M ${x1} ${y1} C ${x1 + middle} ${y1}, ${x2 - middle} ${y2}, ${x2} ${y2}`}
            fill="none"
            stroke="var(--color-primary)"
            strokeWidth="2"
            markerEnd="url(#flow-arrow)"
          />
        );
      })}
    </svg>
  );
}

function createPositions(
  nodes: SimulationVersionNodeResponse[],
  current: NodePositions,
): NodePositions {
  return Object.fromEntries(
    nodes.map((node, index) => {
      const serverPosition =
        typeof node.positionX === "number" && typeof node.positionY === "number"
          ? { x: node.positionX, y: node.positionY }
          : null;
      const column = index % 4;
      const row = Math.floor(index / 4);
      return [
        node.id,
        serverPosition ?? current[node.id] ?? { x: 40 + column * 350, y: 40 + row * 230 },
      ];
    }),
  );
}

function clamp(value: number, minimum: number, maximum: number) {
  return Math.max(minimum, Math.min(maximum, value));
}

function SimulationLinks({
  loading,
  simulations,
}: {
  loading: boolean;
  simulations: SimulationSummaryResponse[];
}) {
  if (loading) return <span className="text-sm text-muted-foreground">Carregando...</span>;
  return (
    <div className="flex flex-wrap gap-2">
      {simulations.map((simulation) => (
        <Link
          key={`${simulation.id}-${simulation.versionNumber}`}
          to="/nova/mapa"
          search={{
            simulationId: simulation.id,
            nodeId: undefined,
            versionNumber: simulation.versionNumber,
          }}
          className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          {simulation.name} v{simulation.versionNumber}
        </Link>
      ))}
    </div>
  );
}
