import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/assessment-journeys/new")({
  beforeLoad: () => {
    throw redirect({ to: "/jornadas" });
  },
});
