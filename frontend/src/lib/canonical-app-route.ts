const ROUTE_ALIASES: Record<string, string> = {
  "/simulations/new": "/nova/avaliacao",
  "/candidate-links/new": "/enviar-link",
  "/assessment-journeys": "/jornadas",
  "/assessment-journeys/": "/jornadas",
  "/assessment-journeys/new": "/jornadas",
  "/configuracoes/integracoes": "/integrations",
};

/**
 * Normaliza atalhos legados recebidos do backend antes de exibi-los no frontend.
 * Query string e fragmento são preservados.
 */
export function canonicalAppRoute(route: string): string {
  if (!route.startsWith("/")) return route;

  const suffixIndex = firstSuffixIndex(route);
  const pathname = suffixIndex === -1 ? route : route.slice(0, suffixIndex);
  const suffix = suffixIndex === -1 ? "" : route.slice(suffixIndex);

  return `${ROUTE_ALIASES[pathname] ?? pathname}${suffix}`;
}

function firstSuffixIndex(route: string): number {
  const queryIndex = route.indexOf("?");
  const hashIndex = route.indexOf("#");

  if (queryIndex === -1) return hashIndex;
  if (hashIndex === -1) return queryIndex;
  return Math.min(queryIndex, hashIndex);
}
