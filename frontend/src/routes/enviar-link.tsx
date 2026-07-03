import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { CheckCircle2, Copy, Link2, Mail, MessageCircle, Send } from "lucide-react";
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
  listCandidateLinks,
  listSimulations,
  type CandidateLinkResponse,
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
        content: "Gere um link de avaliação para compartilhar por e-mail ou WhatsApp.",
      },
    ],
  }),
  component: EnviarLinkPage,
});

type Step = "select" | "form" | "share";

function EnviarLinkPage() {
  const { t } = useLanguage();
  const queryClient = useQueryClient();
  const [step, setStep] = useState<Step>("select");
  const [selectedSimulation, setSelectedSimulation] = useState<SimulationSummaryResponse | null>(
    null,
  );
  const [candidateName, setCandidateName] = useState("");
  const [candidateEmail, setCandidateEmail] = useState("");
  const [generatedLink, setGeneratedLink] = useState("");
  const [simulationName, setSimulationName] = useState("");
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
      setStep("share");
      void queryClient.invalidateQueries({ queryKey: ["candidate-links"] });
    },
  });

  // Inclui também avaliações cuja versão exibida é rascunho mas mantêm uma versão
  // publicada no ar: o link de candidato é gerado contra a versão publicada ativa.
  const publishedSimulations = (simulationsQuery.data ?? []).filter(
    (s) => s.status === "published" || s.livePublishedVersionNumber != null,
  );

  function handleSelectSimulation(simulation: SimulationSummaryResponse) {
    setSelectedSimulation(simulation);
    setStep("form");
  }

  function handleGenerateLink() {
    if (!selectedSimulation || !candidateName.trim() || !candidateEmail.trim()) return;
    linkMutation.mutate({
      simulationId: selectedSimulation.id,
      candidateName: candidateName.trim(),
      candidateEmail: candidateEmail.trim(),
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
    setGeneratedLink("");
    setSimulationName("");
    setCopied(false);
    linkMutation.reset();
  }

  return (
    <AppShell>
      <div className="mb-6">
        <div className="text-xs uppercase text-primary">{t.sendLinkPage.headerEyebrow}</div>
        <h1 className="mt-1 text-3xl font-semibold">{t.sendLinkPage.headerTitle}</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">{t.sendLinkPage.headerLede}</p>
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
          onNameChange={setCandidateName}
          onEmailChange={setCandidateEmail}
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
      <span
        className={cn("text-sm", active ? "font-medium text-foreground" : "text-muted-foreground")}
      >
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
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t.sendLinkPage.tableEvaluation}</TableHead>
              <TableHead>{t.sendLinkPage.tableDescription}</TableHead>
              <TableHead>{t.common.status}</TableHead>
              <TableHead className="text-right">{t.sendLinkPage.tableVersion}</TableHead>
              <TableHead>{t.sendLinkPage.tableCompetencies}</TableHead>
              <TableHead className="text-right">{t.sendLinkPage.tableAttempts}</TableHead>
              <TableHead className="text-right">{t.sendLinkPage.tableAction}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {simulations.map((simulation) => (
              <TableRow key={simulation.id}>
                <TableCell className="min-w-[180px] font-medium">{simulation.name}</TableCell>
                <TableCell className="min-w-[220px] max-w-[360px] text-muted-foreground">
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
                <TableCell className="min-w-[220px]">
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
      </section>
    </div>
  );
}

// The candidate page (`/candidato/{token}`) is always served by this frontend
// app on the same origin the RH user is currently on. The backend builds
// `candidateUrl` from `PRAXIS_CANDIDATE_PAGE_BASE_URL`, which may be
// misconfigured to the API domain (e.g. `api-praxis.iforce.com.br`). Rebuild the
// link against the current origin so the displayed/copied/shared link always
// points to the candidate page that actually exists.
function toParticipantPageUrl(candidateUrl: string) {
  if (typeof window === "undefined") {
    return candidateUrl;
  }
  try {
    const token = new URL(candidateUrl).pathname.split("/candidato/")[1];
    if (token) {
      return `${window.location.origin}/candidato/${token}`;
    }
  } catch {
    // Fall back to the backend-provided URL if it cannot be parsed.
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
    created: t.sendLinkPage.statusCreated,
    started: t.sendLinkPage.statusStarted,
    completed: t.sendLinkPage.statusCompleted,
    expired: t.sendLinkPage.statusExpired,
  };

  return labels[status] ?? status;
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
  const [copiedAttemptId, setCopiedAttemptId] = useState<string | null>(null);

  async function copyLink(link: CandidateLinkResponse) {
    await navigator.clipboard.writeText(getParticipantLink(link));
    setCopiedAttemptId(link.attemptId);
    window.setTimeout(() => setCopiedAttemptId(null), 2000);
  }

  async function shareLink(link: CandidateLinkResponse) {
    const url = getParticipantLink(link);
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
  }

  return (
    <section className="mt-8 rounded-md border border-border bg-card">
      <div className="border-b border-border p-5">
        <h2 className="text-xl font-semibold">{t.sendLinkPage.linksHeading}</h2>
        <p className="mt-1 text-sm text-muted-foreground">{t.sendLinkPage.linksDescription}</p>
      </div>

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
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t.sendLinkPage.tableParticipant}</TableHead>
                <TableHead>{t.sendLinkPage.tableEvaluationFlow}</TableHead>
                <TableHead>{t.common.status}</TableHead>
                <TableHead>{t.sendLinkPage.tableLink}</TableHead>
                <TableHead className="text-right">{t.sendLinkPage.tableActions}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {links.map((link) => {
                const participantLink = getParticipantLink(link);
                const copied = copiedAttemptId === link.attemptId;

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
                      <div className="mt-0.5 text-xs text-muted-foreground">
                        ID: {link.simulationId}
                      </div>
                    </TableCell>
                    <TableCell>
                      <span className="rounded-md border border-border bg-background px-2 py-1 text-xs text-muted-foreground">
                        {candidateStatusLabel(link.status, t)}
                      </span>
                    </TableCell>
                    <TableCell className="min-w-[280px] max-w-[420px]">
                      <code className="block truncate rounded-md border border-border bg-background px-2 py-1.5 text-xs text-muted-foreground">
                        {participantLink}
                      </code>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => void copyLink(link)}
                          className={cn(
                            "inline-flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-xs font-medium hover:bg-accent",
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
                          onClick={() => void shareLink(link)}
                          className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                        >
                          <Send className="h-3.5 w-3.5" />
                          {t.sendLinkPage.share}
                        </button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      )}
    </section>
  );
}

function CandidateFormStep({
  simulation,
  candidateName,
  candidateEmail,
  onNameChange,
  onEmailChange,
  onSubmit,
  onBack,
  loading,
}: {
  simulation: SimulationSummaryResponse;
  candidateName: string;
  candidateEmail: string;
  onNameChange: (value: string) => void;
  onEmailChange: (value: string) => void;
  onSubmit: () => void;
  onBack: () => void;
  loading: boolean;
}) {
  const { t } = useLanguage();
  const valid = candidateName.trim().length > 0 && candidateEmail.trim().includes("@");

  return (
    <div className="space-y-6">
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
              className="input w-full"
              placeholder={t.sendLinkPage.participantNamePlaceholder}
              value={candidateName}
              onChange={(e) => onNameChange(e.target.value)}
            />
          </div>

          <div>
            <label htmlFor="candidate-email" className="mb-1.5 block text-sm font-medium">
              {t.sendLinkPage.participantEmailLabel}
            </label>
            <input
              id="candidate-email"
              type="email"
              className="input w-full"
              placeholder={t.sendLinkPage.participantEmailPlaceholder}
              value={candidateEmail}
              onChange={(e) => onEmailChange(e.target.value)}
            />
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
            disabled={!valid || loading}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <Link2 className="h-4 w-4" />
            {loading ? t.sendLinkPage.generating : t.sendLinkPage.generateLink}
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
  copied: boolean;
  onCopy: () => void;
  onShareEmail: () => void;
  onShareWhatsApp: () => void;
  onReset: () => void;
}) {
  const { t } = useLanguage();
  return (
    <div className="space-y-6">
      <StateBanner tone="ok" title={t.sendLinkPage.successTitle}>
        {t.sendLinkPage.successBanner
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
                {copied ? <CheckCircle2 className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
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

        <StateBanner tone="info" title={t.sendLinkPage.oneLinkTitle}>
          {t.sendLinkPage.oneLinkDescription}
        </StateBanner>
      </div>

      <div className="flex gap-3">
        <button
          type="button"
          onClick={onReset}
          className="inline-flex items-center gap-2 rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Send className="h-4 w-4" />
          {t.sendLinkPage.sendToAnother}
        </button>
        <Link
          to="/"
          className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          {t.sendLinkPage.backToDashboard}
        </Link>
      </div>
    </div>
  );
}
