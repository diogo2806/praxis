import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/simulations/new")({
  beforeLoad: () => {
    throw redirect({
      to: "/nova/avaliacao",
      search: { simulationId: undefined, versionNumber: undefined },
    });
  },
});
