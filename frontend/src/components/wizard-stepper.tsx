import { Link, useRouterState } from "@tanstack/react-router";
import { Eye } from "lucide-react";
import {
  canonicalAuthoringRoutes,
  isScenarioAuthoringPath,
} from "@/lib/authoring-flow";
import { wizardSteps, type WizardSlug } from "@/lib/simulation-meta";
import { useLanguage } from "@/lib/language-context";
import { cn } from "@/lib/utils";

export function WizardStepper({
  current,
  unlockedThrough,
}: {
  current: WizardSlug;
  unlockedThrough?: WizardSlug;
}) {
  const currentSearch = useRouterState({ select: (state) => state.location.search });
  const currentPathname = useRouterState({ select: (state) => state.location.pathname });
  const simulationSearch = {
    simulationId:
      typeof currentSearch.simulationId === "string" ? currentSearch.simulationId : undefined,
    versionNumber:
      typeof currentSearch.versionNumber === "number" ? currentSearch.versionNumber : undefined,
  };
  const mapSearch = {
    ...simulationSearch,
    nodeId: typeof currentSearch.nodeId === "string" ? currentSearch.nodeId : undefined,
  };
  const { t } = useLanguage();
  const effectiveCurrent = isScenarioAuthoringPath(currentPathname) ? "cenario" : current;
  const idx = wizardSteps.findIndex((step) => step.slug === effectiveCurrent);
  const unlockedIdx = Math.max(
    idx,
    unlockedThrough ? wizardSteps.findIndex((step) => step.slug === unlockedThrough) : idx,
  );
  const showScenarioNavigation = isScenarioAuthoringPath(currentPathname);
  const canOpenPreview = Boolean(simulationSearch.simulationId && simulationSearch.versionNumber);

  return (
    <div className="mb-8 rounded-xl border border-border bg-card p-4">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-3 text-xs text-muted-foreground">
        <span>
          <span className="font-semibold text-foreground">{t.wizard.createTest}</span> ·{" "}
          {t.wizard.stepOf
            .replace("{current}", String(idx + 1))
            .replace("{total}", String(wizardSteps.length))}
        </span>
        {canOpenPreview && currentPathname !== canonicalAuthoringRoutes.preview && (
          <Link
            to={canonicalAuthoringRoutes.preview}
            search={simulationSearch}
            className="inline-flex min-h-10 items-center justify-center gap-2 rounded-md border border-primary/40 bg-primary/5 px-3 py-2 text-sm font-semibold text-primary transition hover:bg-primary/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <Eye className="h-4 w-4" />
            Testar como candidato
          </Link>
        )}
      </div>
      <ol className="flex gap-2 overflow-x-auto pb-1 md:grid md:grid-cols-4 md:overflow-visible md:pb-0">
        {wizardSteps.map((step, index) => {
          const done = index < idx;
          const active = index === idx;
          const locked = index > unlockedIdx;
          const content = (
            <>
              <div
                className={cn(
                  "flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-wider",
                  active && "text-foreground",
                  done && "text-foreground",
                  locked && "text-muted-foreground",
                  !active && !done && !locked && "text-foreground",
                )}
              >
                <span
                  className={cn(
                    "inline-flex h-4 w-4 items-center justify-center rounded-full text-[10px]",
                    active && "bg-primary text-primary-foreground",
                    done && "bg-success text-success-foreground",
                    locked && "bg-muted text-muted-foreground",
                    !active && !done && !locked && "bg-card text-foreground ring-1 ring-border",
                  )}
                >
                  {done ? "✓" : step.n}
                </span>
                {t.wizard.step} {step.n}
              </div>
              <div className="mt-1 truncate text-xs font-medium text-foreground">
                {t.wizard.steps[step.slug]}
              </div>
            </>
          );
          return (
            <li key={step.slug} className="min-w-[8.25rem] md:min-w-0">
              {locked ? (
                <div
                  aria-disabled="true"
                  title={t.wizard.completeCurrentStep}
                  className={cn(
                    "block cursor-default rounded-md border px-2 py-2 text-left opacity-70",
                    "border-border bg-muted/40",
                  )}
                >
                  {content}
                </div>
              ) : (
                <Link
                  to={step.path}
                  search={simulationSearch}
                  aria-current={active ? "step" : undefined}
                  className={cn(
                    "block rounded-md border px-2 py-2 text-left transition",
                    active && "border-primary bg-primary/5",
                    done && "border-success/40 bg-success/5",
                    !active && !done && "border-border bg-card hover:bg-accent",
                  )}
                >
                  {content}
                </Link>
              )}
            </li>
          );
        })}
      </ol>

      {showScenarioNavigation && (
        <nav
          aria-label="Subetapas do cenário"
          className="mt-4 flex flex-wrap gap-2 border-t border-border pt-4"
        >
          <Link
            to={canonicalAuthoringRoutes.character}
            search={simulationSearch}
            aria-current={
              currentPathname === canonicalAuthoringRoutes.character ? "page" : undefined
            }
            className={cn(
              "rounded-md border px-3 py-2 text-sm font-medium transition",
              currentPathname === canonicalAuthoringRoutes.character
                ? "border-primary bg-primary/10 text-primary"
                : "border-border bg-card text-foreground hover:bg-accent",
            )}
          >
            1. Personagem
          </Link>
          <Link
            to={canonicalAuthoringRoutes.dialogue}
            search={mapSearch}
            aria-current={
              currentPathname === canonicalAuthoringRoutes.dialogue ? "page" : undefined
            }
            className={cn(
              "rounded-md border px-3 py-2 text-sm font-medium transition",
              currentPathname === canonicalAuthoringRoutes.dialogue
                ? "border-primary bg-primary/10 text-primary"
                : "border-border bg-card text-foreground hover:bg-accent",
            )}
          >
            2. Diálogo
          </Link>
          <Link
            to={canonicalAuthoringRoutes.map}
            search={mapSearch}
            aria-current={currentPathname === canonicalAuthoringRoutes.map ? "page" : undefined}
            className={cn(
              "rounded-md border px-3 py-2 text-sm font-medium transition",
              currentPathname === canonicalAuthoringRoutes.map
                ? "border-primary bg-primary/10 text-primary"
                : "border-border bg-card text-foreground hover:bg-accent",
            )}
          >
            3. Mapa do fluxo
          </Link>
        </nav>
      )}
    </div>
  );
}
