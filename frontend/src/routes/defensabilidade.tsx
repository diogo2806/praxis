import { createFileRoute, Link } from "@tanstack/react-router";
import { ClipboardCheck, Scale, Shield } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";

export const Route = createFileRoute("/defensabilidade")({
  head: () => ({
    meta: [
      { title: "Defensabilidade" },
      { name: "description", content: "Base tecnica e juridica do SJT deterministico." },
    ],
  }),
  component: DefensibilityPage,
});

const pillars = [
  {
    icon: ClipboardCheck,
    title: "Construto definido",
    text: "Blueprint fixa cargo, situacao critica e comportamento observavel.",
  },
  {
    icon: Scale,
    title: "Score auditavel",
    text: "Rubrica, peso e caminho explicam cada ponto do resultado.",
  },
  {
    icon: Shield,
    title: "Sem julgamento por IA",
    text: "Nenhuma chamada de modelo em runtime; calculo deterministico.",
  },
];

function DefensibilityPage() {
  return (
    <AppShell>
      <ScreenStateStrip blockedReason="promessa comercial indefensavel precisa ser removida" />
      <div className="mb-5">
        <div className="text-xs uppercase text-primary">Conformidade</div>
        <h1 className="mt-1 text-3xl font-semibold">Defensabilidade</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          O produto mede julgamento situacional, nao atendimento real nem texto livre.
        </p>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        {pillars.map(({ icon: Icon, title, text }) => (
          <div key={title} className="rounded-md border border-border bg-card p-5">
            <Icon className="h-5 w-5 text-primary" />
            <h2 className="mt-3 text-sm font-semibold">{title}</h2>
            <p className="mt-1 text-sm text-muted-foreground">{text}</p>
          </div>
        ))}
      </div>
      <div className="mt-5">
        <StateBanner tone="danger" title="Promessa proibida">
          Nao vender como transcricao real, atendimento real ou IA que entende o candidato.
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
