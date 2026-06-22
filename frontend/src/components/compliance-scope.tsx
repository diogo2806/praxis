import { Link } from "@tanstack/react-router";
import { Scale, ShieldCheck, UserRound } from "lucide-react";
import { cn } from "@/lib/utils";
import { useLanguage } from "@/lib/language-context";

type ComplianceArea = "governanca" | "lgpd" | "defensabilidade" | "compliance";

export function ComplianceScope({ current }: { current: ComplianceArea }) {
  const { t } = useLanguage();

  const areas = [
    {
      id: "governanca",
      to: "/governanca",
      icon: ShieldCheck,
      title: t.governance.heading,
      body: t.descriptions.governanceNav,
    },
    {
      id: "lgpd",
      to: "/lgpd",
      icon: UserRound,
      title: t.lgpd.heading,
      body: t.descriptions.lgpd,
    },
    {
      id: "defensabilidade",
      to: "/defensabilidade",
      icon: Scale,
      title: t.defensability.heading,
      body: t.descriptions.defensibility,
    },
  ] as const;

  return (
    <section className="mb-5 rounded-md border border-border bg-card p-4">
      <div className="grid gap-3 lg:grid-cols-3">
        {areas.map(({ id, to, icon: Icon, title, body }) => {
          const active = id === current;
          return (
            <Link
              key={id}
              to={to}
              className={cn(
                "rounded-md border p-3 text-left transition hover:bg-accent",
                active
                  ? "border-primary/40 bg-primary/10"
                  : "border-border bg-background",
              )}
              aria-current={active ? "page" : undefined}
            >
              <div className="flex items-start gap-3">
                <Icon
                  className={cn(
                    "mt-0.5 h-4 w-4 shrink-0",
                    active ? "text-primary" : "text-muted-foreground",
                  )}
                />
                <div className="min-w-0">
                  <div className="text-sm font-semibold">{title}</div>
                  <p className="mt-1 text-xs leading-relaxed text-muted-foreground">{body}</p>
                </div>
              </div>
            </Link>
          );
        })}
      </div>
    </section>
  );
}
