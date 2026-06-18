import { createFileRoute } from "@tanstack/react-router";
import { CandidateExperience } from "@/routes/candidato";

export const Route = createFileRoute("/candidato/$token")({
  head: () => ({
    meta: [
      { title: "Visão do Candidato — Práxis" },
      {
        name: "description",
        content: "Acesso do candidato à simulação situacional pelo código de acesso do convite.",
      },
    ],
  }),
  component: TokenCandidatePage,
});

function TokenCandidatePage() {
  const { token } = Route.useParams();
  return <CandidateExperience token={token} />;
}
