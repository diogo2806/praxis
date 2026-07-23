import { createFileRoute, redirect } from "@tanstack/react-router";

import { legacyAuthoringRedirects } from "@/lib/authoring-flow";

export const Route = createFileRoute("/simulations/new")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number" && Number.isFinite(search.versionNumber)
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  }),
  beforeLoad: ({ search }) => {
    throw redirect({
      to: legacyAuthoringRedirects.simulationsNew,
      search,
    });
  },
});
