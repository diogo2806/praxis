import type { SimulationVersionStatus } from "@/lib/api/praxis";
import { canonicalAuthoringRoutes } from "@/lib/authoring-flow";

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
  { slug: "avaliacao", label: "Teste", n: 1, path: canonicalAuthoringRoutes.assessment },
  { slug: "cenario", label: "Cenário", n: 2, path: canonicalAuthoringRoutes.character },
  { slug: "revisao", label: "Revisão", n: 3, path: canonicalAuthoringRoutes.review },
  { slug: "publicacao", label: "Publicação", n: 4, path: canonicalAuthoringRoutes.governance },
] as const;

export type WizardSlug = (typeof wizardSteps)[number]["slug"];

export const legacyWizardStepMap = {
  avaliacao: "avaliacao",
  blueprint: "avaliacao",
  objetivo: "avaliacao",
  personagem: "cenario",
  validador: "revisao",
  governanca: "publicacao",
} as const satisfies Record<string, WizardSlug>;

export const authoringSubviewStepMap = {
  dialogo: "cenario",
  mapa: "cenario",
} as const satisfies Record<string, WizardSlug>;

export const postPublicationStepMap = {
  piloto: "publicacao",
  gupy: "publicacao",
} as const satisfies Record<string, WizardSlug>;

export type LegacyWizardSlug = keyof typeof legacyWizardStepMap;
export type AuthoringSubviewSlug = keyof typeof authoringSubviewStepMap;
export type PostPublicationSlug = keyof typeof postPublicationStepMap;

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
