import { AlertTriangle, CheckCircle2, Info } from "lucide-react";
import type {
  CalibrationFlag,
  CalibrationReportResponse,
  CompetencyCalibrationDto,
  OptionDiscriminationDto,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

const FLAG_META: Record<
  CalibrationFlag,
  { label: string; dot: string; badge: string; icon: typeof CheckCircle2 }
> = {
  OK: {
    label: "OK",
    dot: "🟢",
    badge: "border-success/30 bg-success/10 text-success",
    icon: CheckCircle2,
  },
  FRACO: {
    label: "Fraco",
    dot: "🟡",
    badge: "border-warning/40 bg-warning/15 text-warning-foreground",
    icon: AlertTriangle,
  },
  REVISAR: {
    label: "Revisar",
    dot: "🔴",
    badge: "border-danger/40 bg-danger/10 text-danger",
    icon: AlertTriangle,
  },
};

function explain(item: OptionDiscriminationDto): string {
  switch (item.flag) {
    case "REVISAR":
      return "Padrão de escolha invertido em relação à pontuação — a calibração desta opção pode estar trocada. Revise os pesos.";
    case "FRACO":
      return "Esta opção separa pouco quem performa bem de quem performa mal. Considere reescrever o texto ou ajustar a pontuação.";
    default:
      return "Esta opção discrimina bem entre os perfis de desempenho.";
  }
}

/**
 * Relatório de calibração: tabela de opções com badge de status e um gráfico
 * de barras simples da distribuição (média) por competência.
 */
export function CalibrationReport({ report }: { report: CalibrationReportResponse }) {
  if (!report.sufficientSample) {
    return (
      <div className="rounded-md border border-border bg-muted/30 p-4 text-sm text-muted-foreground">
        <div className="flex items-center gap-2 font-medium text-foreground">
          <Info className="h-4 w-4" />
          Calibração ainda não disponível
        </div>
        <p className="mt-1">
          A calibração fica disponível a partir de {report.minimumSampleRequired} tentativas
          concluídas (você tem {report.sampleSize}).
        </p>
      </div>
    );
  }

  const byNode = new Map<string, OptionDiscriminationDto[]>();
  for (const item of report.items) {
    const list = byNode.get(item.nodeId) ?? [];
    list.push(item);
    byNode.set(item.nodeId, list);
  }

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-sm font-semibold">
          Calibração — baseada em {report.sampleSize} tentativas concluídas
        </h3>
        <p className="mt-1 text-xs text-muted-foreground">
          Índice de discriminação compara quem teve nota final alta x baixa. Índice de dificuldade é
          a proporção que escolheu a opção.
        </p>
      </div>

      <div className="space-y-4">
        {[...byNode.entries()].map(([nodeId, options]) => (
          <div key={nodeId} className="rounded-md border border-border bg-card">
            <div className="border-b border-border px-4 py-2 font-mono text-[11px] uppercase text-primary">
              {nodeId}
            </div>
            <ul className="divide-y divide-border">
              {options.map((option) => (
                <CalibrationRow key={option.optionId} option={option} />
              ))}
            </ul>
          </div>
        ))}
        {byNode.size === 0 && (
          <p className="text-sm text-muted-foreground">Nenhuma opção com dados suficientes.</p>
        )}
      </div>

      <CompetencyDistribution competencies={report.competencies} />
    </div>
  );
}

function CalibrationRow({ option }: { option: OptionDiscriminationDto }) {
  const meta = FLAG_META[option.flag];
  const Icon = meta.icon;
  return (
    <li className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-start sm:justify-between">
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <span className="font-mono text-[11px] text-muted-foreground">{option.optionId}</span>
          <span
            className={cn(
              "inline-flex items-center gap-1 rounded-md border px-1.5 py-0.5 text-[10px] font-medium",
              meta.badge,
            )}
          >
            <Icon className="h-3 w-3" />
            {meta.dot} {meta.label}
          </span>
        </div>
        <p className="mt-1 line-clamp-2 text-sm text-foreground/85">{option.optionLabel}</p>
        <p className="mt-1 text-xs text-muted-foreground">{explain(option)}</p>
      </div>
      <dl className="flex shrink-0 gap-4 text-right text-xs tabular-nums">
        <div>
          <dt className="text-[10px] uppercase text-muted-foreground">Discrim.</dt>
          <dd className="font-semibold">{option.discriminationIndex.toFixed(2)}</dd>
        </div>
        <div>
          <dt className="text-[10px] uppercase text-muted-foreground">Dificuld.</dt>
          <dd className="font-semibold">{option.difficultyIndex.toFixed(2)}</dd>
        </div>
      </dl>
    </li>
  );
}

function CompetencyDistribution({
  competencies,
}: {
  competencies: CompetencyCalibrationDto[];
}) {
  if (competencies.length === 0) return null;
  return (
    <div className="rounded-md border border-border bg-card p-4">
      <h4 className="text-sm font-semibold">Distribuição por competência</h4>
      <ul className="mt-3 space-y-3">
        {competencies.map((competency) => {
          const width = Math.max(0, Math.min(100, competency.averageScore));
          return (
            <li key={competency.competencyName} className="text-xs">
              <div className="flex items-center justify-between gap-2">
                <span className="font-medium text-foreground">{competency.competencyName}</span>
                <span className="tabular-nums text-muted-foreground">
                  média {competency.averageScore.toFixed(1)} · desvio{" "}
                  {competency.stdDeviation.toFixed(1)}
                </span>
              </div>
              <div className="mt-1 h-2 w-full overflow-hidden rounded-full bg-muted">
                <div className="h-full rounded-full bg-primary" style={{ width: `${width}%` }} />
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
