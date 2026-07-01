import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Loader2, ShieldCheck, Star } from "lucide-react";

import { LinkedinEmbed } from "@/components/marketplace/linkedin-embed";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { getMarketplaceProfessional } from "@/lib/api/praxis";
import { professionalStatusLabels } from "@/lib/marketplace";

export const Route = createFileRoute("/marketplace/professionals/$professionalId")({
  head: () => ({
    meta: [{ title: "Profissional - Marketplace Praxis" }],
  }),
  component: MarketplaceProfessionalProfilePage,
});

function MarketplaceProfessionalProfilePage() {
  const { professionalId } = Route.useParams();
  const profile = useQuery({
    queryKey: ["marketplace-professional", professionalId],
    queryFn: () => getMarketplaceProfessional(professionalId),
    enabled: Number.isFinite(Number(professionalId)),
  });

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-4xl px-5 py-6">
        <Button asChild variant="ghost" size="sm" className="mb-4">
          <Link to="/marketplace">
            <ArrowLeft className="h-4 w-4" />
            Marketplace
          </Link>
        </Button>

        {profile.isLoading && (
          <div className="flex items-center gap-2 rounded-md border border-border bg-card p-4 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            Carregando profissional
          </div>
        )}
        {profile.isError && (
          <StateBanner tone="danger" title="Nao foi possivel carregar o profissional">
            {profile.error instanceof Error ? profile.error.message : "Tente novamente."}
          </StateBanner>
        )}
        {profile.data && (
          <section className="rounded-md border border-border bg-card p-5">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <div className="inline-flex items-center gap-1 rounded-md bg-primary/10 px-2 py-1 text-xs font-medium text-primary">
                  <ShieldCheck className="h-3.5 w-3.5" />
                  {professionalStatusLabels[profile.data.verificationStatus]}
                </div>
                <h1 className="mt-3 text-3xl font-semibold">{profile.data.displayName}</h1>
                <div className="mt-3 flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
                  <span className="inline-flex items-center gap-1">
                    <Star className="h-4 w-4 text-warning" />
                    {profile.data.averageRating ? Number(profile.data.averageRating).toFixed(1) : "Sem notas"}
                  </span>
                  <span>{profile.data.totalReviews} review(s)</span>
                  <span>{profile.data.totalSales} venda(s)</span>
                </div>
              </div>
              <LinkedinEmbed url={profile.data.linkedinUrl} />
            </div>

            {profile.data.specialties.length > 0 && (
              <div className="mt-5 flex flex-wrap gap-2">
                {profile.data.specialties.map((specialty) => (
                  <span key={specialty} className="rounded-md border border-border bg-background px-2 py-1 text-xs">
                    {specialty}
                  </span>
                ))}
              </div>
            )}

            {profile.data.bio && (
              <p className="mt-5 whitespace-pre-line text-sm leading-6 text-muted-foreground">{profile.data.bio}</p>
            )}
          </section>
        )}
      </div>
    </main>
  );
}
