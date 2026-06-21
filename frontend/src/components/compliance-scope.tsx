import { Link } from "@tanstack/react-router";
import { Scale, ShieldCheck, UserRound } from "lucide-react";
import { cn } from "@/lib/utils";

type ComplianceArea = "governanca" | "lgpd" | "defensabilidade" | "compliance";

const areas = [
  {
    id: "governanca",
    to: "/governanca",
    icon: ShieldCheck,
    title: "Governança e auditoria",
    body: "Histórico de versões, eventos registrados, aprovações e criação de novo rascunho.",
  },
  {
    id: "lgpd",
    to: "/lgpd",
    icon: UserRound,
    title: "LGPD e direitos do candidato",
    body: "Retenção de dados, canal de revisão humana e explicação do resultado para o titular.",
  },
  {
    id: "defensabilidade",
    to: "/defensabilidade",
    icon: Scale,
    title: "Defensabilidade do resultado",
    body: "Base técnica do teste: construto, score auditável, pesos e limites da promessa comercial.",
  },
] as const;

export function ComplianceScope({ current }: { current: ComplianceArea }) {
  return (
    <section className="mb-5 rounded-md border border-border bg-card p-4">
      <div className="mb-3">
        <div className="text-xs font-semibold uppercase text-muted-foreground">
          Como estas telas se dividem
        </div>
        <p className="mt-1 text-sm text-muted-foreground">
          As três telas usam parte das mesmas evidências, mas respondem perguntas diferentes.
        </p>
      </div>
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
