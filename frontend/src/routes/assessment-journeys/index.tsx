import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/assessment-journeys/")({
  beforeLoad: () => {
    throw redirect({ to: "/jornadas" });
  },
});
