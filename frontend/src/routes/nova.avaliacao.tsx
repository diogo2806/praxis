import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  cloneSimulationVersionToDraft,
  createSimulationDraft,
  getSimulationVersion,
  updateSimulationBlueprint,
} from "@/lib/api/praxis";
import { useEmpresaConfig } from "@/lib/empresa-config";
import { useLanguage } from "@/lib/language-context";
import { canEditSimulationVersion } from "@/lib/simulation-meta";
import { getTranslations, type Language } from "@/lib/translations";

export const Route = createFileRoute("/nova/avaliacao")({
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
        { name: "description", content: copy.metaDescription },
      ],
    };
  },
  component: Page,
});

function getStoredLanguage(): Language {
  if (typeof window === "undefined") return "pt-BR";
  try {
    const stored = localStorage.getItem("praxis-language");
    if (stored === "pt-BR" || stored === "en" || stored === "es-MX") return stored;
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

  const competencies = useMemo(
    () => (config?.competencies ?? []).filter((competency) => competency.active !== false),
    [config?.competencies],
  );
  const catalogValues = useMemo(
    () => new Set(competencies.map((competency) => normalizeLabel(competency.value))),
    [competencies],
  );

  const [role, setRole] = useState("");
  const [criticalSituation, setCriticalSituation] = useState("");
  const [selectedCompetencies, setSelectedCompetencies] = useState<string[]>([]);
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [hydratedVersionKey, setHydratedVersionKey] = useState<string | null>(null);
  const [competencySearch, setCompetencySearch] = useState("");
  const roleMaxLength = 180;
  const criticalSituationMaxLength = 1200;

  const visibleCompetencies = useMemo(() => {
    const normalizedSearch = competencySearch.trim().toLowerCase();
    return [...competencies]
      .filter((competency) => competency.label.toLowerCase().includes(normalizedSearch))
      .sort((a, b) => a.label.localeCompare(b.label, "pt-BR", { sensitivity: "base" }));
  }, [competencies, competencySearch]);

  const selectedOutsideCatalog = useMemo(
    () =>
      selectedCompetencies.filter(
        (competency) => !catalogValues.has(normalizeLabel(competency)),
      ),
    [catalogValues, selectedCompetencies],
  );

  const missingFields = [
    role.trim().length === 0 && copy.missingRole,
    criticalSituation.trim().length === 0 && copy.missingCriticalSituation,
    selectedCompetencies.length === 0 && copy.missingCompetency,
  ].filter((field): field is string => Boolean(field));

  const canGoNext =
    role.trim().length > 0 &&
    criticalSituation.trim().length > 0 &&
    selectedCompetencies.length > 0;

  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasVersionContext,
  });

  const versionStatus = versionQuery.data?.status;
  const isEditable =
    !hasVersionContext || (versionStatus ? canEditSimulationVersion(versionStatus) : false);

  useEffect(() => {
    if (!versionQuery.data || empresaConfigLoading) return;
    const versionKey = `${versionQuery.data.simulationId}:${versionQuery.data.versionNumber}`;
    if (hydratedVersionKey === versionKey) return;

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
        rootNodeId: "turno-1",
        competencies: selectedCompetencies,
        criticalSituation: criticalSituation.trim(),
      }),
    onSuccess: (simulation) => {
      void navigate({
        to: "/nova/personagem",
        search: { simulationId: simulation.id, versionNumber: simulation.versionNumber },
      });
    },
  });

  const updateExistingMutation = useMutation({
    mutationFn: () => {
      if (!isEditable) {
        throw new Error("Esta versão não pode ser editada. Crie um rascunho antes de alterar.");
      }
      return updateSimulationBlueprint(search.simulationId!, search.versionNumber!, {
        rootNodeId: versionQuery.data?.blueprint.rootNodeId ?? "turno-1",
        competencies: buildCompetencyWeights(
          selectedCompetencies,
          versionQuery.data?.blueprint.competencies,
        ),
        criticalSituation: criticalSituation.trim(),
      });
    },
    onSuccess: async (simulation) => {
      await queryClient.invalidateQueries({
        queryKey: ["simulation-version", search.simulationId, search.versionNumber],
      });
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
      void navigate({
        to: "/nova/personagem",
        search: { simulationId: simulation.id, versionNumber: simulation.versionNumber },
      });
    },
  });

  const cloneDraftMutation = useMutation({
    mutationFn: () => cloneSimulationVersionToDraft(search.simulationId!, search.versionNumber!),
    onSuccess: async (draft) => {
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
      void navigate({
        to: "/nova/avaliacao",
        search: { simulationId: draft.simulationId, versionNumber: draft.newVersionNumber },
      });
    },
  });

  function toggleCompetency(competency: string) {
    if (!isEditable) return;
    setSelectedCompetencies((current) =>
      current.includes(competency)
        ? current.filter((item) => item !== competency)
        : [...current, competency],
    );
  }

  function removeLegacyCompetency(competency: string) {
    if (!isEditable) return;
    setSelectedCompetencies((current) => current.filter((item) => item !== competency));
  }

  const pending =
    createDraftMutation.isPending ||
    updateExistingMutation.isPending ||
    cloneDraftMutation.isPending;

  return (
    <AppShell>
      <WizardStepper
        current="avaliacao"
        unlockedThrough={isEditable && canGoNext ? "cenario" : "avaliacao"}
      />

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

      {cloneDraftMutation.isError && (
        <div className="mb-5">
          <StateBanner tone="danger" title="Não foi possível criar o rascunho">
            {cloneDraftMutation.error instanceof Error
              ? cloneDraftMutation.error.message
              : "Tente novamente."}
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
          <Header kicker={copy.kicker} title={copy.heading} lede={copy.lede} />

          {versionStatus && !isEditable && (
            <StateBanner
              tone="warn"
              title="Esta versão não pode ser editada"
              action={
                versionStatus === "published" ? (
                  <button
                    type="button"
                    onClick={() => cloneDraftMutation.mutate()}
                    disabled={cloneDraftMutation.isPending}
                    className="shrink-0 rounded-md border border-current/20 bg-background/70 px-3 py-2 text-xs font-medium hover:bg-background disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {cloneDraftMutation.isPending ? "Criando..." : "Criar rascunho"}
                  </button>
                ) : undefined
              }
            >
              {versionStatus === "published"
                ? "A versão no ar permanece protegida. Crie um rascunho para alterar o plano sem afetar candidatos em andamento."
                : "Esta versão está arquivada e não pode receber alterações."}
            </StateBanner>
          )}

          <p className="text-sm text-muted-foreground">
            Prefere começar de um modelo pronto?{" "}
            <Link to="/nova/rapido" className="font-medium text-primary hover:underline">
              Usar um modelo →
            </Link>{" "}
            · Quer consultar o que já existe?{" "}
            <Link to="/avaliacoes" className="font-medium text-primary hover:underline">
              Ver avaliações cadastradas →
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
                disabled={!isEditable}
                onChange={(event) => setRole(event.target.value)}
              />
              <FieldMeta
                error={submitAttempted && role.trim().length === 0 ? copy.roleRequiredError : undefined}
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
              disabled={!isEditable}
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
            <div className="mb-4 flex flex-col gap-3 rounded-lg border border-border bg-muted/30 p-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="text-sm font-medium text-foreground">Catálogo centralizado</div>
                <p className="mt-1 text-xs leading-5 text-muted-foreground">
                  Nesta etapa você apenas seleciona competências ativas. Criação, edição e inativação ficam exclusivamente na tela Competências.
                </p>
              </div>
              <Link
                to="/competencias"
                className="shrink-0 rounded-md border border-border bg-card px-3 py-2 text-xs font-medium text-primary hover:bg-accent"
              >
                Gerenciar catálogo
              </Link>
            </div>

            <div className="mb-3">
              <input
                type="search"
                className="input"
                placeholder={copy.searchCompetencyPlaceholder}
                value={competencySearch}
                onChange={(event) => setCompetencySearch(event.target.value)}
                disabled={!isEditable || empresaConfigLoading}
              />
              <div className="mt-1 text-xs text-muted-foreground">
                {copy.competenciesAvailable
                  .replace("{visible}", String(visibleCompetencies.length))
                  .replace("{total}", String(competencies.length))}
              </div>
            </div>

            <div className="flex flex-wrap gap-2">
              {visibleCompetencies.length === 0 ? (
                <p className="text-sm text-muted-foreground">{copy.noCompetencyFound}</p>
              ) : (
                visibleCompetencies.map((competency) => (
                  <label
                    key={competency.value}
                    title={competencyHint(competency.label, copy.competencyHintTemplate)}
                    className={`rounded-full border px-3 py-1.5 text-sm ${
                      isEditable ? "cursor-pointer" : "cursor-not-allowed opacity-70"
                    } ${
                      selectedCompetencies.includes(competency.value)
                        ? "border-primary bg-primary/10 text-foreground"
                        : "border-border bg-card text-foreground/75 hover:bg-accent"
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={selectedCompetencies.includes(competency.value)}
                      disabled={!isEditable}
                      onChange={() => toggleCompetency(competency.value)}
                      className="sr-only"
                    />
                    {competency.label}
                  </label>
                ))
              )}
            </div>

            {selectedOutsideCatalog.length > 0 && (
              <div className="mt-4 rounded-lg border border-warning/40 bg-warning/10 p-4">
                <div className="text-sm font-medium text-foreground">
                  Competências antigas fora do catálogo ativo
                </div>
                <p className="mt-1 text-xs leading-5 text-muted-foreground">
                  Elas permanecem na versão para preservar o histórico. Remova-as do rascunho ou cadastre uma competência equivalente na Biblioteca de competências.
                </p>
                <div className="mt-3 flex flex-wrap gap-2">
                  {selectedOutsideCatalog.map((competency) => (
                    <span
                      key={competency}
                      className="inline-flex items-center gap-2 rounded-full border border-warning/40 bg-background px-3 py-1.5 text-sm"
                    >
                      {competency}
                      {isEditable && (
                        <button
                          type="button"
                          onClick={() => removeLegacyCompetency(competency)}
                          aria-label={`Remover ${competency}`}
                          className="rounded-full px-1 text-muted-foreground hover:bg-accent hover:text-foreground"
                        >
                          ×
                        </button>
                      )}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </Card>

          <div className="sticky bottom-0 -mx-6 mt-2 flex flex-col gap-3 border-t border-border bg-background/90 px-6 py-4 backdrop-blur sm:flex-row sm:items-center sm:justify-between lg:-mx-10 lg:px-10">
            <Link
              to="/"
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              {copy.cancel}
            </Link>

            <div className="flex flex-col items-start gap-2 sm:items-end">
              {isEditable && !canGoNext && (
                <p
                  className={`text-xs ${submitAttempted ? "text-danger" : "text-muted-foreground"}`}
                  aria-live="polite"
                >
                  {copy.missingFieldsPrefix} {missingFields.join(", ")}.
                </p>
              )}

              <button
                type="button"
                disabled={pending || (!isEditable && versionStatus !== "published")}
                title={
                  !isEditable
                    ? versionStatus === "published"
                      ? "Criar uma nova versão em rascunho"
                      : "Esta versão não pode ser editada"
                    : canGoNext
                      ? copy.createDraftTitle
                      : copy.disabledNextTitle
                }
                onClick={() => {
                  if (!isEditable && versionStatus === "published" && hasVersionContext) {
                    cloneDraftMutation.mutate();
                    return;
                  }
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
                aria-disabled={isEditable && !canGoNext}
                className={`rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 ${
                  (isEditable && !canGoNext) || pending || (!isEditable && versionStatus !== "published")
                    ? "cursor-not-allowed opacity-50"
                    : ""
                }`}
              >
                {!isEditable
                  ? versionStatus === "published"
                    ? cloneDraftMutation.isPending
                      ? "Criando rascunho..."
                      : "Criar rascunho"
                    : "Versão não editável"
                  : createDraftMutation.isPending || updateExistingMutation.isPending
                    ? copy.creatingDraft
                    : copy.next}
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
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
  labels: { role: string; criticalSituation: string; competencies: string };
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
  existingCompetencies?: {
    name: string;
    weight: number;
    targetScore?: number | null;
    tier?: "major" | "minor" | null;
  }[],
) {
  const uniqueCompetencies = [...new Set(competencies.map((competency) => competency.trim()))].filter(Boolean);
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
  return uniqueCompetencies.map((name) => ({
    name,
    weight,
    targetScore: null,
    tier: "major" as const,
  }));
}

function parseBlueprintDescription(description: string) {
  const fields = { role: "", criticalSituation: "" };
  for (const line of description.split("\n")) {
    const separatorIndex = line.indexOf(":");
    if (separatorIndex < 0) continue;
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

function competencyHint(label: string, template: string) {
  return template.replace("{label}", label);
}

function Help({ children }: { children: React.ReactNode }) {
  return <p className="mt-2 text-xs text-muted-foreground">{children}</p>;
}
