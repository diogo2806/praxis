import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { Termo } from "@/components/glossario";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  createSimulationDraft,
  getSimulationVersion,
  updateSimulationBlueprint,
  updateEmpresaConfig,
  type EmpresaConfigOption,
} from "@/lib/api/praxis";
import { useLanguage } from "@/lib/language-context";
import { useEmpresaConfig } from "@/lib/empresa-config";
import { getTranslations, type Language } from "@/lib/translations";

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
  head: () => {
    const copy = getTranslations(getStoredLanguage()).blueprint;

    return {
      meta: [
        { title: copy.metaTitle },
        {
          name: "description",
          content: copy.metaDescription,
        },
      ],
    };
  },
  component: Page,
});

function getStoredLanguage(): Language {
  if (typeof window === "undefined") {
    return "pt-BR";
  }

  try {
    const stored = localStorage.getItem("praxis-language");
    if (stored === "pt-BR" || stored === "en" || stored === "es-MX") {
      return stored;
    }
  } catch {
    return "pt-BR";
  }

  return "pt-BR";
}

function Page() {
  const search = Route.useSearch();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { t } = useLanguage();
  const copy = t.blueprint;
  const hasVersionContext = Boolean(search.simulationId && search.versionNumber);
  const {
    config,
    isLoading: empresaConfigLoading,
    isError: empresaConfigError,
    error: empresaConfigQueryError,
  } = useEmpresaConfig();
  const competencies = config?.competencies ?? [];
  const [role, setRole] = useState("");
  const [criticalSituation, setCriticalSituation] = useState("");
  const [selectedCompetencies, setSelectedCompetencies] = useState<string[]>([]);
  const [newCompetency, setNewCompetency] = useState("");
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [publishToMarketplace, setPublishToMarketplace] = useState(false);
  const [hydratedVersionKey, setHydratedVersionKey] = useState<string | null>(null);
  const roleMaxLength = 180;
  const criticalSituationMaxLength = 1200;
  const [competencySearch, setCompetencySearch] = useState("");
  const visibleCompetencies = useMemo(() => {
    const normalizedSearch = competencySearch.trim().toLowerCase();
    return [...competencies]
      .filter((competency) => competency.label.toLowerCase().includes(normalizedSearch))
      .sort((a, b) => a.label.localeCompare(b.label, "pt-BR", { sensitivity: "base" }));
  }, [competencies, competencySearch]);
  const missingFields = [
    role.trim().length === 0 && copy.missingRole,
    criticalSituation.trim().length === 0 && copy.missingCriticalSituation,
    selectedCompetencies.length === 0 && copy.missingCompetency,
  ].filter((field): field is string => Boolean(field));
  const canGoNext =
    role.trim().length > 0 &&
    criticalSituation.trim().length > 0 &&
    selectedCompetencies.length > 0;
  const normalizedCompetencies = competencies.map((competency) =>
    competency.value.trim().toLowerCase(),
  );
  const canAddCompetency =
    newCompetency.trim().length > 0 &&
    !normalizedCompetencies.includes(newCompetency.trim().toLowerCase());
  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasVersionContext,
  });

  useEffect(() => {
    if (!versionQuery.data || empresaConfigLoading) {
      return;
    }

    const versionKey = `${versionQuery.data.simulationId}:${versionQuery.data.versionNumber}`;
    if (hydratedVersionKey === versionKey) {
      return;
    }

    const parsedBlueprint = parseBlueprintDescription(versionQuery.data.description);
    setRole(parsedBlueprint.role || versionQuery.data.name);
    setCriticalSituation(versionQuery.data.criticalSituation ?? parsedBlueprint.criticalSituation);
    setSelectedCompetencies(
      versionQuery.data.blueprint.competencies.map((competency) => competency.name),
    );
    setHydratedVersionKey(versionKey);
  }, [hydratedVersionKey, empresaConfigLoading, versionQuery.data]);

  const createDraftMutation = useMutation({
    mutationFn: () =>
      createSimulationDraft({
        name: role.trim(),
        description: buildBlueprintDescription({
          role,
          criticalSituation,
          competencies: selectedCompetencies,
          labels: {
            role: copy.descriptionRoleLabel,
            criticalSituation: copy.descriptionCriticalSituationLabel,
            competencies: copy.descriptionCompetenciesLabel,
          },
        }),
        rootNodeId: "etapa-1",
        competencies: selectedCompetencies,
        criticalSituation: criticalSituation.trim(),
      }),
    onSuccess: (simulation) => {
      void navigate({
        to: "/nova/personagem",
        search: {
          simulationId: simulation.id,
          versionNumber: simulation.versionNumber,
        },
      });
    },
  });

  const updateExistingMutation = useMutation({
    mutationFn: () =>
      updateSimulationBlueprint(search.simulationId!, search.versionNumber!, {
        rootNodeId: versionQuery.data?.blueprint.rootNodeId ?? "etapa-1",
        competencies: buildCompetencyWeights(
          selectedCompetencies,
          versionQuery.data?.blueprint.competencies,
        ),
        criticalSituation: criticalSituation.trim(),
      }),
    onSuccess: async (simulation) => {
      await queryClient.invalidateQueries({
        queryKey: ["simulation-version", search.simulationId, search.versionNumber],
      });
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
      void navigate({
        to: "/nova/personagem",
        search: {
          simulationId: simulation.id,
          versionNumber: simulation.versionNumber,
        },
      });
    },
  });

  const addCompetencyMutation = useMutation({
    mutationFn: async (value: string) => {
      const nextCompetencies = [
        ...competencies,
        { value, label: value, locked: false, selectedByDefault: false, active: true },
      ];
      return updateEmpresaConfig("COMPETENCY", nextCompetencies);
    },
    onSuccess: async (_, value) => {
      setNewCompetency("");
      setSelectedCompetencies((current) =>
        current.includes(value) ? current : [...current, value],
      );
      await queryClient.invalidateQueries({ queryKey: ["empresa-config"] });
    },
  });

  function toggleCompetency(competency: string) {
    setSelectedCompetencies((current) =>
      current.includes(competency)
        ? current.filter((item) => item !== competency)
        : [...current, competency],
    );
  }

  return (
    <AppShell>
      <WizardStepper current="avaliacao" unlockedThrough={canGoNext ? "cenario" : "avaliacao"} />
      <ScreenStateStrip blockedReason={copy.blockedReason} />

      {empresaConfigLoading && (
        <StateBanner tone="info" title={copy.loadingConfigTitle}>
          {copy.loadingConfigBody}
        </StateBanner>
      )}

      {empresaConfigError && (
        <StateBanner tone="danger" title={copy.configErrorTitle}>
          {empresaConfigQueryError instanceof Error
            ? empresaConfigQueryError.message
            : copy.configErrorFallback}
        </StateBanner>
      )}

      {createDraftMutation.isError && (
        <div className="mb-5">
          <StateBanner tone="danger" title={copy.createErrorTitle}>
            {createDraftMutation.error instanceof Error
              ? createDraftMutation.error.message
              : copy.createErrorFallback}
          </StateBanner>
        </div>
      )}

      {updateExistingMutation.isError && (
        <div className="mb-5">
          <StateBanner tone="danger" title={copy.updateErrorTitle}>
            {updateExistingMutation.error instanceof Error
              ? updateExistingMutation.error.message
              : copy.updateErrorFallback}
          </StateBanner>
        </div>
      )}

      {versionQuery.isLoading && (
        <div className="mb-5">
          <StateBanner tone="info" title={copy.loadingPlanTitle}>
            {copy.loadingPlanBody
              .replace("{simulationId}", search.simulationId ?? "")
              .replace("{versionNumber}", String(search.versionNumber ?? ""))}
          </StateBanner>
        </div>
      )}

      {versionQuery.isError && (
        <div className="mb-5">
          <StateBanner tone="danger" title={copy.planErrorTitle}>
            {versionQuery.error instanceof Error
              ? versionQuery.error.message
              : copy.planErrorFallback}
          </StateBanner>
        </div>
      )}

      {config && !versionQuery.isLoading && !versionQuery.isError && (
        <div className="space-y-6">
          <div className="rounded-xl border border-border bg-card p-5">
            <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              {copy.whyTitle}
            </div>
            <p className="mt-2 text-sm text-foreground/80">
              {copy.whyBodyStart} <Termo id="blueprint">{copy.whyTerm}</Termo>{" "}
              {copy.whyBodyEnd.split("{validator}")[0]}
              <Termo id="validador">{copy.validatorTerm}</Termo>
              {copy.whyBodyEnd.split("{validator}")[1]}
            </p>
          </div>
          <div className="rounded-xl border border-warning/30 bg-warning/10 p-5">
            <div className="text-xs font-semibold uppercase tracking-wider text-warning-foreground">
              {copy.scoringTitle}
            </div>
            <p className="mt-2 text-sm text-foreground/80">
              {copy.scoringBodyStart} <Termo id="blueprint">{copy.whyTerm}</Termo>{" "}
              {copy.scoringBodyEnd}
            </p>
          </div>
          <div className="space-y-6">
            <Header kicker={copy.kicker} title={copy.heading} lede={copy.lede} />

            <p className="text-sm text-muted-foreground">
              Prefere começar de um modelo pronto?{" "}
              <Link to="/nova/rapido" className="font-medium text-primary hover:underline">
                Usar um modelo →
              </Link>
            </p>

            <p className="text-sm text-muted-foreground">
              Quer consultar o que já existe?{" "}
              <Link to="/nova/testes" className="font-medium text-primary hover:underline">
                Ver testes cadastrados →
              </Link>
            </p>

            <Card title={copy.roleCard} required requiredLabel={copy.required}>
              <Field label={copy.roleLabel}>
                <input
                  className={`input ${submitAttempted && role.trim().length === 0 ? "border-danger" : ""}`}
                  placeholder={copy.rolePlaceholder}
                  maxLength={roleMaxLength}
                  required
                  aria-required="true"
                  value={role}
                  onChange={(event) => setRole(event.target.value)}
                />
                <FieldMeta
                  error={
                    submitAttempted && role.trim().length === 0 ? copy.roleRequiredError : undefined
                  }
                  count={role.length}
                  max={roleMaxLength}
                />
              </Field>
            </Card>

            <Card title={copy.criticalSituationCard} required requiredLabel={copy.required}>
              <textarea
                aria-label={copy.criticalSituationAria}
                className={`input min-h-24 ${submitAttempted && criticalSituation.trim().length === 0 ? "border-danger" : ""}`}
                maxLength={criticalSituationMaxLength}
                required
                aria-required="true"
                value={criticalSituation}
                onChange={(event) => setCriticalSituation(event.target.value)}
              />
              <FieldMeta
                error={
                  submitAttempted && criticalSituation.trim().length === 0
                    ? copy.criticalSituationRequiredError
                    : undefined
                }
                count={criticalSituation.length}
                max={criticalSituationMaxLength}
              />
              <Help>{copy.criticalSituationHelp}</Help>
            </Card>

            <Card title={copy.competenciesCard} required requiredLabel={copy.required}>
              <div className="mb-3">
                <input
                  className="input"
                  placeholder="Buscar competência"
                  value={competencySearch}
                  onChange={(event) => setCompetencySearch(event.target.value)}
                  disabled={empresaConfigLoading || addCompetencyMutation.isPending}
                />
                <div className="mt-1 text-xs text-muted-foreground">
                  {visibleCompetencies.length} de {competencies.length} disponíveis
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                {visibleCompetencies.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    Nenhuma competência encontrada para esse filtro.
                  </p>
                ) : (
                  visibleCompetencies.map((competency) => (
                    <label
                      key={competency.value}
                      title={competencyHint(competency.label)}
                      className={`cursor-pointer rounded-full border px-3 py-1.5 text-sm ${
                        selectedCompetencies.includes(competency.value)
                          ? "border-primary bg-primary/10 text-foreground"
                          : "border-border bg-card text-foreground/75 hover:bg-accent"
                      }`}
                    >
                      <input
                        type="checkbox"
                        checked={selectedCompetencies.includes(competency.value)}
                        onChange={() => toggleCompetency(competency.value)}
                        className="sr-only"
                      />
                      {competency.label}
                    </label>
                  ))
                )}
              </div>
              <div className="mt-4 grid gap-2 md:grid-cols-[1fr_auto]">
                <input
                  className="input"
                  placeholder={copy.addCompetencyPlaceholder}
                  value={newCompetency}
                  onChange={(event) => setNewCompetency(event.target.value)}
                />
                <button
                  type="button"
                  onClick={() => addCompetencyMutation.mutate(newCompetency.trim())}
                  disabled={!canAddCompetency || addCompetencyMutation.isPending}
                  className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {addCompetencyMutation.isPending ? copy.saving : copy.addAndSave}
                </button>
              </div>
              <Help>{copy.addedCompetencyHelp}</Help>
              {addCompetencyMutation.isError && (
                <p className="mt-2 text-xs text-danger">
                  {addCompetencyMutation.error instanceof Error
                    ? addCompetencyMutation.error.message
                    : copy.competencySaveError}
                </p>
              )}
              <Help>{copy.customCompetencyHelp}</Help>
            </Card>

            <div className="rounded-xl border border-border bg-card p-5">
              <label className="flex items-start gap-3 text-sm">
                <input
                  type="checkbox"
                  checked={publishToMarketplace}
                  onChange={(event) => setPublishToMarketplace(event.target.checked)}
                  className="mt-1 h-4 w-4"
                />
                <span>
                  <span className="block font-medium">Publicar este teste no Marketplace</span>
                  <span className="mt-1 block text-muted-foreground">
                    Profissionais podem transformar uma versao publicada em anuncio para revisao.
                  </span>
                </span>
              </label>
              {publishToMarketplace && (
                <Link
                  to="/profissional/listings/novo"
                  className="mt-4 inline-flex rounded-md border border-border bg-background px-4 py-2 text-sm hover:bg-accent"
                >
                  Abrir publicacao no Marketplace
                </Link>
              )}
            </div>


            <div className="sticky bottom-0 -mx-6 mt-2 flex flex-col gap-3 border-t border-border bg-background/90 px-6 py-4 backdrop-blur sm:flex-row sm:items-center sm:justify-between lg:-mx-10 lg:px-10">
              <Link
                to="/"
                className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
              >
                {copy.cancel}
              </Link>
              <div className="flex flex-col items-start gap-2 sm:items-end">
                {!canGoNext && (
                  <p
                    className={`text-xs ${submitAttempted ? "text-danger" : "text-muted-foreground"}`}
                    aria-live="polite"
                  >
                    {copy.missingFieldsPrefix} {missingFields.join(", ")}.
                  </p>
                )}
                <button
                  type="button"
                  disabled={createDraftMutation.isPending || updateExistingMutation.isPending}
                  title={canGoNext ? copy.createDraftTitle : copy.disabledNextTitle}
                  onClick={() => {
                    if (!canGoNext) {
                      setSubmitAttempted(true);
                      return;
                    }
                    if (hasVersionContext) {
                      updateExistingMutation.mutate();
                      return;
                    }
                    createDraftMutation.mutate();
                  }}
                  aria-disabled={!canGoNext}
                  className={`rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 ${
                    !canGoNext || createDraftMutation.isPending || updateExistingMutation.isPending
                      ? "cursor-not-allowed opacity-50"
                      : ""
                  }`}
                >
                  {createDraftMutation.isPending ? copy.creatingDraft : copy.next}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
}

function getDefaultOption(options: EmpresaConfigOption[]) {
  return (
    options.find((option) => option.selectedByDefault && !option.locked) ??
    options.find((option) => !option.locked) ??
    options[0]
  );
}

function buildBlueprintDescription({
  role,
  criticalSituation,
  competencies,
  labels,
}: {
  role: string;
  criticalSituation: string;
  competencies: string[];
  labels: {
    role: string;
    criticalSituation: string;
    competencies: string;
  };
}) {
  const lines = [
    `${labels.role}: ${role.trim()}`,
    `${labels.criticalSituation}: ${criticalSituation.trim()}`,
    competencies.length > 0 && `${labels.competencies}: ${competencies.join(", ")}`,
  ].filter((line): line is string => Boolean(line));

  const description = lines.join("\n");
  return description.length > 1000 ? `${description.slice(0, 997).trimEnd()}...` : description;
}

function buildCompetencyWeights(
  competencies: string[],
  existingCompetencies?: { name: string; weight: number; targetScore?: number | null; tier?: "major" | "minor" | null }[],
) {
  const uniqueCompetencies = [
    ...new Set(competencies.map((competency) => competency.trim())),
  ].filter(Boolean);
  const normalizedUniqueCompetencies = uniqueCompetencies.map(normalizeLabel).sort();
  const normalizedExistingCompetencies = (existingCompetencies ?? [])
    .map((competency) => normalizeLabel(competency.name))
    .sort();

  if (
    existingCompetencies &&
    normalizedUniqueCompetencies.length === normalizedExistingCompetencies.length &&
    normalizedUniqueCompetencies.every(
      (competency, index) => competency === normalizedExistingCompetencies[index],
    )
  ) {
    return uniqueCompetencies.map((name) => {
      const existingCompetency = existingCompetencies.find(
        (competency) => normalizeLabel(competency.name) === normalizeLabel(name),
      );
      return {
        name,
        weight: existingCompetency?.weight ?? 1 / uniqueCompetencies.length,
        targetScore: existingCompetency?.targetScore ?? null,
        tier: existingCompetency?.tier ?? "major",
      };
    });
  }

  const weight = uniqueCompetencies.length > 0 ? 1 / uniqueCompetencies.length : 1;
  return uniqueCompetencies.map((name) => ({ name, weight, targetScore: null, tier: "major" as const }));
}

function parseBlueprintDescription(description: string) {
  const fields = {
    role: "",
    criticalSituation: "",
  };

  for (const line of description.split("\n")) {
    const separatorIndex = line.indexOf(":");
    if (separatorIndex < 0) {
      continue;
    }

    const label = normalizeLabel(line.slice(0, separatorIndex));
    const value = line.slice(separatorIndex + 1).trim();

    if (["cargo", "role", "puesto"].includes(label)) {
      fields.role = value;
    } else if (
      (label.includes("situa") && label.includes("cr")) ||
      label === "critical situation" ||
      label === "critical role situation"
    ) {
      fields.criticalSituation = value;
    }
  }

  return fields;
}

function normalizeLabel(value: string) {
  return value
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .trim()
    .toLowerCase();
}

function Header({ kicker, title, lede }: { kicker: string; title: string; lede: string }) {
  return (
    <div>
      <div className="text-xs uppercase tracking-[0.2em] text-foreground">{kicker}</div>
      <h1 className="mt-1 font-display text-3xl">{title}</h1>
      <p className="mt-2 max-w-2xl text-sm text-muted-foreground">{lede}</p>
    </div>
  );
}

function Card({
  title,
  children,
  tone,
  required,
  requiredLabel = "Obrigatório",
}: {
  title: string;
  children: React.ReactNode;
  tone?: "ok" | "danger";
  required?: boolean;
  requiredLabel?: string;
}) {
  const accent =
    tone === "ok"
      ? "before:bg-success"
      : tone === "danger"
        ? "before:bg-danger"
        : "before:bg-transparent";
  return (
    <section
      className={`relative overflow-hidden rounded-xl border border-border bg-card p-5 before:absolute before:left-0 before:top-0 before:h-full before:w-1 ${accent}`}
    >
      <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold text-foreground">
        {title}
        {required && (
          <span className="rounded-full border border-danger/40 bg-danger/10 px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide text-danger">
            {requiredLabel}
          </span>
        )}
      </h2>
      {children}
    </section>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium text-muted-foreground">{label}</span>
      {children}
    </label>
  );
}

function FieldMeta({ error, count, max }: { error?: string; count: number; max: number }) {
  return (
    <div className="mt-1 flex items-center justify-between gap-3 text-xs">
      <span className={error ? "text-danger" : "text-muted-foreground"}>{error ?? " "}</span>
      <span className="shrink-0 text-muted-foreground">
        {count}/{max}
      </span>
    </div>
  );
}

function competencyHint(label: string) {
  return `Competência "${label}": esta competência descreve o que será cobrado durante o teste.`;
}

function Help({ children }: { children: React.ReactNode }) {
  return <p className="mt-2 text-xs text-muted-foreground">{children}</p>;
}
