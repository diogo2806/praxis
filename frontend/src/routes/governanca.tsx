import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { ArchiveRestore, Lock, RefreshCw } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";

export const Route = createFileRoute("/governanca")({
  head: () => ({
    meta: [
      { title: "Governanca e Auditoria" },
      { name: "description", content: "Controles operacionais, auditoria e versionamento." },
    ],
  }),
  component: GovernanceHub,
});

const events = [
  "Renata criou v0.1",
  "Gestor aprovou dialogo",
  "Validador registrou 2 warnings",
  "Compliance aprovou LGPD",
  "Publicacao vinculada a vaga GUPY-8421",
];

function GovernanceHub() {
  const [versionDialogOpen, setVersionDialogOpen] = useState(false);

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="papel atual nao tem permissao para esta transicao" />
      <div className="mb-5">
        <div className="text-xs uppercase text-primary">Conformidade</div>
        <h1 className="mt-1 text-3xl font-semibold">Governanca e auditoria</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Trilha imutavel de decisoes, versionamento e reprocessamento restrito a admin.
        </p>
      </div>

      <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_340px]">
        <section className="rounded-md border border-border bg-card p-5">
          <h2 className="text-sm font-semibold">AuditLog imutavel</h2>
          <ul className="mt-4 divide-y divide-border">
            {events.map((event, index) => (
              <li key={event} className="flex items-center gap-3 py-3 text-sm">
                <span className="flex h-7 w-7 items-center justify-center rounded-md bg-muted text-xs">
                  {index + 1}
                </span>
                <span className="flex-1">{event}</span>
                <span className="text-xs text-muted-foreground">15/06 14:{20 + index}</span>
              </li>
            ))}
          </ul>
        </section>
        <aside className="space-y-3">
          <StateBanner tone="warn" title="Edicao de publicada cria nova versao">
            Candidatos em andamento continuam na versao atual.
          </StateBanner>
          <button
            type="button"
            onClick={() => setVersionDialogOpen(true)}
            className="inline-flex w-full items-center justify-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
          >
            <RefreshCw className="h-4 w-4" />
            Criar versao v1.3
          </button>
          <button className="inline-flex w-full items-center justify-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent">
            <ArchiveRestore className="h-4 w-4" />
            Restaurar arquivada
          </button>
          <div className="flex items-start gap-2 rounded-md border border-border bg-muted/40 p-3 text-xs text-muted-foreground">
            <Lock className="mt-0.5 h-3.5 w-3.5" />
            Publicar com blocker e barrado na transicao de estado, nao apenas no log.
          </div>
          <Link
            to="/nova/governanca"
            className="inline-flex w-full justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
          >
            Ver etapa do wizard
          </Link>
          <Link
            to="/"
            className="inline-flex w-full justify-center rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
          >
            Voltar ao painel
          </Link>
        </aside>
      </div>
      {versionDialogOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-foreground/30 p-4">
          <div className="w-full max-w-md rounded-md border border-border bg-card p-5 shadow-xl">
            <div className="text-sm font-semibold">Criar nova versao?</div>
            <p className="mt-2 text-sm text-muted-foreground">
              Isto cria a versao v1.3. Candidatos em andamento continuam na versao atual e
              tentativas ja iniciadas nao migram.
            </p>
            <div className="mt-4 rounded-md border border-border bg-muted/45 p-3 text-xs text-muted-foreground">
              Typo entra como minor. Pontuacao, peso ou grafo entram como major.
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setVersionDialogOpen(false)}
                className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => setVersionDialogOpen(false)}
                className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
              >
                Confirmar nova versao
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
}
