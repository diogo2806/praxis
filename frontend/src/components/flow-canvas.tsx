/**
 * Práxis · FlowCanvas — mapa do validador.
 *
 * Modelo de pontuação: POR RESPOSTA. Cada saída (opção) tem suas próprias notas
 * de competência, mostradas DENTRO da resposta, logo abaixo do texto (linha alta).
 * É o que o ResultScoringService usa: por nó, melhor = max das opções; nota =
 * escolhida ÷ melhor, ponderada pelos pesos do blueprint. Respostas diferentes no
 * mesmo nó → notas diferentes (boa vs ruim).
 *
 * Estrutura segue as LIGAÇÕES (nextNodeId / timeoutNextNodeId), nunca o nome do ID
 * (que no backend é "turno-N"). Rótulos 1, 1.1, 1.2.1 são derivados do fluxo.
 *
 * No topo, um card de CONTEXTO resume o que foi preenchido nas etapas 1 (Teste) e
 * 2 (Cenário), lido do próprio `version`.
 *
 * Tipado contra o client gerado. Não guarda estado de domínio: recebe `version` e
 * emite intenções pelos callbacks já ligados às mutações da ValidatorPage.
 */
import { useEffect, useMemo, useRef, useState } from "react";
import { ChevronDown, Clock, Crosshair, Flag, Maximize2, Plus, Target, Timer, Trash2, User, Workflow, X, ZoomIn, ZoomOut } from "lucide-react";
import type { SimulationVersionDetailResponse, UpdateNodeRequest, UpdateOptionRequest } from "@/lib/api/praxis";

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
}

/* ---- geometria fixa ---- */
/* a resposta é empilhada: texto → destino → competências (ponto + acum) → linha em branco.
   a altura (OPT_H) é calculada dentro do componente conforme o nº de competências. */
const NODE_W = 350;
const MSG_H = 76;
const LABEL_H = 24;
const OPT_TEXT_H = 35;     // linha do texto da resposta
const OPT_DEST_H = 40;     // linha do destino (select)
const OPT_SCORE_ROW = 50;  // cada linha de competência (nome · ponto · acum)
const OPT_BOTTOM = 24;     // "pula uma linha" antes da próxima resposta
const OPT_PORT = OPT_TEXT_H + OPT_DEST_H / 2; // porta no centro da linha do destino
const TIMEOUT_H = 40;
const FOOTER_H = 38;
const OPT_START = MSG_H + LABEL_H;
const END_HEAD_H = 44;
const END_NOTE_H = 84;
const END_REPORT_H = 140;
const END_HEIGHT = END_HEAD_H + END_NOTE_H + END_REPORT_H;
const FIM = "__fim__";
const NEW = "__new__";

const isEnd = (n?: NodeDto) => !!n && (n as { isFinal?: boolean }).isFinal === true;
const clamp = (v: number, a: number, b: number) => Math.min(b, Math.max(a, v));
const shortName = (s: string) => { const t = s.trim(); return t.length <= 4 ? t : t.slice(0, 3); };

export default function FlowCanvas({
  version, canEdit, selectedNodeId, onSelectNode,
  onUpdateNode, onEditOption, onAddOption, onDeleteOption, onDeleteStep, onCreateChild,
}: FlowCanvasProps) {
  const nodes = useMemo(
    () => [...version.nodes].sort((a, b) => a.turnIndex - b.turnIndex || a.id.localeCompare(b.id)),
    [version.nodes],
  );
  const nodeById = useMemo(() => Object.fromEntries(nodes.map((n) => [n.id, n])) as Record<string, NodeDto>, [nodes]);
  const rootId = version.blueprint.rootNodeId ?? nodes[0]?.id ?? null;
  const comps: CompW[] = useMemo(
    () => (version.blueprint.competencies ?? []).slice(0, 3).map((c) => ({ name: c.name, weight: c.weight ?? 0, short: shortName(c.name) })),
    [version.blueprint.competencies],
  );

  /* altura da resposta cresce com o nº de competências (evita texto trepado) */
  const nComps = comps.length;
  const OPT_H = OPT_TEXT_H + OPT_DEST_H + nComps * OPT_SCORE_ROW + OPT_BOTTOM;
  const stepHeight = (n: NodeDto) => OPT_START + n.options.length * OPT_H + TIMEOUT_H + FOOTER_H;
  const nodeHeight = (n: NodeDto) => (isEnd(n) ? END_HEIGHT : stepHeight(n));

  /* ---- contexto (etapas 1 e 2), lido do version ---- */
  const rootNode = rootId ? nodeById[rootId] : null;
  const critical = (version as { criticalSituation?: string | null }).criticalSituation ?? null;
  const resultUse = (version as { resultUse?: string | null }).resultUse ?? null;
  const [ctxOpen, setCtxOpen] = useState(true);

  /* ---- ligações ---- */
  const incomingOf = (nodeId: string): { parent: NodeDto; option: OptionDto | null; via: "option" | "timeout" } | null => {
    for (const p of nodes) { const o = p.options.find((x) => x.nextNodeId === nodeId); if (o) return { parent: p, option: o, via: "option" }; }
    for (const p of nodes) { if (p.timeoutNextNodeId === nodeId) return { parent: p, option: null, via: "timeout" }; }
    return null;
  };
  const chainTo = (nodeId: string): NodeDto[] => {
    const chain: NodeDto[] = [];
    const seen = new Set<string>();
    let cur: string | null = nodeId;
    while (cur && !seen.has(cur)) { seen.add(cur); const n = nodeById[cur]; if (!n) break; chain.unshift(n); const inc = incomingOf(cur); cur = inc ? inc.parent.id : null; }
    return chain;
  };
  /* nota determinística do caminho (mesma fórmula do ResultScoringService) */
  const pathScore = (nodeId: string): number | null => {
    const chain = chainTo(nodeId);
    if (chain.length < 2) return null;
    const raw: Record<string, number> = {}, max: Record<string, number> = {};
    comps.forEach((c) => { raw[c.name] = 0; max[c.name] = 0; });
    for (let i = 0; i < chain.length - 1; i++) {
      const step = chain[i], next = chain[i + 1];
      const picked = step.options.find((o) => o.nextNodeId === next.id);
      comps.forEach((c) => {
        const best = step.options.reduce((m, o) => Math.max(m, o.competencyLevels?.[c.name] ?? 0), 0);
        max[c.name] += best;
        raw[c.name] += picked?.competencyLevels?.[c.name] ?? 0;
      });
    }
    let ws = 0; comps.forEach((c) => { if (max[c.name] > 0) ws += c.weight; });
    if (ws === 0) return 0;
    let w = 0; comps.forEach((c) => { if (max[c.name] > 0) w += (raw[c.name] / max[c.name]) * (c.weight / ws); });
    return Math.round(w * 100);
  };
  /* acumulada bruta por competência até o nó (informativo, usado no inspetor) */
  const accTo = (nodeId: string): Record<string, number> => {
    const acc: Record<string, number> = {}; comps.forEach((c) => { acc[c.name] = 0; });
    const chain = chainTo(nodeId);
    for (let i = 0; i < chain.length - 1; i++) {
      const picked = chain[i].options.find((o) => o.nextNodeId === chain[i + 1].id);
      comps.forEach((c) => { acc[c.name] += picked?.competencyLevels?.[c.name] ?? 0; });
    }
    return acc;
  };
  /* rótulos em árvore derivados do fluxo */
  const treeLabel = useMemo(() => {
    const lab: Record<string, string> = {};
    const visit = (id: string | null | undefined, l: string) => {
      if (!id || lab[id] != null) return;
      lab[id] = l;
      const n = nodeById[id]; if (!n) return;
      let k = 0;
      n.options.forEach((o) => { if (o.nextNodeId) { k++; visit(o.nextNodeId, `${l}.${k}`); } });
      if (n.timeoutNextNodeId) { k++; visit(n.timeoutNextNodeId, `${l}.${k}`); }
    };
    if (rootId) visit(rootId, "1");
    nodes.forEach((n) => { if (lab[n.id] == null) lab[n.id] = n.id; });
    return lab;
  }, [nodes, rootId, nodeById]);
  const labelOf = (id: string) => treeLabel[id] ?? id;

  const forwardTargets = (curId: string) => {
    const anc = new Set(chainTo(curId).map((n) => n.id));
    return nodes.filter((n) => !isEnd(n) && !anc.has(n.id)).sort((a, b) => a.turnIndex - b.turnIndex);
  };

  /* ---- pontuação POR RESPOSTA ---- */
  const setScore = (nodeId: string, opt: OptionDto, comp: string, raw: string) => {
    const levels: Record<string, number> = { ...(opt.competencyLevels ?? {}) };
    levels[comp] = clamp(Number(raw) || 0, 0, 100);
    onEditOption(nodeId, opt.id, { competencyLevels: levels });
  };

  /* ---- posições ---- */
  const autoLayout = useMemo(() => {
    const depth: Record<string, number> = {};
    const visit = (id: string | null | undefined, d: number) => {
      if (!id || depth[id] != null) return;
      depth[id] = d;
      const n = nodeById[id]; if (!n) return;
      n.options.forEach((o) => visit(o.nextNodeId, d + 1));
      visit(n.timeoutNextNodeId, d + 1);
    };
    if (rootId) visit(rootId, 0);
    const perDepth: Record<number, number> = {};
    const pos: Record<string, Pos> = {};
    nodes.forEach((n) => {
      const d = depth[n.id] ?? 0;
      const i = (perDepth[d] = (perDepth[d] ?? 0) + 1) - 1;
      pos[n.id] = { x: 60 + d * (NODE_W + 120), y: 60 + i * 280 };
    });
    return pos;
  }, [nodes, rootId, nodeById]);

  const [localPos, setLocalPos] = useState<Record<string, Pos>>({});
  const posOf = (n: NodeDto): Pos => {
    if (localPos[n.id]) return localPos[n.id];
    const px = (n as { positionX?: number | null }).positionX;
    const py = (n as { positionY?: number | null }).positionY;
    if (px != null && py != null) return { x: px, y: py };
    return autoLayout[n.id] ?? { x: 60, y: 60 };
  };

  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState<Pos>({ x: 24, y: 10 });
  const [drag, setDrag] = useState<any>(null);
  const [conn, setConn] = useState<Pos | null>(null);
  const canvasRef = useRef<HTMLDivElement>(null);

  const inPort = (n: NodeDto) => ({ x: posOf(n).x, y: posOf(n).y + (isEnd(n) ? END_HEAD_H / 2 : MSG_H / 2) });
  const outPort = (n: NodeDto, i: number) => ({ x: posOf(n).x + NODE_W, y: posOf(n).y + OPT_START + i * OPT_H + OPT_PORT });
  const toPort = (n: NodeDto) => ({ x: posOf(n).x + NODE_W, y: posOf(n).y + OPT_START + n.options.length * OPT_H + TIMEOUT_H / 2 });
  const edgePath = (s: Pos, t: Pos) => { const dx = Math.max(44, Math.abs(t.x - s.x) * 0.5); return `M ${s.x} ${s.y} C ${s.x + dx} ${s.y}, ${t.x - dx} ${t.y}, ${t.x} ${t.y}`; };

  const toCanvas = (cx: number, cy: number): Pos => { const r = canvasRef.current!.getBoundingClientRect(); return { x: (cx - r.left - pan.x) / zoom, y: (cy - r.top - pan.y) / zoom }; };
  const nodeAt = (cx: number, cy: number, exclude?: string): NodeDto | null => {
    for (let i = nodes.length - 1; i >= 0; i--) { const n = nodes[i]; if (n.id === exclude) continue; const p = posOf(n); if (cx >= p.x && cx <= p.x + NODE_W && cy >= p.y && cy <= p.y + nodeHeight(n)) return n; }
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

  /* ---- drag / pan / conexão ---- */
  useEffect(() => {
    if (!drag) return;
    const onMove = (e: PointerEvent) => {
      if (drag.type === "pan") setPan({ x: drag.panX + (e.clientX - drag.sx), y: drag.panY + (e.clientY - drag.sy) });
      else if (drag.type === "node") setLocalPos((p) => ({ ...p, [drag.id]: { x: Math.max(0, drag.nx + (e.clientX - drag.sx) / zoom), y: Math.max(0, drag.ny + (e.clientY - drag.sy) / zoom) } }));
      else setConn(toCanvas(e.clientX, e.clientY));
    };
    const onUp = (e: PointerEvent) => {
      if (drag.type === "node") { const p = localPos[drag.id]; if (p && canEdit) onUpdateNode(drag.id, { positionX: Math.round(p.x), positionY: Math.round(p.y) }); }
      else if (drag.type === "connect" || drag.type === "connect-timeout") {
        const c = toCanvas(e.clientX, e.clientY);
        const tgt = nodeAt(c.x, c.y, drag.fromNodeId);
        const isAncestor = tgt ? chainTo(drag.fromNodeId).some((a) => a.id === tgt.id) : false;
        if (tgt && !isEnd(tgt) && tgt.id !== drag.fromNodeId && !isAncestor) {
          if (drag.type === "connect-timeout") onUpdateNode(drag.fromNodeId, { timeoutNextNodeId: tgt.id });
          else onEditOption(drag.fromNodeId, drag.optionId, { nextNodeId: tgt.id });
        }
      }
      setDrag(null); setConn(null);
    };
    window.addEventListener("pointermove", onMove);
    window.addEventListener("pointerup", onUp);
    return () => { window.removeEventListener("pointermove", onMove); window.removeEventListener("pointerup", onUp); };
  }, [drag, zoom, pan, localPos, nodes]);

  useEffect(() => {
    const el = canvasRef.current; if (!el) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      const r = el.getBoundingClientRect(); const cx = e.clientX - r.left, cy = e.clientY - r.top;
      const nz = clamp(zoom * (e.deltaY < 0 ? 1.1 : 0.9), 0.4, 1.8);
      setPan({ x: cx - ((cx - pan.x) / zoom) * nz, y: cy - ((cy - pan.y) / zoom) * nz }); setZoom(nz);
    };
    el.addEventListener("wheel", onWheel, { passive: false });
    return () => el.removeEventListener("wheel", onWheel);
  }, [zoom, pan]);

  const zoomBy = (f: number) => { const r = canvasRef.current!.getBoundingClientRect(); const cx = r.width / 2, cy = r.height / 2; const nz = clamp(zoom * f, 0.4, 1.8); setPan({ x: cx - ((cx - pan.x) / zoom) * nz, y: cy - ((cy - pan.y) / zoom) * nz }); setZoom(nz); };
  const fit = () => {
    if (!nodes.length) return;
    const r = canvasRef.current!.getBoundingClientRect();
    const ps = nodes.map(posOf);
    const minX = Math.min(...ps.map((p) => p.x)), minY = Math.min(...ps.map((p) => p.y));
    const maxX = Math.max(...nodes.map((n) => posOf(n).x + NODE_W)), maxY = Math.max(...nodes.map((n) => posOf(n).y + nodeHeight(n)));
    const pad = 48;
    const nz = clamp(Math.min((r.width - pad * 2) / (maxX - minX), (r.height - pad * 2) / (maxY - minY)), 0.4, 1.4);
    setZoom(nz); setPan({ x: (r.width - (maxX - minX) * nz) / 2 - minX * nz, y: (r.height - (maxY - minY) * nz) / 2 - minY * nz });
  };

  const onCanvasDown = (e: React.PointerEvent) => { const t = e.target as HTMLElement; if (t.closest(".vx-node") || t.closest(".vx-port")) return; onSelectNode(null); setDrag({ type: "pan", sx: e.clientX, sy: e.clientY, panX: pan.x, panY: pan.y }); };
  const onNodeDown = (e: React.PointerEvent, n: NodeDto) => { const t = e.target as HTMLElement; if (t.closest(".vx-port") || t.closest("button") || t.closest("input") || t.closest("select") || t.closest("textarea")) return; e.stopPropagation(); onSelectNode(n.id); if (canEdit) setDrag({ type: "node", id: n.id, sx: e.clientX, sy: e.clientY, nx: posOf(n).x, ny: posOf(n).y }); };

  const sel = selectedNodeId ? nodeById[selectedNodeId] : null;
  const connSource: NodeDto | null = drag?.fromNodeId ? nodeById[drag.fromNodeId] : null;
  const connStart = connSource ? (drag.type === "connect-timeout" ? toPort(connSource) : outPort(connSource, connSource.options.findIndex((o) => o.id === drag.optionId))) : null;

  /* competências de uma resposta: por competência → nome · ponto (editável) · acumulado */
  const ScoreRow = ({ nodeId, opt, big }: { nodeId: string; opt: OptionDto; big?: boolean }) => {
    if (comps.length === 0) return <div className="vx-score-empty">defina competências no blueprint (etapa 1)</div>;
    const base = accTo(nodeId); // acumulado que chega até esta etapa
    return (
      <div className={`vx-opt-scores ${big ? "vx-opt-scores-lg" : ""}`}>
        {comps.map((c) => {
          const pts = opt.competencyLevels?.[c.name] ?? 0;
          const acc = (base[c.name] ?? 0) + pts;
          return (
            <div className="vx-score-row" key={c.name} title={c.name}>
              <span className="vx-sr-name">{c.short}</span>
              <input type="number" min={0} max={100} disabled={!canEdit} value={pts}
                onClick={(e) => e.stopPropagation()} onChange={(e) => setScore(nodeId, opt, c.name, e.target.value)} />
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

      {/* CONTEXTO — etapas 1 e 2 */}
      <div className="vx-ctx">
        <button className="vx-ctx-toggle" onClick={() => setCtxOpen((v) => !v)}>
          <ChevronDown size={15} className={ctxOpen ? "vx-rot" : ""} />
          Contexto do teste
          <span className="vx-ctx-name">{version.name}</span>
        </button>
        {ctxOpen && (
          <div className="vx-ctx-grid">
            <div className="vx-ctx-col">
              <div className="vx-ctx-tag"><Target size={12} /> Etapa 1 · Teste</div>
              <div className="vx-ctx-row"><b>Nome</b><span>{version.name || "—"}</span></div>
              <div className="vx-ctx-row"><b>Descrição</b><span>{version.description || "—"}</span></div>
              {resultUse && <div className="vx-ctx-row"><b>Uso do resultado</b><span>{resultUse}</span></div>}
              <div className="vx-ctx-row"><b>Competências</b>
                <span className="vx-ctx-comps">
                  {comps.length ? comps.map((c) => <i key={c.name} title={`peso ${c.weight}`}>{c.name}<small>{c.weight}</small></i>) : "—"}
                </span>
              </div>
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
        </div>
        <div className="vx-tools">
          <div className="vx-seg">
            <button onClick={() => zoomBy(0.9)} title="Diminuir"><ZoomOut size={16} /></button>
            <button onClick={() => setZoom(1)} className="vx-zlabel">{Math.round(zoom * 100)}%</button>
            <button onClick={() => zoomBy(1.1)} title="Aumentar"><ZoomIn size={16} /></button>
          </div>
          <button className="vx-ghost" onClick={fit}><Maximize2 size={15} /> Enquadrar</button>
        </div>
      </div>

      <div className="vx-stage">
        <div ref={canvasRef} className={`vx-canvas ${drag?.type === "pan" ? "vx-grabbing" : ""}`} onPointerDown={onCanvasDown}>
          <div className="vx-content" style={{ transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})` }}>
            <svg className="vx-edges" width={4200} height={3200}>
              <defs>
                <marker id="vx-a" markerWidth="9" markerHeight="9" refX="7.5" refY="4.5" orient="auto"><path d="M0,0 L9,4.5 L0,9 Z" /></marker>
                <marker id="vx-at" markerWidth="9" markerHeight="9" refX="7.5" refY="4.5" orient="auto"><path d="M0,0 L9,4.5 L0,9 Z" className="vx-at" /></marker>
              </defs>
              {nodes.filter((n) => !isEnd(n)).flatMap((n) =>
                n.options.map((o, i) => {
                  const t = o.nextNodeId ? nodeById[o.nextNodeId] : undefined;
                  if (!t) return null;
                  const on = selectedNodeId === n.id || selectedNodeId === t.id;
                  return <path key={`${n.id}-${o.id}`} d={edgePath(outPort(n, i), inPort(t))} className={`vx-edge ${on ? "vx-edge-on" : ""} ${isEnd(t) ? "vx-edge-end" : ""}`} markerEnd="url(#vx-a)" />;
                }),
              )}
              {nodes.filter((n) => !isEnd(n)).map((n) => {
                const t = n.timeoutNextNodeId ? nodeById[n.timeoutNextNodeId] : undefined;
                if (!t) return null;
                return <path key={`${n.id}-to`} d={edgePath(toPort(n), inPort(t))} className="vx-edge vx-edge-to" markerEnd="url(#vx-at)" />;
              })}
              {connStart && conn && <path d={edgePath(connStart, conn)} className={`vx-edge vx-edge-drag ${drag.type === "connect-timeout" ? "vx-edge-drag-to" : ""}`} />}
            </svg>

            {nodes.map((n) => {
              const p = posOf(n);
              const on = selectedNodeId === n.id;
              if (isEnd(n)) {
                const nota = pathScore(n.id);
                const report = (n as { reportText?: string }).reportText ?? "";
                const pending = !report.trim();
                const perCompScore = (nodeId: string) => {
                const chain = chainTo(nodeId);
                return comps.map((c) => {
                  let raw = 0, max = 0;
                  for (let i = 0; i < chain.length - 1; i++) {
                    const step = chain[i], next = chain[i + 1];
                    const picked = step.options.find((o) => o.nextNodeId === next.id);
                    max += step.options.reduce((m, o) => Math.max(m, o.competencyLevels?.[c.name] ?? 0), 0);
                    raw += picked?.competencyLevels?.[c.name] ?? 0;
                  }
                  return { name: c.name, short: c.short, acc: raw, pct: max > 0 ? Math.round((raw / max) * 100) : null };
                });
              };
                return (
                  <div key={n.id} className={`vx-node vx-end ${on ? "vx-node-on" : ""}`} style={{ left: p.x, top: p.y, width: NODE_W }} onPointerDown={(e) => onNodeDown(e, n)}>
                    <span className="vx-port vx-port-in vx-port-end" />
                    <div className="vx-end-head">
                      <span className="vx-id vx-id-end"><Flag size={12} />{labelOf(n.id)}</span>
                      <span className="vx-badge vx-badge-final">encerramento</span>
                      {pending && <span className="vx-warn-mini" title="Falta o relatório"><Flag size={11} /></span>}
                      {canEdit && <button className="vx-end-del" onClick={() => onDeleteStep(n.id)} title="Remover encerramento"><Trash2 size={13} /></button>}
                    </div>
                    <div className="vx-end-note">
                      <span className="vx-end-note-l">Nota determinística por competência</span>
                      <div className="vx-end-comps">
                        {perCompScore(n.id).map((c) => (
                          <div className="vx-end-comp" key={c.name} title={c.name}>
                            <span className="vx-ec-name">{c.short}</span>
                            <span className="vx-ec-acc">acum {c.acc}</span>
                            <span className="vx-ec-pct">{c.pct == null ? "—" : c.pct}<small>%</small></span>
                          </div>
                        ))}
                      </div>
                      <span className="vx-end-note-final">final ponderada <b>{nota == null ? "—" : nota}%</b></span>
                    </div>
                    <div className="vx-end-report">
                      <div className="vx-end-report-label">Relatório para a equipe responsável {pending && <em>*</em>}</div>
                      <textarea className={pending ? "vx-end-report-warn" : ""} value={report} disabled={!canEdit}
                        onChange={(e) => onUpdateNode(n.id, { reportText: e.target.value })}
                        placeholder="Resumo apresentado à equipe responsável ao fim deste caminho…" />
                    </div>
                  </div>
                );
              }

              const isRoot = n.id === rootId;
              const noTo = !n.timeoutNextNodeId;
              const fwd = forwardTargets(n.id);
              return (
                <div key={n.id} className={`vx-node ${on ? "vx-node-on" : ""} ${isRoot ? "vx-node-root" : ""}`} style={{ left: p.x, top: p.y, width: NODE_W }} onPointerDown={(e) => onNodeDown(e, n)}>
                  <span className="vx-port vx-port-in" />
                  <div className="vx-msg">
                    <div className="vx-msg-head">
                      <span className="vx-id">{isRoot ? <Crosshair size={12} /> : <Workflow size={12} />}{labelOf(n.id)}</span>
                      {isRoot && <span className="vx-badge">início</span>}
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
                      ? <button className="vx-mini-del" onClick={() => (isEnd(nodeById[n.timeoutNextNodeId!]) ? onDeleteStep(n.timeoutNextNodeId!) : onUpdateNode(n.id, { timeoutNextNodeId: "" }))} title="Remover ligação"><Trash2 size={12} /></button>
                      : <button className="vx-mini-add vx-mini-add-to" onClick={() => onCreateChild(n.id, { via: "timeout" }, false)} title="Criar fallback do tempo"><Plus size={12} /></button>)}
                    <span className="vx-port vx-port-to" onPointerDown={(e) => { e.stopPropagation(); setConn(toPort(n)); setDrag({ type: "connect-timeout", fromNodeId: n.id }); }} />
                  </div>

                  <div className="vx-foot">
                    {canEdit && <button className="vx-foot-add" onClick={() => onAddOption(n.id)}><Plus size={13} /> resposta</button>}
                    {canEdit && !isRoot && <button className="vx-foot-del" onClick={() => onDeleteStep(n.id)} title="Remover etapa"><Trash2 size={13} /></button>}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="vx-legend">
            <span className="vx-leg"><i className="vx-sw" /> tempo</span>
            <span className="vx-leg"><Flag size={12} /> encerramento · nota do caminho</span>
            <span className="vx-leg-hint">nota por resposta: o motor compara escolhida ÷ melhor do nó</span>
          </div>
        </div>

        {/* inspetor */}
        <aside className="vx-inspector">
          {!sel ? (
            <div className="vx-insp-empty"><Workflow size={22} /><p>Selecione um card no mapa para editar.</p></div>
          ) : isEnd(sel) ? (
            <div className="vx-insp-body">
              <div className="vx-insp-head"><span className="vx-id vx-id-lg vx-id-end"><Flag size={13} />{labelOf(sel.id)}</span><button className="vx-x" onClick={() => onSelectNode(null)}><X size={15} /></button></div>
              <div className="vx-insp-tag">Card de encerramento</div>
              <div className="vx-insp-note">Nota determinística deste caminho<b>{pathScore(sel.id) ?? "—"}<small>/100</small></b></div>
              <label className={`vx-field ${!((sel as { reportText?: string }).reportText ?? "").trim() ? "vx-field-warn" : ""}`} style={{ marginTop: 12 }}>
                <span>Texto do relatório para a equipe responsável *</span>
                <textarea rows={8} disabled={!canEdit} value={(sel as { reportText?: string }).reportText ?? ""} onChange={(e) => onUpdateNode(sel.id, { reportText: e.target.value })} />
              </label>
            </div>
          ) : (
            <div className="vx-insp-body">
              <div className="vx-insp-head"><span className="vx-id vx-id-lg">{sel.id === rootId ? <Crosshair size={13} /> : <Workflow size={13} />}{labelOf(sel.id)}</span><button className="vx-x" onClick={() => onSelectNode(null)}><X size={15} /></button></div>
              <label className="vx-field"><span>Fala da etapa</span>
                <textarea rows={3} disabled={!canEdit} value={sel.clientMessage ?? ""} onChange={(e) => onUpdateNode(sel.id, { clientMessage: e.target.value })} />
              </label>
              <label className="vx-field"><span>Tempo de resposta (s)</span>
                <input type="number" min={1} disabled={!canEdit} value={sel.timeLimitSeconds ?? ""} onChange={(e) => onUpdateNode(sel.id, { timeLimitSeconds: e.target.value === "" ? null : Number(e.target.value) })} />
              </label>
              {chainTo(sel.id).length > 1 && (
                <div className="vx-insp-acc">acumulado até aqui:{comps.map((c) => { const a = accTo(sel.id); return <span key={c.name}>{c.short} <b>{a[c.name] ?? 0}</b></span>; })}</div>
              )}

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
                    {canEdit && <button className="vx-opt-del" onClick={() => onDeleteOption(sel.id, o.id)} title="Remover resposta"><Trash2 size={13} /></button>}
                  </div>
                </div>
              ))}
              {canEdit && <button className="vx-add-out" onClick={() => onAddOption(sel.id)}><Plus size={14} /> Adicionar resposta</button>}
            </div>
          )}
        </aside>
      </div>

      {/* Placar do fluxo — uma linha por RESPOSTA */}
      <div className="vx-board">
        <div className="vx-board-head">
          <h3>Placar do fluxo</h3>
          <p>Uma linha por resposta. Em cada nó o motor usa escolhida ÷ melhor (a melhor resposta vira referência). A nota final normalizada de cada caminho está nos encerramentos.</p>
        </div>
        <div className="vx-board-scroll">
          <table className="vx-tbl">
            <thead>
              <tr>
                <th>Etapa</th>
                <th>Resposta</th>
                {comps.map((c) => <th key={c.name} className="vx-th-n" title={c.name}>{c.short}</th>)}
                <th>Destino</th>
              </tr>
            </thead>
            <tbody>
              {nodes.filter((n) => !isEnd(n)).flatMap((n) =>
                (n.options.length ? n.options : [null]).map((o, i) => (
                  <tr key={`${n.id}-${o?.id ?? i}`} className={selectedNodeId === n.id ? "vx-tr-on" : ""} onClick={() => onSelectNode(n.id)}>
                    <td className="vx-td-step">{i === 0 ? <span className="vx-id">{n.id === rootId ? <Crosshair size={11} /> : <Workflow size={11} />}{labelOf(n.id)}</span> : ""}</td>
                    <td className="vx-td-txt" title={o?.text ?? ""}>{o ? (o.text || <em>—</em>) : <em>sem respostas</em>}</td>
                    {comps.map((c) => (
                      <td key={c.name} className="vx-td-n">
                        {o ? <input type="number" min={0} max={100} disabled={!canEdit} value={o.competencyLevels?.[c.name] ?? 0}
                          onClick={(e) => e.stopPropagation()} onChange={(e) => setScore(n.id, o, c.name, e.target.value)} /> : null}
                      </td>
                    ))}
                    <td className="vx-td-out">{o?.nextNodeId ? <span className="vx-out-pill">{labelOf(o.nextNodeId)}</span> : <em>—</em>}</td>
                  </tr>
                )),
              )}
            </tbody>
          </table>
        </div>
        {nodes.some(isEnd) && (
          <div className="vx-ends">
            {nodes.filter(isEnd).map((n) => {
              const nota = pathScore(n.id);
              const pend = !((n as { reportText?: string }).reportText ?? "").trim();
              return (
                <button key={n.id} className={`vx-endchip ${selectedNodeId === n.id ? "vx-endchip-on" : ""}`} onClick={() => onSelectNode(n.id)}>
                  <Flag size={11} /><b>{labelOf(n.id)}</b><span>{nota == null ? "—" : nota}<small>/100</small></span>{pend && <i title="Falta o relatório">!</i>}
                </button>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

const CSS = `
.vx-root *{box-sizing:border-box}
.vx-root{
  --bg:#FCFAF6;--panel:#FFFFFF;--ink:#172128;--muted:#616A71;--soft:#9AA3AA;
  --border:#D9DFE3;--line:#EBEEF0;
  --primary:#1B6C8C;--primary-weak:#E2F1FA;--primary-ink:#0E5570;
  --danger:#CC3E35;--danger-weak:#FAE7E5;
  --amber:#7A5410;--amber-weak:#FCF2DB;--amber-line:#E6C98A;--amber-solid:#E1A536;
  --gold:#8A6312;--gold-weak:#FBEFD4;--gold-line:#E4C57E;
  --dot:#E6E3DA;
  font-family:system-ui,-apple-system,"Segoe UI",sans-serif;color:var(--ink);
  display:flex;flex-direction:column;border:1px solid var(--border);border-radius:14px;overflow:hidden;background:var(--bg)}
.vx-root b{font-weight:650}
.vx-ctx{background:var(--panel);border-bottom:1px solid var(--border)}
.vx-ctx-toggle{width:100%;display:flex;align-items:center;gap:8px;padding:10px 14px;border:none;background:transparent;font-size:12.5px;font-weight:700;color:var(--ink);cursor:pointer;text-transform:uppercase;letter-spacing:.04em}
.vx-ctx-toggle svg{transition:transform .15s;color:var(--muted)} .vx-ctx-toggle .vx-rot{transform:rotate(180deg)}
.vx-ctx-name{margin-left:auto;font-weight:600;text-transform:none;letter-spacing:0;color:var(--primary-ink);font-size:12.5px;max-width:50%;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.vx-ctx-grid{display:grid;grid-template-columns:1fr 1fr;gap:1px;background:var(--line);border-top:1px solid var(--line)}
.vx-ctx-col{background:var(--panel);padding:11px 14px 13px}
.vx-ctx-tag{display:inline-flex;align-items:center;gap:5px;font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.05em;color:var(--primary);background:var(--primary-weak);padding:3px 8px;border-radius:6px;margin-bottom:9px}
.vx-ctx-row{display:grid;grid-template-columns:104px 1fr;gap:8px;padding:3px 0;font-size:12px;line-height:1.4}
.vx-ctx-row b{color:var(--soft);font-weight:600;font-size:11px;text-transform:uppercase;letter-spacing:.03em;padding-top:1px}
.vx-ctx-row span{color:var(--ink)} .vx-ctx-quote{font-style:italic;color:var(--muted)}
.vx-ctx-comps{display:flex;flex-wrap:wrap;gap:5px} .vx-ctx-comps i{font-style:normal;display:inline-flex;align-items:center;gap:4px;background:var(--bg);border:1px solid var(--border);border-radius:6px;padding:1px 7px;font-size:11.5px}
.vx-ctx-comps small{color:var(--primary-ink);font-weight:700;background:var(--primary-weak);border-radius:4px;padding:0 4px}
.vx-bar{display:flex;align-items:center;gap:14px;padding:10px 14px;background:var(--panel);border-bottom:1px solid var(--border)}
.vx-counts{display:flex;align-items:center;gap:9px;font-size:13px;color:var(--muted)} .vx-counts b{color:var(--ink)} .vx-counts span{display:inline-flex;align-items:center;gap:4px}
.vx-dot{width:3px;height:3px;border-radius:50%;background:var(--soft)}
.vx-tools{margin-left:auto;display:flex;align-items:center;gap:8px}
.vx-seg{display:flex;align-items:center;border:1px solid var(--border);border-radius:9px;background:var(--bg);overflow:hidden}
.vx-seg button{height:32px;min-width:32px;display:flex;align-items:center;justify-content:center;border:none;background:transparent;color:var(--muted);cursor:pointer}
.vx-seg button:hover{background:#fff;color:var(--ink)} .vx-zlabel{font-size:12px;font-weight:600;padding:0 6px}
.vx-ghost{display:inline-flex;align-items:center;gap:6px;height:34px;padding:0 12px;border:1px solid var(--border);border-radius:9px;background:#fff;font-size:13px;font-weight:550;cursor:pointer} .vx-ghost:hover{background:var(--bg)}
.vx-stage{display:flex;height:600px;min-height:0}
.vx-canvas{position:relative;flex:1;min-width:0;overflow:hidden;cursor:grab;touch-action:none}
.vx-canvas.vx-grabbing{cursor:grabbing}
.vx-content{position:absolute;top:0;left:0;width:4200px;height:3200px;transform-origin:0 0;background-image:radial-gradient(circle,var(--dot) 1.4px,transparent 1.4px);background-size:22px 22px}
.vx-edges{position:absolute;top:0;left:0;overflow:visible;pointer-events:none}
#vx-a path{fill:#aeb6c6} .vx-at{fill:var(--amber-solid)}
.vx-edge{fill:none;stroke:#b7bfce;stroke-width:1.8} .vx-edge-on{stroke:var(--primary);stroke-width:2.4} .vx-edge-end{stroke:var(--gold)}
.vx-edge-to{stroke:var(--amber-solid);stroke-dasharray:6 5;stroke-width:1.7}
.vx-edge-drag{stroke:var(--primary);stroke-width:2.2;stroke-dasharray:6 5} .vx-edge-drag-to{stroke:var(--amber-solid)}
.vx-node{position:absolute;z-index:10;background:#fff;border:1px solid var(--border);border-radius:12px;box-shadow:0 2px 6px rgba(20,30,55,.06);cursor:grab;user-select:none}
.vx-node:hover{box-shadow:0 6px 18px rgba(20,30,55,.12)}
.vx-node-on{border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-weak),0 8px 22px rgba(27,108,140,.16)}
.vx-node-root .vx-msg{border-top:3px solid var(--primary);border-radius:12px 12px 0 0}
.vx-msg{height:${MSG_H}px;padding:8px 11px;border-bottom:1px solid var(--line)}
.vx-msg-head{display:flex;align-items:center;gap:5px;height:18px}
.vx-id{display:inline-flex;align-items:center;gap:4px;font-family:ui-monospace,Menlo,monospace;font-size:11px;font-weight:650;color:var(--primary-ink)}
.vx-id-end{color:var(--gold)} .vx-id-lg{font-size:13px}
.vx-badge{display:inline-flex;align-items:center;gap:2px;font-size:9px;font-weight:700;text-transform:uppercase;color:var(--primary);background:var(--primary-weak);padding:1px 5px;border-radius:5px}
.vx-badge-final{color:var(--gold);background:var(--gold-weak)}
.vx-warn-mini{display:inline-flex;color:var(--amber-solid)}
.vx-time{margin-left:auto;display:inline-flex;align-items:center;gap:3px;font-size:11px;color:var(--muted)}
.vx-msg-text{margin-top:4px;font-size:12px;line-height:1.32;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.vx-optlabel{height:${LABEL_H}px;display:flex;align-items:center;padding:0 11px;font-size:10px;text-transform:uppercase;letter-spacing:.04em;color:var(--soft);font-weight:600;white-space:nowrap;overflow:hidden}
.vx-opt{position:relative;padding:0 11px;border-top:1px solid var(--line)}
.vx-opt-text{height:${OPT_TEXT_H}px;display:flex;align-items:center;font-size:12px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.vx-opt-dest{height:${OPT_DEST_H}px;display:flex;align-items:center;gap:6px}
.vx-opt-dest .vx-cardsel{flex:1;width:auto;min-width:0}
.vx-cardsel{flex:none;width:96px;height:24px;border:1px solid var(--border);border-radius:6px;background:#fff;font-size:11px;font-family:ui-monospace,monospace;padding:0 2px 0 5px;outline:none;cursor:pointer}
.vx-cardsel:hover{border-color:var(--primary)} .vx-cardsel:disabled{background:var(--bg);cursor:default}
.vx-cardsel-to{flex:1;min-width:0;color:var(--amber);border-color:var(--amber-line)}
.vx-opt-scores{display:flex;flex-direction:column}
.vx-score-row{height:${OPT_SCORE_ROW}px;display:flex;align-items:center;gap:8px}
.vx-sr-name{flex:none;width:34px;font-size:10px;font-weight:700;text-transform:uppercase;color:var(--soft);letter-spacing:.02em}
.vx-score-row input{flex:none;width:60px;height:21px;border:1px solid var(--border);border-radius:6px;background:#fff;text-align:center;font-size:12px;font-weight:600;font-variant-numeric:tabular-nums;color:var(--primary-ink);outline:none;padding:0 2px}
.vx-score-row input:focus{border-color:var(--primary);box-shadow:0 0 0 2px var(--primary-weak)}
.vx-score-row input:disabled{background:var(--bg);color:var(--soft)}
.vx-sr-acc{flex:1;text-align:right;font-size:10.5px;color:var(--soft)} .vx-sr-acc b{color:var(--primary-ink);font-size:12px;font-variant-numeric:tabular-nums;margin-left:2px}
.vx-score-empty{font-size:10.5px;color:var(--amber);font-style:italic;padding:4px 0}
.vx-mini-del{flex:none;width:22px;height:22px;display:inline-flex;align-items:center;justify-content:center;border:1px solid var(--border);border-radius:6px;background:var(--danger-weak);color:var(--danger);cursor:pointer}
.vx-mini-add{flex:none;width:22px;height:22px;display:inline-flex;align-items:center;justify-content:center;border:1px solid var(--border);border-radius:6px;background:var(--primary-weak);color:var(--primary);cursor:pointer}
.vx-mini-add-to{background:var(--amber-weak);color:var(--amber);border-color:var(--amber-line)}
.vx-timeout{position:relative;height:${TIMEOUT_H}px;display:flex;align-items:center;gap:6px;padding:0 11px;border-top:1px solid var(--line);background:#FCFBF7}
.vx-timeout svg{color:var(--amber-solid);flex:none}
.vx-timeout-l{flex:none;font-size:10.5px;font-weight:600;text-transform:uppercase;color:var(--soft)}
.vx-timeout-warn{background:var(--amber-weak);box-shadow:inset 2px 0 0 var(--amber-solid)}
.vx-foot{height:${FOOTER_H}px;display:flex;align-items:center;gap:6px;padding:0 9px;border-top:1px solid var(--line)}
.vx-foot-add{display:inline-flex;align-items:center;gap:4px;height:24px;padding:0 9px;border:1px dashed var(--border);border-radius:7px;background:transparent;color:var(--primary);font-size:11px;font-weight:600;cursor:pointer}
.vx-foot-add:hover{background:var(--primary-weak);border-color:var(--primary)}
.vx-foot-del{margin-left:auto;width:24px;height:24px;display:inline-flex;align-items:center;justify-content:center;border:1px solid var(--border);border-radius:7px;background:#fff;color:var(--danger);cursor:pointer}
.vx-end{border-color:var(--gold-line)} .vx-end.vx-node-on{border-color:var(--primary)}
.vx-end-head{height:${END_HEAD_H}px;display:flex;align-items:center;gap:6px;padding:0 11px;background:var(--gold-weak);border-bottom:1px solid var(--gold-line);border-radius:12px 12px 0 0}
.vx-end-del{margin-left:auto;width:24px;height:24px;display:inline-flex;align-items:center;justify-content:center;border:1px solid var(--gold-line);border-radius:7px;background:#fff;color:var(--danger);cursor:pointer}
.vx-end-note{height:${END_NOTE_H}px;display:flex;flex-direction:column;justify-content:center;gap:1px;padding:0 11px;border-bottom:1px solid var(--line)}
.vx-end-note-l{font-size:9.5px;text-transform:uppercase;letter-spacing:.05em;color:var(--soft);font-weight:700}
.vx-end-note-v{font-size:24px;font-weight:700;color:var(--gold);font-family:Georgia,serif} .vx-end-note-v small{font-size:11px;color:var(--muted);font-weight:600;margin-left:2px}
.vx-end-report{height:${END_REPORT_H}px;padding:8px 11px;display:flex;flex-direction:column}
.vx-end-report-label{font-size:10px;text-transform:uppercase;letter-spacing:.05em;color:var(--soft);font-weight:700;margin-bottom:5px} .vx-end-report-label em{color:var(--amber);font-style:normal}
.vx-end-report textarea{flex:1;width:100%;resize:none;border:1px solid var(--border);border-radius:8px;padding:7px 8px;font-size:12px;line-height:1.4;font-family:inherit;outline:none}
.vx-end-report textarea:focus{border-color:var(--gold);box-shadow:0 0 0 3px var(--gold-weak)} .vx-end-report-warn{border-color:var(--amber-line);background:var(--amber-weak)}
.vx-port{position:absolute;width:12px;height:12px;border-radius:50%;background:#fff;border:2px solid var(--primary);z-index:12}
.vx-port-in{left:-7px;top:${MSG_H / 2}px;transform:translateY(-50%);border-color:#aeb6c6}
.vx-port-end{top:${END_HEAD_H / 2}px;border-color:var(--gold)}
.vx-port-out{right:-7px;top:${OPT_PORT}px;transform:translateY(-50%);cursor:crosshair} .vx-port-out:hover{background:var(--primary)}
.vx-port-to{right:-7px;top:50%;transform:translateY(-50%);border-color:var(--amber-solid);cursor:crosshair} .vx-port-to:hover{background:var(--amber-solid)}
.vx-legend{position:absolute;left:14px;bottom:12px;display:flex;align-items:center;gap:14px;flex-wrap:wrap;background:rgba(252,250,246,.92);border:1px solid var(--border);border-radius:9px;padding:6px 11px;max-width:96%}
.vx-leg{display:inline-flex;align-items:center;gap:6px;font-size:11px;font-weight:550} .vx-leg svg{color:var(--gold)}
.vx-sw{width:18px;border-top:2px dashed var(--amber-solid);display:inline-block} .vx-leg-hint{font-size:10.5px;color:var(--soft)}
.vx-inspector{width:344px;flex:none;background:var(--panel);border-left:1px solid var(--border);overflow-y:auto}
.vx-insp-empty{height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10px;padding:30px;text-align:center;color:var(--muted)} .vx-insp-empty svg{color:var(--soft)}
.vx-insp-body{padding:16px}
.vx-insp-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
.vx-x{width:28px;height:28px;display:inline-flex;align-items:center;justify-content:center;border:1px solid var(--border);border-radius:8px;background:#fff;color:var(--muted);cursor:pointer}
.vx-insp-tag{display:inline-block;font-size:10px;font-weight:700;text-transform:uppercase;color:var(--gold);background:var(--gold-weak);padding:3px 8px;border-radius:6px;margin-bottom:12px}
.vx-insp-note{display:flex;align-items:center;justify-content:space-between;border:1px solid var(--gold-line);background:var(--gold-weak);border-radius:10px;padding:10px 11px;margin-bottom:8px;font-size:11px;font-weight:700;text-transform:uppercase;color:var(--gold)}
.vx-insp-note b{font-family:Georgia,serif;font-size:24px;color:var(--gold)} .vx-insp-note small{font-size:11px;color:var(--muted)}
.vx-insp-acc{display:flex;flex-wrap:wrap;gap:10px;background:var(--bg);border:1px solid var(--border);border-radius:9px;padding:8px 11px;margin-bottom:12px;font-size:11px;color:var(--muted)} .vx-insp-acc span{display:inline-flex;gap:4px} .vx-insp-acc b{color:var(--primary-ink)}
.vx-field{display:block;margin-bottom:12px}
.vx-field>span{display:block;font-size:11px;font-weight:600;color:var(--muted);margin-bottom:5px}
.vx-field textarea,.vx-field input{width:100%;border:1px solid var(--border);border-radius:8px;background:#fff;padding:7px 9px;font-size:13px;font-family:inherit;outline:none}
.vx-field textarea:focus,.vx-field input:focus{border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-weak)}
.vx-field-warn textarea{border-color:var(--amber-line);background:var(--amber-weak)}
.vx-insp-sub{font-size:11px;text-transform:uppercase;letter-spacing:.06em;color:var(--soft);font-weight:700;margin:4px 0 8px}
.vx-opt-card{border:1px solid var(--border);border-radius:10px;padding:9px;margin-bottom:9px;background:var(--bg)}
.vx-opt-input{width:100%;border:1px solid var(--border);border-radius:7px;padding:6px 8px;font-size:12.5px;font-family:inherit;background:#fff;outline:none}
.vx-opt-input:focus{border-color:var(--primary);box-shadow:0 0 0 3px var(--primary-weak)}
.vx-opt-scores-lg{margin-top:7px} .vx-opt-scores-lg .vx-score-row{height:26px} .vx-opt-scores-lg input{height:23px;font-size:13px}
.vx-opt-meta{display:flex;align-items:center;gap:7px;margin-top:7px}
.vx-opt-meta select{flex:1;border:1px solid var(--border);border-radius:7px;padding:5px 6px;font-size:12px;background:#fff;font-family:inherit;outline:none}
.vx-opt-del{width:28px;height:28px;flex:none;display:inline-flex;align-items:center;justify-content:center;border:1px solid var(--border);border-radius:7px;background:#fff;color:var(--danger);cursor:pointer}
.vx-add-out{width:100%;display:inline-flex;align-items:center;justify-content:center;gap:6px;height:34px;border:1px dashed var(--primary);border-radius:9px;background:var(--primary-weak);color:var(--primary-ink);font-size:12.5px;font-weight:600;cursor:pointer}
.vx-board{border-top:1px solid var(--border);background:var(--panel)}
.vx-board-head{padding:11px 14px 6px} .vx-board-head h3{margin:0;font-size:13px;font-weight:700} .vx-board-head p{margin:3px 0 0;font-size:11.5px;line-height:1.4;color:var(--muted)}
.vx-board-scroll{max-height:226px;overflow:auto;padding:0 14px}
.vx-tbl{width:100%;border-collapse:collapse;font-size:12px}
.vx-tbl th{position:sticky;top:0;z-index:1;background:var(--panel);text-align:left;font-size:10px;text-transform:uppercase;letter-spacing:.04em;color:var(--soft);font-weight:700;padding:7px 8px;border-bottom:1px solid var(--border)}
.vx-tbl th.vx-th-n{text-align:center;width:58px}
.vx-tbl td{padding:5px 8px;border-bottom:1px solid var(--line);vertical-align:middle}
.vx-tbl tbody tr{cursor:pointer} .vx-tbl tbody tr:hover{background:var(--bg)} .vx-tr-on{background:var(--primary-weak)!important}
.vx-td-step .vx-id{font-size:11px}
.vx-td-txt{max-width:280px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;color:var(--ink)} .vx-td-txt em{color:var(--soft);font-style:normal}
.vx-td-n{text-align:center} .vx-td-n input{width:50px;height:25px;border:1px solid var(--border);border-radius:6px;text-align:center;font-size:12px;font-variant-numeric:tabular-nums;outline:none;background:#fff} .vx-td-n input:focus{border-color:var(--primary);box-shadow:0 0 0 2px var(--primary-weak)} .vx-td-n input:disabled{background:var(--bg);color:var(--soft)}
.vx-td-out em{color:var(--soft);font-style:normal} .vx-out-pill{font-family:ui-monospace,Menlo,monospace;font-size:10px;color:var(--primary-ink);background:var(--primary-weak);border-radius:5px;padding:1px 5px}
.vx-ends{display:flex;flex-wrap:wrap;gap:7px;padding:10px 14px 13px;border-top:1px solid var(--line)}
.vx-endchip{display:inline-flex;align-items:center;gap:6px;border:1px solid var(--gold-line);background:var(--gold-weak);border-radius:8px;padding:5px 10px;font-size:11px;cursor:pointer;color:var(--gold)}
.vx-endchip svg{color:var(--gold)} .vx-endchip b{font-family:ui-monospace,Menlo,monospace;font-weight:650}
.vx-endchip span{font-family:Georgia,serif;font-size:15px;font-weight:700} .vx-endchip span small{font-size:9px;color:var(--muted);font-weight:600;margin-left:1px}
.vx-endchip i{font-style:normal;color:var(--amber-solid);font-weight:800}
.vx-endchip-on{border-color:var(--primary);box-shadow:0 0 0 2px var(--primary-weak)}
`;
