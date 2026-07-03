import { createFileRoute, redirect } from "@tanstack/react-router";

/**
 * Rota legada: a etapa "Avaliação" passou a viver em /nova/avaliacao.
 * Mantida apenas para não quebrar links/bookmarks antigos — redireciona
 * preservando os parâmetros (simulationId, versionNumber).
 */
export const Route = createFileRoute("/nova/blueprint")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  }),
  beforeLoad: ({ search }) => {
    throw redirect({ to: "/nova/avaliacao", search });
  },
});
