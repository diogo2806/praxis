import { Link, useRouterState } from "@tanstack/react-router";
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
  const idx = wizardSteps.findIndex((s) => s.slug === current);
  const unlockedIdx = Math.max(
    idx,
    unlockedThrough ? wizardSteps.findIndex((s) => s.slug === unlockedThrough) : idx,
  );
  const showDialogReviewNavigation = current === "revisao" && currentPathname === "/nova/dialogo";

  return (
    <div className="mb-8 rounded-xl border border-border bg-card p-4">
      <div className="mb-3 text-xs text-muted-foreground">
        <span className="font-semibold text-foreground">{t.wizard.createTest}</span> ·{" "}
        {t.wizard.stepOf
          .replace("{current}", String(idx + 1))
          .replace("{total}", String(wizardSteps.length))}
      </div>
      <ol className="flex gap-2 overflow-x-auto pb-1 md:grid md:grid-cols-4 md:overflow-visible md:pb-0">
        {wizardSteps.map((s, i) => {
          const done = i < idx;
          const active = i === idx;
          const locked = i > unlockedIdx;
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
                  {done ? "✓" : s.n}
                </span>
                {t.wizard.step} {s.n}
              </div>
              <div className="mt-1 truncate text-xs font-medium text-foreground">
                {t.wizard.steps[s.slug]}
              </div>
            </>
          );
          return (
            <li key={s.slug} className="min-w-[8.25rem] md:min-w-0">
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
                  to={s.path}
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

      {showDialogReviewNavigation && (
        <nav
          aria-label="Navegação da revisão"
          className="mt-4 flex flex-wrap justify-end gap-2 border-t border-border pt-4"
        >
          <Link
            to="/nova/validador"
            search={simulationSearch}
            className="rounded-md border border-border bg-card px-3 py-2 text-sm font-medium text-foreground transition hover:bg-accent"
          >
            Revisão e prontidão
          </Link>
          <Link
            to="/nova/mapa"
            search={mapSearch}
            className="rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground transition hover:bg-primary/90"
          >
            Mapa do fluxo
          </Link>
        </nav>
      )}
    </div>
  );
}
