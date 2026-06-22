import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Scale, ShieldCheck } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ComplianceScope } from "@/components/compliance-scope";
import { StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Sheet, SheetClose, SheetContent, SheetTitle } from "@/components/ui/sheet";
import { useNavigate } from "@tanstack/react-router";
import {
  getSimulationVersion,
  listSimulations,
  type SimulationSummaryResponse,
  type SimulationVersionDetailResponse,
} from "@/lib/api/praxis";
import { DossiePanel } from "./compliance";

const CORTA = 60;

export const Route = createFileRoute("/defensabilidade")({
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
      { title: "Defensabilidade - Praxis" },
      {
        name: "description",
        content:
          "Base técnica do resultado: construto, score auditável, pesos e limites do uso.",
      },
    ],
  }),
  component: DefensabilidadePage,
});

function DefensabilidadePage() {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/defensabilidade" });
  const hasContext = Boolean(search.simulationId && search.versionNumber);

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
  });
  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });

  const simulations = simulationsQuery.data ?? [];

  const closeDrawer = () =>
    navigate({
      to: "/defensabilidade",
      search: { simulationId: undefined, versionNumber: undefined },
    });

  return (
    <AppShell>
      <div className="mx-auto max-w-6xl px-2 py-8 sm:px-6">
        <div className="mb-6">
          <div className="text-[11px] font-semibold uppercase tracking-wider text-primary">
            Compliance
          </div>
          <h1 className="mt-1 font-serif text-3xl leading-tight">Defensabilidade do resultado</h1>
          <p className="mt-2 max-w-3xl text-sm text-muted-foreground">
            Base técnica que sustenta cada score: construto medido, critérios de pontuação, pesos
            relativos e caminhos possíveis. Se alguém perguntar "por que essa nota?", esta tela
            responde.
          </p>
        </div>

        <ComplianceScope current="defensabilidade" />

        <section className="rounded-xl border border-border bg-card">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border px-4 py-3">
            <div className="text-sm font-semibold">Selecione um teste para ver a base técnica</div>
          </div>

          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Teste</TableHead>
                  <TableHead>Versão</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Competências</TableHead>
                  <TableHead className="text-right">Ação</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {simulationsQuery.isLoading ? (
                  <TableRow>
                    <TableCell colSpan={5} className="p-4 text-sm text-muted-foreground">
                      Carregando testes...
                    </TableCell>
                  </TableRow>
                ) : simulations.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} className="p-4 text-sm text-muted-foreground">
                      Nenhum teste encontrado.
                    </TableCell>
                  </TableRow>
                ) : (
                  simulations.map((simulation) => (
                    <TableRow key={simulation.id}>
                      <TableCell className="font-medium">{simulation.name}</TableCell>
                      <TableCell className="font-mono text-xs">
                        v{simulation.versionNumber}
                      </TableCell>
                      <TableCell>
                        <StatusBadge status={simulation.status} variant="status" />
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-wrap gap-1">
                          {simulation.competencies.slice(0, 3).map((c) => (
                            <span
                              key={c}
                              className="rounded-md border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground"
                            >
                              {c}
                            </span>
                          ))}
                          {simulation.competencies.length > 3 && (
                            <span className="text-[10px] text-muted-foreground">
                              +{simulation.competencies.length - 3}
                            </span>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="text-right">
                        <Link
                          to="/defensabilidade"
                          search={{
                            simulationId: simulation.id,
                            versionNumber: simulation.versionNumber,
                          }}
                          className="inline-flex items-center gap-1.5 rounded-md border border-border bg-background px-2 py-1 text-xs hover:bg-accent"
                        >
                          <Scale className="h-3 w-3" />
                          Ver base técnica
                        </Link>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </section>

        <section className="mt-6 rounded-lg border border-border bg-muted/40 p-4">
          <div className="flex items-start gap-3">
            <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-success" />
            <div className="text-sm text-muted-foreground">
              <strong className="text-foreground">Por que "defensabilidade"?</strong> Porque o score
              de cada candidato é reproduzível: dado o mesmo cenário e as mesmas respostas, o
              resultado será sempre idêntico. Não há componente subjetivo ou modelo estatístico que
              varie entre execuções.
            </div>
          </div>
        </section>
      </div>

      <Sheet open={hasContext} onOpenChange={(value) => !value && closeDrawer()}>
        <SheetContent className="w-full max-w-xl overflow-y-auto p-0">
          <SheetTitle className="sr-only">Base técnica da versão</SheetTitle>

          <div className="flex items-start justify-between border-b border-border px-5 py-4">
            <div>
              <div className="text-[11px] uppercase tracking-wider text-muted-foreground">
                {search.simulationId} · v{search.versionNumber}
              </div>
              <div className="font-serif text-xl">Base técnica do resultado</div>
            </div>
            <SheetClose asChild>
              <button className="rounded-md p-1 hover:bg-accent" aria-label="Fechar">
                <span className="text-lg">&times;</span>
              </button>
            </SheetClose>
          </div>

          <div className="p-5">
            {versionQuery.isLoading ? (
              <div className="rounded-md border border-border bg-card p-4 text-sm text-muted-foreground">
                Carregando análise da versão...
              </div>
            ) : versionQuery.isError ? (
              <StateBanner tone="danger" title="Falha ao carregar dados">
                {versionQuery.error instanceof Error
                  ? versionQuery.error.message
                  : "Tente novamente."}
              </StateBanner>
            ) : versionQuery.data ? (
              <DossiePanel version={versionQuery.data} cutoff={CORTA} />
            ) : null}
          </div>
        </SheetContent>
      </Sheet>
    </AppShell>
  );
}
