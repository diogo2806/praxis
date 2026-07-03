import { createFileRoute, redirect } from "@tanstack/react-router";

// A configuração de API e webhooks foi consolidada na Central de Integrações,
// dentro do detalhe da integração "API própria". Esta rota agora só redireciona
// para lá, evitando duas telas de integrações separadas.
export const Route = createFileRoute("/configuracoes/api")({
  beforeLoad: () => {
    throw redirect({ to: "/integrations/$provider", params: { provider: "custom-api" } });
  },
});
