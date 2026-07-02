import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/nova/avaliacoes")({
  beforeLoad: () => {
    throw redirect({ to: "/avaliacoes" });
  },
});
