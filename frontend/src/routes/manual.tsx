import { createFileRoute } from "@tanstack/react-router";
import { BookOpenText, ChevronLeft, ChevronRight, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { ScreenManualContent } from "@/components/screen-manual";
import { Input } from "@/components/ui/input";
import { ACCESS_ONBOARDING_MANUALS } from "@/lib/screen-manual-access-onboarding";
import { ANALYSIS_OPERATION_MANUALS } from "@/lib/screen-manual-analysis-operations";
import { COMPETENCY_OWNERSHIP_MANUALS } from "@/lib/screen-manual-competency-ownership";
import { PORTABILITY_MANUALS } from "@/lib/screen-manual-portability";
import { PUBLICATION_MANUALS } from "@/lib/screen-manual-publication";
import { SCENARIO_OWNERSHIP_MANUALS } from "@/lib/screen-manual-scenario-ownership";
import { SCREEN_MANUAL_OVERRIDES } from "@/lib/screen-manual-overrides";
import { SCREEN_MANUALS, type ScreenManualDefinition } from "@/lib/screen-manuals";

export const Route = createFileRoute("/manual")({
  head: () => ({
    meta: [
      { title: "Central de manuais - Práxis" },
      {
        name: "description",
        content:
          "Consulte os fluxos, campos, permissões, estados, bloqueios e exemplos das telas do Práxis.",
      },
    ],
  }),
  component: ManualPage,
});

const REPLACED_BASE_MANUALS = new Set([
  "jornadas",
  "operacao",
  "primeiros-passos",
  "parceiros",
]);
const REPLACED_OVERRIDE_MANUALS = new Set(["jornadas-composicao", "central-operational"]);
const PAGE_SIZE = 20;

const MANUALS = [
  ...SCENARIO_OWNERSHIP_MANUALS,
  ...ANALYSIS_OPERATION_MANUALS,
  ...ACCESS_ONBOARDING_MANUALS,
  ...PUBLICATION_MANUALS,
  ...PORTABILITY_MANUALS,
  ...COMPETENCY_OWNERSHIP_MANUALS,
  ...SCREEN_MANUAL_OVERRIDES.filter(
    (manual) => !REPLACED_OVERRIDE_MANUALS.has(manual.id),
  ),
  ...SCREEN_MANUALS.filter((manual) => !REPLACED_BASE_MANUALS.has(manual.id)),
];

type ManualCategoryDefinition = {
  id: string;
  label: string;
  manualIds: Set<string>;
};

const MANUAL_CATEGORIES: ManualCategoryDefinition[] = [
  {
    id: "visao-geral",
    label: "Visão geral",
    manualIds: new Set(["manual", "dashboard", "onboarding-inicial"]),
  },
  {
    id: "criacao-conteudo",
    label: "Criação e conteúdo",
    manualIds: new Set([
      "personagem-contexto-inicial",
      "editor-dialogo",
      "mapa-fluxo",
      "nova-avaliacao-catalogo",
      "objetivo-somente-leitura",
      "avaliacoes-catalogo",
      "validador-diagnostico",
      "conformidade-contextual",
      "portabilidade-avaliacoes",
      "criacao-avaliacao",
      "avaliacoes",
      "competencias",
    ]),
  },
  {
    id: "jornadas-participacao",
    label: "Jornadas e participação",
    manualIds: new Set([
      "jornadas-confirmacoes",
      "participacoes",
      "convite-jornada",
      "convites",
      "experiencia-participante",
    ]),
  },
  {
    id: "resultados-analise",
    label: "Resultados e análise",
    manualIds: new Set([
      "talent-match-contextual",
      "piloto-indicadores",
      "resultados",
      "talent-match",
    ]),
  },
  {
    id: "operacao-publicacao",
    label: "Operação e publicação",
    manualIds: new Set([
      "central-operacional-fila",
      "governanca-publicacao",
      "central-operacional",
    ]),
  },
  {
    id: "integracoes",
    label: "Integrações",
    manualIds: new Set(["ativacao-gupy-preflight", "integracoes"]),
  },
  {
    id: "administracao-acesso",
    label: "Administração e acesso",
    manualIds: new Set([
      "perfil-empresa",
      "equipe-acessos",
      "parceiros-condicional",
      "administracao-empresa",
      "administracao-plataforma",
      "acesso",
    ]),
  },
];

const FALLBACK_CATEGORY = { id: "outros", label: "Outros" } as const;

function normalizeSearchValue(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase("pt-BR");
}

function resolveManualCategory(manual: ScreenManualDefinition) {
  const category = MANUAL_CATEGORIES.find((item) => item.manualIds.has(manual.id));
  return category ?? FALLBACK_CATEGORY;
}

function buildManualSearchText(manual: ScreenManualDefinition) {
  return normalizeSearchValue(
    [
      manual.title,
      manual.purpose,
      ...manual.flow,
      ...manual.fields.flatMap((field) => [field.name, field.description]),
      ...manual.permissions,
      ...manual.states,
      ...manual.blocks,
      ...manual.examples,
      ...manual.shortcuts,
    ].join(" "),
  );
}

function ManualPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [categoryId, setCategoryId] = useState("all");
  const [selectedManualId, setSelectedManualId] = useState(MANUALS[0]?.id ?? "");
  const [currentPage, setCurrentPage] = useState(1);

  const manualEntries = useMemo(
    () =>
      MANUALS.map((manual) => ({
        manual,
        category: resolveManualCategory(manual),
        searchText: buildManualSearchText(manual),
      })),
    [],
  );

  const categoryOptions = useMemo(() => {
    const options = MANUAL_CATEGORIES.map((category) => ({
      id: category.id,
      label: category.label,
      count: manualEntries.filter((entry) => entry.category.id === category.id).length,
    })).filter((category) => category.count > 0);
    const fallbackCount = manualEntries.filter(
      (entry) => entry.category.id === FALLBACK_CATEGORY.id,
    ).length;

    if (fallbackCount > 0) {
      options.push({
        id: FALLBACK_CATEGORY.id,
        label: FALLBACK_CATEGORY.label,
        count: fallbackCount,
      });
    }

    return options;
  }, [manualEntries]);

  const filteredEntries = useMemo(() => {
    const normalizedSearch = normalizeSearchValue(searchTerm.trim());

    return manualEntries.filter((entry) => {
      const matchesCategory = categoryId === "all" || entry.category.id === categoryId;
      const matchesSearch = !normalizedSearch || entry.searchText.includes(normalizedSearch);
      return matchesCategory && matchesSearch;
    });
  }, [categoryId, manualEntries, searchTerm]);

  const totalPages = Math.max(1, Math.ceil(filteredEntries.length / PAGE_SIZE));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const pageStart = (safeCurrentPage - 1) * PAGE_SIZE;
  const visibleEntries = filteredEntries.slice(pageStart, pageStart + PAGE_SIZE);
  const selectedEntry =
    manualEntries.find((entry) => entry.manual.id === selectedManualId) ?? manualEntries[0];

  useEffect(() => {
    function synchronizeManualWithHash() {
      const hashId = decodeURIComponent(window.location.hash.replace(/^#/, ""));
      if (hashId && MANUALS.some((manual) => manual.id === hashId)) {
        setSelectedManualId(hashId);
      }
    }

    synchronizeManualWithHash();
    window.addEventListener("hashchange", synchronizeManualWithHash);
    return () => window.removeEventListener("hashchange", synchronizeManualWithHash);
  }, []);

  function handleSearchChange(value: string) {
    setSearchTerm(value);
    setCurrentPage(1);
  }

  function handleCategoryChange(value: string) {
    setCategoryId(value);
    setCurrentPage(1);
  }

  function handleSelectManual(manualId: string) {
    setSelectedManualId(manualId);
    window.history.replaceState(
      null,
      "",
      `${window.location.pathname}${window.location.search}#${encodeURIComponent(manualId)}`,
    );
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl">
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
                Pesquise uma tela ou filtre por área para consultar somente o manual necessário.
              </p>
            </div>
          </div>
        </header>

        <div className="grid gap-6 lg:grid-cols-[20rem_minmax(0,1fr)] lg:items-start">
          <aside className="rounded-xl border border-border bg-card p-4 lg:sticky lg:top-20">
            <div className="space-y-4">
              <div>
                <label htmlFor="manual-search" className="text-sm font-semibold text-foreground">Buscar manual</label>
                <div className="relative mt-2">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input id="manual-search" value={searchTerm} onChange={(event) => handleSearchChange(event.target.value)} placeholder="Tela, campo, estado ou bloqueio" className="pl-9" />
                </div>
              </div>
              <div>
                <label htmlFor="manual-category" className="text-sm font-semibold text-foreground">Área do sistema</label>
                <select id="manual-category" value={categoryId} onChange={(event) => handleCategoryChange(event.target.value)} className="mt-2 min-h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none focus-visible:ring-2 focus-visible:ring-ring">
                  <option value="all">Todas as áreas ({manualEntries.length})</option>
                  {categoryOptions.map((category) => <option key={category.id} value={category.id}>{category.label} ({category.count})</option>)}
                </select>
              </div>
            </div>

            <div className="mt-5 flex items-center justify-between border-t border-border pt-4 text-xs text-muted-foreground">
              <span aria-live="polite">{filteredEntries.length} {filteredEntries.length === 1 ? "manual" : "manuais"}</span>
              {filteredEntries.length > PAGE_SIZE && <span>Página {safeCurrentPage} de {totalPages}</span>}
            </div>

            <nav aria-label="Manuais encontrados" className="mt-3">
              {visibleEntries.length > 0 ? (
                <div className="max-h-[32rem] space-y-2 overflow-y-auto pr-1">
                  {visibleEntries.map((entry) => {
                    const isSelected = entry.manual.id === selectedEntry?.manual.id;
                    return (
                      <button key={entry.manual.id} type="button" onClick={() => handleSelectManual(entry.manual.id)} aria-current={isSelected ? "page" : undefined} className={`w-full rounded-lg border px-3 py-3 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${isSelected ? "border-primary bg-primary/10" : "border-border bg-background hover:bg-accent"}`}>
                        <span className="block text-sm font-semibold text-foreground">{entry.manual.title}</span>
                        <span className="mt-1 block line-clamp-2 text-xs leading-5 text-muted-foreground">{entry.manual.purpose}</span>
                      </button>
                    );
                  })}
                </div>
              ) : (
                <div className="rounded-lg border border-dashed border-border bg-background p-4 text-sm leading-6 text-muted-foreground">Nenhum manual corresponde aos filtros informados.</div>
              )}
            </nav>

            {filteredEntries.length > PAGE_SIZE && (
              <div className="mt-4 flex items-center justify-between border-t border-border pt-4">
                <button type="button" onClick={() => setCurrentPage((page) => Math.max(1, page - 1))} disabled={safeCurrentPage === 1} className="inline-flex min-h-9 items-center gap-1 rounded-md border border-border bg-background px-2.5 text-xs font-medium text-foreground hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"><ChevronLeft className="h-4 w-4" />Anterior</button>
                <button type="button" onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))} disabled={safeCurrentPage === totalPages} className="inline-flex min-h-9 items-center gap-1 rounded-md border border-border bg-background px-2.5 text-xs font-medium text-foreground hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50">Próxima<ChevronRight className="h-4 w-4" /></button>
              </div>
            )}
          </aside>

          {selectedEntry ? (
            <article id={selectedEntry.manual.id} className="scroll-mt-24 rounded-xl border border-border bg-card shadow-sm">
              <header className="border-b border-border px-5 py-4 sm:px-6">
                <p className="text-xs font-semibold uppercase tracking-wide text-primary">{selectedEntry.category.label}</p>
                <h2 className="mt-1 text-xl font-semibold text-foreground sm:text-2xl">{selectedEntry.manual.title}</h2>
              </header>
              <ScreenManualContent manual={selectedEntry.manual} />
            </article>
          ) : (
            <section className="rounded-xl border border-dashed border-border bg-card p-8 text-sm text-muted-foreground">Nenhum manual foi cadastrado.</section>
          )}
        </div>
      </main>
    </AppShell>
  );
}
