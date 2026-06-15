import { createFileRoute } from "@tanstack/react-router";
import { CandidateExperience } from "@/routes/candidato";

export const Route = createFileRoute("/candidato/$token")({
  head: () => ({
    meta: [
      { title: "Visão do Candidato — Práxis" },
      {
        name: "description",
        content: "Acesso do candidato à simulação situacional por token de tentativa.",
      },
    ],
  }),
  component: TokenCandidatePage,
});

function TokenCandidatePage() {
  const { token } = Route.useParams();
  return <CandidateExperience token={token} />;
}
