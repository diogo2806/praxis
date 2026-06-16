import type { SimulationVersionStatus } from "@/lib/api/praxis";

export type Maturity =
  | "Rascunho"
  | "Em revisão"
  | "Aprovada"
  | "Calibrada"
  | "Arquivada";

export const statusMeta: Record<
  SimulationVersionStatus,
  { label: string; tone: "muted" | "warn" | "info" | "ok" | "danger" }
> = {
  draft: { label: "Rascunho", tone: "muted" },
  inReview: { label: "Em revisão", tone: "warn" },
  approved: { label: "Aprovada", tone: "info" },
  rejected: { label: "Reprovada", tone: "danger" },
  published: { label: "Publicada", tone: "ok" },
  archived: { label: "Arquivada", tone: "muted" },
};

export const wizardSteps = [
  { slug: "blueprint", label: "Blueprint", n: 0 },
  { slug: "objetivo", label: "Objetivo", n: 1 },
  { slug: "personagem", label: "Personagem", n: 2 },
  { slug: "dialogo", label: "Diálogo & Rubricas", n: 3 },
  { slug: "validador", label: "Validador", n: 3.5 },
  { slug: "piloto", label: "Piloto", n: 4 },
  { slug: "mapa", label: "Mapa & Score", n: 5 },
  { slug: "governanca", label: "Governança", n: 7 },
  { slug: "gupy", label: "Gupy", n: 8 },
] as const;

export type WizardSlug = (typeof wizardSteps)[number]["slug"];

export function maturityForStatus(status: SimulationVersionStatus): Maturity {
  const maturity: Record<SimulationVersionStatus, Maturity> = {
    draft: "Rascunho",
    inReview: "Em revisão",
    approved: "Aprovada",
    rejected: "Rascunho",
    published: "Calibrada",
    archived: "Arquivada",
  };
  return maturity[status];
}
