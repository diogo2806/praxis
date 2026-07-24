import type { ReactNode } from "react";
import { AlertTriangle, Archive, CheckCircle2, CircleDot, FileText } from "lucide-react";
import { cn } from "@/lib/utils";
import type { SimulationVersionStatus } from "@/lib/api/praxis";
import { statusMeta, type Maturity } from "@/lib/simulation-meta";

const toneClass = {
  ok: "border-success/25 bg-success/10 text-foreground",
  success: "border-success/25 bg-success/10 text-foreground",
  warn: "border-warning/35 bg-warning/15 text-warning-foreground",
  warning: "border-warning/35 bg-warning/15 text-warning-foreground",
  info: "border-primary/25 bg-primary/10 text-foreground",
  danger: "border-danger/25 bg-danger/10 text-foreground",
  muted: "border-border bg-muted text-foreground",
} as const;

const maturityMeta: Record<Maturity, { label: string; tone: keyof typeof toneClass }> = {
  Rascunho: { label: "Rascunho", tone: "muted" },
  "Pronta para uso": { label: "Pronta para uso", tone: "ok" },
  Arquivada: { label: "Arquivada", tone: "muted" },
};

export function StatusBadge({
  status,
  maturity,
  variant = "both",
}: {
  status: SimulationVersionStatus;
  maturity?: Maturity;
  variant?: "both" | "status" | "maturity";
}) {
  const statusInfo = statusMeta[status];
  const maturityInfo = maturity ? maturityMeta[maturity] : undefined;
  const showStatus = variant !== "maturity";
  const showMaturity = variant !== "status";
  return (
    <div className="flex flex-wrap gap-1.5">
      {showStatus && (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-[11px] font-medium",
            toneClass[statusInfo.tone],
          )}
        >
          <CircleDot className="h-3 w-3" />
          {statusInfo.label}
        </span>
      )}
      {showMaturity && maturityInfo && (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-[11px] font-medium",
            toneClass[maturityInfo.tone],
          )}
        >
          {maturityInfo.label === "Arquivada" ? (
            <Archive className="h-3 w-3" />
          ) : maturityInfo.label === "Rascunho" ? (
            <FileText className="h-3 w-3" />
          ) : (
            <CheckCircle2 className="h-3 w-3" />
          )}
          {maturityInfo.label}
        </span>
      )}
    </div>
  );
}

export function StateBanner({
  tone,
  title,
  children,
  action,
  live = tone === "danger" ? "assertive" : "polite",
}: {
  tone: keyof typeof toneClass;
  title: string;
  children?: ReactNode;
  action?: ReactNode;
  live?: "off" | "polite" | "assertive";
}) {
  const Icon = tone === "danger" || tone === "warning" || tone === "warn"
    ? AlertTriangle
    : CheckCircle2;
  return (
    <div
      role={tone === "danger" ? "alert" : "status"}
      aria-live={live}
      className={cn("flex items-start gap-3 rounded-md border p-3", toneClass[tone])}
    >
      <Icon className="mt-0.5 h-4 w-4 shrink-0" />
      <div className="min-w-0 flex-1">
        <div className="text-sm font-semibold">{title}</div>
        {children && <div className="mt-0.5 text-xs opacity-85">{children}</div>}
      </div>
      {action}
    </div>
  );
}

export function EmptyState({
  title,
  description,
  actions,
}: {
  title: string;
  description: string;
  actions?: ReactNode;
}) {
  return (
    <section className="rounded-md border border-border bg-card p-6">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
        <div className="max-w-2xl">
          <div className="mb-3 inline-flex h-10 w-10 items-center justify-center rounded-md bg-primary/10 text-primary">
            <FileText className="h-5 w-5" />
          </div>
          <h2 className="text-2xl font-semibold text-foreground">{title}</h2>
          <p className="mt-2 text-sm text-muted-foreground">{description}</p>
        </div>
        {actions && <div className="grid min-w-0 gap-2 sm:min-w-[420px]">{actions}</div>}
      </div>
    </section>
  );
}
