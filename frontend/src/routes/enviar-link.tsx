import { createFileRoute } from "@tanstack/react-router";
import { EnviarLinkPage } from "@/features/candidate-links/enviar-link-page";

export const Route = createFileRoute("/enviar-link")({
  head: () => ({
    meta: [
      { title: "Compartilhar avaliação - Práxis" },
      {
        name: "description",
        content: "Crie novas tentativas ou reenvie links existentes com efeitos explícitos.",
      },
    ],
  }),
  component: EnviarLinkPage,
});
