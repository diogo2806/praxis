import { useEffect } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";

export const Route = createFileRoute("/comecar")({
  head: () => ({
    meta: [
      { title: "Entrar - Práxis" },
      {
        name: "description",
        content: "Acesse o painel da Práxis.",
      },
    ],
  }),
  component: EntryRedirectPage,
});

function EntryRedirectPage() {
  const navigate = useNavigate();

  useEffect(() => {
    void navigate({ to: "/login", replace: true });
  }, [navigate]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 text-center text-sm text-muted-foreground">
      Redirecionando...
    </main>
  );
}
