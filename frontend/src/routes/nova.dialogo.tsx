import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { GitBranch, ListTree, Plus, Save, Smartphone, Trash2, Workflow } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import {
  NextStepContract,
  ScreenStateStrip,
  StateBanner,
  UndoRedoBar,
} from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/nova/dialogo")({
  head: () => ({
    meta: [
      { title: "Editor de Dialogo e Rubricas" },
      {
        name: "description",
        content:
          "Fluxo vivo com arvore navegavel, opcoes plausiveis, rubricas inline e preview do candidato.",
      },
    ],
  }),
  component: DialogEditor,
});

type Option = {
  id: string;
  label: string;
  text: string;
  next: string;
  empatia: number;
  processo: number;
  resolucao: number;
  critical?: boolean;
  best?: boolean;
};

type Node = {
  id: string;
  title: string;
  client: string;
  timeLimit: number | null;
  timeReason: string;
  status: "ready" | "review" | "blocker";
  options: Option[];
};

const initialNodes: Node[] = [
  {
    id: "T1",
    title: "Turno 1",
    client: "Chegou quebrado. Quero meu dinheiro de volta agora.",
    timeLimit: 30,
    timeReason: "Atendimento exige resposta rapida sob pressao emocional.",
    status: "blocker",
    options: [
      {
        id: "A",
        label: "A",
        text: "Acolho e prometo estorno imediato para acalmar o cliente.",
        next: "T2b",
        empatia: 3,
        processo: 0,
        resolucao: 1,
        critical: true,
      },
      {
        id: "B",
        label: "B",
        text: "Sigo o processo, peço dados e aviso que precisa aguardar.",
        next: "T2a",
        empatia: 1,
        processo: 3,
        resolucao: 2,
      },
      {
        id: "C",
        label: "C",
        text: "Acolho, coleto dados minimos e explico o proximo passo.",
        next: "T2a",
        empatia: 3,
        processo: 3,
        resolucao: 3,
        best: true,
      },
      {
        id: "D",
        label: "D",
        text: "Resolvo rapido, mas deixo o registro para depois.",
        next: "T2c",
        empatia: 2,
        processo: 0,
        resolucao: 2,
      },
    ],
  },
  {
    id: "T2a",
    title: "Turno 2A",
    client: "Tudo bem, mas eu preciso disso resolvido hoje.",
    timeLimit: 45,
    timeReason: "Ha pressao, mas o candidato ja tem dados suficientes.",
    status: "ready",
    options: [
      {
        id: "C",
        label: "C",
        text: "Confirmo prazo, registro caso e escalo quando sair da alcada.",
        next: "FIM",
        empatia: 2,
        processo: 3,
        resolucao: 3,
      },
      {
        id: "B",
        label: "B",
        text: "Explico a politica e ofereco alternativa documentada.",
        next: "FIM",
        empatia: 2,
        processo: 3,
        resolucao: 2,
      },
    ],
  },
  {
    id: "T2b",
    title: "Turno 2B",
    client: "Entao voce confirmou o estorno?",
    timeLimit: 30,
    timeReason: "Turno curto para testar correcao de erro critico.",
    status: "review",
    options: [
      {
        id: "A",
        label: "A",
        text: "Recuo, explico limite de alcada e levo para revisao humana.",
        next: "FIM",
        empatia: 2,
        processo: 2,
        resolucao: 1,
      },
      {
        id: "B",
        label: "B",
        text: "Mantenho a promessa sem validar a politica.",
        next: "FIM",
        empatia: 2,
        processo: 0,
        resolucao: 0,
        critical: true,
      },
    ],
  },
];

function DialogEditor() {
  const [nodes, setNodes] = useState<Node[]>(initialNodes);
  const [history, setHistory] = useState<Node[][]>([]);
  const [future, setFuture] = useState<Node[][]>([]);
  const [selectedId, setSelectedId] = useState("T1");
  const [mode, setMode] = useState<"lista" | "grafo">("grafo");
  const [editorMode, setEditorMode] = useState<"edit" | "review" | "published">("edit");
  const [savedAt, setSavedAt] = useState("14:21");

  const selected = nodes.find((node) => node.id === selectedId) ?? nodes[0];
  const isLocked = editorMode === "published";
  const canAddTurn = nodes.length < 10;

  const previewMessages = useMemo(() => {
    const best = selected.options.find((option) => option.best) ?? selected.options[0];
    return [
      { from: "client", text: selected.client },
      { from: "candidate", text: best?.text ?? "Escolha uma alternativa" },
      {
        from: "client",
        text: best?.next === "FIM" ? "Obrigado. Vou aguardar o retorno." : "Certo, e agora?",
      },
    ];
  }, [selected]);

  function commit(updater: (current: Node[]) => Node[]) {
    if (isLocked) return;
    setHistory((current) => [...current.slice(-8), nodes]);
    setFuture([]);
    setNodes((current) => updater(current));
    setSavedAt(new Date().toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" }));
  }

  function updateOption(optionId: string, value: string) {
    commit((current) =>
      current.map((node) =>
        node.id === selected.id
          ? {
              ...node,
              options: node.options.map((option) =>
                option.id === optionId ? { ...option, text: value.slice(0, 160) } : option,
              ),
            }
          : node,
      ),
    );
  }

  function updateNext(optionId: string, next: string) {
    commit((current) =>
      current.map((node) =>
        node.id === selected.id
          ? {
              ...node,
              options: node.options.map((option) =>
                option.id === optionId ? { ...option, next } : option,
              ),
            }
          : node,
      ),
    );
  }

  function addOption() {
    if (isLocked || selected.options.length >= 4) return;
    const label = String.fromCharCode(65 + selected.options.length);
    commit((current) =>
      current.map((node) =>
        node.id === selected.id
          ? {
              ...node,
              status: "review",
              options: [
                ...node.options,
                {
                  id: label,
                  label,
                  text: "Nova alternativa plausivel.",
                  next: "FIM",
                  empatia: 1,
                  processo: 1,
                  resolucao: 1,
                },
              ],
            }
          : node,
      ),
    );
  }

  function addTurn() {
    if (isLocked || !canAddTurn) return;
    const id = `T${nodes.length + 1}`;
    commit((current) => [
      ...current,
      {
        id,
        title: `Turno ${nodes.length + 1}`,
        client: "Escreva a proxima fala do cliente ficticio.",
        timeLimit: 45,
        timeReason: "Justificativa obrigatoria do limite deste turno.",
        status: "review",
        options: [
          {
            id: "A",
            label: "A",
            text: "Alternativa inicial plausivel.",
            next: "FIM",
            empatia: 1,
            processo: 1,
            resolucao: 1,
          },
          {
            id: "B",
            label: "B",
            text: "Outra alternativa inicial plausivel.",
            next: "FIM",
            empatia: 2,
            processo: 2,
            resolucao: 2,
          },
        ],
      },
    ]);
    setSelectedId(id);
  }

  function deleteTurn(id: string) {
    if (isLocked || nodes.length <= 1) return;
    commit((current) =>
      current
        .filter((node) => node.id !== id)
        .map((node) => ({
          ...node,
          options: node.options.map((option) =>
            option.next === id ? { ...option, next: "FIM" } : option,
          ),
        })),
    );
    setSelectedId("T1");
  }

  function undo() {
    if (isLocked) return;
    const previous = history.at(-1);
    if (!previous) return;
    setFuture((current) => [nodes, ...current]);
    setNodes(previous);
    setHistory((current) => current.slice(0, -1));
  }

  function redo() {
    if (isLocked) return;
    const next = future[0];
    if (!next) return;
    setHistory((current) => [...current, nodes]);
    setNodes(next);
    setFuture((current) => current.slice(1));
  }

  return (
    <AppShell>
      <WizardStepper current="dialogo" />
      <ScreenStateStrip blockedReason="no com blocker ou alternativa fora da regra 2 a 4" />
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 3 - fluxo vivo</div>
          <h1 className="mt-1 text-3xl font-semibold">Editor de dialogo</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Construa a arvore de turnos, edite opcoes inline e veja o chat do candidato em tempo
            real.
          </p>
        </div>
        <UndoRedoBar savedAt={savedAt} onUndo={undo} onRedo={redo} />
      </div>

      <div className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-md border border-border bg-card p-3">
        <div>
          <div className="text-sm font-semibold">Modo do editor</div>
          <p className="text-xs text-muted-foreground">
            Fecha a duvida entre editar, revisar e proteger versao publicada.
          </p>
        </div>
        <div className="inline-flex rounded-md border border-border bg-background p-1 text-xs">
          {[
            { id: "edit", label: "Edicao" },
            { id: "review", label: "Revisao final" },
            { id: "published", label: "Bloqueado pos-publicacao" },
          ].map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => setEditorMode(item.id as typeof editorMode)}
              className={cn(
                "rounded px-3 py-1.5",
                editorMode === item.id && "bg-primary text-primary-foreground",
              )}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {editorMode === "edit" && (
        <StateBanner tone="warn" title="Alteracoes anteriores ficam como revisar, nao sao apagadas">
          Se um passo invalida outro, o validador marca os turnos seguintes em amarelo e preserva o
          trabalho.
        </StateBanner>
      )}
      {editorMode === "review" && (
        <StateBanner tone="info" title="Modo revisao final">
          Edicao ainda permitida. O foco passa a ser validar ramificacoes, rubricas e mensagens que
          ainda podem quebrar a publicacao.
        </StateBanner>
      )}
      {isLocked && (
        <StateBanner
          tone="danger"
          title="Versao publicada bloqueada"
          action={
            <button className="shrink-0 rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium">
              Criar v1.1
            </button>
          }
        >
          Editar uma versao publicada cria uma nova versao. Candidatos em andamento continuam na
          versao atual.
        </StateBanner>
      )}

      <div className="mt-5">
        <NextStepContract
          primary="Validar qualidade quando todos os turnos estiverem sem blocker."
          secondary="Voltar a personagem ou blueprint continua permitido antes de publicar."
          versionRule="Depois de publicar, qualquer edicao cria v1.1 automaticamente."
          lockedAfter="Versao publicada nao altera tentativas ou candidatos em andamento."
        />
      </div>

      <div className="mt-5 grid gap-5 xl:grid-cols-[360px_minmax(0,1fr)_340px]">
        <aside className="rounded-md border border-border bg-card p-4">
          <div className="mb-3 flex items-center justify-between">
            <div className="text-sm font-semibold">Arvore navegavel</div>
            <div className="inline-flex rounded-md border border-border bg-background p-1">
              <button
                type="button"
                onClick={() => setMode("lista")}
                className={cn(
                  "rounded px-2 py-1",
                  mode === "lista" && "bg-primary text-primary-foreground",
                )}
                aria-label="Ver lista"
              >
                <ListTree className="h-4 w-4" />
              </button>
              <button
                type="button"
                onClick={() => setMode("grafo")}
                className={cn(
                  "rounded px-2 py-1",
                  mode === "grafo" && "bg-primary text-primary-foreground",
                )}
                aria-label="Ver grafo"
              >
                <Workflow className="h-4 w-4" />
              </button>
            </div>
          </div>
          {mode === "lista" ? (
            <div className="space-y-2">
              {nodes.map((node) => (
                <NodeButton
                  key={node.id}
                  node={node}
                  active={node.id === selected.id}
                  onClick={setSelectedId}
                />
              ))}
            </div>
          ) : (
            <div className="space-y-3">
              {nodes.map((node) => (
                <div key={node.id}>
                  <NodeButton
                    node={node}
                    active={node.id === selected.id}
                    onClick={setSelectedId}
                  />
                  <div className="ml-5 mt-2 space-y-1 border-l border-dashed border-border pl-3">
                    {node.options.map((option) => (
                      <button
                        key={`${node.id}-${option.id}`}
                        type="button"
                        onClick={() => setSelectedId(option.next === "FIM" ? node.id : option.next)}
                        className="flex w-full items-center gap-2 rounded px-2 py-1 text-left text-xs text-muted-foreground hover:bg-accent"
                      >
                        <GitBranch className="h-3.5 w-3.5" />
                        {option.label} vai para {option.next}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
          <button
            type="button"
            onClick={addTurn}
            disabled={isLocked || !canAddTurn}
            className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Plus className="h-4 w-4" />
            Adicionar turno
          </button>
        </aside>

        <section className="space-y-4 rounded-md border border-border bg-card p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <div className="text-xs uppercase text-muted-foreground">{selected.id}</div>
              <h2 className="text-xl font-semibold">{selected.title}</h2>
            </div>
            <button
              type="button"
              onClick={() => deleteTurn(selected.id)}
              disabled={isLocked}
              className="inline-flex items-center gap-2 rounded-md border border-danger/25 bg-danger/5 px-3 py-2 text-xs text-danger hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Trash2 className="h-4 w-4" />
              Deletar turno
            </button>
          </div>

          <label className="block">
            <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
              Mensagem do cliente
            </span>
            <textarea
              className="input min-h-20"
              value={selected.client}
              disabled={isLocked}
              onChange={(event) =>
                commit((current) =>
                  current.map((node) =>
                    node.id === selected.id ? { ...node, client: event.target.value } : node,
                  ),
                )
              }
            />
          </label>

          <div className="grid gap-3 md:grid-cols-[140px_1fr]">
            <label>
              <span className="mb-1.5 block text-xs font-medium text-muted-foreground">Tempo</span>
              <select
                className="input"
                value={selected.timeLimit ?? "none"}
                disabled={isLocked}
                onChange={(event) =>
                  commit((current) =>
                    current.map((node) =>
                      node.id === selected.id
                        ? {
                            ...node,
                            timeLimit:
                              event.target.value === "none" ? null : Number(event.target.value),
                          }
                        : node,
                    ),
                  )
                }
              >
                <option value="none">Sem limite</option>
                <option value="30">30 s</option>
                <option value="45">45 s</option>
                <option value="60">60 s</option>
              </select>
            </label>
            <label>
              <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                Justifique o tempo
              </span>
              <input
                className="input"
                value={selected.timeReason}
                disabled={isLocked}
                onChange={(event) =>
                  commit((current) =>
                    current.map((node) =>
                      node.id === selected.id ? { ...node, timeReason: event.target.value } : node,
                    ),
                  )
                }
              />
            </label>
          </div>

          <div className="flex items-center justify-between">
            <div className="text-sm font-semibold">Opcoes plausiveis</div>
            <button
              type="button"
              onClick={addOption}
              disabled={isLocked || selected.options.length >= 4}
              className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-xs hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Plus className="h-4 w-4" />
              Opcao
            </button>
          </div>

          <div className="space-y-3">
            {selected.options.map((option) => (
              <div
                key={option.id}
                className={cn(
                  "rounded-md border p-3",
                  option.critical
                    ? "border-danger/30 bg-danger/5"
                    : option.best
                      ? "border-success/30 bg-success/5"
                      : "border-border bg-background",
                )}
              >
                <div className="grid gap-3 md:grid-cols-[32px_1fr_150px]">
                  <div className="flex h-8 w-8 items-center justify-center rounded-md border border-border bg-card text-sm font-semibold">
                    {option.label}
                  </div>
                  <label>
                    <input
                      className="input"
                      value={option.text}
                      maxLength={160}
                      disabled={isLocked}
                      onChange={(event) => updateOption(option.id, event.target.value)}
                    />
                    <span className="mt-1 block text-[11px] text-muted-foreground">
                      {option.text.length}/160 caracteres
                    </span>
                  </label>
                  <label>
                    <select
                      className="input"
                      value={option.next}
                      disabled={isLocked}
                      onChange={(event) => updateNext(option.id, event.target.value)}
                    >
                      {nodes
                        .filter((node) => node.id !== selected.id)
                        .map((node) => (
                          <option key={node.id} value={node.id}>
                            Vai para {node.id}
                          </option>
                        ))}
                      <option value="FIM">Vai para FIM</option>
                    </select>
                  </label>
                </div>
                <div className="mt-2 flex flex-wrap gap-1.5">
                  <Rubric label="Empatia" value={option.empatia} />
                  <Rubric label="Processo" value={option.processo} />
                  <Rubric label="Resolucao" value={option.resolucao} />
                  {option.critical && (
                    <span className="rounded-md border border-danger/30 bg-danger/10 px-2 py-1 text-[11px] font-medium text-danger">
                      Erro critico - revisao humana
                    </span>
                  )}
                  {option.best && (
                    <span className="rounded-md border border-success/30 bg-success/10 px-2 py-1 text-[11px] font-medium text-success">
                      Comportamento esperado
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>

        <aside className="rounded-md border border-border bg-card p-4">
          <div className="mb-3 flex items-center gap-2 text-sm font-semibold">
            <Smartphone className="h-4 w-4" />
            Preview do candidato
          </div>
          <div className="mx-auto max-w-[260px] rounded-[28px] border border-border bg-foreground p-2">
            <div className="rounded-[22px] bg-background p-3">
              <div className="mb-3 flex items-center justify-between border-b border-border pb-2 text-[11px]">
                <span>
                  {selected.timeLimit ? `${selected.timeLimit}s restantes` : "Sem limite"}
                </span>
                <span className="rounded bg-success/10 px-1.5 py-0.5 text-success">salvo</span>
              </div>
              <div className="space-y-2">
                {previewMessages.map((message, index) => (
                  <div
                    key={`${message.from}-${index}`}
                    className={cn(
                      "rounded-md px-3 py-2 text-xs",
                      message.from === "client"
                        ? "mr-6 bg-muted text-foreground"
                        : "ml-6 bg-primary text-primary-foreground",
                    )}
                  >
                    {message.text}
                  </div>
                ))}
                <div className="mr-16 rounded-md bg-muted px-3 py-2 text-xs text-muted-foreground">
                  digitando...
                </div>
              </div>
            </div>
          </div>
          <StateBanner tone="info" title="Autosave ativo">
            Cada resposta persiste no turno. Recarregar ou voltar no deep-link nao perde estado.
          </StateBanner>
          <button className="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent">
            <Save className="h-4 w-4" />
            {isLocked ? "Criar nova versao para editar" : "Salvar manualmente"}
          </button>
        </aside>
      </div>

      <div className="mt-8 flex justify-between">
        <Link
          to="/nova/personagem"
          className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar
        </Link>
        <Link
          to="/nova/validador"
          className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          Validar qualidade
        </Link>
      </div>
    </AppShell>
  );
}

function NodeButton({
  node,
  active,
  onClick,
}: {
  node: Node;
  active: boolean;
  onClick: (id: string) => void;
}) {
  return (
    <button
      type="button"
      onClick={() => onClick(node.id)}
      className={cn(
        "w-full rounded-md border p-3 text-left text-sm hover:bg-accent",
        active && "border-primary bg-primary/5",
        node.status === "blocker" && "border-danger/40",
        node.status === "review" && "border-warning/40",
      )}
    >
      <div className="flex items-center justify-between">
        <span className="font-medium">{node.title}</span>
        <span
          className={cn(
            "rounded px-1.5 py-0.5 text-[10px]",
            node.status === "ready" && "bg-success/10 text-success",
            node.status === "review" && "bg-warning/15 text-warning-foreground",
            node.status === "blocker" && "bg-danger/10 text-danger",
          )}
        >
          {node.status === "ready" ? "pronto" : node.status === "review" ? "revisar" : "blocker"}
        </span>
      </div>
      <div className="mt-1 line-clamp-2 text-xs text-muted-foreground">{node.client}</div>
    </button>
  );
}

function Rubric({ label, value }: { label: string; value: number }) {
  return (
    <span className="rounded-md border border-border bg-card px-2 py-1 text-[11px] text-foreground/75">
      {label} <b>N{value}</b>
    </span>
  );
}
