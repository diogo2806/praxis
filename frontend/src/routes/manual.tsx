import { createFileRoute } from "@tanstack/react-router";
import { BookOpenText } from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { ScreenManualContent } from "@/components/screen-manual";
import { SCREEN_MANUAL_OVERRIDES } from "@/lib/screen-manual-overrides";
import { SCREEN_MANUALS } from "@/lib/screen-manuals";

export const Route = createFileRoute("/manual")({
  head: () => ({
    meta: [
      { title: "Central de manuais - Práxis" },
      {
        name: "description",
        content: "Consulte os fluxos, campos, permissões, estados, bloqueios e exemplos das telas do Práxis.",
      },
    ],
  }),
  component: ManualPage,
});

const REPLACED_BASE_MANUALS = new Set(["jornadas", "operacao"]);

function ManualPage() {
  const manuals = [
    ...SCREEN_MANUAL_OVERRIDES,
    ...SCREEN_MANUALS.filter((manual) => !REPLACED_BASE_MANUALS.has(manual.id)),
  ];

  return (
    <AppShell>
      <main className="mx-auto max-w-5xl">
        <header className="mb-8">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <BookOpenText className="h-6 w-6" />
            </div>
            <div>
              <h1 className="text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">
                Central de manuais
              </h1>
              <p className="mt-1 text-sm leading-6 text-muted-foreground">
                Consulte o processo completo ou abra o ícone de manual em qualquer tela para ver apenas o contexto atual.
              </p>
            </div>
          </div>
        </header>

        <nav aria-label="Processos documentados" className="mb-8 rounded-xl border border-border bg-card p-4">
          <div className="mb-3 text-sm font-semibold text-foreground">Processos documentados</div>
          <div className="flex flex-wrap gap-2">
            {manuals.map((manual) => (
              <a
                key={manual.id}
                href={`#${manual.id}`}
                className="rounded-full border border-border bg-background px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-accent"
              >
                {manual.title}
              </a>
            ))}
          </div>
        </nav>

        <div className="space-y-8">
          {manuals.map((manual) => (
            <article key={manual.id} id={manual.id} className="scroll-mt-24 rounded-xl border border-border bg-card shadow-sm">
              <header className="border-b border-border px-5 py-4 sm:px-6">
                <h2 className="text-xl font-semibold text-foreground">{manual.title}</h2>
              </header>
              <ScreenManualContent manual={manual} />
            </article>
          ))}
        </div>
      </main>
    </AppShell>
  );
}
