import { createFileRoute, Link } from "@tanstack/react-router";
import { FileSearch, ShieldCheck } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";

export const Route = createFileRoute("/lgpd")({
  head: () => ({
    meta: [
      { title: "LGPD e Explicabilidade" },
      { name: "description", content: "Explicacao de score e revisao humana." },
    ],
  }),
  component: LgpdPage,
});

function LgpdPage() {
  return (
    <AppShell>
      <ScreenStateStrip blockedReason="canal de revisao humana nao configurado" />
      <div className="mb-5">
        <div className="text-xs uppercase text-primary">Conformidade</div>
        <h1 className="mt-1 text-3xl font-semibold">LGPD e explicabilidade</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          O candidato pode pedir revisao. A explicacao usa rubrica, escolha e caminho, sem
          caixa-preta.
        </p>
      </div>
      <div className="grid gap-5 lg:grid-cols-3">
        {[
          "Pontuacao normalizada por caminho",
          "Erro critico gera revisao humana",
          "Sem IA julgando candidato",
        ].map((item) => (
          <div key={item} className="rounded-md border border-border bg-card p-5">
            <ShieldCheck className="h-5 w-5 text-success" />
            <div className="mt-3 text-sm font-semibold">{item}</div>
            <p className="mt-1 text-sm text-muted-foreground">
              Evidencia rastreavel por versao, tentativa, turno e alternativa escolhida.
            </p>
          </div>
        ))}
      </div>
      <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_340px]">
        <section className="rounded-md border border-border bg-card p-5">
          <div className="mb-3 flex items-center gap-2 text-sm font-semibold">
            <FileSearch className="h-4 w-4" />
            Exemplo de explicacao
          </div>
          <p className="text-sm text-foreground/85">
            No turno 1, a alternativa escolhida acolheu a frustracao, coletou dados minimos e
            respeitou a politica de estorno. Por isso recebeu Empatia N3, Processo N3 e Resolucao
            N3.
          </p>
        </section>
        <StateBanner tone="warn" title="Canal de revisao obrigatorio">
          Uso eliminatorio so e permitido para simulacao validada, com aprovacao e revisao humana.
        </StateBanner>
      </div>
      <div className="mt-6">
        <Link
          to="/"
          className="inline-flex rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar ao painel
        </Link>
      </div>
    </AppShell>
  );
}
