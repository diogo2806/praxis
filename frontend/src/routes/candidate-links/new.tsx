import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/candidate-links/new")({
  beforeLoad: () => {
    throw redirect({ to: "/enviar-link" });
  },
});
