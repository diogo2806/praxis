import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/nova/competencias")({
  beforeLoad: () => {
    throw redirect({ to: "/competencias" });
  },
});
