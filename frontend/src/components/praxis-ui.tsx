import type { ReactNode } from "react";
import {
  AlertTriangle,
  Archive,
  CheckCircle2,
  CircleDot,
  Clock3,
  FileText,
  Lock,
  RotateCcw,
  XCircle,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { statusMeta, type Maturity, type SimStatus } from "@/lib/mock";

const toneClass = {
  ok: "border-success/25 bg-success/10 text-success",
  warn: "border-warning/35 bg-warning/15 text-warning-foreground",
  info: "border-primary/25 bg-primary/10 text-primary",
  danger: "border-danger/25 bg-danger/10 text-danger",
  muted: "border-border bg-muted text-muted-foreground",
} as const;

const maturityMeta: Record<Maturity, { label: string; tone: keyof typeof toneClass }> = {
  Rascunho: { label: "Rascunho", tone: "muted" },
  Piloto: { label: "Piloto", tone: "info" },
  Calibrada: { label: "Calibrada", tone: "ok" },
  "Validada internamente": { label: "Validada", tone: "ok" },
  Expirada: { label: "Expirada", tone: "danger" },
  Arquivada: { label: "Arquivada", tone: "muted" },
};

export function StatusBadge({ status, maturity }: { status: SimStatus; maturity?: Maturity }) {
  const statusInfo = statusMeta[status];
  const maturityInfo = maturity ? maturityMeta[maturity] : undefined;
  return (
    <div className="flex flex-wrap gap-1.5">
      <span
        className={cn(
          "inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-[11px] font-medium",
          toneClass[statusInfo.tone],
        )}
      >
        <CircleDot className="h-3 w-3" />
        {statusInfo.label}
      </span>
      {maturityInfo && (
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-[11px] font-medium",
            toneClass[maturityInfo.tone],
          )}
        >
          {maturityInfo.label === "Piloto" ? (
            <Clock3 className="h-3 w-3" />
          ) : maturityInfo.label === "Arquivada" ? (
            <Archive className="h-3 w-3" />
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
}: {
  tone: keyof typeof toneClass;
  title: string;
  children?: ReactNode;
  action?: ReactNode;
}) {
  const Icon = tone === "danger" ? AlertTriangle : tone === "warn" ? AlertTriangle : CheckCircle2;
  return (
    <div className={cn("flex items-start gap-3 rounded-md border p-3", toneClass[tone])}>
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
  actions: ReactNode;
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
        <div className="grid min-w-0 gap-2 sm:min-w-[420px]">{actions}</div>
      </div>
    </section>
  );
}

export function LockedCallout({ children }: { children: ReactNode }) {
  return (
    <div className="flex items-start gap-2 rounded-md border border-border bg-muted/45 p-3 text-xs text-muted-foreground">
      <Lock className="mt-0.5 h-3.5 w-3.5 shrink-0" />
      <div>{children}</div>
    </div>
  );
}

export function SkeletonRows({ rows = 4 }: { rows?: number }) {
  return (
    <div className="space-y-2">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="h-12 animate-pulse rounded-md bg-muted" />
      ))}
    </div>
  );
}

const stateItems = [
  { label: "loading", tone: "muted", text: "skeleton pronto" },
  { label: "vazio", tone: "info", text: "estado vazio com CTA" },
  { label: "erro", tone: "danger", text: "banner + tentar de novo" },
  { label: "sucesso", tone: "ok", text: "confirmacao nao bloqueante" },
  { label: "bloqueado", tone: "warn", text: "motivo + destravar" },
] as const;

export function ScreenStateStrip({
  blockedReason = "campos obrigatorios incompletos",
}: {
  blockedReason?: string;
}) {
  return (
    <div className="mb-5 grid gap-2 rounded-md border border-border bg-card p-3 text-xs md:grid-cols-5">
      {stateItems.map((item) => {
        const Icon =
          item.tone === "danger" ? XCircle : item.tone === "warn" ? AlertTriangle : CheckCircle2;
        return (
          <div
            key={item.label}
            className={cn(
              "rounded-md border p-2",
              item.tone === "muted" && "border-border bg-muted text-muted-foreground",
              item.tone === "info" && toneClass.info,
              item.tone === "danger" && toneClass.danger,
              item.tone === "ok" && toneClass.ok,
              item.tone === "warn" && toneClass.warn,
            )}
          >
            <div className="flex items-center gap-1.5 font-semibold uppercase">
              <Icon className="h-3.5 w-3.5" />
              {item.label}
            </div>
            <div className="mt-1 opacity-80">
              {item.label === "bloqueado" ? blockedReason : item.text}
            </div>
          </div>
        );
      })}
    </div>
  );
}

export function UndoRedoBar({
  savedAt,
  onUndo,
  onRedo,
}: {
  savedAt: string;
  onUndo?: () => void;
  onRedo?: () => void;
}) {
  return (
    <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
      <button
        type="button"
        onClick={onUndo}
        className="inline-flex items-center gap-1 rounded-md border border-border bg-card px-2 py-1 hover:bg-accent"
      >
        <RotateCcw className="h-3.5 w-3.5" />
        Desfazer
      </button>
      <button
        type="button"
        onClick={onRedo}
        className="inline-flex items-center gap-1 rounded-md border border-border bg-card px-2 py-1 hover:bg-accent"
      >
        <RotateCcw className="h-3.5 w-3.5 scale-x-[-1]" />
        Refazer
      </button>
      <span className="ml-auto">Autosave: salvo as {savedAt}</span>
    </div>
  );
}
