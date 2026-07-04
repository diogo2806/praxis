/**
 * Práxis · FlowCanvas — mapa do validador.
 *
 * UX ajustada para fluxos grandes: navegação lateral, busca, foco de caminho,
 * enquadramento dinâmico e canvas maior para árvores com até 20 etapas por caminho.
 */
import { useEffect, useMemo, useRef, useState } from "react";
import {
  ChevronDown,
  Clock,
  Crosshair,
  Flag,
  ListTree,
  Maximize2,
  Plus,
  Search,
  Target,
  Timer,
  Trash2,
  User,
  Workflow,
  X,
  ZoomIn,
  ZoomOut,
} from "lucide-react";
import type {
  SimulationVersionDetailResponse,
  UpdateNodeRequest,
  UpdateOptionRequest,
  ValidationIssueResponse,
} from "@/lib/api/praxis";
import { buildStepLabels, localizeStepIds } from "@/lib/step-labels";

type Version = SimulationVersionDetailResponse;
type NodeDto = Version["nodes"][number];
type OptionDto = NodeDto["options"][number];
type Pos = { x: number; y: number };
type CompW = { name: string; weight: number; short: string };

export type NodeUpdateBody = UpdateNodeRequest;

export interface FlowCanvasProps {
  version: Version;
  canEdit: boolean;
  selectedNodeId: string | null;
  onSelectNode: (id: string | null) => void;
  onUpdateNode: (nodeId: string, body: NodeUpdateBody) => void;
  onEditOption: (nodeId: string, optionId: string, body: UpdateOptionRequest) => void;
  onAddOption: (nodeId: string) => void;
  onDeleteOption: (nodeId: string, optionId: string) => void;
  onDeleteStep: (nodeId: string) => void;
  onCreateChild: (
    parentNodeId: string,
    link: { via: "option"; optionId: string } | { via: "timeout" },
    asEnd: boolean,
  ) => void;
  validationIssues?: ValidationIssueResponse[];
}

const NODE_W = 350;
const MSG_H = 76;
const LABEL_H = 24;
const OPT_TEXT_H = 35;
const OPT_DEST_H = 40;
const OPT_SCORE_ROW = 26;
const OPT_BOTTOM = 24;
const OPT_PORT = OPT_TEXT_H + OPT_DEST_H / 2;
const TIMEOUT_H = 40;
const FOOTER_H = 38;
const OPT_START = MSG_H + LABEL_H;
const END_HEAD_H = 44;
const END_NOTE_MIN_H = 84;
const END_NOTE_ROW = 15;
const END_NOTE_BASE = 62;
const END_REPORT_H = 140;
const FIM = "__fim__";
const NEW = "__new__";
const LARGE_FLOW_THRESHOLD = 12;

const isEnd = (n?: NodeDto) => !!n && n.isFinal === true;
const clamp = (v: number, a: number, b: number) => Math.min(b, Math.max(a, v));
const shortName = (s: string) => {
  const t = s.trim();
  return t.length <= 4 ? t : t.slice(0, 3);
};
const normalizeStepId = (id: string | null | undefined) => {
  if (!id) return null;
  const match = /^etapa-(\d+)$/.exec(id);
  return match ? `turno-${match[1]}` : id;
};

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
  validationIssues,
}: FlowCanvasProps) {
  const nodes = useMemo(
    () => [...version.nodes].sort((a, b) => a.turnIndex - b.turnIndex || a.id.localeCompare(b.id)),
    [version.nodes],
  );
  const nodeById = useMemo(
    () => Object.fromEntries(nodes.map((n) => [n.id, n])) as Record<string, NodeDto>,
    [nodes],
  );
  const rootId = normalizeStepId(version.blueprint.rootNodeId);
  const resolvedRootId = rootId && nodeById[rootId] ? rootId : nodes.find((n) => !isEnd(n))?.id ?? nodes[0]?.id ?? null;
  const comps: CompW[] = useMemo(
    () =>
      (version.blueprint.competencies ?? [])
        .slice(0, 3)
        .map((c) => ({ name: c.name, weight: c.weight ?? 0, short: shortName(c.name) })),
    [version.blueprint.competencies],
  );

  const nComps = comps.length;
  const OPT_H = OPT_TEXT_H + OPT_DEST_H + nComps * OPT_SCORE_ROW + OPT_BOTTOM;
  const stepHeight = (n: NodeDto) => OPT_START + n.options.length * OPT_H + TIMEOUT_H + FOOTER_H;
  const END_NOTE_H = Math.max(END_NOTE_MIN_H, END_NOTE_BASE + nComps * END_NOTE_ROW);
  const END_HEIGHT = END_HEAD_H + END_NOTE_H + END_REPORT_H;
  const nodeHeight = (n: NodeDto) => (isEnd(n) ? END_HEIGHT : stepHeight(n));

  const [ctxOpen, setCtxOpen] = useState(() => nodes.length <= LARGE_FLOW_THRESHOLD);
  const [search, setSearch] = useState("");
  const [pathFocusId, setPathFocusId] = useState<string | null>(null);
  const [compactNavigator, setCompactNavigator] = useState(false);
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState<Pos>({ x: 24, y: 10 });
  const [drag, setDrag] = useState<any>(null);
  const [conn, setConn] = useState<Pos | null>(null);
  const [localPos, setLocalPos] = useState<Record<string, Pos>>({});
  const canvasRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (nodes.length > LARGE_FLOW_THRESHOLD) {
      setCtxOpen(false);
    }
  }, [nodes.length]);

  const issuesByNode = useMemo(() => {
    const map = new Map<string, { error: boolean; messages: string[] }>();
    for (const issue of validationIssues ?? []) {
      if (!issue.nodeId) continue;
      const entry = map.get(issue.nodeId) ?? { error: false, messages: [] };
      entry.error = entry.error || issue.severity === "blocker";
      entry.messages.push(issue.message);
      map.set(issue.nodeId, entry);
    }
    return map;
  }, [validationIssues]);
  const nodeIssue = (id: string) => issuesByNode.get(id) ?? null;

  const rootNode = resolvedRootId ? nodeById[resolvedRootId] : null;
  const critical = version.criticalSituation ?? null;
  const resultUse = version.resultUse ?? null;

  const incomingOf = (nodeId: string): { parent: NodeDto; option: OptionDto | null; via: "option" | "timeout" } | null => {
    for (const parent of nodes) {
      const option = parent.options.find((x) => x.nextNodeId === nodeId);
      if (option) return { parent, option, via: "option" };
    }
    for (const parent of nodes) {
      if (parent.timeoutNextNodeId === nodeId) return { parent, option: null, via: "timeout" };
    }
    return null;
  };

  const chainTo = (nodeId: string): NodeDto[] => {
    const chain: NodeDto[] = [];
    const seen = new Set<string>();
    let cur: string | null = nodeId;
    while (cur && !seen.has(cur)) {
      seen.add(cur);
      const n = nodeById[cur];
      if (!n) break;
      chain.unshift(n);
      const inc = incomingOf(cur);
      cur = inc ? inc.parent.id : null;
    }
    return chain;
  };

  const selectedChain = useMemo(() => (pathFocusId ? chainTo(pathFocusId) : []), [pathFocusId, nodes]);
  const focusedIds = useMemo(() => new Set(selectedChain.map((n) => n.id)), [selectedChain]);
  const hasPathFocus = focusedIds.size > 0;

  const pathScore = (nodeId: string): number | null => {
    const chain = chainTo(nodeId);
    if (chain.length < 2) return null;
    const raw: Record<string, number> = {};
    const max: Record<string, number> = {};
    comps.forEach((c) => {
      raw[c.name] = 0;
      max[c.name] = 0;
    });
    for (let i = 0; i < chain.length - 1; i++) {
      const step = chain[i];
      const next = chain[i + 1];
      const picked = step.options.find((o) => o.nextNodeId === next.id);
      comps.forEach((c) => {
        const best = step.options.reduce((m, o) => Math.max(m, o.competencyLevels?.[c.name] ?? 0), 0);
        max[c.name] += best;
        raw[c.name] += picked?.competencyLevels?.[c.name] ?? 0;
      });
    }
    let weightSum = 0;
    comps.forEach((c) => {
      if (max[c.name] > 0) weightSum += c.weight;
    });
    if (weightSum === 0) return 0;
    let weighted = 0;
    comps.forEach((c) => {
      if (max[c.name] > 0) weighted += (raw[c.name] / max[c.name]) * (c.weight / weightSum);
    });
    return Math.round(weighted * 100);
  };

  const perCompScore = (nodeId: string) => {
    const chain = chainTo(nodeId);
    return comps.map((c) => {
      let raw = 0;
      let max = 0;
      for (let i = 0; i < chain.length - 1; i++) {
        const step = chain[i];
        const next = chain[i + 1];
        const picked = step.options.find((o) => o.nextNodeId === next.id);
        max += step.options.reduce((m, o) => Math.max(m, o.competencyLevels?.[c.name] ?? 0), 0);
        raw += picked?.competencyLevels?.[c.name] ?? 0;
      }
      return { name: c.name, short: c.short, acc: raw, pct: max > 0 ? Math.round((raw / max) * 100) : null };
    });
  };

  const accTo = (nodeId: string): Record<string, number> => {
    const acc: Record<string, number> = {};
    comps.forEach((c) => {
      acc[c.name] = 0;
    });
    const chain = chainTo(nodeId);
    for (let i = 0; i < chain.length - 1; i++) {
      const picked = chain[i].options.find((o) => o.nextNodeId === chain[i + 1].id);
      comps.forEach((c) => {
        acc[c.name] += picked?.competencyLevels?.[c.name] ?? 0;
      });
    }
    return acc;
  };

  const stepLabels = useMemo(() => buildStepLabels(nodes), [nodes]);
  const labelOf = (id: string) => stepLabels.get(id) ?? id;
  const issueTitle = (issue: { messages: string[] }) => issue.messages.map((message) => localizeStepIds(message, stepLabels)).join("\n");

  const forwardTargets = (curId: string) => {
    const anc = new Set(chainTo(curId).map((n) => n.id));
    return nodes.filter((n) => !isEnd(n) && !anc.has(n.id)).sort((a, b) => a.turnIndex - b.turnIndex);
  };

  const setScore = (nodeId: string, opt: OptionDto, comp: string, raw: string) => {
    const levels: Record<string, number> = { ...(opt.competencyLevels ?? {}) };
    levels[comp] = clamp(Number(raw) || 0, 0, 100);
    onEditOption(nodeId, opt.id, { competencyLevels: levels });
  };

  const autoLayout = useMemo(() => {
    const depth: Record<string, number> = {};
    const visit = (id: string | null | undefined, d: number) => {
      if (!id || depth[id] != null) return;
      depth[id] = d;
      const n = nodeById[id];
      if (!n || isEnd(n)) return;
      n.options.forEach((o) => visit(o.nextNodeId, d + 1));
      visit(n.timeoutNextNodeId, d + 1);
    };
    if (resolvedRootId) visit(resolvedRootId, 0);

    const grouped = new Map<number, NodeDto[]>();
    nodes.forEach((n) => {
      const d = depth[n.id] ?? 0;
      grouped.set(d, [...(grouped.get(d) ?? []), n]);
    });

    const pos: Record<string, Pos> = {};
    Array.from(grouped.entries())
      .sort(([a], [b]) => a - b)
      .forEach(([d, items]) => {
        let y = 60;
        items
          .sort((a, b) => a.turnIndex - b.turnIndex || a.id.localeCompare(b.id))
          .forEach((n) => {
            pos[n.id] = { x: 60 + d * (NODE_W + 150), y };
            y += nodeHeight(n) + 56;
          });
      });
    return pos;
  }, [nodes, resolvedRootId, nodeById, OPT_H, END_HEIGHT]);

  const posOf = (n: NodeDto): Pos => {
    if (localPos[n.id]) return localPos[n.id];
    const px = n.positionX;
    const py = n.positionY;
    if (px != null && py != null) return { x: px, y: py };
    return autoLayout[n.id] ?? { x: 60, y: 60 };
  };

  const canvasBounds = useMemo(() => {
    if (!nodes.length) return { width: 1600, height: 900, minX: 0, minY: 0, maxX: 1600, maxY: 900 };
    const minX = Math.min(...nodes.map((n) => posOf(n).x));
    const minY = Math.min(...nodes.map((n) => posOf(n).y));
    const maxX = Math.max(...nodes.map((n) => posOf(n).x + NODE_W));
    const maxY = Math.max(...nodes.map((n) => posOf(n).y + nodeHeight(n)));
    return {
      minX,
      minY,
      maxX,
      maxY,
      width: Math.max(2200, maxX + 420),
      height: Math.max(1400, maxY + 360),
    };
  }, [nodes, autoLayout, localPos, OPT_H, END_HEIGHT]);

  const inPort = (n: NodeDto) => ({ x: posOf(n).x, y: posOf(n).y + (isEnd(n) ? END_HEAD_H / 2 : MSG_H / 2) });
  const outPort = (n: NodeDto, i: number) => ({ x: posOf(n).x + NODE_W, y: posOf(n).y + OPT_START + i * OPT_H + OPT_PORT });
  const toPort = (n: NodeDto) => ({ x: posOf(n).x + NODE_W, y: posOf(n).y + OPT_START + n.options.length * OPT_H + TIMEOUT_H / 2 });
  const edgePath = (s: Pos, t: Pos) => {
    const dx = Math.max(58, Math.abs(t.x - s.x) * 0.5);
    return `M ${s.x} ${s.y} C ${s.x + dx} ${s.y}, ${t.x - dx} ${t.y}, ${t.x} ${t.y}`;
  };

  const toCanvas = (cx: number, cy: number): Pos => {
    const r = canvasRef.current!.getBoundingClientRect();
    return { x: (cx - r.left - pan.x) / zoom, y: (cy - r.top - pan.y) / zoom };
  };

  const nodeAt = (cx: number, cy: number, exclude?: string): NodeDto | null => {
    for (let i = nodes.length - 1; i >= 0; i--) {
      const n = nodes[i];
      if (n.id === exclude) continue;
      const p = posOf(n);
      if (cx >= p.x && cx <= p.x + NODE_W && cy >= p.y && cy <= p.y + nodeHeight(n)) return n;
    }
    return null;
  };

  const setOptionTarget = (nodeId: string, opt: OptionDto, val: string) => {
    if (val === NEW) return onCreateChild(nodeId, { via: "option", optionId: opt.id }, false);
    if (val === FIM) return onCreateChild(nodeId, { via: "option", optionId: opt.id }, true);
    if (val === "") return;
    onEditOption(nodeId, opt.id, { nextNodeId: val });
  };
  const setTimeoutTarget = (n: NodeDto, val: string) => {
    if (val === NEW) return onCreateChild(n.id, { via: "timeout" }, false);
    if (val === FIM) return onCreateChild(n.id, { via: "timeout" }, true);
    if (val === "") return;
    onUpdateNode(n.id, { timeoutNextNodeId: val });
  };
  const optValue = (o: OptionDto) => (o.nextNodeId == null ? "" : isEnd(nodeById[o.nextNodeId]) ? FIM : o.nextNodeId);
  const toValue = (n: NodeDto) => (!n.timeoutNextNodeId ? "" : isEnd(nodeById[n.timeoutNextNodeId]) ? FIM : n.timeoutNextNodeId);

  const centerNode = (n: NodeDto) => {
    const r = canvasRef.current?.getBoundingClientRect();
    if (!r) return;
    const p = posOf(n);
    setPan({
      x: r.width / 2 - (p.x + NODE_W / 2) * zoom,
      y: r.height / 2 - (p.y + Math.min(nodeHeight(n), 420) / 2) * zoom,
    });
  };

  const selectAndCenter = (id: string) => {
    const n = nodeById[id];
    if (!n) return;
    onSelectNode(id);
    centerNode(n);
  };

  useEffect(() => {
    if (!drag) return;
    const onMove = (e: PointerEvent) => {
      if (drag.type === "pan") {
        setPan({ x: drag.panX + (e.clientX - drag.sx), y: drag.panY + (e.clientY - drag.sy) });
      } else if (drag.type === "node") {
        setLocalPos((p) => ({
          ...p,
          [drag.id]: { x: Math.max(0, drag.nx + (e.clientX - drag.sx) / zoom), y: Math.max(0, drag.ny + (e.clientY - drag.sy) / zoom) },
        }));
      } else {
        setConn(toCanvas(e.clientX, e.clientY));
      }
    };
    const onUp = (e: PointerEvent) => {
      if (drag.type === "node") {
        const p = localPos[drag.id];
        if (p && canEdit) onUpdateNode(drag.id, { positionX: Math.round(p.x), positionY: Math.round(p.y) });
      } else if (drag.type === "connect" || drag.type === "connect-timeout") {
        const c = toCanvas(e.clientX, e.clientY);
        const tgt = nodeAt(c.x, c.y, drag.fromNodeId);
        const isAncestor = tgt ? chainTo(drag.fromNodeId).some((a) => a.id === tgt.id) : false;
        if (tgt && !isEnd(tgt) && tgt.id !== drag.fromNodeId && !isAncestor) {
          if (drag.type === "connect-timeout") onUpdateNode(drag.fromNodeId, { timeoutNextNodeId: tgt.id });
          else onEditOption(drag.fromNodeId, drag.optionId, { nextNodeId: tgt.id });
        }
      }
      setDrag(null);
      setConn(null);
    };
    window.addEventListener("pointermove", onMove);
    window.addEventListener("pointerup", onUp);
    return () => {
      window.removeEventListener("pointermove", onMove);
      window.removeEventListener("pointerup", onUp);
    };
  }, [drag, zoom, pan, localPos, nodes]);

  useEffect(() => {
    const el = canvasRef.current;
    if (!el) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      const r = el.getBoundingClientRect();
      const cx = e.clientX - r.left;
      const cy = e.clientY - r.top;
      const nz = clamp(zoom * (e.deltaY < 0 ? 1.1 : 0.9), 0.3, 1.8);
      setPan({ x: cx - ((cx - pan.x) / zoom) * nz, y: cy - ((cy - pan.y) / zoom) * nz });
      setZoom(nz);
    };
    el.addEventListener("wheel", onWheel, { passive: false });
    return () => el.removeEventListener("wheel", onWheel);
  }, [zoom, pan]);

  const zoomBy = (f: number) => {
    const r = canvasRef.current!.getBoundingClientRect();
    const cx = r.width / 2;
    const cy = r.height / 2;
    const nz = clamp(zoom * f, 0.3, 1.8);
    setPan({ x: cx - ((cx - pan.x) / zoom) * nz, y: cy - ((cy - pan.y) / zoom) * nz });
    setZoom(nz);
  };

  const fit = () => {
    if (!nodes.length) return;
    const r = canvasRef.current!.getBoundingClientRect();
    const pad = 64;
    const nz = clamp(
      Math.min((r.width - pad * 2) / (canvasBounds.maxX - canvasBounds.minX), (r.height - pad * 2) / (canvasBounds.maxY - canvasBounds.minY)),
      0.3,
      1.35,
    );
    setZoom(nz);
    setPan({
      x: (r.width - (canvasBounds.maxX - canvasBounds.minX) * nz) / 2 - canvasBounds.minX * nz,
      y: (r.height - (canvasBounds.maxY - canvasBounds.minY) * nz) / 2 - canvasBounds.minY * nz,
    });
  };

  const onCanvasDown = (e: React.PointerEvent) => {
    const t = e.target as HTMLElement;
    if (t.closest(".vx-node") || t.closest(".vx-port") || t.closest(".vx-nav")) return;
    onSelectNode(null);
    setDrag({ type: "pan", sx: e.clientX, sy: e.clientY, panX: pan.x, panY: pan.y });
  };
  const onNodeDown = (e: React.PointerEvent, n: NodeDto) => {
    const t = e.target as HTMLElement;
    if (t.closest(".vx-port") || t.closest("button") || t.closest("input") || t.closest("select") || t.closest("textarea")) return;
    e.stopPropagation();
    onSelectNode(n.id);
    if (canEdit) setDrag({ type: "node", id: n.id, sx: e.clientX, sy: e.clientY, nx: posOf(n).x, ny: posOf(n).y });
  };

  const sel = selectedNodeId ? nodeById[selectedNodeId] : null;
  const connSource: NodeDto | null = drag?.fromNodeId ? nodeById[drag.fromNodeId] : null;
  const connStart = connSource
    ? drag.type === "connect-timeout"
      ? toPort(connSource)
      : outPort(connSource, connSource.options.findIndex((o) => o.id === drag.optionId))
    : null;

  const navigatorItems = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return nodes;
    return nodes.filter((n) => {
      const text = [labelOf(n.id), n.id, n.clientMessage, n.reportText, ...n.options.map((o) => o.text)].filter(Boolean).join(" ").toLowerCase();
      return text.includes(q);
    });
  }, [nodes, search, stepLabels]);

  const ScoreRow = ({ nodeId, opt, big }: { nodeId: string; opt: OptionDto; big?: boolean }) => {
    if (comps.length === 0) return <div className="vx-score-empty">defina competências no blueprint (etapa 1)</div>;
    const base = accTo(nodeId);
    return (
      <div className={`vx-opt-scores ${big ? "vx-opt-scores-lg" : ""}`}>
        {comps.map((c) => {
          const pts = opt.competencyLevels?.[c.name] ?? 0;
          const acc = (base[c.name] ?? 0) + pts;
          return (
            <div className="vx-score-row" key={c.name} title={c.name}>
              <span className="vx-sr-name">{c.short}</span>
              <input
                type="number"
                min={0}
                max={100}
                disabled={!canEdit}
                value={pts}
                onClick={(e) => e.stopPropagation()}
                onChange={(e) => setScore(nodeId, opt, c.name, e.target.value)}
              />
              <span className="vx-sr-acc">acum <b>{acc}</b></span>
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <div className="vx-root">
      <style>{CSS}</style>

      <div className="vx-ctx">
        <button className="vx-ctx-toggle" onClick={() => setCtxOpen((v) => !v)}>
          <ChevronDown size={15} className={ctxOpen ? "vx-rot" : ""} />
          Contexto da avaliação
          <span className="vx-ctx-name">{version.name}</span>
        </button>
        {ctxOpen && (
          <div className="vx-ctx-grid">
            <div className="vx-ctx-col">
              <div className="vx-ctx-tag"><Target size={12} /> Etapa 1 · Avaliação</div>
              <div className="vx-ctx-row"><b>Nome</b><span>{version.name || "—"}</span></div>
              <div className="vx-ctx-row"><b>Descrição</b><span>{version.description || "—"}</span></div>
              {resultUse && <div className="vx-ctx-row"><b>Uso do resultado</b><span>{resultUse}</span></div>}
              <div className="vx-ctx-row"><b>Competências</b><span className="vx-ctx-comps">{comps.length ? comps.map((c) => <i key={c.name}>{c.name}</i>) : "—"}</span></div>
            </div>
            <div className="vx-ctx-col">
              <div className="vx-ctx-tag"><User size={12} /> Etapa 2 · Cenário</div>
              <div className="vx-ctx-row"><b>Personagem</b><span>{rootNode?.speaker || "—"}</span></div>
              {critical && <div className="vx-ctx-row"><b>Situação crítica</b><span>{critical}</span></div>}
              <div className="vx-ctx-row"><b>Abertura</b><span className="vx-ctx-quote">{rootNode?.clientMessage || "—"}</span></div>
            </div>
          </div>
        )}
      </div>

      <div className="vx-bar">
        <div className="vx-counts">
          <span><b>{nodes.filter((n) => !isEnd(n)).length}</b> etapas</span>
          <span className="vx-dot" />
          <span><b>{nodes.filter(isEnd).length}</b> encerramentos</span>
          {nodes.length > LARGE_FLOW_THRESHOLD && <span className="vx-big">fluxo grande: use busca e foco de caminho</span>}
        </div>
        <div className="vx-tools">
          <div className="vx-seg">
            <button onClick={() => zoomBy(0.9)} title="Diminuir"><ZoomOut size={16} /></button>
            <button onClick={() => setZoom(1)} className="vx-zlabel">{Math.round(zoom * 100)}%</button>
            <button onClick={() => zoomBy(1.1)} title="Aumentar"><ZoomIn size={16} /></button>
          </div>
          <button className="vx-ghost" onClick={fit}><Maximize2 size={15} /> Enquadrar</button>
          {hasPathFocus && <button className="vx-ghost" onClick={() => setPathFocusId(null)}><X size={15} /> Limpar foco</button>}
        </div>
      </div>

      <div className="vx-stage">
        <aside className={`vx-nav ${compactNavigator ? "vx-nav-compact" : ""}`}>
          <div className="vx-nav-head">
            <span><ListTree size={15} /> Navegação</span>
            <button onClick={() => setCompactNavigator((v) => !v)}>{compactNavigator ? "Abrir" : "Fechar"}</button>
          </div>
          {!compactNavigator && (
            <>
              <label className="vx-search">
                <Search size={14} />
                <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar etapa, texto ou resposta…" />
              </label>
              <div className="vx-nav-list">
                {navigatorItems.map((n) => {
                  const issue = nodeIssue(n.id);
                  const chain = chainTo(n.id);
                  return (
                    <button
                      key={n.id}
                      className={`vx-nav-item ${selectedNodeId === n.id ? "vx-nav-on" : ""}`}
                      onClick={() => selectAndCenter(n.id)}
                    >
                      <span className="vx-nav-id">{isEnd(n) ? <Flag size={12} /> : n.id === resolvedRootId ? <Crosshair size={12} /> : <Workflow size={12} />}{labelOf(n.id)}</span>
                      <span className="vx-nav-text">{isEnd(n) ? n.reportText || "Encerramento sem relatório" : n.clientMessage || "Sem fala cadastrada"}</span>
                      <span className="vx-nav-meta">
                        {chain.length} no caminho · {n.options.length} saídas
                        {issue && <b className={issue.error ? "vx-nav-err" : "vx-nav-warn"}>{issue.error ? "erro" : "aviso"}</b>}
                      </span>
                    </button>
                  );
                })}
              </div>
            </>
          )}
        </aside>

        <div ref={canvasRef} className={`vx-canvas ${drag?.type === "pan" ? "vx-grabbing" : ""}`} onPointerDown={onCanvasDown}>
          <div className="vx-content" style={{ width: canvasBounds.width, height: canvasBounds.height, transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})` }}>
            <svg className="vx-edges" width={canvasBounds.width} height={canvasBounds.height}>
              <defs>
                <marker id="vx-a" markerWidth="9" markerHeight="9" refX="7.5" refY="4.5" orient="auto"><path d="M0,0 L9,4.5 L0,9 Z" /></marker>
                <marker id="vx-at" markerWidth="9" markerHeight="9" refX="7.5" refY="4.5" orient="auto"><path d="M0,0 L9,4.5 L0,9 Z" className="vx-at" /></marker>
              </defs>
              {nodes.filter((n) => !isEnd(n)).flatMap((n) =>
                n.options.map((o, i) => {
                  const t = o.nextNodeId ? nodeById[o.nextNodeId] : undefined;
                  if (!t) return null;
                  const on = selectedNodeId === n.id || selectedNodeId === t.id;
                  const focusOn = hasPathFocus && focusedIds.has(n.id) && focusedIds.has(t.id);
                  const dim = hasPathFocus && !focusOn;
                  return <path key={`${n.id}-${o.id}`} d={edgePath(outPort(n, i), inPort(t))} className={`vx-edge ${on || focusOn ? "vx-edge-on" : ""} ${dim ? "vx-edge-dim" : ""} ${isEnd(t) ? "vx-edge-end" : ""}`} markerEnd="url(#vx-a)" />;
                }),
              )}
              {nodes.filter((n) => !isEnd(n)).map((n) => {
                const t = n.timeoutNextNodeId ? nodeById[n.timeoutNextNodeId] : undefined;
                if (!t) return null;
                const focusOn = hasPathFocus && focusedIds.has(n.id) && focusedIds.has(t.id);
                const dim = hasPathFocus && !focusOn;
                return <path key={`${n.id}-to`} d={edgePath(toPort(n), inPort(t))} className={`vx-edge vx-edge-to ${focusOn ? "vx-edge-on" : ""} ${dim ? "vx-edge-dim" : ""}`} markerEnd="url(#vx-at)" />;
              })}
              {connStart && conn && <path d={edgePath(connStart, conn)} className={`vx-edge vx-edge-drag ${drag.type === "connect-timeout" ? "vx-edge-drag-to" : ""}`} />}
            </svg>

            {nodes.map((n) => {
              const p = posOf(n);
              const on = selectedNodeId === n.id;
              const issue = nodeIssue(n.id);
              const issueClass = issue ? (issue.error ? "vx-node-err" : "vx-node-warn") : "";
              const dimClass = hasPathFocus && !focusedIds.has(n.id) ? "vx-node-dim" : "";

              if (isEnd(n)) {
                const nota = pathScore(n.id);
                const report = n.reportText ?? "";
                const pending = !report.trim();
                return (
                  <div key={n.id} className={`vx-node vx-end ${on ? "vx-node-on" : ""} ${issueClass} ${dimClass}`} style={{ left: p.x, top: p.y, width: NODE_W }} onPointerDown={(e) => onNodeDown(e, n)}>
                    <span className="vx-port vx-port-in vx-port-end" />
                    <div className="vx-end-head">
                      <span className="vx-id vx-id-end"><Flag size={12} />{labelOf(n.id)}</span>
                      <span className="vx-badge vx-badge-final">encerramento</span>
                      {issue && <span className={`vx-vbadge ${issue.error ? "vx-vbadge-err" : "vx-vbadge-warn"}`} title={issueTitle(issue)}>{issue.error ? "🔴" : "🟡"} {issue.messages.length}</span>}
                      {pending && <span className="vx-warn-mini" title="Falta o relatório"><Flag size={11} /></span>}
                      <button className="vx-mini-focus" onClick={() => setPathFocusId(n.id)}>caminho</button>
                      {canEdit && <button className="vx-end-del" onClick={() => window.confirm("Remover este encerramento?") && onDeleteStep(n.id)} title="Remover encerramento"><Trash2 size={13} /></button>}
                    </div>
                    <div className="vx-end-note">
                      <span className="vx-end-note-l">Nota determinística por competência</span>
                      <div className="vx-end-comps">
                        {perCompScore(n.id).map((c) => (
                          <div className="vx-end-comp" key={c.name} title={c.name}>
                            <span className="vx-ec-name">{c.short}</span>
                            <span className="vx-ec-acc">acum {c.acc}</span>
                            <span className="vx-ec-pct">{c.pct == null ? "—" : <>{c.pct}<small>%</small></>}</span>
                          </div>
                        ))}
                      </div>
                      <span className="vx-end-note-final">final ponderada <b>{nota == null ? "—" : `${nota}%`}</b></span>
                    </div>
                    <div className="vx-end-report">
                      <div className="vx-end-report-label">Relatório para a equipe responsável{pending && <em>*</em>}</div>
                      <textarea className={pending ? "vx-end-report-warn" : ""} value={report} disabled={!canEdit} onChange={(e) => onUpdateNode(n.id, { reportText: e.target.value })} placeholder="Resumo apresentado à equipe responsável ao fim deste caminho…" />
                    </div>
                  </div>
                );
              }

              const isRoot = n.id === resolvedRootId;
              const noTo = !n.timeoutNextNodeId;
              const fwd = forwardTargets(n.id);
              return (
                <div key={n.id} className={`vx-node ${on ? "vx-node-on" : ""} ${isRoot ? "vx-node-root" : ""} ${issueClass} ${dimClass}`} style={{ left: p.x, top: p.y, width: NODE_W }} onPointerDown={(e) => onNodeDown(e, n)}>
                  <span className="vx-port vx-port-in" />
                  <div className="vx-msg">
                    <div className="vx-msg-head">
                      <span className="vx-id">{isRoot ? <Crosshair size={12} /> : <Workflow size={12} />}{labelOf(n.id)}</span>
                      {isRoot && <span className="vx-badge">início</span>}
                      {issue && <span className={`vx-vbadge ${issue.error ? "vx-vbadge-err" : "vx-vbadge-warn"}`} title={issueTitle(issue)}>{issue.error ? "🔴" : "🟡"} {issue.messages.length}</span>}
                      {noTo && <span className="vx-warn-mini" title="Falta a saída de tempo"><Timer size={11} /></span>}
                      <span className="vx-time"><Clock size={11} /> {n.timeLimitSeconds ?? "—"}s</span>
                    </div>
                    <p className="vx-msg-text">{n.clientMessage || "Sem fala cadastrada."}</p>
                  </div>

                  <div className="vx-optlabel">Saídas (respostas)</div>
                  {n.options.map((o, i) => (
                    <div key={o.id} className="vx-opt" style={{ height: OPT_H }}>
                      <div className="vx-opt-text" title={o.text}>{o.text || "—"}</div>
                      <div className="vx-opt-dest">
                        <select className="vx-cardsel" disabled={!canEdit} value={optValue(o)} onChange={(e) => setOptionTarget(n.id, o, e.target.value)}>
                          <option value="" disabled>— defina —</option>
                          <option value={FIM}>Fim (encerra)</option>
                          {fwd.map((t) => <option key={t.id} value={t.id}>→ {labelOf(t.id)}</option>)}
                          <option value={NEW}>＋ nova etapa</option>
                        </select>
                        {canEdit && <button className="vx-mini-del" onClick={() => onDeleteOption(n.id, o.id)} title="Remover resposta"><Trash2 size={12} /></button>}
                      </div>
                      <ScoreRow nodeId={n.id} opt={o} />
                      <span className="vx-port vx-port-out" onPointerDown={(e) => { e.stopPropagation(); setConn(outPort(n, i)); setDrag({ type: "connect", fromNodeId: n.id, optionId: o.id }); }} />
                    </div>
                  ))}

                  <div className={`vx-timeout ${noTo ? "vx-timeout-warn" : ""}`}>
                    <Timer size={12} />
                    <span className="vx-timeout-l">Tempo acaba</span>
                    <select className="vx-cardsel vx-cardsel-to" disabled={!canEdit} value={toValue(n)} onChange={(e) => setTimeoutTarget(n, e.target.value)}>
                      <option value="" disabled>— defina —</option>
                      <option value={FIM}>Fim (encerra)</option>
                      {fwd.map((t) => <option key={t.id} value={t.id}>→ {labelOf(t.id)}</option>)}
                      <option value={NEW}>＋ nova etapa</option>
                    </select>
                    {canEdit && (n.timeoutNextNodeId
                      ? <button className="vx-mini-del" onClick={() => (isEnd(nodeById[n.timeoutNextNodeId!]) ? window.confirm("Remover o encerramento ligado ao tempo?") && onDeleteStep(n.timeoutNextNodeId!) : onUpdateNode(n.id, { timeoutNextNodeId: "" }))} title="Remover ligação"><Trash2 size={12} /></button>
                      : <button className="vx-mini-add vx-mini-add-to" onClick={() => onCreateChild(n.id, { via: "timeout" }, false)} title="Criar fallback do tempo"><Plus size={12} /></button>)}
                    <span className="vx-port vx-port-to" onPointerDown={(e) => { e.stopPropagation(); setConn(toPort(n)); setDrag({ type: "connect-timeout", fromNodeId: n.id }); }} />
                  </div>

                  <div className="vx-foot">
                    {canEdit && <button className="vx-foot-add" onClick={() => onAddOption(n.id)}><Plus size={13} /> resposta</button>}
                    <button className="vx-mini-focus" onClick={() => setPathFocusId(n.id)}>focar caminho</button>
                    {canEdit && !isRoot && <button className="vx-foot-del" onClick={() => window.confirm("Remover esta etapa? Esta ação também remove suas ligações no fluxo.") && onDeleteStep(n.id)} title="Remover etapa"><Trash2 size={13} /></button>}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="vx-legend">
            <span className="vx-leg"><i className="vx-sw" /> tempo</span>
            <span className="vx-leg"><Flag size={12} /> encerramento · nota do caminho</span>
            <span className="vx-leg-hint">para fluxos grandes: busque a etapa, centralize e use foco de caminho</span>
          </div>
        </div>

        <aside className="vx-inspector">
          {!sel ? (
            <div className="vx-insp-empty"><Workflow size={22} /><p>Selecione um card no mapa ou na navegação.</p></div>
          ) : isEnd(sel) ? (
            <div className="vx-insp-body">
              <div className="vx-insp-head"><span className="vx-id vx-id-lg vx-id-end"><Flag size={13} />{labelOf(sel.id)}</span><button className="vx-x" onClick={() => onSelectNode(null)}><X size={15} /></button></div>
              <div className="vx-insp-tag">Card de encerramento</div>
              <div className="vx-insp-note">Final ponderada<b>{pathScore(sel.id) ?? "—"}<small>%</small></b></div>
              <button className="vx-focus-btn" onClick={() => setPathFocusId(sel.id)}>Destacar caminho até este encerramento</button>
              <div className="vx-insp-end-comps">
                {perCompScore(sel.id).map((c) => (
                  <span key={c.name} title={c.name}><b>{c.short}</b> acum {c.acc}<em>{c.pct == null ? "—" : `${c.pct}%`}</em></span>
                ))}
              </div>
              <label className={`vx-field ${!(sel.reportText ?? "").trim() ? "vx-field-warn" : ""}`} style={{ marginTop: 12 }}>
                <span>Texto do relatório para a equipe responsável *</span>
                <textarea rows={8} disabled={!canEdit} value={sel.reportText ?? ""} onChange={(e) => onUpdateNode(sel.id, { reportText: e.target.value })} />
              </label>
            </div>
          ) : (
            <div className="vx-insp-body">
              <div className="vx-insp-head"><span className="vx-id vx-id-lg">{sel.id === resolvedRootId ? <Crosshair size={13} /> : <Workflow size={13} />}{labelOf(sel.id)}</span><button className="vx-x" onClick={() => onSelectNode(null)}><X size={15} /></button></div>
              <button className="vx-focus-btn" onClick={() => setPathFocusId(sel.id)}>Destacar caminho até esta etapa</button>
              <label className="vx-field"><span>Fala da etapa</span><textarea rows={3} disabled={!canEdit} value={sel.clientMessage ?? ""} onChange={(e) => onUpdateNode(sel.id, { clientMessage: e.target.value })} /></label>
              <label className="vx-field"><span>Tempo de resposta (s)</span><input type="number" min={1} disabled={!canEdit} value={sel.timeLimitSeconds ?? ""} onChange={(e) => onUpdateNode(sel.id, { timeLimitSeconds: e.target.value === "" ? null : Number(e.target.value) })} /></label>
              {chainTo(sel.id).length > 1 && <div className="vx-insp-acc">acumulado até aqui:{comps.map((c) => { const a = accTo(sel.id); return <span key={c.name}>{c.short} <b>{a[c.name] ?? 0}</b></span>; })}</div>}

              <div className="vx-insp-sub">Saídas (respostas) · nota por resposta</div>
              {sel.options.map((o) => (
                <div key={o.id} className="vx-opt-card">
                  <input className="vx-opt-input" disabled={!canEdit} value={o.text} onChange={(e) => onEditOption(sel.id, o.id, { text: e.target.value })} placeholder="Texto da resposta…" />
                  <ScoreRow nodeId={sel.id} opt={o} big />
                  <div className="vx-opt-meta">
                    <select disabled={!canEdit} value={optValue(o)} onChange={(e) => setOptionTarget(sel.id, o, e.target.value)}>
                      <option value="" disabled>— destino —</option>
                      <option value={FIM}>Fim (cria encerramento)</option>
                      {forwardTargets(sel.id).map((t) => <option key={t.id} value={t.id}>→ {labelOf(t.id)}</option>)}
                      <option value={NEW}>＋ nova etapa</option>
                    </select>
                    {canEdit && <button onClick={() => onDeleteOption(sel.id, o.id)}><Trash2 size={12} /> remover</button>}
                  </div>
                </div>
              ))}
              {canEdit && <button className="vx-add-wide" onClick={() => onAddOption(sel.id)}><Plus size={14} /> adicionar resposta</button>}
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}

const CSS = `
.vx-root *{box-sizing:border-box}
.vx-root{--bg:#FCFAF6;--panel:#FFFFFF;--ink:#172128;--muted:#616A71;--soft:#9AA3AA;--border:#D9DFE3;--line:#EBEEF0;--primary:#1B6C8C;--primary-weak:#E2F1FA;--primary-ink:#0E5570;--danger:#CC3E35;--danger-weak:#FAE7E5;--amber:#7A5410;--amber-weak:#FCF2DB;--amber-line:#E6C98A;--amber-solid:#E1A536;--gold:#8A6312;--gold-weak:#FBEFD4;--gold-line:#E4C57E;--dot:#B9AF95;--canvas-bg:#F1ECE1;font-family:system-ui,-apple-system,"Segoe UI",sans-serif;color:var(--ink);display:flex;flex-direction:column;border:1px solid var(--border);border-radius:14px;overflow:hidden;background:var(--bg)}
.vx-root b{font-weight:650}.vx-ctx{background:var(--panel);border-bottom:1px solid var(--border)}
.vx-ctx-toggle{width:100%;display:flex;align-items:center;gap:8px;padding:10px 14px;border:none;background:transparent;font-size:12.5px;font-weight:700;color:var(--ink);cursor:pointer;text-transform:uppercase;letter-spacing:.04em}.vx-ctx-toggle svg{transition:transform .15s;color:var(--muted)}.vx-ctx-toggle .vx-rot{transform:rotate(180deg)}.vx-ctx-name{margin-left:auto;font-weight:600;text-transform:none;letter-spacing:0;color:var(--primary-ink);font-size:12.5px;max-width:50%;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.vx-ctx-grid{display:grid;grid-template-columns:1fr 1fr;gap:1px;background:var(--line);border-top:1px solid var(--line)}.vx-ctx-col{background:var(--panel);padding:11px 14px 13px}.vx-ctx-tag{display:inline-flex;align-items:center;gap:5px;font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.05em;color:var(--primary);background:var(--primary-weak);padding:3px 8px;border-radius:6px;margin-bottom:9px}.vx-ctx-row{display:grid;grid-template-columns:104px 1fr;gap:8px;padding:3px 0;font-size:12px;line-height:1.4}.vx-ctx-row b{color:var(--soft);font-weight:600;font-size:11px;text-transform:uppercase;letter-spacing:.03em;padding-top:1px}.vx-ctx-row span{color:var(--ink)}.vx-ctx-quote{font-style:italic;color:var(--muted)}.vx-ctx-comps{display:flex;flex-wrap:wrap;gap:5px}.vx-ctx-comps i{font-style:normal;display:inline-flex;background:var(--bg);border:1px solid var(--border);border-radius:6px;padding:1px 7px;font-size:11.5px}
.vx-bar{display:flex;align-items:center;gap:14px;padding:10px 14px;background:var(--panel);border-bottom:1px solid var(--border)}.vx-counts{display:flex;align-items:center;gap:9px;font-size:13px;color:var(--muted);flex-wrap:wrap}.vx-counts b{color:var(--ink)}.vx-counts span{display:inline-flex;align-items:center;gap:4px}.vx-dot{width:3px;height:3px;border-radius:50%;background:var(--soft)}.vx-big{background:var(--primary-weak);color:var(--primary-ink);border-radius:999px;padding:2px 8px;font-size:11px}.vx-tools{margin-left:auto;display:flex;align-items:center;gap:8px;flex-wrap:wrap}.vx-seg{display:flex;align-items:center;border:1px solid var(--border);border-radius:9px;background:var(--bg);overflow:hidden}.vx-seg button{height:32px;min-width:32px;display:flex;align-items:center;justify-content:center;border:none;background:transparent;color:var(--muted);cursor:pointer}.vx-seg button:hover{background:#fff;color:var(--ink)}.vx-zlabel{font-size:12px;font-weight:600;padding:0 6px}.vx-ghost{display:inline-flex;align-items:center;gap:6px;height:34px;padding:0 12px;border:1px solid var(--border);border-radius:9px;background:#fff;font-size:13px;font-weight:550;cursor:pointer}.vx-ghost:hover{background:var(--bg)}
.vx-stage{display:grid;grid-template-columns:280px minmax(0,1fr) 320px;height:min(78vh,920px);min-height:560px}.vx-nav{border-right:1px solid var(--border);background:#fff;display:flex;flex-direction:column;min-width:0}.vx-nav-compact{width:74px}.vx-nav-head{height:45px;display:flex;align-items:center;justify-content:space-between;gap:8px;padding:0 10px;border-bottom:1px solid var(--line);font-size:12px;font-weight:700;text-transform:uppercase;color:var(--muted)}.vx-nav-head span{display:inline-flex;align-items:center;gap:6px}.vx-nav-head button{border:1px solid var(--border);background:var(--bg);border-radius:7px;height:26px;padding:0 7px;font-size:11px;cursor:pointer}.vx-search{margin:10px;display:flex;align-items:center;gap:6px;border:1px solid var(--border);border-radius:9px;background:var(--bg);padding:0 9px;height:34px;color:var(--muted)}.vx-search input{border:none;background:transparent;outline:none;min-width:0;width:100%;font-size:12px}.vx-nav-list{overflow:auto;padding:0 8px 10px;display:flex;flex-direction:column;gap:7px}.vx-nav-item{border:1px solid var(--border);background:var(--panel);border-radius:10px;padding:8px;text-align:left;cursor:pointer}.vx-nav-item:hover,.vx-nav-on{border-color:var(--primary);box-shadow:0 0 0 2px var(--primary-weak)}.vx-nav-id{display:flex;align-items:center;gap:5px;font-family:ui-monospace,Menlo,monospace;font-size:11px;font-weight:700;color:var(--primary-ink)}.vx-nav-text{display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;margin-top:4px;font-size:11.5px;line-height:1.3;color:var(--ink)}.vx-nav-meta{margin-top:5px;display:flex;align-items:center;justify-content:space-between;gap:6px;font-size:10.5px;color:var(--soft)}.vx-nav-err{color:var(--danger)}.vx-nav-warn{color:var(--amber)}
.vx-canvas{position:relative;min-width:0;overflow:hidden;cursor:grab;touch-action:none;background:var(--canvas-bg);box-shadow:inset 0 0 0 1px var(--border)}.vx-canvas.vx-grabbing{cursor:grabbing}.vx-content{position:absolute;top:0;left:0;transform-origin:0 0;background-image:radial-gradient(circle,var(--dot) 1.4px,transparent 1.4px);background-size:22px 22px}.vx-edges{position:absolute;top:0;left:0;overflow:visible;pointer-events:none}#vx-a path{fill:#aeb6c6}.vx-at{fill:var(--amber-solid)}.vx-edge{fill:none;stroke:#b7bfce;stroke-width:1.8;transition:opacity .12s,stroke-width .12s}.vx-edge-on{stroke:var(--primary);stroke-width:2.8}.vx-edge-end{stroke:var(--gold)}.vx-edge-to{stroke:var(--amber-solid);stroke-dasharray:6 5;stroke-width:1.7}.vx-edge-dim{opacity:.18}.vx-edge-drag{stroke:var(--primary);stroke-width:2.2;stroke-dasharray:6 5}.vx-edge-drag-to{stroke:var(--amber-solid)}
.vx-node{position:absolute;z-index:10;background:#fff;border:1px solid var(--border);border-radius:12px;box-shadow:0 2px 6px rgba(20,30,55,.06);cursor:grab;user-select:none;transition:opacity .12s,box-shadow .12s,border-color .12s}.vx-node:hover{box-shadow:0 6px 18px rgba(20,30,55,.12)}.vx-node-dim{opacity:.36}.vx-node-on{border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-weak),0 8px 22px rgba(27,108,140,.16);opacity:1}.vx-node-err{border-color:var(--danger);box-shadow:0 0 0 2px var(--danger-weak)}.vx-node-warn{border-color:var(--amber-line);box-shadow:0 0 0 2px var(--amber-weak)}.vx-node-root .vx-msg{border-top:3px solid var(--primary);border-radius:12px 12px 0 0}.vx-msg{height:${MSG_H}px;padding:8px 11px;border-bottom:1px solid var(--line)}.vx-msg-head{display:flex;align-items:center;gap:5px;height:18px}.vx-id{display:inline-flex;align-items:center;gap:4px;font-family:ui-monospace,Menlo,monospace;font-size:11px;font-weight:650;color:var(--primary-ink)}.vx-id-end{color:var(--gold)}.vx-id-lg{font-size:13px}.vx-badge{display:inline-flex;align-items:center;gap:2px;font-size:9px;font-weight:700;text-transform:uppercase;color:var(--primary);background:var(--primary-weak);padding:1px 5px;border-radius:5px}.vx-badge-final{color:var(--gold);background:var(--gold-weak)}.vx-vbadge{display:inline-flex;align-items:center;gap:3px;font-size:9.5px;font-weight:700;padding:1px 5px;border-radius:5px;border:1px solid}.vx-vbadge-err{color:var(--danger);background:var(--danger-weak);border-color:var(--danger)}.vx-vbadge-warn{color:var(--amber);background:var(--amber-weak);border-color:var(--amber-line)}.vx-warn-mini{display:inline-flex;color:var(--amber-solid)}.vx-time{margin-left:auto;display:inline-flex;align-items:center;gap:3px;font-size:11px;color:var(--muted)}.vx-msg-text{margin-top:4px;font-size:12px;line-height:1.32;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.vx-optlabel{height:${LABEL_H}px;display:flex;align-items:center;padding:0 11px;font-size:10px;text-transform:uppercase;letter-spacing:.04em;color:var(--soft);font-weight:600;white-space:nowrap;overflow:hidden}.vx-opt{position:relative;padding:0 11px;border-top:1px solid var(--line)}.vx-opt-text{height:${OPT_TEXT_H}px;display:flex;align-items:center;font-size:12px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.vx-opt-dest{height:${OPT_DEST_H}px;display:flex;align-items:center;gap:6px}.vx-cardsel{flex:1;min-width:0;height:24px;border:1px solid var(--border);border-radius:6px;background:#fff;font-size:11px;font-family:ui-monospace,monospace;padding:0 2px 0 5px;outline:none;cursor:pointer}.vx-cardsel:hover{border-color:var(--primary)}.vx-cardsel:disabled{background:var(--bg);cursor:default}.vx-cardsel-to{color:var(--amber);border-color:var(--amber-line)}.vx-opt-scores{display:flex;flex-direction:column}.vx-score-row{height:${OPT_SCORE_ROW}px;display:flex;align-items:center;gap:8px}.vx-sr-name{flex:none;width:34px;font-size:10px;font-weight:700;text-transform:uppercase;color:var(--soft);letter-spacing:.02em}.vx-score-row input{flex:none;width:60px;height:21px;border:1px solid var(--border);border-radius:6px;background:#fff;text-align:center;font-size:12px;font-weight:600;font-variant-numeric:tabular-nums;color:var(--primary-ink);outline:none;padding:0 2px}.vx-score-row input:focus{border-color:var(--primary);box-shadow:0 0 0 2px var(--primary-weak)}.vx-score-row input:disabled{background:var(--bg);color:var(--soft)}.vx-sr-acc{flex:1;min-width:0;text-align:right;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-size:10.5px;color:var(--soft)}.vx-sr-acc b{color:var(--primary-ink);font-size:12px;font-variant-numeric:tabular-nums;margin-left:2px}.vx-score-empty{font-size:10.5px;color:var(--amber);font-style:italic;padding:4px 0}
.vx-mini-del,.vx-mini-add,.vx-foot-del,.vx-end-del{flex:none;display:inline-flex;align-items:center;justify-content:center;border:1px solid var(--border);border-radius:7px;background:#fff;color:var(--danger);cursor:pointer}.vx-mini-del,.vx-mini-add{width:22px;height:22px}.vx-mini-add{background:var(--primary-weak);color:var(--primary)}.vx-mini-add-to{background:var(--amber-weak);color:var(--amber);border-color:var(--amber-line)}.vx-mini-focus{height:23px;border:1px solid var(--border);background:#fff;border-radius:7px;padding:0 7px;font-size:10.5px;color:var(--primary-ink);cursor:pointer}.vx-mini-focus:hover{background:var(--primary-weak);border-color:var(--primary)}.vx-timeout{position:relative;height:${TIMEOUT_H}px;display:flex;align-items:center;gap:6px;padding:0 11px;border-top:1px solid var(--line);background:#FCFBF7}.vx-timeout svg{color:var(--amber-solid);flex:none}.vx-timeout-l{flex:none;font-size:10.5px;font-weight:600;text-transform:uppercase;color:var(--soft)}.vx-timeout-warn{background:var(--amber-weak);box-shadow:inset 2px 0 0 var(--amber-solid)}.vx-foot{height:${FOOTER_H}px;display:flex;align-items:center;gap:6px;padding:0 9px;border-top:1px solid var(--line)}.vx-foot-add{display:inline-flex;align-items:center;gap:4px;height:24px;padding:0 9px;border:1px dashed var(--border);border-radius:7px;background:transparent;color:var(--primary);font-size:11px;font-weight:600;cursor:pointer}.vx-foot-add:hover{background:var(--primary-weak);border-color:var(--primary)}.vx-foot-del,.vx-end-del{margin-left:auto;width:24px;height:24px}.vx-end{border-color:var(--gold-line)}.vx-end.vx-node-on{border-color:var(--primary)}.vx-end-head{height:${END_HEAD_H}px;display:flex;align-items:center;gap:6px;padding:0 11px;background:var(--gold-weak);border-bottom:1px solid var(--gold-line);border-radius:12px 12px 0 0}.vx-end-note{height:auto;min-height:${END_NOTE_MIN_H}px;display:flex;flex-direction:column;justify-content:center;gap:4px;padding:8px 11px;border-bottom:1px solid var(--line)}.vx-end-note-l{font-size:9.5px;text-transform:uppercase;letter-spacing:.05em;color:var(--soft);font-weight:700}.vx-end-comps{display:flex;flex-direction:column;gap:3px;width:100%}.vx-end-comp{display:flex;align-items:baseline;gap:8px;font-size:11px}.vx-ec-name{flex:none;width:34px;font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.02em;color:var(--soft)}.vx-ec-acc{flex:1;min-width:0;font-size:10.5px;color:var(--muted);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.vx-ec-pct{flex:none;font-family:Georgia,serif;font-size:16px;font-weight:700;color:var(--gold);font-variant-numeric:tabular-nums}.vx-ec-pct small{font-size:10px}.vx-end-note-final{display:flex;justify-content:space-between;gap:8px;font-size:11px;color:var(--muted)}.vx-end-note-final b{color:var(--gold);font-size:13px}.vx-end-report{padding:8px 11px}.vx-end-report-label{font-size:10px;font-weight:700;text-transform:uppercase;color:var(--soft);margin-bottom:5px}.vx-end-report-label em{color:var(--danger);font-style:normal}.vx-end-report textarea{width:100%;height:100px;border:1px solid var(--border);border-radius:8px;resize:none;padding:7px;font-size:12px;background:#fff}.vx-end-report textarea:disabled{background:var(--bg);color:var(--muted)}.vx-end-report-warn{border-color:var(--amber-line)!important;background:var(--amber-weak)!important}
.vx-port{position:absolute;z-index:12;width:12px;height:12px;border:2px solid #fff;border-radius:50%;background:var(--primary);box-shadow:0 0 0 1px var(--primary);cursor:crosshair}.vx-port-in{left:-6px;top:${MSG_H / 2 - 6}px}.vx-port-end{top:${END_HEAD_H / 2 - 6}px;background:var(--gold);box-shadow:0 0 0 1px var(--gold)}.vx-port-out{right:-6px;top:${OPT_START + OPT_PORT - 6}px}.vx-port-to{right:-6px;bottom:${FOOTER_H + TIMEOUT_H / 2 - 6}px;background:var(--amber-solid);box-shadow:0 0 0 1px var(--amber-solid)}.vx-legend{position:absolute;left:12px;bottom:12px;display:flex;align-items:center;gap:10px;flex-wrap:wrap;padding:8px 10px;border:1px solid var(--border);border-radius:10px;background:rgba(255,255,255,.92);font-size:11px;color:var(--muted);box-shadow:0 6px 18px rgba(20,30,55,.08)}.vx-leg{display:inline-flex;align-items:center;gap:5px}.vx-sw{width:18px;height:0;border-top:2px dashed var(--amber-solid)}.vx-leg-hint{color:var(--primary-ink)}
.vx-inspector{border-left:1px solid var(--border);background:var(--panel);overflow:auto}.vx-insp-empty{height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10px;padding:24px;text-align:center;color:var(--muted);font-size:13px}.vx-insp-body{padding:14px}.vx-insp-head{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:10px}.vx-x{width:28px;height:28px;border:1px solid var(--border);border-radius:8px;background:#fff;display:inline-flex;align-items:center;justify-content:center;cursor:pointer}.vx-insp-tag{display:inline-flex;border:1px solid var(--gold-line);background:var(--gold-weak);color:var(--gold);font-size:10px;text-transform:uppercase;font-weight:700;border-radius:6px;padding:3px 7px}.vx-insp-note{margin-top:10px;border:1px solid var(--gold-line);border-radius:10px;background:var(--gold-weak);padding:10px;display:flex;justify-content:space-between;align-items:center;font-size:12px;color:var(--gold)}.vx-insp-note b{font-size:24px}.vx-insp-note small{font-size:12px}.vx-focus-btn{width:100%;margin-top:10px;height:32px;border:1px solid var(--primary);background:var(--primary-weak);color:var(--primary-ink);border-radius:9px;font-size:12px;font-weight:650;cursor:pointer}.vx-insp-end-comps{display:grid;grid-template-columns:1fr;gap:6px;margin-top:10px}.vx-insp-end-comps span,.vx-insp-acc span{display:flex;justify-content:space-between;gap:8px;border:1px solid var(--border);border-radius:8px;background:var(--bg);padding:6px 8px;font-size:11px}.vx-insp-end-comps em{font-style:normal;font-weight:700;color:var(--gold)}.vx-field{display:flex;flex-direction:column;gap:5px;margin-top:10px}.vx-field span{font-size:10.5px;text-transform:uppercase;font-weight:700;letter-spacing:.04em;color:var(--soft)}.vx-field textarea,.vx-field input{border:1px solid var(--border);border-radius:9px;background:#fff;padding:8px;font-size:12px;outline:none}.vx-field textarea:focus,.vx-field input:focus{border-color:var(--primary);box-shadow:0 0 0 2px var(--primary-weak)}.vx-field textarea:disabled,.vx-field input:disabled{background:var(--bg);color:var(--muted)}.vx-field-warn textarea{border-color:var(--amber-line);background:var(--amber-weak)}.vx-insp-acc{margin-top:10px;display:grid;gap:6px;font-size:11px;color:var(--muted)}.vx-insp-sub{margin-top:14px;margin-bottom:8px;font-size:10.5px;text-transform:uppercase;font-weight:700;letter-spacing:.04em;color:var(--soft)}.vx-opt-card{border:1px solid var(--border);border-radius:10px;background:var(--bg);padding:8px;margin-bottom:8px}.vx-opt-input{width:100%;border:1px solid var(--border);border-radius:8px;background:#fff;padding:7px;font-size:12px}.vx-opt-meta{display:flex;gap:7px;margin-top:7px}.vx-opt-meta select{flex:1;min-width:0;border:1px solid var(--border);border-radius:8px;height:30px;background:#fff;font-size:12px}.vx-opt-meta button{border:1px solid var(--border);border-radius:8px;background:#fff;color:var(--danger);font-size:11px;display:inline-flex;align-items:center;gap:4px;cursor:pointer}.vx-add-wide{width:100%;height:34px;border:1px dashed var(--primary);border-radius:9px;background:var(--primary-weak);color:var(--primary-ink);display:inline-flex;align-items:center;justify-content:center;gap:6px;font-weight:650;cursor:pointer}
@media(max-width:1180px){.vx-stage{grid-template-columns:240px minmax(0,1fr)}.vx-inspector{display:none}.vx-nav{min-width:220px}}
@media(max-width:760px){.vx-stage{grid-template-columns:1fr;height:70vh}.vx-nav{display:none}.vx-bar{align-items:flex-start;flex-direction:column}.vx-tools{margin-left:0}.vx-ctx-grid{grid-template-columns:1fr}}
`;
