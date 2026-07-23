import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { CheckCircle2, Clock3, ExternalLink, ShieldCheck } from "lucide-react";
import type { ReactNode } from "react";

import { LanguageSelector } from "@/components/language-selector";
import {
  getCandidateResultPage,
  type CandidateResultPageResponse,
} from "@/lib/api/candidate-result-page";
import { useLanguage } from "@/lib/language-context";

export const Route = createFileRoute("/candidato/$token/resultado")({
  head: () => ({
    meta: [
      { title: "Resultado da avaliação - Práxis" },
      {
        name: "description",
        content: "Página final segura da avaliação para a pessoa candidata.",
      },
    ],
  }),
  component: CandidateResultPage,
});

function CandidateResultPage() {
  const { token } = Route.useParams();
  const { t } = useLanguage();
  const resultQuery = useQuery({
    queryKey: ["candidate-result-page", token],
    queryFn: () => getCandidateResultPage(token),
    retry: false,
  });

  return (
    <main className="relative flex min-h-screen items-center justify-center bg-background px-4 py-10 text-foreground">
      <div className="absolute right-4 top-4">
        <LanguageSelector />
      </div>
      <div className="w-full max-w-2xl">
        {resultQuery.isLoading ? (
          <ResultCard
            icon={<Clock3 className="h-8 w-8" />}
            label="Carregando"
            title="Consultando o estado da sua avaliação"
            description="Aguarde enquanto confirmamos a conclusão com segurança."
          />
        ) : resultQuery.isError ? (
          <ResultCard
            icon={<ShieldCheck className="h-8 w-8" />}
            label="Link inválido ou expirado"
            title="Não foi possível abrir esta página"
            description={
              resultQuery.error instanceof Error
                ? resultQuery.error.message
                : "Solicite um novo acesso à empresa responsável pelo processo seletivo."
            }
          />
        ) : resultQuery.data ? (
          <CandidateResultContent
            result={resultQuery.data}
            token={token}
            privacyNotice={t.candidateAccess.privacyNotice}
          />
        ) : null}
      </div>
    </main>
  );
}

function CandidateResultContent({
  result,
  token,
  privacyNotice,
}: {
  result: CandidateResultPageResponse;
  token: string;
  privacyNotice: string;
}) {
  const completed = result.status === "concluida";
  const candidateResults = result.resultados ?? [];
  const hasScoringSnapshot =
    completed &&
    result.pontuacaoBruta != null &&
    result.pontuacaoMaximaCaminho != null &&
    result.pontuacaoNormalizada != null;
  const title = completed
    ? "Avaliação concluída"
    : result.finalizado
      ? "Avaliação encerrada"
      : "Avaliação ainda em andamento";
  const description = completed
    ? "Sua participação foi registrada. A decisão sobre a candidatura continua sendo feita por pessoas responsáveis pelo processo seletivo."
    : result.finalizado
      ? "Esta participação foi encerrada. Entre em contato com a empresa responsável caso precise de orientação."
      : "Conclua as etapas pendentes antes de consultar a página final.";

  return (
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-8">
      <div className="flex items-start gap-4">
        <div className="rounded-full bg-primary/10 p-3 text-primary">
          {completed ? <CheckCircle2 className="h-8 w-8" /> : <Clock3 className="h-8 w-8" />}
        </div>
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
            Resultado para a pessoa candidata
          </p>
          <h1 className="mt-2 text-2xl font-semibold tracking-tight sm:text-3xl">{title}</h1>
          <p className="mt-3 text-sm leading-6 text-muted-foreground">{description}</p>
        </div>
      </div>

      <dl className="mt-6 rounded-xl border border-border bg-background/60 p-4">
        <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Avaliação
        </dt>
        <dd className="mt-1 font-medium text-foreground">{result.avaliacaoNome}</dd>
        {result.concluidoEm && (
          <>
            <dt className="mt-4 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              Concluída em
            </dt>
            <dd className="mt-1 text-sm text-foreground">{formatDate(result.concluidoEm)}</dd>
          </>
        )}
      </dl>

      {hasScoringSnapshot && (
        <section className="mt-6 rounded-xl border border-border bg-background/60 p-4" aria-labelledby="score-method-title">
          <h2 id="score-method-title" className="text-base font-semibold text-foreground">
            Como a pontuação foi calculada
          </h2>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">
            A pontuação bruta registra os pontos obtidos no caminho percorrido. A pontuação
            normalizada permite comparar caminhos com oportunidades de pontuação diferentes.
          </p>
          <dl className="mt-4 grid gap-3 sm:grid-cols-2">
            <div className="rounded-lg border border-border bg-card p-3">
              <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Pontuação bruta
              </dt>
              <dd className="mt-1 text-xl font-semibold text-foreground">
                {result.pontuacaoBruta}/{result.pontuacaoMaximaCaminho}
              </dd>
            </div>
            <div className="rounded-lg border border-border bg-card p-3">
              <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Pontuação normalizada
              </dt>
              <dd className="mt-1 text-xl font-semibold text-primary">
                {result.pontuacaoNormalizada}/100
              </dd>
            </div>
          </dl>
          {result.versaoAlgoritmoPontuacao && (
            <p className="mt-3 text-xs text-muted-foreground">
              Versão do cálculo: {result.versaoAlgoritmoPontuacao}
            </p>
          )}
        </section>
      )}

      {completed && candidateResults.length > 0 && (
        <section className="mt-6" aria-labelledby="candidate-results-title">
          <h2 id="candidate-results-title" className="text-base font-semibold text-foreground">
            Seu resultado
          </h2>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">
            As pontuações abaixo mostram as dimensões principais da avaliação e não determinam,
            isoladamente, a decisão do processo seletivo.
          </p>
          <dl className="mt-3 grid gap-3 sm:grid-cols-2">
            {candidateResults.map((item) => (
              <div key={item.titulo} className="rounded-xl border border-border bg-background/60 p-4">
                <dt className="text-sm font-medium text-foreground">{item.titulo}</dt>
                <dd className="mt-2 text-2xl font-semibold text-primary" aria-label={`${item.titulo}: ${item.resultado}`}>
                  {item.resultado}
                </dd>
              </div>
            ))}
          </dl>
        </section>
      )}

      <div className="mt-6 grid gap-3">
        {completed && result.redirectUrl ? (
          <a
            href={result.redirectUrl}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90"
          >
            Voltar ao processo seletivo
            <ExternalLink className="h-4 w-4" />
          </a>
        ) : !result.finalizado ? (
          <a
            href={`/candidato/${encodeURIComponent(token)}`}
            className="inline-flex items-center justify-center rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90"
          >
            Continuar avaliação
          </a>
        ) : null}
      </div>

      <div className="mt-6 rounded-xl border border-border bg-background/60 p-4 text-xs leading-5 text-muted-foreground">
        {privacyNotice}
      </div>
    </section>
  );
}

function ResultCard({
  icon,
  label,
  title,
  description,
}: {
  icon: ReactNode;
  label: string;
  title: string;
  description: string;
}) {
  return (
    <section className="rounded-2xl border border-border bg-card p-8 text-center shadow-sm">
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-primary/10 text-primary">
        {icon}
      </div>
      <p className="mt-5 text-xs font-semibold uppercase tracking-[0.18em] text-primary">{label}</p>
      <h1 className="mt-3 text-2xl font-semibold tracking-tight sm:text-3xl">{title}</h1>
      <p className="mx-auto mt-3 max-w-lg text-sm leading-6 text-muted-foreground">{description}</p>
    </section>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
