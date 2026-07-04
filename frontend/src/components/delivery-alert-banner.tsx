import { Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { StateBanner } from "@/components/praxis-ui";
import { listResultDeliveries } from "@/lib/api/praxis";
import { appShellCopy } from "@/lib/app-shell-copy";
import type { Language } from "@/lib/translations";

export function DeliveryAlertBanner({ language }: { language: Language }) {
  const copy = appShellCopy[language];
  const deliveryQuery = useQuery({
    queryKey: ["result-deliveries", "dlq", "dashboard-banner"],
    queryFn: () => listResultDeliveries({ status: "dlq" }),
    retry: false,
    refetchInterval: 60000,
  });
  const deliveryCount = deliveryQuery.data?.length ?? 0;

  if (deliveryCount === 0) return null;

  return (
    <div className="px-6 pt-6 lg:px-10">
      <StateBanner
        tone="danger"
        title={`${copy.dlqTitle} (${deliveryCount})`}
        action={
          <Link to="/notifications" className="inline-flex items-center gap-2 rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium">
            {copy.dlqCta}
          </Link>
        }
      >
        {copy.dlqBody}
      </StateBanner>
    </div>
  );
}
