import { wizardSteps, type WizardSlug } from "@/lib/mock";
import { cn } from "@/lib/utils";

export function WizardStepper({ current }: { current: WizardSlug }) {
  const idx = wizardSteps.findIndex((s) => s.slug === current);
  return (
    <div className="mb-8 rounded-xl border border-border bg-card p-4">
      <div className="mb-3 flex items-center justify-between text-xs text-muted-foreground">
        <div>
          <span className="font-semibold text-foreground">Nova simulação</span>{" "}
          · etapa {idx + 1} de {wizardSteps.length}
        </div>
        <div>
          Salvo automaticamente <span className="text-success">●</span>
        </div>
      </div>
      <ol className="grid grid-cols-3 gap-2 md:grid-cols-9">
        {wizardSteps.map((s, i) => {
          const done = i < idx;
          const active = i === idx;
          return (
            <li key={s.slug} className="min-w-0">
              <a
                href={`/nova/${s.slug}`}
                className={cn(
                  "block rounded-md border px-2 py-2 text-left transition",
                  active && "border-primary bg-primary/5",
                  done && "border-success/40 bg-success/5",
                  !active && !done && "border-border bg-card hover:bg-accent",
                )}
              >
                <div
                  className={cn(
                    "flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-wider",
                    active && "text-primary",
                    done && "text-success",
                    !active && !done && "text-muted-foreground",
                  )}
                >
                  <span
                    className={cn(
                      "inline-flex h-4 w-4 items-center justify-center rounded-full text-[10px]",
                      active && "bg-primary text-primary-foreground",
                      done && "bg-success text-success-foreground",
                      !active && !done && "bg-muted text-muted-foreground",
                    )}
                  >
                    {done ? "✓" : s.n}
                  </span>
                  Passo {s.n}
                </div>
                <div className="mt-1 truncate text-xs font-medium text-foreground">
                  {s.label}
                </div>
              </a>
            </li>
          );
        })}
      </ol>
    </div>
  );
}