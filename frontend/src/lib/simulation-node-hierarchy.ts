import type { SimulationVersionNodeResponse } from "@/lib/api/praxis";

export type NodeDisplayCodes = Map<string, string>;

export function buildNodeDisplayCodes(
  nodes: SimulationVersionNodeResponse[],
  rootNodeId: string | undefined,
): NodeDisplayCodes {
  const byId = new Map(nodes.map((node) => [node.id, node]));
  const codes = new Map<string, string>();
  const visiting = new Set<string>();

  function visit(nodeId: string, code: string) {
    if (codes.has(nodeId)) return;
    const node = byId.get(nodeId);
    if (!node) return;

    codes.set(nodeId, code);
    if (!visiting.add(nodeId)) return;

    const targets: string[] = [];
    const seenTargets = new Set<string>();
    const sortedOptions = [...node.options].sort((left, right) =>
      left.id.localeCompare(right.id, "pt-BR", { numeric: true }),
    );

    for (const option of sortedOptions) {
      const targetId = option.nextNodeId;
      if (!targetId || seenTargets.has(targetId) || !byId.has(targetId)) continue;
      seenTargets.add(targetId);
      targets.push(targetId);
    }

    if (
      node.timeoutNextNodeId &&
      !seenTargets.has(node.timeoutNextNodeId) &&
      byId.has(node.timeoutNextNodeId)
    ) {
      targets.push(node.timeoutNextNodeId);
    }

    targets.forEach((targetId, index) => visit(targetId, `${code}.${index + 1}`));
    visiting.delete(nodeId);
  }

  if (rootNodeId && byId.has(rootNodeId)) {
    visit(rootNodeId, "1");
  }

  const usedCodes = new Set(codes.values());
  let nextTopLevel = 1;
  [...nodes]
    .sort((left, right) => left.turnIndex - right.turnIndex)
    .forEach((node) => {
      if (codes.has(node.id)) return;
      while (usedCodes.has(String(nextTopLevel))) nextTopLevel += 1;
      const code = String(nextTopLevel);
      codes.set(node.id, code);
      usedCodes.add(code);
      nextTopLevel += 1;
    });

  return codes;
}

export function compareDisplayCodes(left: string, right: string) {
  const leftParts = left.split(".").map(Number);
  const rightParts = right.split(".").map(Number);
  const length = Math.max(leftParts.length, rightParts.length);

  for (let index = 0; index < length; index += 1) {
    const leftValue = leftParts[index] ?? -1;
    const rightValue = rightParts[index] ?? -1;
    if (leftValue !== rightValue) return leftValue - rightValue;
  }

  return 0;
}

export function displayCodeDepth(code: string) {
  return Math.max(0, code.split(".").length - 1);
}
