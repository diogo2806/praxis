import { useEffect, useMemo, useState } from "react";
import { Flag, Save } from "lucide-react";

import type {
  SimulationVersionNodeResponse,
  SimulationVersionOptionResponse,
  UpdateBlueprintCompetencyRequest,
} from "@/lib/api/praxis";

type PathStep = {
  node: SimulationVersionNodeResponse;
  pickedOption: SimulationVersionOptionResponse | null;
};

type CompetencyResult = {
  name: string;
  sum: number;
  percentage: number | null;
};

export interface TerminalOutcomeCardProps {
  nodes: SimulationVersionNodeResponse[];
  rootNodeId: string | undefined;
  node: SimulationVersionNodeResponse;
  option?: SimulationVersionOptionResponse;
  outcome?: "option" | "timeout";
  competencies: UpdateBlueprintCompetencyRequest[];
  reportText: string;
  disabled?: boolean;
  saving?: boolean;
  onSaveReport: (reportText: string) => void;
}

export function TerminalOutcomeCard({
  nodes,
  rootNodeId,
  node,
  option,
  outcome = "option",
  competencies,
  reportText: persistedReportText,
  disabled = false,
  saving = false,
  onSaveReport,
}: TerminalOutcomeCardProps) {
  const [reportText, setReportText] = useState(persistedReportText);

  useEffect(() => {
    setReportText(persistedReportText);
  }, [option?.id, outcome, persistedReportText]);

  const result = useMemo(
    () => calculateTerminalResult(nodes, rootNodeId, node, option ?? null, competencies),
    [nodes, rootNodeId, node, option, competencies],
  );

  const persistedReport = persistedReportText.trim();
  const normalizedReport = reportText.trim();
  const reportMissing = normalizedReport.length === 0;
  const reportChanged = normalizedReport !== persistedReport;
  const isTimeoutOutcome = outcome === "timeout";

  return (
    <section className="mt-3 rounded-md border border-primary/25 bg-primary/5 p-3">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <div className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary">
            <Flag className="h-3.5 w-3.5" />
            {isTimeoutOutcome ? "Encerramento por tempo esgotado" : "Encerramento desta alternativa"}
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            {isTimeoutOutcome
              ? "A pontuação considera o caminho até esta etapa e atribui 0 pontos nesta etapa, pois não houve resposta."
              : "A pontuação abaixo é calculada pelo caminho até esta resposta e não pode ser editada aqui."}
          </p>
        </div>
        <div className="rounded-md border border-primary/20 bg-background px-3 py-2 text-right">
          <div className="text-[10px] uppercase tracking-wide text-muted-foreground">Resultado ponderado</div>
          <div className="text-lg font-semibold text-primary">{result.finalPercentage}%</div>
        </div>
      </div>

      <div className="mt-3 grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
        {result.competencies.map((competency) => (
          <div key={competency.name} className="rounded-md border border-border bg-background px-3 py-2">
            <div className="truncate text-xs font-medium" title={competency.name}>
              {competency.name}
            </div>
            <div className="mt-1 flex items-end justify-between gap-2">
              <span className="text-xs text-muted-foreground">
                Soma <strong className="text-foreground">{competency.sum}</strong>
              </span>
              <span className="text-base font-semibold text-primary">
                {competency.percentage == null ? "—" : `${competency.percentage}%`}
              </span>
            </div>
          </div>
        ))}
      </div>

      <label className="mt-3 block">
        <span className="mb-1.5 block text-xs font-medium text-foreground">
          Texto do relatório <span className="text-danger">*</span>
        </span>
        <textarea
          className="input min-h-24"
          value={reportText}
          disabled={disabled || saving}
          maxLength={1000}
          onChange={(event) => setReportText(event.target.value)}
          placeholder="Descreva o que este encerramento representa para a equipe responsável."
        />
        <span className="mt-1 block text-xs text-muted-foreground">
          {isTimeoutOutcome
            ? "Este texto será registrado na trilha do resultado quando o tempo acabar e a avaliação for finalizada."
            : "Este texto será registrado na trilha do resultado quando a pessoa candidata concluir por esta alternativa."}
        </span>
      </label>

      <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
        <span className={reportMissing ? "text-xs text-danger" : "text-xs text-muted-foreground"}>
          {reportMissing ? "Preencha o texto do relatório para liberar a revisão." : `${reportText.length}/1000 caracteres`}
        </span>
        <button
          type="button"
          disabled={disabled || saving || reportMissing || !reportChanged}
          onClick={() => onSaveReport(normalizedReport)}
          className="inline-flex items-center gap-2 rounded-md bg-primary px-3 py-2 text-xs font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <Save className="h-3.5 w-3.5" />
          {saving ? "Salvando..." : "Salvar relatório"}
        </button>
      </div>
    </section>
  );
}

function calculateTerminalResult(
  nodes: SimulationVersionNodeResponse[],
  rootNodeId: string | undefined,
  terminalNode: SimulationVersionNodeResponse,
  terminalOption: SimulationVersionOptionResponse | null,
  competencies: UpdateBlueprintCompetencyRequest[],
) {
  const path = findPath(nodes, rootNodeId, terminalNode.id);
  const steps: PathStep[] = [...path, { node: terminalNode, pickedOption: terminalOption }];

  const raw = new Map<string, number>();
  const maximum = new Map<string, number>();
  competencies.forEach((competency) => {
    raw.set(competency.name, 0);
    maximum.set(competency.name, 0);
  });

  steps.forEach((step) => {
    competencies.forEach((competency) => {
      const bestAtNode = step.node.options.reduce(
        (best, candidate) => Math.max(best, candidate.competencyLevels?.[competency.name] ?? 0),
        0,
      );
      const gained = step.pickedOption?.competencyLevels?.[competency.name] ?? 0;
      maximum.set(competency.name, (maximum.get(competency.name) ?? 0) + bestAtNode);
      raw.set(competency.name, (raw.get(competency.name) ?? 0) + gained);
    });
  });

  const competencyResults: CompetencyResult[] = competencies.map((competency) => {
    const sum = raw.get(competency.name) ?? 0;
    const max = maximum.get(competency.name) ?? 0;
    return {
      name: competency.name,
      sum,
      percentage: max > 0 ? Math.round((sum / max) * 100) : null,
    };
  });

  const covered = competencyResults.filter((competency) => competency.percentage != null);
  const configuredWeightSum = covered.reduce((sum, competency) => {
    const configured = competencies.find((candidate) => candidate.name === competency.name);
    return sum + (configured?.weight ?? 0);
  }, 0);

  const weighted = covered.reduce((sum, competency) => {
    const configured = competencies.find((candidate) => candidate.name === competency.name);
    const normalized = (competency.percentage ?? 0) / 100;
    const weight =
      configuredWeightSum > 0
        ? (configured?.weight ?? 0) / configuredWeightSum
        : covered.length > 0
          ? 1 / covered.length
          : 0;
    return sum + normalized * weight;
  }, 0);

  return {
    competencies: competencyResults,
    finalPercentage: Math.round(weighted * 100),
  };
}

function findPath(
  nodes: SimulationVersionNodeResponse[],
  rootNodeId: string | undefined,
  targetNodeId: string,
): PathStep[] {
  if (!rootNodeId || rootNodeId === targetNodeId) return [];
  const byId = new Map(nodes.map((node) => [node.id, node]));
  const visiting = new Set<string>();

  function visit(nodeId: string): PathStep[] | null {
    if (nodeId === targetNodeId) return [];
    if (!visiting.add(nodeId)) return null;

    const current = byId.get(nodeId);
    if (!current || current.isFinal) {
      visiting.delete(nodeId);
      return null;
    }

    for (const option of current.options) {
      if (!option.nextNodeId) continue;
      const childPath = visit(option.nextNodeId);
      if (childPath) {
        visiting.delete(nodeId);
        return [{ node: current, pickedOption: option }, ...childPath];
      }
    }

    if (current.timeoutNextNodeId) {
      const timeoutPath = visit(current.timeoutNextNodeId);
      if (timeoutPath) {
        visiting.delete(nodeId);
        return [{ node: current, pickedOption: null }, ...timeoutPath];
      }
    }

    visiting.delete(nodeId);
    return null;
  }

  return visit(rootNodeId) ?? [];
}
