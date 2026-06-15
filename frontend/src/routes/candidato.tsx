import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { Pause, Play, RotateCcw, Wifi, WifiOff } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/candidato")({
  head: () => ({
    meta: [
      { title: "Visao do candidato" },
      { name: "description", content: "Experiencia mobile-first com timer, chat e retomada." },
    ],
  }),
  component: CandidatePage,
});

const options = [
  "Acolher, coletar dados minimos e explicar o proximo passo.",
  "Prometer estorno imediato para acalmar o cliente.",
  "Seguir o processo sem reconhecer a frustracao.",
];

function CandidatePage() {
  const [remaining, setRemaining] = useState(30);
  const [paused, setPaused] = useState(false);
  const [offline, setOffline] = useState(false);
  const [selected, setSelected] = useState<string | null>(null);
  const [finished, setFinished] = useState(false);

  useEffect(() => {
    if (paused || finished) return;
    const id = window.setInterval(() => {
      setRemaining((value) => Math.max(0, value - 1));
    }, 1000);
    return () => window.clearInterval(id);
  }, [paused, finished]);

  useEffect(() => {
    if (remaining === 0 && !finished) {
      setSelected("Sem resposta neste turno");
      window.setTimeout(() => setFinished(true), 600);
    }
  }, [remaining, finished]);

  const pct = remaining / 30;
  const timerTone = pct <= 0.1 ? "bg-danger" : pct <= 0.3 ? "bg-warning" : "bg-primary";

  const finalSummary = useMemo(
    () => [
      { label: "Empatia", value: "alta" },
      { label: "Resolucao", value: "boa" },
      { label: "Processo", value: selected?.includes("Prometer") ? "revisao" : "adequado" },
    ],
    [selected],
  );

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="link expirado ou tentativa abandonada fora da janela" />
      <div className="mb-5">
        <div className="text-xs uppercase text-primary">Visao do candidato</div>
        <h1 className="mt-1 text-3xl font-semibold">Simulacao situacional</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Timer visivel, sem som por padrao, transicao de chat e retomada apos queda.
        </p>
      </div>

      {offline && (
        <StateBanner tone="warn" title="Conexao perdida - reconectando">
          A resposta salva no ultimo turno sera retomada automaticamente quando a conexao voltar.
        </StateBanner>
      )}

      <div className="mt-5 grid gap-5 lg:grid-cols-[360px_minmax(0,1fr)]">
        <section className="rounded-md border border-border bg-card p-5">
          <div className="mx-auto max-w-[320px] rounded-[30px] border border-border bg-foreground p-2">
            <div className="min-h-[620px] rounded-[24px] bg-background p-4">
              {!finished ? (
                <>
                  <div className="mb-4">
                    <div className="mb-1 flex items-center justify-between text-xs">
                      <span>{remaining}s restantes</span>
                      <span>{paused ? "pausado" : "turno 1 de 4"}</span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-muted">
                      <div
                        className={cn("h-full rounded-full transition-all", timerTone)}
                        style={{ width: `${pct * 100}%` }}
                      />
                    </div>
                  </div>
                  <div className="space-y-3">
                    <div className="mr-8 rounded-md bg-muted px-3 py-2 text-sm">
                      Chegou quebrado. Quero meu dinheiro de volta agora.
                    </div>
                    {selected && (
                      <div className="ml-8 rounded-md bg-primary px-3 py-2 text-sm text-primary-foreground">
                        {selected}
                      </div>
                    )}
                    {selected && (
                      <div className="mr-16 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">
                        digitando...
                      </div>
                    )}
                  </div>
                  {!selected && (
                    <div className="mt-5 space-y-2">
                      {options.map((option) => (
                        <button
                          key={option}
                          type="button"
                          onClick={() => {
                            setSelected(option);
                            window.setTimeout(() => setFinished(true), 900);
                          }}
                          className="w-full rounded-md border border-border bg-card p-3 text-left text-sm hover:border-primary hover:bg-primary/5"
                        >
                          {option}
                        </button>
                      ))}
                    </div>
                  )}
                </>
              ) : (
                <div className="flex min-h-[560px] flex-col justify-center">
                  <div className="text-xs uppercase text-success">Respostas enviadas</div>
                  <h2 className="mt-2 text-2xl font-semibold">Obrigado por participar.</h2>
                  <p className="mt-2 text-sm text-muted-foreground">
                    O RH retorna em ate 5 dias uteis. Voce pode revisar acomodacoes ou contatar o
                    recrutador se precisar de suporte.
                  </p>
                  <div className="mt-5 grid gap-2">
                    {finalSummary.map((item) => (
                      <div
                        key={item.label}
                        className="flex justify-between rounded-md border border-border bg-card px-3 py-2 text-sm"
                      >
                        <span>{item.label}</span>
                        <span className="font-medium">{item.value}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </section>

        <aside className="space-y-4">
          <div className="rounded-md border border-border bg-card p-5">
            <h2 className="text-sm font-semibold">Controles de estado</h2>
            <div className="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => setPaused((value) => !value)}
                className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
              >
                {paused ? <Play className="h-4 w-4" /> : <Pause className="h-4 w-4" />}
                {paused ? "Retomar" : "Pausar"}
              </button>
              <button
                type="button"
                onClick={() => setOffline((value) => !value)}
                className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
              >
                {offline ? <Wifi className="h-4 w-4" /> : <WifiOff className="h-4 w-4" />}
                {offline ? "Reconectar" : "Simular queda"}
              </button>
              <button
                type="button"
                onClick={() => {
                  setRemaining(30);
                  setSelected(null);
                  setFinished(false);
                  setPaused(false);
                }}
                className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
              >
                <RotateCcw className="h-4 w-4" />
                Reiniciar turno
              </button>
            </div>
          </div>
          <StateBanner tone="info" title="Tempo esgotado encerra so o turno">
            Quando chega a zero, registra "sem resposta" e avanca. A simulacao inteira nao fecha.
          </StateBanner>
          <StateBanner tone="ok" title="Acomodacao PCD aplicada sem expor motivo">
            O multiplicador altera o tempo apresentado ao candidato, mantendo privacidade.
          </StateBanner>
        </aside>
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
