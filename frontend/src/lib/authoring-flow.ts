export const canonicalAuthoringRoutes = {
  assessment: "/nova/avaliacao",
  character: "/nova/personagem",
  dialogue: "/nova/dialogo",
  map: "/nova/mapa",
  preview: "/nova/previa",
  review: "/nova/validador",
  governance: "/nova/governanca",
} as const;

export const canonicalAuthoringSequence = [
  canonicalAuthoringRoutes.assessment,
  canonicalAuthoringRoutes.character,
  canonicalAuthoringRoutes.dialogue,
  canonicalAuthoringRoutes.map,
  canonicalAuthoringRoutes.review,
  canonicalAuthoringRoutes.governance,
] as const;

export const scenarioAuthoringRoutes = [
  canonicalAuthoringRoutes.character,
  canonicalAuthoringRoutes.dialogue,
  canonicalAuthoringRoutes.map,
] as const;

export const postPublicationRoutes = {
  pilot: "/nova/piloto",
  gupy: "/nova/gupy",
} as const;

export const legacyAuthoringRedirects = {
  objective: canonicalAuthoringRoutes.assessment,
  simulationsNew: canonicalAuthoringRoutes.assessment,
} as const;

export type CanonicalAuthoringRoute = (typeof canonicalAuthoringSequence)[number];
export type ScenarioAuthoringRoute = (typeof scenarioAuthoringRoutes)[number];

export function isScenarioAuthoringPath(pathname: string): pathname is ScenarioAuthoringRoute {
  return scenarioAuthoringRoutes.some((route) => route === pathname);
}
