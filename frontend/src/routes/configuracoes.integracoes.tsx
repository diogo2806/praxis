import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/configuracoes/integracoes")({
  beforeLoad: () => {
    throw redirect({ to: "/integrations" });
  },
});
