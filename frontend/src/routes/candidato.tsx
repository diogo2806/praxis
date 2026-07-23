import { createFileRoute, Link, Outlet, useChildMatches } from "@tanstack/react-router";
import { useState } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState } from "@/components/praxis-ui";
import { CandidateExperience } from "@/features/candidate/candidate-experience";
import { useLanguage } from "@/lib/language-context";

export const Route = createFileRoute("/candidato")({
  head: () => ({
    meta: [
      { title: "Avaliação do participante - Práxis" },
      {
        name: "description",
        content: "Experiência mobile-first com timer, respostas claras, retomada e acessibilidade.",
      },
    ],
  }),
  component: CandidateRouteLayout,
});

function CandidateRouteLayout() {
  const childMatches = useChildMatches();
  if (childMatches.length > 0) {
    return <Outlet />;
  }
  return <CandidateEntryPage />;
}

function CandidateEntryPage() {
  const { t } = useLanguage();
  const copy = t.candidateExecution.entry;
  const [token, setToken] = useState("");
  const normalizedToken = token.trim();

  return (
    <AppShell>
      <EmptyState
        title={copy.title}
        description={copy.description}
        actions={
          <div className="grid gap-2 sm:grid-cols-[minmax(0,1fr)_auto_auto]">
            <input
              className="input"
              placeholder={copy.placeholder}
              value={token}
              onChange={(event) => setToken(event.target.value)}
            />
            <Link
              to="/candidato/$token"
              params={{ token: normalizedToken || "_" }}
              className={`rounded-md bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90 ${
                !normalizedToken ? "pointer-events-none opacity-50" : ""
              }`}
            >
              {copy.openAssessment}
            </Link>
            <Link
              to="/"
              className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
            >
              {copy.backToDashboard}
            </Link>
          </div>
        }
      />
    </AppShell>
  );
}

export { CandidateExperience };
