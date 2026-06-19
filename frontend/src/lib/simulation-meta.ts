import type { SimulationVersionStatus } from "@/lib/api/praxis";

export type Maturity =
  | "Rascunho"
  | "Pronta para uso"
  | "Arquivada";

export const statusMeta: Record<
  SimulationVersionStatus,
  { label: string; tone: "muted" | "warn" | "info" | "ok" | "danger" }
> = {
  draft: { label: "Rascunho", tone: "muted" },
  published: { label: "No ar", tone: "ok" },
  archived: { label: "Arquivada", tone: "muted" },
};

export const wizardSteps = [
  { slug: "avaliacao", label: "Avaliação", n: 1, path: "/nova/blueprint" },
  { slug: "cenario", label: "Cenário", n: 2, path: "/nova/personagem" },
  { slug: "revisao", label: "Revisão", n: 3, path: "/nova/validador" },
  { slug: "publicacao", label: "Publicação", n: 4, path: "/nova/piloto" },
] as const;

export type WizardSlug = (typeof wizardSteps)[number]["slug"];

export const legacyWizardStepMap = {
  blueprint: "avaliacao",
  objetivo: "avaliacao",
  personagem: "cenario",
  dialogo: "cenario",
  validador: "revisao",
  mapa: "revisao",
  piloto: "publicacao",
  governanca: "publicacao",
  gupy: "publicacao",
} as const satisfies Record<string, WizardSlug>;

export type LegacyWizardSlug = keyof typeof legacyWizardStepMap;

export function maturityForStatus(status: SimulationVersionStatus): Maturity {
  const maturity: Record<SimulationVersionStatus, Maturity> = {
    draft: "Rascunho",
    published: "Pronta para uso",
    archived: "Arquivada",
  };
  return maturity[status];
}

export function canEditSimulationVersion(status: SimulationVersionStatus) {
  return status === "draft";
}
