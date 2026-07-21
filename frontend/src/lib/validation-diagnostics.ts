import type {
  SimulationVersionDetailResponse,
  ValidationIssueResponse,
} from "@/lib/api/praxis";

export type ValidationEditor = "avaliacao" | "dialogo" | "mapa";

export interface ValidationDiagnostic {
  issue: ValidationIssueResponse;
  nodeId: string | null;
  nodeLabel: string;
  optionId: string | null;
  optionLabel: string | null;
  optionText: string | null;
  fieldLabel: string;
  resolution: string;
  editor: ValidationEditor;
}

export function buildValidationDiagnostics(
  version: SimulationVersionDetailResponse | undefined,
  issues: ValidationIssueResponse[],
): ValidationDiagnostic[] {
  const nodes = version?.nodes ?? [];
  const nodeById = new Map(nodes.map((node) => [node.id, node]));
  const nodeIds = new Set(nodes.map((node) => node.id));
  const occurrences = new Map<string, number>();

  return issues.map((issue) => {
    const node = issue.nodeId ? nodeById.get(issue.nodeId) : undefined;
    const matchingOptions = node ? findMatchingOptions(issue.message, node, nodeById, nodeIds) : [];
    const occurrenceKey = `${issue.nodeId ?? "global"}|${issue.message}`;
    const occurrence = occurrences.get(occurrenceKey) ?? 0;
    occurrences.set(occurrenceKey, occurrence + 1);
    const option = matchingOptions[occurrence] ?? matchingOptions[0] ?? null;
    const optionIndex = option ? node?.options.findIndex((candidate) => candidate.id === option.id) ?? -1 : -1;
    const metadata = describeIssue(issue.message);

    return {
      issue,
      nodeId: issue.nodeId,
      nodeLabel: node ? `Etapa ${node.turnIndex}` : issue.nodeId ? `Etapa ${issue.nodeId}` : "Regra global",
      optionId: option?.id ?? null,
      optionLabel: optionIndex >= 0 && option ? formatOptionLabel(optionIndex, option.text) : null,
      optionText: option?.text ?? null,
      fieldLabel: metadata.fieldLabel,
      resolution: buildResolution(metadata.resolution, optionIndex, option?.text),
      editor: metadata.editor,
    };
  });
}

function findMatchingOptions(
  message: string,
  node: SimulationVersionDetailResponse["nodes"][number],
  nodeById: Map<string, SimulationVersionDetailResponse["nodes"][number]>,
  nodeIds: Set<string>,
) {
  const normalized = message.toLowerCase();

  if (normalized.includes("resposta está sem destino")) {
    return node.options.filter((option) => option.nextNodeId == null);
  }
  if (normalized.includes("resposta aponta para uma etapa que não existe")) {
    return node.options.filter(
      (option) => option.nextNodeId != null && !nodeIds.has(option.nextNodeId),
    );
  }
  if (normalized.includes("resposta leva para esta etapa ou para uma etapa anterior")) {
    return node.options.filter((option) => {
      if (!option.nextNodeId) return false;
      const target = nodeById.get(option.nextNodeId);
      return target != null && target.turnIndex <= node.turnIndex;
    });
  }
  if (normalized.includes("pontuação de competência está fora")) {
    return node.options.filter((option) =>
      Object.values(option.competencyLevels).some((score) => score < 0 || score > 100),
    );
  }

  return [];
}

function formatOptionLabel(optionIndex: number, optionText: string): string {
  const preview = optionPreview(optionText);
  return preview ? `Resposta ${optionIndex + 1}: “${preview}”` : `Resposta ${optionIndex + 1}`;
}

function buildResolution(
  resolution: string,
  optionIndex: number,
  optionText: string | undefined,
): string {
  if (optionIndex < 0 || !optionText) return resolution;
  return `${resolution} A pendência está na Resposta ${optionIndex + 1}: “${optionPreview(optionText)}”.`;
}

function optionPreview(optionText: string): string {
  const normalized = optionText.trim().replace(/\s+/g, " ");
  return normalized.length > 100 ? `${normalized.slice(0, 97).trimEnd()}...` : normalized;
}

function describeIssue(message: string): {
  fieldLabel: string;
  resolution: string;
  editor: ValidationEditor;
} {
  const normalized = message.toLowerCase();

  if (normalized.includes("peso") || normalized.includes("competência") && normalized.includes("configurad")) {
    return {
      fieldLabel: "Competências e pesos",
      resolution: "Abra o passo Avaliação, ajuste as competências e confirme que os pesos somam 100%.",
      editor: "avaliacao",
    };
  }
  if (normalized.includes("tempo esgotado") || normalized.includes("tempo acaba") || normalized.includes("timeout")) {
    return {
      fieldLabel: "Destino quando o tempo acaba",
      resolution: "Abra o Mapa, selecione esta etapa e defina o destino de tempo esgotado. Se a etapa não tiver limite, mantenha-a como Sem limite.",
      editor: "mapa",
    };
  }
  if (normalized.includes("destino") || normalized.includes("leva para") || normalized.includes("aponta para")) {
    return {
      fieldLabel: "Destino da resposta",
      resolution: "Abra o Mapa, selecione esta etapa e escolha uma etapa posterior ou Finalizar avaliação para a resposta indicada.",
      editor: "mapa",
    };
  }
  if (normalized.includes("pontuação")) {
    return {
      fieldLabel: "Pontuação da resposta",
      resolution: "Abra o Editor de diálogo e informe, para cada competência, uma pontuação inteira entre 0 e 100.",
      editor: "dialogo",
    };
  }
  if (normalized.includes("fala") || normalized.includes("mensagem")) {
    return {
      fieldLabel: "Mensagem da etapa",
      resolution: "Abra o Editor de diálogo e preencha a mensagem que será apresentada antes das respostas.",
      editor: "dialogo",
    };
  }
  if (normalized.includes("resposta(s)") || normalized.includes("alternativa")) {
    return {
      fieldLabel: "Alternativas",
      resolution: "Abra o Editor de diálogo e mantenha entre 2 e 4 alternativas completas nesta etapa.",
      editor: "dialogo",
    };
  }
  if (normalized.includes("relatório") || normalized.includes("encerramento")) {
    return {
      fieldLabel: "Encerramento",
      resolution: "Abra o Editor de diálogo ou o Mapa e complete a configuração da etapa de encerramento indicada.",
      editor: "dialogo",
    };
  }

  return {
    fieldLabel: "Configuração da etapa",
    resolution: message,
    editor: "dialogo",
  };
}
