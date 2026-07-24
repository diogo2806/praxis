import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { AlertTriangle, Download, FlaskConical, Scale, ShieldCheck, Target } from "lucide-react";
import { useMemo, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  downloadAssessmentQualityReport,
  getAssessmentQualityReport,
  getSensitiveQualityReport,
  saveExternalCriterion,
  type AssessmentQualityReport,
  type CriterionType,
  type QualityFilters,
} from "@/lib/api/assessment-quality";

export const Route = createFileRoute("/resultados/qualidade")({
  head: () => ({
    meta: [
      { title: "Qualidade psicométrica - Práxis" },
      {
        name: "description",
        content:
          "Analise distribuição, caminhos, alternativas, precisão por competência, critérios externos e diferenças protegidas por amostra mínima.",
      },
    ],
  }),
  component: AssessmentQualityPage,
});

function AssessmentQualityPage() {
  const queryClient = useQueryClient();
  const [simulationId, setSimulationId] = useState("");
  const [versionNumber, setVersionNumber] = useState("");
  const [gupyJobId, setGupyJobId] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [activeFilters, setActiveFilters] = useState<QualityFilters | null>(null);
  const [criterion, setCriterion] = useState({
    candidateAttemptId: "",
    criterionCode: "",
    criterionLabel: "",
    criterionType: "NUMERIC" as CriterionType,
    numericValue: "",
    categoryValue: "",
  });
  const [sensitive, setSensitive] = useState({
    groupCriterionCode: "",
    purpose: "",
    legalBasis: "",
    minimumSample: 10,
  });
  const [sensitiveReport, setSensitiveReport] = useState<AssessmentQualityReport | null>(null);

  const reportQuery = useQuery({
    queryKey: ["assessment-quality", activeFilters],
    queryFn: () => getAssessmentQualityReport(activeFilters!),
    enabled: activeFilters !== null,
  });

  const saveCriterionMutation = useMutation({
    mutationFn: () =>
      saveExternalCriterion({
        candidateAttemptId: criterion.candidateAttemptId,
        criterionCode: criterion.criterionCode,
        criterionLabel: criterion.criterionLabel,
        criterionType: criterion.criterionType,
        numericValue:
          criterion.criterionType === "NUMERIC" && criterion.numericValue !== ""
            ? Number(criterion.numericValue)
            : undefined,
        categoryValue:
          criterion.criterionType === "CATEGORY" ? criterion.categoryValue : undefined,
      }),
    onSuccess: () => {
      setCriterion((current) => ({
        ...current,
        candidateAttemptId: "",
        numericValue: "",
        categoryValue: "",
      }));
      void queryClient.invalidateQueries({ queryKey: ["assessment-quality"] });
    },
  });

  const sensitiveMutation = useMutation({
    mutationFn: () => getSensitiveQualityReport(activeFilters!, sensitive),
    onSuccess: setSensitiveReport,
  });

  const report = sensitiveReport ?? reportQuery.data ?? null;
  const filters = useMemo(
    () => ({
      simulationId: simulationId.trim(),
      versionNumber: versionNumber ? Number(versionNumber) : undefined,
      gupyJobId: gupyJobId ? Number(gupyJobId) : undefined,
      from: from || undefined,
      to: to || undefined,
    }),
    [simulationId, versionNumber, gupyJobId, from, to],
  );

  function applyFilters() {
    setSensitiveReport(null);
    setActiveFilters(filters);
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Evidência observada, estimativas e recomendações separadas
            </div>
            <h1 className="mt-1 font-display text-3xl">Qualidade psicométrica e justiça</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Avalie funcionamento do teste por versão e contexto. O painel não recomenda contratação,
              não mistura empresas e suprime grupos pequenos para reduzir risco de identificação.
            </p>
          </div>
          <Button
            variant="outline"
            disabled={!activeFilters}
            onClick={() => activeFilters && downloadAssessmentQualityReport(activeFilters)}
          >
            <Download className="mr-2 h-4 w-4" /> Relatório técnico CSV
          </Button>
        </header>

        <section className="rounded-xl border border-border bg-card p-5">
          <h2 className="font-semibold">Filtros de comparabilidade</h2>
          <div className="mt-4 grid gap-3 md:grid-cols-5">
            <Field label="ID da avaliação">
              <input className="input" value={simulationId} onChange={(event) => setSimulationId(event.target.value)} />
            </Field>
            <Field label="Versão">
              <input className="input" type="number" min={1} value={versionNumber} onChange={(event) => setVersionNumber(event.target.value)} />
            </Field>
            <Field label="ID da vaga Gupy">
              <input className="input" type="number" min={1} value={gupyJobId} onChange={(event) => setGupyJobId(event.target.value)} />
            </Field>
            <Field label="Início">
              <input className="input" type="datetime-local" value={from} onChange={(event) => setFrom(event.target.value)} />
            </Field>
            <Field label="Fim">
              <input className="input" type="datetime-local" value={to} onChange={(event) => setTo(event.target.value)} />
            </Field>
          </div>
          <Button className="mt-4" disabled={!filters.simulationId || reportQuery.isFetching} onClick={applyFilters}>
            Calcular indicadores
          </Button>
        </section>

        {(reportQuery.error || saveCriterionMutation.error || sensitiveMutation.error) && (
          <StateBanner tone="danger" title="Não foi possível concluir a operação">
            {String(reportQuery.error ?? saveCriterionMutation.error ?? sensitiveMutation.error)}
          </StateBanner>
        )}

        {report?.warnings.map((warning) => (
          <StateBanner key={warning} tone="warning" title="Limitação da amostra">
            {warning}
          </StateBanner>
        ))}

        {report && <Summary report={report} />}

        {report && (
          <section className="grid gap-6 xl:grid-cols-2">
            <Panel title="Distribuição de notas" icon={<FlaskConical className="h-5 w-5 text-primary" />}>
              <div className="space-y-2">
                {report.scoreDistribution.map((bucket) => (
                  <div key={`${bucket.fromInclusive}-${bucket.toInclusive}`} className="grid grid-cols-[90px_1fr_80px] items-center gap-3 text-sm">
                    <span>{bucket.fromInclusive}–{bucket.toInclusive}</span>
                    <div className="h-2 overflow-hidden rounded bg-muted">
                      <div className="h-full bg-primary" style={{ width: `${Math.min(100, bucket.percentage)}%` }} />
                    </div>
                    <span className="text-right text-muted-foreground">{bucket.count} ({bucket.percentage}%)</span>
                  </div>
                ))}
              </div>
            </Panel>
            <Panel title="Recomendações metodológicas" icon={<Target className="h-5 w-5 text-primary" />}>
              {report.recommendations.length === 0 ? (
                <Empty text="Nenhum alerta metodológico foi gerado na amostra atual." />
              ) : (
                <ul className="space-y-3">
                  {report.recommendations.map((item, index) => (
                    <li key={`${item.code}-${index}`} className="rounded-lg border border-border p-3 text-sm">
                      <div className="font-semibold">{item.title}</div>
                      <div className="mt-1 text-muted-foreground">{item.detail}</div>
                      <Evidence value={item.evidenceType} />
                    </li>
                  ))}
                </ul>
              )}
            </Panel>
          </section>
        )}

        {report && (
          <>
            <DataTable
              title="Alternativas"
              headers={["Cenário", "Alternativa", "Escolhas", "Frequência", "Média", "Diferença", "Diagnóstico"]}
              rows={report.alternatives.map((item) => [
                item.nodeId,
                item.optionId,
                item.selectedCount,
                `${item.selectionPercent}%`,
                format(item.averageFinalScore),
                format(item.discriminationDifference),
                item.diagnostic,
              ])}
            />
            <DataTable
              title="Caminhos percorridos"
              headers={["Caminho", "Amostra", "Frequência", "Média", "Tempo médio"]}
              rows={report.paths.map((item) => [
                item.nodeIds.join(" → "),
                item.count,
                `${item.frequencyPercent}%`,
                format(item.averageScore),
                seconds(item.averageDurationSeconds),
              ])}
            />
            <DataTable
              title="Cenários"
              headers={["Cenário", "Exibições", "Respostas", "Timeouts", "Tempo médio", "Pausas de vídeo"]}
              rows={report.scenarios.map((item) => [
                item.nodeId,
                item.presentations,
                item.answers,
                item.timeouts,
                seconds(item.averageResponseSeconds),
                item.videoPauseEvents,
              ])}
            />
            <DataTable
              title="Precisão por competência"
              headers={["Competência", "Amostra", "Média", "Desvio", "Erro-padrão", "IC 95%", "Precisão"]}
              rows={report.competencies.map((item) => [
                item.competency,
                item.sampleSize,
                format(item.mean),
                format(item.standardDeviation),
                format(item.standardError),
                `${format(item.confidenceLow95)}–${format(item.confidenceHigh95)}`,
                item.precisionLevel,
              ])}
            />
            <ExternalRelations report={report} />
          </>
        )}

        <section className="grid gap-6 xl:grid-cols-2">
          <Panel title="Cadastrar critério externo" icon={<Scale className="h-5 w-5 text-primary" />}>
            <p className="mb-4 text-sm text-muted-foreground">
              Relacione a avaliação a entrevista estruturada, aprovação, desempenho ou retenção. O valor
              fica vinculado à participação e à empresa atual.
            </p>
            <div className="grid gap-3 sm:grid-cols-2">
              <Field label="ID da participação"><input className="input" value={criterion.candidateAttemptId} onChange={(event) => setCriterion({ ...criterion, candidateAttemptId: event.target.value })} /></Field>
              <Field label="Código do critério"><input className="input" value={criterion.criterionCode} onChange={(event) => setCriterion({ ...criterion, criterionCode: event.target.value })} /></Field>
              <Field label="Nome do critério"><input className="input" value={criterion.criterionLabel} onChange={(event) => setCriterion({ ...criterion, criterionLabel: event.target.value })} /></Field>
              <Field label="Tipo">
                <select className="input" value={criterion.criterionType} onChange={(event) => setCriterion({ ...criterion, criterionType: event.target.value as CriterionType })}>
                  <option value="NUMERIC">Numérico</option>
                  <option value="CATEGORY">Categórico</option>
                </select>
              </Field>
              {criterion.criterionType === "NUMERIC" ? (
                <Field label="Valor numérico"><input className="input" type="number" step="0.01" value={criterion.numericValue} onChange={(event) => setCriterion({ ...criterion, numericValue: event.target.value })} /></Field>
              ) : (
                <Field label="Categoria"><input className="input" value={criterion.categoryValue} onChange={(event) => setCriterion({ ...criterion, categoryValue: event.target.value })} /></Field>
              )}
            </div>
            <Button className="mt-4" disabled={saveCriterionMutation.isPending || !criterion.candidateAttemptId || !criterion.criterionCode || !criterion.criterionLabel} onClick={() => saveCriterionMutation.mutate()}>
              Salvar critério externo
            </Button>
          </Panel>

          <Panel title="Análise protegida entre grupos" icon={<ShieldCheck className="h-5 w-5 text-primary" />}>
            <p className="mb-4 text-sm text-muted-foreground">
              Disponível somente com finalidade definida, base legal registrada e amostra mínima. Grupos
              abaixo do limite são suprimidos e a consulta fica auditada.
            </p>
            <div className="space-y-3">
              <Field label="Código do critério categórico"><input className="input" value={sensitive.groupCriterionCode} onChange={(event) => setSensitive({ ...sensitive, groupCriterionCode: event.target.value })} /></Field>
              <Field label="Finalidade"><textarea className="min-h-20 w-full rounded-md border border-input bg-background px-3 py-2" value={sensitive.purpose} onChange={(event) => setSensitive({ ...sensitive, purpose: event.target.value })} /></Field>
              <Field label="Base legal"><input className="input" value={sensitive.legalBasis} onChange={(event) => setSensitive({ ...sensitive, legalBasis: event.target.value })} /></Field>
              <Field label="Amostra mínima por grupo"><input className="input" type="number" min={10} max={500} value={sensitive.minimumSample} onChange={(event) => setSensitive({ ...sensitive, minimumSample: Number(event.target.value) })} /></Field>
            </div>
            <Button className="mt-4" variant="outline" disabled={!activeFilters || sensitiveMutation.isPending || !sensitive.groupCriterionCode || !sensitive.purpose || !sensitive.legalBasis} onClick={() => sensitiveMutation.mutate()}>
              Executar e auditar análise
            </Button>
            {report?.sensitiveAnalysis && (
              <div className="mt-4 rounded-lg border border-border p-3 text-sm">
                <div className="font-semibold">Consulta auditada: {report.sensitiveAnalysis.auditId}</div>
                <div className="mt-2 text-muted-foreground">Grupos suprimidos: {report.sensitiveAnalysis.suppressedGroups}</div>
                <ul className="mt-3 space-y-1">
                  {report.sensitiveAnalysis.groups.map((group) => (
                    <li key={group.category}>{group.category}: {group.suppressed ? "suprimido" : `${group.sampleSize} casos, média ${format(group.averageScore)}`}</li>
                  ))}
                </ul>
              </div>
            )}
          </Panel>
        </section>

        {report && (
          <Panel title="Metodologia e limitações" icon={<AlertTriangle className="h-5 w-5 text-primary" />}>
            <dl className="grid gap-3 text-sm md:grid-cols-2">
              <Description label="Percentis" value={report.methodology.percentileMethod} />
              <Description label="Precisão" value={report.methodology.precisionMethod} />
              <Description label="Discriminação" value={report.methodology.discriminationMethod} />
              <Description label="Escala" value={report.methodology.scoreScale} />
            </dl>
            <ul className="mt-4 list-disc space-y-1 pl-5 text-sm text-muted-foreground">
              {report.methodology.limitations.map((item) => <li key={item}>{item}</li>)}
            </ul>
          </Panel>
        )}
      </main>
    </AppShell>
  );
}

function Summary({ report }: { report: AssessmentQualityReport }) {
  const observed = report.observed;
  return (
    <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-6">
      <Metric label="Amostra" value={String(observed.sampleSize)} />
      <Metric label="Conclusão" value={`${observed.completionRatePercent}%`} />
      <Metric label="Média" value={format(observed.meanScore)} />
      <Metric label="Mediana" value={format(observed.median)} />
      <Metric label="Tempo médio" value={seconds(observed.averageDurationSeconds)} />
      <Metric label="Pausas" value={String(observed.pauseEvents)} />
    </section>
  );
}

function ExternalRelations({ report }: { report: AssessmentQualityReport }) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <h2 className="font-semibold">Relação com critérios externos</h2>
      {report.externalCriteria.length === 0 ? <Empty text="Nenhum critério externo cadastrado para a amostra." /> : (
        <div className="mt-4 grid gap-4 lg:grid-cols-2">
          {report.externalCriteria.map((item) => (
            <article key={item.criterionCode} className="rounded-lg border border-border p-4 text-sm">
              <div className="font-semibold">{item.criterionLabel}</div>
              <div className="mt-1 text-muted-foreground">{item.criterionCode} · n={item.sampleSize}</div>
              {item.pearsonCorrelation !== null && <div className="mt-3 text-lg font-semibold">r = {item.pearsonCorrelation}</div>}
              {item.categoryMeans.length > 0 && (
                <ul className="mt-3 space-y-1">
                  {item.categoryMeans.map((category) => <li key={category.category}>{category.category}: {category.suppressed ? "suprimido" : `${category.sampleSize} casos, média ${format(category.averageScore)}`}</li>)}
                </ul>
              )}
              <p className="mt-3 text-muted-foreground">{item.interpretation}</p>
              <Evidence value={item.evidenceType} />
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function DataTable({ title, headers, rows }: { title: string; headers: string[]; rows: Array<Array<string | number>> }) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <h2 className="font-semibold">{title}</h2>
      {rows.length === 0 ? <Empty text="Sem dados para os filtros selecionados." /> : (
        <div className="mt-4 overflow-auto rounded-lg border border-border">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead className="bg-muted/50"><tr>{headers.map((header) => <th key={header} className="p-3 font-semibold">{header}</th>)}</tr></thead>
            <tbody>{rows.map((row, rowIndex) => <tr key={`${title}-${rowIndex}`} className="border-t border-border">{row.map((cell, cellIndex) => <td key={`${rowIndex}-${cellIndex}`} className="p-3 align-top">{cell}</td>)}</tr>)}</tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function Panel({ title, icon, children }: { title: string; icon: React.ReactNode; children: React.ReactNode }) {
  return <section className="rounded-xl border border-border bg-card p-5"><div className="mb-4 flex items-center gap-2">{icon}<h2 className="font-semibold">{title}</h2></div>{children}</section>;
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return <label className="block text-sm font-medium"><span className="mb-1 block">{label}</span>{children}</label>;
}

function Metric({ label, value }: { label: string; value: string }) {
  return <div className="rounded-xl border border-border bg-card p-4"><div className="text-xs uppercase tracking-wide text-muted-foreground">{label}</div><div className="mt-1 text-2xl font-semibold">{value}</div></div>;
}

function Description({ label, value }: { label: string; value: string }) {
  return <div><dt className="font-semibold">{label}</dt><dd className="mt-1 text-muted-foreground">{value}</dd></div>;
}

function Evidence({ value }: { value: string }) {
  return <div className="mt-2 text-[11px] font-semibold uppercase tracking-wide text-primary">{value.replaceAll("_", " ")}</div>;
}

function Empty({ text }: { text: string }) {
  return <p className="mt-4 text-sm text-muted-foreground">{text}</p>;
}

function format(value: number | null | undefined) {
  return value === null || value === undefined ? "—" : value.toLocaleString("pt-BR", { maximumFractionDigits: 2 });
}

function seconds(value: number | null | undefined) {
  return value === null || value === undefined ? "—" : `${format(value)} s`;
}
