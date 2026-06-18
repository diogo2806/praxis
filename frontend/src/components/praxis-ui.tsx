import type { ReactNode } from "react";
import {
  AlertTriangle,
  Archive,
  CheckCircle2,
  CircleDot,
  CloudOff,
  Clock3,
  FileText,
  Info,
  Loader2,
  Lock,
  RotateCcw,
  Route,
  Send,
  ServerCrash,
  UploadCloud,
  XCircle,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { SimulationVersionStatus } from "@/lib/api/praxis";
import { statusMeta, type Maturity } from "@/lib/simulation-meta";
import { gupyConnectionLabels, type GupyConnectionState, useViewMode } from "@/lib/view-mode";

const toneClass = {
  ok: "border-success/25 bg-success/10 text-foreground",
  warn: "border-warning/35 bg-warning/15 text-warning-foreground",
  info: "border-primary/25 bg-primary/10 text-foreground",
  danger: "border-danger/25 bg-danger/10 text-foreground",
  muted: "border-border bg-muted text-foreground",
} as const;

const maturityMeta: Record<Maturity, { label: string; tone: keyof typeof toneClass }> = {
  Rascunho: { label: "Rascunho", tone: "muted" },
  Piloto: { label: "Piloto", tone: "info" },
  Calibrada: { label: "Calibrada", tone: "ok" },
  "Validada internamente": { label: "Validada", tone: "ok" },
  Expirada: { label: "Expirada", tone: "danger" },
  Arquivada: { label: "Arquivada", tone: "muted" },
};

export function StatusBadge({
  status,
  maturity,
}: {
  status: SimulationVersionStatus;
  maturity?: Maturity;
}) {
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
        <div className="grid min-w-0 gap-2 sm:min-w-[420px]">
          <div className="text-[10px] font-semibold uppercase text-muted-foreground">
            Ação recomendada
          </div>
          {actions}
        </div>
      </div>
    </section>
  );
}

type GlobalProductState = {
  gupy: GupyConnectionState;
  draft: "saved" | "dirty" | "published";
  publication: "idle" | "running" | "blocked";
};

const defaultProductState: GlobalProductState = {
  gupy: "connected",
  draft: "saved",
  publication: "idle",
};

export function GlobalProductStateBar({
  state = defaultProductState,
}: {
  state?: Partial<GlobalProductState>;
}) {
  const current = { ...defaultProductState, ...state };
  const gupyMeta = {
    connected: { label: gupyConnectionLabels.connected, Icon: CheckCircle2, tone: "ok" },
    connecting: { label: gupyConnectionLabels.connecting, Icon: Loader2, tone: "warn" },
    disconnected: { label: gupyConnectionLabels.disconnected, Icon: CloudOff, tone: "danger" },
    error: { label: gupyConnectionLabels.error, Icon: ServerCrash, tone: "danger" },
  } as const;
  const draftMeta = {
    saved: {
      label: "Rascunho global salvo",
      Icon: FileText,
      tone: "muted",
      hint: "Todas as alterações foram gravadas no rascunho. Nada está publicado ainda.",
    },
    dirty: {
      label: "Rascunho global alterado",
      Icon: FileText,
      tone: "warn",
      hint: "Há mudanças não publicadas. Elas ficam salvas no rascunho até você publicar uma nova versão.",
    },
    published: {
      label: "Versão publicada imutável",
      Icon: Lock,
      tone: "ok",
      hint: "A versão em produção está congelada. Editar cria uma nova versão sem afetar a publicada.",
    },
  } as const;
  const publicationMeta = {
    idle: {
      label: "Publicação parada",
      Icon: Send,
      tone: "muted",
      hint: "Nenhuma publicação em andamento para esta simulação.",
    },
    running: {
      label: "Publicação em andamento",
      Icon: UploadCloud,
      tone: "info",
      hint: "A versão está sendo publicada. Aguarde a confirmação antes de novas edições.",
    },
    blocked: {
      label: "Publicação bloqueada",
      Icon: Lock,
      tone: "danger",
      hint: "Há item crítico pendente. Resolva os bloqueios no Validador para liberar a publicação.",
    },
  } as const;
  const items = [
    { ...gupyMeta[current.gupy], hint: undefined as string | undefined },
    draftMeta[current.draft],
    publicationMeta[current.publication],
  ];

  return (
    <div className="mb-4 grid gap-2 rounded-md border border-border bg-card p-2 text-xs md:grid-cols-3">
      {items.map(({ label, Icon, tone, hint }) => (
        <div
          key={label}
          title={hint}
          className={cn(
            "flex min-h-10 items-center gap-2 rounded-md border px-3 py-2",
            toneClass[tone],
          )}
        >
          <Icon
            className={cn(
              "h-3.5 w-3.5",
              label === gupyConnectionLabels.connecting && "animate-spin",
            )}
          />
          <span className="font-medium">{label}</span>
          {hint && <Info className="ml-auto h-3 w-3 shrink-0 opacity-60" aria-hidden />}
        </div>
      ))}
    </div>
  );
}

const globalErrorItems = [
  {
    title: "Integração Gupy",
    description: "Sempre aparece como banner de ação, com diagnóstico e tentativa de reenvio.",
    action: "Abrir diagnóstico",
    Icon: ServerCrash,
    tone: "danger",
  },
  {
    title: "Carregamento da simulação",
    description: "Fallback único: skeleton, recarregar e manter rascunho local visível.",
    action: "Recarregar",
    Icon: Loader2,
    tone: "warn",
  },
  {
    title: "Publicação",
    description: "Falha nunca some: permanece no topo ate publicar, salvar rascunho ou corrigir.",
    action: "Salvar rascunho",
    Icon: UploadCloud,
    tone: "info",
  },
] as const;

export function GlobalErrorFlow() {
  return (
    <div className="mb-5 rounded-md border border-border bg-card p-3">
      <div className="mb-2 flex items-center gap-2 text-xs font-semibold uppercase text-muted-foreground">
        <AlertTriangle className="h-3.5 w-3.5" />
        Fluxo de erro global
      </div>
      <div className="grid gap-2 lg:grid-cols-3">
        {globalErrorItems.map(({ title, description, action, Icon, tone }) => (
          <div key={title} className={cn("rounded-md border p-3", toneClass[tone])}>
            <div className="flex items-center gap-2 text-sm font-semibold">
              <Icon className="h-4 w-4" />
              {title}
            </div>
            <p className="mt-1 min-h-10 text-xs opacity-85">{description}</p>
            <button className="mt-2 rounded-md border border-current/20 bg-background/60 px-2 py-1 text-[11px] font-medium">
              {action}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

export function NextStepContract({
  primary,
  secondary,
  versionRule,
  lockedAfter,
}: {
  primary: string;
  secondary: string;
  versionRule: string;
  lockedAfter: string;
}) {
  return (
    <section className="rounded-md border border-border bg-card p-4">
      <div className="flex items-center gap-2 text-sm font-semibold">
        <Route className="h-4 w-4 text-primary" />
        Proximo passo sempre
      </div>
      <div className="mt-3 grid gap-2 text-xs md:grid-cols-2">
        <div className="rounded-md border border-primary/25 bg-primary/10 p-3 text-primary">
          <div className="font-semibold uppercase">acao primaria</div>
          <div className="mt-1 text-sm font-medium">{primary}</div>
        </div>
        <div className="rounded-md border border-border bg-background p-3 text-muted-foreground">
          <div className="font-semibold uppercase">rota secundaria</div>
          <div className="mt-1 text-sm text-foreground">{secondary}</div>
        </div>
        <div className="rounded-md border border-warning/35 bg-warning/15 p-3 text-warning-foreground">
          <div className="font-semibold uppercase">regra de versao</div>
          <div className="mt-1 text-sm font-medium">{versionRule}</div>
        </div>
        <div className="rounded-md border border-border bg-muted p-3 text-muted-foreground">
          <div className="font-semibold uppercase">travamento</div>
          <div className="mt-1 text-sm text-foreground">{lockedAfter}</div>
        </div>
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
  { label: "sucesso", tone: "ok", text: "confirmação não bloqueante" },
  { label: "bloqueado", tone: "warn", text: "motivo + destravar" },
] as const;

export function ScreenStateStrip({
  blockedReason = "campos obrigatorios incompletos",
}: {
  blockedReason?: string;
}) {
  const mode = useViewMode();
  if (mode !== "technical") return null;

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
