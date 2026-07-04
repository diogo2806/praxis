import { useMemo, useState, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";

import { CandidateExperience } from "@/routes/candidato";
import { getCandidateAttempt, type CandidateAttemptResponse } from "@/lib/api/praxis";

export const Route = createFileRoute("/candidato/$token")({
  head: () => ({
    meta: [
      { title: "Visão do Candidato — Práxis" },
      {
        name: "description",
        content: "Acesso da pessoa participante à avaliação situacional pelo código de acesso do convite.",
      },
    ],
  }),
  component: TokenCandidatePage,
});

function TokenCandidatePage() {
  const { token } = Route.useParams();
  const [readyToStart, setReadyToStart] = useState(false);

  const attemptQuery = useQuery({
    queryKey: ["candidate-attempt", token],
    queryFn: () => getCandidateAttempt(token),
    enabled: readyToStart,
  });

  const terminalStatus = useMemo(() => {
    const attempt = attemptQuery.data;
    if (!attempt || attempt.etapaAtual) return null;
    return getTerminalStatusCopy(attempt);
  }, [attemptQuery.data]);

  if (!readyToStart) {
    return <CandidateStartGate onStart={() => setReadyToStart(true)} />;
  }

  if (attemptQuery.isLoading) {
    return (
      <CandidateShell>
        <div className="rounded-2xl border border-border bg-card p-8 text-center shadow-sm">
          <p className="text-sm font-medium uppercase tracking-[0.18em] text-muted-foreground">
            Preparando
          </p>
          <h1 className="mt-3 text-2xl font-semibold text-foreground">Abrindo sua avaliação.</h1>
          <p className="mt-2 text-sm text-muted-foreground">Aguarde só um instante.</p>
        </div>
      </CandidateShell>
    );
  }

  if (terminalStatus) {
    return <CandidateTerminalStatus status={terminalStatus} />;
  }

  return <CandidateExperience token={token} />;
}

function CandidateStartGate({ onStart }: { onStart: () => void }) {
  const [accepted, setAccepted] = useState(false);

  return (
    <CandidateShell>
      <section className="rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-8">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
          Antes de começar
        </p>
        <h1 className="mt-3 text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">
          Leia as orientações antes de iniciar sua avaliação.
        </h1>
        <p className="mt-3 text-sm leading-6 text-muted-foreground">
          A avaliação apresenta cenários e alternativas de resposta. Depois que você iniciar, leia
          cada situação com atenção, escolha a alternativa que melhor representa sua decisão e
          confirme a resposta final.
        </p>

        <div className="mt-5 rounded-xl border border-border bg-background/60 p-4 text-sm text-muted-foreground">
          <p className="font-medium text-foreground">Instruções importantes</p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li>Algumas etapas podem ter tempo limite. O contador começa após iniciar.</li>
            <li>Você pode trocar a alternativa antes de confirmar, mas a resposta confirmada é final.</li>
            <li>A pontuação segue critérios definidos previamente pela empresa responsável.</li>
            <li>A avaliação apoia a decisão humana; ela não decide sozinha sobre você.</li>
          </ul>
        </div>

        <label className="mt-5 flex items-start gap-3 rounded-xl border border-border bg-background/60 p-4 text-sm leading-6 text-foreground">
          <input
            type="checkbox"
            className="mt-1"
            checked={accepted}
            onChange={(event) => setAccepted(event.target.checked)}
          />
          <span>
            Li as orientações e concordo que a empresa responsável trate meus dados e respostas para
            realizar esta avaliação, gerar registros do percurso, calcular resultados conforme os
            critérios definidos e cumprir suas obrigações legais, conforme a LGPD e a política de
            privacidade aplicável.
          </span>
        </label>

        <button
          type="button"
          disabled={!accepted}
          onClick={onStart}
          className="mt-6 inline-flex w-full items-center justify-center rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
        >
          Iniciar avaliação
        </button>
      </section>
    </CandidateShell>
  );
}

type TerminalStatusCopy = {
  label: string;
  title: string;
  description: string;
  tone: "success" | "warning" | "muted";
};

function getTerminalStatusCopy(attempt: CandidateAttemptResponse): TerminalStatusCopy {
  if (attempt.status === "concluida") {
    return {
      label: "Participação concluída",
      title: "Obrigado por concluir a avaliação.",
      description: "Suas respostas foram registradas e o resultado será processado pela equipe responsável.",
      tone: "success",
    };
  }

  if (attempt.status === "expirada") {
    return {
      label: "Participação expirada",
      title: "O prazo desta avaliação terminou.",
      description:
        "Não registramos uma conclusão completa dentro do prazo disponível. Entre em contato com a empresa responsável se precisar de um novo acesso ou tiver dúvidas.",
      tone: "warning",
    };
  }

  if (attempt.status === "abandonada") {
    return {
      label: "Participação abandonada",
      title: "Esta participação foi encerrada antes da conclusão.",
      description:
        "A avaliação não consta como concluída. Entre em contato com a empresa responsável se precisar retomar ou receber novo convite.",
      tone: "warning",
    };
  }

  return {
    label: "Participação encerrada",
    title: "Esta participação não possui próximas etapas.",
    description: "Entre em contato com a empresa responsável se tiver dúvidas sobre o status da avaliação.",
    tone: "muted",
  };
}

function CandidateTerminalStatus({ status }: { status: TerminalStatusCopy }) {
  const labelClass =
    status.tone === "success"
      ? "text-emerald-600"
      : status.tone === "warning"
        ? "text-amber-600"
        : "text-muted-foreground";

  return (
    <CandidateShell>
      <section className="rounded-2xl border border-border bg-card p-8 text-center shadow-sm">
        <p className={`text-xs font-semibold uppercase tracking-[0.18em] ${labelClass}`}>
          {status.label}
        </p>
        <h1 className="mt-3 text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">
          {status.title}
        </h1>
        <p className="mx-auto mt-3 max-w-lg text-sm leading-6 text-muted-foreground">
          {status.description}
        </p>
        <div className="mx-auto mt-6 max-w-lg rounded-xl border border-border bg-background/60 p-4 text-xs leading-5 text-muted-foreground">
          Seus dados são tratados conforme a LGPD e a política de privacidade da empresa responsável.
          A análise final permanece sob responsabilidade humana.
        </div>
      </section>
    </CandidateShell>
  );
}

function CandidateShell({ children }: { children: ReactNode }) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10 text-foreground">
      <div className="w-full max-w-2xl">{children}</div>
    </main>
  );
}
