/**
 * Rótulos de etapa exibidos ao usuário (1.0, 2.0, 3.1…), derivados do turno e da
 * posição do card dentro daquele turno — os mesmos que o mapa do validador mostra.
 * O identificador interno do backend ("turno-N", ou o alias legado "etapa-N")
 * nunca deve aparecer na interface.
 */

interface StepLike {
  id: string;
  turnIndex: number;
}

export function buildStepLabels(nodes: StepLike[]): Map<string, string> {
  const labels = new Map<string, string>();
  const rowsByTurn = new Map<number, StepLike[]>();
  const orderedNodes = [...nodes].sort(
    (a, b) => a.turnIndex - b.turnIndex || a.id.localeCompare(b.id),
  );

  orderedNodes.forEach((node) => {
    rowsByTurn.set(node.turnIndex, [...(rowsByTurn.get(node.turnIndex) ?? []), node]);
  });

  rowsByTurn.forEach((turnNodes, turnIndex) => {
    turnNodes.forEach((node, rowIndex) => {
      labels.set(node.id, `${turnIndex}.${rowIndex}`);
    });
  });

  return labels;
}

/** Rótulo de uma etapa; aceita o alias legado "etapa-N" e cai para o próprio id. */
export function stepLabelOf(labels: Map<string, string>, nodeId: string): string {
  return labels.get(nodeId) ?? labels.get(nodeId.replace(/^etapa-(\d+)$/, "turno-$1")) ?? nodeId;
}

/** Troca ids internos citados em mensagens do diagnóstico pelos rótulos do mapa. */
export function localizeStepIds(text: string, labels: Map<string, string>): string {
  return text.replace(
    /\b(?:turno|etapa)-(\d+)\b/g,
    (match, turn: string) => labels.get(`turno-${turn}`) ?? match,
  );
}
