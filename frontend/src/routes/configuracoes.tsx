import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/configuracoes")({
  // Esta é a rota layout pai de /configuracoes/perfil, /conta e /integracoes,
  // então o beforeLoad roda para todas as filhas. Redirecionar sem checar o
  // caminho criava um loop infinito (perfil -> pai -> redirect -> perfil...)
  // que travava a aba. Só redirecionamos o índice exato /configuracoes.
  beforeLoad: ({ location }) => {
    if (location.pathname === "/configuracoes" || location.pathname === "/configuracoes/") {
      throw redirect({ to: "/configuracoes/perfil" });
    }
  },
});
