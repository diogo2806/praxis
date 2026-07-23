import { useQuery } from "@tanstack/react-query";
import { ShieldCheck } from "lucide-react";

import {
  getIntegrityReviewSharedStatus,
  type IntegrityReviewDecision,
} from "@/lib/api/integrity-reviews";

export function IntegritySharedStatus({ attemptId }: { attemptId: string }) {
  const statusQuery = useQuery({
    queryKey: ["integrity-shared-status", attemptId],
    queryFn: () => getIntegrityReviewSharedStatus(attemptId),
    retry: false,
  });

  if (!statusQuery.data) return null;

  return (
    <section className="rounded-md border border-primary/25 bg-primary/5 p-4">
      <div className="flex items-start gap-3">
        <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
        <div>
          <h2 className="text-lg font-semibold">Status técnico compartilhado</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            {decisionLabel(statusQuery.data.decision)} · {formatDate(statusQuery.data.decidedAt)}
          </p>
          <p className="mt-2 text-xs leading-5 text-muted-foreground">
            Este status foi aprovado por revisão humana. Ele não altera pontuação, competências ou a
            decisão de contratação. Evidências e justificativas permanecem restritas à operação.
          </p>
        </div>
      </div>
    </section>
  );
}

function decisionLabel(decision: IntegrityReviewDecision) {
  const labels: Record<IntegrityReviewDecision, string> = {
    NO_IMPACT: "Sem impacto",
    TECHNICAL_ISSUE_CONFIRMED: "Problema técnico confirmado",
    REAPPLICATION_RECOMMENDED: "Reaplicação recomendada",
    PRIVACY_COMPLIANCE_REVIEW: "Análise de privacidade/compliance",
  };
  return labels[decision];
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
