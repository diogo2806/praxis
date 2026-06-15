import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { Pause, Play, RotateCcw, ShieldCheck, Wifi, WifiOff } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import { cn } from "@/lib/utils";
import { useViewMode } from "@/lib/view-mode";

export const Route = createFileRoute("/candidato")({
  head: () => ({
    meta: [
      { title: "Visão do Candidato — Práxis" },
      {
        name: "description",
        content: "Experiência mobile-first com timer, chat, retomada e acessibilidade.",
      },
    ],
  }),
  component: CandidateDemoPage,
});

const options = [
  "Acolher, coletar dados mínimos e explicar o próximo passo.",
  "Prometer estorno imediato para acalmar o cliente.",
  "Seguir o processo sem reconhecer a frustração.",
];

function CandidateDemoPage() {
  const [token, setToken] = useState<string | undefined>();
  useEffect(() => {
    const match = window.location.pathname.match(/^\/candidato\/([^/]+)$/);
    setToken(match?.[1]);
  }, []);
  return <CandidateExperience demo={!token} token={token} />;
}

export function CandidateExperience({ token, demo = false }: { token?: string; demo?: boolean }) {
  const [remaining, setRemaining] = useState(30);
  const [paused, setPaused] = useState(false);
  const [offline, setOffline] = useState(false);
  const [selected, setSelected] = useState<string | null>(null);
  const [finished, setFinished] = useState(false);
  const technical = useViewMode() === "technical";

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
      { label: "Resolução", value: "boa" },
      { label: "Processo", value: selected?.includes("Prometer") ? "revisão" : "adequado" },
    ],
    [selected],
  );

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="link expirado ou tentativa abandonada fora da janela" />
      <div className="mb-5">
        <div className="text-xs uppercase text-primary">Visão do candidato</div>
        <h1 className="mt-1 text-3xl font-semibold">Simulação situacional</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Chat estruturado, timer legível, navegação por teclado e retomada após queda.
        </p>
      </div>

      {technical && demo && (
        <StateBanner tone="info" title="Rota demo">
          Em produção o candidato acessa <code>/candidato/:token</code>. Esta rota existe para
          apresentação interna.
        </StateBanner>
      )}

      {technical && token && (
        <StateBanner tone="info" title="Token de tentativa carregado">
          Token visualizado apenas no modo técnico: <code>{token}</code>.
        </StateBanner>
      )}

      {offline && (
        <StateBanner tone="warn" title="Conexão perdida - reconectando">
          A resposta salva no último turno será retomada automaticamente quando a conexão voltar.
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
                      <span aria-live="polite">{remaining}s restantes</span>
                      <span>{paused ? "pausado" : "turno 1 de 4"}</span>
                    </div>
                    <div
                      className="h-2 overflow-hidden rounded-full bg-muted"
                      aria-label={`${remaining} segundos restantes`}
                      role="timer"
                    >
                      <div
                        className={cn("h-full rounded-full transition-all", timerTone)}
                        style={{ width: `${pct * 100}%` }}
                      />
                    </div>
                  </div>
                  <div className="space-y-3" aria-live="polite">
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
                    O RH retorna em até 5 dias úteis. Você pode revisar acomodações ou contatar o
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
            <div className="mb-3 flex items-center gap-2 text-sm font-semibold">
              <ShieldCheck className="h-4 w-4 text-primary" />
              Recursos de acessibilidade
            </div>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>Compatível com leitor de tela</li>
              <li>Alto contraste</li>
              <li>Tempo estendido quando configurado</li>
              <li>Navegação por teclado</li>
            </ul>
          </div>

          {technical && (
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
          )}

          <StateBanner tone="info" title="Tempo esgotado encerra só o turno">
            Quando chega a zero, registra "sem resposta" e avança. A simulação inteira não fecha.
          </StateBanner>
          <StateBanner tone="ok" title="Acomodação PCD aplicada sem expor motivo">
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
