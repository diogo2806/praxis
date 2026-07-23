import { useMutation } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { CheckCircle2, Download, FileJson, ShieldCheck, Upload } from "lucide-react";
import { type ChangeEvent, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  exportAssessmentPackage,
  importAssessmentPackage,
  type AssessmentPackageEnvelope,
  type ImportPackageResponse,
  type PackageValidationProblem,
  type PackageValidationResponse,
  validateAssessmentPackage,
} from "@/lib/api/assessment-portability";

export const Route = createFileRoute("/nova/portabilidade")({
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
      { title: "Portabilidade de avaliações - Práxis" },
      {
        name: "description",
        content:
          "Exporte, valide e importe pacotes de avaliação versionados sem transportar candidatos, resultados ou segredos.",
      },
    ],
  }),
  component: AssessmentPortabilityPage,
});

function AssessmentPortabilityPage() {
  const search = Route.useSearch();
  const [packageEnvelope, setPackageEnvelope] = useState<AssessmentPackageEnvelope | null>(null);
  const [fileName, setFileName] = useState("");
  const [parseError, setParseError] = useState("");
  const [newAssessmentName, setNewAssessmentName] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [confirmCompetencies, setConfirmCompetencies] = useState(false);
  const [validation, setValidation] = useState<PackageValidationResponse | null>(null);
  const [imported, setImported] = useState<ImportPackageResponse | null>(null);

  const exportMutation = useMutation({
    mutationFn: () => exportAssessmentPackage(search.simulationId!, search.versionNumber!),
  });
  const validateMutation = useMutation({
    mutationFn: () => validateAssessmentPackage(packageEnvelope!),
    onSuccess: (result) => {
      setValidation(result);
      setImported(null);
      if (!newAssessmentName.trim() && packageEnvelope) {
        setNewAssessmentName(`${packageEnvelope.manifest.assessment.name} importada`);
      }
    },
  });
  const importMutation = useMutation({
    mutationFn: () =>
      importAssessmentPackage({
        packageEnvelope: packageEnvelope!,
        newAssessmentName: newAssessmentName.trim(),
        confirmed,
        confirmCompetencies,
      }),
    onSuccess: setImported,
  });

  const operationError = exportMutation.error ?? validateMutation.error ?? importMutation.error;
  const exportContext = Boolean(search.simulationId && search.versionNumber);

  async function handleFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    setValidation(null);
    setImported(null);
    setParseError("");
    setPackageEnvelope(null);
    setFileName(file?.name ?? "");
    if (!file) return;
    try {
      const parsed = JSON.parse(await file.text()) as AssessmentPackageEnvelope;
      if (!parsed || typeof parsed !== "object" || !parsed.manifest || !parsed.contentHash) {
        throw new Error("O JSON não possui envelope, manifesto e hash no formato esperado.");
      }
      setPackageEnvelope(parsed);
      setNewAssessmentName(`${parsed.manifest.assessment.name} importada`);
    } catch (error) {
      setParseError(error instanceof Error ? error.message : "Não foi possível interpretar o arquivo JSON.");
    }
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-6xl space-y-6">
        <header className="max-w-3xl">
          <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
            Backup lógico e intercâmbio controlado
          </div>
          <h1 className="mt-1 font-display text-3xl">Portabilidade de avaliações</h1>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            O pacote contém somente conteúdo autoral versionado. Candidatos, participações, respostas,
            resultados, tokens e credenciais não fazem parte do contrato.
          </p>
        </header>

        {operationError && <ErrorBanner error={operationError} />}
        {parseError && (
          <StateBanner tone="danger" title="Arquivo inválido">
            {parseError}
          </StateBanner>
        )}

        <section className="grid gap-6 lg:grid-cols-2">
          <Card icon={<Download className="h-5 w-5 text-primary" />} title="Exportar versão">
            {exportContext ? (
              <>
                <p className="text-sm leading-6 text-muted-foreground">
                  Avaliação <strong>{search.simulationId}</strong>, versão {search.versionNumber}. O
                  arquivo inclui manifesto, origem, grafo, competências, pontuação, acessibilidade,
                  referências de mídia e hash SHA-256.
                </p>
                <Button onClick={() => exportMutation.mutate()} disabled={exportMutation.isPending}>
                  <Download className="mr-2 h-4 w-4" />
                  Exportar pacote JSON
                </Button>
              </>
            ) : (
              <StateBanner tone="warning" title="Selecione uma avaliação">
                Abra esta tela a partir de uma versão da Central de Avaliações para exportar o pacote.
                <div className="mt-3">
                  <Button asChild variant="outline">
                    <Link to="/avaliacoes">Ir para Avaliações</Link>
                  </Button>
                </div>
              </StateBanner>
            )}
          </Card>

          <Card icon={<Upload className="h-5 w-5 text-primary" />} title="Selecionar pacote">
            <label className="block space-y-2 text-sm">
              <span className="font-medium">Arquivo JSON do Práxis</span>
              <input
                type="file"
                accept="application/json,.json"
                onChange={(event) => void handleFile(event)}
                className="block w-full rounded-md border border-input bg-background px-3 py-2 file:mr-4 file:rounded file:border-0 file:bg-primary/10 file:px-3 file:py-2 file:text-sm file:font-semibold file:text-primary"
              />
            </label>
            {fileName && (
              <div className="rounded-lg border border-border bg-background p-3 text-sm">
                <div className="font-semibold">{fileName}</div>
                {packageEnvelope && (
                  <div className="mt-1 text-muted-foreground">
                    {packageEnvelope.formatVersion} · origem {packageEnvelope.manifest.origin.sourceSystem}
                    /{packageEnvelope.manifest.origin.sourceAssessmentId} v
                    {packageEnvelope.manifest.origin.sourceVersionNumber}
                  </div>
                )}
              </div>
            )}
            <Button
              onClick={() => validateMutation.mutate()}
              disabled={!packageEnvelope || validateMutation.isPending}
            >
              <ShieldCheck className="mr-2 h-4 w-4" />
              Validar sem gravar
            </Button>
          </Card>
        </section>

        {validation && <ValidationResult validation={validation} />}

        {validation?.valid && packageEnvelope && (
          <Card icon={<FileJson className="h-5 w-5 text-primary" />} title="Confirmar importação">
            <label className="block space-y-2 text-sm">
              <span className="font-medium">Nome da nova avaliação</span>
              <input
                value={newAssessmentName}
                maxLength={180}
                onChange={(event) => setNewAssessmentName(event.target.value)}
                className="w-full rounded-md border border-input bg-background px-3 py-2"
              />
            </label>
            <label className="flex items-start gap-3 text-sm">
              <input
                type="checkbox"
                checked={confirmCompetencies}
                onChange={(event) => setConfirmCompetencies(event.target.checked)}
                className="mt-1"
              />
              <span>
                Confirmo a criação ou equivalência das competências: {validation.competenciesRequiringConfirmation.join(", ") || "nenhuma pendência"}.
              </span>
            </label>
            <label className="flex items-start gap-3 text-sm">
              <input
                type="checkbox"
                checked={confirmed}
                onChange={(event) => setConfirmed(event.target.checked)}
                className="mt-1"
              />
              <span>
                Revisei o diagnóstico e autorizo a criação de uma nova avaliação independente em
                rascunho. A origem não será alterada.
              </span>
            </label>
            <Button
              onClick={() => importMutation.mutate()}
              disabled={
                importMutation.isPending ||
                !newAssessmentName.trim() ||
                !confirmed ||
                !confirmCompetencies
              }
            >
              <Upload className="mr-2 h-4 w-4" />
              Importar como rascunho
            </Button>
          </Card>
        )}

        {imported && (
          <StateBanner tone="ok" title="Avaliação importada">
            <div className="space-y-2">
              <p>
                Criada como <strong>{imported.simulationId}</strong>, versão {imported.versionNumber},
                estado {imported.status}. Hash validado: {imported.packageHash}.
              </p>
              <Button asChild variant="outline">
                <Link to="/avaliacoes">Abrir Central de Avaliações</Link>
              </Button>
            </div>
          </StateBanner>
        )}
      </main>
    </AppShell>
  );
}

function ValidationResult({ validation }: { validation: PackageValidationResponse }) {
  return (
    <section className="space-y-4 rounded-xl border border-border bg-card p-5">
      <div className="flex items-start gap-3">
        {validation.valid ? (
          <CheckCircle2 className="mt-0.5 h-5 w-5 text-emerald-600" />
        ) : (
          <ShieldCheck className="mt-0.5 h-5 w-5 text-destructive" />
        )}
        <div>
          <h2 className="font-semibold">
            {validation.valid ? "Pacote íntegro e importável" : "Pacote rejeitado"}
          </h2>
          <p className="mt-1 break-all text-xs text-muted-foreground">
            Hash calculado: {validation.calculatedHash || "indisponível"}
          </p>
        </div>
      </div>
      <ProblemList title="Erros" problems={validation.errors} empty="Nenhum erro encontrado." />
      <ProblemList title="Avisos" problems={validation.warnings} empty="Nenhum aviso encontrado." />
      <div>
        <h3 className="text-sm font-semibold">Remapeamento planejado</h3>
        <dl className="mt-2 space-y-2 text-sm">
          {Object.entries(validation.plannedIdMapping).map(([source, target]) => (
            <div key={source} className="rounded border border-border bg-background p-2">
              <dt className="break-all text-muted-foreground">{source}</dt>
              <dd className="break-all font-medium">{target}</dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}

function ProblemList({
  title,
  problems,
  empty,
}: {
  title: string;
  problems: PackageValidationProblem[];
  empty: string;
}) {
  return (
    <div>
      <h3 className="text-sm font-semibold">{title}</h3>
      {problems.length === 0 ? (
        <p className="mt-1 text-sm text-muted-foreground">{empty}</p>
      ) : (
        <ul className="mt-2 space-y-2">
          {problems.map((problem, index) => (
            <li key={`${problem.path}-${problem.code}-${index}`} className="rounded border border-border bg-background p-3 text-sm">
              <div className="font-semibold">{problem.code}</div>
              <div className="mt-1 break-all text-xs text-muted-foreground">{problem.path}</div>
              <p className="mt-1">{problem.message}</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Card({
  icon,
  title,
  children,
}: {
  icon: React.ReactNode;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-center gap-3">
        {icon}
        <h2 className="font-semibold">{title}</h2>
      </div>
      <div className="mt-4 space-y-4">{children}</div>
    </section>
  );
}

function ErrorBanner({ error }: { error: unknown }) {
  return (
    <StateBanner tone="danger" title="Não foi possível concluir a operação">
      {error instanceof Error ? error.message : "Tente novamente."}
    </StateBanner>
  );
}
