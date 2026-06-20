import { useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from "react";
import {
  Clock,
  Crosshair,
  Flag,
  GitBranch,
  Maximize2,
  Plus,
  Timer,
  Trash2,
  Workflow,
  X,
  ZoomIn,
  ZoomOut,
} from "lucide-react";
import type {
  SimulationVersionDetailResponse,
  SimulationVersionNodeResponse,
  SimulationVersionOptionResponse,
  UpdateNodeRequest,
  UpdateOptionRequest,
} from "@/lib/api/praxis";

export type NodeUpdateBody = UpdateNodeRequest;

type NodeDto = SimulationVersionNodeResponse;
type OptionDto = SimulationVersionOptionResponse;
type Point = { x: number; y: number };
type Link = { via: "option"; optionId: string } | { via: "timeout" };
type DragState = {
  id: string;
  origin: Point;
  pointer: Point;
};
type Transition = {
  node: NodeDto;
  option: OptionDto | null;
};

interface FlowCanvasProps {
  version: SimulationVersionDetailResponse;
  canEdit: boolean;
  selectedNodeId: string | null;
  onSelectNode: (id: string | null) => void;
  onUpdateNode: (nodeId: string, body: NodeUpdateBody) => void;
  onEditOption: (nodeId: string, optionId: string, body: UpdateOptionRequest) => void;
  onAddOption: (nodeId: string) => void;
  onDeleteOption: (nodeId: string, optionId: string) => void;
  onDeleteStep: (nodeId: string) => void;
  onCreateChild: (parentNodeId: string, link: Link, asEnd: boolean) => void;
}

const NODE_WIDTH = 280;
const NORMAL_HEIGHT = 184;
const FINAL_HEIGHT = 214;
const OPTION_ROW = 32;
const END_SENTINEL = "__end__";
const NEW_SENTINEL = "__new__";

export default function FlowCanvas({
  version,
  canEdit,
  selectedNodeId,
  onSelectNode,
  onUpdateNode,
  onEditOption,
  onAddOption,
  onDeleteOption,
  onDeleteStep,
  onCreateChild,
}: FlowCanvasProps) {
  const nodes = useMemo(
    () => [...version.nodes].sort((a, b) => a.turnIndex - b.turnIndex || a.id.localeCompare(b.id)),
    [version.nodes],
  );
  const nodeById = useMemo(() => new Map(nodes.map((node) => [node.id, node])), [nodes]);
  const rootNodeId = version.blueprint.rootNodeId;
  const competencies = version.blueprint.competencies ?? [];
  const [zoom, setZoom] = useState(1);
  const [localPositions, setLocalPositions] = useState<Record<string, Point>>({});
  const [drag, setDrag] = useState<DragState | null>(null);
  const [nodeDrafts, setNodeDrafts] = useState<Record<string, { clientMessage?: string; reportText?: string; timeLimitSeconds?: string }>>({});
  const [optionDrafts, setOptionDrafts] = useState<Record<string, { text?: string; competencyLevels?: Record<string, number> }>>({});
  const dragLatest = useRef<Point | null>(null);
  const nodeTimers = useRef<Record<string, number>>({});
  const optionTimers = useRef<Record<string, number>>({});

  useEffect(() => {
    return () => {
      Object.values(nodeTimers.current).forEach(window.clearTimeout);
      Object.values(optionTimers.current).forEach(window.clearTimeout);
    };
  }, []);

  const autoPositions = useMemo(() => {
    const byTurn = new Map<number, NodeDto[]>();
    nodes.forEach((node) => {
      byTurn.set(node.turnIndex, [...(byTurn.get(node.turnIndex) ?? []), node]);
    });

    const turns = [...byTurn.keys()].sort((a, b) => a - b);
    const result: Record<string, Point> = {};
    turns.forEach((turn, turnIndex) => {
      const list = byTurn.get(turn) ?? [];
      list.forEach((node, rowIndex) => {
        result[node.id] = {
          x: 36 + turnIndex * 390,
          y: 34 + rowIndex * 260,
        };
      });
    });
    return result;
  }, [nodes]);

  const positionOf = (node: NodeDto): Point => {
    const local = localPositions[node.id];
    if (local) return local;
    if (node.positionX != null && node.positionY != null) {
      return { x: node.positionX, y: node.positionY };
    }
    return autoPositions[node.id] ?? { x: 36, y: 34 };
  };

  const heightOf = (node: NodeDto) =>
    isFinal(node) ? FINAL_HEIGHT : NORMAL_HEIGHT + Math.max(0, node.options.length - 2) * OPTION_ROW;

  const bounds = useMemo(() => {
    if (nodes.length === 0) return { width: 900, height: 520 };
    const maxX = Math.max(...nodes.map((node) => positionOf(node).x + NODE_WIDTH + 80));
    const maxY = Math.max(...nodes.map((node) => positionOf(node).y + heightOf(node) + 80));
    return { width: Math.max(900, maxX), height: Math.max(520, maxY) };
  }, [nodes, localPositions, autoPositions]);

  const selectedNode = selectedNodeId ? nodeById.get(selectedNodeId) ?? null : null;
  const normalNodes = nodes.filter((node) => !isFinal(node));
  const finalNodes = nodes.filter(isFinal);

  const scheduleNodeUpdate = (nodeId: string, body: NodeUpdateBody) => {
    if (!canEdit) return;
    window.clearTimeout(nodeTimers.current[nodeId]);
    nodeTimers.current[nodeId] = window.setTimeout(() => onUpdateNode(nodeId, body), 450);
  };

  const scheduleOptionUpdate = (nodeId: string, optionId: string, body: UpdateOptionRequest) => {
    if (!canEdit) return;
    const key = optionKey(nodeId, optionId);
    window.clearTimeout(optionTimers.current[key]);
    optionTimers.current[key] = window.setTimeout(() => onEditOption(nodeId, optionId, body), 450);
  };

  const updateNodeText = (node: NodeDto, value: string) => {
    setNodeDrafts((current) => ({
      ...current,
      [node.id]: { ...current[node.id], clientMessage: value },
    }));
    scheduleNodeUpdate(node.id, { clientMessage: value });
  };

  const updateReportText = (node: NodeDto, value: string) => {
    setNodeDrafts((current) => ({
      ...current,
      [node.id]: { ...current[node.id], reportText: value },
    }));
    scheduleNodeUpdate(node.id, { reportText: value });
  };

  const updateTimeLimitDraft = (node: NodeDto, value: string) => {
    setNodeDrafts((current) => ({
      ...current,
      [node.id]: { ...current[node.id], timeLimitSeconds: value },
    }));
  };

  const commitTimeLimit = (node: NodeDto) => {
    const value = nodeDrafts[node.id]?.timeLimitSeconds;
    if (value == null) return;
    onUpdateNode(node.id, { timeLimitSeconds: value.trim() === "" ? null : Number(value) });
  };

  const updateOptionText = (node: NodeDto, option: OptionDto, value: string) => {
    const key = optionKey(node.id, option.id);
    setOptionDrafts((current) => ({
      ...current,
      [key]: { ...current[key], text: value },
    }));
    scheduleOptionUpdate(node.id, option.id, { text: value });
  };

  const updateOptionScore = (node: NodeDto, option: OptionDto, competency: string, value: string) => {
    const nextLevels = {
      ...option.competencyLevels,
      ...(optionDrafts[optionKey(node.id, option.id)]?.competencyLevels ?? {}),
      [competency]: clamp(Number(value) || 0, 0, 100),
    };
    const key = optionKey(node.id, option.id);
    setOptionDrafts((current) => ({
      ...current,
      [key]: { ...current[key], competencyLevels: nextLevels },
    }));
    scheduleOptionUpdate(node.id, option.id, { competencyLevels: nextLevels });
  };

  const setOptionTarget = (node: NodeDto, option: OptionDto, value: string) => {
    if (value === NEW_SENTINEL) {
      onCreateChild(node.id, { via: "option", optionId: option.id }, false);
      return;
    }
    if (value === END_SENTINEL) {
      onCreateChild(node.id, { via: "option", optionId: option.id }, true);
      return;
    }
    onEditOption(node.id, option.id, { nextNodeId: value === "" ? "" : value });
  };

  const setTimeoutTarget = (node: NodeDto, value: string) => {
    if (value === NEW_SENTINEL) {
      onCreateChild(node.id, { via: "timeout" }, false);
      return;
    }
    if (value === END_SENTINEL) {
      onCreateChild(node.id, { via: "timeout" }, true);
      return;
    }
    onUpdateNode(node.id, { timeoutNextNodeId: value === "" ? "" : value });
  };

  const startDrag = (event: ReactPointerEvent, node: NodeDto) => {
    if (!canEdit || isInteractive(event.target)) return;
    event.preventDefault();
    event.stopPropagation();
    const position = positionOf(node);
    dragLatest.current = position;
    setDrag({
      id: node.id,
      origin: position,
      pointer: { x: event.clientX, y: event.clientY },
    });
    onSelectNode(node.id);
  };

  useEffect(() => {
    if (!drag) return;

    const move = (event: PointerEvent) => {
      const next = {
        x: Math.max(0, drag.origin.x + (event.clientX - drag.pointer.x) / zoom),
        y: Math.max(0, drag.origin.y + (event.clientY - drag.pointer.y) / zoom),
      };
      dragLatest.current = next;
      setLocalPositions((current) => ({ ...current, [drag.id]: next }));
    };
    const end = () => {
      const next = dragLatest.current;
      if (next) {
        onUpdateNode(drag.id, {
          positionX: Math.round(next.x),
          positionY: Math.round(next.y),
        });
      }
      setDrag(null);
      dragLatest.current = null;
    };

    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", end, { once: true });
    return () => {
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", end);
    };
  }, [drag, zoom, onUpdateNode]);

  return (
    <div className="flow-canvas-root">
      <style>{styles}</style>
      <div className="flow-canvas-toolbar">
        <div className="flow-canvas-title">
          <Workflow className="h-4 w-4" />
          <span>{normalNodes.length} etapas</span>
          <span className="flow-canvas-separator" />
          <Flag className="h-4 w-4" />
          <span>{finalNodes.length} finais</span>
        </div>
        <div className="flow-canvas-actions">
          <button type="button" onClick={() => setZoom((current) => clamp(current - 0.1, 0.6, 1.4))} title="Diminuir zoom">
            <ZoomOut className="h-4 w-4" />
          </button>
          <button type="button" onClick={() => setZoom(1)} className="flow-canvas-zoom">
            {Math.round(zoom * 100)}%
          </button>
          <button type="button" onClick={() => setZoom((current) => clamp(current + 0.1, 0.6, 1.4))} title="Aumentar zoom">
            <ZoomIn className="h-4 w-4" />
          </button>
          <button type="button" onClick={() => setLocalPositions({})} title="Voltar ao auto layout">
            <Maximize2 className="h-4 w-4" />
          </button>
        </div>
      </div>

      <div className="flow-canvas-shell">
        <div className="flow-canvas-stage" onPointerDown={() => onSelectNode(null)}>
          <div
            className="flow-canvas-board"
            style={{
              height: bounds.height * zoom,
              minWidth: bounds.width * zoom,
              width: bounds.width * zoom,
            }}
          >
            <div
              className="flow-canvas-plane"
              style={{
                height: bounds.height,
                transform: `scale(${zoom})`,
                width: bounds.width,
              }}
            >
              <svg className="flow-canvas-edges" height={bounds.height} width={bounds.width}>
                <defs>
                  <marker id="flow-arrow" markerHeight="8" markerWidth="8" orient="auto" refX="7" refY="4">
                    <path d="M0,0 L8,4 L0,8 Z" />
                  </marker>
                  <marker id="flow-timeout-arrow" markerHeight="8" markerWidth="8" orient="auto" refX="7" refY="4">
                    <path d="M0,0 L8,4 L0,8 Z" className="flow-timeout-arrow" />
                  </marker>
                </defs>
                {normalNodes.flatMap((node) =>
                  node.options.map((option, index) => {
                    const target = option.nextNodeId ? nodeById.get(option.nextNodeId) : undefined;
                    if (!target) return null;
                    return (
                      <path
                        key={`${node.id}-${option.id}`}
                        className={selectedNodeId === node.id || selectedNodeId === target.id ? "flow-edge flow-edge-active" : "flow-edge"}
                        d={edgePath(optionPort(node, index), inputPort(target))}
                        markerEnd="url(#flow-arrow)"
                      />
                    );
                  }),
                )}
                {normalNodes.map((node) => {
                  const target = node.timeoutNextNodeId ? nodeById.get(node.timeoutNextNodeId) : undefined;
                  if (!target) return null;
                  return (
                    <path
                      key={`${node.id}-timeout`}
                      className="flow-edge flow-edge-timeout"
                      d={edgePath(timeoutPort(node), inputPort(target))}
                      markerEnd="url(#flow-timeout-arrow)"
                    />
                  );
                })}
              </svg>

              {nodes.map((node) => {
                const position = positionOf(node);
                const selected = selectedNodeId === node.id;
                const height = heightOf(node);
                return (
                  <article
                    key={node.id}
                    className={[
                      "flow-node",
                      isFinal(node) ? "flow-node-final" : "",
                      selected ? "flow-node-selected" : "",
                      node.id === rootNodeId ? "flow-node-root" : "",
                    ].join(" ")}
                    onPointerDown={(event) => startDrag(event, node)}
                    style={{ height, left: position.x, top: position.y, width: NODE_WIDTH }}
                  >
                    <button className="flow-input-port" type="button" onClick={() => onSelectNode(node.id)} title="Selecionar" />
                    {isFinal(node) ? renderFinalNode(node) : renderStepNode(node)}
                  </article>
                );
              })}
            </div>
          </div>
        </div>

        <aside className="flow-inspector">
          {!selectedNode ? (
            <div className="flow-empty">
              <Workflow className="h-6 w-6" />
              <p>Selecione um card para editar texto, destinos e pontuacao.</p>
            </div>
          ) : isFinal(selectedNode) ? (
            <FinalInspector
              canEdit={canEdit}
              node={selectedNode}
              reportText={nodeDrafts[selectedNode.id]?.reportText ?? selectedNode.reportText ?? ""}
              scoreLabel={pathScoreLabel(selectedNode.id)}
              onClose={() => onSelectNode(null)}
              onDelete={() => onDeleteStep(selectedNode.id)}
              onReportChange={(value) => updateReportText(selectedNode, value)}
            />
          ) : (
            <StepInspector
              canEdit={canEdit}
              competencies={competencies.map((competency) => competency.name)}
              node={selectedNode}
              nodeById={nodeById}
              nodeText={nodeDrafts[selectedNode.id]?.clientMessage ?? selectedNode.clientMessage}
              optionDrafts={optionDrafts}
              targetNodes={targetsFor(selectedNode)}
              timeLimit={nodeDrafts[selectedNode.id]?.timeLimitSeconds ?? String(selectedNode.timeLimitSeconds ?? "")}
              onAddOption={() => onAddOption(selectedNode.id)}
              onClose={() => onSelectNode(null)}
              onDeleteOption={(optionId) => onDeleteOption(selectedNode.id, optionId)}
              onDeleteStep={() => onDeleteStep(selectedNode.id)}
              onNodeTextChange={(value) => updateNodeText(selectedNode, value)}
              onOptionScoreChange={(option, competency, value) => updateOptionScore(selectedNode, option, competency, value)}
              onOptionTargetChange={(option, value) => setOptionTarget(selectedNode, option, value)}
              onOptionTextChange={(option, value) => updateOptionText(selectedNode, option, value)}
              onTimeLimitBlur={() => commitTimeLimit(selectedNode)}
              onTimeLimitChange={(value) => updateTimeLimitDraft(selectedNode, value)}
              onTimeoutTargetChange={(value) => setTimeoutTarget(selectedNode, value)}
            />
          )}
        </aside>
      </div>
    </div>
  );

  function inputPort(node: NodeDto): Point {
    const position = positionOf(node);
    return { x: position.x, y: position.y + 36 };
  }

  function optionPort(node: NodeDto, index: number): Point {
    const position = positionOf(node);
    return { x: position.x + NODE_WIDTH, y: position.y + 94 + index * OPTION_ROW };
  }

  function timeoutPort(node: NodeDto): Point {
    const position = positionOf(node);
    return { x: position.x + NODE_WIDTH, y: position.y + heightOf(node) - 42 };
  }

  function renderStepNode(node: NodeDto) {
    const missingTimeout = !node.timeoutNextNodeId && node.options.some((option) => option.nextNodeId);
    return (
      <>
        <header className="flow-node-header">
          <span className="flow-node-id">
            {node.id === rootNodeId ? <Crosshair className="h-3.5 w-3.5" /> : <Workflow className="h-3.5 w-3.5" />}
            {node.id}
          </span>
          {node.id === rootNodeId && <span className="flow-pill">inicio</span>}
          <span className="flow-time">
            <Clock className="h-3.5 w-3.5" />
            {node.timeLimitSeconds ?? "-"}s
          </span>
        </header>
        <p className="flow-message">{(nodeDrafts[node.id]?.clientMessage ?? node.clientMessage) || "Sem fala cadastrada."}</p>
        <div className="flow-options">
          {node.options.map((option, index) => (
            <div className="flow-option-row" key={option.id}>
              <GitBranch className="h-3.5 w-3.5" />
              <span title={option.text}>{optionDrafts[optionKey(node.id, option.id)]?.text ?? option.text}</span>
              <span className="flow-port" style={{ top: 86 + index * OPTION_ROW }} />
            </div>
          ))}
        </div>
        <footer className={missingTimeout ? "flow-timeout flow-timeout-missing" : "flow-timeout"}>
          <Timer className="h-3.5 w-3.5" />
          <span>Tempo acaba</span>
          {missingTimeout && <b>sem destino</b>}
        </footer>
      </>
    );
  }

  function renderFinalNode(node: NodeDto) {
    const reportText = nodeDrafts[node.id]?.reportText ?? node.reportText ?? "";
    const missingReport = reportText.trim().length === 0;
    return (
      <>
        <header className="flow-node-header flow-final-header">
          <span className="flow-node-id">
            <Flag className="h-3.5 w-3.5" />
            {node.id}
          </span>
          <span className="flow-pill flow-pill-final">fim</span>
          {canEdit && (
            <button type="button" onClick={() => onDeleteStep(node.id)} title="Remover encerramento">
              <Trash2 className="h-3.5 w-3.5" />
            </button>
          )}
        </header>
        <div className="flow-final-score">
          <span>Nota deste caminho</span>
          <strong>{pathScoreLabel(node.id)}</strong>
        </div>
        <p className={missingReport ? "flow-final-report flow-final-report-missing" : "flow-final-report"}>
          {missingReport ? "Relatorio pendente." : reportText}
        </p>
      </>
    );
  }

  function targetsFor(node: NodeDto) {
    return nodes.filter((target) => target.id !== node.id && target.turnIndex > node.turnIndex);
  }

  function pathScoreLabel(finalNodeId: string) {
    const scores = collectPathScores(finalNodeId);
    if (scores.length === 0) return "-";
    const unique = [...new Set(scores)].sort((a, b) => a - b);
    if (unique.length === 1) return `${unique[0]}/100`;
    return `${unique[0]}-${unique[unique.length - 1]}/100`;
  }

  function collectPathScores(finalNodeId: string) {
    const transitions: Transition[] = [];
    const scores: number[] = [];

    const visit = (nodeId: string, seen: Set<string>) => {
      const node = nodeById.get(nodeId);
      if (!node || seen.has(nodeId)) return;
      if (isFinal(node)) {
        if (node.id === finalNodeId) scores.push(calculateScore(transitions));
        return;
      }

      const nextSeen = new Set(seen);
      nextSeen.add(nodeId);
      node.options.forEach((option) => {
        if (!option.nextNodeId) return;
        transitions.push({ node, option });
        visit(option.nextNodeId, nextSeen);
        transitions.pop();
      });
      if (node.timeoutNextNodeId) {
        transitions.push({ node, option: null });
        visit(node.timeoutNextNodeId, nextSeen);
        transitions.pop();
      }
    };

    visit(rootNodeId, new Set());
    return scores;
  }

  function calculateScore(path: Transition[]) {
    const raw: Record<string, number> = {};
    const max: Record<string, number> = {};
    competencies.forEach((competency) => {
      raw[competency.name] = 0;
      max[competency.name] = 0;
    });

    path.forEach((step) => {
      competencies.forEach((competency) => {
        const best = step.node.options.reduce(
          (current, option) => Math.max(current, option.competencyLevels[competency.name] ?? 0),
          0,
        );
        max[competency.name] += best;
        raw[competency.name] += step.option?.competencyLevels[competency.name] ?? 0;
      });
    });

    const activeWeight = competencies
      .filter((competency) => max[competency.name] > 0)
      .reduce((sum, competency) => sum + (competency.weight ?? 0), 0);

    if (activeWeight === 0) return 0;

    const weighted = competencies.reduce((sum, competency) => {
      if (max[competency.name] <= 0) return sum;
      const normalized = raw[competency.name] / max[competency.name];
      return sum + normalized * ((competency.weight ?? 0) / activeWeight);
    }, 0);

    return Math.round(weighted * 100);
  }
}

function FinalInspector({
  canEdit,
  node,
  reportText,
  scoreLabel,
  onClose,
  onDelete,
  onReportChange,
}: {
  canEdit: boolean;
  node: NodeDto;
  reportText: string;
  scoreLabel: string;
  onClose: () => void;
  onDelete: () => void;
  onReportChange: (value: string) => void;
}) {
  return (
    <div className="flow-inspector-body">
      <div className="flow-inspector-head">
        <span className="flow-node-id">
          <Flag className="h-3.5 w-3.5" />
          {node.id}
        </span>
        <button type="button" onClick={onClose} title="Fechar">
          <X className="h-4 w-4" />
        </button>
      </div>
      <div className="flow-score-box">
        <span>Nota deterministica</span>
        <strong>{scoreLabel}</strong>
      </div>
      <label className="flow-field">
        <span>Relatorio ao recrutador</span>
        <textarea disabled={!canEdit} rows={8} value={reportText} onChange={(event) => onReportChange(event.target.value)} />
      </label>
      {canEdit && (
        <button type="button" className="flow-danger-button" onClick={onDelete}>
          <Trash2 className="h-4 w-4" />
          Remover encerramento
        </button>
      )}
    </div>
  );
}

function StepInspector({
  canEdit,
  competencies,
  node,
  nodeById,
  nodeText,
  optionDrafts,
  targetNodes,
  timeLimit,
  onAddOption,
  onClose,
  onDeleteOption,
  onDeleteStep,
  onNodeTextChange,
  onOptionScoreChange,
  onOptionTargetChange,
  onOptionTextChange,
  onTimeLimitBlur,
  onTimeLimitChange,
  onTimeoutTargetChange,
}: {
  canEdit: boolean;
  competencies: string[];
  node: NodeDto;
  nodeById: Map<string, NodeDto>;
  nodeText: string;
  optionDrafts: Record<string, { text?: string; competencyLevels?: Record<string, number> }>;
  targetNodes: NodeDto[];
  timeLimit: string;
  onAddOption: () => void;
  onClose: () => void;
  onDeleteOption: (optionId: string) => void;
  onDeleteStep: () => void;
  onNodeTextChange: (value: string) => void;
  onOptionScoreChange: (option: OptionDto, competency: string, value: string) => void;
  onOptionTargetChange: (option: OptionDto, value: string) => void;
  onOptionTextChange: (option: OptionDto, value: string) => void;
  onTimeLimitBlur: () => void;
  onTimeLimitChange: (value: string) => void;
  onTimeoutTargetChange: (value: string) => void;
}) {
  return (
    <div className="flow-inspector-body">
      <div className="flow-inspector-head">
        <span className="flow-node-id">
          <Workflow className="h-3.5 w-3.5" />
          {node.id}
        </span>
        <button type="button" onClick={onClose} title="Fechar">
          <X className="h-4 w-4" />
        </button>
      </div>

      <label className="flow-field">
        <span>Fala da etapa</span>
        <textarea disabled={!canEdit} rows={4} value={nodeText} onChange={(event) => onNodeTextChange(event.target.value)} />
      </label>

      <label className="flow-field">
        <span>Tempo de resposta (s)</span>
        <input
          disabled={!canEdit}
          min={1}
          type="number"
          value={timeLimit}
          onBlur={onTimeLimitBlur}
          onChange={(event) => onTimeLimitChange(event.target.value)}
        />
      </label>

      <label className="flow-field">
        <span>Destino quando o tempo acaba</span>
        <TargetSelect
          disabled={!canEdit}
          nodeById={nodeById}
          targetNodes={targetNodes}
          value={targetValue(node.timeoutNextNodeId, nodeById)}
          onChange={onTimeoutTargetChange}
        />
      </label>

      <div className="flow-subhead">
        <span>Respostas</span>
        {canEdit && (
          <button type="button" onClick={onAddOption} title="Adicionar resposta">
            <Plus className="h-4 w-4" />
          </button>
        )}
      </div>

      <div className="flow-option-list">
        {node.options.map((option) => {
          const key = optionKey(node.id, option.id);
          const draft = optionDrafts[key];
          const levels = draft?.competencyLevels ?? option.competencyLevels;
          return (
            <article className="flow-option-card" key={option.id}>
              <div className="flow-option-card-head">
                <span>{option.id}</span>
                {canEdit && (
                  <button type="button" onClick={() => onDeleteOption(option.id)} title="Remover resposta">
                    <Trash2 className="h-4 w-4" />
                  </button>
                )}
              </div>
              <input
                disabled={!canEdit}
                value={draft?.text ?? option.text}
                onChange={(event) => onOptionTextChange(option, event.target.value)}
              />
              <TargetSelect
                disabled={!canEdit}
                nodeById={nodeById}
                targetNodes={targetNodes}
                value={targetValue(option.nextNodeId, nodeById)}
                onChange={(value) => onOptionTargetChange(option, value)}
              />
              <div className="flow-score-grid">
                {competencies.length === 0 && <p>Defina competencias no blueprint.</p>}
                {competencies.map((competency) => (
                  <label key={competency}>
                    <span title={competency}>{competency}</span>
                    <input
                      disabled={!canEdit}
                      max={100}
                      min={0}
                      type="number"
                      value={levels[competency] ?? 0}
                      onChange={(event) => onOptionScoreChange(option, competency, event.target.value)}
                    />
                  </label>
                ))}
              </div>
            </article>
          );
        })}
      </div>

      {canEdit && (
        <button type="button" className="flow-danger-button" onClick={onDeleteStep}>
          <Trash2 className="h-4 w-4" />
          Remover etapa
        </button>
      )}
    </div>
  );
}

function TargetSelect({
  disabled,
  nodeById,
  targetNodes,
  value,
  onChange,
}: {
  disabled: boolean;
  nodeById: Map<string, NodeDto>;
  targetNodes: NodeDto[];
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <select disabled={disabled} value={value} onChange={(event) => onChange(event.target.value)}>
      <option value="">Sem destino</option>
      <option value={END_SENTINEL}>Criar fim</option>
      <option value={NEW_SENTINEL}>Criar etapa</option>
      {targetNodes.map((target) => (
        <option key={target.id} value={target.id}>
          {isFinal(target) ? "Fim" : "Etapa"} {target.id}
        </option>
      ))}
      {value && value !== END_SENTINEL && value !== NEW_SENTINEL && !nodeById.has(value) && (
        <option value={value}>Destino ausente: {value}</option>
      )}
    </select>
  );
}

function targetValue(nodeId: string | null, nodeById: Map<string, NodeDto>) {
  if (!nodeId) return "";
  const target = nodeById.get(nodeId);
  if (target && isFinal(target)) return target.id;
  return nodeId;
}

function edgePath(from: Point, to: Point) {
  const gap = Math.max(60, Math.abs(to.x - from.x) * 0.45);
  return `M ${from.x} ${from.y} C ${from.x + gap} ${from.y}, ${to.x - gap} ${to.y}, ${to.x} ${to.y}`;
}

function optionKey(nodeId: string, optionId: string) {
  return `${nodeId}:${optionId}`;
}

function isFinal(node: NodeDto) {
  return node.isFinal === true;
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

function isInteractive(target: EventTarget) {
  return target instanceof HTMLElement && Boolean(target.closest("button,input,select,textarea"));
}

const styles = `
.flow-canvas-root {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--card);
  overflow: hidden;
}
.flow-canvas-toolbar {
  align-items: center;
  border-bottom: 1px solid var(--border);
  display: flex;
  gap: 12px;
  justify-content: space-between;
  padding: 10px 12px;
}
.flow-canvas-title,
.flow-canvas-actions {
  align-items: center;
  display: flex;
  gap: 8px;
}
.flow-canvas-title {
  color: var(--muted-foreground);
  font-size: 12px;
  font-weight: 600;
}
.flow-canvas-separator {
  background: var(--border);
  height: 18px;
  width: 1px;
}
.flow-canvas-actions button,
.flow-inspector button,
.flow-option-card button,
.flow-node-header button {
  align-items: center;
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: 6px;
  color: var(--foreground);
  display: inline-flex;
  gap: 6px;
  justify-content: center;
  min-height: 30px;
  padding: 0 9px;
}
.flow-canvas-actions button:hover,
.flow-inspector button:hover,
.flow-option-card button:hover,
.flow-node-header button:hover {
  background: var(--accent);
}
.flow-canvas-zoom {
  min-width: 54px;
}
.flow-canvas-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  min-height: 650px;
}
.flow-canvas-stage {
  background-color: color-mix(in oklch, var(--muted) 35%, transparent);
  background-image: radial-gradient(color-mix(in oklch, var(--muted-foreground) 24%, transparent) 1px, transparent 1px);
  background-size: 20px 20px;
  overflow: auto;
}
.flow-canvas-board {
  position: relative;
}
.flow-canvas-plane {
  left: 0;
  position: absolute;
  top: 0;
  transform-origin: top left;
}
.flow-canvas-edges {
  inset: 0;
  pointer-events: none;
  position: absolute;
}
#flow-arrow path {
  fill: var(--muted-foreground);
}
.flow-timeout-arrow {
  fill: var(--warning);
}
.flow-edge {
  fill: none;
  stroke: color-mix(in oklch, var(--muted-foreground) 62%, transparent);
  stroke-width: 1.8;
}
.flow-edge-active {
  stroke: var(--primary);
  stroke-width: 2.4;
}
.flow-edge-timeout {
  stroke: var(--warning);
  stroke-dasharray: 6 5;
}
.flow-node {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 8px;
  box-shadow: 0 6px 18px rgb(15 23 42 / 0.08);
  cursor: grab;
  overflow: visible;
  position: absolute;
  user-select: none;
}
.flow-node-selected {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px color-mix(in oklch, var(--primary) 16%, transparent), 0 12px 26px rgb(15 23 42 / 0.12);
}
.flow-node-root {
  border-top-color: var(--primary);
  border-top-width: 3px;
}
.flow-node-final {
  border-color: var(--warning);
}
.flow-node-header {
  align-items: center;
  border-bottom: 1px solid var(--border);
  display: flex;
  gap: 8px;
  height: 42px;
  padding: 0 10px;
}
.flow-final-header {
  background: color-mix(in oklch, var(--warning) 12%, transparent);
}
.flow-node-header button {
  margin-left: auto;
  min-height: 26px;
  padding: 0 6px;
}
.flow-node-id {
  align-items: center;
  color: var(--primary);
  display: inline-flex;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  font-weight: 700;
  gap: 5px;
}
.flow-pill {
  background: color-mix(in oklch, var(--primary) 10%, transparent);
  border-radius: 5px;
  color: var(--primary);
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  text-transform: uppercase;
}
.flow-pill-final {
  background: color-mix(in oklch, var(--warning) 16%, transparent);
  color: var(--warning-foreground);
}
.flow-time {
  align-items: center;
  color: var(--muted-foreground);
  display: inline-flex;
  font-size: 12px;
  gap: 4px;
  margin-left: auto;
}
.flow-message {
  color: var(--foreground);
  display: -webkit-box;
  font-size: 12px;
  line-height: 1.35;
  margin: 0;
  overflow: hidden;
  padding: 10px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.flow-options {
  border-top: 1px solid var(--border);
}
.flow-option-row {
  align-items: center;
  border-bottom: 1px solid var(--border);
  display: flex;
  gap: 7px;
  height: 32px;
  padding: 0 10px;
  position: relative;
}
.flow-option-row span {
  color: var(--foreground);
  flex: 1;
  font-size: 12px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.flow-port {
  background: var(--card);
  border: 2px solid var(--primary);
  border-radius: 999px;
  height: 12px;
  position: absolute;
  right: -7px;
  width: 12px;
}
.flow-input-port {
  background: var(--card);
  border: 2px solid var(--muted-foreground);
  border-radius: 999px;
  height: 12px;
  left: -7px;
  padding: 0;
  position: absolute;
  top: 30px;
  width: 12px;
}
.flow-timeout {
  align-items: center;
  bottom: 0;
  color: var(--muted-foreground);
  display: flex;
  font-size: 12px;
  gap: 7px;
  height: 40px;
  left: 0;
  padding: 0 10px;
  position: absolute;
  right: 0;
}
.flow-timeout b {
  color: var(--warning-foreground);
  font-size: 11px;
  margin-left: auto;
}
.flow-timeout-missing {
  background: color-mix(in oklch, var(--warning) 12%, transparent);
}
.flow-final-score {
  border-bottom: 1px solid var(--border);
  display: grid;
  gap: 4px;
  padding: 14px 12px;
}
.flow-final-score span {
  color: var(--muted-foreground);
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
}
.flow-final-score strong {
  color: var(--warning-foreground);
  font-size: 28px;
  line-height: 1;
}
.flow-final-report {
  color: var(--foreground);
  display: -webkit-box;
  font-size: 12px;
  line-height: 1.45;
  margin: 0;
  overflow: hidden;
  padding: 12px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}
.flow-final-report-missing {
  color: var(--warning-foreground);
}
.flow-inspector {
  border-left: 1px solid var(--border);
  overflow-y: auto;
}
.flow-empty {
  align-items: center;
  color: var(--muted-foreground);
  display: grid;
  gap: 10px;
  height: 100%;
  justify-items: center;
  padding: 28px;
  text-align: center;
}
.flow-inspector-body {
  display: grid;
  gap: 14px;
  padding: 16px;
}
.flow-inspector-head,
.flow-subhead,
.flow-option-card-head {
  align-items: center;
  display: flex;
  justify-content: space-between;
}
.flow-field {
  display: grid;
  gap: 6px;
}
.flow-field span,
.flow-subhead span {
  color: var(--muted-foreground);
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
}
.flow-field input,
.flow-field textarea,
.flow-field select,
.flow-option-card input,
.flow-option-card select,
.flow-score-grid input {
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: 6px;
  color: var(--foreground);
  font: inherit;
  min-height: 34px;
  padding: 7px 9px;
  width: 100%;
}
.flow-field textarea {
  resize: vertical;
}
.flow-field input:focus,
.flow-field textarea:focus,
.flow-field select:focus,
.flow-option-card input:focus,
.flow-option-card select:focus,
.flow-score-grid input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px color-mix(in oklch, var(--primary) 12%, transparent);
  outline: none;
}
.flow-score-box {
  background: color-mix(in oklch, var(--warning) 10%, transparent);
  border: 1px solid color-mix(in oklch, var(--warning) 35%, transparent);
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  padding: 12px;
}
.flow-score-box span {
  color: var(--muted-foreground);
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
}
.flow-score-box strong {
  color: var(--warning-foreground);
}
.flow-option-list {
  display: grid;
  gap: 10px;
}
.flow-option-card {
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: 8px;
  display: grid;
  gap: 8px;
  padding: 10px;
}
.flow-option-card-head span {
  color: var(--muted-foreground);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  font-weight: 700;
}
.flow-option-card-head button {
  min-height: 26px;
  padding: 0 6px;
}
.flow-score-grid {
  display: grid;
  gap: 6px;
}
.flow-score-grid label {
  align-items: center;
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(0, 1fr) 64px;
}
.flow-score-grid span {
  color: var(--muted-foreground);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.flow-score-grid input {
  min-height: 30px;
  text-align: center;
}
.flow-danger-button {
  color: var(--danger) !important;
  justify-self: start;
}
@media (max-width: 980px) {
  .flow-canvas-shell {
    grid-template-columns: 1fr;
  }
  .flow-inspector {
    border-left: 0;
    border-top: 1px solid var(--border);
  }
}
`;
