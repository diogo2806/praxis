import { createFileRoute } from "@tanstack/react-router";
import { CreditCard } from "lucide-react";
import { AppShell } from "@/components/app-shell";

export const Route = createFileRoute("/billing")({
  component: BillingPlaceholder,
});

function BillingPlaceholder() {
  return (
    <AppShell>
      <section className="rounded-md border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <CreditCard className="h-5 w-5 text-muted-foreground" />
          <div>
            <h1 className="text-xl font-semibold">Plano e cobrança</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              A visão consolidada de plano e uso está disponível no dashboard.
            </p>
          </div>
        </div>
      </section>
    </AppShell>
  );
}
