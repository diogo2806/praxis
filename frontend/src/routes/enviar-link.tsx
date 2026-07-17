import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { z } from "zod";
import {
  CalendarPlus,
  CheckCircle2,
  Copy,
  Link2,
  Mail,
  MessageCircle,
  RefreshCw,
  Send,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, SkeletonRows, StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  createCandidateLink,
  extendCandidateLink,
  listCandidateLinks,
  resendCandidateLink,
  type CandidateLinkOperation,
  type CandidateLinkResponse,
} from "@/lib/api/candidate-links";
import {
  listSimulations,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";
import { useLanguage } from "@/lib/language-context";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/enviar-link")({
  head: () => ({
    meta: [
      { title: "Compartilhar avaliação - Práxis" },
      {
        name: "description",
        content: "Crie uma nova aplicação ou reenvie um link de avaliação existente.",
      },
    ],
  }),
  component: EnviarLinkPage,
});

type Step = "select" | "form" | "share";

type CandidateFormErrors = {
  candidateName?: string;
  candidateEmail?: string;
};

const candidateLinkSchema = z.object({
  candidateName: z.string().trim().min(1, "Informe o nome do candidato."),
  candidateEmail: z
    .string()
    .trim()
    .min(1, "Informe o e-mail do candidato.")
    .email("Informe um e-mail válido."),
});

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [10, 25, 50];
const EXTENSION_DAY_OPTIONS = [1, 3, 7, 15, 30, 60, 90];

function EnviarLinkPage() {
  const { t } = useLanguage();
  const queryClient = useQueryClient();
  const [step, setStep] = useState<Step>("select");
  const [selectedSimulation, setSelectedSimulation] = useState<SimulationSummaryResponse | null>(
    null,
  );
  const [candidateName, setCandidateName] = useState("");
  const [candidateEmail, setCandidateEmail] = useState("");
  const [applicationCycleId, setApplicationCycleId] = useState("");
  const [formErrors, setFormErrors] = useState<CandidateFormErrors>({});
  const [generatedLink, setGeneratedLink] = useState("");
  const [simulationName, setSimulationName] = useState("");
  const [generatedOperation, setGeneratedOperation] = useState<CandidateLinkOperation | null>(null);
  const [copied, setCopied] = useState(false);

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });
  const candidateLinksQuery = useQuery({
    queryKey: ["candidate-links", { blind: false }],
    queryFn: () => listCandidateLinks(false),
    retry: false,
  });

  const linkMutation = useMutation({
    mutationFn: createCandidateLink,
    onSuccess: (data) => {
      setGeneratedLink(toParticipantPageUrl(data.candidateUrl));
      setSimulationName(data.simulationName);
      setGeneratedOperation(data.operation);
      setStep("share");
      void queryClient.invalidateQueries({ queryKey: ["candidate-links"] });
    },
  });

  const publishedSimulations = (simulationsQuery.data ?? []).filter(
    (simulation) =>
      simulation.status === "published" || simulation.livePublishedVersionNumber != null,
  );

  function handleSelectSimulation(simulation: SimulationSummaryResponse) {
    setSelectedSimulation(simulation);
    setApplicationCycleId(newApplicationCycleId());
    setFormErrors({});
    setStep("form");
  }

  function validateCandidateForm() {
    const parsed = candidateLinkSchema.safeParse({ candidateName, candidateEmail });
    if (parsed.success) {
      setFormErrors({});
      return parsed.data;
    }
    const errors: CandidateFormErrors = {};
    for (const issue of parsed.error.issues) {
      const field = issue.path[0];
      if (field === "candidateName") errors.candidateName = issue.message;
      if (field === "candidateEmail") errors.candidateEmail = issue.message;
    }
    setFormErrors(errors);
    return null;
  }

  function handleGenerateLink() {
    if (!selectedSimulation) return;
    const validForm = validateCandidateForm();
    if (!validForm) return;

    const cycleId = applicationCycleId || newApplicationCycleId();
    if (!applicationCycleId) setApplicationCycleId(cycleId);

    linkMutation.mutate({
      simulationId: selectedSimulation.id,
      candidateName: validForm.candidateName,
      candidateEmail: validForm.candidateEmail,
      applicationCycleId: cycleId,
      applicationContext: `Envio direto - ${selectedSimulation.name}`,
    });
  }

  function handleCopy() {
    navigator.clipboard.writeText(generatedLink).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  }

  function handleShareEmail() {
    const subject = encodeURIComponent(
      t.sendLinkPage.shareSubject.replace("{simulation}", simulationName),
    );
    const body = encodeURIComponent(
      t.sendLinkPage.shareEmailBody
        .replace("{name}", candidateName)
        .replace("{link}", generatedLink),
    );
    window.open(`mailto:${candidateEmail}?subject=${subject}&body=${body}`);
  }

  function handleShareWhatsApp() {
    const text = encodeURIComponent(
      t.sendLinkPage.shareWhatsAppText
        .replace("{name}", candidateName)
        .replace("{link}", generatedLink),
    );
    window.open(`https://wa.me/?text=${text}`, "_blank");
  }

  function handleReset() {
    setStep("select");
    setSelectedSimulation(null);
    setCandidateName("");
    setCandidateEmail("");
    setApplicationCycleId("");
    setFormErrors({});
    setGeneratedLink("");
    setSimulationName("");
    setGeneratedOperation(null);
    setCopied(false);
    linkMutation.reset();
  }

  return (
    <AppShell>
      <div className="mb-6">
        <div className="text-xs uppercase text-primary">{t.sendLinkPage.headerEyebrow}</div>
        <h1 className="mt-1 text-3xl font-semibold">{t.sendLinkPage.headerTitle}</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Crie uma nova aplicação quando o candidato participar de outro ciclo, vaga ou etapa.
          Para mandar novamente uma tentativa já existente, use a ação “Reenviar link” na lista.
        </p>
      </div>

      <div className="mb-6 flex gap-4">
        <StepIndicator
          number={1}
          label={t.sendLinkPage.stepEvaluation}
          active={step === "select"}
          done={step === "form" || step === "share"}
        />
        <StepIndicator
          number={2}
          label={t.sendLinkPage.stepParticipant}
          active={step === "form"}
          done={step === "share"}
        />
        <StepIndicator
          number={3}
          label={t.sendLinkPage.stepShare}
          active={step === "share"}
          done={false}
        />
      </div>

      {linkMutation.isError && (
        <StateBanner tone="danger" title={t.sendLinkPage.generateErrorTitle}>
          {linkMutation.error instanceof Error
            ? linkMutation.error.message
            : t.sendLinkPage.generateErrorFallback}
        </StateBanner>
      )}

      {step === "select" && (
        <SelectSimulationStep
          simulations={publishedSimulations}
          loading={simulationsQuery.isLoading}
          error={simulationsQuery.isError}
          errorMessage={
            simulationsQuery.error instanceof Error ? simulationsQuery.error.message : undefined
          }
          onSelect={handleSelectSimulation}
        />
      )}

      {step === "form" && selectedSimulation && (
        <CandidateFormStep
          simulation={selectedSimulation}
          candidateName={candidateName}
          candidateEmail={candidateEmail}
          errors={formErrors}
          onNameChange={(value) => {
            setCandidateName(value);
            if (formErrors.candidateName) {
              setFormErrors((current) => ({ ...current, candidateName: undefined }));
            }
          }}
          onEmailChange={(value) => {
            setCandidateEmail(value);
            if (formErrors.candidateEmail) {
              setFormErrors((current) => ({ ...current, candidateEmail: undefined }));
            }
          }}
          onSubmit={handleGenerateLink}
          onBack={() => setStep("select")}
          loading={linkMutation.isPending}
        />
      )}

      {step === "share" && (
        <ShareStep
          link={generatedLink}
          simulationName={simulationName}
          candidateName={candidateName}
          candidateEmail={candidateEmail}
          operation={generatedOperation}
          copied={copied}
          onCopy={handleCopy}
          onShareEmail={handleShareEmail}
          onShareWhatsApp={handleShareWhatsApp}
          onReset={handleReset}
        />
      )}

      <CandidateLinksTable
        links={candidateLinksQuery.data ?? []}
        loading={candidateLinksQuery.isLoading}
        error={candidateLinksQuery.isError}
        errorMessage={
          candidateLinksQuery.error instanceof Error ? candidateLinksQuery.error.message : undefined
        }
      />
    </AppShell>
  );
}

function StepIndicator({
  number,
  label,
  active,
  done,
}: {
  number: number;
  label: string;
  active: boolean;
  done: boolean;
}) {
  return (
    <div className="flex items-center gap-2">
      <div
        className={cn(
          "flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold",
          done
            ? "bg-success text-success-foreground"
            : active
              ? "bg-primary text-primary-foreground"
              : "bg-muted text-muted-foreground",
        )}
      >
        {done ? <CheckCircle2 className="h-4 w-4" /> : number}
      </div>
      <span className={cn("text-sm", active ? "font-medium text-foreground" : "text-muted-foreground")}>
        {label}
      </span>
    </div>
  );
}

function SelectSimulationStep({
  simulations,
  loading,
  error,
  errorMessage,
  onSelect,
}: {
  simulations: SimulationSummaryResponse[];
  loading: boolean;
  error: boolean;
  errorMessage?: string;
  onSelect: (simulation: SimulationSummaryResponse) => void;
}) {
  const { t } = useLanguage();

  if (loading) {
    return (
      <section className="rounded-md border border-border bg-card p-4">
        <SkeletonRows rows={4} />
      </section>
    );
  }

  if (error) {
    return (
      <StateBanner tone="danger" title={t.sendLinkPage.loadEvaluationsErrorTitle}>
        {errorMessage ?? t.sendLinkPage.systemUnavailableRetry}
      </StateBanner>
    );
  }

  if (simulations.length === 0) {
    return (
      <EmptyState
        title={t.sendLinkPage.noEvaluationsTitle}
        description={t.sendLinkPage.noEvaluationsDescription}
        actions={
          <Link
            to="/nova/avaliacao"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            {t.sendLinkPage.createEvaluation}
          </Link>
        }
      />
    );
  }

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold">{t.sendLinkPage.selectEvaluationHeading}</h2>
      <section className="rounded-md border border-border bg-card">
        <div className="overflow-x-auto">
          <Table className="min-w-[860px]">
            <TableHeader>
              <TableRow>
                <TableHead>{t.sendLinkPage.tableEvaluation}</TableHead>
                <TableHead>{t.sendLinkPage.tableDescription}</TableHead>
                <TableHead>{t.common.status}</TableHead>
                <TableHead className="text-right">{t.sendLinkPage.tableVersion}</TableHead>
                <TableHead>{t.sendLinkPage.tableCompetencies}</TableHead>
                <TableHead className="text-right">{t.sendLinkPage.tableAttempts}</TableHead>
                <TableHead className="w-[130px] text-right">{t.sendLinkPage.tableAction}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {simulations.map((simulation) => (
                <TableRow key={simulation.id}>
                  <TableCell className="max-w-[220px] truncate font-medium">
                    {simulation.name}
                  </TableCell>
                  <TableCell className="max-w-[320px] text-muted-foreground">
                    <span className="line-clamp-2">{simulation.description}</span>
                  </TableCell>
                  <TableCell>
                    <StatusBadge
                      status={simulation.status}
                      maturity={maturityForStatus(simulation.status)}
                    />
                  </TableCell>
                  <TableCell className="text-right tabular-nums">
                    v{simulation.versionNumber}
                  </TableCell>
                  <TableCell className="max-w-[220px]">
                    <div className="flex flex-wrap gap-1">
                      {simulation.competencies.slice(0, 3).map((competency) => (
                        <span
                          key={competency}
                          className="rounded-md border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground"
                        >
                          {competency}
                        </span>
                      ))}
                      {simulation.competencies.length > 3 && (
                        <span className="rounded-md border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground">
                          +{simulation.competencies.length - 3}
                        </span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right tabular-nums">
                    {simulation.attemptsCreated.toLocaleString("pt-BR")}
                  </TableCell>
                  <TableCell className="text-right">
                    <button
                      type="button"
                      onClick={() => onSelect(simulation)}
                      className="inline-flex items-center justify-center rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
                    >
                      {t.sendLinkPage.select}
                    </button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </section>
    </div>
  );
}

function toParticipantPageUrl(candidateUrl: string) {
  if (typeof window === "undefined") return candidateUrl;
  try {
    const token = new URL(candidateUrl).pathname.split("/candidato/")[1];
    if (token) return `${window.location.origin}/candidato/${token}`;
  } catch {
    // Mantém a URL recebida quando ela não pode ser normalizada.
  }
  return candidateUrl;
}

function getParticipantLink(link: CandidateLinkResponse) {
  return toParticipantPageUrl(link.candidateUrl);
}

function candidateStatusLabel(
  status: CandidateLinkResponse["status"],
  t: ReturnType<typeof useLanguage>["t"],
) {
  const labels: Record<CandidateLinkResponse["status"], string> = {
    notStarted: t.sendLinkPage.statusCreated,
    inProgress: t.sendLinkPage.statusStarted,
    completed: t.sendLinkPage.statusCompleted,
    abandoned: "Abandonada",
    expired: t.sendLinkPage.statusExpired,
  };
  return labels[status];
}

function linkStatusLabel(link: CandidateLinkResponse) {
  if (link.linkStatus === "expired") return "Expirado";
  if (link.linkStatus === "expiringSoon") {
    return `Expira em ${link.remainingDays} ${link.remainingDays === 1 ? "dia" : "dias"}`;
  }
  return `${link.remainingDays} ${link.remainingDays === 1 ? "dia restante" : "dias restantes"}`;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function CandidateLinksTable({
  links,
  loading,
  error,
  errorMessage,
}: {
  links: CandidateLinkResponse[];
  loading: boolean;
  error: boolean;
  errorMessage?: string;
}) {
  const { t } = useLanguage();
  const queryClient = useQueryClient();
  const [copiedAttemptId, setCopiedAttemptId] = useState<string | null>(null);
  const [resendingAttemptId, setResendingAttemptId] = useState<string | null>(null);
  const [extendingAttemptId, setExtendingAttemptId] = useState<string | null>(null);
  const [extensionDays, setExtensionDays] = useState<Record<string, number>>({});
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

  const totalPages = Math.max(1, Math.ceil(links.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const pageStartIndex = (safeCurrentPage - 1) * pageSize;
  const pageEndIndex = Math.min(pageStartIndex + pageSize, links.length);
  const paginatedLinks = links.slice(pageStartIndex, pageEndIndex);
  const pageRangeStart = links.length === 0 ? 0 : pageStartIndex + 1;

  async function copyLink(link: CandidateLinkResponse) {
    if (link.linkStatus === "expired") return;
    await navigator.clipboard.writeText(getParticipantLink(link));
    setCopiedAttemptId(link.attemptId);
    window.setTimeout(() => setCopiedAttemptId(null), 2000);
  }

  async function shareExistingLink(link: CandidateLinkResponse) {
    if (link.linkStatus === "expired") return;
    setResendingAttemptId(link.attemptId);
    setActionError(null);
    setActionMessage(null);
    try {
      const response = await resendCandidateLink(link.attemptId);
      const url = toParticipantPageUrl(response.candidateUrl);
      const text = t.sendLinkPage.shareText
        .replace("{name}", link.candidateName)
        .replace("{simulation}", link.simulationName);
      if (navigator.share) {
        await navigator.share({
          title: t.sendLinkPage.shareSubject.replace("{simulation}", link.simulationName),
          text,
          url,
        });
        return;
      }
      await navigator.clipboard.writeText(url);
      setCopiedAttemptId(link.attemptId);
      window.setTimeout(() => setCopiedAttemptId(null), 2000);
    } catch (shareError) {
      setActionError(
        shareError instanceof Error ? shareError.message : "Não foi possível reenviar o link.",
      );
    } finally {
      setResendingAttemptId(null);
    }
  }

  async function addValidityDays(link: CandidateLinkResponse) {
    const additionalDays = extensionDays[link.attemptId] ?? 7;
    setExtendingAttemptId(link.attemptId);
    setActionError(null);
    setActionMessage(null);
    try {
      await extendCandidateLink(link.attemptId, additionalDays);
      await queryClient.invalidateQueries({ queryKey: ["candidate-links"] });
      setActionMessage(
        link.linkStatus === "expired"
          ? `Link de ${link.candidateName} reativado por mais ${additionalDays} dias.`
          : `Foram adicionados ${additionalDays} dias ao link de ${link.candidateName}.`,
      );
    } catch (extensionError) {
      setActionError(
        extensionError instanceof Error
          ? extensionError.message
          : "Não foi possível alterar a validade do link.",
      );
    } finally {
      setExtendingAttemptId(null);
    }
  }

  return (
    <section className="mt-8 rounded-md border border-border bg-card">
      <div className="border-b border-border p-5">
        <h2 className="text-xl font-semibold">{t.sendLinkPage.linksHeading}</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          A validade é independente do andamento da avaliação. Links expirados não podem ser
          copiados ou reenviados até que a empresa acrescente novos dias.
        </p>
      </div>
      {actionError && (
        <div className="p-5 pb-0">
          <StateBanner tone="danger" title="Falha ao atualizar o link">
            {actionError}
          </StateBanner>
        </div>
      )}
      {actionMessage && (
        <div className="p-5 pb-0">
          <StateBanner tone="ok" title="Validade atualizada">
            {actionMessage}
          </StateBanner>
        </div>
      )}
      {loading ? (
        <div className="p-5">
          <SkeletonRows rows={4} />
        </div>
      ) : error ? (
        <div className="p-5">
          <StateBanner tone="danger" title={t.sendLinkPage.loadLinksErrorTitle}>
            {errorMessage ?? t.sendLinkPage.systemUnavailableRetry}
          </StateBanner>
        </div>
      ) : links.length === 0 ? (
        <div className="p-5 text-sm text-muted-foreground">{t.sendLinkPage.noLinksYet}</div>
      ) : (
        <div>
          <div className="overflow-x-auto">
            <Table className="min-w-[1500px]">
              <TableHeader>
                <TableRow>
                  <TableHead>{t.sendLinkPage.tableParticipant}</TableHead>
                  <TableHead>{t.sendLinkPage.tableEvaluationFlow}</TableHead>
                  <TableHead>Andamento</TableHead>
                  <TableHead>Situação do link</TableHead>
                  <TableHead>Criado em</TableHead>
                  <TableHead>Válido até</TableHead>
                  <TableHead>{t.sendLinkPage.tableLink}</TableHead>
                  <TableHead className="w-[390px] text-right">
                    {t.sendLinkPage.tableActions}
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {paginatedLinks.map((link) => {
                  const participantLink = getParticipantLink(link);
                  const copied = copiedAttemptId === link.attemptId;
                  const resending = resendingAttemptId === link.attemptId;
                  const extending = extendingAttemptId === link.attemptId;
                  const expired = link.linkStatus === "expired";
                  return (
                    <TableRow key={link.attemptId}>
                      <TableCell className="min-w-[220px]">
                        <div className="font-medium text-foreground">{link.candidateName}</div>
                        {link.candidateEmail && (
                          <div className="mt-0.5 text-xs text-muted-foreground">
                            {link.candidateEmail}
                          </div>
                        )}
                      </TableCell>
                      <TableCell className="min-w-[220px]">
                        <div className="font-medium text-foreground">{link.simulationName}</div>
                        <div className="mt-0.5 max-w-[220px] truncate text-xs text-muted-foreground">
                          ID: {link.simulationId}
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="rounded-md border border-border bg-background px-2 py-1 text-xs text-muted-foreground">
                          {candidateStatusLabel(link.status, t)}
                        </span>
                      </TableCell>
                      <TableCell>
                        <span
                          className={cn(
                            "inline-flex rounded-md border px-2 py-1 text-xs font-medium",
                            link.linkStatus === "expired" &&
                              "border-danger/40 bg-danger/10 text-danger",
                            link.linkStatus === "expiringSoon" &&
                              "border-warning/40 bg-warning/10 text-warning-foreground",
                            link.linkStatus === "active" &&
                              "border-success/40 bg-success/10 text-success",
                          )}
                        >
                          {linkStatusLabel(link)}
                        </span>
                      </TableCell>
                      <TableCell className="whitespace-nowrap text-xs text-muted-foreground">
                        {formatDateTime(link.createdAt)}
                      </TableCell>
                      <TableCell className="whitespace-nowrap text-xs">
                        <div className={cn(expired ? "font-medium text-danger" : "text-foreground")}>
                          {formatDateTime(link.linkExpiresAt)}
                        </div>
                        <div className="mt-0.5 text-[11px] text-muted-foreground">
                          Emitido em {formatDateTime(link.linkIssuedAt)}
                        </div>
                      </TableCell>
                      <TableCell className="max-w-[260px]">
                        <code
                          className={cn(
                            "block truncate rounded-md border border-border bg-background px-2 py-1.5 text-xs",
                            expired ? "text-danger line-through" : "text-muted-foreground",
                          )}
                        >
                          {participantLink}
                        </code>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex flex-wrap justify-end gap-2">
                          <button
                            type="button"
                            disabled={expired}
                            title={expired ? "Reative o link antes de copiar." : undefined}
                            onClick={() => void copyLink(link)}
                            className={cn(
                              "inline-flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-xs font-medium hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50",
                              copied
                                ? "border-success bg-success/10 text-success"
                                : "border-border bg-card",
                            )}
                          >
                            {copied ? (
                              <CheckCircle2 className="h-3.5 w-3.5" />
                            ) : (
                              <Copy className="h-3.5 w-3.5" />
                            )}
                            {copied ? t.sendLinkPage.copied : t.sendLinkPage.copy}
                          </button>
                          <button
                            type="button"
                            disabled={expired || resending}
                            title={expired ? "Reative o link antes de reenviar." : undefined}
                            onClick={() => void shareExistingLink(link)}
                            className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-3 py-1.5 text-xs font-medium hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            <RefreshCw className={cn("h-3.5 w-3.5", resending && "animate-spin")} />
                            {resending ? "Enviando..." : "Reenviar"}
                          </button>
                          <div className="flex items-center rounded-md border border-border bg-background">
                            <label className="sr-only" htmlFor={`extension-days-${link.attemptId}`}>
                              Dias adicionais
                            </label>
                            <select
                              id={`extension-days-${link.attemptId}`}
                              value={extensionDays[link.attemptId] ?? 7}
                              onChange={(event) =>
                                setExtensionDays((current) => ({
                                  ...current,
                                  [link.attemptId]: Number(event.target.value),
                                }))
                              }
                              className="h-8 border-0 bg-transparent px-2 text-xs outline-none"
                            >
                              {EXTENSION_DAY_OPTIONS.map((days) => (
                                <option key={days} value={days}>
                                  +{days} {days === 1 ? "dia" : "dias"}
                                </option>
                              ))}
                            </select>
                            <button
                              type="button"
                              disabled={extending}
                              onClick={() => void addValidityDays(link)}
                              className="inline-flex h-8 items-center gap-1 border-l border-border px-2 text-xs font-medium text-primary hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              <CalendarPlus className={cn("h-3.5 w-3.5", extending && "animate-pulse")} />
                              {extending ? "Atualizando..." : expired ? "Reativar" : "Adicionar"}
                            </button>
                          </div>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
          <div
            className="flex w-full flex-wrap items-center justify-between gap-3 border-t border-border px-4 py-3 text-sm text-muted-foreground"
            role="navigation"
            aria-label="Paginação dos links enviados"
          >
            <span className="tabular-nums" aria-live="polite">
              {pageRangeStart}–{pageEndIndex} de {links.length}
            </span>
            <div className="flex flex-wrap items-center gap-2">
              <label htmlFor="candidate-links-page-size" className="flex items-center gap-2">
                <span className="sr-only sm:not-sr-only">Linhas por página</span>
                <select
                  id="candidate-links-page-size"
                  aria-label="Linhas por página"
                  value={pageSize}
                  onChange={(event) => {
                    setPageSize(Number(event.target.value));
                    setCurrentPage(1);
                  }}
                  className="h-9 rounded-md border border-border bg-background px-2 text-sm text-foreground outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  {PAGE_SIZE_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </label>
              <button
                type="button"
                onClick={() => setCurrentPage(Math.max(1, safeCurrentPage - 1))}
                disabled={safeCurrentPage <= 1}
                aria-label="Ir para a página anterior"
                className="inline-flex h-9 items-center justify-center rounded-md border border-border bg-background px-3 text-sm font-medium text-foreground transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
              >
                Anterior
              </button>
              <span className="min-w-24 text-center tabular-nums text-foreground" aria-live="polite">
                Página {safeCurrentPage} de {totalPages}
              </span>
              <button
                type="button"
                onClick={() => setCurrentPage(Math.min(totalPages, safeCurrentPage + 1))}
                disabled={safeCurrentPage >= totalPages}
                aria-label="Ir para a próxima página"
                className="inline-flex h-9 items-center justify-center rounded-md border border-border bg-background px-3 text-sm font-medium text-foreground transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
              >
                Próxima
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

function CandidateFormStep({
  simulation,
  candidateName,
  candidateEmail,
  errors,
  onNameChange,
  onEmailChange,
  onSubmit,
  onBack,
  loading,
}: {
  simulation: SimulationSummaryResponse;
  candidateName: string;
  candidateEmail: string;
  errors: CandidateFormErrors;
  onNameChange: (value: string) => void;
  onEmailChange: (value: string) => void;
  onSubmit: () => void;
  onBack: () => void;
  loading: boolean;
}) {
  const { t } = useLanguage();

  return (
    <div className="space-y-6">
      <StateBanner tone="info" title="Nova aplicação">
        Esta ação cria uma tentativa independente para este processo. Mesmo candidato e avaliação
        podem ter novas aplicações em outros ciclos. Para apenas mandar o link anterior novamente,
        volte à lista e use “Reenviar link”.
      </StateBanner>
      <div className="rounded-md border border-border bg-card p-6">
        <h2 className="text-lg font-semibold">{t.sendLinkPage.participantDataHeading}</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          {t.sendLinkPage.participantDataDescription}
        </p>
        <div className="mt-5 space-y-4">
          <div>
            <label htmlFor="candidate-name" className="mb-1.5 block text-sm font-medium">
              {t.sendLinkPage.participantNameLabel}
            </label>
            <input
              id="candidate-name"
              type="text"
              className={cn("input w-full", errors.candidateName && "border-danger")}
              placeholder={t.sendLinkPage.participantNamePlaceholder}
              value={candidateName}
              onChange={(event) => onNameChange(event.target.value)}
              aria-invalid={Boolean(errors.candidateName)}
            />
            {errors.candidateName && (
              <p className="mt-1 text-xs text-danger">{errors.candidateName}</p>
            )}
          </div>
          <div>
            <label htmlFor="candidate-email" className="mb-1.5 block text-sm font-medium">
              {t.sendLinkPage.participantEmailLabel}
            </label>
            <input
              id="candidate-email"
              type="email"
              className={cn("input w-full", errors.candidateEmail && "border-danger")}
              placeholder={t.sendLinkPage.participantEmailPlaceholder}
              value={candidateEmail}
              onChange={(event) => onEmailChange(event.target.value)}
              aria-invalid={Boolean(errors.candidateEmail)}
            />
            {errors.candidateEmail && (
              <p className="mt-1 text-xs text-danger">{errors.candidateEmail}</p>
            )}
          </div>
        </div>
        <div className="mt-6 flex gap-3">
          <button
            type="button"
            onClick={onBack}
            className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
          >
            {t.sendLinkPage.back}
          </button>
          <button
            type="button"
            onClick={onSubmit}
            disabled={loading}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <Link2 className="h-4 w-4" />
            {loading ? t.sendLinkPage.generating : "Criar nova aplicação"}
          </button>
        </div>
      </div>
      <aside className="rounded-md border border-border bg-card p-5">
        <h3 className="text-sm font-semibold">{t.sendLinkPage.selectedEvaluationHeading}</h3>
        <div className="mt-3 space-y-2">
          <div className="font-medium text-foreground">{simulation.name}</div>
          <div className="text-xs text-muted-foreground">{simulation.description}</div>
          <StatusBadge status={simulation.status} maturity={maturityForStatus(simulation.status)} />
          <div className="mt-2 flex flex-wrap gap-1">
            {simulation.competencies.map((competency) => (
              <span
                key={competency}
                className="rounded-md border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground"
              >
                {competency}
              </span>
            ))}
          </div>
          <div className="mt-3 text-xs text-muted-foreground">
            {t.sendLinkPage.versionAttempts
              .replace("{version}", String(simulation.versionNumber))
              .replace("{count}", simulation.attemptsCreated.toLocaleString("pt-BR"))}
          </div>
        </div>
      </aside>
    </div>
  );
}

function ShareStep({
  link,
  simulationName,
  candidateName,
  candidateEmail,
  operation,
  copied,
  onCopy,
  onShareEmail,
  onShareWhatsApp,
  onReset,
}: {
  link: string;
  simulationName: string;
  candidateName: string;
  candidateEmail: string;
  operation: CandidateLinkOperation | null;
  copied: boolean;
  onCopy: () => void;
  onShareEmail: () => void;
  onShareWhatsApp: () => void;
  onReset: () => void;
}) {
  const { t } = useLanguage();
  const reconciled = operation === "REUSED_IDEMPOTENT_REQUEST";

  return (
    <div className="space-y-6">
      <StateBanner
        tone="ok"
        title={reconciled ? "Aplicação já criada anteriormente" : "Nova aplicação criada"}
      >
        {reconciled
          ? "A mesma solicitação já havia sido processada. O sistema devolveu a tentativa daquele ciclo sem criar duplicidade."
          : t.sendLinkPage.successBanner
              .replace("{simulation}", simulationName)
              .replace("{name}", candidateName)
              .replace("{email}", candidateEmail)}
      </StateBanner>
      <div className="space-y-6">
        <div className="space-y-5">
          <div className="rounded-md border border-border bg-card p-6">
            <h2 className="text-lg font-semibold">{t.sendLinkPage.participationLinkHeading}</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {t.sendLinkPage.participationLinkDescription}
            </p>
            <div className="mt-4 flex items-center gap-2">
              <div className="min-w-0 flex-1 rounded-md border border-border bg-background px-3 py-2.5">
                <code className="block truncate text-sm text-foreground">{link}</code>
              </div>
              <button
                type="button"
                onClick={onCopy}
                className={cn(
                  "inline-flex shrink-0 items-center gap-2 rounded-md border px-4 py-2.5 text-sm font-medium transition",
                  copied
                    ? "border-success bg-success/10 text-success"
                    : "border-border bg-card hover:bg-accent",
                )}
              >
                {copied ? (
                  <CheckCircle2 className="h-4 w-4" />
                ) : (
                  <Copy className="h-4 w-4" />
                )}
                {copied ? t.sendLinkPage.copied : t.sendLinkPage.copy}
              </button>
            </div>
          </div>
          <div className="rounded-md border border-border bg-card p-6">
            <h2 className="text-lg font-semibold">{t.sendLinkPage.share}</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {t.sendLinkPage.shareChannelDescription}
            </p>
            <div className="mt-4 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={onShareEmail}
                className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-5 py-3 text-sm font-medium transition hover:bg-accent"
              >
                <Mail className="h-4 w-4" />
                {t.sendLinkPage.sendByEmail}
              </button>
              <button
                type="button"
                onClick={onShareWhatsApp}
                className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-5 py-3 text-sm font-medium transition hover:bg-accent"
              >
                <MessageCircle className="h-4 w-4" />
                {t.sendLinkPage.sendByWhatsApp}
              </button>
            </div>
          </div>
        </div>
        <StateBanner tone="info" title="Aplicação independente">
          Criar outra aplicação gera um novo ciclo. Para compartilhar novamente esta mesma tentativa,
          use “Reenviar link” na lista abaixo.
        </StateBanner>
      </div>
      <div className="flex gap-3">
        <button
          type="button"
          onClick={onReset}
          className="inline-flex items-center gap-2 rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Send className="h-4 w-4" />
          Criar outra aplicação
        </button>
        <Link
          to="/dashboard"
          className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          {t.sendLinkPage.backToDashboard}
        </Link>
      </div>
    </div>
  );
}

function newApplicationCycleId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `manual-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
}
