import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { CheckCircle2, Copy, Send, Workflow } from "lucide-react";
import { useMemo, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  createAssessmentJourneyAttempt,
  getAssessmentJourney,
  listAssessmentJourneys,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/participacoes/jornada")({
  head: () => ({
    meta: [
      { title: "Convite por jornada - Práxis" },
      {
        name: "description",
        content: "Crie uma participação usando uma jornada publicada.",
      },
    ],
  }),
  component: JourneyParticipationPage,
});

function JourneyParticipationPage() {
  const queryClient = useQueryClient();
  const [journeyId, setJourneyId] = useState("");
  const [candidateName, setCandidateName] = useState("");
  const [candidateEmail, setCandidateEmail] = useState("");
  const [sequenceKey, setSequenceKey] = useState("");
  const [generatedAttemptId, setGeneratedAttemptId] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const journeysQuery = useQuery({
    queryKey: ["assessment-journeys"],
    queryFn: listAssessmentJourneys,
    retry: false,
  });

  const journeyQuery = useQuery({
    queryKey: ["assessment-journey", journeyId],
    queryFn: () => getAssessmentJourney(journeyId),
    enabled: Boolean(journeyId),
    retry: false,
  });

  const publishedJourneys = useMemo(
    () => (journeysQuery.data ?? []).filter((journey) => journey.status === "published"),
    [journeysQuery.data],
  );
  const journey = journeyQuery.data ?? null;
  const selectedSequence = sequenceKey || journey?.sequences[0]?.sequenceKey || "principal";
  const canSubmit = Boolean(
    journey && candidateName.trim() && candidateEmail.includes("@") && selectedSequence,
  );

  const createMutation = useMutation({
    mutationFn: () =>
      createAssessmentJourneyAttempt({
        journeyId,
        candidateName: candidateName.trim(),
        candidateEmail: candidateEmail.trim(),
        sequenceKey: selectedSequence,
      }),
    onSuccess: async (attempt) => {
      setGeneratedAttemptId(attempt.id);
      setCopied(false);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["participations"] }),
        queryClient.invalidateQueries({ queryKey: ["assessment-journey-attempts"] }),
      ]);
    },
  });

  const generatedUrl = generatedAttemptId ? journeyAttemptUrl(generatedAttemptId) : "";

  async function copyGeneratedLink() {
    if (!generatedUrl) return;
    await navigator.clipboard.writeText(generatedUrl);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 2000);
  }

  function resetForm() {
    setCandidateName("");
    setCandidateEmail("");
    setSequenceKey("");
    setGeneratedAttemptId(null);
    setCopied(false);
    createMutation.reset();
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-4xl space-y-6">
        <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Participações
            </div>
            <h1 className="mt-1 text-3xl font-semibold">Convite por jornada</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">
              Selecione uma jornada publicada e gere o acesso da pessoa participante. O acompanhamento ficará na Central de Participações.
            </p>
          </div>
          <Button asChild variant="outline">
            <Link to="/participacoes">Voltar para participações</Link>
          </Button>
        </header>

        {journeysQuery.isError && (
          <StateBanner tone="danger" title="Não foi possível carregar as jornadas">
            {journeysQuery.error instanceof Error ? journeysQuery.error.message : "Tente novamente."}
          </StateBanner>
        )}
        {createMutation.isError && (
          <StateBanner tone="danger" title="Não foi possível criar a participação">
            {createMutation.error instanceof Error ? createMutation.error.message : "Confira os dados e tente novamente."}
          </StateBanner>
        )}

        {!journeysQuery.isLoading && publishedJourneys.length === 0 ? (
          <EmptyState
            title="Nenhuma jornada publicada"
            description="Monte e publique uma jornada antes de criar o convite."
            actions={<Button asChild><Link to="/jornadas">Abrir jornadas</Link></Button>}
          />
        ) : (
          <section className="rounded-xl border border-border bg-card p-6">
            <div className="flex items-center gap-2">
              <Workflow className="h-5 w-5 text-primary" />
              <h2 className="text-lg font-semibold">Dados da participação</h2>
            </div>

            <div className="mt-5 grid gap-4 md:grid-cols-2">
              <label className="space-y-1.5 text-sm font-medium md:col-span-2">
                Jornada publicada
                <select
                  className="input w-full"
                  value={journeyId}
                  disabled={journeysQuery.isLoading || createMutation.isPending}
                  onChange={(event) => {
                    setJourneyId(event.target.value);
                    setSequenceKey("");
                    setGeneratedAttemptId(null);
                  }}
                >
                  <option value="">Selecione uma jornada</option>
                  {publishedJourneys.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.name} · {item.stepCount} avaliação(ões)
                    </option>
                  ))}
                </select>
              </label>

              <label className="space-y-1.5 text-sm font-medium">
                Nome da pessoa participante
                <input
                  className="input w-full"
                  value={candidateName}
                  disabled={!journey || createMutation.isPending}
                  onChange={(event) => setCandidateName(event.target.value)}
                  placeholder="Nome completo"
                />
              </label>

              <label className="space-y-1.5 text-sm font-medium">
                E-mail
                <input
                  className="input w-full"
                  type="email"
                  value={candidateEmail}
                  disabled={!journey || createMutation.isPending}
                  onChange={(event) => setCandidateEmail(event.target.value)}
                  placeholder="pessoa@empresa.com"
                />
              </label>

              {journey && journey.sequences.length > 1 && (
                <label className="space-y-1.5 text-sm font-medium md:col-span-2">
                  Sequência da jornada
                  <select
                    className="input w-full"
                    value={sequenceKey}
                    disabled={createMutation.isPending}
                    onChange={(event) => setSequenceKey(event.target.value)}
                  >
                    {journey.sequences.map((sequence) => (
                      <option key={sequence.sequenceKey} value={sequence.sequenceKey}>
                        {sequence.sequenceKey} · {sequence.steps.length} etapa(s)
                      </option>
                    ))}
                  </select>
                </label>
              )}
            </div>

            <div className="mt-6 flex flex-wrap gap-3">
              <Button
                type="button"
                className="gap-2"
                disabled={!canSubmit || createMutation.isPending}
                onClick={() => createMutation.mutate()}
              >
                <Send className="h-4 w-4" />
                {createMutation.isPending ? "Criando..." : "Criar participação"}
              </Button>
              <Button type="button" variant="outline" disabled={createMutation.isPending} onClick={resetForm}>
                Limpar
              </Button>
            </div>
          </section>
        )}

        {generatedAttemptId && (
          <section className="rounded-xl border border-success/30 bg-success/10 p-6">
            <div className="flex items-start gap-3">
              <CheckCircle2 className="mt-0.5 h-5 w-5 text-success" />
              <div className="min-w-0 flex-1">
                <h2 className="font-semibold">Participação criada</h2>
                <p className="mt-1 text-sm text-muted-foreground">
                  Compartilhe o link abaixo. Validade, reenvio e acompanhamento ficam em Participações.
                </p>
                <code className="mt-4 block overflow-x-auto rounded-md border border-border bg-background p-3 text-xs">
                  {generatedUrl}
                </code>
                <div className="mt-4 flex flex-wrap gap-2">
                  <Button type="button" variant="outline" className="gap-2" onClick={() => void copyGeneratedLink()}>
                    {copied ? <CheckCircle2 className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                    {copied ? "Copiado" : "Copiar link"}
                  </Button>
                  <Button asChild>
                    <Link to="/participacoes">Acompanhar participação</Link>
                  </Button>
                </div>
              </div>
            </div>
          </section>
        )}
      </main>
    </AppShell>
  );
}

function journeyAttemptUrl(attemptId: string) {
  if (typeof window === "undefined") return `/jornada/${attemptId}`;
  return `${window.location.origin}/jornada/${attemptId}`;
}
