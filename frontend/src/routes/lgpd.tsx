import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { FileCheck2, ShieldCheck, UserRound } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ComplianceScope } from "@/components/compliance-scope";
import { StateBanner } from "@/components/praxis-ui";
import { getPrivacyCompliance } from "@/lib/api/praxis";
import { useLanguage } from "@/lib/language-context";

export const Route = createFileRoute("/lgpd")({
  head: () => ({
    meta: [
      { title: "LGPD e Privacidade - Praxis" },
      {
        name: "description",
        content: "Bases legais, retenção de dados, canal de revisão humana e direitos do titular.",
      },
    ],
  }),
  component: LgpdPage,
});

function LgpdPage() {
  const { t } = useLanguage();
  const privacyQuery = useQuery({
    queryKey: ["privacy-compliance"],
    queryFn: getPrivacyCompliance,
  });

  const privacy = privacyQuery.data;

  return (
    <AppShell>
      <div className="mx-auto max-w-6xl px-2 py-8 sm:px-6">
        <div className="mb-6">
          <div className="text-[11px] font-semibold uppercase tracking-wider text-primary">
            {t.common.compliance}
          </div>
          <h1 className="mt-1 font-serif text-3xl leading-tight">LGPD e direitos do candidato</h1>
          <p className="mt-2 max-w-3xl text-sm text-muted-foreground">
            Informações sobre bases legais utilizadas, política de retenção, canal de revisão humana
            e explicabilidade do resultado para o titular dos dados.
          </p>
        </div>

        <ComplianceScope current="lgpd" />

        {privacyQuery.isLoading && (
          <div className="rounded-md border border-border bg-card p-6 text-sm text-muted-foreground">
            Carregando informações de privacidade...
          </div>
        )}

        {privacyQuery.isError && (
          <StateBanner tone="danger" title="Não foi possível carregar dados de privacidade">
            {privacyQuery.error instanceof Error
              ? privacyQuery.error.message
              : "Tente novamente em alguns instantes."}
          </StateBanner>
        )}

        {privacy && (
          <div className="space-y-6">
            <section className="rounded-xl border border-border bg-card">
              <div className="border-b border-border px-5 py-4">
                <div className="flex items-center gap-2 text-sm font-semibold">
                  <FileCheck2 className="h-4 w-4 text-primary" />
                  Bases legais para tratamento de dados
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  Fundamentos jurídicos que autorizam o tratamento dos dados pessoais do candidato.
                </p>
              </div>
              <div className="divide-y divide-border">
                {privacy.legalBases.map((base) => (
                  <div key={base.name} className="px-5 py-4">
                    <div className="text-sm font-medium">{base.name}</div>
                    <div className="mt-1 text-xs text-muted-foreground">{base.description}</div>
                  </div>
                ))}
              </div>
            </section>

            <div className="grid gap-5 md:grid-cols-2">
              <section className="rounded-xl border border-border bg-card p-5">
                <div className="flex items-center gap-2 text-sm font-semibold">
                  <ShieldCheck className="h-4 w-4 text-primary" />
                  Retenção de dados
                </div>
                <div className="mt-4 space-y-3">
                  <div>
                    <div className="text-xs uppercase text-muted-foreground">
                      Período de retenção
                    </div>
                    <div className="mt-1 text-2xl font-semibold tabular-nums">
                      {privacy.retentionDays} dias
                    </div>
                  </div>
                  <div>
                    <div className="text-xs uppercase text-muted-foreground">Política</div>
                    <div className="mt-1 text-sm text-muted-foreground">
                      {privacy.retentionPolicy}
                    </div>
                  </div>
                </div>
              </section>

              <section className="rounded-xl border border-border bg-card p-5">
                <div className="flex items-center gap-2 text-sm font-semibold">
                  <UserRound className="h-4 w-4 text-primary" />
                  Revisão humana
                </div>
                <div className="mt-4 space-y-3">
                  <div>
                    <div className="text-xs uppercase text-muted-foreground">Canal de revisão</div>
                    <div className="mt-1 text-sm">{privacy.reviewChannel}</div>
                  </div>
                  <div>
                    <div className="text-xs uppercase text-muted-foreground">SLA de resposta</div>
                    <div className="mt-1 text-sm">{privacy.reviewSla}</div>
                  </div>
                  <div>
                    <div className="text-xs uppercase text-muted-foreground">
                      Decisão automatizada sem revisão
                    </div>
                    <div className="mt-1 text-sm">
                      {privacy.automatedDecisionWithoutReviewAllowed ? (
                        <span className="rounded-md bg-warning/15 px-2 py-0.5 text-xs font-medium text-warning">
                          Permitida
                        </span>
                      ) : (
                        <span className="rounded-md bg-success/15 px-2 py-0.5 text-xs font-medium text-success">
                          Não permitida — revisão humana obrigatória
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </section>
            </div>

            <section className="rounded-lg border border-border bg-muted/40 p-4">
              <div className="flex items-start gap-3">
                <UserRound className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
                <div className="text-sm text-muted-foreground">
                  <strong className="text-foreground">Explicabilidade para o candidato.</strong> O
                  Praxis não usa IA generativa para julgar respostas. O score vem de alternativas
                  pré-definidas com pesos declarados. A qualquer momento, o candidato pode solicitar
                  a explicação do resultado pelo canal indicado acima.
                </div>
              </div>
            </section>
          </div>
        )}
      </div>
    </AppShell>
  );
}
