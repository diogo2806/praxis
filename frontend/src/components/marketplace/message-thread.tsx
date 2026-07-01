import type { MarketplaceMessageThread } from "@/lib/api/marketplace";
import { formatMarketplaceDate } from "@/lib/marketplace";
import { cn } from "@/lib/utils";

export function MessageThread({ thread }: { thread: MarketplaceMessageThread }) {
  return (
    <div className="space-y-3">
      {thread.messages.map((message) => (
        <article
          key={message.id}
          className={cn(
            "max-w-[78%] rounded-md border border-border p-3 text-sm",
            message.senderType === "PROFESSIONAL" ? "ml-auto bg-primary/10" : "bg-background",
          )}
        >
          <div className="mb-1 text-xs text-muted-foreground">{formatMarketplaceDate(message.createdAt)}</div>
          <p className="whitespace-pre-line">{message.body}</p>
        </article>
      ))}
    </div>
  );
}
