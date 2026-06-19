import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useMemo, useRef, useState, type PointerEvent, type ReactNode } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  Grip,
  Link2,
  LocateFixed,
  MousePointer2,
  Plus,
  Save,
  ZoomIn,
  ZoomOut,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  createSimulationNode,
  createSimulationOption,
  getSimulationVersion,
  listSimulations,
  updateSimulationOption,
  type SimulationSummaryResponse,
  type SimulationVersionDetailResponse,
  type SimulationVersionNodeResponse,
  type SimulationVersionOptionResponse,
} from "@/lib/api/praxis";
import { canEditSimulationVersion } from "@/lib/simulation-meta";
import { defaultAnswerTimeLimitSeconds, useTenantConfig } from "@/lib/tenant-config";
import { cn } from "@/lib/utils";

type CanvasPoint = { x: number; y: number };
type NodePositions = Record<string, CanvasPoint>;
type DraggingNode = { nodeId: string; offset: CanvasPoint };
type ConnectingEdge = {
  sourceId: string;
  optionId: string;
  from: CanvasPoint;
  to: CanvasPoint;
};

const NODE_WIDTH = 280;
const NODE_MIN_HEIGHT = 148;
const CANVAS_WIDTH = 1800;
const CANVAS_HEIGHT = 1100;

export const Route = createFileRoute("/nova/mapa")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string"
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Construtor Visual - Praxis" },
      {
        name: "description",
        content: "Canvas drag and drop para montar e validar o grafo da simulacao.",
      },
    ],
  }),
  component: Page,
});

function Page() {
  const search = Route.useSearch();
  const queryClient = useQueryClient();
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const tenantConfig = useTenantConfig();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [newNodeText, setNewNodeText] = useState("");
  const [newOptionText, setNewOptionText] = useState("");
  const [feedback, setFeedback] = useState<{
    tone: "info" | "danger";
    title: string;
    body: string;
  } | null>(null);

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

  const version = versionQuery.data;
  const nodes = useMemo(() => version?.nodes ?? [], [version?.nodes]);
  const selectedNode = nodes.find((node) => node.id === selectedId) ?? nodes[0] ?? null;
  const isEditable = version?.status ? canEditSimulationVersion(version.status) : true;
  const competencyLevels = useMemo(
    () =>
      Object.fromEntries((version?.blueprint.competencies ?? []).map((item) => [item.name, 50])),
    [version?.blueprint.competencies],
  );

  useEffect(() => {
    if (!selectedId && nodes.length > 0) setSelectedId(nodes[0].id);
  }, [nodes, selectedId]);

  const refetchVersion = async () => {
    await queryClient.refetchQueries({
      queryKey: ["simulation-version", search.simulationId, search.versionNumber],
      type: "active",
    });
  };

  const addNodeMutation = useMutation({
    mutationFn: () => {
      if (!tenantConfig.config) {
        throw new Error("A configuracao da empresa ainda nao foi carregada.");
      }
      return createSimulationNode(search.simulationId!, search.versionNumber!, {
        clientMessage: newNodeText.trim(),
        timeLimitSeconds: defaultAnswerTimeLimitSeconds(tenantConfig.config),
      });
    },
    onSuccess: async (nodeId) => {
      setNewNodeText("");
      setSelectedId(nodeId);
      setFeedback({
        tone: "info",
        title: "Turno criado",
        body: "O novo no entrou no canvas. Arraste-o para posicionar e conecte uma alternativa.",
      });
      await refetchVersion();
    },
  });

  const addOptionMutation = useMutation({
    mutationFn: () =>
      createSimulationOption(search.simulationId!, search.versionNumber!, selectedNode!.id, {
        text: newOptionText.trim(),
        competencyLevels,
        isCritical: false,
        nextNodeId: null,
      }),
    onSuccess: async () => {
      setNewOptionText("");
      setFeedback({
        tone: "info",
        title: "Alternativa criada",
        body: "Arraste a porta da alternativa para outro turno ou deixe em FIM para concluir.",
      });
      await refetchVersion();
    },
  });

  const connectMutation = useMutation({
    mutationFn: ({
      nodeId,
      option,
      targetNodeId,
    }: {
      nodeId: string;
      option: SimulationVersionOptionResponse;
      targetNodeId: string | null;
    }) =>
      updateSimulationOption(search.simulationId!, search.versionNumber!, nodeId, option.id, {
        text: option.text,
        competencyLevels: option.competencyLevels,
        isCritical: option.isCritical,
        nextNodeId: targetNodeId,
        mediaUrl: option.mediaUrl ?? undefined,
        mediaType: option.mediaType,
      }),
    onSuccess: async () => {
      setFeedback({
        tone: "info",
        title: "Conexao salva",
        body: "A seta foi gravada na alternativa e o validador visual foi atualizado.",
      });
      await refetchVersion();
    },
  });

  const mutationError = addNodeMutation.error ?? addOptionMutation.error ?? connectMutation.error;

  return (
    <AppShell>
      <WizardStepper current="revisao" unlockedThrough="publicacao" />
      <ScreenStateStrip blockedReason="fluxo da conversa invalido ou com caminho sem saida precisa voltar ao editor" />
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 3</div>
          <h1 className="mt-1 font-display text-3xl">Construtor visual</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Monte o grafo da simulacao arrastando turnos e ligando alternativas com setas.
          </p>
        </div>
        {version && <StatusBadge status={version.status} />}
      </div>

      {!hasContext ? (
        <EmptyState
          title="Selecione uma versao para ver o mapa"
          description="Escolha uma versao abaixo para abrir o construtor visual do fluxo."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : versionQuery.isLoading || tenantConfig.isLoading ? (
        <StateBanner tone="info" title="Carregando mapa">
          Buscando o fluxo da conversa da simulacao {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      ) : versionQuery.isError || tenantConfig.isError ? (
        <StateBanner tone="danger" title="Nao foi possivel carregar o mapa">
          {(versionQuery.error ?? tenantConfig.error) instanceof Error
            ? (versionQuery.error ?? tenantConfig.error)?.message
            : "Verifique sua conexao e tente novamente."}
        </StateBanner>
      ) : version ? (
        <>
          {!isEditable && (
            <div className="mb-4">
              <StateBanner tone="warn" title="Versao protegida contra edicao">
                Esta versao nao pode ser alterada. Crie um rascunho na tela de dialogo antes de
                mudar conexoes ou adicionar turnos.
              </StateBanner>
            </div>
          )}
          {feedback && (
            <div className="mb-4">
              <StateBanner tone={feedback.tone} title={feedback.title}>
                {feedback.body}
              </StateBanner>
            </div>
          )}
          {mutationError && (
            <div className="mb-4">
              <StateBanner tone="danger" title="Nao foi possivel salvar">
                {mutationError instanceof Error ? mutationError.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}

          <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_340px]">
            <SimulationGraphCanvas
              version={version}
              disabled={!isEditable || connectMutation.isPending}
              selectedId={selectedNode?.id ?? null}
              onSelectNode={setSelectedId}
              onConnect={(sourceNode, option, targetNodeId) => {
                setFeedback(null);
                connectMutation.mutate({ nodeId: sourceNode.id, option, targetNodeId });
              }}
              onConnectionRejected={(message) =>
                setFeedback({ tone: "danger", title: "Conexao rejeitada", body: message })
              }
            />

            <aside className="rounded-md border border-border bg-card p-4">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <MousePointer2 className="h-4 w-4 text-primary" />
                Propriedades
              </div>
              <p className="mt-1 text-xs text-muted-foreground">
                Crie turnos, adicione alternativas e selecione um no para revisar suas saidas.
              </p>

              <fieldset disabled={!isEditable} className="mt-4 space-y-4">
                <label className="block">
                  <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                    Novo turno
                  </span>
                  <textarea
                    className="input min-h-24"
                    value={newNodeText}
                    onChange={(event) => setNewNodeText(event.target.value)}
                    placeholder="Descreva a situacao apresentada ao candidato"
                  />
                </label>
                <button
                  type="button"
                  onClick={() => {
                    setFeedback(null);
                    addNodeMutation.mutate();
                  }}
                  disabled={!newNodeText.trim() || addNodeMutation.isPending}
                  className="inline-flex w-full items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
                >
                  <Plus className="h-4 w-4" />
                  {addNodeMutation.isPending ? "Criando..." : "Criar turno no canvas"}
                </button>

                {selectedNode ? (
                  <div className="rounded-md border border-border bg-background p-3">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="text-xs uppercase text-muted-foreground">
                          {selectedNode.id}
                        </div>
                        <div className="text-sm font-semibold">Turno {selectedNode.turnIndex}</div>
                      </div>
                      <span className="rounded border border-border px-2 py-1 text-[11px] text-muted-foreground">
                        {selectedNode.options.length} alternativas
                      </span>
                    </div>
                    <p className="mt-2 line-clamp-4 text-xs text-muted-foreground">
                      {selectedNode.clientMessage}
                    </p>
                    <label className="mt-3 block">
                      <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                        Nova alternativa
                      </span>
                      <input
                        className="input"
                        value={newOptionText}
                        onChange={(event) => setNewOptionText(event.target.value)}
                        placeholder="Ex: Priorizar cliente critico"
                      />
                    </label>
                    <button
                      type="button"
                      onClick={() => {
                        setFeedback(null);
                        addOptionMutation.mutate();
                      }}
                      disabled={
                        !newOptionText.trim() ||
                        selectedNode.options.length >= 4 ||
                        addOptionMutation.isPending
                      }
                      className="mt-2 inline-flex w-full items-center justify-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
                    >
                      <Save className="h-4 w-4" />
                      {addOptionMutation.isPending ? "Salvando..." : "Adicionar alternativa"}
                    </button>
                  </div>
                ) : (
                  <div className="rounded-md border border-border bg-background p-3 text-sm text-muted-foreground">
                    Crie ou selecione um turno para editar alternativas.
                  </div>
                )}
              </fieldset>

              <div className="mt-4 space-y-2 rounded-md border border-border bg-background p-3 text-xs text-muted-foreground">
                <div className="flex items-center gap-2 font-semibold text-foreground">
                  <AlertTriangle className="h-3.5 w-3.5 text-warning" />
                  Validador visual
                </div>
                <p>Borda amarela: turno orfao, sem entrada a partir do inicio.</p>
                <p>Borda vermelha: nao ha caminho desse turno ate uma conclusao.</p>
              </div>
            </aside>
          </div>

          <div className="mt-8 flex justify-between">
            <Link
              to="/nova/piloto"
              search={{
                simulationId: search.simulationId,
                versionNumber: search.versionNumber,
              }}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar: Piloto
            </Link>
            <Link
              to="/nova/governanca"
              search={{
                simulationId: search.simulationId,
                versionNumber: search.versionNumber,
              }}
              className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Governanca
            </Link>
          </div>
        </>
      ) : null}
    </AppShell>
  );
}

function SimulationGraphCanvas({
  version,
  disabled,
  selectedId,
  onSelectNode,
  onConnect,
  onConnectionRejected,
}: {
  version: SimulationVersionDetailResponse;
  disabled: boolean;
  selectedId: string | null;
  onSelectNode: (nodeId: string) => void;
  onConnect: (
    sourceNode: SimulationVersionNodeResponse,
    option: SimulationVersionOptionResponse,
    targetNodeId: string | null,
  ) => void;
  onConnectionRejected: (message: string) => void;
}) {
  const storageKey = `praxis.graph.positions.${version.simulationId}.${version.versionNumber}`;
  const viewportRef = useRef<HTMLDivElement>(null);
  const [positions, setPositions] = useState<NodePositions>(() =>
    createInitialPositions(version.nodes, version.blueprint.rootNodeId),
  );
  const [zoom, setZoom] = useState(0.86);
  const [pan, setPan] = useState<CanvasPoint>({ x: 24, y: 24 });
  const [draggingNode, setDraggingNode] = useState<DraggingNode | null>(null);
  const [draggingCanvas, setDraggingCanvas] = useState<CanvasPoint | null>(null);
  const [connecting, setConnecting] = useState<ConnectingEdge | null>(null);

  useEffect(() => {
    try {
      const saved = window.localStorage.getItem(storageKey);
      const parsed = saved ? (JSON.parse(saved) as NodePositions) : {};
      setPositions((current) => ({
        ...createInitialPositions(version.nodes, version.blueprint.rootNodeId),
        ...current,
        ...parsed,
      }));
    } catch {
      setPositions(createInitialPositions(version.nodes, version.blueprint.rootNodeId));
    }
  }, [storageKey, version.blueprint.rootNodeId, version.nodes]);

  useEffect(() => {
    window.localStorage.setItem(storageKey, JSON.stringify(positions));
  }, [positions, storageKey]);

  const validation = useMemo(
    () => validateGraph(version.nodes, version.blueprint.rootNodeId),
    [version.nodes, version.blueprint.rootNodeId],
  );

  const edges = useMemo(
    () =>
      version.nodes.flatMap((node) =>
        node.options
          .filter((option) => option.nextNodeId)
          .map((option) => ({
            source: node,
            option,
            target: version.nodes.find((item) => item.id === option.nextNodeId) ?? null,
          }))
          .filter((edge) => edge.target),
      ),
    [version.nodes],
  );

  const toCanvasPoint = (clientX: number, clientY: number): CanvasPoint => {
    const rect = viewportRef.current?.getBoundingClientRect();
    return {
      x: (clientX - (rect?.left ?? 0) - pan.x) / zoom,
      y: (clientY - (rect?.top ?? 0) - pan.y) / zoom,
    };
  };

  const handlePointerMove = (event: PointerEvent<HTMLDivElement>) => {
    if (draggingNode) {
      const point = toCanvasPoint(event.clientX, event.clientY);
      setPositions((current) => ({
        ...current,
        [draggingNode.nodeId]: {
          x: Math.max(
            32,
            Math.min(CANVAS_WIDTH - NODE_WIDTH - 32, point.x - draggingNode.offset.x),
          ),
          y: Math.max(
            32,
            Math.min(CANVAS_HEIGHT - NODE_MIN_HEIGHT - 32, point.y - draggingNode.offset.y),
          ),
        },
      }));
    }

    if (draggingCanvas) {
      setPan({ x: event.clientX - draggingCanvas.x, y: event.clientY - draggingCanvas.y });
    }

    if (connecting) {
      setConnecting({ ...connecting, to: toCanvasPoint(event.clientX, event.clientY) });
    }
  };

  const endConnection = (targetNodeId: string | null) => {
    if (!connecting) return;
    const sourceNode = version.nodes.find((node) => node.id === connecting.sourceId);
    const option = sourceNode?.options.find((item) => item.id === connecting.optionId);
    setConnecting(null);
    if (!sourceNode || !option) return;

    if (targetNodeId === sourceNode.id) {
      onConnectionRejected("Um turno nao pode apontar para ele mesmo.");
      return;
    }

    if (targetNodeId && createsCycle(version.nodes, sourceNode.id, targetNodeId)) {
      onConnectionRejected("Loops nao sao permitidos em simulacoes.");
      return;
    }

    onConnect(sourceNode, option, targetNodeId);
  };

  const resetView = () => {
    setZoom(0.86);
    setPan({ x: 24, y: 24 });
  };

  return (
    <section className="min-h-[720px] overflow-hidden rounded-md border border-border bg-card">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border px-4 py-3">
        <div>
          <h2 className="text-sm font-semibold">
            {version.name} - v{version.versionNumber}
          </h2>
          <p className="text-xs text-muted-foreground">
            Arraste o canvas para navegar. Arraste a porta de uma alternativa ate outro turno.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <GraphMetric label="Turnos" value={version.nodes.length} />
          <GraphMetric label="Orfaos" value={validation.orphans.size} tone="warn" />
          <GraphMetric label="Sem fim" value={validation.noPathToEnd.size} tone="danger" />
          <IconButton
            label="Menos zoom"
            onClick={() => setZoom((value) => Math.max(0.55, value - 0.08))}
          >
            <ZoomOut className="h-4 w-4" />
          </IconButton>
          <span className="min-w-14 text-center text-xs tabular-nums text-muted-foreground">
            {Math.round(zoom * 100)}%
          </span>
          <IconButton
            label="Mais zoom"
            onClick={() => setZoom((value) => Math.min(1.25, value + 0.08))}
          >
            <ZoomIn className="h-4 w-4" />
          </IconButton>
          <IconButton label="Recentralizar" onClick={resetView}>
            <LocateFixed className="h-4 w-4" />
          </IconButton>
        </div>
      </div>

      <div
        ref={viewportRef}
        className="relative h-[650px] cursor-grab overflow-hidden bg-[radial-gradient(circle_at_1px_1px,color-mix(in_oklab,var(--color-border)_80%,transparent)_1px,transparent_0)] [background-size:24px_24px] active:cursor-grabbing"
        onPointerMove={handlePointerMove}
        onPointerUp={() => {
          setDraggingNode(null);
          setDraggingCanvas(null);
          endConnection(null);
        }}
        onPointerLeave={() => {
          setDraggingNode(null);
          setDraggingCanvas(null);
          setConnecting(null);
        }}
        onPointerDown={(event) => {
          if (event.target === event.currentTarget) {
            setDraggingCanvas({ x: event.clientX - pan.x, y: event.clientY - pan.y });
          }
        }}
        onWheel={(event) => {
          if (!event.ctrlKey && Math.abs(event.deltaY) < Math.abs(event.deltaX)) return;
          setZoom((value) => Math.max(0.55, Math.min(1.25, value - event.deltaY * 0.001)));
        }}
      >
        <div
          className="absolute left-0 top-0 origin-top-left"
          style={{
            width: CANVAS_WIDTH,
            height: CANVAS_HEIGHT,
            transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
          }}
        >
          <svg className="pointer-events-none absolute inset-0 h-full w-full overflow-visible">
            <defs>
              <marker
                id="graph-arrow"
                markerWidth="12"
                markerHeight="12"
                refX="10"
                refY="6"
                orient="auto"
                markerUnits="strokeWidth"
              >
                <path d="M2,2 L10,6 L2,10 Z" fill="var(--color-primary)" />
              </marker>
              <marker
                id="graph-arrow-draft"
                markerWidth="12"
                markerHeight="12"
                refX="10"
                refY="6"
                orient="auto"
                markerUnits="strokeWidth"
              >
                <path d="M2,2 L10,6 L2,10 Z" fill="var(--color-warning)" />
              </marker>
            </defs>
            {edges.map(({ source, option, target }) => {
              const sourcePoint = getOptionAnchor(source, option, positions);
              const targetPoint = getNodeInputAnchor(target!, positions);
              return (
                <g key={`${source.id}-${option.id}-${target!.id}`}>
                  <path
                    d={bezierPath(sourcePoint, targetPoint)}
                    fill="none"
                    stroke="var(--color-primary)"
                    strokeWidth="2.5"
                    markerEnd="url(#graph-arrow)"
                  />
                  <text
                    x={(sourcePoint.x + targetPoint.x) / 2}
                    y={(sourcePoint.y + targetPoint.y) / 2 - 8}
                    className="fill-muted-foreground text-[11px]"
                    textAnchor="middle"
                  >
                    {truncate(option.text, 28)}
                  </text>
                </g>
              );
            })}
            {connecting && (
              <path
                d={bezierPath(connecting.from, connecting.to)}
                fill="none"
                stroke="var(--color-warning)"
                strokeDasharray="8 6"
                strokeWidth="2.5"
                markerEnd="url(#graph-arrow-draft)"
              />
            )}
          </svg>

          {version.nodes.map((node) => {
            const position = positions[node.id] ?? { x: 60, y: 60 };
            const isRoot = node.id === version.blueprint.rootNodeId;
            const isOrphan = validation.orphans.has(node.id);
            const noPathToEnd = validation.noPathToEnd.has(node.id);
            return (
              <div
                key={node.id}
                className={cn(
                  "absolute rounded-md border bg-card shadow-sm",
                  selectedId === node.id && "ring-2 ring-primary/35",
                  isOrphan && "border-warning bg-warning/10",
                  noPathToEnd && "border-danger bg-danger/10",
                  !isOrphan && !noPathToEnd && "border-border",
                )}
                style={{
                  width: NODE_WIDTH,
                  minHeight: NODE_MIN_HEIGHT,
                  left: position.x,
                  top: position.y,
                }}
                onPointerUp={(event) => {
                  event.stopPropagation();
                  endConnection(node.id);
                }}
                onPointerDown={(event) => event.stopPropagation()}
              >
                <div
                  className="flex cursor-grab items-start gap-2 border-b border-border px-3 py-2 active:cursor-grabbing"
                  onPointerDown={(event) => {
                    if (disabled) return;
                    const point = toCanvasPoint(event.clientX, event.clientY);
                    setDraggingNode({
                      nodeId: node.id,
                      offset: { x: point.x - position.x, y: point.y - position.y },
                    });
                    onSelectNode(node.id);
                  }}
                >
                  <Grip className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 text-xs font-semibold uppercase text-muted-foreground">
                      {isRoot ? "Inicio" : `Turno ${node.turnIndex}`}
                      {isOrphan && <AlertTriangle className="h-3.5 w-3.5 text-warning" />}
                      {noPathToEnd && <AlertTriangle className="h-3.5 w-3.5 text-danger" />}
                      {!isOrphan && !noPathToEnd && (
                        <CheckCircle2 className="h-3.5 w-3.5 text-success" />
                      )}
                    </div>
                    <button
                      type="button"
                      onClick={() => onSelectNode(node.id)}
                      className="mt-1 block min-h-0 w-full text-left text-sm font-semibold hover:text-primary"
                    >
                      {node.id}
                    </button>
                  </div>
                  <div className="absolute -left-2 top-14 h-4 w-4 rounded-full border border-primary bg-card" />
                </div>
                <p className="line-clamp-3 px-3 py-2 text-xs text-muted-foreground">
                  {node.clientMessage}
                </p>
                <div className="space-y-1.5 px-3 pb-3">
                  {node.options.map((option, optionIndex) => (
                    <div
                      key={option.id}
                      className="grid grid-cols-[1fr_auto] items-center gap-2 rounded border border-border bg-background px-2 py-1.5"
                    >
                      <div className="min-w-0">
                        <div className="truncate text-[11px] font-medium">{option.text}</div>
                        <div className="text-[10px] text-muted-foreground">
                          {option.nextNodeId ? `vai para ${option.nextNodeId}` : "vai para FIM"}
                        </div>
                      </div>
                      <button
                        type="button"
                        disabled={disabled}
                        title="Arrastar conexao"
                        className="inline-flex h-8 min-h-0 w-8 items-center justify-center rounded-md border border-border bg-card text-primary hover:bg-accent disabled:opacity-50"
                        onPointerDown={(event) => {
                          if (disabled) return;
                          event.stopPropagation();
                          const from = getOptionAnchor(node, option, positions);
                          setConnecting({
                            sourceId: node.id,
                            optionId: option.id,
                            from: {
                              x: from.x,
                              y: (positions[node.id]?.y ?? 0) + 112 + optionIndex * 45,
                            },
                            to: toCanvasPoint(event.clientX, event.clientY),
                          });
                        }}
                      >
                        <Link2 className="h-4 w-4" />
                      </button>
                    </div>
                  ))}
                  {node.options.length === 0 && (
                    <div className="rounded border border-dashed border-border px-2 py-2 text-[11px] text-muted-foreground">
                      Sem alternativas cadastradas.
                    </div>
                  )}
                </div>
              </div>
            );
          })}

          <div
            className="absolute rounded-md border border-success/30 bg-success/10 px-4 py-3 text-sm text-success-foreground"
            style={{ left: 1500, top: 470 }}
          >
            <div className="font-semibold">FIM</div>
            <div className="mt-1 text-xs opacity-80">Conclusao da jornada</div>
          </div>
        </div>
      </div>
    </section>
  );
}

function GraphMetric({
  label,
  value,
  tone = "muted",
}: {
  label: string;
  value: number;
  tone?: "muted" | "warn" | "danger";
}) {
  return (
    <span
      className={cn(
        "rounded-md border px-2 py-1 text-xs",
        tone === "muted" && "border-border bg-background text-muted-foreground",
        tone === "warn" && "border-warning/35 bg-warning/15 text-warning-foreground",
        tone === "danger" && "border-danger/25 bg-danger/10 text-danger",
      )}
    >
      {label}: <strong>{value}</strong>
    </span>
  );
}

function IconButton({
  label,
  onClick,
  children,
}: {
  label: string;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      onClick={onClick}
      className="inline-flex h-9 min-h-0 w-9 items-center justify-center rounded-md border border-border bg-card hover:bg-accent"
    >
      {children}
    </button>
  );
}

function createInitialPositions(
  nodes: SimulationVersionNodeResponse[],
  rootNodeId: string,
): NodePositions {
  return Object.fromEntries(
    [...nodes]
      .sort((a, b) => {
        if (a.id === rootNodeId) return -1;
        if (b.id === rootNodeId) return 1;
        return a.turnIndex - b.turnIndex;
      })
      .map((node, index) => [
        node.id,
        {
          x: 72 + (index % 4) * 380,
          y: 72 + Math.floor(index / 4) * 250,
        },
      ]),
  );
}

function validateGraph(nodes: SimulationVersionNodeResponse[], rootNodeId: string) {
  const incoming = new Map<string, number>();
  const adjacency = new Map<string, string[]>();
  nodes.forEach((node) => {
    incoming.set(node.id, 0);
    adjacency.set(
      node.id,
      node.options.flatMap((option) => (option.nextNodeId ? [option.nextNodeId] : [])),
    );
  });
  nodes.forEach((node) => {
    node.options.forEach((option) => {
      if (option.nextNodeId) {
        incoming.set(option.nextNodeId, (incoming.get(option.nextNodeId) ?? 0) + 1);
      }
    });
  });

  const reachable = new Set<string>();
  const visit = (nodeId: string) => {
    if (reachable.has(nodeId)) return;
    reachable.add(nodeId);
    adjacency.get(nodeId)?.forEach(visit);
  };
  visit(rootNodeId);

  const orphans = new Set(
    nodes
      .filter(
        (node) =>
          node.id !== rootNodeId && ((incoming.get(node.id) ?? 0) === 0 || !reachable.has(node.id)),
      )
      .map((node) => node.id),
  );

  const pathCache = new Map<string, boolean>();
  const hasPathToEnd = (nodeId: string, stack = new Set<string>()): boolean => {
    if (pathCache.has(nodeId)) return pathCache.get(nodeId)!;
    if (stack.has(nodeId)) return false;
    const node = nodes.find((item) => item.id === nodeId);
    if (!node) return false;
    if (node.options.some((option) => option.nextNodeId === null)) {
      pathCache.set(nodeId, true);
      return true;
    }
    stack.add(nodeId);
    const result = node.options.some(
      (option) => option.nextNodeId && hasPathToEnd(option.nextNodeId, stack),
    );
    stack.delete(nodeId);
    pathCache.set(nodeId, result);
    return result;
  };

  return {
    orphans,
    noPathToEnd: new Set(nodes.filter((node) => !hasPathToEnd(node.id)).map((node) => node.id)),
  };
}

function createsCycle(
  nodes: SimulationVersionNodeResponse[],
  sourceNodeId: string,
  targetNodeId: string,
) {
  const adjacency = new Map(
    nodes.map((node) => [
      node.id,
      node.options.flatMap((option) => (option.nextNodeId ? [option.nextNodeId] : [])),
    ]),
  );
  const stack = [targetNodeId];
  const visited = new Set<string>();
  while (stack.length > 0) {
    const current = stack.pop()!;
    if (current === sourceNodeId) return true;
    if (visited.has(current)) continue;
    visited.add(current);
    stack.push(...(adjacency.get(current) ?? []));
  }
  return false;
}

function getNodeInputAnchor(
  node: SimulationVersionNodeResponse,
  positions: NodePositions,
): CanvasPoint {
  const position = positions[node.id] ?? { x: 60, y: 60 };
  return { x: position.x, y: position.y + 56 };
}

function getOptionAnchor(
  node: SimulationVersionNodeResponse,
  option: SimulationVersionOptionResponse,
  positions: NodePositions,
): CanvasPoint {
  const position = positions[node.id] ?? { x: 60, y: 60 };
  const optionIndex = Math.max(
    0,
    node.options.findIndex((item) => item.id === option.id),
  );
  return { x: position.x + NODE_WIDTH, y: position.y + 140 + optionIndex * 45 };
}

function bezierPath(from: CanvasPoint, to: CanvasPoint) {
  const distance = Math.max(120, Math.abs(to.x - from.x) * 0.55);
  return `M ${from.x} ${from.y} C ${from.x + distance} ${from.y}, ${to.x - distance} ${to.y}, ${to.x} ${to.y}`;
}

function truncate(value: string, max: number) {
  return value.length > max ? `${value.slice(0, max - 1)}...` : value;
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
          search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
          className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          {simulation.name} v{simulation.versionNumber}
        </Link>
      ))}
    </div>
  );
}
