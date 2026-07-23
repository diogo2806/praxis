import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { CheckCircle2, Download, FileCheck2, Send, ShieldCheck, Users } from "lucide-react";
import { useMemo, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  approveAnswerKeyReview,
  createAnswerKeyRound,
  downloadAnswerKeyTechnicalReport,
  getLatestAnswerKeyReview,
  inviteAnswerKeyReviewer,
  saveAnswerKeyEvidence,
  saveAnswerKeyOptionReview,
  submitAnswerKeyReview,
  type AnswerKeyAssignmentRole,
  type AnswerKeyReviewSummary,
} from "@/lib/api/answer-key-review";
import { getSimulationVersion } from "@/lib/api/praxis";

export const Route = createFileRoute("/nova/gabarito")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Gabarito por especialistas - Práxis" },
      {
        name: "description",
        content: "Revisão científica, consenso e aprovação auditável do gabarito da avaliação.",
      },
    ],
  }),
  component: AnswerKeyReviewPage,
});

function AnswerKeyReviewPage() {
  const search = Route.useSearch();
  const queryClient = useQueryClient();
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const key = ["answer-key-review", search.simulationId, search.versionNumber];

  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
    retry: false,
  });
  const reviewQuery = useQuery({
    queryKey: key,
    queryFn: () => getLatestAnswerKeyReview(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
    retry: false,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: key });

  const createMutation = useMutation({
    mutationFn: (input: { minimumExperts: number; minimumConsensus: number }) =>
      createAnswerKeyRound(search.simulationId!, search.versionNumber!, input),
    onSuccess: refresh,
  });
  const inviteMutation = useMutation({
    mutationFn: (input: { userId: string; role: AnswerKeyAssignmentRole }) =>
      inviteAnswerKeyReviewer(
        search.simulationId!,
        search.versionNumber!,
        reviewQuery.data!.round.id,
        input.userId,
        input.role,
      ),
    onSuccess: refresh,
  });
  const evidenceMutation = useMutation({
    mutationFn: (input: {
      nodeId: string;
      task: string;
      risk: string;
      competency: string;
      indicator: string;
      weight: number;
    }) =>
      saveAnswerKeyEvidence(
        search.simulationId!,
        search.versionNumber!,
        reviewQuery.data!.round.id,
        input.nodeId,
        input,
      ),
    onSuccess: refresh,
  });
  const optionMutation = useMutation({
    mutationFn: (input: {
      nodeId: string;
      optionId: string;
      effectivenessScore: number;
      behavioralJustification: string;
      competencyScores: Record<string, number>;
    }) =>
      saveAnswerKeyOptionReview(
        search.simulationId!,
        search.versionNumber!,
        reviewQuery.data!.round.id,
        input.nodeId,
        input.optionId,
        input,
      ),
    onSuccess: refresh,
  });
  const submitMutation = useMutation({
    mutationFn: () =>
      submitAnswerKeyReview(
        search.simulationId!,
        search.versionNumber!,
        reviewQuery.data!.round.id,
      ),
    onSuccess: refresh,
  });
  const approveMutation = useMutation({
    mutationFn: () =>
      approveAnswerKeyReview(
        search.simulationId!,
        search.versionNumber!,
        reviewQuery.data!.round.id,
      ),
    onSuccess: refresh,
  });

  const mutationError =
    createMutation.error ??
    inviteMutation.error ??
    evidenceMutation.error ??
    optionMutation.error ??
    submitMutation.error ??
    approveMutation.error;

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Validação científica
            </div>
            <h1 className="mt-1 font-display text-3xl">Gabarito por especialistas</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Vincule incidentes críticos a tarefas, riscos e competências; colete avaliações
              independentes; meça consenso e aprove o gabarito antes da publicação.
            </p>
          </div>
          {hasContext && (
            <div className="flex flex-wrap gap-2">
              <Button asChild variant="outline">
                <Link
                  to="/nova/validador"
                  search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
                >
                  Validador estrutural
                </Link>
              </Button>
              <Button asChild variant="outline">
                <Link
                  to="/nova/governanca"
                  search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
                >
                  Governança
                </Link>
              </Button>
            </div>
          )}
        </header>

        {!hasContext ? (
          <StateBanner tone="warning" title="Abra uma versão em rascunho">
            Acesse Avaliações, escolha uma versão e abra o Gabarito por especialistas com o contexto da
            avaliação. <Link to="/avaliacoes" className="font-semibold underline">Ir para Avaliações</Link>.
          </StateBanner>
        ) : versionQuery.isLoading ? (
          <LoadingCard />
        ) : versionQuery.error ? (
          <ErrorBanner error={versionQuery.error} />
        ) : (
          <>
            <section className="rounded-xl border border-border bg-card p-5">
              <h2 className="text-lg font-semibold">
                {versionQuery.data?.name} · versão {search.versionNumber}
              </h2>
              <p className="mt-1 text-sm text-muted-foreground">
                Situação: {versionQuery.data?.status}. Alterações em versão publicada devem começar por
                uma nova versão em rascunho.
              </p>
            </section>

            {mutationError && <ErrorBanner error={mutationError} />}

            {reviewQuery.isLoading ? (
              <LoadingCard />
            ) : reviewQuery.data ? (
              <ReviewWorkspace
                summary={reviewQuery.data}
                version={versionQuery.data!}
                invite={(userId, role) => inviteMutation.mutate({ userId, role })}
                saveEvidence={(input) => evidenceMutation.mutate(input)}
                saveOption={(input) => optionMutation.mutate(input)}
                submit={() => submitMutation.mutate()}
                approve={() => approveMutation.mutate()}
                download={() =>
                  void downloadAnswerKeyTechnicalReport(
                    search.simulationId!,
                    search.versionNumber!,
                    reviewQuery.data!.round.id,
                  )
                }
                busy={
                  inviteMutation.isPending ||
                  evidenceMutation.isPending ||
                  optionMutation.isPending ||
                  submitMutation.isPending ||
                  approveMutation.isPending
                }
              />
            ) : (
              <CreateRoundCard
                create={(minimumExperts, minimumConsensus) =>
                  createMutation.mutate({ minimumExperts, minimumConsensus })
                }
                busy={createMutation.isPending}
              />
            )}
          </>
        )}
      </main>
    </AppShell>
  );
}

function CreateRoundCard({
  create,
  busy,
}: {
  create: (minimumExperts: number, minimumConsensus: number) => void;
  busy: boolean;
}) {
  const [minimumExperts, setMinimumExperts] = useState(2);
  const [minimumConsensus, setMinimumConsensus] = useState(70);
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-start gap-3">
        <FileCheck2 className="mt-0.5 h-5 w-5 text-primary" />
        <div className="flex-1">
          <h2 className="font-semibold">Criar rodada de revisão</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            A rodada congela os critérios mínimos de especialistas e consenso para esta versão.
          </p>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <Field label="Quantidade mínima de especialistas">
              <input
                className="w-full rounded-md border border-input bg-background px-3 py-2"
                type="number"
                min={2}
                max={20}
                value={minimumExperts}
                onChange={(event) => setMinimumExperts(Number(event.target.value))}
              />
            </Field>
            <Field label="Consenso mínimo (%)">
              <input
                className="w-full rounded-md border border-input bg-background px-3 py-2"
                type="number"
                min={50}
                max={100}
                value={minimumConsensus}
                onChange={(event) => setMinimumConsensus(Number(event.target.value))}
              />
            </Field>
          </div>
          <Button
            className="mt-4"
            onClick={() => create(minimumExperts, minimumConsensus / 100)}
            disabled={busy}
          >
            Criar rodada
          </Button>
        </div>
      </div>
    </section>
  );
}

function ReviewWorkspace({
  summary,
  version,
  invite,
  saveEvidence,
  saveOption,
  submit,
  approve,
  download,
  busy,
}: {
  summary: AnswerKeyReviewSummary;
  version: Awaited<ReturnType<typeof getSimulationVersion>>;
  invite: (userId: string, role: AnswerKeyAssignmentRole) => void;
  saveEvidence: (input: {
    nodeId: string;
    task: string;
    risk: string;
    competency: string;
    indicator: string;
    weight: number;
  }) => void;
  saveOption: (input: {
    nodeId: string;
    optionId: string;
    effectivenessScore: number;
    behavioralJustification: string;
    competencyScores: Record<string, number>;
  }) => void;
  submit: () => void;
  approve: () => void;
  download: () => void;
  busy: boolean;
}) {
  const editable = summary.round.status !== "APPROVED";
  return (
    <>
      <SummaryCards summary={summary} />
      {summary.blockers.length > 0 && (
        <StateBanner tone="danger" title="Bloqueios do gabarito">
          <ul className="list-disc space-y-1 pl-5">
            {summary.blockers.map((blocker) => <li key={blocker}>{blocker}</li>)}
          </ul>
        </StateBanner>
      )}
      {summary.warnings.length > 0 && (
        <StateBanner tone="warning" title="Alternativas ambíguas ou com dispersão">
          <ul className="list-disc space-y-1 pl-5">
            {summary.warnings.map((warning) => <li key={warning}>{warning}</li>)}
          </ul>
        </StateBanner>
      )}
      {editable && (
        <div className="grid gap-6 xl:grid-cols-2">
          <InviteCard invite={invite} busy={busy} />
          <EvidenceCard version={version} save={saveEvidence} busy={busy} />
          <OptionReviewCard version={version} save={saveOption} busy={busy} />
          <ActionsCard
            summary={summary}
            submit={submit}
            approve={approve}
            download={download}
            busy={busy}
          />
        </div>
      )}
      {!editable && (
        <StateBanner tone="ok" title="Gabarito aprovado e protegido">
          A publicação pode prosseguir enquanto conteúdo, destinos e pontuações permanecerem idênticos
          ao fingerprint aprovado. Qualquer alteração exige nova rodada.
        </StateBanner>
      )}
      <ConsensusTable summary={summary} />
      <HistoryTable summary={summary} />
    </>
  );
}

function SummaryCards({ summary }: { summary: AnswerKeyReviewSummary }) {
  const values = [
    ["Rodada", String(summary.round.roundNumber)],
    ["Situação", summary.round.status],
    ["Especialistas concluídos", `${summary.submittedExperts}/${summary.round.minimumExperts}`],
    ["Alternativas revisadas", `${summary.reviewedOptions}/${summary.expectedOptions}`],
  ];
  return (
    <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      {values.map(([label, value]) => (
        <div key={label} className="rounded-xl border border-border bg-card p-4">
          <div className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{label}</div>
          <div className="mt-2 text-2xl font-semibold">{value}</div>
        </div>
      ))}
    </section>
  );
}

function InviteCard({
  invite,
  busy,
}: {
  invite: (userId: string, role: AnswerKeyAssignmentRole) => void;
  busy: boolean;
}) {
  const [userId, setUserId] = useState("");
  const [role, setRole] = useState<AnswerKeyAssignmentRole>("EXPERT");
  return (
    <Card icon={<Users className="h-5 w-5 text-primary" />} title="Especialistas e aprovadores">
      <Field label="Identificador do usuário">
        <input
          className="w-full rounded-md border border-input bg-background px-3 py-2"
          value={userId}
          onChange={(event) => setUserId(event.target.value)}
          placeholder="ID do usuário convidado"
        />
      </Field>
      <Field label="Papel">
        <select
          className="w-full rounded-md border border-input bg-background px-3 py-2"
          value={role}
          onChange={(event) => setRole(event.target.value as AnswerKeyAssignmentRole)}
        >
          <option value="EXPERT">Especialista do cargo</option>
          <option value="APPROVER">Aprovador</option>
        </select>
      </Field>
      <Button onClick={() => invite(userId.trim(), role)} disabled={busy || !userId.trim()}>
        Convidar
      </Button>
    </Card>
  );
}

function EvidenceCard({
  version,
  save,
  busy,
}: {
  version: Awaited<ReturnType<typeof getSimulationVersion>>;
  save: (input: {
    nodeId: string;
    task: string;
    risk: string;
    competency: string;
    indicator: string;
    weight: number;
  }) => void;
  busy: boolean;
}) {
  const nodes = version.nodes.filter((node) => !node.isFinal);
  const [nodeId, setNodeId] = useState(nodes[0]?.id ?? "");
  const [competency, setCompetency] = useState(version.blueprint.competencies[0]?.name ?? "");
  const [task, setTask] = useState("");
  const [risk, setRisk] = useState("");
  const [indicator, setIndicator] = useState("");
  const [weight, setWeight] = useState(1);
  return (
    <Card icon={<ShieldCheck className="h-5 w-5 text-primary" />} title="Matriz de evidências">
      <Field label="Cenário">
        <select className="w-full rounded-md border border-input bg-background px-3 py-2" value={nodeId} onChange={(event) => setNodeId(event.target.value)}>
          {nodes.map((node) => <option key={node.id} value={node.id}>{node.id} · {node.clientMessage}</option>)}
        </select>
      </Field>
      <Field label="Tarefa representada"><textarea className="w-full rounded-md border border-input bg-background px-3 py-2" value={task} onChange={(event) => setTask(event.target.value)} /></Field>
      <Field label="Risco ou consequência"><textarea className="w-full rounded-md border border-input bg-background px-3 py-2" value={risk} onChange={(event) => setRisk(event.target.value)} /></Field>
      <Field label="Competência">
        <select className="w-full rounded-md border border-input bg-background px-3 py-2" value={competency} onChange={(event) => setCompetency(event.target.value)}>
          {version.blueprint.competencies.map((item) => <option key={item.name}>{item.name}</option>)}
        </select>
      </Field>
      <Field label="Indicador comportamental"><textarea className="w-full rounded-md border border-input bg-background px-3 py-2" value={indicator} onChange={(event) => setIndicator(event.target.value)} /></Field>
      <Field label="Peso"><input className="w-full rounded-md border border-input bg-background px-3 py-2" type="number" min={0.0001} step={0.1} value={weight} onChange={(event) => setWeight(Number(event.target.value))} /></Field>
      <Button onClick={() => save({ nodeId, task, risk, competency, indicator, weight })} disabled={busy || !nodeId || !task.trim() || !risk.trim() || !indicator.trim()}>
        Salvar vínculo
      </Button>
    </Card>
  );
}

function OptionReviewCard({
  version,
  save,
  busy,
}: {
  version: Awaited<ReturnType<typeof getSimulationVersion>>;
  save: (input: {
    nodeId: string;
    optionId: string;
    effectivenessScore: number;
    behavioralJustification: string;
    competencyScores: Record<string, number>;
  }) => void;
  busy: boolean;
}) {
  const options = useMemo(
    () => version.nodes.flatMap((node) => node.options.map((option) => ({ node, option }))),
    [version],
  );
  const [selected, setSelected] = useState(`${options[0]?.node.id ?? ""}::${options[0]?.option.id ?? ""}`);
  const [score, setScore] = useState(50);
  const [justification, setJustification] = useState("");
  const [competencyScores, setCompetencyScores] = useState<Record<string, number>>(() =>
    Object.fromEntries(version.blueprint.competencies.map((item) => [item.name, 50])),
  );
  const [nodeId, optionId] = selected.split("::");
  return (
    <Card icon={<FileCheck2 className="h-5 w-5 text-primary" />} title="Classificação da alternativa">
      <Field label="Alternativa">
        <select className="w-full rounded-md border border-input bg-background px-3 py-2" value={selected} onChange={(event) => setSelected(event.target.value)}>
          {options.map(({ node, option }) => <option key={`${node.id}-${option.id}`} value={`${node.id}::${option.id}`}>{node.id}/{option.id} · {option.text}</option>)}
        </select>
      </Field>
      <Field label="Eficácia geral (0 a 100)"><input className="w-full rounded-md border border-input bg-background px-3 py-2" type="number" min={0} max={100} value={score} onChange={(event) => setScore(Number(event.target.value))} /></Field>
      {version.blueprint.competencies.map((competency) => (
        <Field key={competency.name} label={`Evidência de ${competency.name} (0 a 100)`}>
          <input className="w-full rounded-md border border-input bg-background px-3 py-2" type="number" min={0} max={100} value={competencyScores[competency.name] ?? 0} onChange={(event) => setCompetencyScores((current) => ({ ...current, [competency.name]: Number(event.target.value) }))} />
        </Field>
      ))}
      <Field label="Justificativa comportamental"><textarea className="min-h-28 w-full rounded-md border border-input bg-background px-3 py-2" value={justification} onChange={(event) => setJustification(event.target.value)} /></Field>
      <Button onClick={() => save({ nodeId, optionId, effectivenessScore: score, behavioralJustification: justification, competencyScores })} disabled={busy || !nodeId || !optionId || !justification.trim()}>
        Salvar avaliação
      </Button>
    </Card>
  );
}

function ActionsCard({
  summary,
  submit,
  approve,
  download,
  busy,
}: {
  summary: AnswerKeyReviewSummary;
  submit: () => void;
  approve: () => void;
  download: () => void;
  busy: boolean;
}) {
  return (
    <Card icon={<CheckCircle2 className="h-5 w-5 text-primary" />} title="Conclusão e aprovação">
      <p className="text-sm leading-6 text-muted-foreground">
        Especialistas concluem suas avaliações individualmente. O aprovador só consegue aprovar quando
        todas as etapas possuem evidências e todas as alternativas atingem quantidade e consenso mínimos.
      </p>
      <div className="flex flex-wrap gap-2">
        <Button onClick={submit} disabled={busy}><Send className="mr-2 h-4 w-4" />Concluir minha revisão</Button>
        <Button onClick={approve} disabled={busy || !summary.approvable} variant="outline"><ShieldCheck className="mr-2 h-4 w-4" />Aprovar gabarito</Button>
        <Button onClick={download} variant="outline"><Download className="mr-2 h-4 w-4" />Relatório técnico</Button>
      </div>
    </Card>
  );
}

function ConsensusTable({ summary }: { summary: AnswerKeyReviewSummary }) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <h2 className="font-semibold">Consenso por alternativa</h2>
      <div className="mt-4 overflow-x-auto">
        <table className="w-full min-w-[820px] text-left text-sm">
          <thead className="text-xs uppercase tracking-wide text-muted-foreground"><tr><th className="pb-2">Alternativa</th><th className="pb-2">Avaliações</th><th className="pb-2">Média</th><th className="pb-2">Dispersão</th><th className="pb-2">Consenso</th><th className="pb-2">Situação</th></tr></thead>
          <tbody>
            {summary.options.map((option) => (
              <tr key={`${option.nodeId}-${option.optionId}`} className="border-t border-border">
                <td className="py-3 pr-4"><div className="font-semibold">{option.nodeId}/{option.optionId}</div><div className="max-w-xl text-muted-foreground">{option.optionText}</div></td>
                <td className="py-3">{option.reviewCount}</td><td className="py-3">{option.averageScore.toFixed(1)}</td><td className="py-3">{option.dispersion.toFixed(1)}</td><td className="py-3">{(option.consensus * 100).toFixed(1)}%</td><td className="py-3 font-semibold">{option.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function HistoryTable({ summary }: { summary: AnswerKeyReviewSummary }) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <h2 className="font-semibold">Histórico auditável</h2>
      <div className="mt-4 space-y-3">
        {summary.history.map((event, index) => (
          <div key={`${event.occurredAt}-${index}`} className="rounded-lg border border-border bg-background p-3 text-sm">
            <div className="font-semibold">{event.eventType}</div>
            <div className="mt-1 text-muted-foreground">{new Date(event.occurredAt).toLocaleString("pt-BR")} · {event.actorUserId}</div>
          </div>
        ))}
      </div>
    </section>
  );
}

function Card({ icon, title, children }: { icon: React.ReactNode; title: string; children: React.ReactNode }) {
  return <section className="rounded-xl border border-border bg-card p-5"><div className="flex items-center gap-3">{icon}<h2 className="font-semibold">{title}</h2></div><div className="mt-4 space-y-4">{children}</div></section>;
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return <label className="block space-y-1.5 text-sm"><span className="font-medium">{label}</span>{children}</label>;
}

function LoadingCard() {
  return <section className="rounded-xl border border-border bg-card px-5 py-12 text-center text-sm text-muted-foreground">Carregando revisão do gabarito...</section>;
}

function ErrorBanner({ error }: { error: unknown }) {
  return <StateBanner tone="danger" title="Não foi possível concluir a operação">{error instanceof Error ? error.message : "Tente novamente."}</StateBanner>;
}
