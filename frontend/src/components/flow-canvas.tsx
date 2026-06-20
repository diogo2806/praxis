import { useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from "react";
import { Clock, Crosshair, Flag, Maximize2, Plus, Timer, Trash2, Workflow, X, ZoomIn, ZoomOut } from "lucide-react";
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
type ScoreMap = Record<string, number>;
type Link = { via: "option"; optionId: string } | { via: "timeout" };
type DragState =
  | { type: "pan"; sx: number; sy: number; panX: number; panY: number }
  | { type: "node"; id: string; sx: number; sy: number; nx: number; ny: number }
  | { type: "connect"; fromNodeId: string; optionId: string }
  | { type: "connect-timeout"; fromNodeId: string };

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

const NODE_W = 268;
const MSG_H = 76;
const COMP_HEAD_H = 22;
const COMP_ROW_H = 22;
const LABEL_H = 24;
const OPT_H = 34;
const TIMEOUT_H = 40;
const FOOTER_H = 38;
const END_HEAD_H = 44;
const END_REPORT_H = 150;
const MAX_COMPS = 3;
const FIM = "__fim__";
const NEW = "__new__";
const COMP_BLOCK = COMP_HEAD_H + MAX_COMPS * COMP_ROW_H;
const OPT_START = MSG_H + COMP_BLOCK + LABEL_H;
const END_ACC_H = COMP_BLOCK;
const END_HEIGHT = END_HEAD_H + END_ACC_H + END_REPORT_H;
const CANVAS_W = 4200;
const CANVAS_H = 3000;

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
  const nodeMap = useMemo(() => new Map(nodes.map((node) => [node.id, node])), [nodes]);
  const rootId = version.blueprint.rootNodeId ?? nodes[0]?.id ?? null;
  const comps = useMemo(
    () =>
      (version.blueprint.competencies ?? []).slice(0, MAX_COMPS).map((competency) => ({
        key: competency.name,
        label: competency.name,
        short: shortLabel(competency.name),
      })),
    [version.blueprint.competencies],
  );
  const paddedComps = useMemo(() => {
    const result = [...comps];
    while (result.length < MAX_COMPS) {
      const index = result.length + 1;
      result.push({ key: `competencia_${index}`, label: `Competência ${index}`, short: `C${index}` });
    }
    return result;
  }, [comps]);

  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState<Point>({ x: 24, y: 10 });
  const [drag, setDrag] = useState<DragState | null>(null);
  const [conn, setConn] = useState<Point | null>(null);
  const [localPositions, setLocalPositions] = useState<Record<string, Point>>({});
  const [nodeDrafts, setNodeDrafts] = useState<Record<string, { clientMessage?: string; reportText?: string; timeLimitSeconds?: string }>>({});
  const [optionDrafts, setOptionDrafts] = useState<Record<string, { text?: string; competencyLevels?: ScoreMap }>>({});
  const canvasRef = useRef<HTMLDivElement | null>(null);
  const nodeTimers = useRef<Record<string, number>>({});
  const optionTimers = useRef<Record<string, number>>({});
  const dragLatest = useRef<Point | null>(null);

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

    const result: Record<string, Point> = {};
    [...byTurn.keys()]
      .sort((a, b) => a - b)
      .forEach((turn, turnIndex) => {
        const list = byTurn.get(turn) ?? [];
        list.forEach((node, rowIndex) => {
          result[node.id] = { x: 60 + turnIndex * 420, y: 80 + rowIndex * 430 };
        });
      });
    return result;
  }, [nodes]);

  const steps = nodes.filter((node) => !isFinal(node));
  const selected = selectedNodeId ? nodeMap.get(selectedNodeId) ?? null : null;

  const totals = useMemo(() => {
    let opts = 0;
    let noTimeout = 0;
    let ends = 0;
    let endNoReport = 0;
    nodes.forEach((node) => {
      if (isFinal(node)) {
        ends++;
        if (!currentReportText(node).trim()) endNoReport++;
      } else {
        opts += node.options.length;
        if (!node.timeoutNextNodeId) noTimeout++;
      }
    });
    return { steps: nodes.length - ends, opts, ends, noTimeout, endNoReport };
  }, [nodes, nodeDrafts]);

  const accMap = useMemo(() => {
    const result: Record<string, ScoreMap> = {};
    const empty = blankScores(paddedComps);
    const scoreOf = (node: NodeDto) => nodeCardScores(node, paddedComps, optionDrafts);

    const visit = (nodeId: string | null, base: ScoreMap, seen: Set<string>) => {
      if (!nodeId || seen.has(nodeId)) return;
      const node = nodeMap.get(nodeId);
      if (!node) return;
      if (isFinal(node)) {
        result[node.id] = base;
        return;
      }

      const own = scoreOf(node);
      const next = addScores(base, own, paddedComps);
      result[node.id] = next;
      const nextSeen = new Set(seen);
      nextSeen.add(node.id);
      node.options.forEach((option) => visit(option.nextNodeId, next, nextSeen));
      visit(node.timeoutNextNodeId, next, nextSeen);
    };

    visit(rootId, empty, new Set());
    nodes.forEach((node) => {
      if (!result[node.id]) result[node.id] = isFinal(node) ? empty : scoreOf(node);
    });
    return result;
  }, [nodes, nodeMap, rootId, paddedComps, optionDrafts]);

  useEffect(() => {
    if (!drag) return;

    const onMove = (event: PointerEvent) => {
      if (drag.type === "pan") {
        setPan({ x: drag.panX + (event.clientX - drag.sx), y: drag.panY + (event.clientY - drag.sy) });
        return;
      }
      if (drag.type === "node") {
        const next = {
          x: Math.max(0, drag.nx + (event.clientX - drag.sx) / zoom),
          y: Math.max(0, drag.ny + (event.clientY - drag.sy) / zoom),
        };
        dragLatest.current = next;
        setLocalPositions((current) => ({ ...current, [drag.id]: next }));
        return;
      }
      setConn(toCanvas(event.clientX, event.clientY));
    };

    const onUp = (event: PointerEvent) => {
      if (drag.type === "node") {
        const next = dragLatest.current;
        if (next && canEdit) {
          onUpdateNode(drag.id, { positionX: Math.round(next.x), positionY: Math.round(next.y) });
        }
      }
      if (drag.type === "connect" || drag.type === "connect-timeout") {
        const point = toCanvas(event.clientX, event.clientY);
        const target = nodeAt(point.x, point.y, drag.fromNodeId);
        if (target && !isFinal(target) && canEdit) {
          if (drag.type === "connect-timeout") onUpdateNode(drag.fromNodeId, { timeoutNextNodeId: target.id });
          else onEditOption(drag.fromNodeId, drag.optionId, { nextNodeId: target.id });
        }
      }
      dragLatest.current = null;
      setDrag(null);
      setConn(null);
    };

    window.addEventListener("pointermove", onMove);
    window.addEventListener("pointerup", onUp, { once: true });
    return () => {
      window.removeEventListener("pointermove", onMove);
      window.removeEventListener("pointerup", onUp);
    };
  }, [drag, zoom, canEdit, onUpdateNode, onEditOption]);

  useEffect(() => {
    const element = canvasRef.current;
    if (!element) return;

    const onWheel = (event: WheelEvent) => {
      event.preventDefault();
      const rect = element.getBoundingClientRect();
      const cx = event.clientX - rect.left;
      const cy = event.clientY - rect.top;
      const nextZoom = clamp(zoom * (event.deltaY < 0 ? 1.1 : 0.9), 0.4, 1.8);
      setPan({
        x: cx - ((cx - pan.x) / zoom) * nextZoom,
        y: cy - ((cy - pan.y) / zoom) * nextZoom,
      });
      setZoom(nextZoom);
    };

    element.addEventListener("wheel", onWheel, { passive: false });
    return () => element.removeEventListener("wheel", onWheel);
  }, [zoom, pan]);

  const zoomBy = (factor: number) => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) {
      setZoom((current) => clamp(current * factor, 0.4, 1.8));
      return;
    }
    const cx = rect.width / 2;
    const cy = rect.height / 2;
    const nextZoom = clamp(zoom * factor, 0.4, 1.8);
    setPan({
      x: cx - ((cx - pan.x) / zoom) * nextZoom,
      y: cy - ((cy - pan.y) / zoom) * nextZoom,
    });
    setZoom(nextZoom);
  };

  const fit = () => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect || nodes.length === 0) return;

    const minX = Math.min(...nodes.map((node) => positionOf(node).x));
    const minY = Math.min(...nodes.map((node) => positionOf(node).y));
    const maxX = Math.max(...nodes.map((node) => positionOf(node).x + NODE_W));
    const maxY = Math.max(...nodes.map((node) => positionOf(node).y + nodeHeight(node)));
    const pad = 48;
    const nextZoom = clamp(Math.min((rect.width - pad * 2) / Math.max(1, maxX - minX), (rect.height - pad * 2) / Math.max(1, maxY - minY)), 0.4, 1.4);
    setZoom(nextZoom);
    setPan({
      x: (rect.width - (maxX - minX) * nextZoom) / 2 - minX * nextZoom,
      y: (rect.height - (maxY - minY) * nextZoom) / 2 - minY * nextZoom,
    });
  };

  const connSource = drag && "fromNodeId" in drag ? nodeMap.get(drag.fromNodeId) ?? null : null;
  const connIndex = connSource && drag?.type === "connect" ? connSource.options.findIndex((option) => option.id === drag.optionId) : -1;
  const connStart = connSource
    ? drag?.type === "connect-timeout"
      ? timeoutPort(connSource)
      : connIndex >= 0
        ? outPort(connSource, connIndex)
        : null
    : null;

  return (
    <div className="vx-root">
      <style>{CSS}</style>

      <header className="vx-top">
        <div>
          <div className="vx-eyebrow">Passo 3 · Construtor do fluxo</div>
          <h1 className="vx-title">Mapa da simulação</h1>
        </div>
        <div className="vx-counts">
          <span>
            <b>{totals.steps}</b> etapas
          </span>
          <span className="vx-dot" />
          <span>
            <b>{totals.opts}</b> saídas
          </span>
          <span className="vx-dot" />
          <span>
            <b>{totals.ends}</b> encerramentos
          </span>
          {totals.noTimeout > 0 && (
            <span className="vx-warnpill" title="Toda etapa precisa definir o que fazer quando o tempo acaba">
              <Timer size={13} /> {totals.noTimeout} sem tempo
            </span>
          )}
          {totals.endNoReport > 0 && (
            <span className="vx-warnpill" title="Card de encerramento sem texto de relatório">
              <Flag size={13} /> {totals.endNoReport} sem relatório
            </span>
          )}
        </div>
        <div className="vx-tools">
          <div className="vx-seg">
            <button type="button" onClick={() => zoomBy(0.9)} title="Diminuir zoom">
              <ZoomOut size={16} />
            </button>
            <button type="button" onClick={() => setZoom(1)} className="vx-zlabel">
              {Math.round(zoom * 100)}%
            </button>
            <button type="button" onClick={() => zoomBy(1.1)} title="Aumentar zoom">
              <ZoomIn size={16} />
            </button>
          </div>
          <button type="button" className="vx-ghost" onClick={fit}>
            <Maximize2 size={15} /> Enquadrar
          </button>
        </div>
      </header>

      <div className="vx-stage">
        <div ref={canvasRef} className={`vx-canvas ${drag?.type === "pan" ? "vx-grabbing" : ""}`} onPointerDown={onCanvasDown}>
          <div className="vx-content" style={{ transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})` }}>
            <svg className="vx-edges" width={CANVAS_W} height={CANVAS_H}>
              <defs>
                <marker id="vx-a" markerWidth="9" markerHeight="9" refX="7.5" refY="4.5" orient="auto">
                  <path d="M0,0 L9,4.5 L0,9 Z" />
                </marker>
                <marker id="vx-at" markerWidth="9" markerHeight="9" refX="7.5" refY="4.5" orient="auto">
                  <path d="M0,0 L9,4.5 L0,9 Z" className="vx-at" />
                </marker>
              </defs>
              {steps.flatMap((node) =>
                node.options.map((option, index) => {
                  const target = option.nextNodeId ? nodeMap.get(option.nextNodeId) : null;
                  if (!target) return null;
                  const active = selectedNodeId === node.id || selectedNodeId === target.id;
                  return (
                    <path
                      key={`${node.id}-${option.id}-edge`}
                      d={edgePath(outPort(node, index), inputPort(target))}
                      className={`vx-edge ${active ? "vx-edge-on" : ""} ${isFinal(target) ? "vx-edge-end" : ""}`}
                      markerEnd="url(#vx-a)"
                    />
                  );
                }),
              )}
              {steps.map((node) => {
                const target = node.timeoutNextNodeId ? nodeMap.get(node.timeoutNextNodeId) : null;
                if (!target) return null;
                return <path key={`${node.id}-timeout`} d={edgePath(timeoutPort(node), inputPort(target))} className="vx-edge vx-edge-to" markerEnd="url(#vx-at)" />;
              })}
              {connStart && conn && <path d={edgePath(connStart, conn)} className={`vx-edge vx-edge-drag ${drag?.type === "connect-timeout" ? "vx-edge-drag-to" : ""}`} />}
            </svg>

            {steps.flatMap((node) =>
              node.options.map((option, index) => {
                const target = option.nextNodeId ? nodeMap.get(option.nextNodeId) : null;
                if (!target) return null;
                const start = outPort(node, index);
                const end = inputPort(target);
                const label = currentOptionText(node, option);
                return (
                  <button
                    key={`${node.id}-${option.id}-label`}
                    className="vx-elabel"
                    style={{ left: (start.x + end.x) / 2 - 8, top: (start.y + end.y) / 2 - 11 }}
                    type="button"
                    onClick={() => onSelectNode(node.id)}
                    title={label}
                  >
                    {label.length > 18 ? `${label.slice(0, 18)}...` : label}
                  </button>
                );
              }),
            )}

            {nodes.map((node) => (isFinal(node) ? renderEndNode(node) : renderStepNode(node)))}
          </div>

          <div className="vx-legend">
            <span className="vx-leg">
              <i className="vx-sw vx-sw-to" /> tempo · obrigatório
            </span>
            <span className="vx-leg">
              <Flag size={12} /> encerramento · relatório + acumulada
            </span>
            <span className="vx-leg-hint">escolher "Fim" numa saída cria o card de encerramento · acumulada = pontos do card + acumulada anterior</span>
          </div>
        </div>

        <aside className="vx-inspector">
          {!selected ? renderEmptyInspector() : isFinal(selected) ? renderEndInspector(selected) : renderStepInspector(selected)}
        </aside>
      </div>

      <section className="vx-tablewrap">
        <div className="vx-table-head">
          <div>
            <h2>Placar do fluxo</h2>
            <p>Uma linha por etapa: notas editáveis, acumulada e as saídas. Os encerramentos (relatórios) ficam no mapa.</p>
          </div>
          <span className="vx-table-count">
            {totals.steps} {totals.steps === 1 ? "etapa" : "etapas"}
          </span>
        </div>
        {totals.steps === 0 ? (
          <div className="vx-table-empty">A tabela nasce aqui conforme você cria etapas.</div>
        ) : (
          <div className="vx-table-scroll">
            <table className="vx-table">
              <thead>
                <tr>
                  <th className="vx-th-step">Etapa</th>
                  <th>Fala</th>
                  {paddedComps.map((comp) => (
                    <th key={comp.key} className="vx-th-n">
                      {comp.short}
                    </th>
                  ))}
                  <th className="vx-th-acc">Acumulada</th>
                  <th className="vx-th-out">Saídas</th>
                  <th className="vx-th-mid">Tempo</th>
                </tr>
              </thead>
              <tbody>{steps.map((node) => renderTableRow(node))}</tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );

  function renderStepNode(node: NodeDto) {
    const position = positionOf(node);
    const active = selectedNodeId === node.id;
    const isRoot = node.id === rootId;
    const noTimeout = !node.timeoutNextNodeId;
    const score = nodeCardScores(node, paddedComps, optionDrafts);
    const acc = accMap[node.id] ?? blankScores(paddedComps);
    const forward = forwardSteps(node);

    return (
      <div
        key={node.id}
        className={`vx-node ${active ? "vx-node-on" : ""} ${isRoot ? "vx-node-root" : ""}`}
        style={{ left: position.x, top: position.y, width: NODE_W }}
        onPointerDown={(event) => onNodeDown(event, node)}
      >
        <span className="vx-port vx-port-in" title="Entrada" />
        <div className="vx-msg">
          <div className="vx-msg-head">
            <span className="vx-id">
              {isRoot ? <Crosshair size={12} /> : <Workflow size={12} />}
              {node.id}
            </span>
            {isRoot && <span className="vx-badge">início</span>}
            {noTimeout && (
              <span className="vx-warn-mini" title="Falta a saída de tempo">
                <Timer size={11} />
              </span>
            )}
            <span className="vx-time">
              <Clock size={11} /> {node.timeLimitSeconds ?? "—"}s
            </span>
          </div>
          <p className="vx-msg-text">{currentNodeText(node) || "Sem fala cadastrada."}</p>
        </div>

        <div className="vx-comp">
          <div className="vx-comp-head">
            <span>Competências</span>
            <span>acum.</span>
          </div>
          {paddedComps.map((comp) => (
            <div key={comp.key} className="vx-comp-row">
              <span className="vx-comp-name" title={comp.label}>
                {comp.short}
              </span>
              <input
                className="vx-comp-in"
                disabled={!canEdit || comps.length === 0}
                max={100}
                min={0}
                type="number"
                value={score[comp.key] ?? 0}
                onChange={(event) => setCardScore(node, comp.key, event.target.value)}
              />
              <span className="vx-comp-acc">{acc[comp.key] ?? 0}</span>
            </div>
          ))}
        </div>

        <div className="vx-optlabel">Saídas (respostas)</div>
        {node.options.map((option, index) => (
          <div key={option.id} className="vx-opt">
            <span className="vx-opt-text" title={currentOptionText(node, option)}>
              {currentOptionText(node, option)}
            </span>
            <select className="vx-cardsel" disabled={!canEdit} value={targetValue(option.nextNodeId)} onChange={(event) => setOptionTarget(node, option, event.target.value)} title="Destino desta resposta">
              <option value="" disabled>
                — defina —
              </option>
              <option value={FIM}>Fim (encerra)</option>
              {forward.map((target) => (
                <option key={target.id} value={target.id}>
                  → {target.id}
                </option>
              ))}
              <option value={NEW}>＋ nova etapa</option>
            </select>
            {option.nextNodeId ? (
              <button className="vx-mini-del" disabled={!canEdit} onClick={() => unlinkOption(node, option)} title="Remover esta ligação" type="button">
                <Trash2 size={12} />
              </button>
            ) : (
              <button className="vx-mini-add" disabled={!canEdit} onClick={() => onCreateChild(node.id, { via: "option", optionId: option.id }, false)} title="Criar a próxima etapa (novo fluxo)" type="button">
                <Plus size={12} />
              </button>
            )}
            <span className="vx-port vx-port-out" title="Arraste até uma etapa adiante para ligar" onPointerDown={(event) => startConnect(event, node, index)} />
          </div>
        ))}

        <div className={`vx-timeout ${noTimeout ? "vx-timeout-warn" : ""}`}>
          <Timer size={12} />
          <span className="vx-timeout-l">Tempo acaba</span>
          <select className="vx-cardsel vx-cardsel-to" disabled={!canEdit} value={targetValue(node.timeoutNextNodeId)} onChange={(event) => setTimeoutTarget(node, event.target.value)} title="Para onde vai quem deixa o tempo acabar">
            <option value="" disabled>
              — defina —
            </option>
            <option value={FIM}>Fim (encerra)</option>
            {forward.map((target) => (
              <option key={target.id} value={target.id}>
                → {target.id}
              </option>
            ))}
            <option value={NEW}>＋ nova etapa</option>
          </select>
          {node.timeoutNextNodeId ? (
            <button className="vx-mini-del" disabled={!canEdit} onClick={() => unlinkTimeout(node)} title="Remover esta ligação" type="button">
              <Trash2 size={12} />
            </button>
          ) : (
            <button className="vx-mini-add vx-mini-add-to" disabled={!canEdit} onClick={() => onCreateChild(node.id, { via: "timeout" }, false)} title="Criar a etapa de fallback do tempo" type="button">
              <Plus size={12} />
            </button>
          )}
          <span className="vx-port vx-port-to" title="Arraste até uma etapa adiante" onPointerDown={(event) => startConnectTimeout(event, node)} />
        </div>

        <div className="vx-foot">
          <button className="vx-foot-add" disabled={!canEdit} onClick={() => onAddOption(node.id)} title="Adicionar outra resposta possível" type="button">
            <Plus size={13} /> saída
          </button>
          {!isRoot && (
            <button className="vx-foot-del" disabled={!canEdit} onClick={() => onDeleteStep(node.id)} title="Remover esta etapa" type="button">
              <Trash2 size={13} />
            </button>
          )}
        </div>
      </div>
    );
  }

  function renderEndNode(node: NodeDto) {
    const position = positionOf(node);
    const active = selectedNodeId === node.id;
    const reportText = currentReportText(node);
    const pending = !reportText.trim();
    const acc = accMap[node.id] ?? blankScores(paddedComps);

    return (
      <div key={node.id} className={`vx-node vx-end ${active ? "vx-node-on" : ""}`} style={{ left: position.x, top: position.y, width: NODE_W }} onPointerDown={(event) => onNodeDown(event, node)}>
        <span className="vx-port vx-port-in vx-port-end" title="Entrada" />
        <div className="vx-end-head">
          <span className="vx-id vx-id-end">
            <Flag size={12} />
            {node.id}
          </span>
          <span className="vx-badge vx-badge-final">encerramento</span>
          {pending && (
            <span className="vx-warn-mini" title="Falta o texto do relatório">
              <Flag size={11} />
            </span>
          )}
          <button className="vx-end-del" disabled={!canEdit} onClick={() => onDeleteStep(node.id)} title="Remover este encerramento" type="button">
            <Trash2 size={13} />
          </button>
        </div>
        <div className="vx-end-acc">
          <div className="vx-end-acc-head">Pontuação acumulada</div>
          {paddedComps.map((comp) => (
            <div key={comp.key} className="vx-end-acc-row">
              <span>{comp.label}</span>
              <span className="vx-end-acc-v">{acc[comp.key] ?? 0}</span>
            </div>
          ))}
        </div>
        <div className="vx-end-report">
          <div className="vx-end-report-label">Relatório ao recrutador {pending && <em>*</em>}</div>
          <textarea
            className={pending ? "vx-end-report-warn" : ""}
            disabled={!canEdit}
            value={reportText}
            onChange={(event) => updateReportText(node, event.target.value)}
            placeholder="Resumo enviado ao recrutador ao fim deste caminho..."
          />
        </div>
      </div>
    );
  }

  function renderEmptyInspector() {
    return (
      <div className="vx-insp-empty">
        <Workflow size={22} />
        <p>Selecione um card no mapa para editar.</p>
        <span>As notas das competências se editam no próprio card.</span>
      </div>
    );
  }

  function renderEndInspector(node: NodeDto) {
    const reportText = currentReportText(node);
    const acc = accMap[node.id] ?? blankScores(paddedComps);
    return (
      <div className="vx-insp-body">
        <div className="vx-insp-head">
          <span className="vx-id vx-id-lg vx-id-end">
            <Flag size={13} />
            {node.id}
          </span>
          <button className="vx-x" onClick={() => onSelectNode(null)} type="button">
            <X size={15} />
          </button>
        </div>
        <div className="vx-insp-tag">Card de encerramento</div>
        <div className="vx-insp-accbox">
          <div className="vx-insp-accbox-h">Pontuação acumulada deste caminho</div>
          {paddedComps.map((comp) => (
            <div key={comp.key} className="vx-insp-acc-row">
              <span>{comp.label}</span>
              <b>{acc[comp.key] ?? 0}</b>
            </div>
          ))}
        </div>
        <label className={`vx-field ${!reportText.trim() ? "vx-field-warn" : ""}`}>
          <span>Texto do relatório (vai para o recrutador) *</span>
          <textarea disabled={!canEdit} rows={7} value={reportText} onChange={(event) => updateReportText(node, event.target.value)} placeholder="Resumo enviado ao recrutador ao fim deste caminho..." />
          {!reportText.trim() && <em className="vx-hint-warn">Obrigatório: todo encerramento precisa do texto do relatório.</em>}
        </label>
        <button className="vx-del-btn" disabled={!canEdit} onClick={() => onDeleteStep(node.id)} type="button">
          <Trash2 size={14} /> Remover encerramento
        </button>
      </div>
    );
  }

  function renderStepInspector(node: NodeDto) {
    return (
      <div className="vx-insp-body">
        <div className="vx-insp-head">
          <span className="vx-id vx-id-lg">
            {node.id === rootId ? <Crosshair size={13} /> : <Workflow size={13} />}
            {node.id}
          </span>
          <button className="vx-x" onClick={() => onSelectNode(null)} type="button">
            <X size={15} />
          </button>
        </div>
        <label className="vx-field">
          <span>Fala da etapa</span>
          <textarea disabled={!canEdit} rows={3} value={currentNodeText(node)} onChange={(event) => updateNodeText(node, event.target.value)} />
        </label>
        <label className="vx-field">
          <span>Tempo de resposta (s)</span>
          <input
            disabled={!canEdit}
            min={1}
            type="number"
            value={nodeDrafts[node.id]?.timeLimitSeconds ?? String(node.timeLimitSeconds ?? "")}
            onBlur={() => commitTimeLimit(node)}
            onChange={(event) => updateTimeLimitDraft(node, event.target.value)}
          />
        </label>
        <label className={`vx-field ${!node.timeoutNextNodeId ? "vx-field-warn" : ""}`}>
          <span>Quando o tempo acaba, vai para *</span>
          <select disabled={!canEdit} value={targetValue(node.timeoutNextNodeId)} onChange={(event) => setTimeoutTarget(node, event.target.value)}>
            <option value="" disabled>
              — defina —
            </option>
            <option value={FIM}>Fim (cria encerramento)</option>
            {forwardSteps(node).map((target) => (
              <option key={target.id} value={target.id}>
                → {target.id}
              </option>
            ))}
            <option value={NEW}>＋ criar nova etapa</option>
          </select>
          {!node.timeoutNextNodeId && <em className="vx-hint-warn">Obrigatório: etapa adiante ou Fim.</em>}
        </label>
        <div className="vx-insp-sub">Saídas (respostas possíveis)</div>
        {node.options.map((option) => (
          <div key={option.id} className="vx-opt-card">
            <input className="vx-opt-input" disabled={!canEdit} value={currentOptionText(node, option)} onChange={(event) => updateOptionText(node, option, event.target.value)} />
            <div className="vx-opt-meta">
              <select disabled={!canEdit} value={targetValue(option.nextNodeId)} onChange={(event) => setOptionTarget(node, option, event.target.value)}>
                <option value="" disabled>
                  — defina —
                </option>
                <option value={FIM}>Fim (cria encerramento)</option>
                {forwardSteps(node).map((target) => (
                  <option key={target.id} value={target.id}>
                    → {target.id}
                  </option>
                ))}
                <option value={NEW}>＋ nova etapa</option>
              </select>
              <button className="vx-opt-del" disabled={!canEdit} onClick={() => onDeleteOption(node.id, option.id)} title="Remover esta saída" type="button">
                <Trash2 size={13} />
              </button>
            </div>
          </div>
        ))}
        <button className="vx-add-out" disabled={!canEdit} onClick={() => onAddOption(node.id)} type="button">
          <Plus size={14} /> Adicionar saída
        </button>
      </div>
    );
  }

  function renderTableRow(node: NodeDto) {
    const active = selectedNodeId === node.id;
    const acc = accMap[node.id] ?? blankScores(paddedComps);
    const score = nodeCardScores(node, paddedComps, optionDrafts);
    const forward = forwardSteps(node);
    return (
      <tr className={`vx-tr ${active ? "vx-tr-on" : ""}`} key={node.id} onClick={() => onSelectNode(node.id)}>
        <td className="vx-td-step">
          <span className="vx-id">
            {node.id === rootId ? <Crosshair size={11} /> : <Workflow size={11} />}
            {node.id}
          </span>
        </td>
        <td>
          <input className="vx-cell" disabled={!canEdit} value={currentNodeText(node)} onClick={(event) => event.stopPropagation()} onChange={(event) => updateNodeText(node, event.target.value)} />
        </td>
        {paddedComps.map((comp) => (
          <td key={comp.key} className="vx-td-n">
            <input
              className="vx-cell vx-cell-n"
              disabled={!canEdit || comps.length === 0}
              max={100}
              min={0}
              type="number"
              value={score[comp.key] ?? 0}
              onClick={(event) => event.stopPropagation()}
              onChange={(event) => setCardScore(node, comp.key, event.target.value)}
            />
          </td>
        ))}
        <td className="vx-td-acc">
          {paddedComps.map((comp) => (
            <span key={comp.key}>{acc[comp.key] ?? 0}</span>
          ))}
        </td>
        <td className="vx-td-out">
          {node.options.length === 0 ? (
            <span className="vx-out-none">sem saídas</span>
          ) : (
            node.options.map((option) => (
              <div key={option.id} className="vx-out-line">
                <span className="vx-out-text" title={currentOptionText(node, option)}>
                  {currentOptionText(node, option)}
                </span>
                <select className="vx-out-sel" disabled={!canEdit} value={targetValue(option.nextNodeId)} onClick={(event) => event.stopPropagation()} onChange={(event) => setOptionTarget(node, option, event.target.value)}>
                  <option value="" disabled>
                    —
                  </option>
                  <option value={FIM}>Fim</option>
                  {forward.map((target) => (
                    <option key={target.id} value={target.id}>
                      {target.id}
                    </option>
                  ))}
                  <option value={NEW}>＋ nova</option>
                </select>
              </div>
            ))
          )}
        </td>
        <td className="vx-td-mid">
          <select
            className={`vx-cell vx-cell-to ${!node.timeoutNextNodeId ? "vx-cell-warn" : ""}`}
            disabled={!canEdit}
            value={targetValue(node.timeoutNextNodeId)}
            onClick={(event) => event.stopPropagation()}
            onChange={(event) => setTimeoutTarget(node, event.target.value)}
          >
            <option value="" disabled>
              — defina —
            </option>
            <option value={FIM}>Fim</option>
            {forward.map((target) => (
              <option key={target.id} value={target.id}>
                {target.id}
              </option>
            ))}
            <option value={NEW}>＋ nova</option>
          </select>
        </td>
      </tr>
    );
  }

  function positionOf(node: NodeDto): Point {
    const local = localPositions[node.id];
    if (local) return local;
    if (node.positionX != null && node.positionY != null) return { x: node.positionX, y: node.positionY };
    return autoPositions[node.id] ?? { x: 60, y: 80 };
  }

  function nodeHeight(node: NodeDto) {
    return isFinal(node) ? END_HEIGHT : OPT_START + node.options.length * OPT_H + TIMEOUT_H + FOOTER_H;
  }

  function inputPort(node: NodeDto): Point {
    const position = positionOf(node);
    return { x: position.x, y: position.y + (isFinal(node) ? END_HEAD_H / 2 : MSG_H / 2) };
  }

  function outPort(node: NodeDto, index: number): Point {
    const position = positionOf(node);
    return { x: position.x + NODE_W, y: position.y + OPT_START + index * OPT_H + OPT_H / 2 };
  }

  function timeoutPort(node: NodeDto): Point {
    const position = positionOf(node);
    return { x: position.x + NODE_W, y: position.y + OPT_START + node.options.length * OPT_H + TIMEOUT_H / 2 };
  }

  function toCanvas(clientX: number, clientY: number): Point {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return { x: 0, y: 0 };
    return { x: (clientX - rect.left - pan.x) / zoom, y: (clientY - rect.top - pan.y) / zoom };
  }

  function nodeAt(x: number, y: number, excludeId: string) {
    for (let index = nodes.length - 1; index >= 0; index--) {
      const node = nodes[index];
      if (node.id === excludeId) continue;
      const position = positionOf(node);
      if (x >= position.x && x <= position.x + NODE_W && y >= position.y && y <= position.y + nodeHeight(node)) return node;
    }
    return null;
  }

  function forwardSteps(node: NodeDto) {
    return nodes.filter((target) => !isFinal(target) && target.id !== node.id);
  }

  function currentNodeText(node: NodeDto) {
    return nodeDrafts[node.id]?.clientMessage ?? node.clientMessage ?? "";
  }

  function currentReportText(node: NodeDto) {
    return nodeDrafts[node.id]?.reportText ?? node.reportText ?? "";
  }

  function currentOptionText(node: NodeDto, option: OptionDto) {
    return optionDrafts[optionKey(node.id, option.id)]?.text ?? option.text ?? "";
  }

  function targetValue(nodeId: string | null | undefined) {
    if (!nodeId) return "";
    const target = nodeMap.get(nodeId);
    if (target && isFinal(target)) return FIM;
    return nodeId;
  }

  function onCanvasDown(event: ReactPointerEvent<HTMLDivElement>) {
    if (closest(event.target, ".vx-node,.vx-port,.vx-elabel,button,input,select,textarea")) return;
    onSelectNode(null);
    setDrag({ type: "pan", sx: event.clientX, sy: event.clientY, panX: pan.x, panY: pan.y });
  }

  function onNodeDown(event: ReactPointerEvent, node: NodeDto) {
    if (closest(event.target, ".vx-port,button,input,select,textarea")) return;
    event.stopPropagation();
    const position = positionOf(node);
    onSelectNode(node.id);
    dragLatest.current = position;
    setDrag({ type: "node", id: node.id, sx: event.clientX, sy: event.clientY, nx: position.x, ny: position.y });
  }

  function startConnect(event: ReactPointerEvent, node: NodeDto, optionIndex: number) {
    if (!canEdit) return;
    event.stopPropagation();
    setConn(outPort(node, optionIndex));
    setDrag({ type: "connect", fromNodeId: node.id, optionId: node.options[optionIndex].id });
  }

  function startConnectTimeout(event: ReactPointerEvent, node: NodeDto) {
    if (!canEdit) return;
    event.stopPropagation();
    setConn(timeoutPort(node));
    setDrag({ type: "connect-timeout", fromNodeId: node.id });
  }

  function setOptionTarget(node: NodeDto, option: OptionDto, value: string) {
    if (!canEdit) return;
    if (value === NEW) return onCreateChild(node.id, { via: "option", optionId: option.id }, false);
    if (value === FIM) return onCreateChild(node.id, { via: "option", optionId: option.id }, true);
    return onEditOption(node.id, option.id, { nextNodeId: value === "" ? "" : value });
  }

  function setTimeoutTarget(node: NodeDto, value: string) {
    if (!canEdit) return;
    if (value === NEW) return onCreateChild(node.id, { via: "timeout" }, false);
    if (value === FIM) return onCreateChild(node.id, { via: "timeout" }, true);
    return onUpdateNode(node.id, { timeoutNextNodeId: value === "" ? "" : value });
  }

  function unlinkOption(node: NodeDto, option: OptionDto) {
    if (!canEdit) return;
    const target = option.nextNodeId ? nodeMap.get(option.nextNodeId) : null;
    if (target && isFinal(target)) {
      onDeleteStep(target.id);
      return;
    }
    onEditOption(node.id, option.id, { nextNodeId: "" });
  }

  function unlinkTimeout(node: NodeDto) {
    if (!canEdit) return;
    const target = node.timeoutNextNodeId ? nodeMap.get(node.timeoutNextNodeId) : null;
    if (target && isFinal(target)) {
      onDeleteStep(target.id);
      return;
    }
    onUpdateNode(node.id, { timeoutNextNodeId: "" });
  }

  function scheduleNodeUpdate(nodeId: string, body: NodeUpdateBody) {
    if (!canEdit) return;
    window.clearTimeout(nodeTimers.current[nodeId]);
    nodeTimers.current[nodeId] = window.setTimeout(() => onUpdateNode(nodeId, body), 450);
  }

  function scheduleOptionUpdate(nodeId: string, optionId: string, body: UpdateOptionRequest) {
    if (!canEdit) return;
    const key = optionKey(nodeId, optionId);
    window.clearTimeout(optionTimers.current[key]);
    optionTimers.current[key] = window.setTimeout(() => onEditOption(nodeId, optionId, body), 450);
  }

  function updateNodeText(node: NodeDto, value: string) {
    setNodeDrafts((current) => ({ ...current, [node.id]: { ...current[node.id], clientMessage: value } }));
    scheduleNodeUpdate(node.id, { clientMessage: value });
  }

  function updateReportText(node: NodeDto, value: string) {
    setNodeDrafts((current) => ({ ...current, [node.id]: { ...current[node.id], reportText: value } }));
    scheduleNodeUpdate(node.id, { reportText: value });
  }

  function updateTimeLimitDraft(node: NodeDto, value: string) {
    setNodeDrafts((current) => ({ ...current, [node.id]: { ...current[node.id], timeLimitSeconds: value } }));
  }

  function commitTimeLimit(node: NodeDto) {
    const value = nodeDrafts[node.id]?.timeLimitSeconds;
    if (value == null) return;
    onUpdateNode(node.id, { timeLimitSeconds: value.trim() === "" ? null : Number(value) });
  }

  function updateOptionText(node: NodeDto, option: OptionDto, value: string) {
    const key = optionKey(node.id, option.id);
    setOptionDrafts((current) => ({ ...current, [key]: { ...current[key], text: value } }));
    scheduleOptionUpdate(node.id, option.id, { text: value });
  }

  function setCardScore(node: NodeDto, competency: string, value: string) {
    if (!canEdit || comps.length === 0) return;
    const numeric = clamp(Number(value) || 0, 0, 100);
    node.options.forEach((option) => {
      const key = optionKey(node.id, option.id);
      const nextLevels = {
        ...option.competencyLevels,
        ...(optionDrafts[key]?.competencyLevels ?? {}),
        [competency]: numeric,
      };
      setOptionDrafts((current) => ({ ...current, [key]: { ...current[key], competencyLevels: nextLevels } }));
      scheduleOptionUpdate(node.id, option.id, { competencyLevels: nextLevels });
    });
  }
}

function nodeCardScores(node: NodeDto, comps: Array<{ key: string }>, optionDrafts: Record<string, { competencyLevels?: ScoreMap }>) {
  const result = blankScores(comps);
  comps.forEach((comp) => {
    result[comp.key] = node.options.reduce((max, option) => {
      const levels = optionDrafts[optionKey(node.id, option.id)]?.competencyLevels ?? option.competencyLevels ?? {};
      return Math.max(max, levels[comp.key] ?? 0);
    }, 0);
  });
  return result;
}

function blankScores(comps: Array<{ key: string }>) {
  return Object.fromEntries(comps.map((comp) => [comp.key, 0])) as ScoreMap;
}

function addScores(a: ScoreMap, b: ScoreMap, comps: Array<{ key: string }>) {
  return Object.fromEntries(comps.map((comp) => [comp.key, (a[comp.key] ?? 0) + (b[comp.key] ?? 0)])) as ScoreMap;
}

function edgePath(from: Point, to: Point) {
  const dx = Math.max(44, Math.abs(to.x - from.x) * 0.5);
  return `M ${from.x} ${from.y} C ${from.x + dx} ${from.y}, ${to.x - dx} ${to.y}, ${to.x} ${to.y}`;
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

function shortLabel(label: string) {
  const trimmed = label.trim();
  if (!trimmed) return "Comp";
  const words = trimmed.split(/\s+/);
  if (words.length > 1) return words.map((word) => word[0]).join("").slice(0, 3);
  return trimmed.slice(0, 3);
}

function closest(target: EventTarget, selector: string) {
  return target instanceof Element && Boolean(target.closest(selector));
}

const CSS = `
.vx-root *{box-sizing:border-box;margin:0;padding:0}
.vx-root{
  --bg:#FCFAF6;--panel:#FFFFFF;--ink:#172128;--muted:#616A71;--soft:#9AA3AA;
  --border:#D9DFE3;--line:#EBEEF0;
  --primary:#1B6C8C;--primary-weak:#E2F1FA;--primary-ink:#0E5570;
  --danger:#CC3E35;--danger-weak:#FAE7E5;
  --amber:#7A5410;--amber-weak:#FCF2DB;--amber-line:#E6C98A;--amber-solid:#E1A536;
  --violet:#8A6312;--violet-weak:#FBEFD4;--violet-line:#E4C57E;
  --green:#3B9555;--dot:#E6E3DA;
  font-family:system-ui,-apple-system,"Segoe UI",sans-serif;color:var(--ink);
  background:var(--bg);height:100%;min-height:840px;display:flex;flex-direction:column;
  border:1px solid var(--border);border-radius:14px;overflow:hidden}
.vx-root b{font-weight:650}

.vx-top{display:flex;align-items:center;gap:18px;padding:13px 18px;background:var(--panel);border-bottom:1px solid var(--border)}
.vx-eyebrow{font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:var(--primary);font-weight:600}
.vx-title{font-family:Georgia,"Times New Roman",serif;font-size:22px;font-weight:600;margin-top:1px}
.vx-counts{display:flex;align-items:center;gap:9px;font-size:13px;color:var(--muted);flex-wrap:wrap}
.vx-counts b{color:var(--ink)} .vx-counts span{display:inline-flex;align-items:center;gap:4px}
.vx-dot{width:3px;height:3px;border-radius:50%;background:var(--soft)}
.vx-warnpill{color:var(--amber);background:var(--amber-weak);border:1px solid var(--amber-line);padding:2px 9px;border-radius:999px;font-size:11.5px;font-weight:600}
.vx-tools{margin-left:auto;display:flex;align-items:center;gap:8px}
.vx-seg{display:flex;align-items:center;border:1px solid var(--border);border-radius:9px;background:var(--bg);overflow:hidden}
.vx-seg button{height:32px;min-width:32px;display:flex;align-items:center;justify-content:center;border:none;background:transparent;color:var(--muted);cursor:pointer}
.vx-seg button:hover{background:#fff;color:var(--ink)}
.vx-zlabel{font-size:12px;font-weight:600;font-variant-numeric:tabular-nums;padding:0 6px}
.vx-ghost{display:inline-flex;align-items:center;gap:6px;height:34px;padding:0 12px;border:1px solid var(--border);border-radius:9px;background:#fff;color:var(--ink);font-size:13px;font-weight:550;cursor:pointer}
.vx-ghost:hover{background:var(--bg)}

.vx-stage{display:flex;flex:1;min-height:0}
.vx-canvas{position:relative;flex:1;min-width:0;overflow:hidden;background:var(--bg);cursor:grab;touch-action:none}
.vx-canvas.vx-grabbing{cursor:grabbing}
.vx-content{position:absolute;top:0;left:0;width:${CANVAS_W}px;height:${CANVAS_H}px;transform-origin:0 0;background-image:radial-gradient(circle,var(--dot) 1.4px,transparent 1.4px);background-size:22px 22px}
.vx-edges{position:absolute;top:0;left:0;overflow:visible;pointer-events:none}
#vx-a path{fill:#aeb6c6} .vx-at{fill:var(--amber-solid)}
.vx-edge{fill:none;stroke:#b7bfce;stroke-width:1.8}
.vx-edge-on{stroke:var(--primary);stroke-width:2.4}
.vx-edge-end{stroke:var(--violet)}
.vx-edge-to{stroke:var(--amber-solid);stroke-dasharray:6 5;stroke-width:1.7}
.vx-edge-drag{stroke:var(--primary);stroke-width:2.2;stroke-dasharray:6 5}
.vx-edge-drag-to{stroke:var(--amber)}

.vx-elabel{position:absolute;z-index:6;display:inline-flex;align-items:center;gap:4px;max-width:160px;padding:3px 8px;border:1px solid var(--border);border-radius:999px;background:#fff;color:var(--ink);font-size:11px;font-weight:550;white-space:nowrap;cursor:pointer;box-shadow:0 1px 3px rgba(20,30,55,.08)}
.vx-elabel:hover{border-color:var(--primary);color:var(--primary-ink)}

.vx-node{position:absolute;z-index:10;background:#fff;border:1px solid var(--border);border-radius:12px;box-shadow:0 2px 6px rgba(20,30,55,.06);cursor:grab;user-select:none;transition:box-shadow .15s,border-color .15s}
.vx-node:hover{box-shadow:0 6px 18px rgba(20,30,55,.12)}
.vx-node-on{border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-weak),0 8px 22px rgba(27,108,140,.16)}
.vx-node-root .vx-msg{border-top:3px solid var(--primary);border-radius:12px 12px 0 0}
.vx-msg{height:${MSG_H}px;padding:8px 11px;border-bottom:1px solid var(--line)}
.vx-msg-head{display:flex;align-items:center;gap:5px;height:18px}
.vx-id{display:inline-flex;align-items:center;gap:4px;font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:11px;font-weight:650;color:var(--primary-ink)}
.vx-id-end{color:var(--violet)}
.vx-id-lg{font-size:13px}
.vx-badge{display:inline-flex;align-items:center;gap:2px;font-size:9px;font-weight:700;text-transform:uppercase;letter-spacing:.04em;color:var(--primary);background:var(--primary-weak);padding:1px 5px;border-radius:5px}
.vx-badge-final{color:var(--violet);background:var(--violet-weak)}
.vx-warn-mini{display:inline-flex;color:var(--amber)}
.vx-time{margin-left:auto;display:inline-flex;align-items:center;gap:3px;font-size:11px;color:var(--muted);font-variant-numeric:tabular-nums}
.vx-msg-text{margin-top:4px;font-size:12px;line-height:1.32;color:var(--ink);display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}

.vx-comp{height:${COMP_BLOCK}px;padding:0 11px;border-bottom:1px solid var(--line);background:#FCFBF7}
.vx-comp-head{height:${COMP_HEAD_H}px;display:flex;align-items:center;justify-content:space-between;font-size:9.5px;text-transform:uppercase;letter-spacing:.05em;color:var(--soft);font-weight:700}
.vx-comp-row{height:${COMP_ROW_H}px;display:flex;align-items:center;gap:7px}
.vx-comp-name{width:30px;font-size:11px;font-weight:600;color:var(--muted)}
.vx-comp-in{flex:1;min-width:0;height:18px;border:1px solid var(--border);border-radius:5px;background:#fff;font-size:11px;text-align:center;font-variant-numeric:tabular-nums;outline:none}
.vx-comp-in:focus{border-color:var(--primary);box-shadow:0 0 0 2px var(--primary-weak)}
.vx-comp-acc{width:34px;text-align:right;font-size:11.5px;font-weight:700;color:var(--primary-ink);font-variant-numeric:tabular-nums}

.vx-optlabel{height:${LABEL_H}px;display:flex;align-items:center;padding:0 11px;font-size:10px;text-transform:uppercase;letter-spacing:.05em;color:var(--soft);font-weight:600}
.vx-opt{position:relative;height:${OPT_H}px;display:flex;align-items:center;gap:6px;padding:0 11px;border-top:1px solid var(--line)}
.vx-opt-text{flex:1;min-width:0;font-size:12px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.vx-cardsel{flex:none;width:80px;height:24px;border:1px solid var(--border);border-radius:6px;background:#fff;color:var(--ink);font-size:11px;font-family:ui-monospace,monospace;padding:0 2px 0 5px;outline:none;cursor:pointer}
.vx-cardsel:hover{border-color:var(--primary)} .vx-cardsel:focus{border-color:var(--primary);box-shadow:0 0 0 2px var(--primary-weak)}
.vx-cardsel-to{flex:1;min-width:0;color:var(--amber);border-color:var(--amber-line)}
.vx-mini-add{flex:none;display:inline-flex;align-items:center;justify-content:center;width:22px;height:22px;border:1px solid var(--border);border-radius:6px;background:var(--primary-weak);color:var(--primary);cursor:pointer}
.vx-mini-add:hover{background:#D2E8F2;border-color:var(--primary)}
.vx-mini-add-to{background:var(--amber-weak);color:var(--amber);border-color:var(--amber-line)}
.vx-mini-del{flex:none;display:inline-flex;align-items:center;justify-content:center;width:22px;height:22px;border:1px solid var(--border);border-radius:6px;background:var(--danger-weak);color:var(--danger);cursor:pointer}
.vx-mini-del:hover{background:#F6D9D6;border-color:var(--danger)}
.vx-timeout{position:relative;height:${TIMEOUT_H}px;display:flex;align-items:center;gap:6px;padding:0 11px;border-top:1px solid var(--line);background:#FCFBF7}
.vx-timeout svg{color:var(--amber-solid);flex:none}
.vx-timeout-l{flex:none;font-size:10.5px;font-weight:600;text-transform:uppercase;letter-spacing:.04em;color:var(--soft)}
.vx-timeout-warn{background:var(--amber-weak);box-shadow:inset 2px 0 0 var(--amber-solid)}
.vx-foot{height:${FOOTER_H}px;display:flex;align-items:center;gap:6px;padding:0 9px;border-top:1px solid var(--line)}
.vx-foot-add{display:inline-flex;align-items:center;gap:4px;height:24px;padding:0 9px;border:1px dashed var(--border);border-radius:7px;background:transparent;color:var(--primary);font-size:11px;font-weight:600;cursor:pointer}
.vx-foot-add:hover{background:var(--primary-weak);border-color:var(--primary)}
.vx-foot-del{margin-left:auto;display:inline-flex;align-items:center;justify-content:center;height:24px;width:24px;border:1px solid var(--border);border-radius:7px;background:#fff;color:var(--danger);cursor:pointer}
.vx-foot-del:hover{background:var(--danger-weak);border-color:var(--danger)}

.vx-end{border-color:var(--violet-line)}
.vx-end.vx-node-on{border-color:var(--primary)}
.vx-end-head{height:${END_HEAD_H}px;display:flex;align-items:center;gap:6px;padding:0 11px;background:var(--violet-weak);border-bottom:1px solid var(--violet-line);border-radius:12px 12px 0 0}
.vx-end-del{margin-left:auto;display:inline-flex;align-items:center;justify-content:center;height:24px;width:24px;border:1px solid var(--violet-line);border-radius:7px;background:#fff;color:var(--danger);cursor:pointer}
.vx-end-del:hover{background:var(--danger-weak);border-color:var(--danger)}
.vx-end-acc{height:${END_ACC_H}px;padding:6px 11px;border-bottom:1px solid var(--line)}
.vx-end-acc-head{height:${COMP_HEAD_H}px;display:flex;align-items:center;font-size:9.5px;text-transform:uppercase;letter-spacing:.05em;color:var(--soft);font-weight:700}
.vx-end-acc-row{height:${COMP_ROW_H}px;display:flex;align-items:center;justify-content:space-between;font-size:11.5px;color:var(--muted)}
.vx-end-acc-v{font-weight:700;color:var(--violet);font-variant-numeric:tabular-nums}
.vx-end-report{height:${END_REPORT_H}px;padding:8px 11px;display:flex;flex-direction:column}
.vx-end-report-label{font-size:10px;text-transform:uppercase;letter-spacing:.05em;color:var(--soft);font-weight:700;margin-bottom:5px}
.vx-end-report-label em{color:var(--amber);font-style:normal}
.vx-end-report textarea{flex:1;width:100%;resize:none;border:1px solid var(--border);border-radius:8px;padding:7px 8px;font-size:12px;line-height:1.4;font-family:inherit;color:var(--ink);outline:none}
.vx-end-report textarea:focus{border-color:var(--violet);box-shadow:0 0 0 3px var(--violet-weak)}
.vx-end-report-warn{border-color:var(--amber-line);background:var(--amber-weak)}

.vx-port{position:absolute;width:12px;height:12px;border-radius:50%;background:#fff;border:2px solid var(--primary);z-index:12}
.vx-port-in{left:-7px;top:${MSG_H / 2}px;transform:translateY(-50%);border-color:#aeb6c6}
.vx-port-end{top:${END_HEAD_H / 2}px;border-color:var(--violet)}
.vx-port-out{right:-7px;top:50%;transform:translateY(-50%);cursor:crosshair}
.vx-port-out:hover{background:var(--primary);transform:translateY(-50%) scale(1.25)}
.vx-port-to{right:-7px;top:50%;transform:translateY(-50%);border-color:var(--amber);cursor:crosshair}
.vx-port-to:hover{background:var(--amber);transform:translateY(-50%) scale(1.25)}

.vx-legend{position:absolute;left:14px;bottom:12px;display:flex;align-items:center;gap:14px;flex-wrap:wrap;background:rgba(245,246,249,.92);border:1px solid var(--border);border-radius:9px;padding:6px 11px;backdrop-filter:blur(3px);max-width:96%}
.vx-leg{display:inline-flex;align-items:center;gap:6px;font-size:11px;color:var(--ink);font-weight:550}
.vx-leg svg{color:var(--violet)}
.vx-sw{width:18px;height:0;border-top-width:2px;border-top-style:dashed;border-color:var(--amber);display:inline-block}
.vx-leg-hint{font-size:10.5px;color:var(--soft)}

.vx-inspector{width:332px;flex:none;background:var(--panel);border-left:1px solid var(--border);overflow-y:auto}
.vx-insp-empty{height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10px;padding:30px;text-align:center;color:var(--muted)}
.vx-insp-empty svg{color:var(--soft)} .vx-insp-empty p{font-size:13.5px;line-height:1.45;color:var(--ink)} .vx-insp-empty span{font-size:11.5px;color:var(--soft)}
.vx-insp-body{padding:16px}
.vx-insp-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
.vx-x{display:inline-flex;align-items:center;justify-content:center;height:28px;width:28px;border:1px solid var(--border);border-radius:8px;background:#fff;color:var(--muted);cursor:pointer}
.vx-x:hover{background:var(--bg);color:var(--ink)}
.vx-insp-tag{display:inline-block;font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.05em;color:var(--violet);background:var(--violet-weak);padding:3px 8px;border-radius:6px;margin-bottom:12px}
.vx-insp-accbox{border:1px solid var(--violet-line);background:var(--violet-weak);border-radius:10px;padding:10px 11px;margin-bottom:14px}
.vx-insp-accbox-h{font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.04em;color:var(--violet);margin-bottom:6px}
.vx-insp-acc-row{display:flex;align-items:center;justify-content:space-between;font-size:12.5px;color:var(--ink);padding:2px 0}
.vx-insp-acc-row b{color:var(--violet);font-variant-numeric:tabular-nums}
.vx-field{display:block;margin-bottom:12px}
.vx-field>span{display:block;font-size:11px;font-weight:600;color:var(--muted);margin-bottom:5px}
.vx-field textarea,.vx-field input,.vx-field select{width:100%;border:1px solid var(--border);border-radius:8px;background:#fff;padding:7px 9px;font-size:13px;color:var(--ink);font-family:inherit;outline:none}
.vx-field textarea{resize:vertical;line-height:1.4}
.vx-field textarea:focus,.vx-field input:focus,.vx-field select:focus{border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-weak)}
.vx-field-warn select,.vx-field-warn textarea{border-color:var(--amber-line);background:var(--amber-weak)}
.vx-hint-warn{display:block;margin-top:5px;font-size:11px;color:var(--amber);font-style:normal}
.vx-insp-sub{font-size:11px;text-transform:uppercase;letter-spacing:.06em;color:var(--soft);font-weight:700;margin:4px 0 8px}
.vx-opt-card{border:1px solid var(--border);border-radius:10px;padding:9px;margin-bottom:9px;background:var(--bg)}
.vx-opt-input{width:100%;border:1px solid var(--border);border-radius:7px;padding:6px 8px;font-size:12.5px;font-family:inherit;background:#fff;outline:none}
.vx-opt-input:focus{border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-weak)}
.vx-opt-meta{display:flex;align-items:center;gap:7px;margin-top:7px}
.vx-opt-meta select{flex:1;border:1px solid var(--border);border-radius:7px;padding:5px 6px;font-size:12px;background:#fff;font-family:inherit;outline:none}
.vx-opt-del{display:inline-flex;align-items:center;justify-content:center;height:28px;width:28px;flex:none;border:1px solid var(--border);border-radius:7px;background:#fff;color:var(--danger);cursor:pointer}
.vx-opt-del:hover{background:var(--danger-weak);border-color:var(--danger)}
.vx-add-out{width:100%;display:inline-flex;align-items:center;justify-content:center;gap:6px;height:34px;border:1px dashed var(--primary);border-radius:9px;background:var(--primary-weak);color:var(--primary-ink);font-size:12.5px;font-weight:600;cursor:pointer}
.vx-del-btn{width:100%;display:inline-flex;align-items:center;justify-content:center;gap:6px;height:34px;border:1px solid var(--danger);border-radius:9px;background:#fff;color:var(--danger);font-size:12.5px;font-weight:600;cursor:pointer;margin-top:4px}
.vx-del-btn:hover{background:var(--danger-weak)}

.vx-tablewrap{flex:none;max-height:42%;display:flex;flex-direction:column;background:var(--panel);border-top:1px solid var(--border)}
.vx-table-head{display:flex;align-items:center;justify-content:space-between;padding:13px 18px 11px}
.vx-table-head h2{font-family:Georgia,serif;font-size:16px;font-weight:600}
.vx-table-head p{font-size:12px;color:var(--muted);margin-top:2px}
.vx-table-count{font-size:11px;font-weight:600;color:var(--muted);background:var(--bg);border:1px solid var(--border);padding:3px 9px;border-radius:999px;white-space:nowrap}
.vx-table-empty{padding:26px 18px 30px;color:var(--soft);font-size:13px}
.vx-table-scroll{overflow:auto;border-top:1px solid var(--line)}
.vx-table{width:100%;border-collapse:collapse;font-size:12.5px}
.vx-table thead th{position:sticky;top:0;background:#FBFAF6;text-align:left;font-size:10.5px;text-transform:uppercase;letter-spacing:.05em;color:var(--soft);font-weight:700;padding:8px 10px;border-bottom:1px solid var(--border);white-space:nowrap;z-index:2}
.vx-th-step{width:96px} .vx-th-n{width:58px;text-align:center} .vx-th-acc{width:92px;text-align:center} .vx-th-out{width:260px} .vx-th-mid{width:96px}
.vx-tr{border-bottom:1px solid var(--line);cursor:pointer} .vx-tr:hover{background:#FBFAF6}
.vx-tr-on{background:var(--primary-weak)} .vx-tr-on:hover{background:var(--primary-weak)}
.vx-table td{padding:5px 10px;vertical-align:top}
.vx-td-step{padding-top:9px}
.vx-td-step .vx-id{display:flex;align-items:center;gap:4px}
.vx-cell{width:100%;border:1px solid transparent;border-radius:6px;padding:5px 7px;font-size:12.5px;font-family:inherit;background:transparent;color:var(--ink);outline:none}
.vx-cell:hover{border-color:var(--border);background:#fff}
.vx-cell:focus{border-color:var(--primary);background:#fff;box-shadow:0 0 0 3px var(--primary-weak)}
.vx-td-n{text-align:center} .vx-cell-n{text-align:center;font-variant-numeric:tabular-nums}
.vx-td-acc{text-align:center}
.vx-td-acc span{display:inline-block;min-width:24px;margin:0 2px;font-weight:700;color:var(--primary-ink);font-variant-numeric:tabular-nums;font-size:12px}
.vx-td-out{vertical-align:top}
.vx-out-line{display:flex;align-items:center;gap:6px;padding:2px 0}
.vx-out-text{flex:1;min-width:0;font-size:12px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;color:var(--ink)}
.vx-out-sel{flex:none;width:78px;border:1px solid var(--border);border-radius:6px;padding:3px 4px;font-size:11px;font-family:ui-monospace,monospace;background:#fff;outline:none;cursor:pointer}
.vx-out-sel:focus{border-color:var(--primary)}
.vx-out-none{font-size:11.5px;color:var(--soft);font-style:italic}
.vx-td-mid select.vx-cell{font-family:ui-monospace,monospace;font-size:11.5px}
.vx-cell-to{color:var(--amber)} .vx-cell-warn{border-color:var(--amber-line);background:var(--amber-weak)}

button:disabled,input:disabled,select:disabled,textarea:disabled{cursor:not-allowed;opacity:.65}

@media (max-width:980px){.vx-inspector{width:290px}}
`;
