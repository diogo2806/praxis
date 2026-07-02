import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Loader2, Send } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { MessageThread } from "@/components/marketplace/message-thread";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  listMarketplaceMessageThreads,
  sendMarketplaceProfessionalMessage,
} from "@/lib/api/praxis";
import { marketplaceMessageThreadsFallback } from "@/lib/marketplace-professional-fallback";

export const Route = createFileRoute("/profissional/mensagens")({
  head: () => ({
    meta: [{ title: "Mensagens - Marketplace Práxis" }],
  }),
  component: ProfessionalMessagesPage,
});

function ProfessionalMessagesPage() {
  const queryClient = useQueryClient();
  const [selectedThreadId, setSelectedThreadId] = useState<number | null>(null);
  const [body, setBody] = useState("");
  const threads = useQuery({
    queryKey: ["marketplace-message-threads", "professional"],
    queryFn: () => listMarketplaceMessageThreads("professional"),
    placeholderData: marketplaceMessageThreadsFallback,
    retry: false,
  });
  const selectedThread = useMemo(
    () => threads.data?.find((thread) => thread.id === selectedThreadId) ?? threads.data?.[0],
    [selectedThreadId, threads.data],
  );

  useEffect(() => {
    if (selectedThreadId == null && threads.data?.[0]) {
      setSelectedThreadId(threads.data[0].id);
    }
  }, [selectedThreadId, threads.data]);

  const send = useMutation({
    mutationFn: () =>
      sendMarketplaceProfessionalMessage({
        threadId: selectedThread?.id,
        body,
      }),
    onSuccess: async () => {
      setBody("");
      await queryClient.invalidateQueries({
        queryKey: ["marketplace-message-threads", "professional"],
      });
    },
  });

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-6xl px-5 py-6">
        <Button asChild variant="ghost" size="sm" className="mb-4">
          <Link to="/profissional">
            <ArrowLeft className="h-4 w-4" />
            Área do profissional
          </Link>
        </Button>

        <div className="mb-5">
          <div className="text-xs uppercase text-primary">Conversas</div>
          <h1 className="mt-1 text-2xl font-semibold">Mensagens</h1>
        </div>

        {threads.isLoading && (
          <div className="flex items-center gap-2 rounded-md border border-border bg-card p-4 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            Carregando mensagens
          </div>
        )}
        {threads.isError && (
          <StateBanner tone="danger" title="Não foi possível carregar as mensagens">
            {threads.error instanceof Error ? threads.error.message : "Tente novamente."}
          </StateBanner>
        )}
        {threads.data?.length === 0 && (
          <section className="rounded-md border border-border bg-card p-5">
            <p className="text-sm text-muted-foreground">Nenhuma conversa aberta.</p>
          </section>
        )}
        {selectedThread && (
          <section className="overflow-hidden rounded-md border border-border bg-card">
            <aside className="border-b border-border lg:border-b-0 lg:border-r">
              <div className="divide-y divide-border">
                {(threads.data ?? []).map((thread) => (
                  <button
                    key={thread.id}
                    type="button"
                    onClick={() => setSelectedThreadId(thread.id)}
                    className={`w-full px-4 py-3 text-left text-sm transition hover:bg-muted ${
                      thread.id === selectedThread.id ? "bg-muted" : ""
                    }`}
                  >
                    <div className="font-medium">Anúncio #{thread.listingId}</div>
                    <div className="mt-1 line-clamp-1 text-xs text-muted-foreground">
                      {thread.messages.at(-1)?.body ?? "Sem mensagens"}
                    </div>
                  </button>
                ))}
              </div>
            </aside>

            <div className="flex min-h-[520px] flex-col">
              <div className="border-b border-border px-4 py-3">
                <div className="font-medium">Anúncio #{selectedThread.listingId}</div>
              </div>

              <div className="flex-1 overflow-y-auto p-4">
                <MessageThread thread={selectedThread} />
              </div>

              <form
                className="border-t border-border p-4"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (!body.trim()) return;
                  send.mutate();
                }}
              >
                <textarea
                  value={body}
                  onChange={(event) => setBody(event.target.value)}
                  rows={3}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                />
                <div className="mt-3 flex justify-end">
                  <Button type="submit" disabled={send.isPending || !body.trim()}>
                    {send.isPending ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Send className="h-4 w-4" />
                    )}
                    Enviar
                  </Button>
                </div>
              </form>
            </div>
          </section>
        )}
      </div>
    </main>
  );
}
