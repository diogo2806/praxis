import { AlertTriangle, XCircle } from "lucide-react";
import type { ValidationIssueResponse } from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

/**
 * Badge reutilizável que resume os problemas de validação de um nó.
 * 🔴 quando há ao menos um bloqueio (erro); 🟡 quando há apenas avisos.
 * O tooltip lista as mensagens — as mesmas do validador final (etapa 5).
 */
export function ValidationBadge({
  issues,
  className,
}: {
  issues: ValidationIssueResponse[];
  className?: string;
}) {
  if (!issues || issues.length === 0) return null;
  const hasError = issues.some((issue) => issue.severity === "blocker");
  const Icon = hasError ? XCircle : AlertTriangle;
  const title = issues.map((issue) => issue.message).join("\n");

  return (
    <span
      title={title}
      aria-label={title}
      className={cn(
        "inline-flex items-center gap-1 rounded-md border px-1.5 py-0.5 text-[10px] font-medium",
        hasError
          ? "border-danger/40 bg-danger/10 text-danger"
          : "border-warning/40 bg-warning/15 text-warning-foreground",
        className,
      )}
    >
      <Icon className="h-3 w-3" />
      {hasError ? "🔴" : "🟡"} {issues.length}
    </span>
  );
}

/**
 * Resumo agregado no cabeçalho ("2 avisos 🟡 1 erro 🔴").
 */
export function ValidationSummary({
  blockerCount,
  warningCount,
  className,
}: {
  blockerCount: number;
  warningCount: number;
  className?: string;
}) {
  if (blockerCount === 0 && warningCount === 0) {
    return (
      <span className={cn("text-xs text-success", className)}>Sem problemas detectados ✓</span>
    );
  }
  return (
    <span className={cn("flex items-center gap-3 text-xs", className)}>
      {warningCount > 0 && (
        <span className="text-warning-foreground">
          {warningCount} aviso{warningCount === 1 ? "" : "s"} 🟡
        </span>
      )}
      {blockerCount > 0 && (
        <span className="text-danger">
          {blockerCount} erro{blockerCount === 1 ? "" : "s"} 🔴
        </span>
      )}
    </span>
  );
}

/** Agrupa as issues por nodeId para consulta rápida por nó. */
export function groupIssuesByNode(
  issues: ValidationIssueResponse[] | undefined,
): Map<string, ValidationIssueResponse[]> {
  const map = new Map<string, ValidationIssueResponse[]>();
  for (const issue of issues ?? []) {
    if (!issue.nodeId) continue;
    const list = map.get(issue.nodeId) ?? [];
    list.push(issue);
    map.set(issue.nodeId, list);
  }
  return map;
}
