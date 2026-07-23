import type { SimulationVersionDetailResponse } from "@/lib/api/praxis";

export const PREVIEW_LONG_PATH_THRESHOLD = 12;

export type PreviewProblemKind =
  | "missing-destination"
  | "missing-node"
  | "cycle"
  | "long-path"
  | "unreachable-final"
  | "no-reachable-final";

export interface PreviewFlowProblem {
  kind: PreviewProblemKind;
  nodeId: string | null;
  optionId: string | null;
  message: string;
}

export interface PreviewGraphAnalysis {
  rootNodeId: string;
  reachableNodeIds: string[];
  unreachableNodeIds: string[];
  finalNodeIds: string[];
  reachableFinalNodeIds: string[];
  cycleNodeIds: string[];
  longPathNodeIds: string[];
  problems: PreviewFlowProblem[];
  optionKeys: string[];
}

export interface PreviewCoverage {
  visitedNodes: number;
  totalNodes: number;
  visitedOptions: number;
  totalOptions: number;
  visitedFinals: number;
  totalFinals: number;
  nodePercent: number;
  optionPercent: number;
  finalPercent: number;
}

export function previewOptionKey(nodeId: string, optionId: string): string {
  return `${nodeId}:${optionId}`;
}

export function analyzePreviewGraph(
  version: SimulationVersionDetailResponse,
): PreviewGraphAnalysis {
  const nodeById = new Map(version.nodes.map((node) => [node.id, node]));
  const rootNodeId = version.blueprint.rootNodeId;
  const reachable = new Set<string>();
  const queue = nodeById.has(rootNodeId) ? [rootNodeId] : [];
  const problems: PreviewFlowProblem[] = [];
  const optionKeys: string[] = [];

  while (queue.length > 0) {
    const nodeId = queue.shift()!;
    if (reachable.has(nodeId)) continue;
    reachable.add(nodeId);
    const node = nodeById.get(nodeId);
    if (!node) continue;

    for (const option of node.options) {
      optionKeys.push(previewOptionKey(node.id, option.id));
      if (!option.nextNodeId) {
        if (!node.isFinal) {
          problems.push({
            kind: "missing-destination",
            nodeId: node.id,
            optionId: option.id,
            message: `A alternativa “${option.text}” não possui destino.`,
          });
        }
        continue;
      }
      if (!nodeById.has(option.nextNodeId)) {
        problems.push({
          kind: "missing-node",
          nodeId: node.id,
          optionId: option.id,
          message: `A alternativa “${option.text}” aponta para uma etapa inexistente.`,
        });
        continue;
      }
      queue.push(option.nextNodeId);
    }

    if (node.timeoutNextNodeId) {
      if (!nodeById.has(node.timeoutNextNodeId)) {
        problems.push({
          kind: "missing-node",
          nodeId: node.id,
          optionId: null,
          message: "O destino configurado para tempo esgotado não existe.",
        });
      } else {
        queue.push(node.timeoutNextNodeId);
      }
    }
  }

  for (const node of version.nodes) {
    for (const option of node.options) {
      const key = previewOptionKey(node.id, option.id);
      if (!optionKeys.includes(key)) optionKeys.push(key);
    }
  }

  const finalNodeIds = version.nodes.filter((node) => node.isFinal).map((node) => node.id);
  const reachableFinalNodeIds = finalNodeIds.filter((nodeId) => reachable.has(nodeId));
  for (const nodeId of finalNodeIds.filter((candidate) => !reachable.has(candidate))) {
    problems.push({
      kind: "unreachable-final",
      nodeId,
      optionId: null,
      message: "Esta etapa final não pode ser alcançada a partir da raiz.",
    });
  }
  if (reachableFinalNodeIds.length === 0) {
    problems.push({
      kind: "no-reachable-final",
      nodeId: rootNodeId || null,
      optionId: null,
      message: "Nenhum encerramento válido pode ser alcançado a partir da raiz.",
    });
  }

  const cycleNodeIds = new Set<string>();
  const longPathNodeIds = new Set<string>();
  const recordedCycles = new Set<string>();
  const recordedLongPaths = new Set<string>();
  let exploredPaths = 0;
  const explorationLimit = Math.max(200, version.nodes.length * version.nodes.length * 8);

  function walk(nodeId: string, path: string[], stack: Set<string>): void {
    if (exploredPaths >= explorationLimit) return;
    exploredPaths += 1;
    const node = nodeById.get(nodeId);
    if (!node) return;

    if (stack.has(nodeId)) {
      const cycleStart = path.indexOf(nodeId);
      const cycle = [...path.slice(Math.max(0, cycleStart)), nodeId];
      const cycleKey = cycle.join("->");
      if (!recordedCycles.has(cycleKey)) {
        recordedCycles.add(cycleKey);
        cycle.forEach((id) => cycleNodeIds.add(id));
        problems.push({
          kind: "cycle",
          nodeId,
          optionId: null,
          message: `Ciclo detectado: ${cycle.join(" → ")}.`,
        });
      }
      return;
    }

    const nextPath = [...path, nodeId];
    if (nextPath.length > PREVIEW_LONG_PATH_THRESHOLD) {
      const pathKey = nextPath.join("->");
      if (!recordedLongPaths.has(pathKey)) {
        recordedLongPaths.add(pathKey);
        nextPath.forEach((id) => longPathNodeIds.add(id));
        problems.push({
          kind: "long-path",
          nodeId,
          optionId: null,
          message: `Caminho com ${nextPath.length} etapas excede o limite de revisão de ${PREVIEW_LONG_PATH_THRESHOLD}.`,
        });
      }
    }

    const nextStack = new Set(stack);
    nextStack.add(nodeId);
    const targets = [
      ...node.options.map((option) => option.nextNodeId),
      node.timeoutNextNodeId,
    ].filter((target): target is string => Boolean(target) && nodeById.has(target));

    for (const target of targets) walk(target, nextPath, nextStack);
  }

  if (nodeById.has(rootNodeId)) walk(rootNodeId, [], new Set());

  return {
    rootNodeId,
    reachableNodeIds: [...reachable],
    unreachableNodeIds: version.nodes
      .map((node) => node.id)
      .filter((nodeId) => !reachable.has(nodeId)),
    finalNodeIds,
    reachableFinalNodeIds,
    cycleNodeIds: [...cycleNodeIds],
    longPathNodeIds: [...longPathNodeIds],
    problems,
    optionKeys,
  };
}

export function calculatePreviewCoverage(
  version: SimulationVersionDetailResponse,
  analysis: PreviewGraphAnalysis,
  visitedNodeIds: Iterable<string>,
  visitedOptionKeys: Iterable<string>,
): PreviewCoverage {
  const visitedNodes = new Set(visitedNodeIds);
  const visitedOptions = new Set(visitedOptionKeys);
  const visitedFinals = analysis.finalNodeIds.filter((nodeId) => visitedNodes.has(nodeId)).length;

  return {
    visitedNodes: version.nodes.filter((node) => visitedNodes.has(node.id)).length,
    totalNodes: version.nodes.length,
    visitedOptions: analysis.optionKeys.filter((key) => visitedOptions.has(key)).length,
    totalOptions: analysis.optionKeys.length,
    visitedFinals,
    totalFinals: analysis.finalNodeIds.length,
    nodePercent: percent(visitedNodes.size, version.nodes.length),
    optionPercent: percent(visitedOptions.size, analysis.optionKeys.length),
    finalPercent: percent(visitedFinals, analysis.finalNodeIds.length),
  };
}

function percent(value: number, total: number): number {
  if (total <= 0) return 0;
  return Math.min(100, Math.round((value / total) * 100));
}
