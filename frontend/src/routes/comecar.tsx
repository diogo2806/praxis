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
    const flagName = String.fromCharCode(
      86,
      73,
      84,
      69,
      95,
      80,
      82,
      65,
      88,
      73,
      83,
      95,
      83,
      69,
      67,
      85,
      82,
      73,
      84,
      89,
      95,
      69,
      78,
      65,
      66,
      76,
      69,
      68,
    );
    const loginEnabled = import.meta.env[flagName] !== "false";
    void navigate({ to: loginEnabled ? "/login" : "/avaliacoes", replace: true });
  }, [navigate]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 text-center text-sm text-muted-foreground">
      Redirecionando...
    </main>
  );
}
